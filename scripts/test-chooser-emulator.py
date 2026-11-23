#!/usr/bin/env python3
"""Focused chooser/plain-language UI checks; only the isolated Punchy AVD may be changed."""
from pathlib import Path
import json
import xml.etree.ElementTree as ET

helper = Path(__file__).with_name('test-glass-emulator.py')
exec(compile(helper.read_text().split('assert "Punchy_API_34"')[0], str(helper), 'exec'))
OUT = ROOT / 'docs/verification/v0.1.4';OUT.mkdir(parents=True, exist_ok=True)
checks = []
assert 'Punchy_API_34' in adb('emu','avd','name')
assert shell('getprop','ro.build.version.sdk').strip()=='34'
for apk in ['app/build/outputs/apk/debug/app-debug.apk','tools/test-source/build/outputs/apk/debug/test-source-debug.apk','tools/ui-probe/build/outputs/apk/debug/ui-probe-debug.apk']:
    adb('install','-r',str(ROOT/apk))
shell('cmd','notification','disallow_listener','dev.jaycee.punchy/.DownloadListener')
shell('settings','put','secure','enabled_accessibility_services','')
shell('settings','put','secure','accessibility_enabled','0')
shell('pm','clear','dev.jaycee.punchy')
shell('pm','clear','dev.jaycee.punchy.testsource')
shell('pm','grant','dev.jaycee.punchy.testsource','android.permission.POST_NOTIFICATIONS')
shell('settings','put','system','font_scale','1.0')
shell('input','keyevent','KEYCODE_WAKEUP');shell('wm','dismiss-keyguard')
shell('cmd','notification','allow_listener','dev.jaycee.punchy/.DownloadListener')
shell('settings','put','secure','enabled_accessibility_services','dev.jaycee.punchy/.RingService')
shell('settings','put','secure','accessibility_enabled','1')

def main():
    shell('am','start','-W','--activity-clear-top','-n','dev.jaycee.punchy/.MainActivity')
    assert wait_for(lambda: node('Enable Punchy') is not None)

def saved_set(key):
    xml=shell('run-as','dev.jaycee.punchy','cat','shared_prefs/punchy.xml')
    root=ET.fromstring(xml)
    entry=next((x for x in root.findall('set') if x.get('name')==key),None)
    return None if entry is None else {x.text for x in entry.findall('string')}

def close_keyboard():
    shell('input','keyevent','KEYCODE_BACK');time.sleep(.3)

def search(value):
    tap('Search apps')
    shell('input','keycombination','KEYCODE_CTRL_LEFT','KEYCODE_A')
    shell('input','keyevent','KEYCODE_DEL')
    if value:shell('input','text',value)
    time.sleep(.3)

def open_picker(label='Music capsule apps'):
    find_scroll(label);tap(label);assert wait_for(lambda: node('Search apps') is not None)

def no_technical_text():
    texts='\n'.join(n.get('text','') for n in tree().iter('node'))
    return all(s not in texts for s in ['com.shaiban','dev.jaycee.punchy.testsource','progress=','Integer:','category=','summary=false','Live updates received'])

main()
check(wait_for(lambda: node('Off · No progress overlay') is not None), 'Main screen opens with both Android settings connected')
if node('Enable Punchy').get('checked')!='true':tap('Enable Punchy')
open_picker()
check(node('Cancel') is not None and node('Save') is not None, 'Rounded chooser has explicit Cancel and Save actions')
for label in ['Cancel','Save']:
    a,b,c,d=map(int,re.findall(r'-?\d+',node(label).get('bounds')))
    check(c-a>=96 and d-b>=96, label+' keeps a fully visible 48dp touch target')
