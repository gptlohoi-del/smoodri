"""Regression tests for the simplified audio-only SmooDri APK."""
from pathlib import Path
from html.parser import HTMLParser
import re
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
ANDROID=ROOT/'android/app/src/main'
JAVA=ANDROID/'java/org/smoodri/app'

class IdParser(HTMLParser):
    def __init__(self): super().__init__();self.ids=set()
    def handle_starttag(self,tag,attrs):
        self.ids.update(v for k,v in attrs if k=='id')

class AudioOnlyRegression(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.js=(ROOT/'app.js').read_text()
        cls.html=(ROOT/'index.html').read_text()
        cls.manifest=(ANDROID/'AndroidManifest.xml').read_text()
        cls.activity=(JAVA/'MainActivity.java').read_text()
        cls.service=(JAVA/'DriveMonitorService.java').read_text()
        cls.player=(JAVA/'NativeAlertPlayer.java').read_text()

    def test_no_pip_overlay_android_classes_or_permissions(self):
        for name in ('NativePipView.java','NativeOverlayController.java'):
            self.assertFalse((JAVA/name).exists())
        for token in ('supportsPictureInPicture','SYSTEM_ALERT_WINDOW','ACTION_MANAGE_OVERLAY_PERMISSION'):
            self.assertNotIn(token,self.manifest+self.activity)
        for token in ('enterPictureInPictureMode','openFloating','ACTION_OVERLAY_SHOW','TYPE_APPLICATION_OVERLAY'):
            self.assertNotIn(token,self.activity+self.service)

    def test_audio_ui_and_background_control(self):
        self.assertIn('id="btnBackgroundAudio"',self.html)
        self.assertIn('id="btnTestBeepHigh"',self.html)
        self.assertIn('id="btnTestPothole"',self.html)
        self.assertIn('id="chkSoundEnabled"',self.html)
        self.assertIn('goBackground()',self.js)
        self.assertIn('moveTaskToBack(true)',self.activity)
        self.assertNotIn('id="selFloatingMode"',self.html)
        self.assertNotIn('id="pipSettingsPreview"',self.html)
        self.assertNotIn('id="btnNativePip"',self.html)

    def test_native_sensor_does_not_require_gps(self):
        self.assertIn('boolean accelerometerReady=false',self.service)
        self.assertIn('ready=true;',self.service)
        self.assertNotIn('if (!hasLocationPermission() || !locationEnabled())',self.service)
        self.assertIn('if (hasLocationPermission()',self.service)
        self.assertIn('FOREGROUND_SERVICE_SPECIAL_USE',self.manifest)
        self.assertIn('foregroundServiceType="specialUse"',self.manifest)
        self.assertNotIn('FOREGROUND_SERVICE_LOCATION',self.manifest)

    def test_uses_single_native_service_in_background(self):
        self.assertIn('setUiVisible(true)',self.activity)
        self.assertIn('setUiVisible(false)',self.activity)
        self.assertIn('if(!uiVisible && configuration.optBoolean("soundEnabled",true))',self.service)
        self.assertIn("document.visibilityState !== 'visible'",self.js)
        self.assertNotIn('if (IS_NATIVE_ANDROID || !config.soundEnabled) return;',self.js)
        self.assertIn('config.soundEnabled',self.js)

    def test_web_audio_matches_native_timing_and_pitch(self):
        for token in ('frequency = 1550','(i % 2) * 350, 60, i * 105',
                      'tone(850, 95, 0','tone(600, 95, 135'):
            self.assertIn(token,self.js)
        for token in ('1550+(note%2)*350','beepMs=pothole?95:60',
                      'gapStart=pothole?135:105','note==0?850:600','AudioTrack.MODE_STATIC'):
            self.assertIn(token,self.player)
        self.assertIn('configuration.optString("waveType","square")',self.service)
        self.assertIn('configuration.optString("potholeWave","sawtooth")',self.service)
        self.assertIn('alarm.play(kind,wave,volume)',self.service)

    def test_audio_previews_always_use_original_web_audio(self):
        self.assertIn('function previewSmooDriSound(kind, fallback)',self.js)
        self.assertRegex(self.js,r'function previewSmooDriSound\(kind, fallback\)\s*\{[^}]*fallback\(\);')
        self.assertIn('initAudioContext(); // Mở audio ngay trong thao tác chạm',self.js)
        self.assertIn("$('btnTestBeepHigh')",self.js)
        self.assertIn("$('btnTestPothole')",self.js)

    def test_settings_and_ota_preserved(self):
        for key in ('NUMERIC_BASE','NUMERIC_RANGES','rangeValid','smoodri_cfg_v8','initDirectUiEditor'):
            self.assertIn(key,self.js)
        for button in ('btnStart','btnSettings','uiEditPanel','rangeAdvanced','btnCheckOta'):
            self.assertIn(f'id="{button}"',self.html)
        self.assertIn('src="./ota.js"',self.html)
        self.assertTrue((ROOT/'.github/workflows/deploy-web-ota.yml').exists())
        self.assertTrue((ROOT/'.github/workflows/build-apk.yml').exists())

    def test_all_js_element_ids_exist(self):
        parser=IdParser();parser.feed(self.html)
        needed=set(re.findall(r"\$\('([^']+)'\)",self.js))
        self.assertEqual(needed-parser.ids,set())

    def test_version_and_manifest(self):
        gradle=(ROOT/'android/app/build.gradle').read_text()
        self.assertIn("versionName '1.0.10'",gradle)
        self.assertIn('result.put("version","1.0.10")',self.activity)
        self.assertIn('android:name=".DriveMonitorService"',self.manifest)


    def test_mode_static_plays_after_initial_no_data_state(self):
        # This catches the original bug that the previous mock (state=1) missed.
        if not shutil.which('javac') or not shutil.which('java'):
            self.skipTest('Java tooling unavailable')
        import re as _re
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            sources={
                'android/media/AudioAttributes.java': 'package android.media;public class AudioAttributes{public static final int USAGE_MEDIA=1,CONTENT_TYPE_SONIFICATION=1;public static class Builder{public Builder setUsage(int i){return this;}public Builder setContentType(int i){return this;}public AudioAttributes build(){return new AudioAttributes();}}}',
                'android/media/AudioFormat.java': 'package android.media;public class AudioFormat{public static final int ENCODING_PCM_16BIT=2,CHANNEL_OUT_MONO=4;public static class Builder{public Builder setEncoding(int i){return this;}public Builder setSampleRate(int i){return this;}public Builder setChannelMask(int i){return this;}public AudioFormat build(){return new AudioFormat();}}}',
                'android/media/AudioTrack.java': 'package android.media;public class AudioTrack{public static final int MODE_STATIC=0,STATE_INITIALIZED=1,STATE_NO_STATIC_DATA=2,PLAYSTATE_PLAYING=3,WRITE_BLOCKING=0,SUCCESS=0;private int state=STATE_NO_STATIC_DATA;public static class Builder{public Builder setAudioAttributes(AudioAttributes a){return this;}public Builder setAudioFormat(AudioFormat f){return this;}public Builder setTransferMode(int i){return this;}public Builder setBufferSizeInBytes(int i){return this;}public AudioTrack build(){return new AudioTrack();}}public int getState(){return state;}public void release(){}public int write(short[] a,int b,int c,int d){state=STATE_INITIALIZED;return c;}public int getPlayState(){return 0;}public void stop(){}public int reloadStaticData(){return SUCCESS;}public int setVolume(float v){return SUCCESS;}public void play(){if(state!=STATE_INITIALIZED)throw new IllegalStateException("no PCM");}}',
                'android/util/Log.java': 'package android.util;public class Log{public static int e(String a,String b,Throwable t){return 0;}}',
                'org/smoodri/app/AudioHarness.java': 'package org.smoodri.app;public class AudioHarness{public static void main(String[] args){NativeAlertPlayer p=new NativeAlertPlayer();if(!p.play("normal","square",70))throw new AssertionError(p.lastError());if(!p.play("normal","square",70))throw new AssertionError(p.lastError());p.release();System.out.println("STATIC_AUDIO_OK");}}',
            }
            files=[]
            for rel,code in sources.items():
                f=root/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(code);files.append(str(f))
            cmd=['javac','-encoding','UTF-8','-d',str(root/'out'),*files,str(JAVA/'NativeAlertPlayer.java')]
            compile_result=subprocess.run(cmd,capture_output=True,text=True)
            self.assertEqual(compile_result.returncode,0,compile_result.stderr)
            run=subprocess.run(['java','-cp',str(root/'out'),'org.smoodri.app.AudioHarness'],capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stderr)
            self.assertIn('STATIC_AUDIO_OK',run.stdout)

    def test_java_pcm_player_compiles_with_android_api_stubs(self):
        if not shutil.which('javac'):self.skipTest('javac unavailable')
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            sources={
              'android/media/AudioAttributes.java': 'package android.media;public class AudioAttributes{public static final int USAGE_MEDIA=1,CONTENT_TYPE_SONIFICATION=1;public static class Builder{public Builder setUsage(int i){return this;}public Builder setContentType(int i){return this;}public AudioAttributes build(){return new AudioAttributes();}}}',
              'android/media/AudioFormat.java':'package android.media;public class AudioFormat{public static final int ENCODING_PCM_16BIT=2,CHANNEL_OUT_MONO=4;public static class Builder{public Builder setEncoding(int i){return this;}public Builder setSampleRate(int i){return this;}public Builder setChannelMask(int i){return this;}public AudioFormat build(){return new AudioFormat();}}}',
              'android/media/AudioTrack.java':'package android.media;public class AudioTrack{public static final int MODE_STATIC=0,STATE_INITIALIZED=1,STATE_NO_STATIC_DATA=2,PLAYSTATE_PLAYING=3,WRITE_BLOCKING=0,SUCCESS=0;public static class Builder{public Builder setAudioAttributes(AudioAttributes a){return this;}public Builder setAudioFormat(AudioFormat f){return this;}public Builder setTransferMode(int i){return this;}public Builder setBufferSizeInBytes(int i){return this;}public AudioTrack build(){return new AudioTrack();}}private int state=STATE_NO_STATIC_DATA;public int getState(){return state;}public void release(){}public int write(short[] a,int b,int c,int d){state=STATE_INITIALIZED;return c;}public int getPlayState(){return 3;}public void stop(){}public int reloadStaticData(){return 0;}public int setVolume(float v){return 0;}public void play(){}}',
              'android/util/Log.java':'package android.util;public class Log{public static int e(String a,String b,Throwable t){return 0;}}'
            }
            files=[]
            for rel,src in sources.items():
                f=root/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(src);files.append(str(f))
            cmd=['javac','-encoding','UTF-8','-d',str(root/'out'),*files,str(JAVA/'NativeAlertPlayer.java')]
            result=subprocess.run(cmd,capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)

if __name__=='__main__':unittest.main()
