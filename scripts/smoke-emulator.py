#!/usr/bin/env python3
"""Integration checks in the isolated Punchy_API_34 emulator, never on a phone."""
from pathlib import Path
import json
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from PIL import Image
from io import BytesIO

ROOT = Path(__file__).resolve().parents[1]
SERIAL = sys.argv[1] if len(sys.argv) > 1 else "emulator-5556"
OUT = ROOT / "docs/verification/v0.1.1"
OUT.mkdir(parents=True, exist_ok=True)
checks = []


def adb(*args, raw=False):
    result = subprocess.run(["adb", "-s", SERIAL, *args], check=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=40)
    return result.stdout if raw else result.stdout.decode(errors="replace")


def shell(*args):
    return adb("shell", shlex.join(args))


def check(ok, message):
    if not ok:
        screenshot("failure")
        raise AssertionError(message)
    checks.append(message)
    print("PASS:", message, flush=True)


def wait_for(predicate, timeout=12):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        if predicate():
            return True
        time.sleep(.35)
    return False


def tree():
    result = shell("am", "instrument", "-w", "-r", "dev.jaycee.punchy.uiprobe/.HierarchyProbe")
    prefix = "INSTRUMENTATION_RESULT: hierarchy="
    for line in result.splitlines():
        if line.startswith(prefix):
            return ET.fromstring(line[len(prefix):])
    raise AssertionError("UI probe failed: " + result)


def node(text):
    return next((n for n in tree().iter("node") if text in (n.attrib.get("text"), n.attrib.get("description"))), None)


def tap(text):
    item = node(text)
    if item is None:
        raise AssertionError("UI control missing: " + text)
    x1, y1, x2, y2 = map(int, re.findall(r"-?\d+", item.attrib["bounds"]))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.25)


def find_scroll(text):
    for _ in range(7):
        if node(text) is not None:
            return
        shell("input", "swipe", "30", "1320", "30", "420", "250")
    raise AssertionError("Could not scroll to " + text)


def top():
    for _ in range(7):
        if node("Enable Punchy") is not None:
            return
        shell("input", "swipe", "30", "400", "30", "1350", "200")
    raise AssertionError("Could not return to the main toggle")


def screenshot(name):
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p", raw=True))


def window():
    text = shell("dumpsys", "window", "windows")
    return "Punchy download progress" in text


def indicator_width():
    dump = shell("dumpsys", "window", "windows")
    part = re.search(r"Window #\d+ Window\{[^\n]*Punchy download progress\}:(.*?)(?=\n  Window #|\Z)", dump, re.S)
    return int(re.search(r"Requested w=(\d+)", part.group(1)).group(1)) if part else 0


def indicator_pixel():
    return Image.open(BytesIO(adb("exec-out", "screencap", "-p", raw=True))).convert("RGB").getpixel((360, 48))


def click_indicator():
    shell("input", "tap", "360", "48")
    time.sleep(.4)


def music(command):
    shell("am", "start", "--activity-clear-top", "--activity-single-top", "-n",
          "dev.jaycee.punchy.testsource/.TestActivity", "--es", "music", command)
    time.sleep(.5)


def notification(progress=0, ident=1, unknown=False, cancel=False):
    shell("am", "start", "--activity-clear-top", "--activity-single-top", "-n", "dev.jaycee.punchy.testsource/.TestActivity",
          "--ei", "progress", str(progress), "--ei", "id", str(ident),
          "--ez", "unknown", str(unknown).lower(), "--ez", "cancel", str(cancel).lower())
    time.sleep(.3)


def contains(text):
    return any(text in (n.attrib.get("text", "") + "\n" + n.attrib.get("description", ""))
               for n in tree().iter("node"))


