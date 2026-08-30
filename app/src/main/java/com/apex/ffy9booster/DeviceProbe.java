package com.apex.ffy9booster;

import android.app.ActivityManager;
import android.os.BatteryManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;

final class DeviceProbe {
    private DeviceProbe() {}

    static final class MemorySnapshot {
        final long availMb;
        final long totalMb;
        final long thresholdMb;
        final boolean lowMemory;
        final int pressurePercent;

        MemorySnapshot(long availMb, long totalMb, long thresholdMb, boolean lowMemory) {
            this.availMb = availMb;
            this.totalMb = totalMb;
            this.thresholdMb = thresholdMb;
            this.lowMemory = lowMemory;
            if (availMb >= 0 && totalMb > 0) {
                long used = Math.max(0, totalMb - Math.min(totalMb, availMb));
                this.pressurePercent = (int) Math.max(0, Math.min(100, Math.round(used * 100f / totalMb)));
            } else {
                this.pressurePercent = -1;
            }
        }

        static MemorySnapshot unavailable() {
            return new MemorySnapshot(-1, -1, -1, false);
        }
    }

    static final class NetworkSample {
        final int medianRttMs;
        final int jitterMs;
        final int failurePercent;
        final int successes;
        final int total;

        NetworkSample(int medianRttMs, int jitterMs, int failurePercent, int successes, int total) {
            this.medianRttMs = medianRttMs;
            this.jitterMs = jitterMs;
            this.failurePercent = failurePercent;
            this.successes = successes;
            this.total = total;
        }

        static NetworkSample unavailable(int total) {
            return new NetworkSample(-1, -1, total > 0 ? 100 : -1, 0, total);
        }

        boolean unstable() {
            return failurePercent >= 25 || jitterMs >= 30 || medianRttMs >= 120;
        }
    }

    static MemorySnapshot memory(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return new MemorySnapshot(
                    mi.availMem / (1024L * 1024L),
                    mi.totalMem / (1024L * 1024L),
                    mi.threshold / (1024L * 1024L),
                    mi.lowMemory);
        } catch (Throwable e) {
            return MemorySnapshot.unavailable();
        }
    }

    static long freeRamMb(Context context) {
        return memory(context).availMb;
    }

    static boolean isLowMemory(Context context) {
        return memory(context).lowMemory;
    }

    static float batteryTempC(Context context) {
        Intent battery = batteryIntent(context);
        if (battery == null) return -1f;
        int raw = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        return raw > 0 ? raw / 10f : -1f;
    }

    static int batteryPercent(Context context) {
        Intent battery = batteryIntent(context);
        if (battery == null) return -1;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return -1;
        return Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
    }

    static boolean isCharging(Context context) {
        Intent battery = batteryIntent(context);
        if (battery == null) return false;
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    static int batteryCurrentMa(Context context) {
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm == null) return -1;
            long ua = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if (ua == Long.MIN_VALUE || ua == 0) return -1;
            long ma = Math.abs(ua) / 1000L;
            if (ma <= 0 || ma > 10000) return -1;
            return (int) ma;
        } catch (Throwable e) {
            return -1;
        }
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

    static boolean isWifiConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
            return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
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

    static long freeStorageMb() {
        try {
            StatFs stat = new StatFs("/data");
            return stat.getAvailableBytes() / (1024L * 1024L);
        } catch (Throwable e) {
            return -1;
        }
    }

    static int thermalStatus(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null ? pm.getCurrentThermalStatus() : -1;
        } catch (Throwable e) {
            return -1;
        }
    }

    static float thermalHeadroom(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return -1f;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null) return -1f;
            float value = pm.getThermalHeadroom(5);
            if (Float.isNaN(value) || value < 0f) return -1f;
            return value;
        } catch (Throwable e) {
            return -1f;
        }
    }

    static String thermalLabel(Context context) {
        int status = thermalStatus(context);
        if (status >= 0) {
            switch (status) {
                case PowerManager.THERMAL_STATUS_NONE: return "THERMAL OK";
                case PowerManager.THERMAL_STATUS_LIGHT: return "THERMAL LIGHT";
                case PowerManager.THERMAL_STATUS_MODERATE: return "THERMAL MOD";
                case PowerManager.THERMAL_STATUS_SEVERE: return "THERMAL SEVERE";
                case PowerManager.THERMAL_STATUS_CRITICAL: return "THERMAL CRIT";
                case PowerManager.THERMAL_STATUS_EMERGENCY: return "THERMAL EMERG";
                case PowerManager.THERMAL_STATUS_SHUTDOWN: return "THERMAL STOP";
                default: return "THERMAL ?";
            }
        }
        float t = batteryTempC(context);
        if (t >= 43f) return "BAT HOT";
        if (t >= 40f) return "BAT WARM";
        return "BAT OK";
    }

    static NetworkSample probeNetwork() {
        return probeNetwork(4, 650);
    }

    static NetworkSample probeNetwork(int count, int timeoutMs) {
        int total = Math.max(1, Math.min(6, count));
        String[] hosts = {"1.1.1.1", "8.8.8.8", "9.9.9.9", "208.67.222.222"};
        int[] good = new int[total];
        int goodCount = 0;

        for (int i = 0; i < total; i++) {
            int value = connectRtt(hosts[i % hosts.length], 443, timeoutMs);
            if (value >= 0) good[goodCount++] = value;
        }

        if (goodCount == 0) return NetworkSample.unavailable(total);

        int[] values = Arrays.copyOf(good, goodCount);
        Arrays.sort(values);
        int median = values[goodCount / 2];
        if (goodCount % 2 == 0) {
            median = (values[goodCount / 2 - 1] + values[goodCount / 2]) / 2;
        }

        long deviation = 0;
        for (int value : values) deviation += Math.abs(value - median);
        int jitter = (int) Math.round(deviation / (double) goodCount);
        int failure = Math.round((total - goodCount) * 100f / total);
        return new NetworkSample(median, jitter, failure, goodCount, total);
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

    static String storageText() {
        long mb = freeStorageMb();
        if (mb < 0) return "?";
        if (mb >= 1024) return String.format(Locale.US, "%.1f GB trống", mb / 1024f);
        return mb + " MB trống";
    }
}
