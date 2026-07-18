#!/usr/bin/env bash
# scripts/analyze-test-results.sh
#
# Classifies Gradle/TestNG JUnit XML results against the accepted known-failure baseline.
# Called by the GitHub Actions CI workflow as the final gate step.
#
# Exit codes:
#   0  VALIDATED_BASELINE       — exactly the accepted 6 AUT failures
#   1  UNEXPECTED_REGRESSION    — different or additional test failures
#   2  INFRASTRUCTURE_FAILURE   — tests executed but Cucumber suite did not run
#   3  RESULTS_UNAVAILABLE      — no JUnit XML files found
#
# Environment variables consumed:
#   GRADLE_EXIT_CODE    — original Gradle exit code captured before this step
#   TEST_ENV_LABEL      — environment label for display (defaults to TEST_ENV)
#   GITHUB_STEP_SUMMARY — GitHub Actions summary file path (optional)

set -uo pipefail

RESULTS_DIR="${RESULTS_DIR:-build/test-results/test}"

python3 - "$RESULTS_DIR" <<'PYEOF'
import sys, os, glob, re
import xml.etree.ElementTree as ET

results_dir  = sys.argv[1] if len(sys.argv) > 1 else 'build/test-results/test'
gradle_exit  = os.environ.get('GRADLE_EXIT_CODE', 'unknown')
summary_path = os.environ.get('GITHUB_STEP_SUMMARY', '')

# ── Accepted baseline — commit e118ac9 / AUT-LIM-001 and AUT-LIM-002 ─────────
KNOWN_FAILURE_INDICES = frozenset({'9', '10', '11', '13', '14', '15'})
EXPECTED_CUCUMBER     = 18
EXPECTED_SAFETY       = 12

INDEX_RE = re.compile(r'^runScenario\[(\d+)\]')


def parse_xml_dir(d):
    files = glob.glob(os.path.join(d, 'TEST-*.xml'))
    if not files:
        return None, 'no JUnit XML files found in ' + d

    cucumber = {'total': 0, 'passed': 0, 'failed': []}
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
            name        = tc.get('name', '')
            has_failure = (tc.find('failure') is not None or
                           tc.find('error')   is not None)

            if is_cucumber:
                cucumber['total'] += 1
                if has_failure:
                    cucumber['failed'].append(name)
                else:
                    cucumber['passed'] += 1
            elif is_safety:
                safety['total'] += 1
                if has_failure:
                    safety['failed'] += 1
                else:
                    safety['passed'] += 1

    return {'cucumber': cucumber, 'safety': safety}, None


def classify(r):
    c, s = r['cucumber'], r['safety']
    reasons = []

    # Safety test failures always indicate an unexpected regression
    if s['failed'] > 0:
        reasons.append('production-safety tests FAILED ({} failures)'.format(s['failed']))

    # No Cucumber tests ran
    if c['total'] == 0:
        if s['total'] == 0:
            return 'INFRASTRUCTURE_FAILURE', 'no tests executed'
        if not reasons:
            return 'INFRASTRUCTURE_FAILURE', 'Cucumber suite did not execute'
        return 'UNEXPECTED_REGRESSION', '; '.join(reasons)

    # Safety test count mismatch
    if s['total'] != EXPECTED_SAFETY:
        reasons.append('safety test count: expected {}, got {}'.format(EXPECTED_SAFETY, s['total']))

    # Cucumber execution count mismatch
    if c['total'] != EXPECTED_CUCUMBER:
        reasons.append('Cucumber execution count: expected {}, got {}'.format(
            EXPECTED_CUCUMBER, c['total']))

    # Compare actual failure set to accepted baseline
    actual_idx  = set()
    unparseable = []
    for name in c['failed']:
        m = INDEX_RE.match(name)
        if m:
            actual_idx.add(m.group(1))
        else:
            unparseable.append(name)

    extra   = actual_idx - KNOWN_FAILURE_INDICES
    missing = KNOWN_FAILURE_INDICES - actual_idx

    if extra:
        reasons.append('unexpected failures: runScenario[{}]'.format(
            ', '.join(sorted(extra, key=int))))
    if missing:
        reasons.append('known failures now passing: runScenario[{}]'.format(
            ', '.join(sorted(missing, key=int))))
    if unparseable:
        reasons.append('failures with unparseable name: {}'.format(unparseable))

    if reasons:
        return 'UNEXPECTED_REGRESSION', '; '.join(reasons)

    return 'VALIDATED_BASELINE', None


# ── Parse ─────────────────────────────────────────────────────────────────────
results, error = parse_xml_dir(results_dir)

# ── CI metadata (available as standard GitHub Actions env vars) ───────────────
env_label  = os.environ.get('TEST_ENV_LABEL', os.environ.get('TEST_ENV', 'unknown'))
ref_name   = os.environ.get('GITHUB_REF_NAME',   'local')
event_name = os.environ.get('GITHUB_EVENT_NAME', 'manual')
run_num    = os.environ.get('GITHUB_RUN_NUMBER', '—')
sha        = (os.environ.get('GITHUB_SHA', 'local') or 'local')[:8]

if results is None:
    classification = 'RESULTS_UNAVAILABLE'
    reason         = error
    exit_code      = 3
    c = s = None
else:
    classification, reason = classify(results)
    c = results['cucumber']
    s = results['safety']
    if classification == 'VALIDATED_BASELINE':
        exit_code = 0
    elif classification == 'INFRASTRUCTURE_FAILURE':
        exit_code = 2
    else:
        exit_code = 1

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
        c['total'], c['passed'], len(c['failed'])))
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
        '| Known AUT failures | {} |'.format(len(c['failed'])),
    ]
if reason:
    lines.append('| Detail | {} |'.format(reason))

lines += [
    '',
    '### Known AUT Failures (Expected)',
    '',
    '- `runScenario[9,10,11]` — AUT-LIM-001: server does not render error element on injection probes',
    '- `runScenario[13,14,15]` — AUT-LIM-002: demo server session does not redirect after registration',
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
