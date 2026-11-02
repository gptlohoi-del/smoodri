"""Guard six independently adjustable native PiP fonts, and the removal of trip score."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parent.parent
ANDROID=ROOT/'android/app/src/main/java/org/smoodri/app'
FONT_KEYS=['pipSpeedFontSize','pipGaFontSize','pipPhanhFontSize',
           'pipCuaFontSize','pipXocFontSize','pipGpsFontSize']

class PiPIndependentFonts(unittest.TestCase):
    def test_six_controls_connect_web_and_native(self):
        html=(ROOT/'index.html').read_text()
        app=(ROOT/'app.js').read_text()
        service=(ANDROID/'DriveMonitorService.java').read_text()
        view=(ANDROID/'NativePipView.java').read_text()
        for key in FONT_KEYS:
            control='rng'+key[0].upper()+key[1:]
            display='lbl'+key[0].upper()+key[1:]
            self.assertIn('id="'+control+'"',html,key)
            self.assertIn('id="'+display+'"',html,key)
            self.assertIn(key+':{label:',app,key)
            self.assertIn('"'+key+'"',service,key)
        for field in ['speedFontSize','gaFontSize','phanhFontSize',
                      'cuaFontSize','xocFontSize','gpsFontSize']:
            self.assertIn('s.'+field,view,field)
        self.assertIn('syncNativeConfig()',app)
        self.assertIn('ACTION_SETTINGS',service)
    def test_cumulative_trip_score_removed(self):
        app=(ROOT/'app.js').read_text()
        html=(ROOT/'index.html').read_text()
        service=(ANDROID/'DriveMonitorService.java').read_text()
        view=(ANDROID/'NativePipView.java').read_text()
        self.assertNotIn('tripPenalty',app)
        self.assertNotIn('tripScore',app)
        self.assertNotIn('ĐIỂM CHUYẾN ĐI</span>',html)
        self.assertNotIn('data-pip-visible="score"',html)
        self.assertNotIn('visible(s,"score")',view)
        self.assertNotIn('s.score',view)
        self.assertNotIn('private double penalty;',service)
        self.assertIn('ĐIỂM HIỆN TẠI',html)
    def test_apk_version_bumped(self):
        gradle=(ROOT/'android/app/build.gradle').read_text()
        self.assertIn('versionCode 4',gradle)
        self.assertIn("versionName '1.0.3'",gradle)
