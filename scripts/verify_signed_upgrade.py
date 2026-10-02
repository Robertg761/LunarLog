#!/usr/bin/env python3
"""Verify a production-signed in-place upgrade on a disposable emulator."""
import datetime
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.lunarlog'
NOTE = 'LunarLogUpgradeVerification'
EVIDENCE = Path('dist/upgrade-verification')


def command(*args):
    result = subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
    if result.returncode:
        raise RuntimeError(f'{args[0]} failed: {result.stdout[-2000:]}')
    return result.stdout


def adb(*args):
    return command('adb', *args)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/lunarlog-upgrade.xml')
    xml = adb('shell', 'cat', '/sdcard/lunarlog-upgrade.xml')
    (EVIDENCE / 'screen.xml').write_text(xml)
    return ET.fromstring(xml)


def bounds(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))


def find(predicate, seconds=45):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        root = tree()
        for node in root.iter('node'):
            box = bounds(node)
            if predicate(node) and len(box) == 4 and box[2] > box[0] and box[3] > box[1]:
                return node
        time.sleep(1)
    raise AssertionError('Expected UI was not found; see upgrade-verification/screen.xml')


def label(value):
    return lambda n: n.get('text') == value or n.get('content-desc') == value


def tap(node):
    left, top, right, bottom = bounds(node)
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))


def open_today():
    epoch_day = (datetime.datetime.now(datetime.timezone.utc).date() - datetime.date(1970, 1, 1)).days
    adb('shell', 'am', 'start', '-W', '-a', 'android.intent.action.VIEW', '-d',
        f'lunarlog://details/{epoch_day}', '-n', f'{PACKAGE}/.MainActivity')


def main():
    baseline, candidate, version = sys.argv[1:]
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    sdk = Path(os.environ['ANDROID_HOME'])
    signer = sorted((sdk / 'build-tools').glob('*/apksigner'), key=lambda p: tuple(int(x) for x in re.findall(r'\d+', p.parent.name)))[-1]
    certificates = []
    for apk in (baseline, candidate):
        output = command(str(signer), 'verify', '--print-certs', apk)
        digest = sorted(set(re.findall(r'certificate SHA-256 digest: (\w+)', output)))
        if not digest:
            raise AssertionError('APK has no verified signer certificate')
        certificates.append(digest)
    if certificates[0] != certificates[1]:
        raise AssertionError('Release signer differs from the published APK')
    print('Published APK and candidate signing certificates match.', flush=True)
    adb('wait-for-device')
    adb('shell', 'settings', 'put', 'system', 'time_12_24', '24')
    adb('shell', 'wm', 'size', '1080x2400')
    adb('shell', 'wm', 'density', '360')
    adb('shell', 'input', 'keyevent', '82')
    adb('install', baseline)
    adb('shell', 'am', 'start', '-W', '-n', f'{PACKAGE}/.MainActivity')
    tap(find(label('Begin Journey')))
    find(label('Settings'))
    open_today()
    tap(find(label('Add Log')))
    # Expand the old bottom sheet before selecting its horizontally scrolling tabs.
    adb('shell', 'input', 'swipe', '540', '1550', '540', '400', '400')
    for attempt in range(12):
        root = tree()
        notes = [n for n in root.iter('node') if label('Note')(n) and len(bounds(n)) == 4 and bounds(n)[2] > bounds(n)[0]]
        if notes:
            tap(notes[0])
            break
        tabs = [n for n in root.iter('node') if n.get('text', '').lower() in ('flow', 'symptom', 'mood', 'water', 'sleep', 'sleep quality', 'sex drive', 'cervical mucus', 'basal temperature') and len(bounds(n)) == 4 and bounds(n)[2] > bounds(n)[0]]
        if not tabs:
            raise AssertionError('Could not locate the logging tabs')
        box = bounds(tabs[0])
        y = (box[1] + box[3]) // 2
        adb('shell', 'input', 'swipe', '950', str(y), '120', str(y), '350')
    else:
        raise AssertionError('Could not reach the Note tab')
    tap(find(lambda n: n.get('class') == 'android.widget.EditText'))
    adb('shell', 'input', 'text', NOTE)
    adb('shell', 'input', 'keyevent', '4')
    for attempt in range(8):
        root = tree()
        save = [n for n in root.iter('node') if n.get('text', '').startswith('Save (1 item') and len(bounds(n)) == 4 and bounds(n)[2] > bounds(n)[0]]
        if save:
            tap(save[0])
            break
        adb('shell', 'input', 'swipe', '540', '1950', '540', '550', '400')
    else:
        raise AssertionError('Could not save the baseline note')
    find(lambda n: n.get('content-desc') == 'Add Log')
    find(lambda n: NOTE in n.get('text', '') or NOTE in n.get('content-desc', ''))
    # Relaunch to verify the baseline write was persisted before upgrading.
    adb('shell', 'am', 'force-stop', PACKAGE)
    open_today()
    find(lambda n: NOTE in n.get('text', '') or NOTE in n.get('content-desc', ''))
    print('Baseline note persisted; installing candidate without clearing data.', flush=True)
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('install', '-r', candidate)
    package = adb('shell', 'dumpsys', 'package', PACKAGE)
    if f'versionName={version}' not in package:
        raise AssertionError('Installed version does not match the release candidate')
    open_today()
    find(lambda n: NOTE in n.get('text', '') or NOTE in n.get('content-desc', ''))
    (EVIDENCE / 'result.txt').write_text(f'PASS: published 1.10.2 upgraded in place to {version}; signer matched and persisted note survived.\n')
    print('::notice::Signed in-place upgrade passed; saved note survived.', flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        message = str(error).replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print(f'::error title=Signed upgrade verification::{message}', flush=True)
        if EVIDENCE.exists():
            try:
                (EVIDENCE / 'logcat.txt').write_text(adb('logcat', '-d'))
                root = tree()
                labels = [n.get('text') or n.get('content-desc') for n in root.iter('node') if n.get('text') or n.get('content-desc')]
                print('::error title=Upgrade screen::' + repr(labels)[-2500:].replace('%', '%25'), flush=True)
            except Exception:
                pass
        raise
