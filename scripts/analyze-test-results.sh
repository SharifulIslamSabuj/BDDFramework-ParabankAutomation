#!/usr/bin/env bash
# scripts/analyze-test-results.sh
#
# Classifies a Gradle/TestNG + Cucumber regression run against the accepted
# known-failure baseline. Called by the GitHub Actions CI workflow as the
# final gate step.
#
# Identity model
# --------------
# Failed-scenario IDENTITY is determined from the Cucumber JSON report
# (build/reports/cucumber/cucumber-report.json), using the element-level
# `id` field (e.g. "customer-sign-in;sign-in-is-protected-against-injection-
# and-scripting-attacks;;2"). This id is derived from the Gherkin document
# (feature name + scenario name + outline row ordinal) and is therefore
# independent of TestNG's `runScenario[N]` naming, which is assigned by
# data-provider invocation order and is NOT stable: under
# `dataproviderthreadcount` > 1, the same index can be reused for different
# scenarios and indices can be assigned out of declaration order. See
# docs/KNOWN_AUT_LIMITATIONS.md and docs/CI_CD_GUIDE.md for the evidence.
#
# `uri` (the feature file path) is carried through for diagnostic display
# only — it is never used for equality matching, since one uri covers every
# scenario in a feature file and cannot identify a single scenario on its own.
#
# JUnit XML (build/test-results/test/TEST-*.xml) remains the source of truth
# for EXECUTION COUNTS (total/passed/failed) and for the production-safety
# gate (ProductionSafetyGuardTest). It is cross-checked against the Cucumber
# JSON element counts; a material disagreement between the two is treated as
# untrustworthy results, not silently resolved.
#
# Exit codes:
#   0  VALIDATED_BASELINE       — exactly the accepted 6 AUT failures (by id)
#   1  UNEXPECTED_REGRESSION    — different or additional failures, or a
#                                 safety-test failure, or a count mismatch
#   2  INFRASTRUCTURE_FAILURE   — tests executed but Cucumber suite did not run
#   3  RESULTS_UNAVAILABLE      — no JUnit XML found, OR the Cucumber JSON
#                                 report is missing/malformed/incomplete, OR
#                                 it disagrees materially with the JUnit XML
#                                 counts
#
# Environment variables consumed:
#   RESULTS_DIR          — JUnit XML directory (default: build/test-results/test)
#   CUCUMBER_JSON_PATH   — Cucumber JSON report path
#                          (default: build/reports/cucumber/cucumber-report.json)
#   GRADLE_EXIT_CODE     — original Gradle exit code captured before this step
#   TEST_ENV_LABEL       — environment label for display (defaults to TEST_ENV)
#   GITHUB_STEP_SUMMARY  — GitHub Actions summary file path (optional)

set -uo pipefail

RESULTS_DIR="${RESULTS_DIR:-build/test-results/test}"
CUCUMBER_JSON_PATH="${CUCUMBER_JSON_PATH:-build/reports/cucumber/cucumber-report.json}"

python3 - "$RESULTS_DIR" "$CUCUMBER_JSON_PATH" <<'PYEOF'
import sys, os, glob, json
import xml.etree.ElementTree as ET

results_dir        = sys.argv[1] if len(sys.argv) > 1 else 'build/test-results/test'
cucumber_json_path = sys.argv[2] if len(sys.argv) > 2 else 'build/reports/cucumber/cucumber-report.json'
gradle_exit         = os.environ.get('GRADLE_EXIT_CODE', 'unknown')
summary_path        = os.environ.get('GITHUB_STEP_SUMMARY', '')

EXPECTED_CUCUMBER = 18
EXPECTED_SAFETY   = 12

