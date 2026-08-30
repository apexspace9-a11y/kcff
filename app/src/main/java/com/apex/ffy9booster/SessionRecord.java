package com.apex.ffy9booster;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class SessionRecord {
    final long timestampMs;
    final long durationSec;
    final float maxTempC;
    final long minRamMb;
    final int avgRttMs;
    final int avgJitterMs;
    final int maxFailurePercent;
    final int requestedProfile;
    final int autoCoolCount;

    SessionRecord(long timestampMs, long durationSec, float maxTempC, long minRamMb,
                  int avgRttMs, int avgJitterMs, int maxFailurePercent,
                  int requestedProfile, int autoCoolCount) {
        this.timestampMs = timestampMs;
        this.durationSec = durationSec;
        this.maxTempC = maxTempC;
        this.minRamMb = minRamMb;
        this.avgRttMs = avgRttMs;
        this.avgJitterMs = avgJitterMs;
        this.maxFailurePercent = maxFailurePercent;
        this.requestedProfile = requestedProfile;
        this.autoCoolCount = autoCoolCount;
    }

    String encode() {
        return timestampMs + "|" + durationSec + "|" + maxTempC + "|" + minRamMb + "|"
                + avgRttMs + "|" + avgJitterMs + "|" + maxFailurePercent + "|"
                + requestedProfile + "|" + autoCoolCount;
    }

    static SessionRecord decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            String[] p = raw.split("\\|");
            if (p.length < 9) return null;
            return new SessionRecord(
                    Long.parseLong(p[0]), Long.parseLong(p[1]), Float.parseFloat(p[2]),
                    Long.parseLong(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                    Integer.parseInt(p[6]), Integer.parseInt(p[7]), Integer.parseInt(p[8]));
        } catch (Throwable e) {
            return null;
        }
    }

    String toDisplayLine(int index) {
        String date = timestampMs > 0
                ? new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestampMs))
                : "?";
        String duration = String.format(Locale.US, "%02d:%02d", durationSec / 60, durationSec % 60);
        String temp = maxTempC > 0 ? String.format(Locale.US, "%.1f°C", maxTempC) : "?";
        String ram = minRamMb >= 0 ? minRamMb + "M" : "?";
        String rtt = avgRttMs >= 0 ? avgRttMs + "ms" : "?";
        String jit = avgJitterMs >= 0 ? avgJitterMs + "ms" : "?";
        return index + ". " + date + " • " + MainActivity.profileName(requestedProfile) + " • " + duration
                + "\n   Nhiệt " + temp + " • RAM min " + ram + " • RTT* " + rtt
                + " • JIT* " + jit + " • FAIL* " + maxFailurePercent + "%"
                + (autoCoolCount > 0 ? " • AUTO-COOL x" + autoCoolCount : "");
    }
}
