# SmooDri 1.0.10 — sửa lỗi AudioTrack chưa khởi tạo

## Nguyên nhân đã xác định từ mã nguồn

Bản 1.0.9 dùng AudioTrack.MODE_STATIC nhưng kiểm tra STATE_INITIALIZED ngay sau build().
Với MODE_STATIC, Android có thể trả về STATE_NO_STATIC_DATA = 2: AudioTrack được tạo thành công nhưng chưa nhận dữ liệu PCM.
Việc kiểm tra sai thứ tự làm phương thức prepare() ném IllegalStateException trước khi gọi write().

## Thay đổi
- NativeAlertPlayer.java: chấp nhận STATE_NO_STATIC_DATA, gọi write(PCM), xác nhận STATE_INITIALIZED sau khi nạp.
- Kiểm tra trạng thái tua lại và âm lượng; ghi thông báo lỗi rõ hơn.
- Giữ nguyên tiếng bíp 4 tiếng theo Web Audio (và tiếng 2 nhịp ổ gà), các cấu hình và OTA.
- Bump native APK thành 1.0.10 (versionCode 11) để dễ phân biệt bản cũ.
- Thêm kiểm thử mô phỏng đúng luồng trạng thái MODE_STATIC.

## Đưa lên GitHub
Cách đơn giản: thay đủ 3 file sau:
1. android/app/src/main/java/org/smoodri/app/NativeAlertPlayer.java
2. android/app/src/main/java/org/smoodri/app/MainActivity.java (chỉ tăng số phiên bản chẩn đoán)
3. android/app/build.gradle (tăng phiên bản APK)

Có thể bỏ qua hai mục tăng phiên bản nếu chỉ muốn sửa đúng một file Java.
Sau đó commit và chạy workflow Build SmooDri APK, cài APK mới.
KHÔNG thể sửa AudioTrack cũ bằng OTA web vì mã này nằm trong APK native.

## Kiểm tra
1. Mở SmooDri trên điện thoại, tăng âm lượng đa phương tiện.
2. Trong SETTING nhấn NGHE THỬ ÂM NỀN ANDROID. Nếu nghe được 4 tiếng bíp, lỗi khởi tạo AudioTrack đã được xử lý trên thiết bị.
3. Nhấn BẮT ĐẦU, chờ cảm biến cân chỉnh, rồi nhấn Home.
4. Chỉ khi giá trị đo vượt ngưỡng mới có tiếng bíp nền (đúng thiết kế).
5. Nếu chưa được, xem KIỂM TRA DỊCH VỤ & ÂM THANH và gửi lỗi âm nền mới để phân biệt lỗi còn lại.

## Hạn chế kiểm thử
Tự động kiểm tra được Java với lớp Android mô phỏng và repo web. Không có Android SDK hoặc điện thoại để build APK và nghe tiếng thật trong môi trường này.
