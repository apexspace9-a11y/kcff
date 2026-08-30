# FF Y9 Booster VIP Pro

Booster Android không-root, tối ưu riêng cho Huawei Y9 2019 / Kirin 710 nhưng vẫn chạy trên Android 6+.

## Có gì thật sự hoạt động
- Quick Boost: yêu cầu Android dừng các process nền mà ứng dụng thường được phép dừng.
- Game Session: giữ Wi‑Fi ở chế độ high-performance khi HUD đang chạy để giảm power-save latency trên Wi‑Fi.
- HUD nổi: RAM trống, nhiệt độ pin, CPU (nếu `/proc/stat` đọc được), loại mạng và FPS*.
- Launch Free Fire (`com.dts.freefireth`) hoặc Free Fire MAX (`com.dts.freefiremax`).
- Preset Y9 2019: nhắc cấu hình đồ họa thấp khi nhiệt/RAM không đẹp.

## FPS* nghĩa là gì?
Android không-root không cho một app bình thường đọc frame timing nội bộ của app khác. Vì vậy FPS* trong HUD là nhịp VSYNC/Choreographer mà booster quan sát được. Nó hữu ích để thấy màn hình/overlay có đang hụt nhịp không, **không phải FPS engine chính xác của Free Fire**.

## Không làm
- Không sửa APK/OBB/data của Free Fire.
- Không inject, hook, cheat, macro hoặc bypass anti-cheat.
- Không quảng cáo "unlock 120 FPS" giả. Huawei Y9 2019 là màn hình 60 Hz.

## Build
GitHub Actions build debug APK sau mỗi push lên `main`. Artifact tên `ff-y9-booster-apk`.
