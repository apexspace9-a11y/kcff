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
    private TextView hzValue;
    private TextView batteryValue;
    private TextView networkValue;
    private TextView lastSessionValue;
    private Button profileButton;
    private int profile;
    private int lastRtt = -1;
    private int lastCpu = -1;
    private boolean overlayRequestPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        profile = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_PROFILE, PROFILE_TURBO);
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

        root.addView(text("HUAWEI Y9 2019 • KIRIN 710 • V2", 11, CYAN, true));
        TextView title = text("FF Y9 BOOSTER\nVIP PRO X2", 31, TEXT, true);
        title.setLineSpacing(0, 0.92f);
        title.setPadding(0, dp(4), 0, dp(4));
        root.addView(title);
        TextView sub = text("Telemetry-first: nhiệt • RAM • CPU • RTT* • Display Hz • Wi-Fi performance session. Không root, không sửa game.", 13, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        root.addView(sub, lp(-1, -2, 0, dp(14)));

        LinearLayout hero = card(CARD, 18);
        hero.setPadding(dp(16), dp(15), dp(16), dp(15));
        root.addView(hero, lp(-1, -2, 0, dp(12)));
        statusValue = text("ĐANG CHẨN ĐOÁN…", 11, LIME, true);
        hero.addView(statusValue);
        scoreValue = text("— / 100", 30, TEXT, true);
        scoreValue.setPadding(0, dp(5), 0, dp(2));
        hero.addView(scoreValue);
        adviceValue = text("Đang đọc nhiệt, RAM, pin và mạng.", 12, MUTED, false);
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
        hzValue = metric(m3, "DISPLAY");
        batteryValue = metric(m3, "PIN");
        root.addView(m3, lp(-1, -2, 0, dp(8)));

        LinearLayout netCard = card(CARD_2, 14);
        netCard.setPadding(dp(13), dp(11), dp(13), dp(11));
        networkValue = text("Mạng: —", 12, TEXT, true);
        netCard.addView(networkValue);
        root.addView(netCard, lp(-1, -2, 0, dp(12)));

        Button launch = button("⚡  CHUẨN BỊ + MỞ FREE FIRE", CYAN, BG);
        launch.setOnClickListener(v -> prepareAndLaunch("com.dts.freefireth"));
        root.addView(launch, lp(-1, dp(54), 0, dp(9)));

        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        Button max = button("FREE FIRE MAX", CARD_2, TEXT);
        max.setOnClickListener(v -> prepareAndLaunch("com.dts.freefiremax"));
        Button hud = button("BẬT HUD", CARD_2, TEXT);
        hud.setOnClickListener(v -> ensureHudPermissionAndStart());
        pair.addView(max, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair.addView(space(dp(9)));
        pair.addView(hud, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair, lp(-1, -2, 0, dp(9)));

        LinearLayout pair2 = new LinearLayout(this);
        pair2.setOrientation(LinearLayout.HORIZONTAL);
        profileButton = button(profileButtonText(), CARD_2, LIME);
        profileButton.setOnClickListener(v -> cycleProfile());
        Button stop = button("TẮT SESSION", CARD_2, WARN);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Đã tắt telemetry / Wi-Fi session.", Toast.LENGTH_SHORT).show();
            handler.postDelayed(this::refreshMetrics, 250);
        });
        pair2.addView(profileButton, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair2.addView(space(dp(9)));
        pair2.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair2, lp(-1, -2, 0, dp(9)));

        Button refresh = button("↻  CHẨN ĐOÁN LẠI", CARD_2, TEXT);
        refresh.setOnClickListener(v -> refreshMetrics());
        root.addView(refresh, lp(-1, dp(48), 0, dp(14)));

        root.addView(section("PRESET CÓ TÁC DỤNG GÌ"));
        LinearLayout preset = card(CARD, 16);
        preset.setPadding(dp(15), dp(12), dp(15), dp(10));
        preset.addView(line("T", "TURBO NET", "Android 10+: Wi-Fi low-latency. Y9 Android 9: fallback Wi-Fi high-performance. Telemetry cập nhật nhanh."));
        preset.addView(line("B", "BALANCED", "Wi-Fi high-performance, telemetry vừa phải. Hợp chơi lâu hơn."));
        preset.addView(line("C", "COOL", "Không giữ Wi-Fi performance lock. Giảm việc booster tự tạo thêm nhiệt/pin drain."));
        root.addView(preset, lp(-1, -2, 0, dp(12)));

        root.addView(section("FPS & ĐỘ TRỄ"));
        LinearLayout truth = card(CARD, 16);
        truth.setPadding(dp(15), dp(12), dp(15), dp(10));
        truth.addView(line("Hz", "Display Hz", "Đây là tần số quét màn hình thật. Y9 2019 thường là 60 Hz."));
        truth.addView(line("FPS", "FPS game", "App Android thường không được đọc frame timing nội bộ của Free Fire. V2 không giả VSYNC thành FPS game."));
        truth.addView(line("RTT*", "Độ trễ đường truyền", "Đo TCP connect tới endpoint Internet công cộng, hữu ích để phát hiện mạng xấu nhưng không phải ping server Garena."));
        root.addView(truth, lp(-1, -2, 0, dp(12)));

        root.addView(section("PHIÊN GẦN NHẤT"));
        LinearLayout last = card(CARD, 16);
        last.setPadding(dp(15), dp(13), dp(15), dp(13));
        lastSessionValue = text("Chưa có dữ liệu phiên.", 12, MUTED, false);
        lastSessionValue.setLineSpacing(dp(2), 1f);
        last.addView(lastSessionValue);
        root.addView(last, lp(-1, -2, 0, dp(12)));

        LinearLayout settingsPair = new LinearLayout(this);
        settingsPair.setOrientation(LinearLayout.HORIZONTAL);
        Button batterySettings = button("PIN / TIẾT KIỆM", CARD_2, TEXT);
        batterySettings.setOnClickListener(v -> openBatterySettings());
        Button appSettings = button("QUYỀN APP", CARD_2, TEXT);
        appSettings.setOnClickListener(v -> openAppSettings());
        settingsPair.addView(batterySettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        settingsPair.addView(space(dp(9)));
        settingsPair.addView(appSettings, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(settingsPair, lp(-1, -2, 0, dp(12)));

        TextView legal = text("Không inject/hook, không sửa APK/OBB/data, không can thiệp anti-cheat. Booster không thể biến Kirin 710 thành GPU flagship; mục tiêu là giảm biến số mạng/power-save và cho bạn số liệu để tìm đúng nguyên nhân lag.", 11, MUTED, false);
        legal.setLineSpacing(dp(2), 1f);
        root.addView(legal);
        return scroll;
    }

    private void refreshMetrics() {
        long ram = DeviceProbe.freeRamMb(this);
        float temp = DeviceProbe.batteryTempC(this);
        int battery = DeviceProbe.batteryPercent(this);
        boolean saver = DeviceProbe.isPowerSave(this);
        boolean connected = DeviceProbe.isConnected(this);

        ramValue.setText(ram >= 0 ? ram + " MB" : "?");
        tempValue.setText(temp > 0 ? String.format(Locale.US, "%.1f°C", temp) : "?");
        batteryValue.setText(battery >= 0 ? battery + "%" : "?");
        hzValue.setText(DeviceProbe.hzText(this));
        networkValue.setText("Mạng: " + DeviceProbe.networkLabel(this) + " • " + DeviceProbe.thermalLabel(this) + (saver ? " • POWER SAVE ON" : ""));
        updateScore(ram, temp, battery, saver, connected, lastRtt);
        loadLastSession();

        statusValue.setText("ĐANG ĐO CPU + RTT*…");
        new Thread(() -> {
            int cpu = DeviceProbe.sampleCpuPercent();
            int rtt = connected ? DeviceProbe.connectRttMs() : -1;
            handler.post(() -> {
                lastCpu = cpu;
                lastRtt = rtt;
                cpuValue.setText(cpu >= 0 ? cpu + "%" : "?");
                rttValue.setText(rtt >= 0 ? rtt + " ms" : "?");
                updateScore(DeviceProbe.freeRamMb(this), DeviceProbe.batteryTempC(this), DeviceProbe.batteryPercent(this), DeviceProbe.isPowerSave(this), DeviceProbe.isConnected(this), rtt);
            });
        }, "preflight-probe").start();
    }

    private void updateScore(long ram, float temp, int battery, boolean saver, boolean connected, int rtt) {
        int score = 100;
        String advice = "Máy đang ở trạng thái khá ổn để vào game.";
        if (!connected) { score -= 35; advice = "Không có mạng. Booster rất tài nhưng chưa học được cách phát Wi-Fi bằng ý chí."; }
        if (ram >= 0 && ram < 450) { score -= 25; advice = "RAM trống rất thấp. Đóng app nặng thủ công trước khi vào trận."; }
        else if (ram >= 0 && ram < 800) { score -= 14; advice = "RAM hơi căng. Free Fire thường sẽ hợp Y9 hơn bản MAX."; }
        else if (ram >= 0 && ram < 1200) score -= 6;
        if (temp >= 43f) { score -= 30; advice = "Máy đang nóng. Throttle nhiệt sẽ gây tụt xung, nên để máy nguội trước."; }
        else if (temp >= 41f) { score -= 18; advice = "Nhiệt khá cao. Dùng BALANCED hoặc COOL sẽ hợp lý hơn TURBO."; }
        else if (temp >= 39.5f) score -= 8;
        if (saver) { score -= 15; advice = "Đang bật tiết kiệm pin. Tắt nó trước khi chơi để tránh giới hạn nền/xung."; }
        if (battery >= 0 && battery <= 15) score -= 7;
        if (rtt >= 180) { score -= 18; advice = "RTT* rất cao. Lag lúc này có khả năng nghiêng về mạng hơn là RAM."; }
        else if (rtt >= 100) { score -= 10; advice = "RTT* cao. Thử Wi-Fi ổn định hơn hoặc đứng gần router."; }
        else if (rtt >= 60) score -= 4;
        score = Math.max(0, Math.min(100, score));

        String grade = score >= 85 ? "SẴN SÀNG" : score >= 65 ? "TẠM ỔN" : score >= 45 ? "CẦN CHỈNH" : "KHÔNG ĐẸP";
        scoreValue.setText(score + " / 100 • " + grade);
        scoreValue.setTextColor(score >= 80 ? LIME : score >= 55 ? WARN : RED);
        adviceValue.setText(advice);
        statusValue.setText("✓ PREFLIGHT • " + profileName(profile));
    }

    private void cycleProfile() {
        profile = (profile + 1) % 3;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_PROFILE, profile).apply();
        profileButton.setText(profileButtonText());
        Intent service = new Intent(this, OverlayService.class).putExtra(KEY_PROFILE, profile);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        } catch (Throwable ignored) { }
        Toast.makeText(this, "Preset: " + profileName(profile), Toast.LENGTH_SHORT).show();
        refreshMetrics();
    }

    private void prepareAndLaunch(String pkg) {
        refreshMetrics();
        if (canDrawOverlays()) startHud();
        else Toast.makeText(this, "HUD chưa có quyền overlay; vẫn mở game bình thường.", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> {
            if (!launchPackage(pkg)) Toast.makeText(this, "Không tìm thấy game đã chọn.", Toast.LENGTH_LONG).show();
        }, 220);
    }

    private void ensureHudPermissionAndStart() {
        if (canDrawOverlays()) {
            startHud();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayRequestPending = true;
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Bật “Hiển thị trên ứng dụng khác” rồi quay lại.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void startHud() {
        Intent service = new Intent(this, OverlayService.class).putExtra(KEY_PROFILE, profile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service); else startService(service);
        Toast.makeText(this, "HUD telemetry • " + profileName(profile), Toast.LENGTH_SHORT).show();
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

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
        }
    }

    private void openBatterySettings() {
        try { startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); }
        catch (Throwable e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void loadLastSession() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        long duration = p.getLong("last_duration_s", 0);
        if (duration <= 0) {
            lastSessionValue.setText("Chưa có dữ liệu phiên. Bật HUD, chơi game rồi bấm TẮT SESSION để lưu thống kê.");
            return;
        }
        float maxTemp = p.getFloat("last_max_temp", -1f);
        long minRam = p.getLong("last_min_ram", -1);
        int avgRtt = p.getInt("last_avg_rtt", -1);
        String time = String.format(Locale.US, "%02d:%02d", duration / 60, duration % 60);
        lastSessionValue.setText("Thời gian " + time + " • Nhiệt max " + (maxTemp > 0 ? String.format(Locale.US, "%.1f°C", maxTemp) : "?") + "\nRAM thấp nhất " + (minRam >= 0 ? minRam + " MB" : "?") + " • RTT* TB " + (avgRtt >= 0 ? avgRtt + " ms" : "?"));
    }

    static String profileName(int p) {
        if (p == PROFILE_BALANCED) return "BALANCED";
        if (p == PROFILE_COOL) return "COOL";
        return "TURBO NET";
    }

    private String profileButtonText() { return "MODE: " + profileName(profile); }

    private TextView metric(LinearLayout parent, String label) {
        LinearLayout box = card(CARD, 13);
        box.setPadding(dp(12), dp(9), dp(12), dp(9));
        TextView l = text(label, 9, MUTED, true);
        TextView v = text("—", 18, TEXT, true);
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
        TextView b = text(badge, 13, CYAN, true);
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