shot('01-music-chooser')
search('Punchy')
check(node('Punchy test source') is not None and node('Calendar') is None, 'Search narrows apps by name')
tap('Punchy test source')
check(node('Punchy test source').get('checked')=='true', 'App rows expose a real checked selection state')
search('Calendar')
check(node('1 app selected') is not None, 'Selection count includes choices hidden by search')
close_keyboard();tap('Cancel')
check(saved_set('music_apps') is None, 'Cancel leaves original music defaults untouched')
open_picker();search('Punchy')
check(node('Punchy test source').get('checked')=='false', 'Canceled draft does not reappear as a saved selection')
tap('Punchy test source');close_keyboard();tap('Save')
check('dev.jaycee.punchy.testsource' in saved_set('music_apps'), 'Save persists the chosen music app')
check('com.shaiban.audioplayer.mplayer' in saved_set('music_apps'), 'Save preserves selected apps absent from the installed list')
open_picker();search('Punchy')
check(node('Punchy test source').get('checked')=='true', 'Reopened chooser reflects saved choices')
tap('Punchy test source');close_keyboard();shell('input','keyevent','KEYCODE_BACK');time.sleep(.5)
check('dev.jaycee.punchy.testsource' in saved_set('music_apps'), 'Back discards edits without saving')
open_picker();search('NothingMatchesZZZ')
check(node('No apps found. Try another name.') is not None, 'An empty search explains how to recover')
close_keyboard();shell('input','tap','4','800');time.sleep(.5)
check(node('Search apps') is None and 'dev.jaycee.punchy.testsource' in saved_set('music_apps'), 'Outside dismissal leaves selections unchanged')

find_scroll('All progress notifications');tap('All progress notifications')
open_picker('Choose download apps');search('Punchy');tap('Punchy test source');close_keyboard();tap('Save')
check('dev.jaycee.punchy.testsource' in saved_set('apps'), 'Download apps use the same chooser and save to their own selection')
check('dev.jaycee.punchy.testsource' in saved_set('music_apps'), 'Changing download apps leaves music choices intact')
main();find_scroll('Music capsule apps');shot('02-settings-no-footer')
shell('input','swipe','30','1300','30','300','300');time.sleep(.3)
check(node('Punchy has no network access.') is None, 'Unnecessary network footer is removed')
find_scroll('Download detection');tap('Download detection')
check(wait_for(lambda: node('Ready for downloads') is not None), 'Connected idle state uses a plain Ready for downloads heading')
check(no_technical_text(), 'Normal status view hides package names and notification field jargon')
shot('03-simple-idle-status')
source('--ei','progress','42')
assert wait_for(lambda: node('Download found') is not None)
check(no_technical_text(), 'Live download status stays plain-language')
shot('04-simple-download-status')
tap('Copy detection report');tap('Back to Punchy');open_picker();tap('Search apps')
shell('input','keyevent','KEYCODE_PASTE');time.sleep(.4)
clipboard=node('Search apps').get('text','')
check('progress=Integer:42' in clipboard and 'dev.jaycee.punchy.testsource' in clipboard,
      'Explicitly copied help report retains useful technical progress details')
check('Test download 1' not in clipboard, 'Copied technical report still excludes filenames')
close_keyboard();tap('Cancel')

shell('settings','put','system','font_scale','1.5');time.sleep(.6)
main();open_picker();search('Punchy')
dump=shell('dumpsys','window','windows')
ime=re.search(r'Window #\d+ Window\{[^\n]*InputMethod\}:(.*?)(?=\n  Window #|\Z)',dump,re.S)
assert ime is not None and 'isVisible=true' in ime.group(1), 'Expected a visible keyboard'
region=re.search(r'touchable region=SkRegion\(\(\d+,(\d+),',ime.group(1))
assert region is not None, 'Keyboard top boundary unavailable'
keyboard_top=int(region.group(1))
for label in ['Save','Cancel']:
    entry=node(label);assert entry is not None,label
    a,b,c,d=map(int,re.findall(r'-?\d+',entry.get('bounds')))
    check(d<=keyboard_top and d-b>=96,label+' stays fully above the keyboard at 150% text size')
shot('05-chooser-large-text-keyboard')
close_keyboard();shot('06-chooser-large-text');tap('Cancel')
shell('settings','put','system','font_scale','1.0')
main();find_scroll('Download detection');tap('Download detection')
source('--ez','cancel','true');tap('Back to Punchy');main()
tap('Enable Punchy');find_scroll('Download detection');tap('Download detection')
check(node('Punchy is off') is not None and no_technical_text(), 'Off state gives a simple next step instead of a technical dump')
shot('07-simple-off-status')
pid=shell('pidof','dev.jaycee.punchy').strip()
logs=adb('logcat','-d','--pid='+pid)
check('FATAL EXCEPTION' not in logs and 'WindowLeaked' not in logs, 'No app crash or leaked chooser window during the focused flow')
(OUT/'chooser-results.json').write_text(json.dumps({'date':'2026-11-24','device':'Punchy_API_34 Android14 720×1610','physical_honor_tested':False,'checks':checks,'passed':len(checks)},indent=2)+'\n')
print(len(checks),'chooser and plain-language checks passed.')
