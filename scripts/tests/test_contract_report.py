import importlib.util
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / 'contract_report.py'
HEAD = 'd0c8056990f7a47fcc202acffa387ba066bcfc67'
SUITE = 'io.github.easy4j.opencli.contract.OpenCliArgvContractTest'
SECOND_SUITE = 'io.github.easy4j.opencli.contract.OpenCliStructuredArgvContractTest'


class ContractReportTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(SCRIPT.is_file(), 'C10 contract report runner is not implemented')
        spec = importlib.util.spec_from_file_location('contract_report', SCRIPT)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)

    def xml(self, tests=2, skipped=0, failures=0, errors=0, suite=SUITE):
        executed = tests - skipped
        cases = []
        for i in range(tests):
            child = '<skipped/>' if i >= executed else ('<failure/>' if i < failures else ('<error/>' if i < failures + errors else ''))
            cases.append('<testcase classname="' + suite + '" name="case' + str(i) + '">' + child + '</testcase>')
        (self.root / ('TEST-' + suite + '.xml')).write_text(
            '<testsuite name="' + suite + '" tests="' + str(tests) + '" skipped="' + str(skipped)
            + '" failures="' + str(failures) + '" errors="' + str(errors) + '">' + ''.join(cases) + '</testsuite>', encoding='utf-8')

    def report(self, **kwargs):
        return self.module.build_report(self.root, head=kwargs.get('head', HEAD),
            branch='feature/2.0.x', java_version='openjdk 17 (test fixture)',
            maven_version='Apache Maven (test fixture)', exit_code=kwargs.get('exit_code', 0),
            required_suites=kwargs.get('required_suites', {'argv': [SUITE]}),
            trace_methods=kwargs.get('trace_methods'))

    def test_complete_argv_evidence_passes_but_live_is_not_run(self):
        self.xml()
        report = self.report()
        self.assertEqual('PASS', report['status'])
        self.assertEqual(2, report['layers']['argv']['executed'])
        self.assertEqual('NOT_RUN', report['layers']['real-execution']['status'])
        self.assertEqual(HEAD, report['head'])
        json.dumps(report)

    def test_process_layer_is_separate_from_real_execution(self):
        self.xml()
        report = self.module.build_report(self.root, head=HEAD, branch='feature/2.0.x',
            java_version='fixture-jdk', maven_version='fixture-maven', exit_code=0,
            required_suites={'process': [SUITE]})
        self.assertEqual('PASS', report['status'])
        self.assertEqual(2, report['layers']['process']['executed'])
        self.assertEqual('NOT_RUN', report['layers']['real-execution']['status'])

    def test_missing_report_fails(self):
        self.assertEqual('FAIL', self.report()['status'])

    def test_missing_traced_method_fails_despite_successful_suite(self):
        self.xml()
        result = self.report(trace_methods={SUITE: {'case0', 'missing'}})
        self.assertEqual('FAIL', result['status'])
        self.assertEqual(2, result['layers']['argv']['executed'])
        self.assertEqual({SUITE: ['missing']}, result['layers']['argv']['missingMethods'])

    def test_parameters_are_removed_from_observed_method_name(self):
        self.xml()
        path = self.root / ('TEST-' + SUITE + '.xml')
        text = path.read_text(encoding='utf-8').replace('name="case0"', 'name="case0(Path)[1]"')
        path.write_text(text, encoding='utf-8')
        result = self.report(trace_methods={SUITE: {'case0'}, SECOND_SUITE: {'unselected'}})
        self.assertEqual('PASS', result['status'])
        self.assertEqual({}, result['layers']['argv']['missingMethods'])

    def test_case_from_another_class_cannot_satisfy_traced_method(self):
        self.xml()
        path = self.root / ('TEST-' + SUITE + '.xml')
        text = path.read_text(encoding='utf-8').replace(
            'classname="' + SUITE + '"', 'classname="' + SECOND_SUITE + '"', 1)
        path.write_text(text, encoding='utf-8')
        result = self.report(trace_methods={SUITE: {'case0'}})
        self.assertEqual({SUITE: ['case0']}, result['layers']['argv']['missingMethods'])

    def test_trace_collects_pinned_test_method_links(self):
        path = self.root / 'trace.md'
        link = ('[`OpenCliArgvContractTest#case0`](https://github.com/easy-4-java/opencli-java-sdk/blob/'
                + HEAD + '/src/test/java/' + SUITE.replace('.', '/') + '.java#L17)')
        path.write_text(link + '\n' + link, encoding='utf-8')
        self.assertEqual({SUITE: {'case0'}}, self.module.read_trace_methods(path))
        path.write_text(link.replace('OpenCliArgvContractTest#', 'WrongTest#'), encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'another class'):
            self.module.read_trace_methods(path)

    def test_trace_requires_pinned_method_links(self):
        path = self.root / 'trace.md'
        path.write_text('missing method links', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'no pinned'):
            self.module.read_trace_methods(path)

    def test_zero_tests_fails(self):
        self.xml(tests=0)
        self.assertEqual('FAIL', self.report()['status'])

    def test_zero_tests_in_one_required_suite_cannot_hide_behind_a_passing_suite(self):
        self.xml()
        self.xml(tests=0, suite=SECOND_SUITE)
        result = self.report(required_suites={'argv': [SUITE, SECOND_SUITE]})
        self.assertEqual('FAIL', result['status'])
        self.assertEqual([SECOND_SUITE], result['layers']['argv']['unprovenSuites'])

    def test_suite_cannot_supply_multiple_evidence_layers(self):
        self.xml()
        result = self.report(required_suites={'argv': [SUITE], 'real-execution': [SUITE]})
        self.assertEqual('FAIL', result['status'])
        self.assertIn('suite attribution must be unique across layers', result['problems'])

    def test_all_skipped_fails(self):
        self.xml(tests=2, skipped=2)
        self.assertEqual('FAIL', self.report()['status'])

    def test_partly_skipped_fails(self):
        self.xml(tests=2, skipped=1)
        self.assertEqual('FAIL', self.report()['status'])

    def test_failure_fails(self):
        self.xml(failures=1)
        self.assertEqual('FAIL', self.report()['status'])

    def test_error_fails(self):
        self.xml(errors=1)
        self.assertEqual('FAIL', self.report()['status'])

    def test_nonzero_command_exit_fails_despite_green_xml(self):
        self.xml()
        self.assertEqual('FAIL', self.report(exit_code=1)['status'])

    def test_malformed_xml_fails_closed(self):
        (self.root / 'TEST-broken.xml').write_text('<broken', encoding='utf-8')
        self.assertEqual('FAIL', self.report()['status'])

    def test_partial_sha_is_not_accepted(self):
        self.xml()
        self.assertEqual('FAIL', self.report(head='d0c8056')['status'])

    def test_declared_counts_cannot_hide_missing_testcases(self):
        self.xml()
        path = self.root / ('TEST-' + SUITE + '.xml')
        path.write_text(path.read_text().replace('tests="2"', 'tests="100"'), encoding='utf-8')
        self.assertEqual('FAIL', self.report()['status'])

    def test_undeclared_failure_fails(self):
        self.xml()
        path = self.root / ('TEST-' + SUITE + '.xml')
        path.write_text(path.read_text().replace('</testcase>', '<failure/></testcase>', 1), encoding='utf-8')
        self.assertEqual('FAIL', self.report()['status'])


