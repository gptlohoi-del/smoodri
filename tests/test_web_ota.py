"""Smoke tests for the web-only GitHub Pages OTA bundle."""
import hashlib
import importlib.util
import pathlib
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("build_web_ota", ROOT / "scripts" / "build_web_ota.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class WebOtaTests(unittest.TestCase):
    def test_generated_bundle_and_hashes(self):
        with tempfile.TemporaryDirectory() as folder:
            dest = pathlib.Path(folder) / "dist"
            result = module.build(dest, "a1b2c3d4e5f601234567890abcdeffedcba00011", "2026-11-02T00:00:00Z")
            self.assertEqual(result["version"], "a1b2c3d4e5f6")
            self.assertIn('content="a1b2c3d4e5f6"', (dest / "index.html").read_text())
            self.assertIn("smoodri-web-ota-", (dest / "sw.js").read_text())
            self.assertIn("a1b2c3d4e5f6", (dest / "sw.js").read_text())
            self.assertNotIn("__SMOODRI_VERSION__", (dest / "sw.js").read_text())
            self.assertIn('"version": "a1b2c3d4e5f6"', (dest / "version.json").read_text())
            for entry in result["files"]:
                raw = (dest / entry["path"]).read_bytes()
                self.assertEqual(len(raw), entry["bytes"])
                self.assertEqual(hashlib.sha256(raw).hexdigest(), entry["sha256"])
            self.assertFalse((dest / "android").exists())
            self.assertFalse((dest / ".github").exists())

    def test_reject_invalid_revision(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaises(ValueError):
                module.build(pathlib.Path(folder) / "out", "release/latest", "2026-11-02T00:00:00Z")

    def test_keeps_existing_web_features(self):
        html = (ROOT / "index.html").read_text()
        javascript = (ROOT / "app.js").read_text()
        for identifier in ("btnStart", "btnSettings", "uiEditPanel", "rangeAdvanced", "btnCheckOta", "otaStatus"):
            self.assertIn(f'id="{identifier}"', html)
        self.assertIn("smoodri_cfg_v8", javascript)
        self.assertIn("function startApp(", javascript)
        self.assertIn("function stopApp(", javascript)
        self.assertIn('src="./ota.js"', html)
        self.assertNotIn("navigator.serviceWorker.register('./sw.js')", javascript)


if __name__ == "__main__":
    unittest.main()
