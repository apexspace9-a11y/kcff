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
import android.os.PowerManager;
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
    private static final int NOTIFICATION_ID = 71040;
    private static final String CHANNEL_ID = "ff_y9_session_v4";

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
    private volatile int jitter = -1;
    private volatile int failure = -1;
    private volatile boolean networkUnstable = false;

    private float smoothRtt = -1f;
    private float smoothJitter = -1f;
    private int requestedProfile = MainActivity.PROFILE_ADAPTIVE;
    private int effectiveProfile = MainActivity.PROFILE_TURBO;
    private int adaptiveHeatLevel = 0;
    private boolean thermalOverride = false;
    private int autoCoolCount = 0;

    private long sessionStart;
    private long lastDeepProbe;
    private long lastHeartbeat;
    private long minRam = Long.MAX_VALUE;
    private int maxRamPressure = -1;
    private float maxTemp = -1f;
    private long rttSum = 0;
    private long jitterSum = 0;
    private int netCount = 0;
    private int successfulNetCount = 0;
    private int unstableSamples = 0;
    private int maxFailure = 0;
    private long cpuSum = 0;
    private int cpuCount = 0;
    private int maxCpu = -1;
    private int startBattery = -1;
    private final long[] profileSeconds = new long[3];

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
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        requestedProfile = p.getInt(MainActivity.KEY_PROFILE, MainActivity.PROFILE_ADAPTIVE);
        sessionStart = SystemClock.elapsedRealtime();
        startBattery = DeviceProbe.batteryPercent(this);

        createNotificationChannel();
        enterForeground();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (canDrawOverlays()) createOverlay();
        reconcileProfile(true);

        p.edit()
                .putBoolean("session_active", true)
                .putLong("session_heartbeat_ms", System.currentTimeMillis())
                .apply();
        handler.post(updateTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            requestedProfile = intent.getIntExtra(MainActivity.KEY_PROFILE, requestedProfile);
        }
        reconcileProfile(true);
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

    private void reconcileProfile(boolean force) {
        float temp = DeviceProbe.batteryTempC(this);
        int thermal = DeviceProbe.thermalStatus(this);

        boolean emergency = temp >= 44f
                || thermal >= PowerManager.THERMAL_STATUS_SEVERE;
        boolean wasOverride = thermalOverride;
        thermalOverride = emergency;
        if (!wasOverride && thermalOverride) autoCoolCount++;

        int desired;
        if (emergency) {
            desired = MainActivity.PROFILE_COOL;
        } else if (requestedProfile == MainActivity.PROFILE_ADAPTIVE) {
            boolean moderateThermal = thermal >= PowerManager.THERMAL_STATUS_MODERATE;
            boolean lightThermal = thermal >= PowerManager.THERMAL_STATUS_LIGHT;

            if (adaptiveHeatLevel == 2) {
                if (temp > 0 && temp <= 39.4f && !moderateThermal) adaptiveHeatLevel = 1;
            } else if (adaptiveHeatLevel == 1) {
                if (temp >= 42.5f || thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
                    adaptiveHeatLevel = 2;
                } else if (temp > 0 && temp <= 38.7f && !lightThermal) {
                    adaptiveHeatLevel = 0;
                }
            } else {
                if (temp >= 42.5f || thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
                    adaptiveHeatLevel = 2;
                } else if (temp >= 40.5f || moderateThermal) {
                    adaptiveHeatLevel = 1;
                }
            }

            if (DeviceProbe.isPowerSave(this) || adaptiveHeatLevel >= 2) {
                desired = MainActivity.PROFILE_COOL;
            } else if (adaptiveHeatLevel == 1) {
                desired = MainActivity.PROFILE_BALANCED;
            } else {
                desired = MainActivity.PROFILE_TURBO;
            }
        } else {
            desired = requestedProfile;
        }

        if (force || desired != effectiveProfile) {
            effectiveProfile = desired;
            applyWifiProfile(effectiveProfile);
            refreshNotification();
        }
    }

    private void applyWifiProfile(int profile) {
        releaseWifiLock();
        if (profile == MainActivity.PROFILE_COOL || !DeviceProbe.isWifiConnected(this)) return;

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;

            int mode;
            if (profile == MainActivity.PROFILE_TURBO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = WifiManager.WIFI_MODE_FULL_LOW_LATENCY;
            } else {
                mode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
            }
            wifiLock = wm.createWifiLock(mode, "ffy9booster:v4:" + profile);
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } catch (Throwable ignored) {
            wifiLock = null;
        }
    }

    private void createOverlay() {
        overlay = new TextView(this);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(10.3f);
        overlay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        overlay.setGravity(Gravity.START);
        overlay.setPadding(dp(10), dp(7), dp(10), dp(7));
        overlay.setText("FF Y9 V4\nđang đo…");
        setOverlayBackground(Color.rgb(0, 229, 255));

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(8);
        params.y = dp(58);
        overlay.setOnTouchListener(new DragTouchListener());

        try {
            windowManager.addView(overlay, params);
        } catch (Throwable e) {
            overlay = null;
        }
    }

    private void updateOverlay() {
        reconcileProfile(false);

        DeviceProbe.MemorySnapshot mem = DeviceProbe.memory(this);
        float temp = DeviceProbe.batteryTempC(this);
        int battery = DeviceProbe.batteryPercent(this);
        boolean charging = DeviceProbe.isCharging(this);
        float hz = DeviceProbe.displayHz(this);
        String net = DeviceProbe.networkLabel(this);
        String thermal = DeviceProbe.thermalLabel(this);
        long elapsed = Math.max(0, (SystemClock.elapsedRealtime() - sessionStart) / 1000L);

        if (mem.availMb >= 0) minRam = Math.min(minRam, mem.availMb);
        if (mem.pressurePercent >= 0) maxRamPressure = Math.max(maxRamPressure, mem.pressurePercent);
        if (temp > 0) maxTemp = Math.max(maxTemp, temp);
        if (effectiveProfile >= 0 && effectiveProfile <= MainActivity.PROFILE_COOL) {
            profileSeconds[effectiveProfile]++;
        }

        long now = SystemClock.elapsedRealtime();
        long probeEvery;
        int probeCount;
        if (networkUnstable) {
            probeEvery = effectiveProfile == MainActivity.PROFILE_COOL ? 6000 : 3500;
            probeCount = 4;
        } else {
            probeEvery = effectiveProfile == MainActivity.PROFILE_TURBO
                    ? 7000 : effectiveProfile == MainActivity.PROFILE_BALANCED ? 9500 : 12500;
            probeCount = 2;
        }

        if (!sampling && now - lastDeepProbe >= probeEvery) {
            lastDeepProbe = now;
            sampling = true;
            final int count = probeCount;
            executor.submit(() -> {
                int newCpu = DeviceProbe.sampleCpuPercent();
                DeviceProbe.NetworkSample sample = DeviceProbe.isConnected(this)
                        ? DeviceProbe.probeNetwork(count, 650)
                        : DeviceProbe.NetworkSample.unavailable(count);

                cpu = newCpu;
                rtt = sample.medianRttMs;
                jitter = sample.jitterMs;
                failure = sample.failurePercent;
                networkUnstable = sample.unstable();

                if (newCpu >= 0) {
                    cpuSum += newCpu;
                    cpuCount++;
                    maxCpu = Math.max(maxCpu, newCpu);
                }

                if (sample.medianRttMs >= 0) {
                    smoothRtt = smoothRtt < 0
                            ? sample.medianRttMs
                            : smoothRtt * 0.68f + sample.medianRttMs * 0.32f;
                    smoothJitter = smoothJitter < 0
                            ? Math.max(0, sample.jitterMs)
                            : smoothJitter * 0.68f + Math.max(0, sample.jitterMs) * 0.32f;
                    rttSum += sample.medianRttMs;
                    if (sample.jitterMs >= 0) jitterSum += sample.jitterMs;
                    netCount++;
                    successfulNetCount++;
                    if (sample.unstable()) unstableSamples++;
                    maxFailure = Math.max(maxFailure, Math.max(0, sample.failurePercent));
                } else if (sample.failurePercent >= 0) {
                    netCount++;
                    unstableSamples++;
                    maxFailure = Math.max(maxFailure, sample.failurePercent);
                }
                sampling = false;
            });
        }

        int stability = liveStabilityScore(mem, temp);
        String state = "STABLE";
        int border = Color.rgb(0, 229, 255);

        if (thermalOverride || temp >= 43f) {
            state = "THERMAL";
            border = Color.rgb(255, 94, 103);
        } else if (mem.lowMemory || (mem.availMb >= 0 && mem.availMb < 520)) {
            state = "RAM";
            border = Color.rgb(255, 188, 74);
        } else if (networkUnstable) {
            state = "NET";
            border = Color.rgb(255, 188, 74);
        } else if (stability < 70) {
            state = "WATCH";
            border = Color.rgb(255, 188, 74);
        }
        setOverlayBackground(border);

        String hzText = hz > 0 ? String.format(Locale.US, "%.0fHz", hz) : "?Hz";
        String tempText = temp > 0 ? String.format(Locale.US, "%.1fC", temp) : "?C";
        String cpuText = cpu >= 0 ? cpu + "%" : "?";
        String rttText = smoothRtt >= 0 ? Math.round(smoothRtt) + "ms" : "?";
        String jitterText = smoothJitter >= 0 ? Math.round(smoothJitter) + "ms" : "?";
        String failureText = failure >= 0 ? failure + "%" : "?";
        String time = String.format(Locale.US, "%02d:%02d", elapsed / 60, elapsed % 60);
        String lock = wifiLock != null && wifiLock.isHeld()
                ? (effectiveProfile == MainActivity.PROFILE_TURBO && Build.VERSION.SDK_INT >= 29
                    ? "LOW-LAT" : "HIGH-PERF")
                : "NO-LOCK";
        String profileText = requestedProfile == MainActivity.PROFILE_ADAPTIVE
                ? "A>" + MainActivity.profileShortName(effectiveProfile)
                : MainActivity.profileShortName(requestedProfile)
                    + (thermalOverride ? ">C" : "");
        String ramText = mem.availMb >= 0 ? mem.availMb + "M" : "?M";
        String pressureText = mem.pressurePercent >= 0 ? mem.pressurePercent + "%" : "?";

        if (overlay != null) {
            if (compact) {
                overlay.setText("S" + stability + " | " + profileText + " | " + rttText
                        + " J" + jitterText + " | " + tempText + " | " + ramText);
            } else {
                overlay.setText(
                        "FF Y9 V4 • S" + stability + " • " + profileText + " • " + time + " • " + state
                        + "\nDISPLAY " + hzText + "   RTT* " + rttText
                        + "\nJIT* " + jitterText + "   FAIL* " + failureText
                        + "\nCPU " + cpuText + "   RAM " + ramText + " P" + pressureText
                        + "\nTEMP " + tempText + "   BAT " + (battery >= 0 ? battery + "%" : "?")
                            + (charging ? " CHG" : "")
                        + "\n" + thermal + " • " + net + " • " + lock
                        + "\nPROBE " + (networkUnstable ? "DEEP/FAST" : "LIGHT/SLOW"));
            }
        }

        if (now - lastHeartbeat >= 5000) {
            lastHeartbeat = now;
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit()
                    .putBoolean("session_active", true)
                    .putLong("session_heartbeat_ms", System.currentTimeMillis())
                    .putInt("live_stability", stability)
                    .putInt("live_effective_profile", effectiveProfile)
                    .putInt("live_rtt", rtt)
                    .putInt("live_jitter", jitter)
                    .putInt("live_failure", failure)
                    .apply();
        }
    }

    private int liveStabilityScore(DeviceProbe.MemorySnapshot mem, float temp) {
        int s = 100;
        if (thermalOverride || temp >= 44f) s -= 35;
        else if (temp >= 42f) s -= 20;
        else if (temp >= 40f) s -= 8;

        if (mem.lowMemory) s -= 25;
        if (mem.availMb >= 0 && mem.availMb < 450) s -= 20;
        else if (mem.availMb >= 0 && mem.availMb < 750) s -= 10;
        if (mem.pressurePercent >= 94) s -= 10;
        else if (mem.pressurePercent >= 88) s -= 5;

        int srtt = smoothRtt >= 0 ? Math.round(smoothRtt) : rtt;
        int sjit = smoothJitter >= 0 ? Math.round(smoothJitter) : jitter;
        if (srtt >= 180) s -= 18;
        else if (srtt >= 110) s -= 10;
        if (sjit >= 55) s -= 18;
        else if (sjit >= 30) s -= 10;
        if (failure >= 50) s -= 18;
        else if (failure >= 25) s -= 8;
        return Math.max(0, Math.min(100, s));
    }

    private void setOverlayBackground(int borderColor) {
        if (overlay == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(220, 7, 12, 18));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), borderColor);
        overlay.setBackground(bg);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Free Fire Stability Session", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("HUD V4, adaptive thermal guard và dynamic network probe");
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        int imm = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open, imm);

        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent stopPi = PendingIntent.getService(this, 2, stop, imm);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        String profileText = requestedProfile == MainActivity.PROFILE_ADAPTIVE
                ? "ADAPTIVE > " + MainActivity.profileName(effectiveProfile)
                : MainActivity.profileName(requestedProfile)
                    + (thermalOverride ? " > COOL" : "");

        return b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("FF Y9 Booster V4 • " + profileText)
                .setContentText(networkUnstable
                        ? "Dynamic probe đang theo dõi mạng bất ổn"
                        : "Stability HUD + thermal guard")
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Tắt + lưu", stopPi).build())
                .build();
    }

    private void refreshNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
        } catch (Throwable ignored) { }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void releaseWifiLock() {
        if (wifiLock == null) return;
        try {
            if (wifiLock.isHeld()) wifiLock.release();
        } catch (Throwable ignored) { }
        wifiLock = null;
    }

    private void saveSessionSummary() {
        long duration = Math.max(0, (SystemClock.elapsedRealtime() - sessionStart) / 1000L);
        int avgRtt = successfulNetCount > 0 ? (int) (rttSum / successfulNetCount) : -1;
        int avgJitter = successfulNetCount > 0 ? (int) (jitterSum / successfulNetCount) : -1;
        int avgCpu = cpuCount > 0 ? (int) (cpuSum / cpuCount) : -1;
        int endBattery = DeviceProbe.batteryPercent(this);

        SessionRecord record = new SessionRecord(
                System.currentTimeMillis(),
                duration,
                maxTemp,
                minRam == Long.MAX_VALUE ? -1 : minRam,
                avgRtt,
                avgJitter,
                maxFailure,
                requestedProfile,
                autoCoolCount,
                avgCpu,
                maxCpu,
                maxRamPressure,
                startBattery,
                endBattery,
                unstableSamples,
                netCount);

        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        for (int i = 4; i >= 1; i--) {
            String previous = p.getString("history_" + (i - 1), null);
            if (previous != null) e.putString("history_" + i, previous);
            else e.remove("history_" + i);
        }
        e.putString("history_0", record.encode());
        e.putBoolean("session_active", false);
        e.putLong("session_heartbeat_ms", 0);
        e.putInt("last_session_score", record.score());
        e.putString("last_session_grade", record.grade());
        e.putString("last_session_issue", record.dominantIssue());
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

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class DragTouchListener implements View.OnTouchListener {
        private int startX;
        private int startY;
        private float downX;
        private float downY;

        @Override public boolean onTouch(View v, MotionEvent event) {
            if (params == null || windowManager == null) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int) (event.getRawX() - downX);
                    params.y = startY + (int) (event.getRawY() - downY);
                    try { windowManager.updateViewLayout(overlay, params); } catch (Throwable ignored) { }
                    return true;

                case MotionEvent.ACTION_UP:
                    float dx = Math.abs(event.getRawX() - downX);
                    float dy = Math.abs(event.getRawY() - downY);
                    if (dx < dp(8) && dy < dp(8)) {
                        compact = !compact;
                        updateOverlay();
                    }
                    return true;

                default:
                    return true;
            }
        }
    }
}
