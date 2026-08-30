# FF Y9 Booster VIP Pro X3

V3 là một **game-session telemetry/tuner không-root** cho Huawei Y9 2019 / Kirin 710 và Android 6+.

## V3 nâng cấp gì
- **ADAPTIVE mode** tự chuyển TURBO / BALANCED / COOL theo nhiệt pin, Android thermal status và Power Saver.
- Có **hysteresis nhiệt** để tránh nhảy mode liên tục quanh một ngưỡng.
- **Thermal guard bắt buộc:** nếu máy quá nóng hoặc Android báo thermal `SEVERE` trở lên, session tạm hạ về COOL kể cả khi người dùng chọn TURBO.
- **Multi-probe network:** nhiều TCP connect tới các endpoint Internet công cộng, tính RTT* median, Jitter* và Failure*.
- HUD V3: profile hiệu lực, Display Hz, RTT*, Jitter*, Failure*, CPU, RAM, nhiệt, pin, thermal status, mạng, Wi-Fi lock và session timer.
- HUD kéo được; chạm để thu gọn/mở rộng.
- Lưu **3 phiên gần nhất**: thời gian, nhiệt max, RAM thấp nhất, RTT* trung bình, Jitter* trung bình, Failure* cao nhất và số lần AUTO-COOL.
- Preflight Score chấm RAM, memory pressure, nhiệt, thermal status, Power Saver và chất lượng mạng.
- Hiển thị trạng thái sạc, dòng sạc nếu firmware hỗ trợ và dung lượng lưu trữ trống.
- Shortcut tới Battery Saver, Battery Optimization, Display Settings và App Settings.

## Các preset
- **ADAPTIVE:** mặc định. Tự chọn mode theo trạng thái thiết bị.
- **TURBO NET:** Android 10+ dùng `WIFI_MODE_FULL_LOW_LATENCY`; Android cũ fallback `WIFI_MODE_FULL_HIGH_PERF`.
- **BALANCED:** Wi-Fi high-performance với nhịp probe vừa phải.
- **COOL:** không giữ Wi-Fi performance lock, probe thưa hơn.

## RTT* / Jitter* / Failure* nghĩa là gì?
Các chỉ số có dấu `*` được đo từ TCP connect tới endpoint Internet công cộng như Cloudflare, Google, Quad9 và OpenDNS.

- RTT*: trung vị thời gian connect thành công.
- Jitter*: độ lệch trung bình của các mẫu RTT quanh trung vị.
- Failure*: tỷ lệ TCP probe thất bại.

Chúng hữu ích để phát hiện đường truyền xấu nhưng **không phải ping, jitter hay packet loss trực tiếp tới server Garena**.

## FPS đâu?
Một app Android bình thường không-root không có quyền đọc frame timing nội bộ của Free Fire. V3 chỉ hiển thị **Display Hz** và không giả VSYNC thành FPS game.

## Không làm
- Không sửa APK/OBB/data Free Fire.
- Không inject, hook, cheat, macro hoặc bypass anti-cheat.
- Không ép CPU/GPU clock và không quảng cáo unlock FPS giả.

## Build
GitHub Actions build debug APK sau mỗi push lên `main`. Artifact V3 là `ff-y9-booster-v3-apk`.
