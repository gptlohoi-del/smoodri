# SmooDri v1.0.7 — Sửa PiP / Overlay / âm thanh nền và GPS

**Bản sửa 1.0.7:** Chờ Foreground Service thật sự khởi động, dùng GPS native để không báo sai quyền WebView, chuyển tiếng bíp nền qua âm lượng Media, bổ sung KIỂM TRA ANDROID trong SETTING và bỏ `km/h` màu đen. Xem [HUONG_DAN_SUA_LOI_V1_0_7.md](HUONG_DAN_SUA_LOI_V1_0_7.md).

# SmooDri v1.0.6 — TỐC ĐỘ / TRẠNG THÁI / CỐC G + âm thanh nền

Phiên bản này giữ One View, OTA, Foreground Service, PiP và Overlay, bổ sung chế độ thứ ba **Chỉ âm thanh nền** (không hiện cửa sổ nổi). Bố cục PiP/Overlay mặc định chỉ có TỐC ĐỘ, TRẠNG THÁI, CỐC G-force; có thể bật riêng các chỉ số GA/PHANH/CUA/Ổ GÀ/GPS.

**Đã loại bỏ hoàn toàn hệ thống điểm êm ái và cài đặt hồi phục điểm.** Nút MỨC ÊM ÁI vẫn tồn tại vì nó dùng để chỉnh độ nhạy của các ngưỡng cảnh báo, không phải điểm số.

- TỐC ĐỘ: bật/tắt và chỉnh cỡ chữ riêng.
- TRẠNG THÁI: bật/tắt, cỡ chữ, thời gian giữ cảnh báo theo `alertHoldTime`.
- CỐC G: bật/tắt, đường kính và vị trí G-force theo dữ liệu cảm biến native.
- PiP/Overlay: có thêm các thông số chi tiết khi cần.
- Chỉ âm thanh nền: sau BẮT ĐẦU, nhấn nút ♪ NỀN hoặc Home để về màn hình ngoài. Foreground Service vẫn đo và phát âm báo khi vượt ngưỡng (cần bật âm báo và cấp quyền thích hợp).
- Bản giao diện thuần web không tự tạo nền native trên trình duyệt. Các tính năng Android native yêu cầu build lại APK.

**Lưu ý an toàn:** Đây là thông tin tham khảo, không phải thiết bị bảo đảm an toàn giao thông. Không nhìn hoặc điều chỉnh ứng dụng trong lúc lái xe.

# SmooDri — APK native PiP / Overlay + Foreground Service + OTA web

**Bản hợp nhất 1.0.6:** Phiên bản này kết hợp mã web One View, cài đặt chung/chỉnh trực tiếp, OTA qua GitHub Pages và phần Android native PiP/Foreground Service. Không thay thế folder `android` bằng một bản OTA web cũ không có PiP.

- APK `SmooDri-v1.0.6-debug.apk` được tạo khi chạy `.github/workflows/build-apk.yml` (nếu build thành công).
- Cập nhật giao diện web: `.github/workflows/deploy-web-ota.yml` → Pages.
- PiP native và Overlay: `MainActivity.java`, `DriveMonitorService.java`, `NativePipView.java`, `NativeOverlayController.java`, `AndroidManifest.xml`.
- **Mới 1.0.6:** Trong SETTING chọn PiP / Overlay / Chỉ âm thanh nền. Overlay chỉnh kích thước dp và kéo/thu phóng trực tiếp; cần quyền SYSTEM_ALERT_WINDOW. PiP/Overlay mặc định hiển thị TỐC ĐỘ, TRẠNG THÁI và CỐC G. Xem `HUONG_DAN_PIP_OVERLAY.md`.
- **Mới 1.0.4:** Bỏ giới hạn MIN/MAX ẩn trong CÀI ĐẶT NÂNG CAO (23 thông số số); Android native nhận cỡ chữ PiP lớn hơn ngưỡng cũ. Khôi phục cấu hình gốc vẫn hoạt động.
- **Từ 1.0.3:** PiP có 6 cỡ chữ độc lập (Tốc độ, GA, Phanh, Cua, Ổ gà, GPS); bỏ Điểm chuyến đi tích lũy trên Web và PiP, giữ Điểm hiện tại ở One View.
- Cách sử dụng và kiểm tra: `HUONG_DAN_PIP_OTA_HOP_NHAT.md`.

