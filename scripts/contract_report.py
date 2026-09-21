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

LAYERS = ('enumeration', 'argv', 'protocol', 'typed-result', 'real-execution')


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


def build_report(reports_dir, *, head, branch, java_version, maven_version, exit_code, required_suites):
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
    suites = {}
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
        except (ET.ParseError, OSError, KeyError, ValueError):
            # Do not copy testcase failure bodies or captured application output.
            problems.append('invalid or duplicate Surefire report: ' + path.name)
    layers = {}
    for layer in LAYERS:
        names = list(required_suites.get(layer, []))
        counts = dict.fromkeys(('tests', 'executed', 'skipped', 'failures', 'errors'), 0)
        missing = [name for name in names if name not in suites]
        for name in names:
            for key in counts:
                counts[key] += suites.get(name, {}).get(key, 0)
        state = 'NOT_RUN'
        if names:
            state = 'PASS' if (not missing and counts['executed'] > 0 and
                               not any(counts[key] for key in ('skipped', 'failures', 'errors'))) else 'FAIL'
        layers[layer] = dict(counts, status=state, requiredSuites=names, missingSuites=missing)
        if state == 'FAIL':
            problems.append('required layer lacks passing, non-skipped execution: ' + layer)
    return {'schemaVersion': 1, 'status': 'FAIL' if problems else 'PASS',
            'head': head, 'branch': branch, 'javaVersion': java_version,
            'mavenVersion': maven_version, 'commandExitCode': exit_code,
            'layers': layers, 'problems': problems,
            'scope': 'Explicit Surefire suites only; synthetic argv probes are not live OpenCLI verification.'}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reports', type=pathlib.Path, required=True)
    parser.add_argument('--head', required=True)
    parser.add_argument('--branch', required=True)
    parser.add_argument('--java-version-file', type=pathlib.Path, required=True)
    parser.add_argument('--maven-version-file', type=pathlib.Path, required=True)
    parser.add_argument('--exit-code', type=int, required=True)
    parser.add_argument('--suite', action='append', required=True, help='layer=fully.qualified.TestClass; repeatable')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args(argv)
    required = {}
    for item in args.suite:
        layer, separator, name = item.partition('=')
        if not separator or layer not in LAYERS or not name:
            parser.error('--suite must be recognized-layer=fully.qualified.TestClass')
        if name in required.setdefault(layer, []):
            parser.error('duplicate --suite')
        required[layer].append(name)
    try:
        report = build_report(args.reports, head=args.head, branch=args.branch,
                              java_version=args.java_version_file.read_text(encoding='utf-8'),
                              maven_version=args.maven_version_file.read_text(encoding='utf-8'),
                              exit_code=args.exit_code, required_suites=required)
    except OSError as exc:
        print('Missing tool-version evidence: ' + exc.__class__.__name__, file=sys.stderr)
        return 1
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': report['status'], 'head': report['head'], 'layers': report['layers']}))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
