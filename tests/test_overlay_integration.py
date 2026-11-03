"""Dual-mode overlay/PiP source checks, including the Android permission and cleanup."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
ANDROID=ROOT/'android/app/src/main/java/org/smoodri/app'
class DualFloatingMode(unittest.TestCase):
    def test_overlay_permission_and_service(self):
        manifest=(ROOT/'android/app/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android.permission.SYSTEM_ALERT_WINDOW',manifest)
        main=(ANDROID/'MainActivity.java').read_text()
        service=(ANDROID/'DriveMonitorService.java').read_text()
        overlay=(ANDROID/'NativeOverlayController.java').read_text()
        for item in ('Settings.canDrawOverlays','Settings.ACTION_MANAGE_OVERLAY_PERMISSION','requestOverlayPermission',
                     'openFloating','overlayGeometry','ACTION_OVERLAY_SHOW','ACTION_OVERLAY_HIDE'):
            self.assertIn(item,main)
        for item in ('new NativeOverlayController(this)','overlay.show()','overlay.hide()','void stopFromOverlay()'):
            self.assertIn(item,service)
        for item in ('TYPE_APPLICATION_OVERLAY','Settings.canDrawOverlays','WindowManager','enableDrag','enableResize',
                     'handler.removeCallbacks(frameUpdater)','service.stopFromOverlay()'):
            self.assertIn(item,overlay)
    def test_modes_are_mutually_exclusive(self):
        main=(ANDROID/'MainActivity.java').read_text()
        self.assertIn('"pip".equals(floatingMode)',main)
        self.assertIn('"overlay".equals(floatingMode)',main)
        self.assertIn('builder.setAutoEnterEnabled("pip".equals(floatingMode)',main)
        self.assertIn('sendService(DriveMonitorService.ACTION_OVERLAY_SHOW,null)',main)
        self.assertIn('if(isInPictureInPictureMode())',main)
    def test_web_settings_have_each_mode_and_shared_metrics(self):
        html=(ROOT/'index.html').read_text()
        js=(ROOT/'app.js').read_text()
        for identifier in ('selFloatingMode','rngOverlayWidth','rngOverlayHeight','btnOverlayPermission','overlaySizeOptions'):
            self.assertIn('id="'+identifier+'"',html)
        for item in ("floatingMode:'pip'", 'syncFloatingSettings()', 'bindFloatingSettings()',
                     'SmooDriAndroid.openFloating()', 'overlayDragRevision', "overlayWidth:{label:","overlayHeight:{label:"):
            self.assertIn(item,js)
        for val in ('pipSpeedFontSize','pipGaFontSize','pipPhanhFontSize','pipCuaFontSize','pipXocFontSize','pipGpsFontSize'):
            self.assertIn(val,js)
if __name__ == '__main__':unittest.main()