---

# SmooDri 8.0 — Trợ lý lái xe êm ái

## Cài đặt / cập nhật

1. Sao lưu phiên bản trước.
2. Giải nén và **tải toàn bộ nội dung trong thư mục `smoodri-main`** lên thư mục gốc website (hoặc cùng đường dẫn với bản cũ). Giữ nguyên cấu trúc thư mục `icons/`.
3. Mở website bằng **HTTPS** (ví dụ GitHub Pages). Cảm biến trên điện thoại có thể không hoạt động qua HTTP hoặc khi chưa được cấp quyền.
4. Tắt/mở lại tab hoặc ứng dụng đã cài để nhận bản cập nhật. Nếu màn hình vẫn là bản cũ, đóng mọi tab của ứng dụng rồi mở lại trực tuyến.
5. Cố định điện thoại và **dừng xe** khi bấm Bắt đầu/cân chỉnh lại 0G. Không thao tác ứng dụng trong lúc điều khiển xe.

## Thay đổi chính

- Nút **− / +** thay đổi mức độ êm ái theo bước 5, trong khoảng 20–100. **+** yêu cầu êm hơn (ngưỡng G thấp hơn), **−** linh hoạt hơn (ngưỡng G cao hơn).
- Bỏ cài đặt ngưỡng bắt đầu phát tiếng bíp, cảnh báo phát từ **100%** của từng chỉ số. Âm cảnh báo được giới hạn cường độ và có khoảng nghỉ.
- Trong cài đặt có thể chọn 1–4 thanh hiển thị, ở cả Thanh ngang và Vòng tròn. **Ẩn thanh không dừng đo, tính điểm và cảnh báo**.
- Chế độ cài đặt nâng cao có 5 ngưỡng G/G/s riêng. Điều chỉnh các giá trị này chuyển sang `Tùy chỉnh`; nút ± hoặc thanh mức êm ái sẽ áp dụng lại ngưỡng đồng bộ.
- Chỉ hiển thị **Điểm hiện tại** (có hồi phục); bỏ Điểm chuyến đi tích lũy ở One View và PiP. Đây là thuật toán đánh giá thử nghiệm, chưa được chuẩn hóa.
- Cân chỉnh trung bình nhiều mẫu sau thời gian chờ, phát hiện mất dữ liệu cảm biến, không nhận diện lạng lách chỉ từ một cú cua hoặc một đỉnh jerk.
- Độ dốc chỉ hiển thị dạng **tương đối / ước tính** khi có dữ liệu gia tốc tuyến tính; không coi là đo độ dốc tuyệt đối.
- Icon PNG/SVG cục bộ và Service Worker cache phiên bản 8 hỗ trợ hoạt động offline sau khi tải trọn các tệp cốt lõi.

## Lưu ý quan trọng

- Không thể kiểm định độ chính xác lực G, độ dốc và mức êm ái nếu chưa thử trên thiết bị thật, ở các vị trí đặt điện thoại và loại đường/xe khác nhau.
- Độ dốc tuyệt đối, tư thế lắp điện thoại, độ ổn định cảm biến, hạn chế hoạt động nền và phát âm thanh phụ thuộc thiết bị/trình duyệt.
- Kết quả chỉ để tham khảo, không dùng để quyết định tốc độ lái an toàn.
- Dữ liệu cài đặt dùng `localStorage` ở khóa `smoodri_cfg_v8`; chương trình đọc lại bản cũ `smoodri_cfg_v7` nếu có.

## Danh sách file

- `index.html`: màn hình chính và cài đặt
- `style.css`: giao diện và bố cục mobile one-view
- `app.js`: đo cảm biến, đánh giá, cảnh báo và lưu cài đặt
- `manifest.json`: cài đặt PWA
- `sw.js`: cache offline
- `icons/`: icon PNG chuẩn và SVG nguồn

## Cập nhật OTA tự động qua GitHub Pages

Đã tích hợp workflow `Publish SmooDri Web OTA (Pages)` và mục cập nhật trong CÀI ĐẶT CHUNG. Xem [`HUONG_DAN_OTA_SMOODRI.md`](HUONG_DAN_OTA_SMOODRI.md) để bật GitHub Pages (Source: GitHub Actions), kiểm tra offline và phân biệt cập nhật web với APK native.
