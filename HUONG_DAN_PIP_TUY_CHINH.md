# Tùy chỉnh cửa sổ PiP từ SmooDri

Mở CÀI ĐẶT → 6. Cửa sổ PiP trong App Hub. Tỷ lệ 16:9, 4:3 hoặc 1:1; chọn thông số; cỡ chữ; độ đậm; màu nền và màu chữ. Thay đổi lưu vào `smoodri_cfg_v8`, được bridge trong App Hub chuyển cho giao diện PiP.

Tốc độ GPS xuất phát từ Web SmooDri. Khi ứng dụng bị Android hạn chế WebView chạy nền, dữ liệu tốc độ không được làm mới; PiP hiển thị `--` sau 3,2 giây thay vì giữ số cũ. Điểm và bốn chỉ số PiP thuộc dịch vụ native Android, có thể khác số trong Web. Cấu hình PiP không thay đổi ngưỡng cảm biến native.

Chỉ thay tỷ lệ PiP đòi một lần build/cài APK App Hub mới, vì bridge Android hiện tại cố định 16:9. Những chỉnh sửa giao diện sau đó có thể theo OTA. Chưa kiểm thử trên điện thoại.
