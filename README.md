# FF Y9 Booster VIP Pro X2

V2 là một **game-session telemetry/tuner không-root** cho Huawei Y9 2019 / Kirin 710 và Android 6+.

## V2 thay đổi gì
- Bỏ kiểu "RAM cleaner" ép kill app nền làm tính năng chính. Android tự quản lý cached process; kill bừa có thể làm app phải khởi động lại và tốn tài nguyên hơn.
- Preflight Score 0–100 dựa trên RAM trống, nhiệt pin, Power Saver, mạng và RTT*.
- 3 preset có hành vi thật:
  - **TURBO NET:** Android 10+ dùng `WIFI_MODE_FULL_LOW_LATENCY`; Android 9/Y9 2019 fallback `WIFI_MODE_FULL_HIGH_PERF`.
  - **BALANCED:** Wi-Fi high-performance, probe chậm hơn.
  - **COOL:** không giữ Wi-Fi performance lock.
- HUD nổi kéo được, chạm để thu gọn/mở rộng.
- HUD: Display Hz, RTT*, CPU (nếu firmware cho đọc `/proc/stat`), RAM trống, nhiệt pin, % pin, thermal status, loại mạng, session timer.
- Lưu tóm tắt phiên: thời gian, nhiệt max, RAM thấp nhất, RTT* trung bình.
- Target Android 35, khai báo foreground service `specialUse` và package visibility cho Free Fire/Free Fire MAX.

## FPS đâu?
Một app Android bình thường không-root không có quyền đọc frame timing nội bộ của Free Fire. V1 dùng nhịp Choreographer/VSYNC nên có thể gây hiểu nhầm; V2 bỏ số đó và hiển thị **Display Hz** chính xác thay vì giả nó là FPS game.

## RTT* là gì?
RTT* đo thời gian TCP connect tới endpoint Internet công cộng (Cloudflare/Google) để phát hiện đường truyền đang chậm. Nó **không phải ping tới server Garena**.

## Không làm
- Không sửa APK/OBB/data Free Fire.
- Không inject, hook, cheat, macro hoặc bypass anti-cheat.
- Không quảng cáo unlock 90/120 FPS cho màn hình 60 Hz.

## Build
GitHub Actions build debug APK sau mỗi push lên `main`.
