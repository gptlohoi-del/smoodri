# Tạo APK SmooDri với icon xe màu vàng

## 1. Trường hợp repo chỉ có mã nguồn web

Bản ZIP gốc của người dùng không có thư mục `android/` hoặc `.github/workflows/`.
Bộ này bổ sung một ứng dụng Android **độc lập** dạng WebView. Không thay đổi logic
đo lực G/GPS, cài đặt, hay các trang HTML/JS/CSS đang có.

> LƯU Ý: APK độc lập này không phải module SmooDri chạy trong App Hub.
> Nó không có Foreground Service/PiP native của App Hub. Không nên cài đè
> một APK SmooDri khác mà chưa kiểm tra applicationId và chữ ký ký APK.

## 2. Cách đưa lên GitHub

- Giải nén ZIP và chép **toàn bộ nội dung bên trong** vào repo `gptlohoi-del/smoodri`.
- Các file web đang có được giữ nguyên; thay các file icon, `manifest.json`, `sw.js`
  và thêm các file Android/GitHub Actions trong ZIP.
- Commit lên nhánh `main`.
- Vào Actions > Build SmooDri APK > Run workflow.
- Tải artifact `SmooDri-v1.0.0-debug.apk` (GitHub có thể nén artifact trong ZIP).

## 3. Đổi phiên bản trong tương lai

Sửa `android/app/build.gradle`:

```
versionCode 2
versionName '1.0.1'
```

Sau đó chạy lại workflow. Tên APK là `SmooDri-v1.0.1-debug.apk`.
`versionCode` luôn phải tăng nếu cài đè một bản APK cũ.

## 4. Tên và icon

- Tên ứng dụng trên màn hình Android: `android/app/src/main/res/values/strings.xml`.
- Icon Android: `android/app/src/main/res/mipmap-*/ic_launcher*.png` và
  `android/app/src/main/res/drawable/ic_launcher_foreground.png`.
- Tên PWA: `manifest.json`.
- Icon PWA: `icons/` (đã đồng bộ cả các icon tương ứng ở thư mục gốc).
- Chỉ có **một** ảnh xe PNG được cung cấp; đã dùng ảnh này cho icon Android và PWA.

## 5. Web OTA và ngoại tuyến

APK truy cập `https://gptlohoi-del.github.io/smoodri/` và có sẵn tài nguyên web
được đóng gói trong APK khi không kết nối được. Để nhận cập nhật web online,
bạn phải bật GitHub Pages cho repo `smoodri` (thường chọn main/root).
Cơ chế Service Worker của web có thể cần đóng/mở app thêm lần nữa để áp dụng cache mới.
Nếu GitHub Pages không khả dụng, APK hiển thị phiên bản web được đóng gói.
Điều này **không** phải cập nhật mã Android native qua OTA.

## 6. Khóa ký

Bản build mặc định là Debug APK. Chưa sử dụng khóa JKS release cố định.
Vì vậy cài đè giữa các runner khác nhau có thể thất bại do khác chữ ký.
Muốn cập nhật Android lâu dài nên cấu hình ký Release APK bằng cùng khóa JKS.

## 7. Giới hạn

Chưa có xác nhận build Android thực tế tại đây (máy xử lý không có SDK/Gradle).
Các chức năng cảm biến/GPS cần kiểm tra bằng điện thoại thật và cấp quyền GPS.
PiP/Foreground Service native cần mã nguồn App Hub hoặc một dự án Android
SmooDri native riêng; không thể sao chép những tính năng này chỉ từ ZIP PWA.