assert SERIAL.startswith("emulator-"), "This script may only change an emulator."
assert "Punchy_API_34" in adb("emu", "avd", "name"), "Only the isolated Punchy AVD is authorized."
assert shell("getprop", "ro.build.version.sdk").strip() == "34", "Android 14 is required."
adb("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"))
adb("install", "-r", str(ROOT / "tools/test-source/build/outputs/apk/debug/test-source-debug.apk"))
adb("install", "-r", str(ROOT / "tools/ui-probe/build/outputs/apk/debug/ui-probe-debug.apk"))
shell("cmd", "notification", "disallow_listener", "dev.jaycee.punchy/.DownloadListener")
shell("settings", "put", "secure", "enabled_accessibility_services", "")
shell("settings", "put", "secure", "accessibility_enabled", "0")
shell("pm", "clear", "dev.jaycee.punchy")
shell("pm", "clear", "dev.jaycee.punchy.testsource")
shell("pm", "grant", "dev.jaycee.punchy.testsource", "android.permission.POST_NOTIFICATIONS")
shell("settings", "put", "system", "font_scale", "1.0")
shell("settings", "put", "system", "user_rotation", "0")
shell("input", "keyevent", "KEYCODE_WAKEUP")
shell("wm", "dismiss-keyguard")
shell("am", "start", "-W", "-n", "dev.jaycee.punchy/.MainActivity")
screenshot("01-first-use")
tap("Enable Punchy")
check(node("Enable Punchy").attrib.get("checked") == "false", "Enabling before setup is rejected")
check(not window(), "No overlay before access is granted")

# Test grants are confined to the blank, isolated emulator. A physical phone uses Android Settings.
shell("cmd", "notification", "allow_listener", "dev.jaycee.punchy/.DownloadListener")
shell("settings", "put", "secure", "enabled_accessibility_services", "dev.jaycee.punchy/.RingService")
shell("settings", "put", "secure", "accessibility_enabled", "1")
check(wait_for(lambda: contains("Off · No progress overlay")), "Both Android services connect")
find_scroll("All progress notifications")
tap("All progress notifications")
top()
tap("Enable Punchy")
check(node("Enable Punchy").attrib.get("checked") == "true", "Master toggle enables after setup")
check(not window(), "Enabled idle state owns no overlay window")
screenshot("02-ready")

notification(45)
check(wait_for(window), "A real progress notification attaches the overlay")
check(contains("1 download · 45%"), "Notification listener reports real 45% progress")
windows = shell("dumpsys", "window", "windows")
check("ty=2032" in windows and "NOT_TOUCH_MODAL" in windows and "NOT_FOCUSABLE" in windows,
      "Interactive indicator uses the accessibility layer without taking focus or blocking outside touches")
screenshot("03-active-circle")
check(indicator_pixel() == (0, 0, 0), "Normal ring has an opaque black center")
normal_width = indicator_width()
click_indicator()
check(wait_for(lambda: contains("Download details")), "Tapping the normal ring opens download details")
check(contains("Test download 1") and contains("45%"), "Download card shows the actual filename and percentage")
check(not window(), "Collapsed indicator hides while its detail card is open")
screenshot("07-download-card")
tap("Close details")
check(wait_for(window), "Closing download details restores the indicator")
music("play")
check(wait_for(lambda: indicator_width() > normal_width), "Playing music automatically changes the circle to a wide capsule")
check(indicator_pixel() != (0, 0, 0), "Music capsule center stays clear for native artwork")
screenshot("08-music-capsule")
click_indicator()
check(wait_for(lambda: contains("Music & downloads")), "Tapping the music capsule opens the combined card")
check(contains("Synthetic music 1") and contains("Test download 1"), "Combined card includes live music metadata and download details")
screenshot("09-combined-card")
tap("Pause music")
check(wait_for(lambda: node("Play music") is not None), "Music pause action reaches the player session")
tap("Play music")
check(wait_for(lambda: node("Pause music") is not None), "Music play action reaches the player session")
tap("Next track")
check(wait_for(lambda: contains("Synthetic music 2")), "Next-track action updates music metadata")
tap("Previous track")
check(wait_for(lambda: contains("Synthetic music 1")), "Previous-track action reaches the player")
tap("Open app")
check(wait_for(lambda: contains("Download app opened")), "Open app follows the download notification's content intent")
music("stop")
check(wait_for(lambda: indicator_width() == normal_width), "Closing the music session restores the original circle")
shell("am", "start", "--activity-clear-top", "-n", "dev.jaycee.punchy/.MainActivity")
check(wait_for(lambda: contains("1 download · 45%")), "Download tracking survives music and popup transitions")
notification(25, ident=2)
check(contains("2 downloads · 35%"), "Simultaneous downloads aggregate correctly")
notification(100)
check(contains("1 download · 25%"), "Completing one item retains the other download")
notification(100, ident=2)
check(wait_for(lambda: not window()), "Final completion hold returns to an idle window-free state")

