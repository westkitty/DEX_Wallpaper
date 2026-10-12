#!/usr/bin/env python3
"""Execute Android instrumentation and retain truthful, bounded device evidence."""
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RESULT_ROOT = Path('app/build/outputs/androidTest-results/connected')
EVIDENCE = Path('app/build/outputs/device-evidence')


def _xml_files(root):
    return sorted(root.rglob('*.xml')) if root.exists() else []


def _count(root):
    if root.tag not in ('testsuite', 'testsuites'):
        raise ValueError(f'unrecognized test-report root {root.tag!r}')
    if root.get('tests') is not None:
        return int(root.get('tests')), int(root.get('failures', '0')) + int(root.get('errors', '0'))
    if root.tag == 'testsuites':
        counts = [_count(child) for child in root if child.tag in ('testsuite', 'testsuites')]
        return sum(v[0] for v in counts), sum(v[1] for v in counts)
    cases = root.findall('testcase')
    return len(cases), sum(bool(case.find('failure') is not None or case.find('error') is not None) for case in cases)


def collect_results(root, before=None):
    """Count only XML that changed during this execution, never stale reports."""
    before = before or {}
    total = failures = 0
    problems = []
    for path in _xml_files(root):
        if before.get(path.resolve()) == path.stat().st_mtime_ns:
            continue
        try:
            count, failed = _count(ET.parse(path).getroot())
            if count < 0 or failed < 0 or failed > count:
                raise ValueError('invalid test counts')
            total += count
            failures += failed
        except (ET.ParseError, ValueError, OSError) as error:
            problems.append(f'{path.name}: {type(error).__name__}')
    return total, failures, problems


def _annotation(message, level='error'):
    escaped = message.replace('%', '%25').replace('\n', '%0A').replace('\r', '%0D')
    print(f'::{level} title=Android device verification::{escaped[-6000:]}', flush=True)


def _crash_log(args, name):
    try:
        result = subprocess.run(args, capture_output=True, text=True, timeout=15)
        EVIDENCE.joinpath(name).write_text((result.stdout or result.stderr)[-50000:])
        detail = (result.stdout or result.stderr)[-2500:]
        if detail:
            _annotation(f'{name}: {detail}')
    except (subprocess.TimeoutExpired, OSError) as error:
        _annotation(f'{name} capture unavailable: {type(error).__name__}')


def main():
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    before = {p.resolve(): p.stat().st_mtime_ns for p in _xml_files(RESULT_ROOT)}
    tail = []
    command = ['./gradlew', '--no-daemon', '--console=plain', 'connectedDebugAndroidTest']
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            print(line, end='', flush=True)
            tail.append(line)
            if len(tail) > 1000:
                tail.pop(0)
        exit_code = process.wait()
    except OSError as error:
        tail.append(f'Gradle could not start: {type(error).__name__}\n')
        exit_code = 1
    (EVIDENCE / 'gradle-tail.txt').write_text(''.join(tail))
    tests, failures, parse_problems = collect_results(RESULT_ROOT, before)
    passed = exit_code == 0 and tests > 0 and failures == 0 and not parse_problems
    summary = (f'Gradle exit={exit_code}; fresh instrumentation tests={tests}; '
               f'failures/errors={failures}; malformed reports={len(parse_problems)}; '
               f'accepted={passed}.\n')
    (EVIDENCE / 'summary.txt').write_text(summary + '\n'.join(parse_problems) + '\n')
    if not passed:
        _crash_log(['adb', 'logcat', '-d', '-s', 'AndroidRuntime:E'], 'android-runtime.txt')
        _crash_log(['adb', 'logcat', '-b', 'crash', '-d'], 'crash-buffer.txt')
        if exit_code and tail:
            summary += ''.join(tail[-40:])[-4000:]
        _annotation(summary)
    else:
        _annotation(summary, 'notice')
    return 0 if passed else (exit_code or 1)


if __name__ == '__main__':
    sys.exit(main())
