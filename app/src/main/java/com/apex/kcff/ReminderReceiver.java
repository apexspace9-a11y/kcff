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
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("dummy", false)) { /* keep prefs initialized */ }
        String raw = prefs.getString(MainActivity.KEY_STATE, "");
        if (raw.isEmpty()) return;
        try {
            JSONObject state = new JSONObject(raw);
            if (!state.optBoolean("reminderEnabled", true)) return;
            JSONArray passes = state.optJSONArray("passes");
            if (passes == null) return;
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            long now = System.currentTimeMillis();
            int count = 0, amount = 0;
            for (int i = 0; i < passes.length(); i++) {
                JSONObject p = passes.optJSONObject(i); if (p == null) continue;
                int days = p.optInt("days");
                JSONArray claimed = p.optJSONArray("claimed");
                int claimedCount = claimed == null ? 0 : claimed.length();
                if (now >= p.optLong("start") + days * DAY_MS || claimedCount >= days || contains(claimed, today)) continue;
                count++; amount += p.optInt("daily");
            }
            if (count == 0) return;

            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent content = PendingIntent.getActivity(context, 9002, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(context, MainActivity.CHANNEL_ID)
                    : new android.app.Notification.Builder(context);
            b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("KCFF • Còn " + count + " thẻ chưa nhận")
                    .setContentText("Bạn còn " + amount + " KC có thể nhận hôm nay.")
                    .setContentIntent(content)
                    .setAutoCancel(true);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(9003, b.build());
        } catch (Exception ignored) {}
    }

    private boolean contains(JSONArray a, String v) {
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) if (v.equals(a.optString(i))) return true;
        return false;
    }
}
