"""Regression checks for speed/status/G-cup and background audio mode."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java/org/smoodri/app'
class CompactFloatingModes(unittest.TestCase):
    def test_three_mode_config_and_display(self):
        html=(ROOT/'index.html').read_text()
        js=(ROOT/'app.js').read_text()
        for key in ('pipStatusFontSize','pipCupSize'):
            self.assertIn(key,js)
            self.assertIn('id="rng'+key[0].upper()+key[1:]+'"',html)
        for field in ('speed','status','cup'):
            self.assertIn('data-pip-visible="'+field+'"',html)
        self.assertIn("const PIP_DEFAULT_VISIBLE = ['speed','status','cup']",js)
        self.assertIn('value="audio"',html)
        self.assertIn("floatingMode:'pip'",js)
        self.assertIn("raw.pipLayoutVersion===2",js)
    def test_audio_mode_never_opens_a_floating_window(self):
        activity=(JAVA/'MainActivity.java').read_text()
        self.assertIn('if("audio".equals(floatingMode))',activity)
        self.assertIn('moveTaskToBack(true)',activity)
        self.assertIn('builder.setAutoEnterEnabled("pip".equals(floatingMode)',activity)
        self.assertIn('"pip".equals(floatingMode) && Build.VERSION.SDK_INT>=26',activity)
        service=(JAVA/'DriveMonitorService.java').read_text()
        self.assertIn('MainActivity.uiForeground',service)
        self.assertIn('soundEnabled',service)
        self.assertIn('ToneGenerator',service)
        self.assertIn('startForeground(',service)
    def test_native_status_and_live_cup(self):
        service=(JAVA/'DriveMonitorService.java').read_text()
        view=(JAVA/'NativePipView.java').read_text()
        for label in ('PHANH GẤP','TĂNG GA MẠNH','VÀO CUA MẠNH','ĐƯỜNG XÓC','ÊM ÁI'):
            self.assertIn(label,service)
        for value in ('s.statusFontSize','s.cupSize','s.lateralG','s.longitudinalG','s.cupLimit'):
            self.assertIn(value,view)
        self.assertIn('visible(s,"cup")',view)
        self.assertIn('visible(s,"status")',view)
    def test_score_fully_removed_from_app_source(self):
        for path in (ROOT/'index.html', ROOT/'app.js',ROOT/'style.css',JAVA/'NativePipView.java'):
            source=path.read_text().lower()
            for forbidden in ('scorevalue','instantscore','scorerecovery','scorefont','điểm hiện tại'):
                self.assertNotIn(forbidden,source,str(path))
if __name__=='__main__': unittest.main()
