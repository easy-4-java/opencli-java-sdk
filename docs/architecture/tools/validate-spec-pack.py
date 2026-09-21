#!/usr/bin/env python3
"""Validate this proposal pack's structure; NOT the official OpenSpec validator.

Usage: python docs/architecture/tools/validate-spec-pack.py --root . --self-test
No product code, network access, or SDK tests are executed. PyYAML, when present,
adds safe configuration parsing; otherwise YAML checks are reported as partial.
"""
from __future__ import annotations
import argparse
import csv
import io
import json
import re
import shutil
import sys
import tempfile
from pathlib import Path
from urllib.parse import unquote

try:
    import yaml
except ImportError:
    yaml = None

REQ = re.compile(r'^### Requirement:\s+(OC-[A-Z]+-\d{3})\s+(.+)$', re.M)
SCENARIO = re.compile(r'^#### Scenario:\s+(.+)$', re.M)
TASK = re.compile(r'^- \[([ xX])\] (\d+\.\d+)\s+(.+)$', re.M)
LINK = re.compile(r'\[[^\]\n]+\]\(([^)\n]+)\)')


def check(root: Path) -> dict:
    errors: list[str] = []
    warnings: list[str] = []
    counts = {'changes': 0, 'requirements': 0, 'scenarios': 0,
              'implementation_tasks': 0, 'checked_tasks': 0, 'relative_links': 0}
    all_req: set[str] = set()
    scenario_keys: set[tuple[str, str, str]] = set()
    root = root.resolve()

    def fail(message: str) -> None:
        errors.append(message)

    def read(rel: str) -> str:
        try:
            return (root / rel).read_text(encoding='utf-8')
        except (OSError, UnicodeError) as exc:
            fail(f'{rel}: unreadable: {exc.__class__.__name__}')
            return ''

    try:
        index = json.loads(read('openspec/change-index.json'))
        changes = index['changes']
        if index.get('status') != 'DRAFT':
            fail('change-index status must remain DRAFT')
    except (KeyError, TypeError, json.JSONDecodeError):
        return {'status': 'FAIL', 'errors': errors + ['Invalid change index'], 'warnings': [], 'counts': counts}
    numbers = [c.get('n') for c in changes]
    ids = [c.get('id') for c in changes]
    caps = [c.get('cap') for c in changes]
    for field, values in [('number', numbers), ('change-id', ids), ('capability', caps)]:
        if len(set(values)) != len(values):
            fail(f'duplicate {field}')
    if len(changes) != 10:
        fail('This pack is expected to contain exactly 10 changes')
    graph = {c['n']: c.get('deps', []) for c in changes}
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(n: str) -> None:
        if n in visiting:
            fail(f'dependency cycle: {n}')
            return
        if n in visited:
            return
        visiting.add(n)
        for d in graph.get(n, []):
            if d not in graph:
                fail(f'{n}: unknown dependency {d}')
            else:
                visit(d)
        visiting.remove(n)
        visited.add(n)

    for n in graph:
        visit(n)
    counts['changes'] = len(changes)

    for c in changes:
        p = f"openspec/changes/{c['id']}"
        for fname in ['proposal.md', 'design.md', 'tasks.md', '.openspec.yaml', f"specs/{c['cap']}/spec.md"]:
            if not (root / p / fname).is_file():
                fail(f'{p}/{fname}: missing required artifact')
        prop = read(p + '/proposal.md')
        for h in ['## Why', '## What Changes', '## Capabilities', '## Impact']:
            if h not in prop:
                fail(f'{p}/proposal.md: missing {h}')
        if 'DRAFT' not in prop or 'NOT_STARTED' not in prop:
            fail(f'{p}/proposal.md: draft/not-started status missing')
        design = read(p + '/design.md')
        for h in ['## Context', '## Decisions', '## Risks / Trade-offs', '## Migration plan', '## Validation strategy']:
            if h not in design:
                fail(f'{p}/design.md: missing {h}')
        text = read(p + f"/specs/{c['cap']}/spec.md")
        if '## ADDED Requirements' not in text:
            fail(f'{p}: missing ADDED Requirements')
        requirements = list(REQ.finditer(text))
        if not requirements:
            fail(f'{p}: no valid requirements')
        for i, m in enumerate(requirements):
            rid, title = m.groups()
            if rid in all_req:
                fail(f'duplicate requirement ID {rid}')
            all_req.add(rid)
            body = text[m.end():requirements[i + 1].start() if i + 1 < len(requirements) else len(text)]
            scenarios = list(SCENARIO.finditer(body))
            statement = body[:scenarios[0].start()] if scenarios else body
            if not re.search(r'\b(SHALL|MUST)\b', statement):
                fail(f'{rid}: no normative SHALL/MUST statement')
            if not scenarios:
                fail(f'{rid}: no valid four-level Scenario')
            for j, s in enumerate(scenarios):
                name = s.group(1).strip()
                segment = body[s.end():scenarios[j + 1].start() if j + 1 < len(scenarios) else len(body)]
                key = (c['n'], rid, name)
                if key in scenario_keys:
                    fail(f'{rid}: duplicate scenario {name}')
                scenario_keys.add(key)
                for label in ['WHEN', 'THEN']:
                    if not re.search(rf'^- \*\*{label}\*\*\s+\S.+', segment, re.M):
                        fail(f'{rid}/{name}: missing substantive {label}')
                counts['scenarios'] += 1
            counts['requirements'] += 1
        tasktext = read(p + '/tasks.md')
        tasks = TASK.findall(tasktext)
        if not tasks:
            fail(f'{p}/tasks.md: no numbered implementation tasks')
        if len(set(t[1] for t in tasks)) != len(tasks):
            fail(f'{p}/tasks.md: duplicate task numbers')
        counts['implementation_tasks'] += len(tasks)
        checked = sum(t[0].lower() == 'x' for t in tasks)
        counts['checked_tasks'] += checked
        if checked:
            fail(f'{p}/tasks.md: unimplemented tasks are checked')
        cfg = read(p + '/.openspec.yaml')
        if yaml:
            try:
                obj = yaml.safe_load(cfg)
                if not isinstance(obj, dict) or obj.get('schema') != 'spec-driven':
                    fail(f'{p}/.openspec.yaml: wrong schema')
                if str(obj.get('created')) != '2026-09-21':
                    fail(f'{p}/.openspec.yaml: unexpected creation date')
            except yaml.YAMLError:
                fail(f'{p}/.openspec.yaml: invalid YAML')
        elif 'schema: spec-driven' not in cfg:
            fail(f'{p}/.openspec.yaml: schema missing')

    if yaml:
        try:
            cfg = yaml.safe_load(read('openspec/config.yaml'))
            if cfg.get('schema') != 'spec-driven':
                fail('openspec/config.yaml: wrong schema')
        except (yaml.YAMLError, AttributeError):
            fail('openspec/config.yaml: invalid YAML')
    else:
        warnings.append('PyYAML unavailable: YAML syntax parsing not performed')

    # Verify syntax-only links; external sources are intentionally not fetched.
    for md in sorted(root.rglob('*.md')):
        content = read(str(md.relative_to(root)))
        if '\x00' in content:
            fail(f'{md.relative_to(root)}: embedded NUL')
        # Ignore example fenced code to avoid treating sample syntax as links.
        prose = re.sub(r'```.*?```', '', content, flags=re.S)
        for dest in LINK.findall(prose):
            if re.match(r'^[a-z][a-z0-9+.-]*:', dest, re.I) or dest.startswith('#'):
                continue
            dest = unquote(dest.split('#', 1)[0].split(' "', 1)[0])
            target = (md.parent / dest).resolve()
            if not target.is_relative_to(root):
                fail(f'{md.relative_to(root)}: relative link escapes pack: {dest}')
            elif not target.exists():
                fail(f'{md.relative_to(root)}: broken relative link: {dest}')
            counts['relative_links'] += 1
        if re.search(r'\b(?:TODO|TBD|FIXME)\b', prose):
            fail(f'{md.relative_to(root)}: unfinished placeholder')

    for jf in sorted(root.rglob('*.json')):
        try:
            json.loads(read(str(jf.relative_to(root))))
        except json.JSONDecodeError:
            fail(f'{jf.relative_to(root)}: invalid JSON')

    # Check requirement-to-scenario plan has no missing or fabricated rows.
    rows = list(csv.DictReader(io.StringIO(read('docs/architecture/opencli-java-sdk-acceptance-matrix.csv'))))
    rowkeys = {(r['change'], r['requirement_id'], r['scenario']) for r in rows}
    if rowkeys != scenario_keys or len(rows) != len(rowkeys):
        fail('acceptance matrix must map each scenario exactly once')
    if any(r.get('test_evidence_status') != 'PLANNED_NOT_RUN' for r in rows):
        fail('acceptance matrix must not claim implementation test execution')
    root_specs = root / 'openspec/specs'
    if root_specs.exists() and any(root_specs.rglob('spec.md')):
        fail('root authoritative specs must not be promoted before implementation')
    for src in json.loads(read('docs/architecture/opencli-java-sdk-sources.lock.json')).get('sources', []):
        if not re.fullmatch(r'[0-9a-f]{40}', src.get('ref', '')):
            fail(f"source {src.get('id')}: ref is not pinned")
        if '/blob/' + src['ref'] + '/' not in src['url']:
            fail(f"source {src.get('id')}: URL ref mismatch")

    return {'status': 'FAIL' if errors else ('PARTIAL' if warnings else 'PASS'),
            'validator': 'custom_document_structure_only',
            'official_openspec_strict': 'NOT_RUN_ENVIRONMENT',
            'sdk_tests': 'NOT_RUN', 'counts': counts, 'errors': errors, 'warnings': warnings}


