#!/usr/bin/env python3
"""Actual Accessibility-grant/lifecycle checks, only in the owned blank AVD."""
from pathlib import Path
import hashlib
import json
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SERIAL = sys.argv[1] if len(sys.argv) > 1 else 'emulator-5556'
OUT = ROOT / 'docs/verification/v0.1.5'
OUT.mkdir(parents=True, exist_ok=True)
SERVICE = 'dev.jaycee.punchy/.RingService'
OTHER = 'dev.jaycee.punchy.testsource/.PassiveAccessibilityService'
checks = []


def adb(*args, raw=False):
    result = subprocess.check_output(['adb', '-s', SERIAL, *args], timeout=40)
    return result if raw else result.decode(errors='replace')


def shell(*args):
    return adb('shell', shlex.join(args))


def shot(name):
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p', raw=True))


def check(ok, description):
    if not ok:
        shot('failure')
        raise AssertionError(description)
    checks.append(description)
    print('PASS:', description, flush=True)


def wait_for(predicate, timeout=12):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        if predicate():
            return True
        time.sleep(.25)
    return False


def tree():
    result = shell('am', 'instrument', '-w', '-r', 'dev.jaycee.punchy.uiprobe/.HierarchyProbe')
    prefix = 'INSTRUMENTATION_RESULT: hierarchy='
    for line in result.splitlines():
        if line.startswith(prefix):
            return ET.fromstring(line[len(prefix):])
    raise AssertionError('UI probe failed: ' + result)


def node(label):
    return next((n for n in tree().iter('node') if label in
                 (n.get('text'), n.get('description'))), None)


