# SMOODRI v1.0.5 — HƯỚNG DẪN DÙNG CẢ PiP VÀ OVERLAY

## 1. Chọn chế độ ngay trong SETTING

Mở **CÀI ĐẶT CHUNG → Cửa sổ nổi PiP / Overlay** rồi chọn:

- **PiP** (mặc định): Android quản lý kích thước. Có thể thử phóng to/thu nhỏ bằng thao tác mà Android cho phép.
- **Overlay**: cửa sổ nổi độc lập phía trên ứng dụng khác, thay đổi kích thước và vị trí. Chỉ hoạt động trong APK Android.

Ứng dụng **chỉ bật một chế độ ở một thời điểm**, không hiển thị cả PiP lẫn Overlay đồng thời. Lựa chọn được tự lưu trong `smoodri_cfg_v8`.

## 2. Quyền Overlay (bắt buộc khi chọn Overlay)

1. Chọn Overlay trong CÀI ĐẶT CHUNG.
2. Nhấn **CẤP QUYỀN HIỂN THỊ TRÊN ỨNG DỤNG KHÁC**.
3. Trong cài đặt Android, cấp quyền cho SmooDri, sau đó quay lại ứng dụng.
4. Nếu thiết bị bị quản trị viên giới hạn, quyền có thể không cấp được. Khi đó chuyển về **PiP**.

Quyền chỉ được yêu cầu khi người dùng chọn và khởi chạy Overlay. Overlay sử dụng `TYPE_APPLICATION_OVERLAY` với quyền `SYSTEM_ALERT_WINDOW`.

## 3. Sử dụng

1. Đỗ xe an toàn, bật GPS, mở SmooDri và nhấn **BẮT ĐẦU**.
2. Nhấn **▣ PiP** hoặc **▣ Overlay** ở thanh trên, tùy lựa chọn trong SETTING. Hoặc trở về Home khi đang theo dõi.
3. Chế độ PiP: Android quyết định vị trí/kích thước; ứng dụng đề xuất tỷ lệ 16:9, 4:3 hoặc 1:1.
4. Chế độ Overlay: kéo vùng **≡ SmooDri** để di chuyển; kéo góc **◢** để chỉnh rộng/cao.
5. Trong Overlay, nút **MỞ** trở lại ứng dụng; **×** dừng phiên đo và đóng cửa sổ.
6. Các cỡ chữ, màu nền, màu chữ, mục hiển thị của 6 thông số **TỐC ĐỘ / GA / PHANH / CUA / Ổ GÀ / GPS** dùng chung giữa hai chế độ.

Chiều rộng và chiều cao Overlay trong SETTING dùng đơn vị **dp** (điểm ảnh độc lập mật độ). Kích thước được Android chặn theo diện tích màn hình còn lại. Vị trí và kích thước kéo trực tiếp được ghi nhớ, đồng bộ trở lại SETTING khi bạn mở SmooDri.

Nếu kéo cửa sổ quá nhỏ hoặc chọn cỡ chữ quá lớn, các chữ có thể tự co hoặc một vài hàng không đủ chỗ. Tăng chiều cao Overlay hoặc bỏ bớt mục hiển thị.

## 4. Xây dựng và OTA

Vì thêm quyền Android và `NativeOverlayController.java`, **phải build và cài APK v1.0.5 ít nhất một lần**:

- Commit toàn repo lên GitHub, vào **Actions → Build SmooDri APK**.
- File tạo ra có tên dự kiến `SmooDri-v1.0.5-debug.apk`.
- Nếu APK debug cũ ký khác khóa mới, việc cài đè có thể thất bại. Muốn phát hành ổn định, nên dùng khóa release cố định.
- Khi APK nền đã có Overlay native, các chỉnh sửa HTML/CSS/JS thông thường vẫn có thể OTA qua **Publish SmooDri Web OTA (Pages)**.
- OTA web **không thể tự bổ sung quyền Overlay hay mã native** cho APK cũ.

## 5. Kiểm tra trước khi sử dụng

- Kiểm tra PiP khi nhấn Home.
- Kiểm tra quyền Overlay, nhấn Home, kéo và đổi kích thước, quay lại rồi mở SETTING xem giá trị được ghi nhớ.
- Kiểm tra dừng từ nút × trong Overlay và từ thông báo Android.
- Kiểm tra OTA với Internet và mở lại offline.
- Đảm bảo One View vẫn hiển thị bình thường, bốn chỉ số và GPS còn đo chính xác.

**Lưu ý:** Đây là ứng dụng đo tham khảo, không phải thiết bị lái xe an toàn được kiểm định. Không điều chỉnh ứng dụng khi đang điều khiển phương tiện. Trên một số thiết bị quản trị, Overlay và/hoặc PiP có thể bị hạn chế bởi chính sách hệ thống.