notification(52)
check(wait_for(window), "A reused download notification starts a new session")
notification(cancel=True)
check(wait_for(lambda: not window(), timeout=3), "Cancellation removes the overlay")

notification(unknown=True)
check(wait_for(window) and contains("Total unknown"), "Indeterminate download is visible without a false percentage")
screenshot("04-indeterminate")
shell("cmd", "statusbar", "add-tile", "dev.jaycee.punchy/.PunchyTile")
shell("cmd", "statusbar", "expand-settings")
time.sleep(1)
shell("cmd", "statusbar", "click-tile", "dev.jaycee.punchy/.PunchyTile")
check(wait_for(lambda: not window()), "Quick Settings can turn Punchy off")
shell("cmd", "statusbar", "click-tile", "dev.jaycee.punchy/.PunchyTile")
check(wait_for(window), "Quick Settings re-enables and rescans the existing download")
shell("cmd", "statusbar", "collapse")

tap("Adjust shape & alignment")
tap("Capsule")
check(wait_for(window), "Capsule shape and preview attach successfully")
screenshot("05-alignment-capsule")
shell("settings", "put", "system", "accelerometer_rotation", "0")
shell("settings", "put", "system", "user_rotation", "1")
check(wait_for(lambda: not window()), "Landscape hides the overlay")
shell("settings", "put", "system", "user_rotation", "0")
check(wait_for(window), "Portrait restores an active download overlay")
shell("input", "keyevent", "KEYCODE_SLEEP")
check(wait_for(lambda: not window()), "Screen off removes the overlay")
shell("input", "keyevent", "KEYCODE_WAKEUP")
shell("wm", "dismiss-keyguard")
check(wait_for(window), "Unlock restores active progress")
tap("Back to Punchy")
top()
notification(cancel=True)
check(wait_for(lambda: not window()), "No residual overlay after active notification removal")
tap("Preview on phone · 6 seconds")
check(wait_for(window), "Explicit preview renders without a download")
tap("Enable Punchy")
check(wait_for(lambda: not window()), "Master off immediately cancels preview")
shell("settings", "put", "system", "font_scale", "1.5")
shell("am", "start", "-W", "-n", "dev.jaycee.punchy/.MainActivity")
time.sleep(1)
top()
screenshot("06-large-text")
check(contains("Enable Punchy"), "Main control remains available at 150% font scale")
check(wait_for(lambda: not window()), "Off remains window-free after configuration change")
shell("settings", "put", "system", "font_scale", "1.0")
pid = shell("pidof", "dev.jaycee.punchy").strip()
logs = adb("logcat", "-d", "--pid=" + pid)
check("FATAL EXCEPTION" not in logs, "No Punchy fatal exception during the integration flow")
report = {"date": "2026-11-23", "device": "Punchy_API_34 AOSP emulator, 720×1610, Android 14",
          "physical_honor_tested": False, "checks": checks, "passed": len(checks)}
(OUT / "emulator-results.json").write_text(json.dumps(report, indent=2) + "\n")
print(f"{len(checks)} Android integration checks passed.", flush=True)
