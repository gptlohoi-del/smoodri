#!/usr/bin/env python3
"""Đóng gói trang GitHub Pages có phiên bản OTA theo Git commit.

Không sửa file nguồn; chỉ xuất dist/ để publish. Các file web cốt lõi
được SW precache và chỉ kích hoạt bản cập nhật khi tải thành công.
"""
import argparse
import datetime
import hashlib
import json
import pathlib
import re
import shutil

ROOT = pathlib.Path(__file__).resolve().parent.parent
FILES = [
    "index.html", "style.css", "app.js", "ota.js", "sw.js", "manifest.json",
    "icons/icon-192.png", "icons/icon-512.png", "icons/icon-maskable.png",
    "icons/apple-touch-icon.png", "icons/favicon-32.png",
]


def build(output: pathlib.Path, revision: str, published: str) -> dict:
    revision = revision.strip()
    if not re.fullmatch(r"[a-fA-F0-9]{7,64}", revision):
        raise ValueError("revision phải là Git commit SHA (7–64 ký tự hex)")
    short = revision[:12].lower()
    if output.resolve() in (ROOT.resolve(), ROOT.parent.resolve()):
        raise ValueError("Không được xóa thư mục mã nguồn")
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    for name in FILES:
        origin = ROOT / name
        if not origin.is_file():
            raise FileNotFoundError(f"Thiếu file bắt buộc: {name}")
        dest = output / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(origin, dest)
    html = (output / "index.html").read_text(encoding="utf-8")
    build_tag = f'<meta name="smoodri-web-version" content="{short}">'
    if '<head>' not in html:
        raise ValueError("Thiếu <head> trong index.html")
    html = html.replace('<head>', '<head>\n  ' + build_tag, 1)
    (output / "index.html").write_text(html, encoding="utf-8")
    sw_path = output / "sw.js"
    sw = sw_path.read_text(encoding="utf-8")
    if "__SMOODRI_VERSION__" not in sw:
        raise ValueError("sw.js thiếu placeholder phiên bản")
    sw_path.write_text(sw.replace("__SMOODRI_VERSION__", short), encoding="utf-8")
    metadata = []
    for name in FILES:
        data = (output / name).read_bytes()
        metadata.append({"path": name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
    manifest = {
        "app": "SmooDri", "channel": "web-ota", "version": short,
        "revision": revision.lower(), "publishedAtUTC": published,
        "files": metadata,
    }
    (output / "version.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--output", default="dist")
    p.add_argument("--revision", required=True)
    p.add_argument("--published", default=datetime.datetime.now(datetime.timezone.utc).isoformat())
    args = p.parse_args()
    manifest = build(pathlib.Path(args.output), args.revision, args.published)
    print(f"Published SmooDri web OTA {manifest['version']} ({len(manifest['files'])} files) -> {args.output}")


if __name__ == "__main__":
    main()
