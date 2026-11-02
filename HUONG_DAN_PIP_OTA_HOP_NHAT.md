# HƯỚNG DẪN SMOODRI — PiP + FOREGROUND SERVICE + OTA

## 1. Đưa repo lên GitHub

1. Sao lưu bản cũ. Giải nén ZIP và đưa **nội dung bên trong** lên đúng thư mục gốc repo `gptlohoi-del/smoodri` (kể cả thư mục ẩn `.github`).
2. Bảo đảm có đủ `android/app/src/main/java/org/smoodri/app/MainActivity.java`, `DriveMonitorService.java`, `NativePipView.java` và `android/app/src/main/AndroidManifest.xml`.
3. Commit. Vào GitHub **Settings → Pages → Source: GitHub Actions**.
4. Chạy **Publish SmooDri Web OTA (Pages)** để phát hành bản web hiện tại.
5. Chạy **Build SmooDri APK** để tạo APK **có native PiP**. Cài APK mới trên điện thoại. Cập nhật web KHÔNG thể thêm native PiP vào APK cũ.

## 2. Sử dụng

1. Cho phép Vị trí (GPS), Thông báo (Android 13+) và Hình trong hình (PiP). Trên Android có thể vào Cài đặt → Ứng dụng → Truy cập ứng dụng đặc biệt → Hình trong hình → SmooDri → Cho phép (tên mục có thể khác nhau).
2. Bật GPS, gắn điện thoại cố định, mở SmooDri, nhấn **BẮT ĐẦU**. Đợi cân chỉnh khi xe đang dừng.
3. Nhấn **▣ PiP** trên thanh trên cùng hoặc Home để chuyển sang ứng dụng khác. Hệ thống Android quyết định kích thước PiP. Trên web/PWA, nút PiP native không xuất hiện.
4. Thay tỷ lệ 16:9, 4:3, 1:1, màu và chỉ số trong CÀI ĐẶT CHUNG → PiP. Các cấu hình được gửi sang Foreground Service qua cầu nối JavaScript.
5. Khi nhấn DỪNG hoặc dừng bằng thông báo Android, theo dõi dừng. Đóng hẳn Activity cũng có thể dừng dịch vụ; đây là hành vi có chủ đích để tránh chạy ngầm không kiểm soát.

**An toàn:** SmooDri đo thử nghiệm, không phải thiết bị hỗ trợ lái xe được kiểm định. Không điều chỉnh thông số khi đang lái.

## 3. OTA web và offline

- Thay đổi `index.html`, `style.css`, `app.js`, `ota.js`, `sw.js` được đưa lên GitHub Pages qua workflow OTA; thiết bị kiểm tra khi mở có mạng.
- Cập nhật OTA chỉ thay **nội dung web**, không thay `.java`, `AndroidManifest.xml`, quyền, PiP native hay APK.
- Nếu đang đo, OTA không được chủ động reload để tránh ngắt phiên đo. Khi kết thúc hoặc mở lại, bản mới có thể kích hoạt.
- Khi offline: ưu tiên bản WebView Service Worker đã tải và lưu; nếu chưa có cache, sử dụng nội dung được đóng gói trong APK. Muốn kiểm tra offline, mở ứng dụng online thành công trước, sau đó đóng hẳn, tắt Wi-Fi/dữ liệu và mở lại.

## 4. Phân biệt cập nhật APK

- APK debug từ GitHub Actions có thể mang chữ ký debug KHÁC NHAU ở các lần build, tùy runner và cách tạo khóa. Vì vậy việc cài đè không được bảo đảm; bạn có thể phải gỡ bản cũ và mất cài đặt.
- Để phát hành lâu dài, cần cấu hình chữ ký Release JKS cố định; giữ nguyên `applicationId`, tăng `versionCode`, và phát hành APK ký bằng cùng khóa. Với người đang dùng bản debug ký khác khóa, lần chuyển sang Release thường phải gỡ/cài lại một lần.
- Mặc định workflow này phát hành **debug** để chạy thử, không tự nhận khóa Release JKS.

## 5. Kiểm tra nếu không hiện PiP

- Nếu có nút `▣ PiP` trong APK: cầu nối native đã hoạt động. Nếu không có nút, nhiều khả năng APK chưa build từ repo này hoặc web đã tải từ bản không có nút.
- Bắt buộc nhấn **BẮT ĐẦU**, bật GPS và cấp quyền Vị trí trước. Nếu GPS đang tắt, dịch vụ có thể không khởi động.
- Trong Cài đặt Android, bật quyền **Hình trong hình** cho SmooDri.
- Nếu ứng dụng chỉ hiển thị trong trình duyệt GitHub Pages/PWA, PiP native không có sẵn.
- Cài đặt tỷ lệ PiP không bảo đảm cửa sổ thực tế đổi theo đúng tỷ lệ nếu Android/nhà quản trị thiết bị giới hạn.
- Nếu sử dụng thiết bị do quản trị viên kiểm soát, PiP hoặc chạy nền có thể bị chặn bởi chính sách thiết bị.

## 6. Các kiểm thử cần thực hiện trên điện thoại

(1) Khởi động GPS + cảm biến; (2) PiP từ nút và Home; (3) đổi tỷ lệ/hiển thị PiP; (4) dừng rồi kiểm tra thông báo/dịch vụ ngừng; (5) cập nhật web OTA online; (6) mở lại offline; (7) cài đặt không bị mất.
