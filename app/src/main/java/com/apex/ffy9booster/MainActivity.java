package com.apex.ffy9booster;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREFS = "ff_y9_booster";
    static final String KEY_PROFILE = "profile";
    static final int PROFILE_TURBO = 0;
    static final int PROFILE_BALANCED = 1;
    static final int PROFILE_COOL = 2;
    static final int PROFILE_ADAPTIVE = 3;

    private static final int BG = Color.rgb(6, 9, 14);
    private static final int CARD = Color.rgb(15, 22, 31);
    private static final int CARD_2 = Color.rgb(23, 33, 45);
    private static final int CYAN = Color.rgb(0, 229, 255);
    private static final int LIME = Color.rgb(183, 255, 105);
    private static final int TEXT = Color.rgb(244, 248, 252);
    private static final int MUTED = Color.rgb(143, 159, 177);
    private static final int WARN = Color.rgb(255, 188, 74);
    private static final int RED = Color.rgb(255, 104, 112);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusValue;
    private TextView scoreValue;
    private TextView adviceValue;
    private TextView ramValue;
    private TextView cpuValue;
    private TextView tempValue;
    private TextView rttValue;
    private TextView jitterValue;
    private TextView failureValue;
    private TextView hzValue;
    private TextView batteryValue;
    private TextView networkValue;
    private TextView lastSessionValue;
    private Button profileButton;

    private int profile;
    private int lastRtt = -1;
    private int lastJitter = -1;
    private int lastFailure = -1;
    private boolean overlayRequestPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        profile = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_PROFILE, PROFILE_ADAPTIVE);
        if (profile < PROFILE_TURBO || profile > PROFILE_ADAPTIVE) profile = PROFILE_ADAPTIVE;
        setContentView(buildUi());
        ensureNotificationPermission();
        refreshMetrics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMetrics();
        if (overlayRequestPending && canDrawOverlays()) {
            overlayRequestPending = false;
            startHud();
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(text("HUAWEI Y9 2019 • KIRIN 710 • V3 ADAPTIVE", 11, CYAN, true));
        TextView title = text("FF Y9 BOOSTER\nVIP PRO X3", 31, TEXT, true);
        title.setLineSpacing(0, 0.92f);
        title.setPadding(0, dp(4), 0, dp(4));
        root.addView(title);

        String model = Build.MANUFACTURER + " " + Build.MODEL + " • Android " + Build.VERSION.RELEASE;
        TextView sub = text(model + "\nAdaptive thermal guard • multi-probe network • session history. Không root, không sửa game.", 13, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        root.addView(sub, lp(-1, -2, 0, dp(14)));

        LinearLayout hero = card(CARD, 18);
        hero.setPadding(dp(16), dp(15), dp(16), dp(15));
        root.addView(hero, lp(-1, -2, 0, dp(12)));
        statusValue = text("ĐANG CHẨN ĐOÁN…", 11, LIME, true);
        hero.addView(statusValue);
        scoreValue = text("— / 100", 29, TEXT, true);
        scoreValue.setPadding(0, dp(5), 0, dp(2));
        hero.addView(scoreValue);
        adviceValue = text("Đang đọc nhiệt, RAM, pin và chất lượng mạng.", 12, MUTED, false);
        adviceValue.setLineSpacing(dp(1), 1f);
        hero.addView(adviceValue);

        LinearLayout m1 = new LinearLayout(this);
        m1.setOrientation(LinearLayout.HORIZONTAL);
        ramValue = metric(m1, "RAM TRỐNG");
        tempValue = metric(m1, "NHIỆT PIN");
        root.addView(m1, lp(-1, -2, 0, dp(8)));

        LinearLayout m2 = new LinearLayout(this);
        m2.setOrientation(LinearLayout.HORIZONTAL);
        cpuValue = metric(m2, "CPU");
        rttValue = metric(m2, "RTT*");
        root.addView(m2, lp(-1, -2, 0, dp(8)));

        LinearLayout m3 = new LinearLayout(this);
        m3.setOrientation(LinearLayout.HORIZONTAL);
        jitterValue = metric(m3, "JITTER*");
        failureValue = metric(m3, "FAIL*");
        root.addView(m3, lp(-1, -2, 0, dp(8)));

        LinearLayout m4 = new LinearLayout(this);
        m4.setOrientation(LinearLayout.HORIZONTAL);
        hzValue = metric(m4, "DISPLAY");
        batteryValue = metric(m4, "PIN");
        root.addView(m4, lp(-1, -2, 0, dp(8)));

        LinearLayout netCard = card(CARD_2, 14);
        netCard.setPadding(dp(13), dp(11), dp(13), dp(11));
        networkValue = text("Mạng: —", 12, TEXT, true);
        networkValue.setLineSpacing(dp(2), 1f);
        netCard.addView(networkValue);
        root.addView(netCard, lp(-1, -2, 0, dp(12)));

        Button launch = button("⚡  ADAPT + MỞ FREE FIRE", CYAN, BG);
        launch.setOnClickListener(v -> prepareAndLaunch("com.dts.freefireth"));
        root.addView(launch, lp(-1, dp(54), 0, dp(9)));

        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        Button max = button("FREE FIRE MAX", CARD_2, TEXT);
        max.setOnClickListener(v -> prepareAndLaunch("com.dts.freefiremax"));
        Button hud = button("BẬT HUD V3", CARD_2, TEXT);
        hud.setOnClickListener(v -> ensureHudPermissionAndStart());
        pair.addView(max, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair.addView(space(dp(9)));
        pair.addView(hud, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair, lp(-1, -2, 0, dp(9)));

        LinearLayout pair2 = new LinearLayout(this);
        pair2.setOrientation(LinearLayout.HORIZONTAL);
        profileButton = button(profileButtonText(), CARD_2, LIME);
        profileButton.setOnClickListener(v -> cycleProfile());
        Button stop = button("TẮT + LƯU SESSION", CARD_2, WARN);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Đã tắt session và lưu thống kê.", Toast.LENGTH_SHORT).show();
            handler.postDelayed(this::refreshMetrics, 350);
        });
        pair2.addView(profileButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair2.addView(space(dp(9)));
        pair2.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair2, lp(-1, -2, 0, dp(9)));

        Button refresh = button("↻  MULTI-PROBE CHẨN ĐOÁN LẠI", CARD_2, TEXT);
        refresh.setOnClickListener(v -> refreshMetrics());
        root.addView(refresh, lp(-1, dp(48), 0, dp(14)));

        root.addView(section("V3 ADAPTIVE LÀM GÌ"));
        LinearLayout preset = card(CARD, 16);
        preset.setPadding(dp(15), dp(12), dp(15), dp(10));
        preset.addView(line("A", "ADAPTIVE", "Tự chọn TURBO / BALANCED / COOL theo nhiệt, thermal status và Power Saver. Có hysteresis để tránh đổi mode liên tục."));
        preset.addView(line("T", "TURBO NET", "Android 10+: Wi-Fi low-latency; Android cũ fallback high-performance. Nếu máy quá nóng, thermal guard vẫn có quyền hạ COOL."));
        preset.addView(line("B", "BALANCED", "Wi-Fi high-performance với nhịp probe vừa phải, hợp phiên chơi dài."));
        preset.addView(line("C", "COOL", "Không giữ Wi-Fi performance lock và probe thưa hơn để booster tự tạo ít nhiệt hơn."));
        root.addView(preset, lp(-1, -2, 0, dp(12)));

        root.addView(section("ĐO MẠNG KHÔNG TỰ LỪA MÌNH"));
        LinearLayout truth = card(CARD, 16);
        truth.setPadding(dp(15), dp(12), dp(15), dp(10));
        truth.addView(line("RTT", "RTT* median", "Lấy trung vị từ nhiều TCP connect tới endpoint Internet công cộng, đỡ bị một mẫu dị thường làm sai kết luận."));
        truth.addView(line("JIT", "Jitter*", "Độ lệch giữa các probe. Cao thường nghĩa là đường truyền thiếu ổn định."));
        truth.addView(line("FAIL", "Failure*", "Tỷ lệ probe TCP thất bại. Không phải packet-loss trực tiếp tới server Garena."));
        truth.addView(line("Hz", "Display Hz", "Tần số quét màn hình. V3 vẫn không giả Display Hz hay VSYNC thành FPS engine của Free Fire."));
        root.addView(truth, lp(-1, -2, 0, dp(12)));

        root.addView(section("3 PHIÊN GẦN NHẤT"));
        LinearLayout history = card(CARD, 16);
        history.setPadding(dp(15), dp(13), dp(15), dp(13));
        lastSessionValue = text("Chưa có dữ liệu phiên.", 11, MUTED, false);
        lastSessionValue.setLineSpacing(dp(2), 1f);
        history.addView(lastSessionValue);
        root.addView(history, lp(-1, -2, 0, dp(9)));

        Button clearHistory = button("XÓA LỊCH SỬ SESSION", CARD_2, MUTED);
        clearHistory.setOnClickListener(v -> clearSessionHistory());
        root.addView(clearHistory, lp(-1, dp(44), 0, dp(12)));

        LinearLayout settingsPair = new LinearLayout(this);
        settingsPair.setOrientation(LinearLayout.HORIZONTAL);
        Button batterySettings = button("PIN / TIẾT KIỆM", CARD_2, TEXT);
        batterySettings.setOnClickListener(v -> openBatterySettings());
        Button optimization = button("TỐI ƯU PIN", CARD_2, TEXT);
        optimization.setOnClickListener(v -> openBatteryOptimizationSettings());
        settingsPair.addView(batterySettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        settingsPair.addView(space(dp(9)));
        settingsPair.addView(optimization, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(settingsPair, lp(-1, -2, 0, dp(9)));

        LinearLayout settingsPair2 = new LinearLayout(this);
        settingsPair2.setOrientation(LinearLayout.HORIZONTAL);
        Button displaySettings = button("MÀN HÌNH", CARD_2, TEXT);
        displaySettings.setOnClickListener(v -> openDisplaySettings());
        Button appSettings = button("QUYỀN APP", CARD_2, TEXT);
        appSettings.setOnClickListener(v -> openAppSettings());
        settingsPair2.addView(displaySettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        settingsPair2.addView(space(dp(9)));
        settingsPair2.addView(appSettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(settingsPair2, lp(-1, -2, 0, dp(12)));

        TextView legal = text(
                "Không inject/hook, không sửa APK/OBB/data, không macro và không can thiệp anti-cheat. "
                        + "RTT*/Jitter*/Failure* là probe Internet công cộng, không phải ping server Garena. "
                        + "App tối ưu các biến số mà Android cho phép, chứ không thể đàm phán lại định luật nhiệt động lực học với Kirin 710.",
                11, MUTED, false);
        legal.setLineSpacing(dp(2), 1f);
        root.addView(legal);
        return scroll;
    }

    private void refreshMetrics() {
        long ram = DeviceProbe.freeRamMb(this);
        float temp = DeviceProbe.batteryTempC(this);
        int battery = DeviceProbe.batteryPercent(this);
        int currentMa = DeviceProbe.batteryCurrentMa(this);
        boolean charging = DeviceProbe.isCharging(this);
        boolean saver = DeviceProbe.isPowerSave(this);
        boolean connected = DeviceProbe.isConnected(this);
        int thermal = DeviceProbe.thermalStatus(this);

        ramValue.setText(ram >= 0 ? ram + " MB" : "?");
        tempValue.setText(temp > 0 ? String.format(Locale.US, "%.1f°C", temp) : "?");
        batteryValue.setText(battery >= 0 ? battery + "%" + (charging ? " CHG" : "") : "?");
        hzValue.setText(DeviceProbe.hzText(this));

        String powerText = saver ? "POWER SAVE ON" : "Power Save off";
        String chargeText = charging ? "ĐANG SẠC" + (currentMa > 0 ? " ~" + currentMa + "mA" : "") : "không sạc";
        String live = isSessionActive() ? " • SESSION LIVE" : "";
        networkValue.setText(
                "Mạng: " + DeviceProbe.networkLabel(this) + " • " + DeviceProbe.thermalLabel(this) + live
                        + "\n" + powerText + " • " + chargeText + " • " + DeviceProbe.storageText());

        updateScore(ram, temp, battery, saver, connected, lastRtt, lastJitter, lastFailure, thermal);
        loadSessionHistory();

        statusValue.setText("ĐANG ĐO CPU + 4 NETWORK PROBES…");
        new Thread(() -> {
            int cpu = DeviceProbe.sampleCpuPercent();
            DeviceProbe.NetworkSample net = connected
                    ? DeviceProbe.probeNetwork()
                    : DeviceProbe.NetworkSample.unavailable(4);
            handler.post(() -> {
                lastRtt = net.medianRttMs;
                lastJitter = net.jitterMs;
                lastFailure = net.failurePercent;
                cpuValue.setText(cpu >= 0 ? cpu + "%" : "?");
                rttValue.setText(net.medianRttMs >= 0 ? net.medianRttMs + " ms" : "?");
                jitterValue.setText(net.jitterMs >= 0 ? net.jitterMs + " ms" : "?");
                failureValue.setText(net.failurePercent >= 0 ? net.failurePercent + "%" : "?");
                updateScore(
                        DeviceProbe.freeRamMb(this),
                        DeviceProbe.batteryTempC(this),
                        DeviceProbe.batteryPercent(this),
                        DeviceProbe.isPowerSave(this),
                        DeviceProbe.isConnected(this),
                        net.medianRttMs,
                        net.jitterMs,
                        net.failurePercent,
                        DeviceProbe.thermalStatus(this));
            });
        }, "preflight-v3-probe").start();
    }

    private void updateScore(long ram, float temp, int battery, boolean saver, boolean connected,
                             int rtt, int jitter, int failure, int thermal) {
        int score = 100;
        String advice = "Tình trạng khá ổn. ADAPTIVE sẽ tự điều chỉnh session khi nhiệt thay đổi.";

        if (battery >= 0 && battery <= 15) {
            score -= 7;
            advice = "Pin thấp. Nếu chơi lâu, tránh vừa sạc nhanh vừa ép TURBO khi máy đã nóng.";
        }

        if (ram >= 0 && ram < 1200) score -= 5;
        if (ram >= 0 && ram < 800) {
            score -= 9;
            advice = "RAM hơi căng. Free Fire thường hợp Y9 2019 hơn bản MAX.";
        }
        if (ram >= 0 && ram < 450) {
            score -= 12;
            advice = "RAM trống rất thấp. Đóng app nặng thủ công trước khi vào trận.";
        }
        if (DeviceProbe.isLowMemory(this)) {
            score -= 8;
            advice = "Android đang báo memory pressure. Đây là dấu hiệu đáng tin hơn một nút 'RAM cleaner' trang trí.";
        }

        if (rtt >= 60) score -= 4;
        if (rtt >= 100) {
            score -= 7;
            advice = "RTT* cao. Lag có khả năng nghiêng về đường mạng.";
        }
        if (rtt >= 180) {
            score -= 8;
            advice = "RTT* rất cao. Wi-Fi performance lock không thể cứu một đường truyền vốn đã chậm.";
        }

        if (jitter >= 20) score -= 4;
        if (jitter >= 45) {
            score -= 8;
            advice = "Jitter* cao: độ trễ thay đổi mạnh giữa các probe. Mạng thiếu ổn định.";
        }
        if (jitter >= 90) {
            score -= 6;
            advice = "Jitter* cực cao. Đây thường khó chịu hơn một RTT hơi cao nhưng ổn định.";
        }

        if (failure >= 25) {
            score -= 8;
            advice = "Có probe mạng thất bại. Kiểm tra Wi-Fi/router hoặc chuyển mạng.";
        }
        if (failure >= 50) {
            score -= 12;
            advice = "Failure* cao. Đừng đổ hết cho GPU khi Internet đang biểu tình.";
        }

        if (saver) {
            score -= 15;
            advice = "Đang bật tiết kiệm pin. ADAPTIVE sẽ nghiêng COOL; tắt Power Saver nếu ưu tiên game.";
        }

        if (temp >= 39.5f) score -= 7;
        if (temp >= 41f) {
            score -= 11;
            advice = "Nhiệt khá cao. ADAPTIVE sẽ hạ mode để tránh booster góp thêm nhiệt.";
        }
        if (temp >= 43f) {
            score -= 14;
            advice = "Máy nóng. Throttle nhiệt có thể tụt xung; nên để máy nguội trước.";
        }
        if (thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
            score -= 18;
            advice = "Android đang báo thermal SEVERE trở lên. V3 sẽ ép session về COOL.";
        }

        if (!connected) {
            score -= 35;
            advice = "Không có mạng. Phần mềm vẫn chưa thể phát Internet bằng niềm tin.";
        }

        score = Math.max(0, Math.min(100, score));
        String grade = score >= 85 ? "SẴN SÀNG"
                : score >= 65 ? "TẠM ỔN"
                : score >= 45 ? "CẦN CHỈNH"
                : "KHÔNG ĐẸP";

        scoreValue.setText(score + " / 100 • " + grade);
        scoreValue.setTextColor(score >= 80 ? LIME : score >= 55 ? WARN : RED);
        adviceValue.setText(advice);

        String recommended = recommendedProfile(temp, saver, thermal);
        statusValue.setText("✓ PREFLIGHT • MODE " + profileName(profile) + " • GỢI Ý " + recommended);
    }

    private String recommendedProfile(float temp, boolean saver, int thermal) {
        if (thermal >= PowerManager.THERMAL_STATUS_SEVERE || temp >= 42.5f || saver) return "COOL";
        if (thermal >= PowerManager.THERMAL_STATUS_MODERATE || temp >= 40.5f) return "BALANCED";
        return "ADAPTIVE";
    }

    private void cycleProfile() {
        profile = (profile + 1) % 4;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_PROFILE, profile).apply();
        profileButton.setText(profileButtonText());

        if (isSessionActive()) {
            Intent service = new Intent(this, OverlayService.class).putExtra(KEY_PROFILE, profile);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
                else startService(service);
            } catch (Throwable ignored) { }
        }

        Toast.makeText(this, "Preset: " + profileName(profile), Toast.LENGTH_SHORT).show();
        refreshMetrics();
    }

    private void prepareAndLaunch(String pkg) {
        float temp = DeviceProbe.batteryTempC(this);
        int thermal = DeviceProbe.thermalStatus(this);
        if (temp >= 44f || thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
            Toast.makeText(this, "Cảnh báo: máy đang rất nóng; V3 sẽ ép session về COOL.", Toast.LENGTH_LONG).show();
        }

        refreshMetrics();
        if (canDrawOverlays()) startHud();
        else Toast.makeText(this, "HUD chưa có quyền overlay; vẫn mở game bình thường.", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> {
            if (!launchPackage(pkg)) {
                Toast.makeText(this, "Không tìm thấy game đã chọn.", Toast.LENGTH_LONG).show();
            }
        }, 220);
    }

    private void ensureHudPermissionAndStart() {
        if (canDrawOverlays()) {
            startHud();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayRequestPending = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Bật “Hiển thị trên ứng dụng khác” rồi quay lại.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void startHud() {
        Intent service = new Intent(this, OverlayService.class).putExtra(KEY_PROFILE, profile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
        Toast.makeText(this, "HUD V3 • " + profileName(profile), Toast.LENGTH_SHORT).show();
    }

    private boolean launchPackage(String pkg) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isSessionActive() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!p.getBoolean("session_active", false)) return false;
        long heartbeat = p.getLong("session_heartbeat_ms", 0);
        return heartbeat > 0 && System.currentTimeMillis() - heartbeat < 12000;
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
        }
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        } catch (Throwable e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void openBatteryOptimizationSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Throwable e) {
            openAppSettings();
        }
    }

    private void openDisplaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        } catch (Throwable e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void loadSessionHistory() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        StringBuilder out = new StringBuilder();
        int shown = 0;

        for (int i = 0; i < 3; i++) {
            SessionRecord record = SessionRecord.decode(p.getString("history_" + i, ""));
            if (record == null) continue;
            if (shown > 0) out.append("\n\n");
            shown++;
            out.append(record.toDisplayLine(shown));
        }

        if (shown == 0) {
            long duration = p.getLong("last_duration_s", 0);
            if (duration > 0) {
                float maxTemp = p.getFloat("last_max_temp", -1f);
                long minRam = p.getLong("last_min_ram", -1);
                int avgRtt = p.getInt("last_avg_rtt", -1);
                int avgJitter = p.getInt("last_avg_jitter", -1);
                out.append("Dữ liệu legacy V2 • ")
                        .append(String.format(Locale.US, "%02d:%02d", duration / 60, duration % 60))
                        .append("\nNhiệt max ")
                        .append(maxTemp > 0 ? String.format(Locale.US, "%.1f°C", maxTemp) : "?")
                        .append(" • RAM min ").append(minRam >= 0 ? minRam + "M" : "?")
                        .append(" • RTT* ").append(avgRtt >= 0 ? avgRtt + "ms" : "?")
                        .append(" • JIT* ").append(avgJitter >= 0 ? avgJitter + "ms" : "?");
            } else {
                out.append("Chưa có dữ liệu. Bật HUD, chơi game rồi bấm TẮT + LƯU SESSION.");
            }
        }

        lastSessionValue.setText(out.toString());
    }

    private void clearSessionHistory() {
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (int i = 0; i < 3; i++) e.remove("history_" + i);
        e.remove("last_duration_s")
                .remove("last_max_temp")
                .remove("last_min_ram")
                .remove("last_avg_rtt")
                .remove("last_avg_jitter")
                .remove("last_max_failure")
                .apply();
        loadSessionHistory();
        Toast.makeText(this, "Đã xóa lịch sử session.", Toast.LENGTH_SHORT).show();
    }

    static String profileName(int p) {
        if (p == PROFILE_BALANCED) return "BALANCED";
        if (p == PROFILE_COOL) return "COOL";
        if (p == PROFILE_ADAPTIVE) return "ADAPTIVE";
        return "TURBO NET";
    }

    private String profileButtonText() {
        return "MODE: " + profileName(profile);
    }

    private TextView metric(LinearLayout parent, String label) {
        LinearLayout box = card(CARD, 13);
        box.setPadding(dp(12), dp(9), dp(12), dp(9));
        TextView l = text(label, 9, MUTED, true);
        TextView v = text("—", 17, TEXT, true);
        v.setPadding(0, dp(3), 0, 0);
        box.addView(l);
        box.addView(v);
        parent.addView(box, new LinearLayout.LayoutParams(0, dp(68), 1f));
        if (parent.getChildCount() == 1) parent.addView(space(dp(8)));
        return v;
    }

    private View line(String badge, String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));

        TextView b = text(badge, 12, CYAN, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(CARD_2);
        badgeBg.setCornerRadius(dp(8));
        b.setBackground(badgeBg);
        row.addView(b, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(11), 0, 0, 0);
        copy.addView(text(title, 13, TEXT, true));
        TextView d = text(desc, 11, MUTED, false);
        d.setPadding(0, dp(2), 0, 0);
        d.setLineSpacing(dp(1), 1f);
        copy.addView(d);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private TextView section(String s) {
        TextView t = text(s, 10, MUTED, true);
        t.setPadding(dp(2), dp(3), 0, dp(8));
        return t;
    }

    private LinearLayout card(int color, int radius) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), Color.rgb(35, 48, 63));
        box.setBackground(bg);
        return box;
    }

    private Button button(String label, int bgColor, int fgColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(fgColor);
        b.setTextSize(11.5f);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(7), 0, dp(7), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(14));
        b.setBackground(bg);
        return b;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private View space(int width) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return v;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
