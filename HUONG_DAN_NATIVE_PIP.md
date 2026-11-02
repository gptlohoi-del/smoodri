# PiP native và Foreground Service

Phiên bản SmooDri 1.0.2 có đủ các file Android native hỗ trợ PiP và Foreground Service.

- `MainActivity.java`: WebView, cầu nối JavaScript, mở PiP, xử lý ngoại tuyến.
- `DriveMonitorService.java`: đọc cảm biến và GPS, thông báo chạy nền, cảnh báo âm thanh native.
- `NativePipView.java`: hiển thị các chỉ số khi ứng dụng nằm trong cửa sổ PiP.
- `AndroidManifest.xml`: quyền Vị trí, thông báo, foreground service, hỗ trợ PiP.

Xem hướng dẫn cài đặt, OTA và khắc phục sự cố trong [`HUONG_DAN_PIP_OTA_HOP_NHAT.md`](HUONG_DAN_PIP_OTA_HOP_NHAT.md).
