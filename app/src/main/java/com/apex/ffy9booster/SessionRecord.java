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
    final int avgCpuPercent;
    final int maxCpuPercent;
    final int maxRamPressurePercent;
    final int startBatteryPercent;
    final int endBatteryPercent;
    final int unstableSamples;
    final int totalNetworkSamples;

    SessionRecord(long timestampMs, long durationSec, float maxTempC, long minRamMb,
                  int avgRttMs, int avgJitterMs, int maxFailurePercent,
                  int requestedProfile, int autoCoolCount,
                  int avgCpuPercent, int maxCpuPercent, int maxRamPressurePercent,
                  int startBatteryPercent, int endBatteryPercent,
                  int unstableSamples, int totalNetworkSamples) {
        this.timestampMs = timestampMs;
        this.durationSec = durationSec;
        this.maxTempC = maxTempC;
        this.minRamMb = minRamMb;
        this.avgRttMs = avgRttMs;
        this.avgJitterMs = avgJitterMs;
        this.maxFailurePercent = maxFailurePercent;
        this.requestedProfile = requestedProfile;
        this.autoCoolCount = autoCoolCount;
        this.avgCpuPercent = avgCpuPercent;
        this.maxCpuPercent = maxCpuPercent;
        this.maxRamPressurePercent = maxRamPressurePercent;
        this.startBatteryPercent = startBatteryPercent;
        this.endBatteryPercent = endBatteryPercent;
        this.unstableSamples = unstableSamples;
        this.totalNetworkSamples = totalNetworkSamples;
    }

    String encode() {
        return "4|" + timestampMs + "|" + durationSec + "|" + maxTempC + "|" + minRamMb + "|"
                + avgRttMs + "|" + avgJitterMs + "|" + maxFailurePercent + "|"
                + requestedProfile + "|" + autoCoolCount + "|" + avgCpuPercent + "|"
                + maxCpuPercent + "|" + maxRamPressurePercent + "|" + startBatteryPercent + "|"
                + endBatteryPercent + "|" + unstableSamples + "|" + totalNetworkSamples;
    }

    static SessionRecord decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            String[] p = raw.split("\\|");
            if (p.length >= 17 && "4".equals(p[0])) {
                return new SessionRecord(
                        Long.parseLong(p[1]), Long.parseLong(p[2]), Float.parseFloat(p[3]),
                        Long.parseLong(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                        Integer.parseInt(p[7]), Integer.parseInt(p[8]), Integer.parseInt(p[9]),
                        Integer.parseInt(p[10]), Integer.parseInt(p[11]), Integer.parseInt(p[12]),
                        Integer.parseInt(p[13]), Integer.parseInt(p[14]), Integer.parseInt(p[15]),
                        Integer.parseInt(p[16]));
            }
            if (p.length >= 9) {
                return new SessionRecord(
                        Long.parseLong(p[0]), Long.parseLong(p[1]), Float.parseFloat(p[2]),
                        Long.parseLong(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                        Integer.parseInt(p[6]), Integer.parseInt(p[7]), Integer.parseInt(p[8]),
                        -1, -1, -1, -1, -1, 0, 0);
            }
        } catch (Throwable ignored) { }
        return null;
    }

    int score() {
        int s = 100;
        if (maxTempC >= 44f) s -= 32;
        else if (maxTempC >= 42.5f) s -= 20;
        else if (maxTempC >= 40.5f) s -= 9;

        if (minRamMb >= 0 && minRamMb < 420) s -= 22;
        else if (minRamMb >= 0 && minRamMb < 700) s -= 12;
        else if (minRamMb >= 0 && minRamMb < 1000) s -= 5;

        if (avgRttMs >= 180) s -= 20;
        else if (avgRttMs >= 110) s -= 12;
        else if (avgRttMs >= 70) s -= 5;

        if (avgJitterMs >= 55) s -= 20;
        else if (avgJitterMs >= 30) s -= 12;
        else if (avgJitterMs >= 18) s -= 5;

        if (maxFailurePercent >= 75) s -= 24;
        else if (maxFailurePercent >= 50) s -= 16;
        else if (maxFailurePercent >= 25) s -= 8;

        if (maxRamPressurePercent >= 94) s -= 10;
        else if (maxRamPressurePercent >= 88) s -= 5;

        if (autoCoolCount > 0) s -= Math.min(12, autoCoolCount * 3);
        if (totalNetworkSamples > 0) {
            int unstablePercent = Math.round(unstableSamples * 100f / totalNetworkSamples);
            if (unstablePercent >= 60) s -= 12;
            else if (unstablePercent >= 30) s -= 6;
        }
        return Math.max(0, Math.min(100, s));
    }

    String grade() {
        int s = score();
        if (s >= 94) return "A+";
        if (s >= 86) return "A";
        if (s >= 74) return "B";
        if (s >= 60) return "C";
        return "D";
    }

    String dominantIssue() {
        int thermalRisk = maxTempC >= 44f ? 3 : maxTempC >= 42f ? 2 : maxTempC >= 40f ? 1 : 0;
        int ramRisk = minRamMb >= 0 && minRamMb < 450 ? 3 : minRamMb >= 0 && minRamMb < 750 ? 2 : 0;
        int netRisk = 0;
        if (avgRttMs >= 150 || avgJitterMs >= 45 || maxFailurePercent >= 50) netRisk = 3;
        else if (avgRttMs >= 95 || avgJitterMs >= 25 || maxFailurePercent >= 25) netRisk = 2;
        else if (avgRttMs >= 65 || avgJitterMs >= 15) netRisk = 1;

        if (thermalRisk == 0 && ramRisk == 0 && netRisk == 0) return "ỔN ĐỊNH";
        if (netRisk >= thermalRisk && netRisk >= ramRisk) return "MẠNG";
        if (thermalRisk >= ramRisk) return "NHIỆT";
        return "RAM";
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
        String unstable = totalNetworkSamples > 0
                ? Math.round(unstableSamples * 100f / totalNetworkSamples) + "%"
                : "?";
        return index + ". [" + grade() + "] " + date + " • " + MainActivity.profileName(requestedProfile)
                + " • " + duration + " • " + dominantIssue()
                + "\n   Nhiệt " + temp + " • RAM min " + ram + " • RTT* " + rtt
                + " • JIT* " + jit + " • FAIL* " + maxFailurePercent + "%"
                + " • NET BAD " + unstable
                + (autoCoolCount > 0 ? " • AUTO-COOL x" + autoCoolCount : "");
    }

    String toShareLine() {
        return "[" + grade() + "] " + dominantIssue() + " | "
                + "time=" + durationSec + "s"
                + ", maxTemp=" + (maxTempC > 0 ? String.format(Locale.US, "%.1fC", maxTempC) : "?")
                + ", minRAM=" + (minRamMb >= 0 ? minRamMb + "MB" : "?")
                + ", RTT*=" + (avgRttMs >= 0 ? avgRttMs + "ms" : "?")
                + ", JIT*=" + (avgJitterMs >= 0 ? avgJitterMs + "ms" : "?")
                + ", FAIL*=" + maxFailurePercent + "%"
                + ", autoCool=" + autoCoolCount;
    }
}
