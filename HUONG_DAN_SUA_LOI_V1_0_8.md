# SMOODRI v1.0.8 — SỬA OVERLAY KHÔNG MỞ VÀ ÂM BÁO KHÔNG ĐỒNG NHẤT

## Những thay đổi trong mã nguồn

1. **Overlay**: Khi nhấn mở Overlay, ứng dụng đưa Activity về nền trước, sau đó yêu cầu Foreground Service tạo cửa sổ nổi. Điều này hạn chế cuộc đua giữa việc mở cửa sổ và `onResume()` tự ẩn Overlay. Các thao tác nhấn Home cũng dùng cách mở trì hoãn tương tự.
2. **Báo lỗi Overlay**: Android có thể từ chối `WindowManager.addView()`. Phiên bản cũ nuốt lỗi này. Phiên bản mới ghi rõ ngoại lệ và đưa các thông tin `Quyền Overlay`, `Overlay ĐANG HIỂN THỊ / CHƯA HIỂN THỊ` và `Lỗi Overlay` vào **KIỂM TRA ANDROID / PiP / OVERLAY / GPS**. Nếu máy bị quản trị viên chặn cửa sổ nổi, mã ứng dụng không thể tự vượt qua.
3. **Âm báo native**: Không còn dùng `ToneGenerator.TONE_PROP_BEEP` (âm gốc 35 ms ghép 400 Hz + 1.200 Hz). Thay bằng `NativeAlertPlayer.java`, tạo bốn tiếng bíp ngắn có điểm vào/ra mềm, khoảng nghỉ cố định, âm lượng đa phương tiện.
4. **Một bộ phát âm trong APK**: Khi là APK native, cả giao diện trước mắt, PiP, Overlay và chế độ chỉ âm thanh nền đều dùng cùng bộ phát Java; không phát chồng Web Audio. Trên trình duyệt/PWA vẫn giữ âm Web Audio như trước.
5. **Nghe thử**: Trong APK, nghe thử 4 tiếng bíp hoặc âm ổ gà dùng chính bộ tạo âm native mới, kể cả khi chưa BẮT ĐẦU theo dõi. Cỡ âm lượng/kiểu âm nghe thử lấy từ cài đặt hiện hành.
6. **Giữ nguyên** giao diện One View, GPS native, thanh trượt cài đặt, PiP, OTA, không khôi phục Điểm êm ái.

## Cách cập nhật GitHub

- Sao lưu repo và cài đặt hiện có.
- Đưa file trong ZIP `SmooDri-v1.0.8-chi-file-can-thay.zip` vào đúng đường dẫn trong repo `smoodri`. Hoặc dùng ZIP toàn repo.
- Commit thay đổi.
- Chạy **Actions → Build SmooDri APK**; lấy artifact `SmooDri-v1.0.8-debug.apk` nếu build thành công.
- **Cần cài APK mới**: thay đổi `MainActivity.java`, `DriveMonitorService.java`, `NativeOverlayController.java` và thêm `NativeAlertPlayer.java` không thể cập nhật chỉ bằng OTA web.
- Nếu APK trước đó ký bằng khóa khác, Android có thể không cho cài đè. Hãy sao lưu dữ liệu trước khi thay phiên bản, nhất là khi phải gỡ app.

## Thử riêng các chức năng khi xe đứng yên

### A. Kiểm tra âm thanh

1. Mở SmooDri, tăng âm lượng **Đa phương tiện/Media** của điện thoại.
2. Trong **CÀI ĐẶT CHUNG**, bật **Âm thanh cảnh báo**.
3. Nhấn **▶ Nghe thử 4 tiếng bíp nhanh**. Phiên bản 1.0.8 có thể nghe thử **trước khi** BẮT ĐẦU.
4. Nghe thử **âm ổ gà**; nếu cần hãy chọn tiếng `Bíp nhẹ` / `Âm nhẹ` để có âm mềm hơn.
5. Nhấn BẮT ĐẦU, chờ cân chỉnh, thử PiP và Chỉ âm thanh nền. Tiếng cảnh báo chỉ xảy ra khi cảm biến vượt ngưỡng; không phải âm báo phát liên tục.
6. Nếu nghe thử không phát, vào **KIỂM TRA ANDROID** xem mục **Lỗi âm thanh**, rồi gửi nguyên văn kết quả.

### B. Kiểm tra Overlay

1. CÀI ĐẶT CHUNG → chọn **Overlay** và cấp quyền **Hiển thị trên ứng dụng khác** cho đúng APK SmooDri. Trên Android mới trang cấp quyền có thể không tự chọn ứng dụng; hãy tìm tên SmooDri trong danh sách.
2. Nhấn **BẮT ĐẦU**, chờ dịch vụ đo thực sự chạy.
3. Nhấn **▣ Overlay** hoặc Home để ra màn hình ngoài.
4. Nếu không hiện, quay lại SmooDri → **KIỂM TRA ANDROID / PiP / OVERLAY / GPS**. Gửi đầy đủ dòng thông báo gồm **Quyền Overlay**, **Overlay**, **Chế độ**, **Lỗi Overlay**.
5. Trên một số điện thoại bị quản trị viên kiểm soát, quyền hiển thị trên ứng dụng khác có thể bị vô hiệu dù PiP hoạt động. Nếu Android từ chối, cần cấp đúng quyền hoặc dùng PiP thay thế.
6. Lưu ý khi quay lại giao diện chính, SmooDri tự ẩn Overlay; vì vậy mục “Overlay chưa hiển thị” lúc đang ở màn hình chính là **bình thường** nếu không có dòng **Lỗi Overlay**.

### C. Kiểm tra chạy nền

1. Chọn **Chỉ âm thanh nền**, bật âm báo, nhấn BẮT ĐẦU và chờ cân chỉnh.
2. Nhấn **♪ NỀN** hoặc Home. Không có cửa sổ hiện lên. Android phải hiển thị thông báo Foreground Service đang đo.
3. Chế độ nền chỉ phát âm khi vượt ngưỡng. Để nghe thử ngay, quay lại ứng dụng và dùng nút **Nghe thử**.
4. Không vuốt xóa SmooDri khỏi màn hình đa nhiệm nếu muốn tiếp tục phiên đo; việc xóa tác vụ có thể làm dịch vụ dừng theo thiết kế hiện tại.

## Giới hạn xác minh

Mã Java mới, liên kết bridge và các bài kiểm tra source-level đã được kiểm tra. `NativeAlertPlayer.java` được biên dịch bằng javac + lớp Android mô phỏng. **Chưa build toàn APK với Android SDK, cũng chưa chạy thử Overlay hoặc âm thanh trên thiết bị của bạn.** GitHub Actions và phép thử thực tế là bước xác nhận bắt buộc.
