> **Cập nhật v1.0.8:** Khi nhấn Overlay, Activity được đưa ra nền trước rồi dịch vụ mới yêu cầu tạo cửa sổ. Nếu không xuất hiện, xem **KIỂM TRA ANDROID** để đọc `Lỗi Overlay`. PiP, Overlay và Chỉ âm thanh nền dùng chung 4 tiếng bíp native; nghe thử được trong SETTING. Xem `HUONG_DAN_SUA_LOI_V1_0_8.md`.

# PiP, Overlay và Chỉ âm thanh nền (v1.0.7)

Mở **CÀI ĐẶT CHUNG → Kiểu cửa sổ khi ra màn hình ngoài** và chọn:

1. **PiP** — cửa sổ do Android quản lý kích thước.
2. **Overlay** — kéo và thay đổi chiều rộng/cao; cần quyền Hiển thị trên ứng dụng khác.
3. **Chỉ âm thanh nền** — không mở PiP hay Overlay. Ứng dụng vẫn chạy Foreground Service sau khi bấm BẮT ĐẦU, để tiếp tục đo và phát âm báo khi có vi phạm ngưỡng. Khi chọn chế độ này với âm thanh bật, ứng dụng sử dụng khóa CPU từng phiên để tăng khả năng nhận cảm biến lúc tắt màn hình; tính năng này có thể tăng hao pin. Bật **Âm thanh cảnh báo** trong SETTING và lưu ý quyền thông báo/vị trí của Android.

PiP/Overlay mặc định hiển thị **TỐC ĐỘ, TRẠNG THÁI và CỐC G**. Bạn có thể bật/tắt từng thành phần và điều chỉnh cỡ chữ/kích thước. GA, PHANH, CUA, Ổ GÀ, GPS là các mục bổ sung tùy chọn. Trạng thái hiển thị cảnh báo ưu tiên theo mức vượt ngưỡng và được giữ trong khoảng thời gian cài đặt.

Sau khi chọn chế độ, bấm **BẮT ĐẦU** rồi nhấn Home hoặc nút mở chế độ tương ứng trên màn hình chính. **Không vuốt bỏ ứng dụng khỏi danh sách ứng dụng gần đây** nếu muốn dịch vụ còn hoạt động; bản Android hiện tại dừng theo dõi khi tác vụ bị loại bỏ hoặc người dùng nhấn DỪNG. Thông báo dịch vụ có nút Dừng theo dõi.

Chuyển đổi PiP/Overlay/Chỉ âm thanh nền cần cài APK mới **v1.0.7**, vì phải sửa Android native. Các tùy chọn giao diện web về sau có thể cập nhật OTA. Trên một số máy Android bị quản trị hạn chế quyền, các chức năng nền có thể không khả dụng.


## Chẩn đoán khi không có PiP, Overlay hoặc âm thanh

Vào CÀI ĐẶT CHUNG → **KIỂM TRA ANDROID / PiP / OVERLAY / GPS**. Xem `HUONG_DAN_SUA_LOI_V1_0_7.md` và sử dụng bản APK v1.0.7, không chỉ thay web OTA.
