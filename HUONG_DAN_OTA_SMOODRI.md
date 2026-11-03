# Cập nhật OTA cho SmooDri 1.0.9 (chỉ âm thanh nền)

**Có hai loại thay đổi:**

1. Thay đổi HTML/CSS/JavaScript, hình ảnh hoặc hướng dẫn hiển thị: chạy `Publish SmooDri Web OTA (Pages)`. Khi có Internet, APK kiểm tra phiên bản web, tải và lưu để dùng offline. Không cần build lại APK nếu cấu trúc Android không thay đổi.
2. Thay đổi `MainActivity.java`, `DriveMonitorService.java`, `NativeAlertPlayer.java`, `AndroidManifest.xml` hoặc quyền Android: **phải build và cài APK mới**. OTA web không thay thế Java Android.

### Khi cập nhật từ 1.0.8 lên 1.0.9

- Ghi đè các file mới trên GitHub và **xóa hai lớp Java PiP/Overlay cũ**, xem `FILES_TO_DELETE_ON_GITHUB.txt`.
- Chạy `Publish SmooDri Web OTA (Pages)` trước; sau khi thành công, chạy `Build SmooDri APK`.
- Bản 1.0.9 không có PiP hoặc Overlay. Khi ra nền, chỉ có thông báo Android và âm cảnh báo theo cảm biến.
- Tài khoản/điện thoại có quyền quản trị có thể giới hạn hoạt động nền. Cấp quyền thông báo nếu Android hỏi, kiểm tra mục tiết kiệm pin của ứng dụng nếu không có âm khi Home.

### Giữ được dữ liệu cũ?

Mã vẫn dùng `smoodri_cfg_v8` và `smoodri_numeric_limits_v1`. Không tự ý xóa bộ nhớ ứng dụng. Tuy nhiên, việc gỡ APK để cài lại do khác chữ ký có thể xóa localStorage. Dùng khóa ký release cố định nếu muốn cài đè lâu dài.

**Lưu ý:** Khi offline lần đầu chưa từng vào ứng dụng, APK dùng nội dung web đóng gói của lúc build. Mở có Internet để Service Worker nhận bản OTA mới. Không tự sửa `sw.js` nếu workflow đã quản lý phiên bản.
