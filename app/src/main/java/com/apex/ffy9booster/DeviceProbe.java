package com.apex.ffy9booster;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

final class DeviceProbe {
    private DeviceProbe() {}

    static long freeRamMb(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.availMem / (1024L * 1024L);
        } catch (Throwable e) {
            return -1;
        }
    }

    static float batteryTempC(Context context) {
        Intent battery = batteryIntent(context);
        if (battery == null) return -1f;
        int raw = battery.getIntExtra("temperature", -1);
        return raw > 0 ? raw / 10f : -1f;
    }

    static int batteryPercent(Context context) {
        Intent battery = batteryIntent(context);
        if (battery == null) return -1;
        int level = battery.getIntExtra("level", -1);
        int scale = battery.getIntExtra("scale", -1);
        if (level < 0 || scale <= 0) return -1;
        return Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
    }

    private static Intent batteryIntent(Context context) {
        try {
            return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Throwable e) {
            return null;
        }
    }

    static boolean isPowerSave(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isPowerSaveMode();
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean isConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
            return info != null && info.isConnected();
        } catch (Throwable e) {
            return false;
        }
    }

    static String networkLabel(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
            if (info == null || !info.isConnected()) return "OFFLINE";
            if (info.getType() == ConnectivityManager.TYPE_WIFI) return "Wi-Fi";
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) return "MOBILE";
            return info.getTypeName();
        } catch (Throwable e) {
            return "NET ?";
        }
    }

    static float displayHz(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm != null ? wm.getDefaultDisplay() : null;
            return display != null ? display.getRefreshRate() : -1f;
        } catch (Throwable e) {
            return -1f;
        }
    }

    static String thermalLabel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    switch (pm.getCurrentThermalStatus()) {
                        case PowerManager.THERMAL_STATUS_NONE: return "THERMAL OK";
                        case PowerManager.THERMAL_STATUS_LIGHT: return "THERMAL LIGHT";
                        case PowerManager.THERMAL_STATUS_MODERATE: return "THERMAL MOD";
                        case PowerManager.THERMAL_STATUS_SEVERE: return "THERMAL SEVERE";
                        case PowerManager.THERMAL_STATUS_CRITICAL: return "THERMAL CRIT";
                        case PowerManager.THERMAL_STATUS_EMERGENCY: return "THERMAL EMERG";
                        case PowerManager.THERMAL_STATUS_SHUTDOWN: return "THERMAL STOP";
                    }
                }
            } catch (Throwable ignored) { }
        }
        float t = batteryTempC(context);
        if (t >= 43f) return "BAT HOT";
        if (t >= 40f) return "BAT WARM";
        return "BAT OK";
    }

    static int connectRttMs() {
        int rtt = connectRtt("1.1.1.1", 443, 900);
        if (rtt >= 0) return rtt;
        return connectRtt("8.8.8.8", 443, 900);
    }

    private static int connectRtt(String host, int port, int timeoutMs) {
        Socket socket = new Socket();
        try {
            long start = SystemClock.elapsedRealtime();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long elapsed = SystemClock.elapsedRealtime() - start;
            return (int) Math.max(1, Math.min(9999, elapsed));
        } catch (Throwable e) {
            return -1;
        } finally {
            try { socket.close(); } catch (Throwable ignored) { }
        }
    }

    static int sampleCpuPercent() {
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

    private static long[] readCpuStat() {
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

    static String hzText(Context context) {
        float hz = displayHz(context);
        return hz > 0 ? String.format(Locale.US, "%.0f Hz", hz) : "? Hz";
    }
}
