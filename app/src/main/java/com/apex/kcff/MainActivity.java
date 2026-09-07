package com.apex.kcff;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlarmManager;
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

public class MainActivity extends Activity {
    public static final String PREFS = "kcff_store";
    public static final String KEY_STATE = "state";
    public static final String CHANNEL_ID = "kcff_claim_reminders";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final int BG = Color.rgb(6, 12, 22);
    private final int SURFACE = Color.rgb(12, 23, 38);
    private final int SURFACE_2 = Color.rgb(18, 34, 55);
    private final int SURFACE_3 = Color.rgb(26, 46, 70);
    private final int TEXT = Color.rgb(242, 247, 255);
    private final int MUTED = Color.rgb(150, 168, 191);
    private final int ACCENT = Color.rgb(77, 222, 196);
    private final int BLUE = Color.rgb(100, 166, 255);
    private final int GOLD = Color.rgb(255, 194, 92);
    private final int RED = Color.rgb(255, 105, 123);
    private final int PURPLE = Color.rgb(185, 133, 255);

    private SharedPreferences prefs;
    private JSONObject state;
    private FrameLayout contentHost;
    private int activeTab = 0;
    private final String[] tabNames = {"Tổng quan", "Thẻ", "Chi tiêu", "Tiết kiệm", "Thống kê"};
    private final Button[] tabButtons = new Button[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
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

    private void loadState() {
        String raw = prefs.getString(KEY_STATE, "");
        try { state = raw.isEmpty() ? freshState() : new JSONObject(raw); }
        catch (Exception e) { state = freshState(); }
        ensureState();
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
                if (!p.has("imported")) p.put("imported", false);
                if (!p.has("originalDays")) p.put("originalDays", p.optInt("days", "weekly".equals(p.optString("type")) ? 7 : 30));
                if (!p.has("upfrontCredited")) p.put("upfrontCredited", p.optBoolean("imported", false) ? 0 : ("weekly".equals(p.optString("type")) ? 100 : 500));
            }
        } catch (Exception ignored) {}
    }

    private void saveState() { prefs.edit().putString(KEY_STATE, state.toString()).apply(); }
    private JSONArray arr(String key) { JSONArray a = state.optJSONArray(key); return a == null ? new JSONArray() : a; }
    private JSONObject budgets() { JSONObject o = state.optJSONObject("budgets"); return o == null ? new JSONObject() : o; }
    private int available() { return Math.max(0, state.optInt("available", 0)); }
    private void setAvailable(int n) { try { state.put("available", Math.max(0, n)); } catch (Exception ignored) {} }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(10));
        LinearLayout titleWrap = new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        titleWrap.addView(text("KCFF", 26, TEXT, true));
        titleWrap.addView(text("DIAMOND CONTROL • OFFLINE", 10, ACCENT, true));
        header.addView(titleWrap, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(pill("v3.0", BLUE, Color.rgb(15, 28, 46)));
        shell.addView(header);
        contentHost = new FrameLayout(this);
        shell.addView(contentHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(5), dp(6), dp(5), dp(8));
        tabBar.setBackgroundColor(Color.rgb(9, 18, 30));
        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(tabNames[i]);
            b.setAllCaps(false);
            b.setTextSize(10);
            b.setMinHeight(0);
            b.setMinWidth(0);
            b.setPadding(dp(2), dp(8), dp(2), dp(8));
            b.setOnClickListener(v -> { activeTab = index; renderTab(); });
            tabButtons[i] = b;
            tabBar.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        shell.addView(tabBar);
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
        root.setPadding(dp(14), dp(10), dp(14), dp(28));
        scroll.addView(root);
        contentHost.addView(scroll);
        switch (activeTab) {
            case 0: buildDashboard(root); break;
            case 1: buildPasses(root); break;
            case 2: buildExpenses(root); break;
            case 3: buildCampaigns(root); break;
            case 4: buildStats(root); break;
        }
    }

    private void buildDashboard(LinearLayout root) {
        sectionHeader(root, "TỔNG QUAN", "Kho kim cương của bạn", "Theo dõi số dư, nguồn thẻ và việc cần làm hôm nay.");
        LinearLayout hero = card(SURFACE_2);
        hero.addView(text("TỔNG TÀI SẢN", 11, ACCENT, true));
        hero.addView(text(fmt(totalOwned()) + " KC", 34, TEXT, true));
        hero.addView(text("Sẵn " + fmt(available()) + "  •  Đang tiết kiệm " + fmt(savedTotal()), 13, MUTED, false));
        addGap(hero, 12);
        LinearLayout quick = row();
        Button add = actionButton("+ Thêm KC", ACCENT, Color.BLACK);
        add.setOnClickListener(v -> showAddKcDialog());
        Button claim = actionButton("Nhận tất cả", BLUE, Color.BLACK);
        claim.setOnClickListener(v -> claimAllToday());
        quick.addView(add, weight()); quick.addView(claim, weight()); hero.addView(quick); root.addView(hero);
        LinearLayout metricRow = row();
        metricRow.addView(metricCard("Chờ từ thẻ", fmt(pendingFor("weekly") + pendingFor("monthly")), BLUE), weight());
        metricRow.addView(metricCard("Nhận hôm nay", fmt(receivedTodayFromPasses()), ACCENT), weight());
        root.addView(metricRow);
        LinearLayout due = card(SURFACE);
        due.addView(text("VIỆC CẦN LÀM HÔM NAY", 11, GOLD, true));
        int dueCount = duePassCount(), dueKc = duePassAmount();
        if (dueCount == 0) {
            due.addView(text("✓ Không còn lượt nhận KC nào hôm nay", 17, TEXT, true));
            due.addView(text("Bạn đã xử lý hết các thẻ đang hoạt động.", 12, MUTED, false));
        } else {
            due.addView(text(dueCount + " thẻ đang chờ • " + fmt(dueKc) + " KC", 18, TEXT, true));
            due.addView(text("Nhận trước khi ngày kết thúc để thống kê nguồn không bị hụt.", 12, MUTED, false));
        }
        addGap(due, 10);
        Button all = actionButton(dueCount == 0 ? "Đã nhận đủ hôm nay" : "Nhận tất cả " + fmt(dueKc) + " KC", dueCount == 0 ? SURFACE_3 : GOLD, dueCount == 0 ? MUTED : Color.BLACK);
        all.setEnabled(dueCount > 0); all.setOnClickListener(v -> claimAllToday()); due.addView(all); root.addView(due);
        LinearLayout source = card(SURFACE);
        source.addView(text("NGUỒN THẺ", 11, PURPLE, true)); addGap(source, 5);
        LinearLayout sr = row();
        sr.addView(sourceMini("Thẻ tuần", sourceReceived("weekly"), countPasses("weekly"), ACCENT), weight());
        sr.addView(sourceMini("Thẻ tháng", sourceReceived("monthly"), countPasses("monthly"), PURPLE), weight());
        source.addView(sr); root.addView(source);
        LinearLayout reminder = card(SURFACE);
        reminder.addView(text("NHẮC NHẬN KC", 11, BLUE, true));
        boolean enabled = state.optBoolean("reminderEnabled", true);
        reminder.addView(text(enabled ? "Đang bật • " + reminderTimeText() : "Đang tắt", 17, TEXT, true));
        reminder.addView(text(enabled ? "Thông báo chỉ xuất hiện khi còn thẻ chưa nhận trong ngày." : "Bạn sẽ không nhận thông báo nhắc thẻ.", 12, MUTED, false));
        addGap(reminder, 8);
        Button config = actionButton("Cài giờ nhắc", BLUE, Color.BLACK); config.setOnClickListener(v -> showReminderDialog()); reminder.addView(config); root.addView(reminder);
        sectionHeader(root, "GẦN ĐÂY", "Biến động mới nhất", null); renderRecentHistory(root, 6);
    }

    private void buildPasses(LinearLayout root) {
        sectionHeader(root, "THẺ THÀNH VIÊN", "Quản lý thẻ tuần & tháng", "Thêm thẻ mới hoặc nhập thẻ đã mua và chỉ còn vài ngày.");
        LinearLayout newCard = card(SURFACE_2);
        newCard.addView(text("THẺ MỚI", 11, ACCENT, true));
        newCard.addView(text("Kích hoạt đúng lúc vừa mua", 18, TEXT, true));
        newCard.addView(text("Thẻ tuần nhận ngay 100 KC. Thẻ tháng nhận ngay 500 KC.", 12, MUTED, false)); addGap(newCard, 10);
        LinearLayout r1 = row();
        Button nw = actionButton("+ Tuần mới", ACCENT, Color.BLACK); nw.setOnClickListener(v -> activatePass("weekly", 7, false));
        Button nm = actionButton("+ Tháng mới", PURPLE, Color.BLACK); nm.setOnClickListener(v -> activatePass("monthly", 30, false));
        r1.addView(nw, weight()); r1.addView(nm, weight()); newCard.addView(r1); root.addView(newCard);
        LinearLayout existingCard = card(Color.rgb(20, 30, 46));
        existingCard.addView(text("THẺ ĐANG CHẠY • NHẬP THỦ CÔNG", 11, GOLD, true));
        existingCard.addView(text("Đã mua trước đó? Nhập số ngày còn lại", 18, TEXT, true));
        existingCard.addView(text("Không cộng thưởng ban đầu. Ví dụ tuần còn 4 ngày = chỉ còn 4 × 50 KC để nhận.", 12, MUTED, false)); addGap(existingCard, 10);
        LinearLayout r2 = row();
        Button ew = actionButton("Tuần còn ngày", GOLD, Color.BLACK); ew.setOnClickListener(v -> showExistingPassDialog("weekly"));
        Button em = actionButton("Tháng còn ngày", BLUE, Color.BLACK); em.setOnClickListener(v -> showExistingPassDialog("monthly"));
        r2.addView(ew, weight()); r2.addView(em, weight()); existingCard.addView(r2); root.addView(existingCard);
        if (duePassCount() > 0) {
            Button claimAll = actionButton("Nhận tất cả hôm nay • " + fmt(duePassAmount()) + " KC", ACCENT, Color.BLACK); claimAll.setOnClickListener(v -> claimAllToday()); root.addView(claimAll); addGap(root, 12);
        }
        JSONArray passes = arr("passes");
        if (passes.length() == 0) { root.addView(emptyCard("Chưa có thẻ nào. Thêm thẻ mới hoặc nhập thẻ đang chạy.")); return; }
        for (int i = passes.length() - 1; i >= 0; i--) { JSONObject p = passes.optJSONObject(i); if (p != null) root.addView(passCard(p)); }
    }

    private View passCard(JSONObject p) {
        boolean weekly = "weekly".equals(p.optString("type"));
        boolean imported = p.optBoolean("imported", false);
        int days = p.optInt("days", weekly ? 7 : 30), daily = p.optInt("daily", weekly ? 50 : 70);
        JSONArray claimed = p.optJSONArray("claimed"); int claimedCount = claimed == null ? 0 : claimed.length();
        long end = p.optLong("start") + days * DAY_MS;
        boolean active = System.currentTimeMillis() < end && claimedCount < days;
        boolean todayClaimed = contains(claimed, dateKey(System.currentTimeMillis()));
        int remainClaims = Math.max(0, days - claimedCount);
        int received = claimedCount * daily + p.optInt("upfrontCredited", imported ? 0 : (weekly ? 100 : 500));
        LinearLayout c = card(SURFACE);
        LinearLayout head = row();
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text(weekly ? "THẺ TUẦN" : "THẺ THÁNG", 11, weekly ? ACCENT : PURPLE, true));
        t.addView(text(imported ? "Thẻ nhập thủ công" : "Thẻ kích hoạt trong app", 15, TEXT, true));
        head.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        head.addView(pill(active ? "Đang chạy" : "Đã xong", active ? ACCENT : MUTED, SURFACE_3)); c.addView(head); addGap(c, 10);
        c.addView(text("Đã nhận từ thẻ này: " + fmt(received) + " KC", 17, TEXT, true));
        c.addView(text("Lượt ngày: " + claimedCount + "/" + days + "  •  Còn " + remainClaims + " lượt = " + fmt(remainClaims * daily) + " KC", 12, MUTED, false));
        if (imported) c.addView(text("Không cộng thưởng ban đầu • bắt đầu quản lý từ ngày nhập", 11, GOLD, true));
        addGap(c, 8);
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(Math.max(1, days)); pb.setProgress(claimedCount); c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))); addGap(c, 10);
        Button claim = actionButton(todayClaimed ? "Hôm nay đã nhận" : (active ? "Nhận " + daily + " KC hôm nay" : "Thẻ đã kết thúc"), active && !todayClaimed ? (weekly ? ACCENT : PURPLE) : SURFACE_3, active && !todayClaimed ? Color.BLACK : MUTED);
        claim.setEnabled(active && !todayClaimed); String id = p.optString("id"); claim.setOnClickListener(v -> claimPass(id)); c.addView(claim); return c;
    }

    private void buildExpenses(LinearLayout root) {
        sectionHeader(root, "CHI TIÊU", "Kiểm soát KC đã dùng", "Theo dõi ngân sách tháng theo từng nhóm và hoàn tác khi ghi nhầm.");
        LinearLayout top = card(SURFACE_2);
        top.addView(text("ĐÃ CHI THÁNG NÀY", 11, RED, true)); top.addView(text(fmt(monthSpend()) + " KC", 30, TEXT, true)); top.addView(text("Trung bình " + fmt(avgDailySpendThisMonth()) + " KC/ngày có phát sinh", 12, MUTED, false)); addGap(top, 10);
        LinearLayout actions = row();
        Button add = actionButton("+ Ghi chi tiêu", RED, Color.WHITE); add.setOnClickListener(v -> showExpenseDialog());
        Button budget = actionButton("Đặt ngân sách", BLUE, Color.BLACK); budget.setOnClickListener(v -> showBudgetDialog());
        actions.addView(add, weight()); actions.addView(budget, weight()); top.addView(actions); root.addView(top);
        for (String cat : categories()) {
            int spent = monthSpendFor(cat), limit = budgets().optInt(cat, 0); if (spent == 0 && limit == 0) continue;
            LinearLayout b = card(SURFACE); b.addView(text(cat, 14, TEXT, true)); b.addView(text(limit > 0 ? fmt(spent) + " / " + fmt(limit) + " KC" : fmt(spent) + " KC • chưa đặt giới hạn", 12, limit > 0 && spent > limit ? RED : MUTED, false));
            if (limit > 0) { ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(limit); pb.setProgress(Math.min(spent, limit)); b.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7))); }
            root.addView(b);
        }
        sectionHeader(root, "LỊCH SỬ CHI", "Khoản chi gần đây", null);
        JSONArray e = arr("expenses"); if (e.length() == 0) { root.addView(emptyCard("Chưa có khoản chi nào.")); return; }
        int start = Math.max(0, e.length() - 20);
        for (int i = e.length() - 1; i >= start; i--) {
            JSONObject item = e.optJSONObject(i); if (item == null) continue;
            LinearLayout c = card(SURFACE); LinearLayout h = row(); LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(item.optString("name"), 15, TEXT, true)); left.addView(text(item.optString("category") + " • " + dateTime(item.optLong("time")), 11, MUTED, false));
            h.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); h.addView(text("-" + fmt(item.optInt("amount")), 17, RED, true)); c.addView(h);
            Button undo = smallAction("Hoàn tác & hoàn KC"); String id = item.optString("id"); undo.setOnClickListener(v -> undoExpense(id)); c.addView(undo); root.addView(c);
        }
    }

    private void buildCampaigns(LinearLayout root) {
        sectionHeader(root, "CHIẾN DỊCH TIẾT KIỆM", "Khóa KC cho mục tiêu", "Đặt deadline để app tính số KC nên dành mỗi ngày.");
        LinearLayout hero = card(SURFACE_2); hero.addView(text("ĐANG KHÓA", 11, GOLD, true)); hero.addView(text(fmt(savedTotal()) + " KC", 30, TEXT, true)); hero.addView(text(arr("campaigns").length() + " chiến dịch đang theo dõi", 12, MUTED, false)); addGap(hero, 10);
        Button add = actionButton("+ Tạo chiến dịch", GOLD, Color.BLACK); add.setOnClickListener(v -> showCampaignDialog()); hero.addView(add); root.addView(hero);
        JSONArray a = arr("campaigns"); if (a.length() == 0) { root.addView(emptyCard("Chưa có chiến dịch tiết kiệm.")); return; }
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject cdata = a.optJSONObject(i); if (cdata == null) continue;
            int target = Math.max(1, cdata.optInt("target")), saved = Math.max(0, cdata.optInt("saved")); long deadline = cdata.optLong("deadline", 0); int left = Math.max(0, target - saved); int daysLeft = deadline > 0 ? Math.max(0, (int)Math.ceil((deadline - System.currentTimeMillis()) / (double)DAY_MS)) : 0; int perDay = daysLeft > 0 ? (int)Math.ceil(left / (double)daysLeft) : left; int pct = Math.min(100, (int)Math.round(saved * 100.0 / target));
            LinearLayout c = card(SURFACE); c.addView(text(cdata.optString("name"), 18, TEXT, true)); c.addView(text(fmt(saved) + " / " + fmt(target) + " KC • " + pct + "%", 13, GOLD, true));
            if (deadline > 0) c.addView(text(daysLeft > 0 ? "Còn " + daysLeft + " ngày • nên giữ ~" + fmt(perDay) + " KC/ngày" : (left == 0 ? "Đã đạt mục tiêu" : "Đã tới deadline • còn thiếu " + fmt(left) + " KC"), 12, MUTED, false));
            addGap(c, 7); ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(target); pb.setProgress(Math.min(saved, target)); c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))); addGap(c, 8);
            LinearLayout acts = row(); String id = cdata.optString("id"); Button put = smallAction("+ Gửi KC"); put.setOnClickListener(v -> showCampaignTransfer(id, true)); Button take = smallAction("Rút KC"); take.setOnClickListener(v -> showCampaignTransfer(id, false)); Button close = smallAction("Đóng"); close.setOnClickListener(v -> confirmCloseCampaign(id)); acts.addView(put); acts.addView(take); acts.addView(close); c.addView(acts); root.addView(c);
        }
    }

    private void buildStats(LinearLayout root) {
        sectionHeader(root, "THỐNG KÊ", "Nguồn vào & dòng KC", "Tách rõ KC từ thẻ tuần, thẻ tháng, nạp thủ công và chi tiêu.");
        LinearLayout source = card(SURFACE_2); source.addView(text("KC ĐÃ NHẬN TỪ THẺ • TOÀN THỜI GIAN", 11, PURPLE, true)); addGap(source, 8);
        source.addView(statLine("Thẻ tuần", sourceReceived("weekly"), ACCENT)); source.addView(statLine("Trong đó thẻ tuần nhập thủ công", sourceReceivedImported("weekly"), GOLD)); source.addView(statLine("Thẻ tháng", sourceReceived("monthly"), PURPLE)); source.addView(statLine("Trong đó thẻ tháng nhập thủ công", sourceReceivedImported("monthly"), BLUE)); root.addView(source);
        LinearLayout month = card(SURFACE); month.addView(text("THÁNG NÀY", 11, BLUE, true)); month.addView(statLine("KC từ thẻ tuần", sourceReceivedThisMonth("weekly"), ACCENT)); month.addView(statLine("KC từ thẻ tháng", sourceReceivedThisMonth("monthly"), PURPLE)); month.addView(statLine("KC nạp/thêm khác", sourceReceivedThisMonth("manual"), BLUE)); month.addView(statLine("KC đã chi", monthSpend(), RED)); root.addView(month);
        LinearLayout future = card(SURFACE); future.addView(text("DỰ KIẾN", 11, GOLD, true)); future.addView(statLine("KC hiện sở hữu", totalOwned(), TEXT)); future.addView(statLine("KC còn chờ từ thẻ tuần", pendingFor("weekly"), ACCENT)); future.addView(statLine("KC còn chờ từ thẻ tháng", pendingFor("monthly"), PURPLE)); future.addView(statLine("Tài sản sau khi nhận hết", totalOwned() + pendingFor("weekly") + pendingFor("monthly"), GOLD)); root.addView(future);
        LinearLayout chart = card(SURFACE); chart.addView(text("CHI TIÊU 7 NGÀY GẦN NHẤT", 11, RED, true)); addGap(chart, 8); render7DayBars(chart); root.addView(chart);
        LinearLayout tools = card(SURFACE); tools.addView(text("DỮ LIỆU & NHẮC NHỞ", 11, MUTED, true));
        Button remind = actionButton("Cài nhắc nhận KC", BLUE, Color.BLACK); remind.setOnClickListener(v -> showReminderDialog());
        Button backup = actionButton("Xem mã backup JSON", SURFACE_3, TEXT); backup.setOnClickListener(v -> showBackupDialog());
        Button restore = actionButton("Khôi phục từ JSON", SURFACE_3, TEXT); restore.setOnClickListener(v -> showRestoreDialog());
        Button reset = actionButton("Xóa toàn bộ dữ liệu", RED, Color.WHITE); reset.setOnClickListener(v -> confirmReset());
        tools.addView(remind); addGap(tools, 6); tools.addView(backup); addGap(tools, 6); tools.addView(restore); addGap(tools, 6); tools.addView(reset); root.addView(tools);
    }

    private void showExistingPassDialog(String type) {
        boolean weekly = "weekly".equals(type); int max = weekly ? 7 : 30; int daily = weekly ? 50 : 70;
        LinearLayout form = dialogForm(); EditText days = input("Số ngày còn lại (1-" + max + ")", true); form.addView(days); form.addView(text("Thẻ đã mua trước đó. App KHÔNG cộng " + (weekly ? "100" : "500") + " KC ban đầu. Chỉ quản lý " + daily + " KC/ngày trong số ngày còn lại.", 12, MUTED, false));
        new AlertDialog.Builder(this).setTitle(weekly ? "Nhập thẻ tuần đang chạy" : "Nhập thẻ tháng đang chạy").setView(form).setPositiveButton("Thêm thẻ", (d, w) -> { int remaining = parsePositive(days.getText().toString()); if (remaining < 1 || remaining > max) { toast("Số ngày phải từ 1 đến " + max); return; } activatePass(type, remaining, true); }).setNegativeButton("Hủy", null).show();
    }

    private void activatePass(String type, int managedDays, boolean imported) {
        boolean weekly = "weekly".equals(type); int upfront = imported ? 0 : (weekly ? 100 : 500); int daily = weekly ? 50 : 70; JSONObject p = new JSONObject();
        try {
            p.put("id", UUID.randomUUID().toString()); p.put("type", type); p.put("start", System.currentTimeMillis()); p.put("days", managedDays); p.put("originalDays", weekly ? 7 : 30); p.put("daily", daily); p.put("claimed", new JSONArray()); p.put("imported", imported); p.put("upfrontCredited", upfront); arr("passes").put(p);
            if (upfront > 0) setAvailable(available() + upfront);
            addHistory(imported ? (weekly ? "Nhập thẻ tuần còn " : "Nhập thẻ tháng còn ") + managedDays + " ngày" : (weekly ? "Kích hoạt thẻ tuần mới" : "Kích hoạt thẻ tháng mới"), upfront, type, imported);
            saveState(); renderTab(); if (upfront > 0) toast("Đã cộng thưởng ban đầu " + upfront + " KC"); else toast("Đã thêm thẻ đang chạy • không cộng thưởng ban đầu"); scheduleReminderIfNeeded();
        } catch (Exception e) { toast("Không thể thêm thẻ"); }
    }

    private void claimPass(String id) {
        JSONArray a = arr("passes"); long now = System.currentTimeMillis();
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i); if (p == null || !id.equals(p.optString("id"))) continue; int days = p.optInt("days"); JSONArray claimed = p.optJSONArray("claimed"); if (claimed == null) claimed = new JSONArray();
            if (now >= p.optLong("start") + days * DAY_MS || claimed.length() >= days) { toast("Thẻ đã kết thúc"); return; }
            String today = dateKey(now); if (contains(claimed, today)) { toast("Hôm nay đã nhận thẻ này rồi"); return; }
            int daily = p.optInt("daily"); claimed.put(today); try { p.put("claimed", claimed); } catch (Exception ignored) {} setAvailable(available() + daily);
            addHistory("Nhận " + ("weekly".equals(p.optString("type")) ? "thẻ tuần" : "thẻ tháng") + (p.optBoolean("imported") ? " • thẻ nhập thủ công" : ""), daily, p.optString("type"), p.optBoolean("imported")); saveState(); renderTab(); toast("+" + daily + " KC"); return;
        }
    }

    private void claimAllToday() {
        JSONArray a = arr("passes"); long now = System.currentTimeMillis(); String today = dateKey(now); int total = 0, count = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i); if (p == null) continue; int days = p.optInt("days"); JSONArray claimed = p.optJSONArray("claimed"); if (claimed == null) claimed = new JSONArray();
            if (now >= p.optLong("start") + days * DAY_MS || claimed.length() >= days || contains(claimed, today)) continue;
            int daily = p.optInt("daily"); claimed.put(today); try { p.put("claimed", claimed); } catch (Exception ignored) {} total += daily; count++;
            addHistory("Nhận " + ("weekly".equals(p.optString("type")) ? "thẻ tuần" : "thẻ tháng") + (p.optBoolean("imported") ? " • thẻ nhập thủ công" : ""), daily, p.optString("type"), p.optBoolean("imported"));
        }
        if (count == 0) { toast("Không có KC thẻ nào đang chờ hôm nay"); return; } setAvailable(available() + total); saveState(); renderTab(); toast("Đã nhận " + fmt(total) + " KC từ " + count + " thẻ");
    }

    private int duePassCount() { int c = 0; long now = System.currentTimeMillis(); String today = dateKey(now); JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p == null) continue; JSONArray cl = p.optJSONArray("claimed"); if (now < p.optLong("start") + p.optInt("days") * DAY_MS && (cl == null || cl.length() < p.optInt("days")) && !contains(cl, today)) c++; } return c; }
    private int duePassAmount() { int total = 0; long now = System.currentTimeMillis(); String today = dateKey(now); JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p == null) continue; JSONArray cl = p.optJSONArray("claimed"); if (now < p.optLong("start") + p.optInt("days") * DAY_MS && (cl == null || cl.length() < p.optInt("days")) && !contains(cl, today)) total += p.optInt("daily"); } return total; }
    private int pendingFor(String type) { int total = 0; long now = System.currentTimeMillis(); JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p == null || !type.equals(p.optString("type"))) continue; int days = p.optInt("days"); JSONArray cl = p.optJSONArray("claimed"); int n = cl == null ? 0 : cl.length(); if (now < p.optLong("start") + days * DAY_MS) total += Math.max(0, days - n) * p.optInt("daily"); } return total; }
    private int countPasses(String type) { int n = 0; JSONArray a = arr("passes"); for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && type.equals(p.optString("type"))) n++; } return n; }
    private int sourceReceived(String source) { return historySum(source, false, 0, false); }
    private int sourceReceivedImported(String source) { return historySum(source, true, 0, false); }
    private int sourceReceivedThisMonth(String source) { return historySum(source, false, monthStart(), true); }

    private int historySum(String source, boolean importedOnly, long since, boolean useSince) {
        int total = 0; JSONArray h = arr("history");
        for (int i = 0; i < h.length(); i++) {
            JSONObject x = h.optJSONObject(i); if (x == null) continue;
            String recordedSource = x.optString("source", "");
            if (recordedSource.isEmpty()) {
                String label = x.optString("label", "").toLowerCase(new Locale("vi", "VN"));
                if (label.contains("thẻ tuần")) recordedSource = "weekly"; else if (label.contains("thẻ tháng")) recordedSource = "monthly"; else if (x.optInt("delta") > 0) recordedSource = "manual";
            }
            if (source != null && !source.equals(recordedSource)) continue;
            if (importedOnly && !(x.optBoolean("imported", false) || x.optString("label", "").toLowerCase(new Locale("vi", "VN")).contains("nhập thủ công"))) continue;
            if (useSince && x.optLong("time") < since) continue; int d = x.optInt("delta"); if (d > 0) total += d;
        }
        return total;
    }

    private int receivedTodayFromPasses() { int total = 0; String today = dateKey(System.currentTimeMillis()); JSONArray h = arr("history"); for (int i = 0; i < h.length(); i++) { JSONObject x = h.optJSONObject(i); if (x == null) continue; String s = x.optString("source"); if (("weekly".equals(s) || "monthly".equals(s)) && today.equals(dateKey(x.optLong("time"))) && x.optInt("delta") > 0) total += x.optInt("delta"); } return total; }
    private int totalOwned() { return available() + savedTotal(); }
    private int savedTotal() { int t = 0; JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c != null) t += Math.max(0, c.optInt("saved")); } return t; }

    private void showAddKcDialog() { LinearLayout f = dialogForm(); EditText amount = input("Số KC", true); EditText note = input("Ghi chú", false); f.addView(amount); f.addView(note); new AlertDialog.Builder(this).setTitle("Thêm KC sẵn").setView(f).setPositiveButton("Thêm", (d, w) -> { int n = parsePositive(amount.getText().toString()); if (n <= 0) { toast("Số KC không hợp lệ"); return; } setAvailable(available() + n); String label = note.getText().toString().trim(); addHistory(label.isEmpty() ? "Thêm KC sẵn" : label, n, "manual", false); saveState(); renderTab(); }).setNegativeButton("Hủy", null).show(); }

    private void showExpenseDialog() {
        LinearLayout f = dialogForm(); EditText name = input("Nội dung chi tiêu", false); EditText amount = input("Số KC", true); Spinner cat = new Spinner(this); cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories())); f.addView(name); f.addView(amount); f.addView(cat);
        new AlertDialog.Builder(this).setTitle("Ghi chi tiêu").setView(f).setPositiveButton("Lưu", (d, w) -> { String nme = name.getText().toString().trim(); int n = parsePositive(amount.getText().toString()); if (nme.isEmpty() || n <= 0) { toast("Nhập đủ nội dung và số KC"); return; } if (n > available()) { toast("KC sẵn không đủ"); return; } String category = String.valueOf(cat.getSelectedItem()); int limit = budgets().optInt(category, 0); int projected = monthSpendFor(category) + n; if (limit > 0 && projected > limit) toast("Cảnh báo: khoản này vượt ngân sách " + category); JSONObject e = new JSONObject(); try { e.put("id", UUID.randomUUID().toString()); e.put("name", nme); e.put("amount", n); e.put("category", category); e.put("time", System.currentTimeMillis()); arr("expenses").put(e); } catch (Exception ignored) {} setAvailable(available() - n); addHistory("Chi: " + nme, -n, "expense", false); saveState(); renderTab(); }).setNegativeButton("Hủy", null).show();
    }

    private void undoExpense(String id) { JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e == null || !id.equals(e.optString("id"))) continue; int n = e.optInt("amount"); String name = e.optString("name"); a.remove(i); setAvailable(available() + n); addHistory("Hoàn tác chi: " + name, n, "refund", false); saveState(); renderTab(); toast("Đã hoàn " + fmt(n) + " KC"); return; } }
    private String[] categories() { return new String[]{"Vòng quay", "Trang phục", "Vũ khí", "Pet", "Sự kiện", "Nâng cấp súng", "Royale Pass", "Khác"}; }

    private void showBudgetDialog() { LinearLayout f = dialogForm(); Spinner cat = new Spinner(this); cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories())); EditText amount = input("Giới hạn KC / tháng (0 = bỏ)", true); f.addView(cat); f.addView(amount); new AlertDialog.Builder(this).setTitle("Ngân sách theo nhóm").setView(f).setPositiveButton("Lưu", (d, w) -> { String c = String.valueOf(cat.getSelectedItem()); int n; try { n = Integer.parseInt(amount.getText().toString().trim()); } catch (Exception e) { n = -1; } if (n < 0) { toast("Giới hạn không hợp lệ"); return; } try { JSONObject b = budgets(); b.put(c, n); state.put("budgets", b); } catch (Exception ignored) {} saveState(); renderTab(); }).setNegativeButton("Hủy", null).show(); }
    private int monthSpend() { int t = 0; JSONArray a = arr("expenses"); long s = monthStart(); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= s) t += e.optInt("amount"); } return t; }
    private int monthSpendFor(String cat) { int t = 0; JSONArray a = arr("expenses"); long s = monthStart(); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= s && cat.equals(e.optString("category"))) t += e.optInt("amount"); } return t; }
    private int avgDailySpendThisMonth() { JSONArray a = arr("expenses"); long s = monthStart(); int sum = 0; java.util.HashSet<String> days = new java.util.HashSet<>(); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= s) { sum += e.optInt("amount"); days.add(dateKey(e.optLong("time"))); } } return days.isEmpty() ? 0 : (int)Math.round(sum / (double)days.size()); }

    private void showCampaignDialog() { LinearLayout f = dialogForm(); EditText name = input("Tên chiến dịch", false); EditText target = input("Mục tiêu KC", true); EditText days = input("Deadline sau bao nhiêu ngày", true); f.addView(name); f.addView(target); f.addView(days); new AlertDialog.Builder(this).setTitle("Tạo chiến dịch tiết kiệm").setView(f).setPositiveButton("Tạo", (d, w) -> { String nme = name.getText().toString().trim(); int t = parsePositive(target.getText().toString()); int ds = parsePositive(days.getText().toString()); if (nme.isEmpty() || t <= 0 || ds <= 0) { toast("Nhập đủ tên, mục tiêu và số ngày"); return; } JSONObject c = new JSONObject(); try { c.put("id", UUID.randomUUID().toString()); c.put("name", nme); c.put("target", t); c.put("saved", 0); c.put("time", System.currentTimeMillis()); c.put("deadline", System.currentTimeMillis() + ds * DAY_MS); arr("campaigns").put(c); } catch (Exception ignored) {} addHistory("Tạo chiến dịch: " + nme, 0, "campaign", false); saveState(); renderTab(); }).setNegativeButton("Hủy", null).show(); }
    private JSONObject findCampaign(String id) { JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c != null && id.equals(c.optString("id"))) return c; } return null; }
    private void showCampaignTransfer(String id, boolean into) { JSONObject c = findCampaign(id); if (c == null) return; LinearLayout f = dialogForm(); EditText amount = input("Số KC", true); f.addView(amount); new AlertDialog.Builder(this).setTitle((into ? "Gửi vào: " : "Rút từ: ") + c.optString("name")).setView(f).setPositiveButton(into ? "Gửi" : "Rút", (d, w) -> { int n = parsePositive(amount.getText().toString()); if (n <= 0) { toast("Số KC không hợp lệ"); return; } if (into && n > available()) { toast("KC sẵn không đủ"); return; } if (!into && n > c.optInt("saved")) { toast("Quỹ không đủ KC"); return; } try { c.put("saved", c.optInt("saved") + (into ? n : -n)); } catch (Exception ignored) {} setAvailable(available() + (into ? -n : n)); addHistory((into ? "Tiết kiệm: " : "Rút tiết kiệm: ") + c.optString("name"), 0, "campaign", false); saveState(); renderTab(); }).setNegativeButton("Hủy", null).show(); }
    private void confirmCloseCampaign(String id) { JSONObject c = findCampaign(id); if (c == null) return; new AlertDialog.Builder(this).setTitle("Đóng chiến dịch?").setMessage("Hoàn " + fmt(c.optInt("saved")) + " KC về KC sẵn.").setPositiveButton("Đóng & hoàn KC", (d, w) -> closeCampaign(id)).setNegativeButton("Hủy", null).show(); }
    private void closeCampaign(String id) { JSONArray a = arr("campaigns"); for (int i = 0; i < a.length(); i++) { JSONObject c = a.optJSONObject(i); if (c == null || !id.equals(c.optString("id"))) continue; int s = c.optInt("saved"); setAvailable(available() + s); addHistory("Đóng chiến dịch: " + c.optString("name"), 0, "campaign", false); a.remove(i); saveState(); renderTab(); toast("Đã hoàn " + fmt(s) + " KC"); return; } }

    private void showReminderDialog() {
        LinearLayout f = dialogForm(); Switch enabled = new Switch(this); enabled.setText("Bật nhắc nhận KC hằng ngày"); enabled.setChecked(state.optBoolean("reminderEnabled", true)); f.addView(enabled); TimePicker picker = new TimePicker(this); picker.setIs24HourView(true); int h = state.optInt("reminderHour", 19), m = state.optInt("reminderMinute", 0); if (Build.VERSION.SDK_INT >= 23) { picker.setHour(h); picker.setMinute(m); } else { picker.setCurrentHour(h); picker.setCurrentMinute(m); } f.addView(picker);
        new AlertDialog.Builder(this).setTitle("Nhắc nhận KC").setMessage("Thông báo chạy offline và chỉ nhắc khi còn thẻ chưa nhận trong ngày.").setView(f).setPositiveButton("Lưu", (d, w) -> { int hh, mm; if (Build.VERSION.SDK_INT >= 23) { hh = picker.getHour(); mm = picker.getMinute(); } else { hh = picker.getCurrentHour(); mm = picker.getCurrentMinute(); } try { state.put("reminderEnabled", enabled.isChecked()); state.put("reminderHour", hh); state.put("reminderMinute", mm); } catch (Exception ignored) {} saveState(); if (enabled.isChecked()) requestNotificationPermissionIfNeeded(); scheduleReminderIfNeeded(); renderTab(); toast(enabled.isChecked() ? "Đã bật nhắc lúc " + String.format(Locale.US, "%02d:%02d", hh, mm) : "Đã tắt nhắc nhận KC"); }).setNegativeButton("Hủy", null).show();
    }

    private void requestNotificationPermissionIfNeeded() { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42); }
    private void createNotificationChannel() { if (Build.VERSION.SDK_INT >= 26) { NotificationChannel c = new NotificationChannel(CHANNEL_ID, "Nhắc nhận kim cương", NotificationManager.IMPORTANCE_DEFAULT); c.setDescription("Nhắc nhận KC từ thẻ tuần/tháng đang hoạt động"); NotificationManager nm = getSystemService(NotificationManager.class); if (nm != null) nm.createNotificationChannel(c); } }
    private String reminderTimeText() { return String.format(new Locale("vi", "VN"), "%02d:%02d mỗi ngày", state.optInt("reminderHour", 19), state.optInt("reminderMinute", 0)); }
    private void scheduleReminderIfNeeded() { AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE); if (am == null) return; Intent i = new Intent(this, ReminderReceiver.class); PendingIntent pi = PendingIntent.getBroadcast(this, 9001, i, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)); am.cancel(pi); if (!state.optBoolean("reminderEnabled", true)) return; Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY, state.optInt("reminderHour", 19)); c.set(Calendar.MINUTE, state.optInt("reminderMinute", 0)); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1); am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi); }

    private void showBackupDialog() { EditText e = new EditText(this); e.setText(state.toString()); e.setSelectAllOnFocus(true); e.setMinLines(8); new AlertDialog.Builder(this).setTitle("Backup JSON").setMessage("Sao chép đoạn này và giữ ở nơi an toàn.").setView(e).setPositiveButton("Đóng", null).show(); }
    private void showRestoreDialog() { EditText e = new EditText(this); e.setHint("Dán JSON backup vào đây"); e.setMinLines(8); new AlertDialog.Builder(this).setTitle("Khôi phục dữ liệu").setView(e).setPositiveButton("Khôi phục", (d, w) -> { try { JSONObject restored = new JSONObject(e.getText().toString().trim()); state = restored; ensureState(); saveState(); scheduleReminderIfNeeded(); renderTab(); toast("Đã khôi phục dữ liệu"); } catch (Exception ex) { toast("JSON không hợp lệ"); } }).setNegativeButton("Hủy", null).show(); }
    private void confirmReset() { new AlertDialog.Builder(this).setTitle("Xóa toàn bộ dữ liệu?").setMessage("KC, thẻ, chi tiêu, chiến dịch, ngân sách và lịch sử sẽ bị xóa trên máy.").setPositiveButton("Xóa", (d, w) -> { state = freshState(); saveState(); scheduleReminderIfNeeded(); renderTab(); toast("Đã đặt lại dữ liệu"); }).setNegativeButton("Hủy", null).show(); }

    private void addHistory(String label, int delta, String source, boolean imported) { JSONObject h = new JSONObject(); try { h.put("label", label); h.put("delta", delta); h.put("source", source); h.put("imported", imported); h.put("time", System.currentTimeMillis()); arr("history").put(h); } catch (Exception ignored) {} }
    private void renderRecentHistory(LinearLayout root, int limit) { JSONArray a = arr("history"); if (a.length() == 0) { root.addView(emptyCard("Chưa có biến động.")); return; } int start = Math.max(0, a.length() - limit); for (int i = a.length() - 1; i >= start; i--) { JSONObject h = a.optJSONObject(i); if (h == null) continue; LinearLayout c = card(SURFACE); LinearLayout r = row(); LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.addView(text(h.optString("label"), 14, TEXT, true)); l.addView(text(dateTime(h.optLong("time")), 11, MUTED, false)); r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); int d = h.optInt("delta"); if (d != 0) r.addView(text((d > 0 ? "+" : "") + fmt(d), 16, d > 0 ? ACCENT : RED, true)); c.addView(r); root.addView(c); } }

    private void render7DayBars(LinearLayout parent) { int[] vals = new int[7]; String[] labels = new String[7]; int max = 1; for (int x = 6; x >= 0; x--) { Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -x); long start = dayStart(c.getTimeInMillis()), end = start + DAY_MS; int sum = 0; JSONArray a = arr("expenses"); for (int i = 0; i < a.length(); i++) { JSONObject e = a.optJSONObject(i); if (e != null && e.optLong("time") >= start && e.optLong("time") < end) sum += e.optInt("amount"); } int idx = 6 - x; vals[idx] = sum; labels[idx] = new SimpleDateFormat("dd/MM", Locale.US).format(new Date(start)); max = Math.max(max, sum); } for (int i = 0; i < 7; i++) { LinearLayout r = row(); r.addView(text(labels[i], 11, MUTED, false), new LinearLayout.LayoutParams(dp(46), ViewGroup.LayoutParams.WRAP_CONTENT)); ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(max); pb.setProgress(vals[i]); r.addView(pb, new LinearLayout.LayoutParams(0, dp(12), 1)); r.addView(text("  " + fmt(vals[i]), 11, vals[i] > 0 ? RED : MUTED, true)); parent.addView(r); addGap(parent, 5); } }

    private long monthStart() { Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }
    private long dayStart(long t) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(t); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTimeInMillis(); }
    private boolean contains(JSONArray a, String value) { if (a == null) return false; for (int i = 0; i < a.length(); i++) if (value.equals(a.optString(i))) return true; return false; }
    private String dateKey(long t) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t)); }
    private String dateTime(long t) { return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date(t)); }
    private String fmt(int n) { return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(n); }
    private int parsePositive(String raw) { try { long n = Long.parseLong(raw.trim()); return n > 0 && n <= Integer.MAX_VALUE ? (int)n : -1; } catch (Exception e) { return -1; } }

    private void sectionHeader(LinearLayout root, String eyebrow, String title, String subtitle) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(2), dp(8), dp(2), dp(10)); l.addView(text(eyebrow, 10, ACCENT, true)); l.addView(text(title, 22, TEXT, true)); if (subtitle != null) l.addView(text(subtitle, 12, MUTED, false)); root.addView(l); }
    private TextView text(String s, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL); v.setLineSpacing(0, 1.08f); return v; }
    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private LinearLayout.LayoutParams weight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); p.setMargins(dp(3), 0, dp(3), 0); return p; }
    private LinearLayout card(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(15), dp(16), dp(15)); l.setBackground(round(color, 18)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, 0, 0, dp(10)); l.setLayoutParams(p); return l; }
    private GradientDrawable round(int color, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private Button actionButton(String label, int bg, int fg) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13); b.setTextColor(fg); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setMinHeight(0); b.setPadding(dp(10), dp(10), dp(10), dp(10)); b.setBackground(round(bg, 14)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, dp(3), 0, dp(3)); b.setLayoutParams(p); return b; }
    private Button smallAction(String label) { Button b = actionButton(label, SURFACE_3, TEXT); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(dp(2), dp(3), dp(2), dp(3)); b.setLayoutParams(p); b.setTextSize(11); return b; }
    private TextView pill(String s, int fg, int bg) { TextView t = text(s, 10, fg, true); t.setPadding(dp(9), dp(5), dp(9), dp(5)); t.setBackground(round(bg, 30)); return t; }
    private View metricCard(String label, String value, int color) { LinearLayout c = card(SURFACE); c.setPadding(dp(12), dp(12), dp(12), dp(12)); c.addView(text(label, 10, MUTED, true)); c.addView(text(value + " KC", 18, color, true)); return c; }
    private View sourceMini(String label, int received, int count, int color) { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(8), dp(8), dp(8), dp(8)); c.addView(text(label, 11, MUTED, true)); c.addView(text(fmt(received) + " KC", 18, color, true)); c.addView(text(count + " thẻ đã ghi", 10, MUTED, false)); return c; }
    private View statLine(String label, int value, int color) { LinearLayout r = row(); r.setPadding(0, dp(7), 0, dp(7)); r.addView(text(label, 13, MUTED, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); r.addView(text(fmt(value) + " KC", 15, color, true)); return r; }
    private View emptyCard(String message) { LinearLayout c = card(SURFACE); c.addView(text(message, 13, MUTED, false)); return c; }
    private LinearLayout dialogForm() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20), dp(8), dp(20), 0); return l; }
    private EditText input(String hint, boolean numeric) { EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER); e.setPadding(dp(4), dp(12), dp(4), dp(12)); return e; }
    private void addGap(LinearLayout p, int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); p.addView(v); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
