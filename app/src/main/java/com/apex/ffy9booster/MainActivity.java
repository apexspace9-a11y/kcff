package com.apex.ffy9booster;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 10, 15);
    private static final int CARD = Color.rgb(16, 23, 32);
    private static final int CARD_2 = Color.rgb(24, 34, 46);
    private static final int CYAN = Color.rgb(0, 229, 255);
    private static final int LIME = Color.rgb(185, 255, 102);
    private static final int TEXT = Color.rgb(244, 248, 252);
    private static final int MUTED = Color.rgb(146, 161, 177);
    private static final int WARN = Color.rgb(255, 191, 71);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView ramValue;
    private TextView tempValue;
    private TextView cpuValue;
    private TextView deviceValue;
    private TextView statusValue;
    private boolean overlayRequestPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
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
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView eyebrow = text("HUAWEI Y9 2019 • KIRIN 710 PRESET", 11, CYAN, true);
        root.addView(eyebrow);

        TextView title = text("FF Y9 BOOSTER\nVIP PRO", 32, TEXT, true);
        title.setLineSpacing(0, 0.92f);
        title.setPadding(0, dp(4), 0, dp(3));
        root.addView(title);

        TextView subtitle = text("Booster không-root, không sửa file game. Có HUD FPS* + RAM + CPU + nhiệt + Wi‑Fi high-performance.", 13, MUTED, false);
        subtitle.setLineSpacing(dp(2), 1f);
        root.addView(subtitle, lp(-1, -2, dp(6), dp(14)));

        LinearLayout hero = card(CARD, 18);
        hero.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.addView(hero, lp(-1, -2, 0, dp(12)));

        statusValue = text("ĐANG KIỂM TRA THIẾT BỊ…", 12, LIME, true);
        hero.addView(statusValue);
        deviceValue = text("—", 20, TEXT, true);
        deviceValue.setPadding(0, dp(5), 0, dp(9));
        hero.addView(deviceValue);

        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        ramValue = metric(metricRow, "RAM TRỐNG");
        cpuValue = metric(metricRow, "CPU");
        tempValue = metric(metricRow, "PIN");
        hero.addView(metricRow, new LinearLayout.LayoutParams(-1, -2));

        Button launch = button("⚡  BOOST + MỞ FREE FIRE", CYAN, BG);
        launch.setOnClickListener(v -> {
            quickBoost();
            if (canDrawOverlays()) startHud();
            handler.postDelayed(this::launchFreeFire, 180);
        });
        root.addView(launch, lp(-1, dp(54), 0, dp(9)));

        LinearLayout pair = new LinearLayout(this);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        Button boost = button("DỌN NỀN", CARD_2, TEXT);
        boost.setOnClickListener(v -> quickBoost());
        Button hud = button("HUD FPS*", CARD_2, TEXT);
        hud.setOnClickListener(v -> ensureHudPermissionAndStart());
        pair.addView(boost, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair.addView(space(dp(9)));
        pair.addView(hud, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair, lp(-1, -2, 0, dp(9)));

        LinearLayout pair2 = new LinearLayout(this);
        pair2.setOrientation(LinearLayout.HORIZONTAL);
        Button max = button("FREE FIRE MAX", CARD_2, TEXT);
        max.setOnClickListener(v -> {
            quickBoost();
            if (canDrawOverlays()) startHud();
            handler.postDelayed(() -> launchPackage("com.dts.freefiremax"), 180);
        });
        Button stop = button("TẮT SESSION", CARD_2, WARN);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Đã tắt HUD / Wi‑Fi performance session", Toast.LENGTH_SHORT).show();
        });
        pair2.addView(max, new LinearLayout.LayoutParams(0, dp(50), 1f));
        pair2.addView(space(dp(9)));
        pair2.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(pair2, lp(-1, -2, 0, dp(14)));

        root.addView(section("PRESET Y9 2019"));

        LinearLayout preset = card(CARD, 16);
        preset.setPadding(dp(15), dp(14), dp(15), dp(14));
        root.addView(preset, lp(-1, -2, 0, dp(12)));
        preset.addView(line("✓", "Ưu tiên Free Fire thường", "Nhẹ hơn bản MAX trên Kirin 710 / RAM 3–4 GB."));
        preset.addView(line("✓", "Đồ họa thấp trước", "Nếu giật hoặc nóng, hạ chất lượng trong game thay vì tin nút “unlock GPU”."));
        preset.addView(line("✓", "Giữ nhiệt dễ chịu", "Nếu pin vượt khoảng 43°C, nghỉ cho máy nguội. Throttle nhiệt thì booster cũng chịu thua vật lý."));
        preset.addView(line("✓", "Wi‑Fi high-performance", "HUD giữ Wi‑Fi khỏi power-save quá mạnh trong phiên chơi. Tốn pin hơn một chút."));

        root.addView(section("BỘ ĐO"));
        LinearLayout hudInfo = card(CARD, 16);
        hudInfo.setPadding(dp(15), dp(14), dp(15), dp(14));
        root.addView(hudInfo, lp(-1, -2, 0, dp(12)));
        hudInfo.addView(line("FPS*", "Nhịp màn hình / Choreographer", "Không-root không đọc được FPS engine chính xác của app khác."));
        hudInfo.addView(line("RAM", "Bộ nhớ trống hệ thống", "Dùng để thấy máy có đang thiếu RAM hay không."));
        hudInfo.addView(line("CPU", "Tải CPU toàn máy", "Hiện “?” nếu firmware Huawei chặn /proc/stat."));
        hudInfo.addView(line("TEMP", "Nhiệt độ pin", "Không phải nhiệt độ lõi SoC, nhưng đủ hữu ích để phát hiện máy đang nóng."));

        Button settingsButton = button("MỞ CÀI ĐẶT PIN / APP", CARD_2, TEXT);
        settingsButton.setOnClickListener(v -> openBatterySettings());
        root.addView(settingsButton, lp(-1, dp(50), 0, dp(12)));

        TextView legal = text(
                "An toàn tài khoản: app không inject, không hook, không sửa APK/OBB/data Free Fire và không đụng anti-cheat. " +
                "Quick Boost chỉ dùng API Android bình thường; mức hiệu quả tùy EMUI.",
                11, MUTED, false);
        legal.setLineSpacing(dp(2), 1f);
        root.addView(legal);

        return scroll;
    }

    private TextView metric(LinearLayout parent, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(9), dp(10), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_2);
        bg.setCornerRadius(dp(12));
        box.setBackground(bg);

        TextView l = text(label, 9, MUTED, true);
        TextView v = text("—", 17, TEXT, true);
        v.setPadding(0, dp(3), 0, 0);
        box.addView(l);
        box.addView(v);
        parent.addView(box, new LinearLayout.LayoutParams(0, dp(68), 1f));

        if (parent.getChildCount() < 5) parent.addView(space(dp(7)));
        return v;
    }

    private View line(String badge, String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));

        TextView b = text(badge, 13, CYAN, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_2);
        bg.setCornerRadius(dp(8));
        b.setBackground(bg);
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

    private void refreshMetrics() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long freeMb = mi.availMem / (1024L * 1024L);
        ramValue.setText(freeMb + " MB");

        float temp = readBatteryTemp();
        tempValue.setText(temp > 0 ? String.format(Locale.US, "%.1f°C", temp) : "—");

        String model = Build.MANUFACTURER + " " + Build.MODEL;
        deviceValue.setText(model + " • Android " + Build.VERSION.RELEASE);
        boolean y9 = Build.MODEL != null && Build.MODEL.toUpperCase(Locale.US).startsWith("JKM");
        statusValue.setText(y9 ? "✓ PRESET Y9 2019 ĐANG HOẠT ĐỘNG" : "✓ CHẾ ĐỘ TƯƠNG THÍCH ANDROID");

        new Thread(() -> {
            int cpu = sampleCpu();
            handler.post(() -> cpuValue.setText(cpu >= 0 ? cpu + "%" : "?"));
        }).start();
    }

    private void quickBoost() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(before);

        int attempts = 0;
        try {
            List<ActivityManager.RunningAppProcessInfo> running = am.getRunningAppProcesses();
            if (running != null) {
                Set<String> packages = new HashSet<>();
                for (ActivityManager.RunningAppProcessInfo p : running) {
                    if (p == null || p.pkgList == null || p.uid <= 10000 || p.uid == android.os.Process.myUid()) continue;
                    for (String pkg : p.pkgList) {
                        if (pkg == null || shouldKeep(pkg)) continue;
                        packages.add(pkg);
                    }
                }
                for (String pkg : packages) {
                    try {
                        am.killBackgroundProcesses(pkg);
                        attempts++;
                    } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable ignored) { }

        final int finalAttempts = attempts;
        handler.postDelayed(() -> {
            ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(after);
            long mb = after.availMem / (1024L * 1024L);
            ramValue.setText(mb + " MB");
            String msg = finalAttempts > 0
                    ? "Boost xong: đã yêu cầu dừng " + finalAttempts + " app/process nền."
                    : "Boost xong. EMUI không cho app thường thấy nhiều process nền.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }, 300);
    }

    private boolean shouldKeep(String pkg) {
        return pkg.equals(getPackageName())
                || pkg.equals("com.dts.freefireth")
                || pkg.equals("com.dts.freefiremax")
                || pkg.startsWith("com.android.systemui")
                || pkg.startsWith("com.huawei.android.launcher")
                || pkg.startsWith("com.huawei.systemmanager")
                || pkg.startsWith("android");
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
        Intent service = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
        Toast.makeText(this, "HUD FPS* + game session đã bật.", Toast.LENGTH_SHORT).show();
    }

    private void launchFreeFire() {
        if (launchPackage("com.dts.freefireth")) return;
        if (launchPackage("com.dts.freefiremax")) return;
        Toast.makeText(this, "Không tìm thấy Free Fire / Free Fire MAX.", Toast.LENGTH_LONG).show();
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

    private void openBatterySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            startActivity(i);
        } catch (Throwable e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private float readBatteryTemp() {
        try {
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return -1f;
            int raw = battery.getIntExtra("temperature", -1);
            return raw > 0 ? raw / 10f : -1f;
        } catch (Throwable e) {
            return -1f;
        }
    }

    private int sampleCpu() {
        try {
            long[] a = readCpuStat();
            Thread.sleep(180);
            long[] b = readCpuStat();
            if (a == null || b == null) return -1;
            long idle = b[0] - a[0];
            long total = b[1] - a[1];
            if (total <= 0) return -1;
            return (int) Math.max(0, Math.min(100, ((total - idle) * 100L) / total));
        } catch (Throwable e) {
            return -1;
        }
    }

    private long[] readCpuStat() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = br.readLine();
            if (line == null || !line.startsWith("cpu ")) return null;
            String[] p = line.trim().split("\\s+");
            long user = Long.parseLong(p[1]);
            long nice = Long.parseLong(p[2]);
            long system = Long.parseLong(p[3]);
            long idle = Long.parseLong(p[4]);
            long iowait = p.length > 5 ? Long.parseLong(p[5]) : 0;
            long irq = p.length > 6 ? Long.parseLong(p[6]) : 0;
            long softirq = p.length > 7 ? Long.parseLong(p[7]) : 0;
            long steal = p.length > 8 ? Long.parseLong(p[8]) : 0;
            long idleAll = idle + iowait;
            long total = user + nice + system + idle + iowait + irq + softirq + steal;
            return new long[]{idleAll, total};
        } catch (Throwable e) {
            return null;
        }
    }

    private LinearLayout card(int color, int radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        l.setBackground(bg);
        return l;
    }

    private Button button(String s, int bgColor, int fgColor) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(fgColor);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
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
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private View space(int px) {
        View v = new View(this);
        v.setLayoutParams(new ViewGroup.LayoutParams(px, 1));
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
