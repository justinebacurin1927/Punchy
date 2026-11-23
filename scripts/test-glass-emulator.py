#!/usr/bin/env python3
"""Visual/interaction checks on the isolated AVD, after smoke-emulator.py flows."""
from pathlib import Path
import json, re, shlex, subprocess, time
import xml.etree.ElementTree as ET
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/verification/v0.1.4"
SERIAL = "emulator-5556"
checks = []

def adb(*args, raw=False):
    result = subprocess.check_output(["adb", "-s", SERIAL, *args], timeout=40)
    return result if raw else result.decode(errors="replace")

def shell(*args): return adb("shell", shlex.join(args))

def tree():
    result = shell("am", "instrument", "-w", "-r", "dev.jaycee.punchy.uiprobe/.HierarchyProbe")
    for line in result.splitlines():
        if line.startswith("INSTRUMENTATION_RESULT: hierarchy="):
            return ET.fromstring(line.split("=", 1)[1])
    raise AssertionError(result)

def node(label):
    return next((n for n in tree().iter("node") if label in (n.get("text"), n.get("description"))), None)

def tap(label):
    n = node(label)
    until=time.monotonic()+6
    while n is None and time.monotonic()<until:
        time.sleep(.2);n=node(label)
    assert n is not None, label
    x1,y1,x2,y2 = map(int, re.findall(r"-?\d+", n.get("bounds")))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2)); time.sleep(.4)

def find_scroll(label):
    for _ in range(7):
        if node(label) is not None:return
        shell("input", "swipe", "30", "1320", "30", "420", "250")
    raise AssertionError(label)

def wait_for(predicate, timeout=8):
    until=time.monotonic()+timeout
    while time.monotonic()<until:
        if predicate():return True
        time.sleep(.2)
    return False

def check(ok, label):
    assert ok, label
    checks.append(label); print("PASS:", label, flush=True)

def shot(name):
    p=OUT/(name+".png");p.write_bytes(adb("exec-out", "screencap", "-p", raw=True));return p

def source(*args):
    wait = ["-W"] if "backdrop" in args else []
    shell("am", "start", *wait, "--activity-clear-top", "--activity-single-top", "-n", "dev.jaycee.punchy.testsource/.TestActivity", *args)
    time.sleep(.5)

def open_island():
    # Font/foreground changes can consume an injected tap during the platform transition.
    for attempt in range(2):
        shell("input", "tap", "360", "48")
        until=time.monotonic()+8
        while time.monotonic()<until:
            labels=[n.get("text", "") for n in tree().iter("node")]
            if "Download details" in labels or "Music & downloads" in labels:return
            time.sleep(.2)
        if not overlay():break
    raise AssertionError("Island did not open after tapping its indicator")

def overlay(): return "Punchy download progress" in shell("dumpsys", "window", "windows")

assert "Punchy_API_34" in adb("emu", "avd", "name")
assert shell("getprop", "ro.build.version.sdk").strip()=="34"
shell("am", "start", "--activity-clear-top", "-n", "dev.jaycee.punchy/.MainActivity")
time.sleep(.5)
# A package update rebinds services; wait for an actual ready status before enabling.
until=time.monotonic()+12
while time.monotonic()<until:
    labels=[n.get("text","") for n in tree().iter("node")]
    if any(label=="Off · No progress overlay" or label.startswith("On ·") for label in labels):break
    time.sleep(.3)
else:raise AssertionError("Punchy services did not reconnect after update")
# Smoke ends off; its accessibility and notification grants remain active.
if node("Enable Punchy").get("checked")!="true":tap("Enable Punchy")
source("--ei", "progress", "42")
source("--ez", "backdrop", "true")
open_island()
check(node("Download details") is not None and node("42%") is not None, "Download-only glass card shows live progress above another app")
root=tree();x1,y1,x2,y2=map(int,re.findall(r"-?\d+",root.find("node").get("bounds")))
check(x2-x1<720 and y2-y1<900, "Expanded island is a compact inset floating window")
for label in ["Close details", "Open app"]:
    n=node(label);a,b,c,d=map(int,re.findall(r"-?\d+",n.get("bounds")))
    check(c-a>=96 and d-b>=96,label+" has at least a 48dp touch target")
