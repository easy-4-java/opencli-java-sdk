#!/usr/bin/env python3
"""Build fail-closed, layer-specific evidence from real Surefire XML reports.

This is not a code coverage percentage or a claim that OpenCLI websites work.
Only explicitly selected suites contribute to a layer. No network is used.
"""
import argparse
import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

LAYERS = ('enumeration', 'argv', 'process', 'protocol', 'typed-result', 'real-execution')
TRACE_TEST_LINK = re.compile(
    r'\[`(?P<class_name>[\w$]+)#(?P<method>[\w$]+)`\]'
    r'\(https://github\.com/easy-4-java/opencli-java-sdk/blob/[0-9a-f]{40}/'
    r'src/test/java/(?P<path>[\w$/]+)\.java#L\d+\)'
)


def read_trace_methods(path):
    """Read the pinned Java method links from the requirement trace."""
    methods = {}
    for link in TRACE_TEST_LINK.finditer(path.read_text(encoding='utf-8')):
        suite = link['path'].replace('/', '.')
        if suite.rsplit('.', 1)[-1] != link['class_name']:
            raise ValueError('requirement trace links a method to another class')
        methods.setdefault(suite, set()).add(link['method'])
    if not methods:
        raise ValueError('requirement trace contains no pinned Java test methods')
    return methods


def _suite_summary(suite):
    declared = {key: int(suite.attrib.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')}
    cases = suite.findall('testcase')
    actual = {'tests': len(cases), 'failures': sum(c.find('failure') is not None for c in cases),
              'errors': sum(c.find('error') is not None for c in cases),
              'skipped': sum(c.find('skipped') is not None for c in cases)}
    if declared != actual or any(v < 0 for v in declared.values()):
        raise ValueError('declared testcase counts disagree with XML evidence')
    actual['executed'] = actual['tests'] - actual['skipped']
    return actual


def build_report(reports_dir, *, head, branch, java_version, maven_version, exit_code, required_suites,
                 trace_methods=None):
    """Return a JSON-serializable report; malformed/missing evidence fails closed."""
    problems = []
    if not re.fullmatch(r'[0-9a-fA-F]{40}', head or ''):
        problems.append('exact 40-character source HEAD is required')
    if not branch or not java_version.strip() or not maven_version.strip():
        problems.append('branch and actual tool versions are required')
    if exit_code != 0:
        problems.append('verification command exited nonzero')
    if not required_suites or any(layer not in LAYERS or not names for layer, names in required_suites.items()):
        problems.append('non-empty required suites must use recognized layers')
    attributed = [name for names in required_suites.values() for name in names]
    if len(attributed) != len(set(attributed)):
        problems.append('suite attribution must be unique across layers')
    trace_methods = {} if trace_methods is None else trace_methods
    expected_methods = {name: trace_methods[name] for name in attributed if name in trace_methods}
    if trace_methods and not expected_methods:
        problems.append('no selected suites match the requirement trace')
    suites = {}
    passed_methods = {}
    for path in sorted(pathlib.Path(reports_dir).glob('TEST-*.xml')):
        try:
            root = ET.parse(path).getroot()
            nodes = [root] if root.tag == 'testsuite' else list(root.findall('testsuite'))
            if not nodes:
                raise ValueError('no testsuite')
            for suite in nodes:
                name = suite.attrib['name']
                if name in suites:
                    raise ValueError('duplicate testsuite')
                suites[name] = _suite_summary(suite)
                if name in expected_methods:
                    passed_methods[name] = {
                        re.split(r'[\[(]', case.attrib.get('name', ''), maxsplit=1)[0]
                        for case in suite.findall('testcase')
                        if case.attrib.get('classname') == name
                        and not any(case.find(tag) is not None for tag in ('skipped', 'failure', 'error'))
                    }
        except (ET.ParseError, OSError, KeyError, ValueError):
            problems.append('invalid or duplicate Surefire report: ' + path.name)
    layers = {}
    for layer in LAYERS:
        names = list(required_suites.get(layer, []))
        counts = dict.fromkeys(('tests', 'executed', 'skipped', 'failures', 'errors'), 0)
        missing = [name for name in names if name not in suites]
        for name in names:
            for key in counts:
                counts[key] += suites.get(name, {}).get(key, 0)
        unproven = [
            name for name in names
            if name not in suites or suites[name]['executed'] == 0
            or any(suites[name][key] for key in ('skipped', 'failures', 'errors'))
        ]
        absent_methods = {name: sorted(expected_methods[name] - passed_methods.get(name, set()))
                          for name in names if name in expected_methods}
        absent_methods = {name: methods for name, methods in absent_methods.items() if methods}
        state = 'NOT_RUN' if not names else ('FAIL' if unproven or absent_methods else 'PASS')
        method_count = sum(len(expected_methods.get(name, ())) for name in names)
        layers[layer] = dict(counts, status=state, requiredSuites=names, missingSuites=missing,
                             unprovenSuites=unproven, requiredMethodCount=method_count,
                             missingMethods=absent_methods)
        if unproven:
            problems.append('required layer lacks passing, non-skipped execution: ' + layer)
        if absent_methods:
            problems.append('required layer lacks traced test methods: ' + layer)
    return {'schemaVersion': 1, 'status': 'FAIL' if problems else 'PASS',
            'head': head, 'branch': branch, 'javaVersion': java_version,
            'mavenVersion': maven_version, 'commandExitCode': exit_code,
            'layers': layers, 'problems': problems,
            'scope': 'Explicit Surefire suites only; synthetic argv/process probes are not live OpenCLI verification.'}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reports', type=pathlib.Path, required=True)
    parser.add_argument('--head', required=True)
    parser.add_argument('--branch', required=True)
    parser.add_argument('--java-version-file', type=pathlib.Path, required=True)
    parser.add_argument('--maven-version-file', type=pathlib.Path, required=True)
    parser.add_argument('--exit-code', type=int, required=True)
    parser.add_argument('--suite', action='append', required=True, help='layer=fully.qualified.TestClass; repeatable')
    parser.add_argument('--trace', type=pathlib.Path, help='require linked Java methods for selected suites')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args(argv)
    required = {}
    attributed = set()
    for item in args.suite:
        layer, separator, name = item.partition('=')
        if not separator or layer not in LAYERS or not name:
            parser.error('--suite must be recognized-layer=fully.qualified.TestClass')
        if name in attributed:
            parser.error('--suite must be unique across layers')
        attributed.add(name)
        required.setdefault(layer, []).append(name)
    try:
        trace_methods = read_trace_methods(args.trace) if args.trace else None
        report = build_report(args.reports, head=args.head, branch=args.branch,
                              java_version=args.java_version_file.read_text(encoding='utf-8'),
                              maven_version=args.maven_version_file.read_text(encoding='utf-8'),
                              exit_code=args.exit_code, required_suites=required, trace_methods=trace_methods)
    except OSError as exc:
        print('Missing contract evidence input: ' + exc.__class__.__name__, file=sys.stderr)
        return 1
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': report['status'], 'head': report['head'], 'layers': report['layers']}))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
