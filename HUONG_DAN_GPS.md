# SmooDri: tốc độ GPS

- Hiển thị tốc độ dưới cốc G, không có dòng tiêu đề. Dòng trạng thái: `GPS ±?m ?Hz`.
- Trong Cài đặt có bật/tắt, cỡ chữ, màu, độ đậm, thời gian ước tính mất GPS 0–3 giây (mặc định 2).
- GPS chỉ chạy khi nhấn BẮT ĐẦU, dừng khi nhấn DỪNG. Trình duyệt sẽ hỏi quyền vị trí.
- GPS mất tạm: ước tính giới hạn dựa trên xu hướng tốc độ GPS và độ lớn gia tốc cảm biến. Không thể xác định chiều gia tốc chính xác khi máy không cố định hướng; chỉ là tham khảo.
- Khi dữ liệu cũ hơn khoảng 2.5 giây, chuyển sang trạng thái ƯỚC TÍNH tối đa 2 giây; sau đó hiển thị --.
- GPS ±?m là sai số vị trí, không phải sai số tốc độ. Hz là tần suất hai mẫu gần nhất, không phải thông số phần cứng.
- Android App Hub: GPS trong WebView có thể cần quyền LOCATION và chính sách cấp quyền phù hợp; OTA web không tự thêm được quyền Android native.
- Không dùng tốc độ này để thay đồng hồ tốc độ xe.
