"""Check unrestricted advanced ranges and native PiP font handoff."""
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parent.parent

class UnrestrictedRanges(unittest.TestCase):
    def test_no_hidden_guard_in_advanced_editor(self):
        js=(ROOT/'app.js').read_text()
        html=(ROOT/'index.html').read_text()
        self.assertNotRegex(js,r'safeMin\s*:[-\d.]|safeMax\s*:[-\d.]')
        self.assertIn('function rangeValid(',js)
        self.assertNotIn('base.safeMin',js)
        self.assertNotIn('base.safeMax',js)
        self.assertNotIn('Giới hạn bảo vệ:',js)
        self.assertNotIn('vượt giới hạn bảo vệ',html)
        self.assertIn('btnResetCurrentRange',html)
        self.assertIn('btnResetAllRanges',html)
    def test_native_fonts_not_clamped_to_old_limit(self):
        service=(ROOT/'android/app/src/main/java/org/smoodri/app/DriveMonitorService.java').read_text()
        for name in ['pipSpeedFontSize','pipGaFontSize','pipPhanhFontSize','pipCuaFontSize','pipXocFontSize','pipGpsFontSize']:
            self.assertIn(f'pipTextSize("{name}"',service)
            self.assertNotRegex(service,r'clamp\(setting\("'+name+r'"')
        self.assertIn('private int pipTextSize(',service)
        self.assertIn('Math.max(0, setting("startDelay", 1.5))',service)
if __name__=='__main__': unittest.main()