# ── Canonical known-failure baseline — Cucumber JSON element-level `id` ──────
# Derived from the Gherkin document (feature + scenario name + outline row
# ordinal); stable across execution order, thread count, and TestNG naming.
# Update this set only when a deliberate change is made to one of these six
# scenarios (name edit, outline row reorder) or to the accepted AUT-limitation
# baseline itself — see docs/KNOWN_AUT_LIMITATIONS.md.
KNOWN_FAILURE_IDS = frozenset({
    'customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;2',
    'customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;3',
    'customer-sign-in;sign-in-is-protected-against-injection-and-scripting-attacks;;4',
    'customer-account-registration;a-new-customer-can-open-a-bank-account-with-their-personal-information',
    'customer-account-registration;a-new-customer-can-open-a-bank-account-with-a-freshly-generated-profile',
    'customer-account-registration;a-customer-can-open-a-bank-account-using-details-provided-by-an-external-source',
})


def parse_xml_dir(d):
    """Returns (counts, error). counts holds Cucumber/safety totals only —
    identity is never derived from XML testcase names."""
    files = glob.glob(os.path.join(d, 'TEST-*.xml'))
    if not files:
        return None, 'no JUnit XML files found in ' + d

    cucumber = {'total': 0, 'passed': 0, 'failed': 0}
    safety   = {'total': 0, 'passed': 0, 'failed': 0}

    for f in files:
        try:
            root = ET.parse(f).getroot()
        except Exception as e:
            return None, 'XML parse error in {}: {}'.format(f, e)

        sname = root.get('name', '')
        is_cucumber = 'TestRunner' in sname
        is_safety   = 'ProductionSafetyGuardTest' in sname

        for tc in root.findall('testcase'):
            has_failure = (tc.find('failure') is not None or
                           tc.find('error')   is not None)

            if is_cucumber:
                cucumber['total'] += 1
                if has_failure:
                    cucumber['failed'] += 1
                else:
                    cucumber['passed'] += 1
            elif is_safety:
                safety['total'] += 1
                if has_failure:
                    safety['failed'] += 1
                else:
                    safety['passed'] += 1

    return {'cucumber': cucumber, 'safety': safety}, None


