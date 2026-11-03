# HƯỚNG DẪN TẠO APK SMOODRI (CÓ PiP NATIVE)

Repo hiện bao gồm Android native **Foreground Service + PiP**, giao diện One View và OTA web.

1. Chép toàn bộ nội dung ZIP vào gốc repo GitHub `smoodri` (kể cả `.github/`), Commit.
2. Trong GitHub Actions, chạy **Build SmooDri APK**.
3. Artifact dự kiến: `SmooDri-v1.0.6-debug.apk`, theo `versionName` tại `android/app/build.gradle`.
4. Cài APK, bật quyền Vị trí, GPS, Thông báo nếu cần và PiP.
5. Nhấn BẮT ĐẦU rồi nhấn nút **▣ PiP** hoặc Home để vào PiP.
6. Để phát hành cập nhật web, bật GitHub Pages → Source: GitHub Actions và chạy **Publish SmooDri Web OTA (Pages)**.

**Quan trọng:** APK debug không bảo đảm chữ ký ổn định giữa các runner; muốn cài đè lâu dài, cấu hình Release JKS cố định. Việc đưa PiP native vào APK cũ bắt buộc tạo và cài APK mới. Các cập nhật giao diện về sau có thể dùng OTA web.

Xem hướng dẫn chi tiết: [`HUONG_DAN_PIP_OTA_HOP_NHAT.md`](HUONG_DAN_PIP_OTA_HOP_NHAT.md).
