# SMOODRI v1.0.6 — TỐC ĐỘ / TRẠNG THÁI / CỐC G + CHỈ ÂM THANH NỀN

## Các thay đổi

- Loại bỏ hệ thống điểm hiện tại, hồi phục điểm và cấu hình điểm ra khỏi HTML/CSS/JS. Nút MỨC ÊM ÁI vẫn được giữ vì đó là điều chỉnh độ nhạy ngưỡng cảnh báo, KHÔNG phải điểm số.
- PiP/Overlay mặc định chỉ hiện TỐC ĐỘ, TRẠNG THÁI và CỐC G-force (cốc có bi chuyển động theo gia tốc). Có thể bật thêm GA, PHANH, CUA, Ổ GÀ, GPS.
- Cài đặt từng mục: hiển thị hoặc ẩn; cỡ chữ TỐC ĐỘ, cỡ chữ TRẠNG THÁI, đường kính CỐC G, màu nền/chữ chung; các chỉ số chi tiết có cỡ chữ riêng như phiên bản trước.
- Chế độ thứ ba: Chỉ âm thanh nền. Không hiện PiP/Overlay, nhưng Foreground Service Android tiếp tục đo và phát tiếng bíp khi vượt ngưỡng. Nếu tắt âm thanh trong SETTING, chế độ này không thể phát cảnh báo.
- Trạng thái được giữ theo thông số `alertHoldTime` và dựa trên mức vượt ngưỡng; đây là đánh giá tham khảo, không phải cảnh báo an toàn đã được kiểm định.
- Tăng độ ổn định khi tắt màn hình bằng PARTIAL_WAKE_LOCK chỉ trong chế độ âm thanh nền và khi âm báo bật; quyền WAKE_LOCK là quyền thông thường. Khóa được nhả khi dừng phiên; có thể tăng hao pin.
- Vẫn giữ OTA web, PiP native, Overlay, Foreground Service và One View.

## Cập nhật GitHub

Cách 1: Sao lưu repo rồi giải nén **ZIP toàn repo**, tải lên GitHub đúng cấu trúc thư mục gốc, gồm .github/workflows.
Cách 2: Giải nén **ZIP chỉ file thay**, ghi đè các file vào đúng đường dẫn ghi trong ZIP, giữ nguyên các file khác.

Sau khi Commit, vào **Actions → Build SmooDri APK**. Artifact dự kiến có file `SmooDri-v1.0.6-debug.apk` bên trong. Cần cài APK mới vì sửa native Java và AndroidManifest.xml; cập nhật web OTA đơn thuần không thể bổ sung chế độ nền và vẽ cốc G.

## Sử dụng

1. Đỗ xe an toàn, mở SmooDri, chọn **CÀI ĐẶT CHUNG → Kiểu cửa sổ khi ra màn hình ngoài**.
2. Chọn **PiP / Overlay / Chỉ âm thanh nền**. Với PiP/Overlay, chọn những thông tin cần hiển thị bên dưới, chỉnh cỡ chữ TỐC ĐỘ, TRẠNG THÁI, đường kính CỐC G.
3. Với **Chỉ âm thanh nền**, bật **Âm thanh cảnh báo**; nhấn **BẮT ĐẦU**, chờ cân chỉnh, sau đó dùng nút **♪ NỀN** hoặc nút Home. Foreground Service vẫn hoạt động kèm thông báo Android, không hiện PiP/Overlay.
4. Nhấn DỪNG trong ứng dụng hoặc hành động Dừng theo dõi ở thông báo Android để kết thúc. Vuốt bỏ ứng dụng khỏi Recent Apps có thể dừng dịch vụ.

## Giới hạn kiểm thử

Đã kiểm tra `node --check`, 18 bài kiểm tra Python, giao diện ở độ rộng 320 / 390 / 720 px trong Chromium nội bộ, và biên dịch riêng `NativePipView.java` với các lớp Android mô phỏng. CHƯA build APK bằng Android SDK hoặc kiểm tra Foreground Service, PiP/Overlay và cảnh báo khi tắt màn hình trên điện thoại thật. GitHub Actions và kiểm thử Android là bước xác nhận tiếp theo.

Trong lúc lái xe, không tương tác với SmooDri. Cảnh báo chỉ mang tính tham khảo, không đảm bảo độ an toàn của xe.
