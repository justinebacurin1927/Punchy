#!/usr/bin/env python3
"""Music/video source regressions in the isolated AVD after the UI-flow suite."""
from pathlib import Path
import json

# Reuse the test-only ADB/UI helpers, without executing its setup or clearing any data.
helper_file = Path(__file__).with_name('smoke-emulator.py')
exec(compile(helper_file.read_text().split('assert SERIAL.startswith')[0], str(helper_file), 'exec'))
checks = []
assert 'Punchy_API_34' in adb('emu', 'avd', 'name')
assert shell('getprop', 'ro.build.version.sdk').strip() == '34'
adb('install', '-r', str(ROOT / 'tools/video-source/build/outputs/apk/debug/video-source-debug.apk'))
shell('am', 'start', '-W', '--activity-clear-top', '-n', 'dev.jaycee.punchy/.MainActivity')
check(wait_for(lambda: node('Enable Punchy') is not None), 'Punchy main screen is available for source tests')
if node('Enable Punchy').get('checked') != 'true': tap('Enable Punchy')
tap('Adjust shape & alignment');tap('Circle');tap('Stop preview');tap('Back to Punchy')
notification(37)
check(wait_for(window), 'Real download remains visible for video-source tests')
normal_width = indicator_width()

def video(command):
    shell('am', 'start', '--activity-clear-top', '--activity-single-top', '-n',
          'dev.jaycee.punchy.videosource/dev.jaycee.punchy.testsource.TestActivity', '--es', 'music', command)
    time.sleep(.7)

video('play')
check(indicator_width() == normal_width, 'Unselected video session with unknown content type cannot force the wide music shape')
check(contains('On · Watching for downloads'), 'Video playback does not claim music capsule mode')
click_indicator()
check(wait_for(lambda: contains('Download details')) and node('Pause music') is None,
      'Video-only playback keeps the download-only card without video transport controls')
tap('Close details')
check(wait_for(window), 'Download indicator survives closing the video-excluded card')
music('play')
check(wait_for(lambda: indicator_width() > normal_width), 'Selected music player still widens the indicator while a video session exists')
click_indicator()
check(wait_for(lambda: contains('Music & downloads')) and contains('Synthetic music'),
      'Combined card chooses selected music rather than the other playing video session')
tap('Close details')
music('pause')
check(wait_for(lambda: indicator_width() == normal_width), 'Paused music plus playing video returns to the normal download shape')
music('play')
find_scroll('Music capsule apps');tap('Music capsule apps');tap('Punchy test source');tap('Save')
check(wait_for(lambda: indicator_width() == normal_width), 'Deselecting a playing music source updates the shape immediately')
find_scroll('Music capsule apps');tap('Music capsule apps');tap('Punchy test source');tap('Save')
check(wait_for(lambda: indicator_width() > normal_width), 'Reselecting the existing music session restores the wide profile without restarting playback')
shell('am', 'start', '--activity-clear-top', '--activity-single-top', '-n', 'dev.jaycee.punchy.testsource/.TestActivity',
      '--es', 'music', 'play', '--ez', 'movie', 'true')
check(wait_for(lambda: indicator_width() == normal_width), 'Explicit movie-content playback is rejected even from a selected music app')
music('play')
check(wait_for(lambda: indicator_width() > normal_width), 'Music source recovers after its content type changes back from movie')
video('stop');music('stop')
notification(74)
top()
check(contains('1 download · 74%') and indicator_width() == normal_width,
      'Download progress updates normally after video and music sessions close')
find_scroll('Music capsule apps');screenshot('19-music-source-settings');tap('Music capsule apps');screenshot('20-music-source-chooser');tap('Cancel');top()
notification(cancel=True)
check(wait_for(lambda: not window()), 'Removing the download leaves no residual video/music overlay')
(OUT/'media-results.json').write_text(json.dumps({'date':'2026-11-24', 'device':'Punchy_API_34 Android14',
    'physical_youtube_tested':False, 'checks':checks, 'passed':len(checks)}, indent=2)+'\n')
print(len(checks), 'music/video checks passed.')
