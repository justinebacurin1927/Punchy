#!/usr/bin/env python3
"""Verify real 0.1.1 -> 0.1.2 preference preservation in the isolated emulator."""
from pathlib import Path
import json
import shlex
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SERIAL = "emulator-5556"


def adb(*args, text=None):
    return subprocess.check_output(["adb", "-s", SERIAL, *args], input=text, text=True, timeout=40)


def shell(*args, text=None):
    return adb("shell", shlex.join(args), text=text)


assert "Punchy_API_34" in adb("emu", "avd", "name"), "Only the isolated Punchy emulator is authorized."
assert shell("getprop", "ro.build.version.sdk").strip() == "34"
old = ROOT / "dist/Punchy-0.1.1-debug.apk"
new = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
assert old.exists() and new.exists(), "Both APKs are required."
shell("am", "force-stop", "dev.jaycee.punchy")
adb("install", "-r", "-d", str(old))
shell("am", "force-stop", "dev.jaycee.punchy")
values = {"x": 9, "y": 4, "stroke": 3, "height": 30, "width": 123, "music_width": 125, "music_height": 41}
xml = '<?xml version="1.0" encoding="utf-8"?><map><boolean name="enabled" value="false"/><boolean name="all_apps" value="false"/>'
xml += ''.join(f'<int name="{key}" value="{value}"/>' for key, value in values.items()) + '</map>\n'
shell("run-as", "dev.jaycee.punchy", "sh", "-c", "mkdir -p shared_prefs && cat > shared_prefs/punchy.xml", text=xml)
adb("install", "-r", str(new))
shell("am", "start", "-W", "-n", "dev.jaycee.punchy/.MainActivity")
actual_xml = shell("run-as", "dev.jaycee.punchy", "cat", "shared_prefs/punchy.xml")
actual = {item.attrib['name']: int(item.attrib['value']) for item in ET.fromstring(actual_xml).findall('int')}
assert all(actual.get(key) == value for key, value in values.items()), (actual, values)
booleans = {item.attrib['name']: item.attrib['value'] for item in ET.fromstring(actual_xml).findall('boolean')}
assert booleans.get('all_apps') == 'false', 'An explicitly saved source filter must survive the update.'
version = shell("dumpsys", "package", "dev.jaycee.punchy")
assert "versionCode=3 " in version
report = {"date": "2026-11-23", "emulator": "Punchy_API_34", "from": "0.1.1", "to": "0.1.2",
          "preserved": values, "explicit_all_apps_filter_preserved": True, "version_code": 3, "passed": True}
out = ROOT / 'docs/verification/v0.1.2/upgrade-results.json'
out.write_text(json.dumps(report, indent=2) + '\n')
print("PASS: Installing 0.1.2 over 0.1.1 preserves existing X/Y, size and thickness values.")
