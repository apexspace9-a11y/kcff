# KCFF - Quản lý kim cương Free Fire

Web app/PWA nhỏ gọn để quản lý kim cương cá nhân. Không kết nối Garena/Free Fire và không yêu cầu backend.

## Tính năng

- Tổng quan: KC tổng, KC sẵn, KC thẻ tuần còn lại, KC thẻ tháng còn lại.
- Thẻ tuần 7 ngày: +100 KC ngay, 50 KC/ngày, tổng 450 KC.
- Thẻ tháng 30 ngày: +500 KC ngay, 70 KC/ngày, tổng 2.600 KC.
- Mỗi thẻ chỉ nhận thưởng ngày tối đa một lần/ngày theo ngày trên thiết bị.
- Quản lý khoản chi, tự trừ từ KC sẵn.
- Chiến dịch tiết kiệm: tạo mục tiêu, chuyển KC vào mục tiêu, hoàn KC về ví.
- Lịch sử biến động và xuất sao lưu JSON.
- Dữ liệu lưu trong `localStorage` của trình duyệt.
- Responsive, cài được như PWA khi host qua HTTPS.

## Chạy

Có thể mở bằng một static server bất kỳ. Ví dụ:

```bash
python3 -m http.server 8080
```

Sau đó mở `http://localhost:8080`.

## Lưu ý logic

`KC tổng = KC sẵn + KC đang tiết kiệm`.

Phần `KC thẻ tuần` và `KC thẻ tháng` là số phần thưởng hằng ngày còn lại của tất cả thẻ tương ứng. Quyền lợi chưa nhận chưa được cộng vào `KC tổng` cho tới khi bấm nhận.
