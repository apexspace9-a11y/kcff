package com.apex.kcff;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class KcffActivity extends Activity {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int REMINDER_REQUEST = 9001;
    private final int BG = Color.rgb(5, 11, 20);
    private final int SURFACE = Color.rgb(12, 23, 38);
    private final int SURFACE_2 = Color.rgb(18, 34, 55);
    private final int SURFACE_3 = Color.rgb(27, 47, 70);
    private final int TEXT = Color.rgb(242, 247, 255);
    private final int MUTED = Color.rgb(151, 168, 190);
    private final int ACCENT = Color.rgb(73, 226, 196);
    private final int BLUE = Color.rgb(94, 164, 255);
    private final int GOLD = Color.rgb(255, 192, 85);
    private final int PURPLE = Color.rgb(188, 133, 255);
    private final int RED = Color.rgb(255, 104, 123);
    private final int GREEN = Color.rgb(119, 229, 156);

    private SharedPreferences prefs;
    private JSONObject state;
    private FrameLayout contentHost;
    private int activeTab = 0;
    private final String[] tabNames = {"Tổng quan", "Thẻ", "Chi tiêu", "Tiết kiệm", "Thống kê"};
    private final Button[] tabButtons = new Button[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        loadState();
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        buildShell();
        renderTab();
        scheduleReminderIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
        renderTab();
    }

    private JSONObject freshState() {
        JSONObject s = new JSONObject();
        try {
            s.put("available", 0);
            s.put("passes", new JSONArray());
            s.put("campaigns", new JSONArray());
            s.put("expenses", new JSONArray());
            s.put("history", new JSONArray());
            s.put("budgets", new JSONObject());
            s.put("reminderEnabled", true);
            s.put("reminderHour", 19);
            s.put("reminderMinute", 0);
        } catch (Exception ignored) {}
        return s;
    }

    private void loadState() {
        String raw = prefs.getString(MainActivity.KEY_STATE, "");
        try { state = raw.isEmpty() ? freshState() : new JSONObject(raw); }
        catch (Exception e) { state = freshState(); }
        ensureState();
        saveState();
    }

    private void ensureState() {
        try {
            if (!state.has("available")) state.put("available", 0);
            if (!state.has("passes")) state.put("passes", new JSONArray());
            if (!state.has("campaigns")) state.put("campaigns", new JSONArray());
            if (!state.has("expenses")) state.put("expenses", new JSONArray());
            if (!state.has("history")) state.put("history", new JSONArray());
            if (!state.has("budgets")) state.put("budgets", new JSONObject());
            if (!state.has("reminderEnabled")) state.put("reminderEnabled", true);
            if (!state.has("reminderHour")) state.put("reminderHour", 19);
            if (!state.has("reminderMinute")) state.put("reminderMinute", 0);
            JSONArray passes = arr("passes");
            for (int i = 0; i < passes.length(); i++) {
                JSONObject p = passes.optJSONObject(i);
                if (p == null) continue;
                boolean weekly = "weekly".equals(p.optString("type"));
                boolean imported = p.optBoolean("imported", false);
                int days = Math.max(1, p.optInt("days", weekly ? 7 : 30));
                long start = p.optLong("start", System.currentTimeMillis());
                if (!p.has("imported")) p.put("imported", false);
                if (!p.has("originalDays")) p.put("originalDays", weekly ? 7 : 30);
                if (!p.has("daily")) p.put("daily", weekly ? 50 : 70);
                if (!p.has("claimed")) p.put("claimed", new JSONArray());
                if (!p.has("upfrontCredited")) p.put("upfrontCredited", imported ? 0 : (weekly ? 100 : 500));
                if (!p.has("enteredAt")) p.put("enteredAt", start);
                if (!p.has("eligibleFrom")) {
                    long eligible = imported ? nextDayStart(start) : dayStart(start);
                    p.put("eligibleFrom", eligible);
                }
                if (!p.has("expiresAt")) p.put("expiresAt", p.optLong("eligibleFrom") + (long) days * DAY_MS);
                if (!p.has("todayAlreadyReceived")) p.put("todayAlreadyReceived", imported);
            }
        } catch (Exception ignored) {}
    }

    private void saveState() { prefs.edit().putString(MainActivity.KEY_STATE, state.toString()).apply(); }
    private JSONArray arr(String key) { JSONArray a = state.optJSONArray(key); return a == null ? new JSONArray() : a; }
    private JSONObject budgets() { JSONObject o = state.optJSONObject("budgets"); return o == null ? new JSONObject() : o; }
    private int available() { return Math.max(0, state.optInt("available", 0)); }
    private void setAvailable(int value) { try { state.put("available", Math.max(0, value)); } catch (Exception ignored) {} }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(10));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.addView(text("KCFF", 27, TEXT, true));
        title.addView(text("DIAMOND CONTROL • OFFLINE", 10, ACCENT, true));
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(pill("v3.1", BLUE, Color.rgb(16, 30, 48)));
        shell.addView(header);

        contentHost = new FrameLayout(this);
        shell.addView(contentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(5), dp(6), dp(5), dp(8));
        tabs.setBackgroundColor(Color.rgb(8, 17, 29));
        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(tabNames[i]);
            b.setAllCaps(false);
            b.setTextSize(10);
            b.setMinWidth(0);
            b.setMinHeight(0);
            b.setPadding(dp(2), dp(8), dp(2), dp(8));
            b.setOnClickListener(v -> { activeTab = index; renderTab(); });
            tabButtons[i] = b;
            tabs.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        shell.addView(tabs);
        setContentView(shell);
    }

    private void renderTab() {
        if (contentHost == null) return;
        contentHost.removeAllViews();
        for (int i = 0; i < tabButtons.length; i++) {
            boolean active = i == activeTab;
            tabButtons[i].setTextColor(active ? Color.BLACK : MUTED);
            tabButtons[i].setBackground(round(active ? ACCENT : Color.TRANSPARENT, 16));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(8), dp(14), dp(28));
        scroll.addView(root);
        contentHost.addView(scroll);
        switch (activeTab) {
            case 0: buildDashboard(root); break;
            case 1: buildPasses(root); break;
            case 2: buildExpenses(root); break;
            case 3: buildCampaigns(root); break;
            default: buildStats(root); break;
        }
    }

    private void buildDashboard(LinearLayout root) {
        sectionHeader(root, "TỔNG QUAN", "Kho kim cương", "Số dư, lịch thẻ và việc cần làm hôm nay.");
        LinearLayout hero = card(SURFACE_2, BLUE);
        hero.addView(text("TỔNG TÀI SẢN", 11, ACCENT, true));
        hero.addView(text(fmt(totalOwned()) + " KC", 36, TEXT, true));
        hero.addView(text("Sẵn " + fmt(available()) + " • Tiết kiệm " + fmt(savedTotal()), 13, MUTED, false));
        addGap(hero, 12);
        LinearLayout q = row();
        Button add = actionButton("+ Thêm KC", ACCENT, Color.BLACK); add.setOnClickListener(v -> showAddKcDialog());
        Button claim = actionButton("Nhận tất cả", BLUE, Color.BLACK); claim.setOnClickListener(v -> claimAllToday());
        q.addView(add, weight()); q.addView(claim, weight()); hero.addView(q); root.addView(hero);

        LinearLayout metrics = row();
        metrics.addView(metricCard("Có thể nhận", fmt(pendingAll()), BLUE), weight());
        metrics.addView(metricCard("Đã nhận thẻ", fmt(sourceReceived("weekly") + sourceReceived("monthly")), ACCENT), weight());
        root.addView(metrics);

        LinearLayout today = card(SURFACE, duePassCount() > 0 ? GOLD : GREEN);
        today.addView(text("HÔM NAY", 11, duePassCount() > 0 ? GOLD : GREEN, true));
        if (duePassCount() > 0) {
            today.addView(text(duePassCount() + " thẻ đang chờ • " + fmt(duePassAmount()) + " KC", 19, TEXT, true));
            today.addView(text("Chỉ các thẻ đã tới ngày nhận mới xuất hiện ở đây.", 12, MUTED, false));
            addGap(today, 9);
            Button all = actionButton("Nhận " + fmt(duePassAmount()) + " KC", GOLD, Color.BLACK); all.setOnClickListener(v -> claimAllToday()); today.addView(all);
        } else {
            today.addView(text("Không có lượt thẻ nào cần nhận", 18, TEXT, true));
            int waiting = waitingUntilTomorrowCount();
            today.addView(text(waiting > 0 ? waiting + " thẻ nhập hôm nay sẽ bắt đầu nhận từ ngày mai." : "Các lượt hợp lệ hôm nay đã được xử lý.", 12, MUTED, false));
        }
        root.addView(today);

        LinearLayout next = card(SURFACE, PURPLE);
        next.addView(text("LỊCH THẺ SẮP TỚI", 11, PURPLE, true));
        int future = nextDayAmount();
        next.addView(text(future > 0 ? "Ngày mai dự kiến " + fmt(future) + " KC" : "Chưa có KC thẻ dự kiến cho ngày mai", 18, TEXT, true));
        next.addView(text("KC bỏ lỡ không được cộng bù. App tính theo từng ngày hợp lệ.", 12, MUTED, false));
        root.addView(next);

        LinearLayout sources = card(SURFACE, ACCENT);
        sources.addView(text("NGUỒN THẺ", 11, ACCENT, true));
        sources.addView(statLine("Thẻ tuần đã nhận", sourceReceived("weekly"), ACCENT));
        sources.addView(statLine("Thẻ tháng đã nhận", sourceReceived("monthly"), PURPLE));
        sources.addView(statLine("Còn có thể nhận", pendingAll(), GOLD));
        root.addView(sources);

        LinearLayout reminder = card(SURFACE, BLUE);
        reminder.addView(text("NHẮC NHẬN KC", 11, BLUE, true));
        boolean enabled = state.optBoolean("reminderEnabled", true);
        reminder.addView(text(enabled ? "Đang bật • " + reminderTimeText() : "Đang tắt", 18, TEXT, true));
        reminder.addView(text(enabled ? "Không nhắc thẻ nhập hôm nay trước ngày bắt đầu." : "Thông báo nhắc đang tắt.", 12, MUTED, false));
        addGap(reminder, 8);
        Button config = actionButton("Cài giờ nhắc", BLUE, Color.BLACK); config.setOnClickListener(v -> showReminderDialog()); reminder.addView(config); root.addView(reminder);

        sectionHeader(root, "GẦN ĐÂY", "Biến động mới nhất", null);
        renderRecentHistory(root, 6);
    }

    private void buildPasses(LinearLayout root) {
        sectionHeader(root, "THẺ THÀNH VIÊN", "Lịch nhận theo ngày", "Thẻ nhập thủ công mặc định hôm nay đã nhận bên Free Fire và bắt đầu từ ngày mai.");

        LinearLayout newPass = card(SURFACE_2, ACCENT);
        newPass.addView(text("THẺ MỚI", 11, ACCENT, true));
        newPass.addView(text("Vừa mua và kích hoạt trong app", 18, TEXT, true));
        newPass.addView(text("Tuần: +100 KC ngay, 50 KC/ngày × 7. Tháng: +500 KC ngay, 70 KC/ngày × 30.", 12, MUTED, false));
        addGap(newPass, 10);
        LinearLayout nr = row();
        Button nw = actionButton("+ Tuần mới", ACCENT, Color.BLACK); nw.setOnClickListener(v -> activateNewPass("weekly"));
        Button nm = actionButton("+ Tháng mới", PURPLE, Color.BLACK); nm.setOnClickListener(v -> activateNewPass("monthly"));
        nr.addView(nw, weight()); nr.addView(nm, weight()); newPass.addView(nr); root.addView(newPass);

        LinearLayout manual = card(Color.rgb(21, 31, 47), GOLD);
        manual.addView(text("THẺ ĐANG CHẠY • GHI THỦ CÔNG", 11, GOLD, true));
        manual.addView(text("Nhập số ngày còn lại tự do", 19, TEXT, true));
        manual.addView(text("Hôm nay được coi là đã nhận bên Free Fire. App không cộng 100/500 KC và lượt đầu tiên bắt đầu từ ngày mai.", 12, MUTED, false));
        manual.addView(text("Không khóa 1–7 hay 1–30: bạn tự ghi đúng số ngày thực tế cần theo dõi.", 11, GOLD, true));
        addGap(manual, 10);
        LinearLayout mr = row();
        Button mw = actionButton("Ghi thẻ tuần", GOLD, Color.BLACK); mw.setOnClickListener(v -> showManualPassDialog("weekly"));
        Button mm = actionButton("Ghi thẻ tháng", BLUE, Color.BLACK); mm.setOnClickListener(v -> showManualPassDialog("monthly"));
        mr.addView(mw, weight()); mr.addView(mm, weight()); manual.addView(mr); root.addView(manual);

        if (duePassCount() > 0) {
            Button all = actionButton("Nhận tất cả hôm nay • " + fmt(duePassAmount()) + " KC", ACCENT, Color.BLACK); all.setOnClickListener(v -> claimAllToday()); root.addView(all); addGap(root, 12);
        }

        JSONArray passes = arr("passes");
        if (passes.length() == 0) { root.addView(emptyCard("Chưa có thẻ nào.")); return; }
        sectionHeader(root, "DANH SÁCH", passes.length() + " thẻ đã ghi", null);
        for (int i = passes.length() - 1; i >= 0; i--) {
            JSONObject p = passes.optJSONObject(i);
            if (p != null) root.addView(passCard(p));
        }
    }

    private View passCard(JSONObject p) {
        boolean weekly = "weekly".equals(p.optString("type"));
        boolean imported = p.optBoolean("imported", false);
        int days = Math.max(1, p.optInt("days", weekly ? 7 : 30));
        int daily = p.optInt("daily", weekly ? 50 : 70);
        int claimed = claimedCount(p);
        int missed = missedCount(p);
        int potential = potentialClaimsRemaining(p);
        int received = claimed * daily + p.optInt("upfrontCredited", imported ? 0 : (weekly ? 100 : 500));
        long now = System.currentTimeMillis();
        long eligible = p.optLong("eligibleFrom", p.optLong("start"));
        long expires = p.optLong("expiresAt", eligible + (long) days * DAY_MS);
        boolean due = isDueToday(p, now);
        boolean waiting = now < eligible;
        boolean finished = now >= expires || potential == 0 && claimed >= days;

        LinearLayout c = card(SURFACE, weekly ? ACCENT : PURPLE);
        LinearLayout head = row();
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(weekly ? "THẺ TUẦN" : "THẺ THÁNG", 11, weekly ? ACCENT : PURPLE, true));
        left.addView(text(imported ? "Ghi thủ công" : "Kích hoạt trong app", 16, TEXT, true));
        head.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String status = waiting ? "Chờ ngày nhận" : (finished ? "Đã kết thúc" : (due ? "Đang chờ" : "Đã xử lý"));
        int statusColor = waiting ? GOLD : (finished ? MUTED : (due ? ACCENT : GREEN));
        head.addView(pill(status, statusColor, SURFACE_3)); c.addView(head); addGap(c, 10);

        if (imported && waiting) {
            c.addView(text("Hôm nay đã nhận bên FF", 13, GOLD, true));
            c.addView(text("Lượt đầu trong app: " + dateOnly(eligible), 16, TEXT, true));
        } else {
            c.addView(text(due ? "Có thể nhận " + daily + " KC hôm nay" : "Lượt tiếp theo: " + nextClaimText(p), 16, TEXT, true));
        }
        c.addView(text("Đã ghi nhận: " + fmt(received) + " KC • Còn có thể nhận: " + fmt(potential * daily) + " KC", 12, MUTED, false));
        c.addView(text("Đã nhận " + claimed + "/" + days + " lượt" + (missed > 0 ? " • Bỏ lỡ " + missed + " ngày" : ""), 12, missed > 0 ? RED : MUTED, false));
        c.addView(text("Kết thúc lịch: " + dateOnly(expires - 1), 11, MUTED, false));
        addGap(c, 7);
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(days); pb.setProgress(Math.min(days, claimed + missed));
        c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        addGap(c, 9);

        Button claim = actionButton(waiting ? "Bắt đầu nhận từ " + dateOnly(eligible) : (due ? "Nhận " + daily + " KC hôm nay" : (finished ? "Thẻ đã kết thúc" : "Hôm nay đã nhận")), due ? (weekly ? ACCENT : PURPLE) : SURFACE_3, due ? Color.BLACK : MUTED);
        claim.setEnabled(due); String id = p.optString("id"); claim.setOnClickListener(v -> claimPass(id)); c.addView(claim);

        LinearLayout tools = row();
        if (imported) {
            Button edit = smallAction("Sửa số ngày"); edit.setOnClickListener(v -> showAdjustPassDialog(id)); tools.addView(edit);
        }
        Button remove = smallAction("Xóa thẻ"); remove.setOnClickListener(v -> confirmDeletePass(id)); tools.addView(remove);
        c.addView(tools);
        return c;
    }

    private void buildExpenses(LinearLayout root) {
        sectionHeader(root, "CHI TIÊU", "Kiểm soát KC đã dùng", "Ngân sách theo nhóm và hoàn tác khoản ghi nhầm.");
        LinearLayout hero = card(SURFACE_2, RED);
        hero.addView(text("ĐÃ CHI THÁNG NÀY", 11, RED, true));
        hero.addView(text(fmt(monthSpend()) + " KC", 32, TEXT, true));
        hero.addView(text("Số dư sẵn hiện tại " + fmt(available()) + " KC", 12, MUTED, false)); addGap(hero, 10);
        LinearLayout actions = row();
        Button add = actionButton("+ Ghi chi", RED, Color.WHITE); add.setOnClickListener(v -> showExpenseDialog());
        Button budget = actionButton("Ngân sách", BLUE, Color.BLACK); budget.setOnClickListener(v -> showBudgetDialog());
        actions.addView(add, weight()); actions.addView(budget, weight()); hero.addView(actions); root.addView(hero);

        for (String cat : categories()) {
            int spent = monthSpendFor(cat), limit = budgets().optInt(cat, 0);
            if (spent == 0 && limit == 0) continue;
            LinearLayout c = card(SURFACE, limit > 0 && spent > limit ? RED : BLUE);
            c.addView(text(cat, 14, TEXT, true));
            c.addView(text(limit > 0 ? fmt(spent) + " / " + fmt(limit) + " KC" : fmt(spent) + " KC • chưa đặt giới hạn", 12, limit > 0 && spent > limit ? RED : MUTED, false));
            if (limit > 0) { ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(limit); pb.setProgress(Math.min(spent, limit)); c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7))); }
            root.addView(c);
        }

        sectionHeader(root, "LỊCH SỬ", "Khoản chi gần đây", null);
        JSONArray expenses = arr("expenses");
        if (expenses.length() == 0) { root.addView(emptyCard("Chưa có khoản chi nào.")); return; }
        int start = Math.max(0, expenses.length() - 20);
        for (int i = expenses.length() - 1; i >= start; i--) {
            JSONObject e = expenses.optJSONObject(i); if (e == null) continue;
            LinearLayout c = card(SURFACE, RED); LinearLayout h = row();
            LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
            l.addView(text(e.optString("name"), 15, TEXT, true));
            l.addView(text(e.optString("category") + " • " + dateTime(e.optLong("time")), 11, MUTED, false));
            h.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            h.addView(text("-" + fmt(e.optInt("amount")), 17, RED, true)); c.addView(h);
            Button undo = smallAction("Hoàn tác & hoàn KC"); String id = e.optString("id"); undo.setOnClickListener(v -> undoExpense(id)); c.addView(undo); root.addView(c);
        }
    }

    private void buildCampaigns(LinearLayout root) {
        sectionHeader(root, "TIẾT KIỆM", "Chiến dịch KC", "Khóa KC cho mục tiêu và tính số cần để dành mỗi ngày.");
        LinearLayout hero = card(SURFACE_2, GOLD);
        hero.addView(text("ĐANG KHÓA", 11, GOLD, true)); hero.addView(text(fmt(savedTotal()) + " KC", 32, TEXT, true));
        hero.addView(text(arr("campaigns").length() + " chiến dịch", 12, MUTED, false)); addGap(hero, 10);
        Button add = actionButton("+ Tạo chiến dịch", GOLD, Color.BLACK); add.setOnClickListener(v -> showCampaignDialog()); hero.addView(add); root.addView(hero);

        JSONArray campaigns = arr("campaigns");
        if (campaigns.length() == 0) { root.addView(emptyCard("Chưa có chiến dịch tiết kiệm.")); return; }
        for (int i = campaigns.length() - 1; i >= 0; i--) {
            JSONObject d = campaigns.optJSONObject(i); if (d == null) continue;
            int target = Math.max(1, d.optInt("target")); int saved = Math.max(0, d.optInt("saved"));
            long deadline = d.optLong("deadline", 0); int left = Math.max(0, target - saved);
            int daysLeft = deadline > 0 ? Math.max(0, (int) Math.ceil((deadline - System.currentTimeMillis()) / (double) DAY_MS)) : 0;
            int perDay = daysLeft > 0 ? (int) Math.ceil(left / (double) daysLeft) : left;
            int pct = Math.min(100, (int) Math.round(saved * 100.0 / target));
            LinearLayout c = card(SURFACE, GOLD);
            c.addView(text(d.optString("name"), 18, TEXT, true));
            c.addView(text(fmt(saved) + " / " + fmt(target) + " KC • " + pct + "%", 13, GOLD, true));
            if (deadline > 0) c.addView(text(daysLeft > 0 ? "Còn " + daysLeft + " ngày • nên giữ ~" + fmt(perDay) + " KC/ngày" : (left == 0 ? "Đã đạt mục tiêu" : "Đã tới deadline • thiếu " + fmt(left) + " KC"), 12, MUTED, false));
            addGap(c, 7); ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(target); pb.setProgress(Math.min(saved, target)); c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))); addGap(c, 8);
            LinearLayout acts = row(); String id = d.optString("id");
            Button put = smallAction("+ Gửi KC"); put.setOnClickListener(v -> showCampaignTransfer(id, true));
            Button take = smallAction("Rút KC"); take.setOnClickListener(v -> showCampaignTransfer(id, false));
            Button close = smallAction("Đóng"); close.setOnClickListener(v -> confirmCloseCampaign(id));
            acts.addView(put); acts.addView(take); acts.addView(close); c.addView(acts); root.addView(c);
        }
    }

    private void buildStats(LinearLayout root) {
        sectionHeader(root, "THỐNG KÊ", "Nguồn KC & hiệu suất thẻ", "Theo dõi riêng tuần, tháng, thẻ ghi thủ công, lượt bỏ lỡ và dự kiến còn lại.");
        LinearLayout all = card(SURFACE_2, PURPLE);
        all.addView(text("KC TỪ THẺ • TOÀN THỜI GIAN", 11, PURPLE, true));
        all.addView(statLine("Thẻ tuần", sourceReceived("weekly"), ACCENT));
        all.addView(statLine("Tuần ghi thủ công", sourceReceivedImported("weekly"), GOLD));
        all.addView(statLine("Thẻ tháng", sourceReceived("monthly"), PURPLE));
        all.addView(statLine("Tháng ghi thủ công", sourceReceivedImported("monthly"), BLUE));
        root.addView(all);

        LinearLayout schedule = card(SURFACE, GOLD);
        schedule.addView(text("LỊCH THẺ", 11, GOLD, true));
        schedule.addView(statLine("Lượt đã nhận", totalClaimedPassDays(), GREEN));
        schedule.addView(statLine("Ngày đã bỏ lỡ", totalMissedPassDays(), RED));
        schedule.addView(statLine("Lượt còn có thể nhận", pendingClaimCount(), GOLD));
        schedule.addView(statLine("KC còn có thể nhận", pendingAll(), BLUE));
        root.addView(schedule);

        LinearLayout month = card(SURFACE, BLUE);
        month.addView(text("THÁNG NÀY", 11, BLUE, true));
        month.addView(statLine("KC từ thẻ tuần", sourceReceivedThisMonth("weekly"), ACCENT));
        month.addView(statLine("KC từ thẻ tháng", sourceReceivedThisMonth("monthly"), PURPLE));
        month.addView(statLine("KC thêm thủ công", sourceReceivedThisMonth("manual"), BLUE));
        month.addView(statLine("KC đã chi", monthSpend(), RED)); root.addView(month);

        LinearLayout future = card(SURFACE, ACCENT);
        future.addView(text("DỰ KIẾN", 11, ACCENT, true));
        future.addView(statLine("KC hiện sở hữu", totalOwned(), TEXT));
        future.addView(statLine("Còn từ thẻ tuần", pendingFor("weekly"), ACCENT));
        future.addView(statLine("Còn từ thẻ tháng", pendingFor("monthly"), PURPLE));
        future.addView(statLine("Tài sản nếu nhận đủ", totalOwned() + pendingAll(), GOLD)); root.addView(future);

        LinearLayout chart = card(SURFACE, RED); chart.addView(text("CHI TIÊU 7 NGÀY GẦN NHẤT", 11, RED, true)); addGap(chart, 8); render7DayBars(chart); root.addView(chart);

        LinearLayout tools = card(SURFACE, MUTED); tools.addView(text("DỮ LIỆU & NHẮC NHỞ", 11, MUTED, true));
        Button reminder = actionButton("Cài nhắc nhận KC", BLUE, Color.BLACK); reminder.setOnClickListener(v -> showReminderDialog());
        Button backup = actionButton("Xem backup JSON", SURFACE_3, TEXT); backup.setOnClickListener(v -> showBackupDialog());
        Button restore = actionButton("Khôi phục JSON", SURFACE_3, TEXT); restore.setOnClickListener(v -> showRestoreDialog());
        Button reset = actionButton("Xóa toàn bộ dữ liệu", RED, Color.WHITE); reset.setOnClickListener(v -> confirmReset());
        tools.addView(reminder); addGap(tools, 6); tools.addView(backup); addGap(tools, 6); tools.addView(restore); addGap(tools, 6); tools.addView(reset); root.addView(tools);
    }

    private void activateNewPass(String type) {
        boolean weekly = "weekly".equals(type);
        int days = weekly ? 7 : 30; int daily = weekly ? 50 : 70; int upfront = weekly ? 100 : 500;
        long now = System.currentTimeMillis(); long eligible = dayStart(now);
        JSONObject p = new JSONObject();
        try {
            p.put("id", UUID.randomUUID().toString()); p.put("type", type); p.put("start", now); p.put("enteredAt", now);
            p.put("eligibleFrom", eligible); p.put("days", days); p.put("originalDays", days); p.put("daily", daily);
            p.put("expiresAt", eligible + (long) days * DAY_MS); p.put("claimed", new JSONArray()); p.put("imported", false);
            p.put("todayAlreadyReceived", false); p.put("upfrontCredited", upfront); arr("passes").put(p);
            setAvailable(available() + upfront);
            addHistory(weekly ? "Kích hoạt thẻ tuần mới" : "Kích hoạt thẻ tháng mới", upfront, type, false, "upfront");
            saveState(); renderTab(); scheduleReminderIfNeeded(); toast("Đã cộng " + upfront + " KC ban đầu");
        } catch (Exception e) { toast("Không thể thêm thẻ"); }
    }

    private void showManualPassDialog(String type) {
        boolean weekly = "weekly".equals(type);
        LinearLayout f = dialogForm();
        EditText days = input("Số ngày còn lại • nhập tự do", true);
        f.addView(days);
        f.addView(text("Ví dụ nhập 4: hôm nay được coi là đã nhận bên FF, ngày mai mới có nút nhận 50/70 KC. App theo dõi 4 ngày từ ngày mai.", 12, MUTED, false));
        f.addView(text("Không cộng thưởng ban đầu " + (weekly ? "100" : "500") + " KC.", 12, GOLD, true));
        new AlertDialog.Builder(this)
                .setTitle(weekly ? "Ghi thẻ tuần đang chạy" : "Ghi thẻ tháng đang chạy")
                .setView(f)
                .setPositiveButton("Ghi thẻ", (d, w) -> {
                    int remaining = parsePositive(days.getText().toString());
                    if (remaining < 1) { toast("Nhập số ngày lớn hơn 0"); return; }
                    addManualPass(type, remaining);
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void addManualPass(String type, int remainingDays) {
        boolean weekly = "weekly".equals(type); int daily = weekly ? 50 : 70;
        long now = System.currentTimeMillis(); long eligible = nextDayStart(now);
        JSONObject p = new JSONObject();
        try {
            p.put("id", UUID.randomUUID().toString()); p.put("type", type); p.put("start", now); p.put("enteredAt", now);
            p.put("eligibleFrom", eligible); p.put("expiresAt", eligible + (long) remainingDays * DAY_MS);
            p.put("days", remainingDays); p.put("originalDays", weekly ? 7 : 30); p.put("daily", daily);
            p.put("claimed", new JSONArray()); p.put("imported", true); p.put("todayAlreadyReceived", true); p.put("upfrontCredited", 0);
            arr("passes").put(p);
            addHistory((weekly ? "Ghi thẻ tuần còn " : "Ghi thẻ tháng còn ") + remainingDays + " ngày • bắt đầu ngày mai", 0, type, true, "pass_note");
            saveState(); renderTab(); scheduleReminderIfNeeded();
            toast("Đã ghi • lượt đầu từ " + dateOnly(eligible));
        } catch (Exception e) { toast("Không thể ghi thẻ"); }
    }

    private void showAdjustPassDialog(String id) {
        JSONObject p = findPass(id); if (p == null) return;
        int claimed = claimedCount(p);
        EditText days = input("Tổng số ngày muốn theo dõi", true); days.setText(String.valueOf(p.optInt("days")));
        LinearLayout f = dialogForm(); f.addView(days);
        f.addView(text("Đã nhận " + claimed + " lượt. Số mới không được nhỏ hơn số lượt đã nhận.", 12, MUTED, false));
        new AlertDialog.Builder(this).setTitle("Sửa số ngày thẻ").setView(f).setPositiveButton("Lưu", (d, w) -> {
            int n = parsePositive(days.getText().toString()); if (n < claimed || n < 1) { toast("Số ngày mới phải ≥ " + Math.max(1, claimed)); return; }
            try { p.put("days", n); p.put("expiresAt", p.optLong("eligibleFrom") + (long) n * DAY_MS); } catch (Exception ignored) {}
            addHistory("Sửa lịch thẻ thành " + n + " ngày", 0, p.optString("type"), p.optBoolean("imported"), "pass_note"); saveState(); renderTab();
        }).setNegativeButton("Hủy", null).show();
    }

    private void claimPass(String id) {
        JSONObject p = findPass(id); if (p == null) return;
        long now = System.currentTimeMillis();
        if (now < p.optLong("eligibleFrom")) { toast("Thẻ này bắt đầu nhận từ " + dateOnly(p.optLong("eligibleFrom"))); return; }
        if (!isDueToday(p, now)) { toast("Hôm nay không có lượt nhận cho thẻ này"); return; }
        JSONArray claimed = p.optJSONArray("claimed"); if (claimed == null) claimed = new JSONArray();
        claimed.put(dateKey(now)); try { p.put("claimed", claimed); } catch (Exception ignored) {}
        int daily = p.optInt("daily"); setAvailable(available() + daily);
        addHistory("Nhận " + ("weekly".equals(p.optString("type")) ? "thẻ tuần" : "thẻ tháng") + (p.optBoolean("imported") ? " • ghi thủ công" : ""), daily, p.optString("type"), p.optBoolean("imported"), "daily");
        saveState(); renderTab(); toast("+" + daily + " KC");
    }

    private void claimAllToday() {
        long now = System.currentTimeMillis(); JSONArray passes = arr("passes"); int total = 0, count = 0;
        for (int i = 0; i < passes.length(); i++) {
            JSONObject p = passes.optJSONObject(i); if (p == null || !isDueToday(p, now)) continue;
            JSONArray claimed = p.optJSONArray("claimed"); if (claimed == null) claimed = new JSONArray();
            claimed.put(dateKey(now)); try { p.put("claimed", claimed); } catch (Exception ignored) {}
            int daily = p.optInt("daily"); total += daily; count++;
            addHistory("Nhận " + ("weekly".equals(p.optString("type")) ? "thẻ tuần" : "thẻ tháng") + (p.optBoolean("imported") ? " • ghi thủ công" : ""), daily, p.optString("type"), p.optBoolean("imported"), "daily");
        }
        if (count == 0) { toast("Hôm nay chưa có KC thẻ nào cần nhận"); return; }
        setAvailable(available() + total); saveState(); renderTab(); toast("Đã nhận " + fmt(total) + " KC từ " + count + " thẻ");
    }

    private boolean isDueToday(JSONObject p, long now) {
        long eligible = p.optLong("eligibleFrom", p.optLong("start")); long expires = p.optLong("expiresAt", eligible + (long) p.optInt("days") * DAY_MS);
        if (now < eligible || now >= expires) return false;
        JSONArray claimed = p.optJSONArray("claimed");
        return claimedCount(p) < p.optInt("days") && !contains(claimed, dateKey(now));
    }

    private int claimedCount(JSONObject p) { JSONArray a = p.optJSONArray("claimed"); return a == null ? 0 : a.length(); }

    private int potentialClaimsRemaining(JSONObject p) {
        long now = System.currentTimeMillis(); long today = dayStart(now);
        long eligible = dayStart(p.optLong("eligibleFrom", p.optLong("start")));
        long expires = dayStart(p.optLong("expiresAt", eligible + (long) p.optInt("days") * DAY_MS));
        if (now >= expires) return 0;
        long first = Math.max(today, eligible);
        int slots = (int) Math.max(0, (expires - first) / DAY_MS);
        if (first == today && contains(p.optJSONArray("claimed"), dateKey(now))) slots--;
        int byCount = Math.max(0, p.optInt("days") - claimedCount(p));
        return Math.max(0, Math.min(byCount, slots));
    }

    private int missedCount(JSONObject p) {
        long today = dayStart(System.currentTimeMillis());
        long eligible = dayStart(p.optLong("eligibleFrom", p.optLong("start")));
        long expires = dayStart(p.optLong("expiresAt", eligible + (long) p.optInt("days") * DAY_MS));
        long pastEnd = Math.min(today, expires);
        int pastSlots = (int) Math.max(0, (pastEnd - eligible) / DAY_MS);
        int pastClaims = 0; JSONArray claimed = p.optJSONArray("claimed");
        if (claimed != null) for (int i = 0; i < claimed.length(); i++) {
            String key = claimed.optString(i); long t = parseDateKey(key); if (t >= eligible && t < pastEnd) pastClaims++;
        }
        return Math.max(0, pastSlots - pastClaims);
    }

    private String nextClaimText(JSONObject p) {
        long now = System.currentTimeMillis(); long eligible = p.optLong("eligibleFrom", p.optLong("start")); long expires = p.optLong("expiresAt");
        if (now < eligible) return dateOnly(eligible);
        if (now >= expires || potentialClaimsRemaining(p) <= 0) return "không còn";
        if (isDueToday(p, now)) return "hôm nay";
        long next = nextDayStart(now); return next < expires ? dateOnly(next) : "không còn";
    }

    private JSONObject findPass(String id) { JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && id.equals(p.optString("id"))) return p; } return null; }

    private void confirmDeletePass(String id) {
        JSONObject p = findPass(id); if (p == null) return;
        new AlertDialog.Builder(this).setTitle("Xóa thẻ khỏi app?").setMessage("KC đã nhận trước đó không bị trừ. Chỉ xóa lịch theo dõi thẻ.").setPositiveButton("Xóa", (d, w) -> deletePass(id)).setNegativeButton("Hủy", null).show();
    }

    private void deletePass(String id) {
        JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && id.equals(p.optString("id"))) { addHistory("Xóa lịch " + ("weekly".equals(p.optString("type")) ? "thẻ tuần" : "thẻ tháng"), 0, p.optString("type"), p.optBoolean("imported"), "pass_note"); a.remove(i); saveState(); renderTab(); scheduleReminderIfNeeded(); return; } }
    }

    private int duePassCount() { int n = 0; JSONArray a = arr("passes"); long now = System.currentTimeMillis(); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && isDueToday(p, now)) n++; } return n; }
    private int duePassAmount() { int total = 0; JSONArray a = arr("passes"); long now = System.currentTimeMillis(); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && isDueToday(p, now)) total += p.optInt("daily"); } return total; }
    private int waitingUntilTomorrowCount() { int n = 0; long now = System.currentTimeMillis(), tomorrow = nextDayStart(now); JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && p.optLong("eligibleFrom") >= tomorrow && p.optLong("eligibleFrom") < tomorrow + DAY_MS) n++; } return n; }
    private int nextDayAmount() { int total = 0; long tomorrow = nextDayStart(System.currentTimeMillis()); String key = dateKey(tomorrow); JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p == null) continue; long eligible = p.optLong("eligibleFrom"), expires = p.optLong("expiresAt"); if (tomorrow >= dayStart(eligible) && tomorrow < dayStart(expires) && !contains(p.optJSONArray("claimed"), key)) total += p.optInt("daily"); } return total; }
    private int pendingFor(String type) { int total = 0; JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && type.equals(p.optString("type"))) total += potentialClaimsRemaining(p) * p.optInt("daily"); } return total; }
    private int pendingAll() { return pendingFor("weekly") + pendingFor("monthly"); }
    private int pendingClaimCount() { int n = 0; JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null) n += potentialClaimsRemaining(p); } return n; }
    private int totalClaimedPassDays() { int n = 0; JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null) n += claimedCount(p); } return n; }
    private int totalMissedPassDays() { int n = 0; JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null) n += missedCount(p); } return n; }

    private int sourceReceived(String source) { return historySum(source, false, 0, false); }
    private int sourceReceivedImported(String source) { return historySum(source, true, 0, false); }
    private int sourceReceivedThisMonth(String source) { return historySum(source, false, monthStart(), true); }

    private int historySum(String source, boolean importedOnly, long since, boolean useSince) {
        int total = 0; JSONArray h = arr("history");
        for (int i = 0; i < h.length(); i++) {
            JSONObject x = h.optJSONObject(i); if (x == null) continue;
            String recorded = x.optString("source", "");
            if (recorded.isEmpty()) {
                String label = x.optString("label", "").toLowerCase(new Locale("vi", "VN"));
                if (label.contains("thẻ tuần")) recorded = "weekly"; else if (label.contains("thẻ tháng")) recorded = "monthly"; else if (x.optInt("delta") > 0) recorded = "manual";
            }
            if (source != null && !source.equals(recorded)) continue;
            if (importedOnly && !(x.optBoolean("imported", false) || x.optString("label", "").toLowerCase(new Locale("vi", "VN")).contains("thủ công"))) continue;
            if (useSince && x.optLong("time") < since) continue;
            int delta = x.optInt("delta"); if (delta > 0) total += delta;
        }
        return total;
    }

    private void showAddKcDialog() {
        LinearLayout f = dialogForm(); EditText amount = input("Số KC", true); EditText note = input("Ghi chú", false); f.addView(amount); f.addView(note);
        new AlertDialog.Builder(this).setTitle("Thêm KC sẵn").setView(f).setPositiveButton("Thêm", (d, w) -> {
            int n = parsePositive(amount.getText().toString()); if (n <= 0) { toast("Số KC không hợp lệ"); return; }
            setAvailable(available() + n); String label = note.getText().toString().trim(); addHistory(label.isEmpty() ? "Thêm KC sẵn" : label, n, "manual", false, "manual"); saveState(); renderTab();
        }).setNegativeButton("Hủy", null).show();
    }

    private void showExpenseDialog() {
        LinearLayout f = dialogForm(); EditText name = input("Nội dung", false); EditText amount = input("Số KC", true); Spinner category = new Spinner(this); category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories())); f.addView(name); f.addView(amount); f.addView(category);
        new AlertDialog.Builder(this).setTitle("Ghi chi tiêu").setView(f).setPositiveButton("Lưu", (d, w) -> {
            String title = name.getText().toString().trim(); int n = parsePositive(amount.getText().toString()); if (title.isEmpty() || n <= 0) { toast("Nhập đủ nội dung và số KC"); return; } if (n > available()) { toast("KC sẵn không đủ"); return; }
            JSONObject e = new JSONObject(); try { e.put("id", UUID.randomUUID().toString()); e.put("name", title); e.put("amount", n); e.put("category", String.valueOf(category.getSelectedItem())); e.put("time", System.currentTimeMillis()); arr("expenses").put(e); } catch (Exception ignored) {}
            setAvailable(available() - n); addHistory("Chi: " + title, -n, "expense", false, "expense"); saveState(); renderTab();
        }).setNegativeButton("Hủy", null).show();
    }

    private void undoExpense(String id) {
        JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && id.equals(e.optString("id"))) { int n = e.optInt("amount"); setAvailable(available() + n); addHistory("Hoàn tác chi: " + e.optString("name"), n, "refund", false, "refund"); a.remove(i); saveState(); renderTab(); toast("Đã hoàn " + n + " KC"); return; } }
    }

    private void showBudgetDialog() {
        LinearLayout f = dialogForm(); Spinner category = new Spinner(this); category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories())); EditText amount = input("Giới hạn KC/tháng • 0 để xóa", true); f.addView(category); f.addView(amount);
        new AlertDialog.Builder(this).setTitle("Ngân sách danh mục").setView(f).setPositiveButton("Lưu", (d, w) -> { String cat = String.valueOf(category.getSelectedItem()); int n; try { n = Integer.parseInt(amount.getText().toString().trim()); } catch (Exception e) { n = -1; } if (n < 0) { toast("Số không hợp lệ"); return; } try { budgets().put(cat, n); state.put("budgets", budgets()); } catch (Exception ignored) {} saveState(); renderTab(); }).setNegativeButton("Hủy", null).show();
    }

    private void showCampaignDialog() {
        LinearLayout f = dialogForm(); EditText name = input("Tên chiến dịch", false); EditText target = input("Mục tiêu KC", true); EditText days = input("Số ngày tới deadline", true); f.addView(name); f.addView(target); f.addView(days);
        new AlertDialog.Builder(this).setTitle("Tạo chiến dịch").setView(f).setPositiveButton("Tạo", (d, w) -> {
            String title = name.getText().toString().trim(); int t = parsePositive(target.getText().toString()); int dayCount = parsePositive(days.getText().toString()); if (title.isEmpty() || t <= 0 || dayCount <= 0) { toast("Thông tin chưa hợp lệ"); return; }
            JSONObject c = new JSONObject(); try { c.put("id", UUID.randomUUID().toString()); c.put("name", title); c.put("target", t); c.put("saved", 0); c.put("time", System.currentTimeMillis()); c.put("deadline", nextDayStart(System.currentTimeMillis()) + (long) dayCount * DAY_MS); arr("campaigns").put(c); } catch (Exception ignored) {}
            addHistory("Tạo chiến dịch: " + title, 0, "campaign", false, "campaign"); saveState(); renderTab();
        }).setNegativeButton("Hủy", null).show();
    }

    private JSONObject findCampaign(String id) { JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c != null && id.equals(c.optString("id"))) return c; } return null; }

    private void showCampaignTransfer(String id, boolean into) {
        JSONObject c = findCampaign(id); if (c == null) return; EditText amount = input(into ? "KC gửi vào quỹ" : "KC rút khỏi quỹ", true); LinearLayout f = dialogForm(); f.addView(amount);
        new AlertDialog.Builder(this).setTitle(c.optString("name")).setView(f).setPositiveButton(into ? "Gửi" : "Rút", (d, w) -> {
            int n = parsePositive(amount.getText().toString()); if (n <= 0) { toast("Số KC không hợp lệ"); return; }
            int saved = c.optInt("saved"); if (into && n > available()) { toast("KC sẵn không đủ"); return; } if (!into && n > saved) { toast("Quỹ không đủ KC"); return; }
            try { c.put("saved", into ? saved + n : saved - n); } catch (Exception ignored) {} setAvailable(available() + (into ? -n : n)); addHistory((into ? "Tiết kiệm: " : "Rút quỹ: ") + c.optString("name"), 0, "campaign", false, "campaign"); saveState(); renderTab();
        }).setNegativeButton("Hủy", null).show();
    }

    private void confirmCloseCampaign(String id) { JSONObject c = findCampaign(id); if (c == null) return; new AlertDialog.Builder(this).setTitle("Đóng chiến dịch?").setMessage("Hoàn " + fmt(c.optInt("saved")) + " KC về số KC sẵn.").setPositiveButton("Đóng & hoàn", (d, w) -> closeCampaign(id)).setNegativeButton("Hủy", null).show(); }
    private void closeCampaign(String id) { JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c != null && id.equals(c.optString("id"))) { int saved = c.optInt("saved"); setAvailable(available() + saved); addHistory("Đóng chiến dịch: " + c.optString("name"), 0, "campaign", false, "campaign"); a.remove(i); saveState(); renderTab(); toast("Đã hoàn " + saved + " KC"); return; } } }

    private int monthSpend() { int total = 0; long start = monthStart(); JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= start) total += e.optInt("amount"); } return total; }
    private int monthSpendFor(String category) { int total = 0; long start = monthStart(); JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= start && category.equals(e.optString("category"))) total += e.optInt("amount"); } return total; }
    private int savedTotal() { int total = 0; JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c != null) total += Math.max(0, c.optInt("saved")); } return total; }
    private int totalOwned() { return available() + savedTotal(); }
    private String[] categories() { return new String[]{"Vòng quay", "Trang phục", "Vũ khí", "Pet", "Sự kiện", "Khác"}; }

    private void showReminderDialog() {
        LinearLayout f = dialogForm(); Switch enabled = new Switch(this); enabled.setText("Bật nhắc nhận KC"); enabled.setChecked(state.optBoolean("reminderEnabled", true)); TimePicker picker = new TimePicker(this); picker.setIs24HourView(true); picker.setHour(state.optInt("reminderHour", 19)); picker.setMinute(state.optInt("reminderMinute", 0)); f.addView(enabled); f.addView(picker);
        new AlertDialog.Builder(this).setTitle("Nhắc nhận KC hằng ngày").setMessage("Thông báo chỉ hiện nếu có thẻ đã tới ngày nhận và chưa nhận hôm đó.").setView(f).setPositiveButton("Lưu", (d, w) -> { try { state.put("reminderEnabled", enabled.isChecked()); state.put("reminderHour", picker.getHour()); state.put("reminderMinute", picker.getMinute()); } catch (Exception ignored) {} saveState(); scheduleReminderIfNeeded(); renderTab(); }).setNegativeButton("Hủy", null).show();
    }

    private void createNotificationChannel() { if (Build.VERSION.SDK_INT >= 26) { NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); if (nm != null) nm.createNotificationChannel(new NotificationChannel(MainActivity.CHANNEL_ID, "Nhắc nhận KC", NotificationManager.IMPORTANCE_DEFAULT)); } }
    private void requestNotificationPermissionIfNeeded() { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77); }

    private void scheduleReminderIfNeeded() {
        Intent intent = new Intent(this, ReminderReceiver.class); PendingIntent pi = PendingIntent.getBroadcast(this, REMINDER_REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)); AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE); if (am == null) return;
        am.cancel(pi); if (!state.optBoolean("reminderEnabled", true)) return;
        Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY, state.optInt("reminderHour", 19)); c.set(Calendar.MINUTE, state.optInt("reminderMinute", 0)); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1); am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private String reminderTimeText() { return String.format(Locale.US, "%02d:%02d", state.optInt("reminderHour", 19), state.optInt("reminderMinute", 0)); }

    private void showBackupDialog() { EditText e = new EditText(this); e.setText(state.toString()); e.setSelectAllOnFocus(true); e.setMinLines(8); new AlertDialog.Builder(this).setTitle("Backup JSON").setMessage("Sao chép đoạn này để khôi phục dữ liệu sau này.").setView(e).setPositiveButton("Đóng", null).show(); }
    private void showRestoreDialog() { EditText e = new EditText(this); e.setHint("Dán JSON backup"); e.setMinLines(8); new AlertDialog.Builder(this).setTitle("Khôi phục dữ liệu").setView(e).setPositiveButton("Khôi phục", (d, w) -> { try { state = new JSONObject(e.getText().toString().trim()); ensureState(); saveState(); scheduleReminderIfNeeded(); renderTab(); toast("Đã khôi phục dữ liệu"); } catch (Exception ex) { toast("JSON không hợp lệ"); } }).setNegativeButton("Hủy", null).show(); }
    private void confirmReset() { new AlertDialog.Builder(this).setTitle("Xóa toàn bộ dữ liệu?").setMessage("KC, thẻ, chi tiêu, chiến dịch, ngân sách và lịch sử sẽ bị xóa trên máy.").setPositiveButton("Xóa", (d, w) -> { state = freshState(); saveState(); scheduleReminderIfNeeded(); renderTab(); toast("Đã đặt lại dữ liệu"); }).setNegativeButton("Hủy", null).show(); }

    private void addHistory(String label, int delta, String source, boolean imported, String kind) { JSONObject h = new JSONObject(); try { h.put("label", label); h.put("delta", delta); h.put("source", source); h.put("imported", imported); h.put("kind", kind); h.put("time", System.currentTimeMillis()); arr("history").put(h); } catch (Exception ignored) {} }

    private void renderRecentHistory(LinearLayout root, int limit) {
        JSONArray a = arr("history"); if (a.length() == 0) { root.addView(emptyCard("Chưa có biến động.")); return; }
        int start = Math.max(0, a.length() - limit);
        for (int i = a.length() - 1; i >= start; i--) { JSONObject h = a.optJSONObject(i); if (h == null) continue; LinearLayout c = card(SURFACE, MUTED); LinearLayout r = row(); LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.addView(text(h.optString("label"), 14, TEXT, true)); l.addView(text(dateTime(h.optLong("time")), 11, MUTED, false)); r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); int d = h.optInt("delta"); if (d != 0) r.addView(text((d > 0 ? "+" : "") + fmt(d), 16, d > 0 ? ACCENT : RED, true)); c.addView(r); root.addView(c); }
    }

    private void render7DayBars(LinearLayout parent) {
        int[] values = new int[7]; String[] labels = new String[7]; int max = 1;
        for (int x = 6; x >= 0; x--) { Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -x); long start = dayStart(c.getTimeInMillis()), end = start + DAY_MS; int sum = 0; JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= start && e.optLong("time") < end) sum += e.optInt("amount"); } int idx = 6 - x; values[idx] = sum; labels[idx] = new SimpleDateFormat("dd/MM", Locale.US).format(new Date(start)); max = Math.max(max, sum); }
        for (int i = 0; i < 7; i++) { LinearLayout r = row(); r.addView(text(labels[i], 11, MUTED, false), new LinearLayout.LayoutParams(dp(46), ViewGroup.LayoutParams.WRAP_CONTENT)); ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(max); pb.setProgress(values[i]); r.addView(pb, new LinearLayout.LayoutParams(0, dp(12), 1)); r.addView(text("  " + fmt(values[i]), 11, values[i] > 0 ? RED : MUTED, true)); parent.addView(r); addGap(parent, 5); }
    }

    private long monthStart() { Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }
    private long dayStart(long time) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(time); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }
    private long nextDayStart(long time) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(dayStart(time)); c.add(Calendar.DAY_OF_YEAR, 1); return c.getTimeInMillis(); }
    private long parseDateKey(String key) { try { return dayStart(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key).getTime()); } catch (Exception e) { return -1; } }
    private String dateKey(long time) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(time)); }
    private String dateOnly(long time) { return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(time)); }
    private String dateTime(long time) { return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date(time)); }
    private String fmt(int value) { return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(value); }
    private boolean contains(JSONArray a, String value) { if (a == null) return false; for (int i = 0; i < a.length(); i++) if (value.equals(a.optString(i))) return true; return false; }
    private int parsePositive(String raw) { try { long n = Long.parseLong(raw.trim()); return n > 0 && n <= Integer.MAX_VALUE ? (int) n : -1; } catch (Exception e) { return -1; } }

    private LinearLayout dialogForm() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(22), dp(8), dp(22), 0); return l; }
    private EditText input(String hint, boolean numeric) { EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER); e.setPadding(dp(4), dp(12), dp(4), dp(12)); return e; }

    private LinearLayout card(int color, int accent) {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent))); l.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, 0, 0, dp(11)); l.setLayoutParams(p); return l;
    }
    private View emptyCard(String message) { LinearLayout l = card(SURFACE, MUTED); l.addView(text(message, 13, MUTED, false)); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams weight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); p.setMargins(dp(3), 0, dp(3), 0); return p; }
    private void addGap(LinearLayout parent, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); parent.addView(v); }
    private void sectionHeader(LinearLayout root, String eyebrow, String title, String sub) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(2), dp(8), dp(2), dp(8)); l.addView(text(eyebrow, 10, ACCENT, true)); l.addView(text(title, 22, TEXT, true)); if (sub != null) l.addView(text(sub, 12, MUTED, false)); root.addView(l); }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL); t.setLineSpacing(0, 1.08f); return t; }
    private TextView pill(String value, int textColor, int bgColor) { TextView t = text(value, 11, textColor, true); t.setGravity(Gravity.CENTER); t.setPadding(dp(10), dp(6), dp(10), dp(6)); t.setBackground(round(bgColor, 20)); return t; }
    private GradientDrawable round(int color, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private Button actionButton(String label, int bg, int fg) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(fg); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setMinHeight(0); b.setPadding(dp(10), dp(11), dp(10), dp(11)); b.setBackground(round(bg, 14)); return b; }
    private Button smallAction(String label) { Button b = actionButton(label, SURFACE_3, TEXT); b.setTextSize(11); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(dp(3), dp(5), dp(3), 0); b.setLayoutParams(p); return b; }
    private View metricCard(String label, String value, int color) { LinearLayout c = card(SURFACE, color); c.addView(text(label, 11, MUTED, false)); c.addView(text(value + " KC", 21, color, true)); return c; }
    private View statLine(String label, int value, int color) { LinearLayout r = row(); r.setPadding(0, dp(5), 0, dp(5)); r.addView(text(label, 13, MUTED, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); r.addView(text(fmt(value) + " KC", 14, color, true)); return r; }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