class InstalledContractsTest(unittest.TestCase):
    def test_existing_reports_do_not_prove_an_invocation_that_executed_no_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            bin_dir = root / 'bin'
            bin_dir.mkdir()
            scripts = root / 'scripts'
            scripts.mkdir()
            runner = SCRIPT.with_name('run_installed_contracts.sh')
            for script in (SCRIPT, runner):
                shutil.copy(script, scripts)
            reports = root / 'target' / 'surefire-reports'
            reports.mkdir(parents=True)
            names = ['OpenCliCapabilityOverrideIT', 'OpenCliReadOnlyDiagnosticsIT']
            for name in names:
                (reports / ('TEST-' + name + '.xml')).write_text(
                    '<testsuite name="io.github.easy4j.opencli.contract.' + name + '" tests="1">'
                    '<testcase name="previousRun"/></testsuite>', encoding='utf-8')

            commands = {
                'npm': 'exit 0',
                'node': 'case "$1" in --version) echo v24.18.0;; -p) echo /fixture/node;; *) echo 1.8.8;; esac',
                'java': 'echo fixture-jdk',
                'mvn': 'if [[ "$1" == -version ]]; then echo fixture-maven; fi',
                'git': 'echo ' + HEAD,
                'python': 'exec "$OPENCLI_TEST_PYTHON" "$@"',
            }
            for name, body in commands.items():
                command = bin_dir / name
                command.write_text('#!/usr/bin/env bash\n' + body + '\n', encoding='utf-8')
                command.chmod(0o755)

            env = dict(os.environ, PATH=str(bin_dir) + os.pathsep + os.environ['PATH'],
                       RUNNER_OS='Linux', RUNNER_TEMP=directory, GITHUB_REF_NAME='feature/2.0.x',
                       OPENCLI_TEST_PYTHON=sys.executable)
            result = subprocess.run(['bash', 'scripts/run_installed_contracts.sh', 'mvn'],
                                    cwd=root, env=env, capture_output=True, text=True, timeout=15)
            report = json.loads((root / 'platform-evidence' / 'installed-report.json').read_text(encoding='utf-8'))
            self.assertEqual(1, result.returncode, result.stdout + result.stderr)
            self.assertEqual('FAIL', report['layers']['real-execution']['status'])
            self.assertEqual(0, report['layers']['real-execution']['executed'])
            archived = list((root / 'platform-evidence').glob('pre-install-surefire.*/surefire-reports/TEST-*.xml'))
            self.assertCountEqual(['TEST-' + name + '.xml' for name in names], [p.name for p in archived])


if __name__ == '__main__':
    unittest.main()
