# KCFF v3.0 — Quản lý kim cương Free Fire offline

Ứng dụng Android native chạy offline hoàn toàn, không có quyền Internet.

## Điểm mới v3

- Nhập **thẻ tuần đang chạy** với số ngày còn lại (1–7), **không cộng 100 KC ban đầu**.
- Nhập tương tự cho thẻ tháng đang chạy (1–30), không cộng 500 KC ban đầu.
- Thống kê riêng KC nhận từ **thẻ tuần** và **thẻ tháng**, gồm toàn thời gian, tháng hiện tại và phần từ thẻ nhập thủ công.
- Nhắc nhận KC hằng ngày bằng notification offline, có chọn giờ và tự khôi phục sau khi khởi động lại máy.
- Dashboard 5 tab: Tổng quan / Thẻ / Chi tiêu / Tiết kiệm / Thống kê.
- Nhận tất cả thẻ trong ngày.
- Ngân sách chi tiêu theo nhóm, cảnh báo vượt ngân sách, hoàn tác khoản chi.
- Chiến dịch tiết kiệm có deadline và gợi ý KC/ngày.
- Backup/restore JSON cục bộ.

## Logic thẻ

- Thẻ tuần mới: +100 KC ngay, sau đó 50 KC/ngày × 7.
- Thẻ tháng mới: +500 KC ngay, sau đó 70 KC/ngày × 30 = 2.600 KC.
- Thẻ nhập thủ công: chỉ quản lý số ngày còn lại, không cộng thưởng ban đầu.

## Quyền

- Không có `android.permission.INTERNET`.
- `POST_NOTIFICATIONS`: để nhắc nhận KC trên Android mới.
- `RECEIVE_BOOT_COMPLETED`: để khôi phục lịch nhắc sau khi khởi động lại máy.

Dữ liệu lưu trong `SharedPreferences` trên thiết bị.
