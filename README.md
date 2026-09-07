# KCFF 2.0 — Free Fire Diamond Manager Offline

KCFF là app Android native để quản lý kim cương Free Fire hoàn toàn offline. App không khai báo quyền Internet, không đăng nhập Garena và không gửi dữ liệu ra ngoài thiết bị.

## Tính năng
- Dashboard: KC tổng, KC sẵn, KC đang tiết kiệm, KC thẻ còn chờ nhận.
- Thẻ tuần: +100 KC ngay, 50 KC/ngày × 7 ngày, tổng 450 KC.
- Thẻ tháng: +500 KC ngay, 70 KC/ngày × 30 ngày, tổng 2.600 KC.
- Nhận từng thẻ hoặc nhận tất cả KC thẻ trong ngày.
- Quản lý chi tiêu theo danh mục và ngân sách tháng.
- Hoàn tác khoản chi và hoàn KC về số dư.
- Chiến dịch tiết kiệm có mục tiêu, deadline, số KC cần giữ mỗi ngày, gửi/rút KC.
- Thống kê KC vào/ra theo tháng, biểu đồ chi tiêu 7 ngày, phân bổ theo danh mục.
- Backup/restore JSON bằng Android Storage Access Framework, không cần quyền bộ nhớ.
- Dữ liệu lưu cục bộ bằng SharedPreferences.

## Build
GitHub Actions build `app-debug.apk` sau mỗi lần push lên `main`.

```bash
gradle :app:assembleDebug
```

Yêu cầu Android 6.0 (API 23) trở lên.
