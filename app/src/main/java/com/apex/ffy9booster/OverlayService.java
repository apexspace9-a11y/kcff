package com.apex.ffy9booster;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public class OverlayService extends Service {
    private static final int NOTIFICATION_ID = 71019;
    private static final String CHANNEL_ID = "ff_y9_session";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private TextView overlay;
    private WindowManager.LayoutParams params;
    private WifiManager.WifiLock wifiLock;

    private long frameCount = 0;
    private long lastFrameSnapshot = 0;
    private long lastFpsAt = 0;
    private int fps = 0;
    private int cpu = -1;
    private boolean running = true;

    private final Choreographer.FrameCallback frameCallback = frameTimeNanos -> {
        if (!running) return;
        frameCount++;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    };

    private final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            updateOverlay();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        acquireWifiPerformanceLock();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (canDrawOverlays()) createOverlay();

        lastFpsAt = System.currentTimeMillis();
        Choreographer.getInstance().postFrameCallback(frameCallback);
        handler.post(updateTask);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (overlay == null && canDrawOverlays()) createOverlay();
        return START_STICKY;
    }

    private void createOverlay() {
        if (overlay != null) return;

        overlay = new TextView(this);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(11);
        overlay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        overlay.setGravity(Gravity.START);
        overlay.setPadding(dp(10), dp(7), dp(10), dp(7));
        overlay.setText("FF BOOST\nđang khởi động…");

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(205, 8, 13, 19));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), Color.rgb(0, 229, 255));
        overlay.setBackground(bg);

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
        params.y = dp(60);

        overlay.setOnTouchListener(new DragTouchListener());
        try {
            windowManager.addView(overlay, params);
        } catch (Throwable e) {
            overlay = null;
        }
    }

    private void updateOverlay() {
        long now = System.currentTimeMillis();
        long dt = now - lastFpsAt;
        if (dt >= 900) {
            long frames = frameCount - lastFrameSnapshot;
            fps = dt > 0 ? (int) Math.round(frames * 1000.0 / dt) : 0;
            lastFrameSnapshot = frameCount;
            lastFpsAt = now;
        }

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long ramMb = mi.availMem / (1024L * 1024L);

        float temp = readBatteryTemp();
        String net = networkLabel();

        if (overlay != null) {
            String tempText = temp > 0 ? String.format(Locale.US, "%.1f°C", temp) : "—";
            String cpuText = cpu >= 0 ? cpu + "%" : "?";
            String state = temp >= 43f ? "HOT" : (ramMb < 550 ? "LOW RAM" : "VIP");
            overlay.setText(
                    "FPS* " + fps + "  " + state +
                    "\nCPU " + cpuText + "  RAM " + ramMb + "M" +
                    "\nTEMP " + tempText + "  " + net);
        }

        new Thread(() -> cpu = sampleCpu()).start();
    }

    private void acquireWifiPerformanceLock() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ffy9booster:highperf");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } catch (Throwable ignored) {
            wifiLock = null;
        }
    }

    private String networkLabel() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
            if (info == null || !info.isConnected()) return "OFFLINE";
            if (info.getType() == ConnectivityManager.TYPE_WIFI) return "Wi‑Fi HP";
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) return "MOBILE";
            return info.getTypeName();
        } catch (Throwable e) {
            return "NET ?";
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

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Free Fire Booster Session", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("HUD và Wi‑Fi performance mode");
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 1, open,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent stopPi = PendingIntent.getService(
                this, 2, stop,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("FF Y9 Booster đang chạy")
                .setContentText("HUD FPS* + Wi‑Fi high-performance")
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Tắt", stopPi).build())
                .build();
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        } catch (Throwable ignored) { }

        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Throwable ignored) { }
            overlay = null;
        }
        if (wifiLock != null) {
            try {
                if (wifiLock.isHeld()) wifiLock.release();
            } catch (Throwable ignored) { }
            wifiLock = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
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
                default:
                    return true;
            }
        }
    }
}
