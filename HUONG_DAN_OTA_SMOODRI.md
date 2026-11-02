# HƯỚNG DẪN CẬP NHẬT OTA — SMOODRI

## 1. Cơ chế cập nhật

- **OTA web:** sửa `index.html`, `style.css`, `app.js`, `ota.js`, `sw.js`, `manifest.json` hoặc `icons/`, commit lên `main`. GitHub Actions `Publish SmooDri Web OTA (Pages)` tự chạy, đóng gói `dist/` theo Git commit và phát hành lên GitHub Pages.
- Khi mở SmooDri có Internet, WebView/PWA kiểm tra bản `version.json` và cập nhật service worker. Bản web mới được tải vào bộ nhớ đệm và kích hoạt khi không theo dõi chuyến đi. Nếu đang theo dõi, tránh reload làm gián đoạn việc đo.
- Đã tải thành công một lần thì có thể mở offline bằng bộ nhớ đệm của Service Worker. Trường hợp thiết bị chưa có bộ nhớ đệm, APK có nội dung web dự phòng đóng gói sẵn.
- OTA web **không thể** cập nhật Java, Manifest, Foreground Service, Android PiP, quyền hệ thống, icon APK, `versionCode` / `versionName`.

## 2. Thiết lập GitHub lần đầu

1. Thay các file đã sửa trong repo `smoodri` và commit lên nhánh `main`.
2. Trong GitHub: **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. Vào **Actions → Publish SmooDri Web OTA (Pages) → Run workflow** (nếu workflow chưa tự chạy).
4. Đảm bảo workflow thành công và URL có thể mở: `https://gptlohoi-del.github.io/smoodri/`.
5. Kiểm tra `https://gptlohoi-del.github.io/smoodri/version.json` có phiên bản Git SHA mới, không phải `builtin`.
6. Trên điện thoại, mở SmooDri khi kết nối Internet. Mục CÀI ĐẶT CHUNG có trạng thái **CẬP NHẬT NỘI DUNG OTA** và nút **KIỂM TRA CẬP NHẬT**.

## 3. Một lần APK mới để cải thiện offline

`MainActivity.java` có sửa phần xử lý WebView offline, giúp ưu tiên bản đã cache qua OTA thay vì luôn ép về web đóng gói trong APK. Để Android nhận thay đổi native này, cần build/cài APK một lần. Các lần sửa **chỉ web** về sau không cần cài APK.

- `android/app/build.gradle` được tăng `versionCode=2`, `versionName='1.0.1'` để build mới.
- `Build SmooDri APK` sẽ phát hành artifact `SmooDri-v1.0.1-debug.apk`.
- Nếu các APK debug trước đây được ký bằng khóa khác nhau, Android có thể **không cho cài đè**. Nên chuyển sang khóa ký Release JKS cố định trước khi dùng lâu dài. Tránh gỡ ứng dụng nếu chưa sao lưu cài đặt.
- Repo nguồn được cung cấp ở đây chỉ có `MainActivity.java` cơ bản, **không chứa Foreground Service và PiP native** trong mã Android. Bộ OTA này không bổ sung hay thay thế các thành phần native đó. Nếu máy đang dùng bản Android có PiP native khác, không thay các file native của bản ấy bằng MainActivity này mà cần gộp thay đổi offline vào đúng phiên bản đó.

## 4. Quy trình sử dụng lâu dài

**Đổi giao diện, thuật toán JavaScript, thông số, trang cài đặt:** Chỉnh các file web, commit, chờ `Publish SmooDri Web OTA (Pages)` hoàn thành; không build APK.

**Đổi quyền Android, foreground service, PiP native, icon Android:** Build APK mới, giữ nguyên `applicationId` và khóa ký nếu muốn cài đè.

## 5. Kiểm tra và xử lý sự cố

- Nếu giao diện cũ: xác nhận workflow xanh, `version.json` đúng SHA mới; mở ứng dụng có mạng, chờ cập nhật hoàn tất, đóng rồi mở lại. Không cần bấm nút cập nhật trừ khi muốn kiểm tra trạng thái.
- Nếu không có mạng: ứng dụng giữ nội dung cũ đã lưu hoặc dự phòng trong APK, không mất cấu hình `smoodri_cfg_v8`.
- Nếu bản PWA chưa từng mở thành công khi online, phải mở online ít nhất một lần để tải đầy đủ các tệp cốt lõi.
- Tránh thay đổi cài đặt/kiểm tra cập nhật trong lúc lái xe.
- Đừng xóa dữ liệu WebView/ứng dụng nếu muốn giữ cài đặt localStorage; cập nhật OTA web không yêu cầu xóa dữ liệu.

## 6. File mới và file sửa

**Mới:** `.github/workflows/deploy-web-ota.yml`, `scripts/build_web_ota.py`, `tests/test_web_ota.py`, `ota.js`, `version.json`, `HUONG_DAN_OTA_SMOODRI.md`.

**Sửa:** `sw.js`, `index.html`, `style.css`, `app.js`, `android/app/build.gradle`, `android/app/src/main/java/org/smoodri/app/MainActivity.java`.
