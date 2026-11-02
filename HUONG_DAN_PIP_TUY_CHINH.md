# HƯỚNG DẪN CÀI ĐẶT PiP — SMOODRI

Trong APK SmooDri, vào **CÀI ĐẶT CHUNG → PiP trên Android / App Hub**:

- Chọn tỷ lệ 16:9 / 4:3 / 1:1, cỡ chữ điểm chính, độ đậm, màu nền và chữ.
- Chọn thông số hiện trong PiP: điểm, tốc độ, GA, PHANH, VÀO CUA, Ổ GÀ và GPS.
- Thay đổi được lưu và gửi sang dịch vụ native khi đã BẮT ĐẦU theo dõi.
- Khi nhấn Home hoặc nút `▣ PiP`, cửa sổ native hiển thị giá trị cảm biến/GPS của Foreground Service. Điểm PiP là phép tính native độc lập; có thể khác nhẹ so với giao diện web.
- Nếu không đổi được tỷ lệ, kiểm tra giới hạn PiP của Android và quyền quản trị thiết bị.
- Nếu chỉ mở bằng trình duyệt hoặc cài PWA, không có nút native PiP.

Muốn dùng PiP phải cài bản APK chứa ba file `MainActivity.java`, `DriveMonitorService.java`, `NativePipView.java` và Manifest khai báo PiP. Không thể chỉ OTA web để bổ sung phần native này.

Xem [`HUONG_DAN_PIP_OTA_HOP_NHAT.md`](HUONG_DAN_PIP_OTA_HOP_NHAT.md).