blur_status=shell("wm", "disable-blur")
shell("settings", "put", "global", "disable_window_blurs", "0");time.sleep(.7)
enabled=shot("11-glass-download")
shell("settings", "put", "global", "disable_window_blurs", "1");time.sleep(.7)
disabled=shot("12-glass-fallback")
check(node("42%") is not None and node("Open app") is not None, "Blur-disabled fallback preserves legible content and app action")
if re.search(r"supported[^\n]*true", blur_status.lower()):
    region=(max(x1+12,0),max(y1+12,0),min(x2-12,720),min(y2-12,1610))
    delta=ImageChops.difference(Image.open(enabled).convert("RGB").crop(region),Image.open(disabled).convert("RGB").crop(region))
    check(delta.getbbox() is not None, "Runtime blur capability change visibly updates the glass surface")
shell("settings", "put", "global", "disable_window_blurs", "0");time.sleep(.3)
tap("Close details")
check(wait_for(overlay),"Closing the glass window restores the camera indicator")
source("--ez", "backdrop", "true", "--ez", "light", "true")
open_island()
white = shot("16-glass-white-backdrop")
frame = tree().find("node")
a,b,c,d = map(int,re.findall(r"-?\d+",frame.get("bounds")))
pixels = Image.open(white).convert("RGB")
def luminance(rgb):
    channels = [value/255 for value in rgb]
    linear = [v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in channels]
    return .2126*linear[0]+.7152*linear[1]+.0722*linear[2]
foreground = luminance((221,231,225))
contrast = min((foreground+.05)/(luminance(pixels.getpixel((x,y)))+.05)
    for x in (a+16,c-16) for y in (b+100,b+(d-b)//2,d-60))
check(contrast>=4.5,"Glass secondary text retains 4.5:1 contrast over a white backdrop")
tap("Close details")
source("--ez", "backdrop", "true")
open_island();shell("input", "tap", "360", "1450");time.sleep(.5)
check(wait_for(lambda: node("Download details") is None and overlay()),"A tap outside dismisses the floating island")
open_island();shell("input", "keyevent", "KEYCODE_BACK");time.sleep(.5)
check(wait_for(lambda: node("Download details") is None and overlay()),"Android Back dismisses the floating island")
source("--es", "music", "play")
source("--ez", "backdrop", "true")
open_island()
check(node("Music & downloads") is not None and node("Pause music") is not None,"Music and download controls share the glass card")
shot("13-glass-music")
tap("Pause music");check(node("Play music") is not None,"Paused music controls remain available inside the glass card")
tap("Play music");check(node("Pause music") is not None,"Play action still reaches the player")
shell("settings", "put", "system", "font_scale", "1.5")
time.sleep(.7)
check(node("Open app") is not None and node("Close details") is not None,"Glass card actions stay reachable at 150% font size")
shot("14-glass-large-text")
tap("Close details")
shell("settings", "put", "system", "font_scale", "1.0")
source("--es", "music", "stop")
shell("settings", "put", "global", "animator_duration_scale", "0")
open_island()
check(node("Download details") is not None,"Glass card works with system animations disabled")
tap("Close details")
shell("settings", "put", "global", "animator_duration_scale", "1")
source("--ez", "cancel", "true")
shell("am", "start", "--activity-clear-top", "-n", "dev.jaycee.punchy/.MainActivity")
find_scroll("More setup options");tap("More setup options")
find_scroll("Add Quick Settings tile")
check(node("Add Quick Settings tile") is not None,"Advanced setup actions remain reachable through the disclosure")
find_scroll("All progress notifications");tap("All progress notifications")
find_scroll("Choose download apps")
check(node("Choose download apps") is not None,"Turning off all-sources reveals individual app selection")
tap("All progress notifications")
check(node("Choose download apps") is None,"All-sources mode hides the inapplicable app chooser")
find_scroll("Download detection");tap("Download detection")
check(node("Refresh notifications") is not None and node("Copy detection report") is not None,"Redesigned diagnostics keep refresh and copy actions available")
shot("17-detection-screen")
tap("Back to Punchy")
shell("input", "keyevent", "KEYCODE_HOME")
shell("input", "swipe", "360", "1450", "360", "350", "350");time.sleep(.6)
shot("15-launcher-icon")
report={"date":"2026-11-24","device":"Punchy_API_34 Android14 720×1610 density320","blur_status":blur_status.strip(),"white_backdrop_secondary_contrast":round(contrast,2),"physical_honor_tested":False,"passed":len(checks),"checks":checks}
(OUT/"glass-results.json").write_text(json.dumps(report,indent=2)+"\n")
print(len(checks),"glass checks passed.")
