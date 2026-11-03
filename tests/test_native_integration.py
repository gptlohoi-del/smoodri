"""Source-level checks to prevent accidental replacement of the PiP APK by web-only shell."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parent.parent
BASE=ROOT/'android/app/src/main'
class NativeIntegrationTests(unittest.TestCase):
    def test_native_files_and_manifest(self):
        for name in ('MainActivity.java','DriveMonitorService.java','NativePipView.java'):
            self.assertTrue((BASE/'java/org/smoodri/app'/name).is_file())
        manifest=(BASE/'AndroidManifest.xml').read_text()
        for mark in ('supportsPictureInPicture="true"','FOREGROUND_SERVICE_LOCATION','android:foregroundServiceType="location"','DriveMonitorService'):
            self.assertIn(mark,manifest)
    def test_web_bridge_and_pip_button(self):
        activity=(BASE/'java/org/smoodri/app/MainActivity.java').read_text()
        js=(ROOT/'app.js').read_text()
        html=(ROOT/'index.html').read_text()
        for name in ('SmooDriAndroid','enterPip','ACTION_START','onNativeSample','DriveMonitorService'):
            self.assertIn(name,activity)
        for name in ('SmooDriAndroid.start','SmooDriAndroid.stop','SmooDriAndroid.updateConfig','SmooDriAndroid.openFloating','onNativeSample(sample)'):
            self.assertIn(name,js)
        self.assertIn('id="btnNativePip"',html)
        self.assertIn('src="./ota.js"',html)
    def test_apk_bundles_ota_web_files(self):
        gradle=(ROOT/'android/app/build.gradle').read_text()
        for name in ('ota.js','version.json','sw.js','app.js'):
            self.assertIn(name,gradle)
if __name__=='__main__':unittest.main()