def self_test(root: Path) -> list[dict]:
    """Negative mutations prove selected checks fail; NOT SDK regression tests."""
    rel = 'openspec/changes/harden-opencli-argv-contract/specs/opencli-argv-contract/spec.md'
    taskrel = 'openspec/changes/harden-opencli-argv-contract/tasks.md'
    tests = [
        ('missing_requirement_keyword', rel, lambda s: re.sub(r'\bSHALL\b', 'may', s)),
        ('wrong_scenario_heading_level', rel, lambda s: s.replace('#### Scenario:', '### Scenario:')),
        ('missing_when', rel, lambda s: s.replace('- **WHEN**', '- **GIVEN**', 1)),
        ('missing_then', rel, lambda s: s.replace('- **THEN**', '- **AND**', 1)),
        ('checked_unimplemented_task', taskrel, lambda s: s.replace('- [ ]', '- [x]', 1)),
        ('broken_relative_link', 'README-OPENCLI-SPEC-PACK.md', lambda s: s + '\n[bad](missing-file.md)\n'),
        ('dependency_cycle', 'openspec/change-index.json', lambda s: s.replace('"deps": []', '"deps": ["C02"]', 1)),
    ]
    results = []
    for name, path, transform in tests:
        with tempfile.TemporaryDirectory(prefix='opencli-spec-validator-') as td:
            target = Path(td) / 'pack'
            shutil.copytree(root, target)
            f = target / path
            f.write_text(transform(f.read_text(encoding='utf-8')), encoding='utf-8')
            result = check(target)
            results.append({'case': name, 'mutation_detected': result['status'] == 'FAIL',
                            'error_count': len(result['errors'])})
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = check(args.root)
    if args.self_test:
        tests = self_test(args.root)
        result['validator_negative_self_tests'] = tests
        if not all(t['mutation_detected'] for t in tests):
            result['status'] = 'FAIL'
            result['errors'].append('Validator self-test failed to detect a mutation')
    output = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(output, encoding='utf-8')
    print(output, end='')
    return 0 if result['status'] == 'PASS' else 1

if __name__ == '__main__':
    sys.exit(main())