def tap(label):
    item = node(label)
    assert item is not None, 'Missing control: ' + label
    x1, y1, x2, y2 = map(int, re.findall(r'-?\d+', item.get('bounds')))
    shell('input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(.3)


def main():
    shell('am', 'start', '-W', '--activity-clear-top', '-n', 'dev.jaycee.punchy/.MainActivity')
    time.sleep(.4)
    for _ in range(8):
        if node('Enable Punchy') is not None:
            break
        shell('input', 'swipe', '30', '400', '30', '1300', '200')


def scroll_to(label):
    for _ in range(10):
        if node(label) is not None:
            return
        shell('input', 'swipe', '30', '1300', '30', '400', '220')
    raise AssertionError('Cannot scroll to ' + label)


def enabled_services():
    raw = shell('settings', 'get', 'secure', 'enabled_accessibility_services').strip().split(':')
    return [value.replace('dev.jaycee.punchy/dev.jaycee.punchy.', 'dev.jaycee.punchy/.').replace('dev.jaycee.punchy.testsource/dev.jaycee.punchy.testsource.', 'dev.jaycee.punchy.testsource/.') for value in raw]


def ring_visible():
    return 'Punchy download progress' in shell('dumpsys', 'window', 'windows')


def notification_access():
    return 'dev.jaycee.punchy/dev.jaycee.punchy.DownloadListener' in shell(
        'settings', 'get', 'secure', 'enabled_notification_listeners')


def resumed_activity():
    result = shell('dumpsys', 'activity', 'activities')
    match = re.search(r'(?:topResumedActivity=|mResumedActivity:)([^\n]+)', result)
    return match.group(1) if match else ''


def source(*extra):
    shell('am', 'start', '--activity-clear-top', '--activity-single-top', '-n',
          'dev.jaycee.punchy.testsource/.TestActivity', *extra)
    time.sleep(.5)


def restore_grant():
    # Test-only equivalent of a user choosing the Android service switch.
    # Drain package-clear broadcasts before granting, otherwise Android may reset a fresh grant.
    main()
    shell('am', 'wait-for-broadcast-idle')
    shell('settings', 'put', 'secure', 'enabled_accessibility_services', SERVICE + ':' + OTHER)
    shell('settings', 'put', 'secure', 'accessibility_enabled', '1')
    main()
    assert wait_for(lambda: node('Off · No progress overlay') is not None)


def open_card():
    assert wait_for(ring_visible), 'Expected active ring'
    # Tap the lower hit-target edge: the simulated physical camera hole rejects center taps.
    # Wait for real foreground activity; a surface can exist before its input channel is ready.
    for _ in range(4):
        shell('input', 'tap', '360', '80')
        if wait_for(lambda: '.DownloadPanelActivity' in resumed_activity(), timeout=2):
            break
    assert '.DownloadPanelActivity' in resumed_activity(), 'Ring tap did not open the island card'
    assert wait_for(lambda: node('Download details') is not None or node('Music & downloads') is not None)
    scroll_to('Turn off Accessibility')


assert SERIAL.startswith('emulator-'), 'Never run against a physical phone.'
assert 'Punchy_API_34' in adb('emu', 'avd', 'name'), 'Only the owned Punchy AVD is authorized.'
assert shell('getprop', 'ro.build.version.sdk').strip() == '34'
assert wait_for(lambda: shell('getprop', 'sys.boot_completed').strip() == '1', timeout=120), 'AVD boot did not complete.'
for package, relative in [
        ('dev.jaycee.punchy', 'app/build/outputs/apk/debug/app-debug.apk'),
        ('dev.jaycee.punchy.testsource', 'tools/test-source/build/outputs/apk/debug/test-source-debug.apk'),
        ('dev.jaycee.punchy.uiprobe', 'tools/ui-probe/build/outputs/apk/debug/ui-probe-debug.apk')]:
    apk = ROOT / relative
    installed = shell('pm', 'path', package).strip().removeprefix('package:')
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if not installed or not shell('sha256sum', installed).startswith(digest):
        adb('install', '-r', str(apk))
# Exercise the installed app while preserving Android's renderer/code caches.
# Reset all Punchy preferences and fixture processes, not unrelated system state.
shell('am', 'force-stop', 'dev.jaycee.punchy')
shell('am', 'force-stop', 'dev.jaycee.punchy.testsource')
shell('settings', 'put', 'secure', 'enabled_accessibility_services', '')
shell('settings', 'put', 'secure', 'accessibility_enabled', '0')
shell('run-as', 'dev.jaycee.punchy', 'sh', '-c',
      'rm -f shared_prefs/punchy.xml shared_prefs/punchy.xml.bak')
shell('settings', 'put', 'system', 'font_scale', '1.0')
shell('settings', 'put', 'system', 'user_rotation', '0')
shell('input', 'keyevent', 'KEYCODE_WAKEUP')
shell('wm', 'dismiss-keyguard')
shell('logcat', '-c')
shell('pm', 'grant', 'dev.jaycee.punchy.testsource', 'android.permission.POST_NOTIFICATIONS')
shell('cmd', 'notification', 'allow_listener', 'dev.jaycee.punchy/.DownloadListener')
initial_anr = shell('dumpsys', 'activity', 'lastanr')
restore_grant()
check(OTHER in enabled_services(), 'Independent companion service enabled before pause')
tap('Enable Punchy')
source('--ei', 'progress', '42')
main()
check(wait_for(ring_visible), 'Real progress notification draws a ring before pausing')
shot('01-main-active')
tap('Enable Punchy')
check(SERVICE in enabled_services(), 'Ordinary display-off switch retains Accessibility access')
check(not ring_visible(), 'Ordinary display-off switch removes the ring')
tap('Turn off Accessibility')
check(wait_for(lambda: SERVICE not in enabled_services()), 'Main control removes the actual Android Accessibility grant even while display is already off')
check(OTHER in enabled_services(), 'Self-disable leaves the independent Accessibility service enabled')
check(notification_access(), 'Accessibility pause leaves the separate notification grant enabled')
check(wait_for(lambda: node('Accessibility off · Ring paused') is not None), 'Main screen confirms off only after Android reports it disabled')
check(not ring_visible(), 'No ring remains after Accessibility pause')
bound = shell('dumpsys', 'accessibility').split('Bound services:', 1)[1].split('Enabled services:', 1)[0]
check('Punchy progress ring' not in bound and 'Punchy test companion' in bound, 'Android unbinds Punchy while retaining the other service binding')
shot('02-main-paused')
tap('Enable Accessibility')
check('com.android.settings' in resumed_activity(), 'Resume control opens Android settings')
assert wait_for(lambda: node('Accessibility') is not None), 'Accessibility settings did not render'
shot('03-android-settings')
shell('input', 'keyevent', 'KEYCODE_BACK')
main()
check(SERVICE not in enabled_services(), 'Returning from settings without enabling does not restore access')
shell('am', 'force-stop', 'dev.jaycee.punchy')
main()
check(node('Accessibility off · Ring paused') is not None and SERVICE not in enabled_services(), 'Paused status and disabled grant survive app process restart')
tap('Enable Punchy')
check(node('Enable Punchy').get('checked') == 'false' and SERVICE not in enabled_services(), 'Ordinary enable switch cannot silently regrant Accessibility')
source('--ei', 'progress', '55')
check(not ring_visible(), 'Further notifications cannot redraw while Accessibility is disabled')
restore_grant()
check(node('Enable Punchy').get('checked') == 'false', 'Explicit Android reenable does not silently start the ring')
tap('Enable Punchy')
check(wait_for(ring_visible), 'Explicit display enable resumes current download progress')
open_card()
check(node('Download details') is not None, 'Expanded download-only island includes the disable control')
pause = node('Turn off Accessibility')
x1, y1, x2, y2 = map(int, re.findall(r'-?\d+', pause.get('bounds')))
check(y2 - y1 >= 96, 'Expanded-island disable control meets the 48dp minimum target')
shot('04-download-island')
tap('Turn off Accessibility')
check(wait_for(lambda: SERVICE not in enabled_services()), 'Expanded-island control disables the actual Android service')
check(wait_for(lambda: '.DownloadPanelActivity' not in resumed_activity()), 'Expanded island closes after disable')
check(not ring_visible(), 'Closing the disabled island does not restore its ring')
check(OTHER in enabled_services() and notification_access(), 'Island pause preserves other service and notification access')
main()
restore_grant()
tap('Enable Punchy')
scroll_to('Music capsule apps')
tap('Music capsule apps')
scroll_to('Punchy test source')
tap('Punchy test source')
tap('Save')
source('--es', 'music', 'play')
open_card()
check(node('Music & downloads') is not None, 'Combined music/download island retains the disable action')
shot('05-music-island')
tap('Turn off Accessibility')
check(wait_for(lambda: SERVICE not in enabled_services()) and not ring_visible(), 'Combined-island action disables Accessibility and leaves no ring')
session = shell('dumpsys', 'media_session').split('Punchy synthetic music', 1)[1].split('queueTitle=', 1)[0]
check(re.search(r'state=(?:PLAYING\()?3\b', session) is not None, 'Disabling Punchy does not pause the source music')
main()
shell('settings', 'put', 'system', 'font_scale', '1.5')
main()
scroll_to('Enable Accessibility')
item = node('Enable Accessibility')
x1, y1, x2, y2 = map(int, re.findall(r'-?\d+', item.get('bounds')))
check(y2 - y1 >= 96 and 0 <= y1 < y2 <= 1610, 'Resume action stays reachable and full-height at 150% text')
shot('06-large-text-paused')
restore_grant()
tap('Enable Punchy')
open_card()
scroll_to('Turn off Accessibility')
shot('07-large-text-island')
tap('Turn off Accessibility')
check(wait_for(lambda: SERVICE not in enabled_services()), 'Large-text island control is reachable and actually disables the grant')
shell('settings', 'put', 'system', 'font_scale', '1.0')
logs = shell('logcat', '-d', '-s', 'AndroidRuntime:E')
check('FATAL EXCEPTION' not in logs, 'No fatal app exception during disable/restore flows')
check(initial_anr == shell('dumpsys', 'activity', 'lastanr'), 'No new app or System UI ANR during the installed-app pause/restore flow')
source('--es', 'music', 'stop', '--ez', 'cancel', 'true')
shell('settings', 'put', 'secure', 'enabled_accessibility_services', '')
shell('settings', 'put', 'secure', 'accessibility_enabled', '0')
apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
(OUT / 'accessibility-pause-results.json').write_text(json.dumps({
    'date': '2026-11-25', 'emulator': 'Punchy_API_34', 'android': 14,
    'checks': checks, 'passed': len(checks), 'physical_honor_tested': False,
    'bank_app_acceptance_tested': False, 'tested_apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
    'preference_reset': True, 'installed_apk_verified_by_hash': True,
    'renderer_cache_preserved': True, 'no_new_anr': True,
    'initial_anr_header': initial_anr.splitlines()[:5],
}, indent=2) + '\n')
print(len(checks), 'Accessibility pause checks passed.', flush=True)
