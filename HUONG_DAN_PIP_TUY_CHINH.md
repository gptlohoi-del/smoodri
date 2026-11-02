# HƯỚNG DẪN CÀI ĐẶT PiP — SMOODRI

Trong APK SmooDri, vào **CÀI ĐẶT CHUNG → PiP trên Android / App Hub**:

- Chọn tỷ lệ 16:9 / 4:3 / 1:1, cỡ chữ riêng của TỐC ĐỘ, GA, PHANH, CUA, Ổ GÀ, GPS, độ đậm, màu nền và chữ.
- Chọn thông số hiện trong PiP: tốc độ, GA, PHANH, VÀO CUA, Ổ GÀ và GPS; mỗi thông số có cỡ chữ riêng.
- Thay đổi được lưu và gửi sang dịch vụ native khi đã BẮT ĐẦU theo dõi.
- Khi nhấn Home hoặc nút `▣ PiP`, cửa sổ native hiển thị giá trị cảm biến/GPS của Foreground Service. Không còn Điểm chuyến đi trong PiP.
- Nếu không đổi được tỷ lệ, kiểm tra giới hạn PiP của Android và quyền quản trị thiết bị.
- Nếu chỉ mở bằng trình duyệt hoặc cài PWA, không có nút native PiP.

Muốn dùng PiP phải cài bản APK chứa ba file `MainActivity.java`, `DriveMonitorService.java`, `NativePipView.java` và Manifest khai báo PiP. Không thể chỉ OTA web để bổ sung phần native này.

Xem [`HUONG_DAN_PIP_OTA_HOP_NHAT.md`](HUONG_DAN_PIP_OTA_HOP_NHAT.md).

## Tại sao chỉnh cỡ chữ trước đây gần như không thấy thay đổi?

PiP native của phiên bản trước cố định cỡ chữ GA/PHANH/CUA/Ổ GÀ = 19 đơn vị và GPS = 17 đơn vị, chỉ dùng `pipFontSize` chung cho Điểm và Tốc độ. Ngoài ra Canvas được thu theo kích thước PiP do Android quản lý.

Bản mới nhận 6 cấu hình riêng qua `app.js` → `MainActivity` → `DriveMonitorService` → `NativePipView`. Đổi thông số trong CÀI ĐẶT CHUNG sẽ được lưu và có hiệu lực trên PiP khi đang theo dõi. Cỡ chữ lớn có thể tự co vừa cột nếu chuỗi số dài, giúp tránh bị cắt chữ.

**Lưu ý:** phần `.java` đã thay đổi, nên cần **build và cài APK mới**. Chỉ phát hành OTA web không đủ để sửa PiP native của APK cũ.
