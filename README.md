# FF Y9 Booster VIP Pro X4

V4 là một **game-session stability monitor/tuner không-root** cho Huawei Y9 2019 / Kirin 710, vẫn chạy từ Android 6+.

## V4 nâng cấp gì

- **Adaptive+ thermal guard:** tự chọn TURBO / BALANCED / COOL theo nhiệt, Android thermal status và Power Saver.
- **Dynamic Probe:** khi đường truyền ổn, V4 dùng ít probe hơn và đo thưa hơn; khi RTT*/Jitter*/Failure* xấu, nó chuyển sang deep probe nhanh hơn.
- **Memory Pressure:** dùng `ActivityManager.MemoryInfo` gồm `availMem`, `totalMem`, `threshold` và `lowMemory`, thay vì đánh giá chỉ bằng "RAM trống".
- **Rolling Stability Score:** HUD chấm 0–100 theo nhiệt, RAM pressure và mạng trong phiên.
- **Session Grade:** khi tắt session, V4 lưu Grade A+/A/B/C/D và nguyên nhân chính: MẠNG / NHIỆT / RAM / ỔN ĐỊNH.
- Lưu **5 phiên gần nhất** và có nút chia sẻ báo cáo dạng text.
- Lưu thêm CPU trung bình/cao nhất, RAM pressure cao nhất, pin đầu/cuối phiên, tỷ lệ mẫu mạng bất ổn.
- Vẫn có Display Hz, pin, dòng sạc (nếu firmware cung cấp), storage trống và HUD kéo/thu gọn.

## Wi-Fi mode

- Android 10+ TURBO yêu cầu `WIFI_MODE_FULL_LOW_LATENCY`.
- Android cũ dùng `WIFI_MODE_FULL_HIGH_PERF`.
- COOL không giữ Wi-Fi lock.
- Hỗ trợ thực tế phụ thuộc Android/firmware/phần cứng. Wi-Fi lock không phải phép "giảm ping server" và có thể tốn pin.

## RTT* / Jitter* / Failure*

Các chỉ số có dấu `*` đến từ TCP connect tới endpoint Internet công cộng. Chúng giúp phát hiện đường truyền chậm hoặc thiếu ổn định nhưng **không phải ping, jitter hay packet loss trực tiếp tới server Garena**.

## FPS

V4 không hiển thị "FPS game" giả. App Android không-root bình thường không được đọc frame timing nội bộ của Free Fire. HUD hiển thị **Display Hz** thay cho việc đổi tên VSYNC thành FPS.

## Không làm

- Không sửa APK/OBB/data Free Fire.
- Không inject, hook, macro, cheat hoặc bypass anti-cheat.
- Không ép xung CPU/GPU.
- Không quảng cáo unlock 90/120 FPS giả.

## Build

GitHub Actions build debug APK sau mỗi push lên `main`.

Artifact: `ff-y9-booster-v4-apk`
APK: `FF-Y9-Booster-VIP-Pro-X4-v4.0.0.apk`
