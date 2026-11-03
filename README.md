# SmooDri v1.0.9 — Chỉ cảnh báo âm thanh dưới nền

Bản này được xây dựng từ ZIP `smoodri-main (1)(2).zip` mà người dùng cung cấp. Mục tiêu là **giữ ứng dụng One View khi mở** và **chỉ phát cảnh báo âm thanh khi chuyển ra nền**, không còn PiP hay Overlay.

## Những gì đã thay đổi

- **Bỏ PiP và Overlay hoàn toàn** khỏi SETTING, AndroidManifest, cầu nối WebView và các lớp Java native. Xóa `NativePipView.java` và `NativeOverlayController.java`.
- Khi màn hình SmooDri **đang mở**, Web Audio phát âm thanh gốc: **4 bíp** nhanh luân phiên 1550/1900 Hz, 60 ms/tiếng, cách nhau 105 ms (kiểu âm đã chọn trong SETTING). Cảnh báo ổ gà là **2 tiếng** 850/600 Hz, 95 ms/tiếng, cách nhau 135 ms. Lắc ngang liên tục phát 4 bíp ở 2000 Hz.
- Khi SmooDri **đang chạy nền**, `DriveMonitorService` tiếp tục nhận cảm biến và `NativeAlertPlayer` tổng hợp PCM có **cùng tần số, hình dạng sóng, nhịp bíp và tỷ lệ âm lượng**. Web Audio không phát cùng lúc để tránh tiếng chồng.
- Có hai nút để so sánh ngay trong SETTING: **NGHE THỬ 4 TIẾNG BÍP** (Web Audio), **NGHE THỬ ÂM NỀN ANDROID** (native). Nút native chỉ hiện trong APK.
- Không buộc GPS phải được cấp quyền hoặc bật khi khởi động **cảnh báo gia tốc**. GPS/tốc độ chỉ là tính năng bổ sung nếu thiết bị cấp quyền.
- Giữ giao diện One View, CÀI ĐẶT CHUNG, CHỈNH GIAO DIỆN TRỰC TIẾP, thanh trượt +/−, MIN/MAX/STEP/DEFAULT, lưu cài đặt `smoodri_cfg_v8` và OTA web.
- **Android Foreground Service** sử dụng `specialUse` cho tác vụ cảnh báo gia tốc liên tục do người dùng chủ động kích hoạt. Có thông báo Android và nút dừng. Xem [tài liệu Android](https://developer.android.com/develop/background-work/services/fgs/service-types).

## Cách sử dụng

1. Mở SmooDri, vào SETTING, bật **ÂM THANH CẢNH BÁO** và đặt âm lượng Media của điện thoại.
2. Nhấn **NGHE THỬ 4 TIẾNG BÍP** (Web Audio); trong APK nhấn tiếp **NGHE THỬ ÂM NỀN ANDROID** để so sánh.
3. Nhấn **BẮT ĐẦU**, đặt điện thoại cố định, đứng yên trong thời gian cân chỉnh.
4. Nhấn **Home** hoặc nút **♪ CHẠY NỀN**. Không có cửa sổ nhỏ hiện ra; dịch vụ tiếp tục chạy và phát cảnh báo **chỉ khi vượt ngưỡng**.
5. Quay lại SmooDri, nhấn **DỪNG**; hoặc nhấn **Dừng theo dõi** trên thông báo Android.

Không có âm thanh liên tục khi xe vận hành êm ái. **Nghe thử có tiếng** không có nghĩa cảnh báo sẽ phát liên tục; phải có cảm biến thực và vượt ngưỡng.

Trên điện thoại bị quản trị hoặc hệ thống tiết kiệm pin mạnh, dịch vụ có thể bị chặn; hãy kiểm tra quyền thông báo và phần **KIỂM TRA DỊCH VỤ & ÂM THANH**. Dùng tính năng khi xe dừng để cấu hình, không thao tác điện thoại khi đang lái.

## Phát hành GitHub

- Trước tiên **Publish SmooDri Web OTA (Pages)** thành công để trang web có giao diện và JavaScript mới.
- Sau đó chạy **Build SmooDri APK**. Bản 1.0.9 cần APK mới do Android `MainActivity`, Foreground Service, Manifest và bộ phát âm đã thay đổi.
- APK dự kiến: `SmooDri-v1.0.9-debug.apk`. Chữ ký debug có thể thay đổi giữa các lần GitHub Actions build; cân nhắc khóa release cố định nếu cần cài đè lâu dài.
- Không sửa `sw.js` bằng tay nếu không cần; workflow OTA tự quản lý version/cache.

**Rất quan trọng khi tải file thủ công lên repo đã tồn tại:** ngoài ghi đè các file mới, bạn còn phải **xóa hai file Java cũ** ở `android/app/src/main/java/org/smoodri/app/`: `NativePipView.java`, `NativeOverlayController.java`. Nếu không xóa, Gradle vẫn biên dịch chúng và có thể báo lỗi.

Xem thêm [HUONG_DAN_AM_THANH_NEN.md](HUONG_DAN_AM_THANH_NEN.md) và [FILES_TO_DELETE_ON_GITHUB.txt](FILES_TO_DELETE_ON_GITHUB.txt).

## Kiểm tra và giới hạn

Đã kiểm tra cú pháp JS, các ID HTML, 13 bài kiểm tra source/OTA và biên dịch riêng bộ phát âm native với các lớp Android mô phỏng. **Chưa build được APK hoàn chỉnh bằng Android SDK hoặc nghe âm thanh trên điện thoại thật.** Cần xác nhận bằng GitHub Actions và máy Android.
