# SMOODRI v1.0.7 — SỬA PiP, OVERLAY, ÂM THANH NỀN, GPS VÀ HIỂN THỊ TỐC ĐỘ

## Vì sao các chế độ không hoạt động?

Bản 1.0.6 có những điểm cần khắc phục:

1. Phía web lấy GPS bằng `navigator.geolocation.watchPosition()` ngay cả khi Android Foreground Service đã có quyền vị trí, dẫn đến thông báo "GPS CHƯA ĐƯỢC CẤP QUYỀN" từ WebView mà không phản ánh đúng quyền Android.
2. Android phản hồi `onStarted(true)` ngay sau lệnh mở dịch vụ mà chưa xác nhận Foreground Service/cảm biến đã sẵn sàng. Nếu dịch vụ không khởi động, PiP/Overlay/âm thanh nền đều không hoạt động.
3. Âm thanh native sử dụng luồng thông báo của Android, có thể im tiếng khi hệ thống tắt âm thông báo dù âm lượng đa phương tiện còn bật.
4. Giao diện tốc độ thêm nhãn `km/h` màu đen bên cạnh số tốc độ.

Đây là các vấn đề xác định được từ mã nguồn, **không khẳng định là nguyên nhân duy nhất trên thiết bị**. Nếu hệ thống quản trị điện thoại chặn PiP, Overlay hoặc Foreground Service thì mã ứng dụng không thể tự vượt qua hạn chế đó.

## Đã sửa trong v1.0.7

- `index.html`: bỏ nhãn `km/h` màu đen, thêm mục **KIỂM TRA ANDROID / PiP / OVERLAY / GPS** trong CÀI ĐẶT CHUNG.
- `app.js`: trong APK, lấy GPS từ `DriveMonitorService` thay vì xin quyền lần hai từ WebView. Giữ `navigator.geolocation` cho bản Web/PWA. Bổ sung nút kiểm tra trạng thái và nghe thử native.
- `MainActivity.java`: chờ Android thực sự bật dịch vụ + nhận cảm biến trước khi xác nhận BẮT ĐẦU; báo lỗi cụ thể khi không chạy; cải thiện việc mở PiP và quay lại sau trang cấp quyền Overlay; cung cấp thông tin chẩn đoán trong SETTING.
- `DriveMonitorService.java`: kiểm tra đã đăng ký cảm biến, chuyển GPS sang JSON gửi cho web, có lệnh kiểm tra tiếng bíp native, dùng âm lượng media cho âm báo nền, vẫn chỉ báo khi vượt ngưỡng.
- `android/app/build.gradle`: nâng `versionName` lên **1.0.7**.

## Cách kiểm tra trên điện thoại

1. Tải repo v1.0.7 lên GitHub và chạy **Actions → Build SmooDri APK**.
2. Cài APK mới. Chỉ OTA web **không thể sửa Java native** trên APK đang cài.
3. Mở **CÀI ĐẶT CHUNG → KIỂM TRA ANDROID / PiP / OVERLAY / GPS**. Mục này sẽ cho biết phiên bản APK, dịch vụ có chạy hay chưa, quyền vị trí, trạng thái GPS hệ thống, quyền Overlay, chế độ đang chọn và lỗi khởi động gần nhất.
4. Đỗ xe, đặt điện thoại cố định, bật Vị trí chính xác cho SmooDri, nhấn **BẮT ĐẦU** và đợi trạng thái theo dõi.
5. Mở SETTING và nhấn **NGHE THỬ 4 TIẾNG BÍP NHANH** trong khi dịch vụ đang chạy. Đây là phép thử âm thanh native; tăng **âm lượng đa phương tiện (Media)** nếu không nghe.
6. Chọn **PiP**, nhấn nút `▣ PiP` hoặc Home. Đảm bảo Android cho phép SmooDri dùng Picture-in-picture.
7. Chọn **Overlay**, cấp quyền **Hiển thị trên ứng dụng khác**, nhấn `▣ Overlay`. Nếu vừa cấp quyền từ trang hệ thống, bản mới có thể tự mở Overlay khi quay lại.
8. Chọn **Chỉ âm thanh nền**, bật âm thanh cảnh báo, rồi nhấn `♪ NỀN` hoặc Home. Không có cửa sổ nổi. Tiếng bíp chỉ phát khi vượt ngưỡng, không phát liên tục khi xe vận hành êm.

**PiP và Overlay vẫn có tiếng cảnh báo** nếu bật âm thanh và dữ liệu đo vượt ngưỡng. Khi ứng dụng ở nền, tiếng bíp do Android native Foreground Service phát, không phụ thuộc Web Audio.

Nếu chẩn đoán cho biết dịch vụ không chạy dù đã bật GPS, hãy kiểm tra quyền Vị trí của đúng APK SmooDri, cài đặt tiết kiệm pin, quyền thông báo, và các hạn chế thiết bị do quản trị viên áp dụng. Có thể gửi lại nguyên văn trạng thái trong mục KIỂM TRA ANDROID để phân tích chính xác hơn.

## Giới hạn kiểm thử

Đã kiểm tra cú pháp web, giao diện giả lập, các kiểm tra source-level và quy trình đóng gói OTA. **Chưa biên dịch APK bằng Android SDK hoặc kiểm tra PiP/Overlay/âm thanh trên điện thoại thật**; GitHub Actions và thiết bị là bước xác nhận cuối cùng.
