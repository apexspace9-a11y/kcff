package com.apex.ffy9booster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverlayService extends Service {
    private static final int NOTIFICATION_ID = 71020;
    private static final String CHANNEL_ID = "ff_y9_session_v2";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WindowManager windowManager;
    private TextView overlay;
    private WindowManager.LayoutParams params;
    private WifiManager.WifiLock wifiLock;
    private boolean running = true;
    private boolean compact = false;
    private volatile boolean sampling = false;
    private volatile int cpu = -1;
    private volatile int rtt = -1;
    private int profile = MainActivity.PROFILE_TURBO;
    private long sessionStart;
    private long lastDeepProbe;
    private long minRam = Long.MAX_VALUE;
    private float maxTemp = -1f;
    private long rttSum = 0;
    private int rttCount = 0;

    private final Runnable updateTask = new Runnable() {
        @Override public void run() {
            if (!running) return;
            updateOverlay();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sessionStart = SystemClock.elapsedRealtime();
        createNotificationChannel();
        enterForeground();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (canDrawOverlays()) createOverlay();
        applyProfile(profile);
        handler.post(updateTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) profile = intent.getIntExtra(MainActivity.KEY_PROFILE, profile);
        applyProfile(profile);
        if (overlay == null && canDrawOverlays()) createOverlay();
        refreshNotification();
        return START_STICKY;
    }

    private void enterForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private void applyProfile(int next) {
        profile = Math.max(MainActivity.PROFILE_TURBO, Math.min(MainActivity.PROFILE_COOL, next));
        releaseWifiLock();
        if (profile == MainActivity.PROFILE_COOL) return;
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            int mode;
            if (profile == MainActivity.PROFILE_TURBO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = WifiManager.WIFI_MODE_FULL_LOW_LATENCY;
            } else {
                mode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
            }
            wifiLock = wm.createWifiLock(mode, "ffy9booster:v2:" + profile);
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } catch (Throwable ignored) {
            wifiLock = null;
        }
    }

    private void createOverlay() {
        overlay = new TextView(this);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(10.5f);
        overlay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        overlay.setGravity(Gravity.START);
        overlay.setPadding(dp(10), dp(7), dp(10), dp(7));
        overlay.setText("FF Y9 V2\nđang đo…");
        setOverlayBackground(Color.rgb(0, 229, 255));

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(8);
        params.y = dp(58);
        overlay.setOnTouchListener(new DragTouchListener());
        try { windowManager.addView(overlay, params); }
        catch (Throwable e) { overlay = null; }
    }

    private void updateOverlay() {
        long ram = DeviceProbe.freeRamMb(this);
        float temp = DeviceProbe.batteryTempC(this);
        int battery = DeviceProbe.batteryPercent(this);
        float hz = DeviceProbe.displayHz(this);
        String net = DeviceProbe.networkLabel(this);
        String thermal = DeviceProbe.thermalLabel(this);
        long elapsed = Math.max(0, (SystemClock.elapsedRealtime() - sessionStart) / 1000L);
        if (ram >= 0) minRam = Math.min(minRam, ram);
        if (temp > 0) maxTemp = Math.max(maxTemp, temp);

        long now = SystemClock.elapsedRealtime();
        long probeEvery = profile == MainActivity.PROFILE_TURBO ? 4000 : profile == MainActivity.PROFILE_BALANCED ? 6000 : 8000;
        if (!sampling && now - lastDeepProbe >= probeEvery) {
            lastDeepProbe = now;
            sampling = true;
            executor.submit(() -> {
                int newCpu = DeviceProbe.sampleCpuPercent();
                int newRtt = DeviceProbe.isConnected(this) ? DeviceProbe.connectRttMs() : -1;
                cpu = newCpu;
                rtt = newRtt;
                if (newRtt >= 0) {
                    rttSum += newRtt;
                    rttCount++;
                }
                sampling = false;
            });
        }

        String state = "OK";
        int border = Color.rgb(0, 229, 255);
        if (temp >= 43f) { state = "HOT"; border = Color.rgb(255, 94, 103); }
        else if (ram >= 0 && ram < 550) { state = "RAM!"; border = Color.rgb(255, 188, 74); }
        else if (rtt >= 150) { state = "NET!"; border = Color.rgb(255, 188, 74); }
        setOverlayBackground(border);

        String hzText = hz > 0 ? String.format(Locale.US, "%.0fHz", hz) : "?Hz";
        String tempText = temp > 0 ? String.format(Locale.US, "%.1fC", temp) : "?C";
        String cpuText = cpu >= 0 ? cpu + "%" : "?";
        String rttText = rtt >= 0 ? rtt + "ms" : "?";
        String time = String.format(Locale.US, "%02d:%02d", elapsed / 60, elapsed % 60);
        String lock = wifiLock != null && wifiLock.isHeld() ? (profile == MainActivity.PROFILE_TURBO && Build.VERSION.SDK_INT >= 29 ? "LOW-LAT" : "HIGH-PERF") : "NO-LOCK";

        if (overlay != null) {
            if (compact) {
                overlay.setText(hzText + " | " + rttText + " | " + tempText + " | " + (ram >= 0 ? ram + "M" : "?M"));
            } else {
                overlay.setText(
                        "FF Y9 V2 • " + MainActivity.profileName(profile) + " • " + time + " • " + state +
                        "\nDISPLAY " + hzText + "   RTT* " + rttText +
                        "\nCPU " + cpuText + "   RAM " + (ram >= 0 ? ram + "M" : "?") +
                        "\nTEMP " + tempText + "   BAT " + (battery >= 0 ? battery + "%" : "?") +
                        "\n" + thermal + " • " + net + " • " + lock);
            }
        }
    }

    private void setOverlayBackground(int borderColor) {
        if (overlay == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(218, 7, 12, 18));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), borderColor);
        overlay.setBackground(bg);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Free Fire Telemetry Session", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("HUD telemetry và Wi-Fi performance mode trong phiên chơi");
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        int imm = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open, imm);
        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent stopPi = PendingIntent.getService(this, 2, stop, imm);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("FF Y9 Booster V2 • " + MainActivity.profileName(profile))
                .setContentText("HUD telemetry + network performance session")
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Tắt", stopPi).build())
                .build();
    }

    private void refreshNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, buildNotification());
        } catch (Throwable ignored) { }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void releaseWifiLock() {
        if (wifiLock == null) return;
        try { if (wifiLock.isHeld()) wifiLock.release(); } catch (Throwable ignored) { }
        wifiLock = null;
    }

    private void saveSessionSummary() {
        long duration = Math.max(0, (SystemClock.elapsedRealtime() - sessionStart) / 1000L);
        int avgRtt = rttCount > 0 ? (int) (rttSum / rttCount) : -1;
        SharedPreferences.Editor e = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit();
        e.putLong("last_duration_s", duration);
        e.putFloat("last_max_temp", maxTemp);
        e.putLong("last_min_ram", minRam == Long.MAX_VALUE ? -1 : minRam);
        e.putInt("last_avg_rtt", avgRtt);
        e.apply();
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        saveSessionSummary();
        releaseWifiLock();
        executor.shutdownNow();
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Throwable ignored) { }
            overlay = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private class DragTouchListener implements View.OnTouchListener {
        private int startX, startY;
        private float downX, downY;
        @Override public boolean onTouch(View v, MotionEvent event) {
            if (params == null || windowManager == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x; startY = params.y; downX = event.getRawX(); downY = event.getRawY(); return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int) (event.getRawX() - downX);
                    params.y = startY + (int) (event.getRawY() - downY);
                    try { windowManager.updateViewLayout(overlay, params); } catch (Throwable ignored) { }
                    return true;
                case MotionEvent.ACTION_UP:
                    float dx = Math.abs(event.getRawX() - downX);
                    float dy = Math.abs(event.getRawY() - downY);
                    if (dx < dp(8) && dy < dp(8)) { compact = !compact; updateOverlay(); }
                    return true;
                default: return true;
            }
        }
    }
}
