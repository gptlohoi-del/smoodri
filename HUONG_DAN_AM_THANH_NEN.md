# HƯỚNG DẪN SỬ DỤNG CHẾ ĐỘ CHỈ ÂM THANH NỀN — SmooDri 1.0.9

## 1. Kiểm tra hai bộ phát âm

- Trong ứng dụng: **CÀI ĐẶT → NGHE THỬ 4 TIẾNG BÍP**. Đây là âm Web Audio gốc của SmooDri.
- Khi chạy APK: **CÀI ĐẶT → NGHE THỬ ÂM NỀN ANDROID**. Đây là âm PCM mà Android sẽ phát sau khi bạn bấm Home.
- Cả hai dùng âm lượng và kiểu âm đã chọn. Android có thể có chênh lệch nhỏ về chất âm do Web Audio và bộ phát native sử dụng hai bộ máy tổng hợp khác nhau, nhưng cao độ và nhịp được ghép tương ứng.
- Nếu cả hai nút đều không có tiếng, kiểm tra âm lượng **Media**, chế độ im lặng, tai nghe Bluetooth và quyền/giới hạn âm thanh thiết bị.

## 2. Bắt đầu

1. Bật **ÂM THANH CẢNH BÁO**.
2. Nhấn **BẮT ĐẦU** và giữ máy cố định cho đến khi cân chỉnh xong.
3. Nhấn **Home** hoặc **♪ CHẠY NỀN** để đưa SmooDri ra nền.
4. Khi gia tốc, phanh, cua, ổ gà hoặc lắc ngang vượt ngưỡng thì điện thoại mới bíp. Không có tiếng khi chưa có cảnh báo.

## 3. Khi chạy nền

Foreground Service tiếp tục lấy mẫu gia tốc. Một thông báo “SmooDri đang theo dõi” xuất hiện và có nút Dừng theo dõi. Không có PiP, không có Overlay.

Khi mở lại SmooDri, Web Audio tiếp tục đảm nhận âm báo trong giao diện. Khi chuyển ra nền, Android native phát âm cùng kiểu đã chọn. Không dùng hai bộ phát đồng thời.

Nếu chỉ muốn dừng ứng dụng, nhấn DỪNG hoặc dùng thông báo Android. Việc **vuốt bỏ khỏi màn hình ứng dụng gần đây** có thể làm dừng dịch vụ; dùng Home để theo dõi nền. Thiết bị do quản trị viên quản lý hoặc có chính sách tiết kiệm pin có thể chặn dịch vụ.

## 4. GPS và OTA

Cảnh báo chuyển động **không phụ thuộc quyền GPS**. Nếu muốn xem tốc độ GPS trên One View, cấp quyền Vị trí riêng. Nếu GPS chưa sẵn sàng, vẫn có thể nhận cảnh báo dựa trên cảm biến gia tốc.

Đây là thay đổi native, bắt buộc build APK v1.0.9 một lần. Bản web mới cần được phát hành qua GitHub Pages trước để không tải lại giao diện cũ.

## 5. Kiểm tra nếu chưa nghe cảnh báo

- Nhấn hai nút nghe thử để xác định bộ phát nào bị lỗi.
- Mở **KIỂM TRA DỊCH VỤ & ÂM THANH**: kiểm tra dịch vụ ĐANG CHẠY, Cảm biến CÓ, Âm nền BẬT. Khi đang chạy mà chưa có sự kiện vượt ngưỡng, “Lần cảnh báo: CHƯA CÓ” là bình thường.
- Đặt MỨC ÊM ÁI theo nhu cầu nhưng không chỉnh ngưỡng cực đoan để ép cảnh báo khi lái xe; hãy thử ở nơi an toàn và khi xe đỗ.
- Nếu có lỗi, chụp màn hình phần KIỂM TRA DỊCH VỤ & ÂM THANH và log Actions để đối chiếu.
