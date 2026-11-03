"""Regression guards for SmooDri 1.0.7 background, GPS and UI corrections."""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/org/smoodri/app'
class NativeReliability(unittest.TestCase):
 def test_no_duplicate_black_speed_unit(self):
  html=(ROOT/'index.html').read_text()
  self.assertIn('id="speedValue"',html)
  self.assertNotIn('class="speed-unit"',html)
 def test_native_gps_does_not_use_webview_permission(self):
  js=(ROOT/'app.js').read_text()
  service=(JAVA/'DriveMonitorService.java').read_text()
  self.assertIn('if(IS_NATIVE_ANDROID) {\n    // Native LocationManager',js)
  self.assertIn('gpsNativeSample(sample)',js)
  self.assertIn('gpsSeq',service)
  self.assertIn('gpsAgeMs',service)
  self.assertIn('gpsSpeed',service)
  self.assertIn('gpsSequence++',service)
 def test_native_fgs_ready_before_ack(self):
  activity=(JAVA/'MainActivity.java').read_text()
  service=(JAVA/'DriveMonitorService.java').read_text()
  self.assertIn('sessionRequested && running && !sessionConfirmed',activity)
  self.assertIn('boolean accelerometerReady=false',service)
  self.assertIn('ready=true;',service)
  self.assertIn('lastStartError()',activity)
 def test_audio_all_modes_and_real_test(self):
  service=(JAVA/'DriveMonitorService.java').read_text()
  js=(ROOT/'app.js').read_text()
  self.assertIn('AudioManager.STREAM_MUSIC',service)
  self.assertIn('ACTION_TEST_BEEP',service)
  self.assertIn('!MainActivity.uiForeground',service)
  self.assertIn('testNativeBeep',js)
 def test_diagnostics_and_three_modes(self):
  activity=(JAVA/'MainActivity.java').read_text()
  html=(ROOT/'index.html').read_text()
  self.assertIn('@JavascriptInterface public String diagnostics()',activity)
  self.assertIn('id="btnAndroidDiagnostics"',html)
  self.assertIn('"audio".equals(floatingMode)',activity)
  self.assertIn('"overlay".equals(floatingMode)',activity)
  self.assertIn('"pip".equals(floatingMode)',activity)
if __name__=='__main__':unittest.main()