def load_cucumber_json(path):
    """Returns (elements, error). elements is a list of dicts:
    {id, uri, line, name, failed} — one per executable scenario element
    (Scenario or Scenario Outline row). Background elements are excluded.
    Returns (None, reason) on any condition that makes the identity source
    untrustworthy — callers must not fall back to any other identity scheme."""
    if not os.path.isfile(path):
        return None, 'Cucumber JSON report not found at {}'.format(path)

    try:
        with open(path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        return None, 'Cucumber JSON report is malformed: {}'.format(e)

    if not isinstance(data, list):
        return None, 'Cucumber JSON report has an unexpected top-level structure (expected a list of features)'

    elements = []
    for feature in data:
        if not isinstance(feature, dict):
            return None, 'Cucumber JSON report contains a non-object feature entry'
        feature_uri = feature.get('uri', '')
        for el in (feature.get('elements') or []):
            if not isinstance(el, dict) or el.get('type') != 'scenario':
                continue  # skip Background elements and anything not an executed scenario

            el_id = el.get('id')
            if not el_id:
                return None, 'a scenario element is missing its required "id" field (uri={})'.format(feature_uri)

            steps = el.get('steps') or []
            hooks = (el.get('before') or []) + (el.get('after') or [])
            statuses = [s.get('result', {}).get('status') for s in steps] + \
                       [h.get('result', {}).get('status') for h in hooks]
            failed = any(st not in ('passed', 'skipped') for st in statuses)

            elements.append({
                'id': el_id,
                'uri': feature_uri,
                'line': el.get('line'),
                'name': el.get('name', ''),
                'failed': failed,
            })

    if not elements:
        return None, 'Cucumber JSON report contains no executable scenario elements'

    return elements, None


def classify(xml_results, json_elements, json_error):
    c, s = xml_results['cucumber'], xml_results['safety']
    reasons = []

    # Safety test failures always indicate an unexpected regression.
    if s['failed'] > 0:
        reasons.append('production-safety tests FAILED ({} failures)'.format(s['failed']))

    # No Cucumber tests ran at all — an infrastructure problem, not a
    # scenario-identity problem, so this is decided before the JSON is
    # consulted.
    if c['total'] == 0:
        if s['total'] == 0:
            return 'INFRASTRUCTURE_FAILURE', 'no tests executed', {}
        if not reasons:
            return 'INFRASTRUCTURE_FAILURE', 'Cucumber suite did not execute', {}
        return 'UNEXPECTED_REGRESSION', '; '.join(reasons), {}

    # From here on, failed-scenario IDENTITY requires the Cucumber JSON.
    # Any problem with that source is reported as RESULTS_UNAVAILABLE —
    # it is never silently skipped or replaced with another identity scheme.
    if json_elements is None:
        return 'RESULTS_UNAVAILABLE', json_error, {}

    json_total  = len(json_elements)
    json_failed = [e for e in json_elements if e['failed']]
    json_passed = json_total - len(json_failed)

    if json_total != c['total'] or json_passed != c['passed'] or len(json_failed) != c['failed']:
        return ('RESULTS_UNAVAILABLE',
                 'Cucumber JSON element counts ({} total, {} passed, {} failed) '
                 'disagree with JUnit XML counts ({} total, {} passed, {} failed)'.format(
                     json_total, json_passed, len(json_failed),
                     c['total'], c['passed'], c['failed']),
                 {})

    id_lookup = {e['id']: e for e in json_elements}

    if s['total'] != EXPECTED_SAFETY:
        reasons.append('safety test count: expected {}, got {}'.format(EXPECTED_SAFETY, s['total']))

    if c['total'] != EXPECTED_CUCUMBER:
        reasons.append('Cucumber execution count: expected {}, got {}'.format(
            EXPECTED_CUCUMBER, c['total']))

    actual_ids = {e['id'] for e in json_failed}
    extra   = actual_ids - KNOWN_FAILURE_IDS
    missing = KNOWN_FAILURE_IDS - actual_ids

    if extra:
        reasons.append('unexpected failed scenario id(s): {}'.format(', '.join(sorted(extra))))
    if missing:
        reasons.append('known failure(s) now passing: {}'.format(', '.join(sorted(missing))))

    diagnostics = {
        'id_lookup': id_lookup,
        'actual_ids': actual_ids,
        'extra': extra,
        'missing': missing,
    }

    if reasons:
        return 'UNEXPECTED_REGRESSION', '; '.join(reasons), diagnostics

    return 'VALIDATED_BASELINE', None, diagnostics


# ── Parse ─────────────────────────────────────────────────────────────────────
xml_results, xml_error   = parse_xml_dir(results_dir)
json_elements, json_error = (None, None)

# ── CI metadata (available as standard GitHub Actions env vars) ───────────────
env_label  = os.environ.get('TEST_ENV_LABEL', os.environ.get('TEST_ENV', 'unknown'))
ref_name   = os.environ.get('GITHUB_REF_NAME',   'local')
event_name = os.environ.get('GITHUB_EVENT_NAME', 'manual')
run_num    = os.environ.get('GITHUB_RUN_NUMBER', '—')
sha        = (os.environ.get('GITHUB_SHA', 'local') or 'local')[:8]

if xml_results is None:
    classification = 'RESULTS_UNAVAILABLE'
    reason         = xml_error
    exit_code      = 3
    c = s = None
    diagnostics = {}
else:
    json_elements, json_error = load_cucumber_json(cucumber_json_path)
    classification, reason, diagnostics = classify(xml_results, json_elements, json_error)
    c = xml_results['cucumber']
    s = xml_results['safety']
    if classification == 'VALIDATED_BASELINE':
        exit_code = 0
    elif classification == 'INFRASTRUCTURE_FAILURE':
        exit_code = 2
    elif classification == 'RESULTS_UNAVAILABLE':
        exit_code = 3
    else:
        exit_code = 1

id_lookup = diagnostics.get('id_lookup', {})
extra_ids   = sorted(diagnostics.get('extra', []))
missing_ids = sorted(diagnostics.get('missing', []))

# ── stdout ────────────────────────────────────────────────────────────────────
print('')
print('=== CI Classification: {} ==='.format(classification))
if reason:
    print('    Reason : {}'.format(reason))
print('    Env    : {}'.format(env_label))
print('    Gradle : exit {}'.format(gradle_exit))
if s:
    print('    Safety : {}/{}'.format(s['passed'], s['total']))
if c:
    print('    Cucumber: {} total, {} passed, {} failed'.format(
        c['total'], c['passed'], c['failed']))

if id_lookup:
    print('')
    print('    Expected failed IDs ({}):'.format(len(KNOWN_FAILURE_IDS)))
    for fid in sorted(KNOWN_FAILURE_IDS):
        info = id_lookup.get(fid)
        if info:
            print('      - {}  [{}, uri={}]'.format(fid, info['name'], info['uri']))
        else:
            print('      - {}  [not present in this run]'.format(fid))
    print('    Actual failed IDs ({}):'.format(len(diagnostics.get('actual_ids', []))))
    for fid in sorted(diagnostics.get('actual_ids', [])):
        info = id_lookup.get(fid, {})
        print('      - {}  [{}, uri={}]'.format(fid, info.get('name', ''), info.get('uri', '')))
    if extra_ids:
        print('    Unexpected failed IDs:')
        for fid in extra_ids:
            info = id_lookup.get(fid, {})
            print('      - {}  [{}, uri={}]'.format(fid, info.get('name', ''), info.get('uri', '')))
    if missing_ids:
        print('    Missing expected IDs (now passing):')
        for fid in missing_ids:
            print('      - {}'.format(fid))
print('')

# ── GitHub Actions step summary ───────────────────────────────────────────────
lines = [
    '## ParaBank BDD Test Run',
    '',
    '| Property | Value |',
    '|---|---|',
    '| Environment | `{}` |'.format(env_label),
    '| Browser | Chrome (Xvfb virtual display) |',
    '| Trigger | `{}` |'.format(event_name),
    '| Branch | `{}` |'.format(ref_name),
    '| Commit | `{}` |'.format(sha),
    '| Run | #{} |'.format(run_num),
    '',
    '## Classification',
    '',
    '| Property | Value |',
    '|---|---|',
    '| **Result** | `{}` |'.format(classification),
    '| Gradle exit code | `{}` |'.format(gradle_exit),
]

if s:
    lines.append('| Safety tests | {}/{} passed |'.format(s['passed'], s['total']))
if c:
    lines += [
        '| Cucumber executions | {} |'.format(c['total']),
        '| Cucumber passed | {} |'.format(c['passed']),
        '| Cucumber failed | {} |'.format(c['failed']),
    ]
if reason:
    lines.append('| Detail | {} |'.format(reason))

if id_lookup:
    lines += [
        '',
        '### Scenario Identity (Cucumber JSON element `id`)',
        '',
        '| Status | ID | Scenario | Feature URI |',
        '|---|---|---|---|',
    ]
    actual_ids = diagnostics.get('actual_ids', set())
    for fid in sorted(KNOWN_FAILURE_IDS):
        info = id_lookup.get(fid)
        if fid in missing_ids:
            status = 'MISSING (now passing)'
        elif info:
            status = 'expected — present'
        else:
            status = 'expected — not present'
        name_val = info['name'] if info else '—'
        uri_val  = info['uri'] if info else '—'
        lines.append('| {} | `{}` | {} | `{}` |'.format(status, fid, name_val, uri_val))
    for fid in extra_ids:
        info = id_lookup.get(fid, {})
        lines.append('| UNEXPECTED failure | `{}` | {} | `{}` |'.format(
            fid, info.get('name', ''), info.get('uri', '')))

lines += [
    '',
    '### Known AUT Failures (Expected)',
    '',
    '- `sign-in-is-protected-against-injection-and-scripting-attacks` (3 example rows) — AUT-LIM-001: server does not render error element on injection probes',
    '- `a-new-customer-can-open-a-bank-account-*` (3 positive registration scenarios) — AUT-LIM-002: demo server session does not redirect after registration',
    '',
    'See [docs/KNOWN_AUT_LIMITATIONS.md](docs/KNOWN_AUT_LIMITATIONS.md) for details.',
    '',
    'Artifacts are available in the **Actions → Artifacts** panel.',
]

if summary_path:
    try:
        with open(summary_path, 'a', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
    except Exception as e:
        print('[warn] failed to write step summary: {}'.format(e))

sys.exit(exit_code)
PYEOF

exit $?
