import importlib.util
import json
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / 'contract_report.py'
HEAD = 'd0c8056990f7a47fcc202acffa387ba066bcfc67'
SUITE = 'io.github.easy4j.opencli.contract.OpenCliArgvContractTest'


class ContractReportTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(SCRIPT.is_file(), 'C10 contract report runner is not implemented')
        spec = importlib.util.spec_from_file_location('contract_report', SCRIPT)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)

    def xml(self, tests=2, skipped=0, failures=0, errors=0):
        executed = tests - skipped
        cases = []
        for i in range(tests):
            child = '<skipped/>' if i >= executed else ('<failure/>' if i < failures else ('<error/>' if i < failures + errors else ''))
            cases.append('<testcase name="case' + str(i) + '">' + child + '</testcase>')
        (self.root / ('TEST-' + SUITE + '.xml')).write_text(
            '<testsuite name="' + SUITE + '" tests="' + str(tests) + '" skipped="' + str(skipped)
            + '" failures="' + str(failures) + '" errors="' + str(errors) + '">' + ''.join(cases) + '</testsuite>', encoding='utf-8')

    def report(self, **kwargs):
        return self.module.build_report(self.root, head=kwargs.get('head', HEAD),
            branch='feature/2.0.x', java_version='openjdk 17 (test fixture)',
            maven_version='Apache Maven (test fixture)', exit_code=kwargs.get('exit_code', 0),
            required_suites={'argv': [SUITE]})

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

    def test_zero_tests_fails(self):
        self.xml(tests=0)
        self.assertEqual('FAIL', self.report()['status'])

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


if __name__ == '__main__':
    unittest.main()
