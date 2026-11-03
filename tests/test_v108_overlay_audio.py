"""Regression guards for visible Overlay failures and consistent PCM audio."""
from pathlib import Path
import shutil, subprocess, tempfile, unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/org/smoodri/app'

class OverlayAudioV108(unittest.TestCase):
 def test_overlay_failure_visible_in_diagnostics(self):
  service=(JAVA/'DriveMonitorService.java').read_text()
  overlay=(JAVA/'NativeOverlayController.java').read_text()
  main=(JAVA/'MainActivity.java').read_text()
  web=(ROOT/'app.js').read_text()
  self.assertIn('String lastOverlayError()', service)
  self.assertIn('boolean overlayVisible()', service)
  self.assertIn('String lastError(){return lastError;}', overlay)
  self.assertIn('Log.e("SmooDriOverlay"',overlay)
  self.assertIn('result.put("overlayError",DriveMonitorService.lastOverlayError())',main)
  self.assertIn('d.overlayError',web)
  self.assertIn('Overlay: ${d.overlayVisible?',web)
 def test_overlay_after_background_order(self):
  main=(JAVA/'MainActivity.java').read_text()
  section=main.split('private void showOverlayInBackground()',1)[1].split('private void openFloating()',1)[0]
  self.assertLess(section.index('moveTaskToBack(true)'),section.index('ACTION_OVERLAY_SHOW'))
  self.assertIn('handler.postDelayed(',section)
  self.assertIn('Settings.canDrawOverlays(this)',section)
 def test_single_native_audio_for_all_modes(self):
  service=(JAVA/'DriveMonitorService.java').read_text()
  web=(ROOT/'app.js').read_text()
  sound=(JAVA/'NativeAlertPlayer.java').read_text()
  self.assertNotIn('import android.media.ToneGenerator;',service)
  self.assertIn('private final NativeAlertPlayer alarm',service)
  self.assertIn('configuration.optBoolean("soundEnabled",true) && severity>=100',service)
  self.assertIn('if (IS_NATIVE_ANDROID || !config.soundEnabled) return;',web)
  self.assertIn('testNativeSound',web)
  self.assertIn('AudioTrack.MODE_STATIC',sound)
  self.assertIn('Math.sin(phase)',sound)
  self.assertIn('BEEP_COUNT = 4',sound)
 def test_new_java_audio_class_compiles_with_api_stubs(self):
  if not shutil.which('javac'): self.skipTest('javac unavailable')
  with tempfile.TemporaryDirectory() as td:
   base=Path(td)
   stubs={
    'android/media/AudioAttributes.java': 'package android.media; public class AudioAttributes { public static final int USAGE_MEDIA=1, CONTENT_TYPE_SONIFICATION=1; public static class Builder { public Builder setUsage(int n){return this;} public Builder setContentType(int n){return this;} public AudioAttributes build(){return new AudioAttributes();} } }',
    'android/media/AudioFormat.java':'package android.media; public class AudioFormat {public static final int ENCODING_PCM_16BIT=2, CHANNEL_OUT_MONO=4; public static class Builder {public Builder setEncoding(int n){return this;} public Builder setSampleRate(int n){return this;} public Builder setChannelMask(int n){return this;} public AudioFormat build(){return new AudioFormat();}}}',
    'android/media/AudioTrack.java':'package android.media; public class AudioTrack {public static final int MODE_STATIC=0,STATE_INITIALIZED=1,PLAYSTATE_PLAYING=3,WRITE_BLOCKING=0; public static class Builder {public Builder setAudioAttributes(AudioAttributes a){return this;} public Builder setAudioFormat(AudioFormat f){return this;} public Builder setTransferMode(int n){return this;} public Builder setBufferSizeInBytes(int n){return this;} public AudioTrack build(){return new AudioTrack();}} public int getState(){return 1;} public void release(){} public int write(short[] b,int o,int n,int mode){return n;} public int getPlayState(){return 3;} public void stop(){} public int reloadStaticData(){return 0;} public int setVolume(float v){return 0;} public void play(){} }',
    'android/util/Log.java':'package android.util; public class Log {public static int e(String a,String b,Throwable t){return 0;}}'
   }
   sources=[]
   for rel,src in stubs.items():
    f=base/rel; f.parent.mkdir(parents=True,exist_ok=True); f.write_text(src);sources.append(str(f))
   cmd=['javac','-encoding','UTF-8','-d',str(base/'classes'),*sources,str(JAVA/'NativeAlertPlayer.java')]
   result=subprocess.run(cmd,text=True,capture_output=True)
   self.assertEqual(result.returncode,0,result.stderr)

if __name__=='__main__': unittest.main()
