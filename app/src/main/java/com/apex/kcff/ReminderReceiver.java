package com.apex.kcff;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(MainActivity.KEY_STATE, "");
        if (raw.isEmpty()) return;
        try {
            JSONObject state = new JSONObject(raw);
            if (!state.optBoolean("reminderEnabled", true)) return;
            JSONArray passes = state.optJSONArray("passes");
            if (passes == null) return;

            long now = System.currentTimeMillis();
            String today = dateKey(now);
            int count = 0;
            int amount = 0;

            for (int i = 0; i < passes.length(); i++) {
                JSONObject p = passes.optJSONObject(i);
                if (p == null) continue;
                boolean imported = p.optBoolean("imported", false);
                int days = Math.max(1, p.optInt("days"));
                long start = p.optLong("start", now);
                long eligible = p.has("eligibleFrom") ? p.optLong("eligibleFrom") : (imported ? nextDayStart(start) : dayStart(start));
                long expires = p.has("expiresAt") ? p.optLong("expiresAt") : eligible + (long) days * DAY_MS;
                JSONArray claimed = p.optJSONArray("claimed");
                int claimedCount = claimed == null ? 0 : claimed.length();

                if (now < eligible || now >= expires || claimedCount >= days || contains(claimed, today)) continue;
                count++;
                amount += p.optInt("daily", "weekly".equals(p.optString("type")) ? 50 : 70);
            }

            if (count == 0) return;

            Intent open = new Intent(context, KcffActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent content = PendingIntent.getActivity(
                    context,
                    9002,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(context, MainActivity.CHANNEL_ID)
                    : new android.app.Notification.Builder(context);

            builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("KCFF • " + count + " thẻ đang chờ")
                    .setContentText("Hôm nay còn " + amount + " KC có thể nhận.")
                    .setContentIntent(content)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(9003, builder.build());
        } catch (Exception ignored) {}
    }

    private boolean contains(JSONArray a, String value) {
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) if (value.equals(a.optString(i))) return true;
        return false;
    }

    private String dateKey(long time) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(time));
    }

    private long dayStart(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long nextDayStart(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart(time));
        c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }
}
