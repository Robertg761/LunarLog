#!/usr/bin/env python3
"""Expose synthetic device-test failures in Actions annotations, preserving the runner exit code."""
from pathlib import Path
import xml.etree.ElementTree as ET


def annotation(level, message):
    # Workflow commands require escaping; test output must not create extra commands.
    safe = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print(f'::{level} title=Android verification::{safe}')


def main():
    tests = failures = skipped = 0
    reports = sorted(Path('app/build/outputs/androidTest-results').rglob('*.xml'))
    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError:
            annotation('warning', f'Could not parse test report: {report.name}')
            continue
        for case in root.iter('testcase'):
            tests += 1
            skipped += int(case.find('skipped') is not None)
            for failure in list(case.findall('failure')) + list(case.findall('error')):
                failures += 1
                details = failure.text or failure.get('message', '')
                annotation('error', f"{case.get('classname', '')}.{case.get('name', '')}\n{details[:3500]}")
    if tests:
        annotation('notice', f'{tests} Android tests; {failures} failures/errors; {skipped} skipped.')
    else:
        # Build/device-discovery errors produce no JUnit cases. Surface Gradle's
        # failure section rather than dumping the full build log into annotations.
        log = Path('app/build/device-verification/gradle.txt')
        text = log.read_text(errors='replace') if log.exists() else ''
        start = text.find('FAILURE:')
        details = text[start:start + 3500] if start >= 0 else 'No JUnit test cases were produced. Inspect the device-verification artifact.'
        annotation('error', details)


if __name__ == '__main__':
    main()
