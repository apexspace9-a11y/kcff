# KCFF Offline

App Android quản lý kim cương Free Fire, chạy hoàn toàn offline.

## Chức năng
- Kim cương tổng = KC sẵn + KC đang giữ trong chiến dịch tiết kiệm.
- Thêm/điều chỉnh KC sẵn thủ công.
- Thẻ tuần: +100 KC ngay, sau đó 50 KC/ngày trong 7 ngày (tổng 450 KC).
- Thẻ tháng: +500 KC ngay, sau đó 70 KC/ngày trong 30 ngày (tổng 2.600 KC).
- Mỗi thẻ chỉ nhận thưởng hằng ngày 1 lần/ngày và tự hết hạn.
- Quản lý chi tiêu KC.
- Chiến dịch tiết kiệm theo mục tiêu, chuyển KC vào quỹ và hoàn KC khi đóng.
- Lịch sử biến động.
- Toàn bộ dữ liệu lưu bằng SharedPreferences trên thiết bị.
- AndroidManifest **không có quyền INTERNET**.

## Build APK
GitHub Actions tự build `app-debug.apk` sau mỗi push vào `main`.
Mở tab **Actions** → workflow **Build Android APK** → run mới nhất → tải artifact `KCFF-offline-debug`.

## Yêu cầu
Android 6.0 (API 23) trở lên.

> KCFF là công cụ ghi chép cá nhân, không liên kết Garena/Free Fire và không đăng nhập tài khoản game.
