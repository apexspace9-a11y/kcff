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
    private TextView pressureValue;
    private TextView tempValue;
    private TextView cpuValue;
    private TextView rttValue;
    private TextView jitterValue;
    private TextView failureValue;
    private TextView hzValue;
    private TextView batteryValue;
    private TextView modeValue;
    private TextView networkValue;
    private TextView historyValue;
    private Button profileButton;

    private int profile;
    private int lastCpu = -1;
    private int lastRtt = -1;
    private int lastJitter = -1;
    private int lastFailure = -1;
    private int lastScore = -1;
    private String lastIssue = "CHƯA ĐO";
    private boolean overlayRequestPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        profile = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_PROFILE, PROFILE_ADAPTIVE);
        if (profile < PROFILE_TURBO || profile > PROFILE_ADAPTIVE) {
            profile = PROFILE_ADAPTIVE;
        }

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

        root.addView(text("HUAWEI Y9 2019 • KIRIN 710 • V4 STABILITY", 11, CYAN, true));

        TextView title = text("FF Y9 BOOSTER\nVIP PRO X4", 31, TEXT, true);
        title.setLineSpacing(0, 0.92f);
        title.setPadding(0, dp(4), 0, dp(4));
        root.addView(title);

        String model = Build.MANUFACTURER + " " + Build.MODEL + " • Android " + Build.VERSION.RELEASE;
        TextView sub = text(
                model + "\nAdaptive+ • dynamic network probe • memory pressure • session grade. Không root, không sửa game.",
                13, MUTED, false);
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

        adviceValue = text("Đang phân tích nhiệt, RAM pressure và độ ổn định mạng.", 12, MUTED, false);
        adviceValue.setLineSpacing(dp(1), 1f);
        hero.addView(adviceValue);

        LinearLayout m1 = metricRow();
        ramValue = metric(m1, "RAM TRỐNG");
        pressureValue = metric(m1, "RAM PRESSURE*");
        root.addView(m1, lp(-1, -2, 0, dp(8)));

        LinearLayout m2 = metricRow();
        tempValue = metric(m2, "NHIỆT PIN");
        cpuValue = metric(m2, "CPU");
        root.addView(m2, lp(-1, -2, 0, dp(8)));

        LinearLayout m3 = metricRow();
        rttValue = metric(m3, "RTT*");
        jitterValue = metric(m3, "JITTER*");
        root.addView(m3, lp(-1, -2, 0, dp(8)));

        LinearLayout m4 = metricRow();
        failureValue = metric(m4, "FAIL*");
        hzValue = metric(m4, "DISPLAY");
        root.addView(m4, lp(-1, -2, 0, dp(8)));

        LinearLayout m5 = metricRow();
        batteryValue = metric(m5, "PIN");
        modeValue = metric(m5, "MODE");
        modeValue.setText(profileShortName(profile));
        root.addView(m5, lp(-1, -2, 0, dp(8)));

        LinearLayout netCard = card(CARD_2, 14);
        netCard.setPadding(dp(13), dp(11), dp(13), dp(11));
        networkValue = text("Thiết bị: —", 12, TEXT, true);
        networkValue.setLineSpacing(dp(2), 1f);
        netCard.addView(networkValue);
        root.addView(netCard, lp(-1, -2, 0, dp(12)));

        Button launch = button("⚡  ADAPTIVE+ + MỞ FREE FIRE", CYAN, BG);
        launch.setOnClickListener(v -> prepareAndLaunch("com.dts.freefireth"));
        root.addView(launch, lp(-1, dp(54), 0, dp(9)));

        LinearLayout pair = metricRow();
        Button max = button("FREE FIRE MAX", CARD_2, TEXT);
        max.setOnClickListener(v -> prepareAndLaunch("com.dts.freefiremax"));
        Button hud = button("BẬT HUD V4", CARD_2, TEXT);
        hud.setOnClickListener(v -> ensureHudPermissionAndStart());
        pair.addView(max, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair.addView(space(dp(9)));
        pair.addView(hud, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair, lp(-1, -2, 0, dp(9)));

        LinearLayout pair2 = metricRow();
        profileButton = button(profileButtonText(), CARD_2, LIME);
        profileButton.setOnClickListener(v -> cycleProfile());
        Button stop = button("TẮT + CHẤM ĐIỂM", CARD_2, WARN);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Đã tắt session. V4 đang lưu Grade + thống kê.", Toast.LENGTH_SHORT).show();
            handler.postDelayed(this::refreshMetrics, 450);
        });
        pair2.addView(profileButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair2.addView(space(dp(9)));
        pair2.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair2, lp(-1, -2, 0, dp(9)));

        Button refresh = button("↻  DEEP PREFLIGHT 4 PROBES", CARD_2, TEXT);
        refresh.setOnClickListener(v -> refreshMetrics());
        root.addView(refresh, lp(-1, dp(48), 0, dp(14)));

        root.addView(section("V4 ADAPTIVE+"));

        LinearLayout features = card(CARD, 16);
        features.setPadding(dp(15), dp(12), dp(15), dp(10));
        features.addView(line("A+", "Adaptive thermal guard",
                "Tự chọn TURBO/BALANCED/COOL theo nhiệt và Power Saver. Nếu quá nóng, COOL được ưu tiên kể cả bạn chọn TURBO."));
        features.addView(line("DP", "Dynamic Probe",
                "Mạng ổn thì đo nhẹ/thưa; bất ổn thì tăng thành deep probe nhanh hơn. Mục tiêu là theo dõi tốt mà booster tự tạo ít overhead."));
        features.addView(line("MP", "Memory Pressure",
                "Kết hợp RAM khả dụng, tổng RAM, low-memory flag và ngưỡng Android. Không thần thánh hóa một con số 'RAM trống'."));
        features.addView(line("S", "Stability Score",
                "HUD chấm 0–100 theo nhiệt, RAM và mạng. Đây là điểm chẩn đoán, không phải benchmark FPS."));
        root.addView(features, lp(-1, -2, 0, dp(12)));

        root.addView(section("5 PHIÊN GẦN NHẤT • SESSION GRADE"));

        LinearLayout history = card(CARD, 16);
        history.setPadding(dp(15), dp(13), dp(15), dp(13));
        historyValue = text("Chưa có dữ liệu V4.", 11, MUTED, false);
        historyValue.setLineSpacing(dp(2), 1f);
        history.addView(historyValue);
        root.addView(history, lp(-1, -2, 0, dp(9)));

        LinearLayout historyButtons = metricRow();
        Button share = button("CHIA SẺ BÁO CÁO", CARD_2, TEXT);
        share.setOnClickListener(v -> shareReport());
        Button clear = button("XÓA LỊCH SỬ", CARD_2, MUTED);
        clear.setOnClickListener(v -> clearSessionHistory());
        historyButtons.addView(share, new LinearLayout.LayoutParams(0, dp(46), 1f));
        historyButtons.addView(space(dp(9)));
        historyButtons.addView(clear, new LinearLayout.LayoutParams(0, dp(46), 1f));
        root.addView(historyButtons, lp(-1, -2, 0, dp(12)));

        root.addView(section("CÀI ĐẶT HỮU ÍCH"));

        LinearLayout settings1 = metricRow();
        Button batterySettings = button("PIN / TIẾT KIỆM", CARD_2, TEXT);
        batterySettings.setOnClickListener(v -> openBatterySettings());
        Button optimization = button("TỐI ƯU PIN", CARD_2, TEXT);
        optimization.setOnClickListener(v -> openBatteryOptimizationSettings());
        settings1.addView(batterySettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        settings1.addView(space(dp(9)));
        settings1.addView(optimization, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(settings1, lp(-1, -2, 0, dp(9)));

        LinearLayout settings2 = metricRow();
        Button displaySettings = button("MÀN HÌNH", CARD_2, TEXT);
        displaySettings.setOnClickListener(v -> openDisplaySettings());
        Button appSettings = button("QUYỀN APP", CARD_2, TEXT);
        appSettings.setOnClickListener(v -> openAppSettings());
        settings2.addView(displaySettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        settings2.addView(space(dp(9)));
        settings2.addView(appSettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(settings2, lp(-1, -2, 0, dp(12)));

        TextView legal = text(
                "Không inject/hook, không sửa APK/OBB/data, không macro và không bypass anti-cheat. "
                        + "RTT*/Jitter*/Fail* là probe Internet công cộng, không phải ping/packet-loss server Garena. "
                        + "RAM Pressure* là ước lượng từ thông tin bộ nhớ Android. V4 cố chẩn đoán đúng nguyên nhân lag, "
                        + "thay vì dán chữ PRO MAX vào nút rồi cầu vật lý hợp tác.",
                11, MUTED, false);
        legal.setLineSpacing(dp(2), 1f);
        root.addView(legal);

        return scroll;
    }

    private LinearLayout metricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void refreshMetrics() {
        DeviceProbe.MemorySnapshot mem = DeviceProbe.memory(this);
        float temp = DeviceProbe.batteryTempC(this);
        int battery = DeviceProbe.batteryPercent(this);
        int currentMa = DeviceProbe.batteryCurrentMa(this);
        boolean charging = DeviceProbe.isCharging(this);
        boolean saver = DeviceProbe.isPowerSave(this);
        boolean connected = DeviceProbe.isConnected(this);
        int thermal = DeviceProbe.thermalStatus(this);
        float headroom = DeviceProbe.thermalHeadroom(this);

        ramValue.setText(mem.availMb >= 0 ? mem.availMb + " MB" : "?");
        pressureValue.setText(mem.pressurePercent >= 0
                ? mem.pressurePercent + "%" + (mem.lowMemory ? " LOW" : "")
                : "?");
        tempValue.setText(temp > 0 ? String.format(Locale.US, "%.1f°C", temp) : "?");
        batteryValue.setText(battery >= 0 ? battery + "%" + (charging ? " CHG" : "") : "?");
        hzValue.setText(DeviceProbe.hzText(this));

        String chargeText = charging
                ? "đang sạc" + (currentMa > 0 ? " ~" + currentMa + "mA" : "")
                : "không sạc";
        String session = isSessionActive()
                ? "SESSION LIVE • Stability " + getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getInt("live_stability", -1)
                : "session off";
        String headroomText = headroom >= 0
                ? " • Headroom5s " + String.format(Locale.US, "%.2f", headroom)
                : "";
        networkValue.setText(
                DeviceProbe.networkLabel(this) + " • " + DeviceProbe.thermalLabel(this) + headroomText
                        + "\n" + session + " • " + (saver ? "POWER SAVE ON" : "Power Save off")
                        + " • " + chargeText
                        + "\nRAM total " + (mem.totalMb >= 0 ? mem.totalMb + "MB" : "?")
                        + " • low threshold " + (mem.thresholdMb >= 0 ? mem.thresholdMb + "MB" : "?")
                        + " • " + DeviceProbe.storageText());

        loadSessionHistory();
        updateScore(mem, temp, battery, saver, connected, lastRtt, lastJitter, lastFailure, thermal);

        statusValue.setText("ĐANG ĐO CPU + 4 NETWORK PROBES…");
        new Thread(() -> {
            int cpu = DeviceProbe.sampleCpuPercent();
            DeviceProbe.NetworkSample net = connected
                    ? DeviceProbe.probeNetwork(4, 650)
                    : DeviceProbe.NetworkSample.unavailable(4);

            handler.post(() -> {
                lastCpu = cpu;
                lastRtt = net.medianRttMs;
                lastJitter = net.jitterMs;
                lastFailure = net.failurePercent;

                cpuValue.setText(cpu >= 0 ? cpu + "%" : "?");
                rttValue.setText(net.medianRttMs >= 0 ? net.medianRttMs + " ms" : "?");
                jitterValue.setText(net.jitterMs >= 0 ? net.jitterMs + " ms" : "?");
                failureValue.setText(net.failurePercent >= 0 ? net.failurePercent + "%" : "?");

                updateScore(
                        DeviceProbe.memory(this),
                        DeviceProbe.batteryTempC(this),
                        DeviceProbe.batteryPercent(this),
                        DeviceProbe.isPowerSave(this),
                        DeviceProbe.isConnected(this),
                        net.medianRttMs,
                        net.jitterMs,
                        net.failurePercent,
                        DeviceProbe.thermalStatus(this));
            });
        }, "v4-preflight").start();
    }

    private void updateScore(DeviceProbe.MemorySnapshot mem,
                             float temp,
                             int battery,
                             boolean saver,
                             boolean connected,
                             int rtt,
                             int jitter,
                             int failure,
                             int thermal) {
        int score = 100;
        int thermalRisk = 0;
        int ramRisk = 0;
        int netRisk = 0;

        if (!connected) {
            score -= 40;
            netRisk = 4;
        }

        if (mem.lowMemory) {
            score -= 22;
            ramRisk = Math.max(ramRisk, 4);
        }
        if (mem.availMb >= 0 && mem.availMb < 420) {
            score -= 22;
            ramRisk = Math.max(ramRisk, 4);
        } else if (mem.availMb >= 0 && mem.availMb < 700) {
            score -= 12;
            ramRisk = Math.max(ramRisk, 3);
        } else if (mem.availMb >= 0 && mem.availMb < 1000) {
            score -= 5;
            ramRisk = Math.max(ramRisk, 1);
        }
        if (mem.pressurePercent >= 94) {
            score -= 10;
            ramRisk = Math.max(ramRisk, 3);
        } else if (mem.pressurePercent >= 88) {
            score -= 5;
            ramRisk = Math.max(ramRisk, 2);
        }

        if (temp >= 44f || thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
            score -= 34;
            thermalRisk = 4;
        } else if (temp >= 42f || thermal >= PowerManager.THERMAL_STATUS_MODERATE) {
            score -= 19;
            thermalRisk = 3;
        } else if (temp >= 40f || thermal >= PowerManager.THERMAL_STATUS_LIGHT) {
            score -= 8;
            thermalRisk = 1;
        }

        if (saver) score -= 12;
        if (battery >= 0 && battery <= 12) score -= 5;

        if (rtt >= 180) {
            score -= 18;
            netRisk = Math.max(netRisk, 4);
        } else if (rtt >= 110) {
            score -= 10;
            netRisk = Math.max(netRisk, 3);
        } else if (rtt >= 70) {
            score -= 4;
            netRisk = Math.max(netRisk, 1);
        }

        if (jitter >= 55) {
            score -= 18;
            netRisk = Math.max(netRisk, 4);
        } else if (jitter >= 30) {
            score -= 10;
            netRisk = Math.max(netRisk, 3);
        } else if (jitter >= 18) {
            score -= 4;
            netRisk = Math.max(netRisk, 1);
        }

        if (failure >= 50) {
            score -= 18;
            netRisk = Math.max(netRisk, 4);
        } else if (failure >= 25) {
            score -= 8;
            netRisk = Math.max(netRisk, 3);
        }

        score = Math.max(0, Math.min(100, score));
        lastScore = score;

        if (thermalRisk == 0 && ramRisk == 0 && netRisk == 0) {
            lastIssue = "ỔN ĐỊNH";
        } else if (netRisk >= thermalRisk && netRisk >= ramRisk) {
            lastIssue = "MẠNG";
        } else if (thermalRisk >= ramRisk) {
            lastIssue = "NHIỆT";
        } else {
            lastIssue = "RAM";
        }

        String grade = score >= 90 ? "READY" : score >= 75 ? "GOOD" : score >= 60 ? "WATCH" : "RISK";
        scoreValue.setText(score + " / 100 • " + grade);
        scoreValue.setTextColor(score >= 80 ? LIME : score >= 60 ? WARN : RED);
        statusValue.setText("✓ PREFLIGHT V4 • " + lastIssue + " • " + profileName(profile));

        String advice;
        if ("NHIỆT".equals(lastIssue)) {
            advice = "Nhiệt là rủi ro chính. ADAPTIVE/COOL hợp lý hơn; throttling thì booster cũng không thương lượng được với silicon.";
        } else if ("RAM".equals(lastIssue)) {
            advice = "Memory pressure cao. Đóng ứng dụng nặng thủ công và ưu tiên Free Fire thường thay vì MAX.";
        } else if ("MẠNG".equals(lastIssue)) {
            advice = "Đường truyền đang là rủi ro chính. Nhìn RTT*/Jitter*/Fail* trước khi đổ mọi tội lỗi lên Kirin 710.";
        } else if (saver) {
            advice = "Máy khá ổn nhưng Power Saver đang bật. Tắt tiết kiệm pin trước khi chơi nếu cần hiệu năng ổn định.";
        } else {
            advice = "Trạng thái hiện tại khá ổn. V4 sẽ dùng Dynamic Probe để theo dõi nếu mạng hoặc nhiệt đổi trong trận.";
        }
        adviceValue.setText(advice);
    }

    private void cycleProfile() {
        if (profile == PROFILE_ADAPTIVE) profile = PROFILE_TURBO;
        else if (profile == PROFILE_TURBO) profile = PROFILE_BALANCED;
        else if (profile == PROFILE_BALANCED) profile = PROFILE_COOL;
        else profile = PROFILE_ADAPTIVE;

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_PROFILE, profile)
                .apply();

        profileButton.setText(profileButtonText());
        if (modeValue != null) modeValue.setText(profileShortName(profile));

        Intent service = new Intent(this, OverlayService.class)
                .putExtra(KEY_PROFILE, profile);
        if (isSessionActive()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
                else startService(service);
            } catch (Throwable ignored) { }
        }

        Toast.makeText(this, "Mode: " + profileName(profile), Toast.LENGTH_SHORT).show();
        refreshMetrics();
    }

    private void prepareAndLaunch(String pkg) {
        refreshMetrics();
        if (canDrawOverlays()) {
            startHud();
        } else {
            Toast.makeText(this,
                    "HUD chưa có quyền overlay; game vẫn mở bình thường.",
                    Toast.LENGTH_SHORT).show();
        }

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
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this,
                    "Bật “Hiển thị trên ứng dụng khác” rồi quay lại.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void startHud() {
        Intent service = new Intent(this, OverlayService.class)
                .putExtra(KEY_PROFILE, profile);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
            else startService(service);
            Toast.makeText(this,
                    "HUD V4 • " + profileName(profile),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Toast.makeText(this, "Không thể bật HUD trên firmware này.", Toast.LENGTH_LONG).show();
        }
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
        return heartbeat > 0 && System.currentTimeMillis() - heartbeat < 15000;
    }

    private void loadSessionHistory() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            SessionRecord r = SessionRecord.decode(p.getString("history_" + i, null));
            if (r == null) continue;
            if (out.length() > 0) out.append("\n\n");
            out.append(r.toDisplayLine(i + 1));
        }

        if (out.length() == 0) {
            historyValue.setText(
                    "Chưa có session V4. Bật HUD, chơi rồi bấm TẮT + CHẤM ĐIỂM để lưu Grade.");
        } else {
            historyValue.setText(out.toString());
        }
    }

    private void clearSessionHistory() {
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        for (int i = 0; i < 5; i++) e.remove("history_" + i);
        e.remove("last_session_score");
        e.remove("last_session_grade");
        e.remove("last_session_issue");
        e.apply();
        loadSessionHistory();
        Toast.makeText(this, "Đã xóa lịch sử session.", Toast.LENGTH_SHORT).show();
    }

    private void shareReport() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        StringBuilder report = new StringBuilder();
        report.append("FF Y9 Booster V4 Stability Report\n");
        report.append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append(" • Android ").append(Build.VERSION.RELEASE).append("\n");
        report.append("Preflight: ")
                .append(lastScore >= 0 ? lastScore + "/100" : "?")
                .append(" • issue=").append(lastIssue)
                .append(" • mode=").append(profileName(profile)).append("\n");
        report.append("Current: CPU=").append(lastCpu >= 0 ? lastCpu + "%" : "?")
                .append(", RTT*=").append(lastRtt >= 0 ? lastRtt + "ms" : "?")
                .append(", JIT*=").append(lastJitter >= 0 ? lastJitter + "ms" : "?")
                .append(", FAIL*=").append(lastFailure >= 0 ? lastFailure + "%" : "?")
                .append("\n\nRecent sessions:\n");

        int count = 0;
        for (int i = 0; i < 5; i++) {
            SessionRecord r = SessionRecord.decode(p.getString("history_" + i, null));
            if (r == null) continue;
            report.append(i + 1).append(". ").append(r.toShareLine()).append("\n");
            count++;
        }
        if (count == 0) report.append("No session data yet.\n");

        report.append("\n* Internet public-endpoint telemetry, not Garena server telemetry.");

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "FF Y9 Booster V4 Report");
        send.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(send, "Chia sẻ báo cáo V4"));
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
            startActivity(new Intent(Settings.ACTION_SETTINGS));
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
        Intent i = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    static String profileName(int p) {
        if (p == PROFILE_TURBO) return "TURBO NET";
        if (p == PROFILE_BALANCED) return "BALANCED";
        if (p == PROFILE_COOL) return "COOL";
        return "ADAPTIVE";
    }

    static String profileShortName(int p) {
        if (p == PROFILE_TURBO) return "TURBO";
        if (p == PROFILE_BALANCED) return "BAL";
        if (p == PROFILE_COOL) return "COOL";
        return "ADAPT";
    }

    private String profileButtonText() {
        return "MODE: " + profileShortName(profile);
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
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(8), 0, dp(8), 0);

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
