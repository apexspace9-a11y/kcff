package com.apex.kcff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS = "kcff_store";
    private static final String KEY_STATE = "state";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int REQ_EXPORT = 701;
    private static final int REQ_IMPORT = 702;
    private static final String[] CATEGORIES = {"Vòng quay", "Trang phục", "Vũ khí", "Pet", "Sự kiện", "Khác"};

    private final int BG = Color.rgb(7, 17, 31);
    private final int PANEL = Color.rgb(15, 29, 48);
    private final int PANEL_2 = Color.rgb(20, 38, 62);
    private final int PANEL_3 = Color.rgb(25, 46, 72);
    private final int TEXT = Color.rgb(240, 247, 255);
    private final int MUTED = Color.rgb(149, 171, 198);
    private final int ACCENT = Color.rgb(79, 209, 197);
    private final int ACCENT_DARK = Color.rgb(18, 83, 82);
    private final int GOLD = Color.rgb(255, 196, 87);
    private final int RED = Color.rgb(255, 108, 122);
    private final int BLUE = Color.rgb(104, 164, 255);

    private SharedPreferences prefs;
    private JSONObject state;
    private FrameLayout content;
    private Button[] navButtons = new Button[5];
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadState();
        buildShell();
        showTab(0);
    }

    private void loadState() {
        String raw = prefs.getString(KEY_STATE, "");
        try {
            state = raw.isEmpty() ? freshState() : new JSONObject(raw);
        } catch (Exception e) {
            state = freshState();
        }
        migrateState();
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
        } catch (Exception ignored) {}
        return s;
    }

    private void migrateState() {
        try {
            if (!state.has("available")) state.put("available", 0);
            if (!state.has("passes")) state.put("passes", new JSONArray());
            if (!state.has("campaigns")) state.put("campaigns", new JSONArray());
            if (!state.has("expenses")) state.put("expenses", new JSONArray());
            if (!state.has("history")) state.put("history", new JSONArray());
            if (!state.has("budgets")) state.put("budgets", new JSONObject());
            JSONArray campaigns = arr("campaigns");
            for (int i = 0; i < campaigns.length(); i++) {
                JSONObject c = campaigns.optJSONObject(i);
                if (c == null) continue;
                if (!c.has("created")) c.put("created", c.optLong("time", System.currentTimeMillis()));
                if (!c.has("deadline")) c.put("deadline", 0);
            }
        } catch (Exception ignored) {}
        saveState();
    }

    private void saveState() {
        prefs.edit().putString(KEY_STATE, state.toString()).apply();
    }

    private JSONArray arr(String key) {
        JSONArray a = state.optJSONArray(key);
        return a == null ? new JSONArray() : a;
    }

    private JSONObject budgets() {
        JSONObject b = state.optJSONObject("budgets");
        if (b == null) {
            b = new JSONObject();
            try { state.put("budgets", b); } catch (Exception ignored) {}
        }
        return b;
    }

    private int available() {
        return Math.max(0, state.optInt("available", 0));
    }

    private void setAvailable(int value) {
        try { state.put("available", Math.max(0, value)); } catch (Exception ignored) {}
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable outlined(int fill, int stroke, int radius) {
        GradientDrawable g = rounded(fill, radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackground(rounded(PANEL, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(12));
        l.setLayoutParams(p);
        return l;
    }

    private LinearLayout heroCard() {
        LinearLayout l = card();
        l.setBackground(rounded(PANEL_2, 22));
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        return l;
    }

    private Button button(String label, int fill, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setMinHeight(dp(46));
        b.setBackground(rounded(fill, 14));
        return b;
    }

    private Button primary(String label) {
        return button(label, ACCENT, Color.rgb(2, 24, 28));
    }

    private Button secondary(String label) {
        Button b = button(label, PANEL_3, TEXT);
        b.setBackground(outlined(PANEL_3, Color.rgb(52, 73, 98), 14));
        return b;
    }

    private Button danger(String label) {
        return button(label, Color.rgb(93, 34, 46), Color.WHITE);
    }

    private void addGap(LinearLayout parent, int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        parent.addView(v);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout pageBody(String title, String sub) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(16), dp(14), dp(24));
        body.setBackgroundColor(BG);
        body.addView(text(title, 26, TEXT, true));
        TextView s = text(sub, 13, MUTED, false);
        s.setPadding(0, dp(2), 0, dp(14));
        body.addView(s);
        return body;
    }

    private ScrollView wrap(LinearLayout body) {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setBackgroundColor(BG);
        s.addView(body);
        return s;
    }

    private TextView eyebrow(String value) {
        TextView t = text(value, 11, ACCENT, true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    private TextView pill(String value, int fill, int color) {
        TextView t = text(value, 11, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(9), dp(5), dp(9), dp(5));
        t.setBackground(rounded(fill, 30));
        return t;
    }

    private ProgressBar progress(int pct, int color) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(100);
        p.setProgress(Math.max(0, Math.min(100, pct)));
        p.setProgressTintList(ColorStateList.valueOf(color));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(37, 55, 77)));
        p.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        return p;
    }

    private View miniStat(String label, String value, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(11), dp(12), dp(11));
        l.setBackground(rounded(PANEL_3, 14));
        l.addView(text(value, 17, color, true));
        l.addView(text(label, 11, MUTED, false));
        return l;
    }

    private void buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);

        LinearLayout top = row();
        top.setPadding(dp(15), dp(10), dp(10), dp(8));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("KCFF", 21, TEXT, true));
        brand.addView(text("DIAMOND MANAGER · OFFLINE", 10, ACCENT, true));
        top.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView local = pill("● LOCAL", Color.rgb(14, 63, 61), ACCENT);
        top.addView(local);
        shell.addView(top);

        content = new FrameLayout(this);
        content.setBackgroundColor(BG);
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = row();
        nav.setPadding(dp(6), dp(6), dp(6), dp(8));
        nav.setBackgroundColor(Color.rgb(10, 22, 38));
        String[] labels = {"Tổng quan", "Thẻ", "Chi tiêu", "Tiết kiệm", "Thống kê"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setAllCaps(false);
            b.setTextSize(10);
            b.setTextColor(MUTED);
            b.setGravity(Gravity.CENTER);
            b.setMinHeight(dp(48));
            b.setPadding(dp(2), dp(4), dp(2), dp(4));
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setOnClickListener(v -> showTab(index));
            navButtons[i] = b;
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(50), 1));
        }
        shell.addView(nav);
        setContentView(shell);
    }

    private void showTab(int index) {
        currentTab = index;
        content.removeAllViews();
        View page;
        if (index == 1) page = buildPassesPage();
        else if (index == 2) page = buildExpensesPage();
        else if (index == 3) page = buildSavingsPage();
        else if (index == 4) page = buildStatsPage();
        else page = buildHomePage();
        content.addView(page);
        for (int i = 0; i < navButtons.length; i++) {
            boolean selected = i == index;
            navButtons[i].setTextColor(selected ? ACCENT : MUTED);
            navButtons[i].setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            navButtons[i].setBackground(selected ? rounded(Color.rgb(17, 53, 61), 14) : rounded(Color.TRANSPARENT, 14));
        }
    }

    private void refresh() {
        showTab(currentTab);
    }

    private View buildHomePage() {
        LinearLayout body = pageBody("Tổng quan", "Tài sản, nhịp chi tiêu và việc cần làm hôm nay.");
        int ready = available();
        int saved = savedTotal();
        int owned = ready + saved;
        int pending = pendingTotal();

        LinearLayout hero = heroCard();
        LinearLayout heroTop = row();
        heroTop.addView(eyebrow("TỔNG KIM CƯƠNG"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        heroTop.addView(pill("+" + fmt(pending) + " KC chờ", Color.rgb(52, 49, 24), GOLD));
        hero.addView(heroTop);
        addGap(hero, 7);
        hero.addView(text(fmt(owned) + " KC", 34, TEXT, true));
        hero.addView(text("Dự kiến sau khi nhận hết thẻ: " + fmt(owned + pending) + " KC", 12, MUTED, false));
        addGap(hero, 14);

        LinearLayout stats = row();
        View a = miniStat("KC sẵn", fmt(ready), ACCENT);
        View b = miniStat("Đang tiết kiệm", fmt(saved), GOLD);
        stats.addView(a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(8), 1);
        View spacer = new View(this); spacer.setLayoutParams(sp); stats.addView(spacer);
        stats.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(stats);
        addGap(hero, 12);
        int reservePct = owned == 0 ? 0 : (int) Math.round(saved * 100.0 / owned);
        hero.addView(text("Tỷ lệ KC được khóa cho mục tiêu · " + reservePct + "%", 11, MUTED, false));
        addGap(hero, 6);
        hero.addView(progress(reservePct, GOLD));
        body.addView(hero);

        LinearLayout actions = card();
        actions.addView(eyebrow("THAO TÁC NHANH"));
        addGap(actions, 10);
        LinearLayout r1 = row();
        Button add = primary("+ Thêm KC");
        Button spend = secondary("Ghi chi tiêu");
        r1.addView(add, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View s1 = new View(this); r1.addView(s1, new LinearLayout.LayoutParams(dp(8), 1));
        r1.addView(spend, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(r1);
        addGap(actions, 8);
        Button claim = secondary("Nhận tất cả KC thẻ hôm nay · " + claimableCount() + " thẻ");
        actions.addView(claim);
        add.setOnClickListener(v -> showAddKcDialog());
        spend.setOnClickListener(v -> showExpenseDialog());
        claim.setOnClickListener(v -> claimAllToday());
        body.addView(actions);

        LinearLayout today = card();
        today.addView(eyebrow("NHỊP THÁNG NÀY"));
        addGap(today, 10);
        int spent = spentThisMonth(null);
        int budget = budgetTotal();
        int budgetPct = budget <= 0 ? 0 : Math.min(100, (int) Math.round(spent * 100.0 / budget));
        LinearLayout trow = row();
        trow.addView(miniStat("Đã chi", fmt(spent) + " KC", RED), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View ts = new View(this); trow.addView(ts, new LinearLayout.LayoutParams(dp(8), 1));
        trow.addView(miniStat("Ngân sách", budget > 0 ? fmt(budget) + " KC" : "Chưa đặt", BLUE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        today.addView(trow);
        if (budget > 0) {
            addGap(today, 10);
            today.addView(progress(budgetPct, spent > budget ? RED : BLUE));
            addGap(today, 5);
            today.addView(text(spent > budget ? "Đã vượt ngân sách " + fmt(spent - budget) + " KC." : "Còn " + fmt(budget - spent) + " KC trong ngân sách tháng.", 12, spent > budget ? RED : MUTED, false));
        } else {
            addGap(today, 9);
            TextView hint = text("Đặt ngân sách theo danh mục ở tab Chi tiêu để app cảnh báo sớm.", 12, MUTED, false);
            today.addView(hint);
        }
        body.addView(today);

        JSONArray campaigns = arr("campaigns");
        if (campaigns.length() > 0) {
            LinearLayout goals = card();
            goals.addView(eyebrow("CHIẾN DỊCH ĐANG CHẠY"));
            addGap(goals, 8);
            int shown = 0;
            for (int i = campaigns.length() - 1; i >= 0 && shown < 2; i--, shown++) {
                JSONObject c = campaigns.optJSONObject(i);
                if (c == null) continue;
                addCampaignSummary(goals, c, false);
                if (shown == 0 && campaigns.length() > 1) addGap(goals, 10);
            }
            body.addView(goals);
        }

        LinearLayout history = card();
        history.addView(eyebrow("BIẾN ĐỘNG GẦN ĐÂY"));
        addGap(history, 8);
        addHistoryRows(history, 6);
        body.addView(history);
        return wrap(body);
    }

    private View buildPassesPage() {
        LinearLayout body = pageBody("Thẻ thành viên", "Theo dõi từng thẻ và nhận KC mỗi ngày, hoàn toàn offline.");

        LinearLayout forecast = heroCard();
        forecast.addView(eyebrow("KC ĐỊNH KỲ ĐANG CHỜ"));
        addGap(forecast, 7);
        forecast.addView(text(fmt(pendingTotal()) + " KC", 31, GOLD, true));
        forecast.addView(text("Tuần: " + fmt(pendingFor("weekly")) + " · Tháng: " + fmt(pendingFor("monthly")), 12, MUTED, false));
        addGap(forecast, 12);
        Button all = primary("Nhận tất cả hôm nay · " + claimableCount() + " thẻ");
        all.setEnabled(claimableCount() > 0);
        all.setAlpha(claimableCount() > 0 ? 1f : 0.45f);
        all.setOnClickListener(v -> claimAllToday());
        forecast.addView(all);
        body.addView(forecast);

        body.addView(passPlanCard("THẺ TUẦN", "7 ngày", "+100 KC ngay", "50 KC/ngày × 7", "Tổng 450 KC", "weekly"));
        body.addView(passPlanCard("THẺ THÁNG", "30 ngày", "+500 KC ngay", "70 KC/ngày × 30", "Tổng 2.600 KC", "monthly"));

        TextView activeTitle = eyebrow("THẺ CỦA BẠN");
        activeTitle.setPadding(dp(2), dp(6), 0, dp(8));
        body.addView(activeTitle);
        JSONArray passes = arr("passes");
        if (passes.length() == 0) {
            body.addView(emptyCard("Chưa có thẻ nào. Thêm thẻ phía trên để bắt đầu theo dõi."));
        } else {
            for (int i = passes.length() - 1; i >= 0; i--) {
                JSONObject p = passes.optJSONObject(i);
                if (p != null) body.addView(activePassCard(p));
            }
        }
        return wrap(body);
    }

    private View passPlanCard(String title, String duration, String upfront, String daily, String total, String type) {
        LinearLayout c = card();
        LinearLayout top = row();
        top.addView(text(title, 18, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(pill(duration, Color.rgb(28, 67, 74), ACCENT));
        c.addView(top);
        addGap(c, 5);
        c.addView(text(upfront + " · " + daily, 13, MUTED, false));
        c.addView(text(total, 15, GOLD, true));
        addGap(c, 10);
        Button add = primary("+ Kích hoạt " + title.toLowerCase(new Locale("vi", "VN")));
        add.setOnClickListener(v -> activatePass(type));
        c.addView(add);
        return c;
    }

    private View activePassCard(JSONObject p) {
        LinearLayout c = card();
        boolean weekly = "weekly".equals(p.optString("type"));
        int days = p.optInt("days", weekly ? 7 : 30);
        int daily = p.optInt("daily", weekly ? 50 : 70);
        long start = p.optLong("start");
        long end = start + days * DAY_MS;
        boolean active = System.currentTimeMillis() < end;
        JSONArray claimed = p.optJSONArray("claimed");
        int count = claimed == null ? 0 : claimed.length();
        int pct = Math.min(100, (int) Math.round(count * 100.0 / Math.max(1, days)));

        LinearLayout top = row();
        top.addView(text(weekly ? "Thẻ tuần" : "Thẻ tháng", 17, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(pill(active ? "ĐANG CHẠY" : "HẾT HẠN", active ? Color.rgb(20, 75, 69) : Color.rgb(64, 53, 57), active ? ACCENT : MUTED));
        c.addView(top);
        c.addView(text("Nhận " + count + "/" + days + " ngày · " + daily + " KC/ngày", 12, MUTED, false));
        c.addView(text("Kết thúc: " + date(end), 12, MUTED, false));
        addGap(c, 8);
        c.addView(progress(pct, weekly ? ACCENT : GOLD));
        addGap(c, 10);

        String id = p.optString("id");
        Button claim = primary("Nhận " + daily + " KC hôm nay");
        boolean can = canClaim(p);
        claim.setEnabled(can);
        claim.setAlpha(can ? 1f : 0.45f);
        if (!active) claim.setText("Thẻ đã hết hạn");
        else if (contains(claimed, dateKey(System.currentTimeMillis()))) claim.setText("Hôm nay đã nhận");
        claim.setOnClickListener(v -> claimPass(id));
        c.addView(claim);
        if (!active) {
            addGap(c, 7);
            Button hide = secondary("Xóa thẻ đã hết hạn");
            hide.setOnClickListener(v -> removePass(id));
            c.addView(hide);
        }
        return c;
    }

    private View buildExpensesPage() {
        LinearLayout body = pageBody("Chi tiêu", "Kiểm soát KC theo ngân sách và danh mục, thay vì nhớ bằng niềm tin.");
        int spent = spentThisMonth(null);
        int budget = budgetTotal();

        LinearLayout hero = heroCard();
        hero.addView(eyebrow("THÁNG NÀY"));
        addGap(hero, 7);
        hero.addView(text(fmt(spent) + " KC đã chi", 29, RED, true));
        hero.addView(text(budget > 0 ? "Ngân sách tổng: " + fmt(budget) + " KC" : "Chưa đặt ngân sách", 12, MUTED, false));
        addGap(hero, 11);
        if (budget > 0) {
            int pct = Math.min(100, (int) Math.round(spent * 100.0 / budget));
            hero.addView(progress(pct, spent > budget ? RED : BLUE));
            addGap(hero, 5);
            hero.addView(text(spent > budget ? "Vượt " + fmt(spent - budget) + " KC" : "Còn " + fmt(budget - spent) + " KC", 12, spent > budget ? RED : ACCENT, true));
        }
        addGap(hero, 11);
        LinearLayout actions = row();
        Button add = primary("+ Ghi chi");
        Button set = secondary("Đặt ngân sách");
        actions.addView(add, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View sp = new View(this); actions.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
        actions.addView(set, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        add.setOnClickListener(v -> showExpenseDialog());
        set.setOnClickListener(v -> showBudgetDialog());
        hero.addView(actions);
        body.addView(hero);

        LinearLayout cats = card();
        cats.addView(eyebrow("NGÂN SÁCH THEO DANH MỤC"));
        addGap(cats, 8);
        for (String cat : CATEGORIES) {
            int s = spentThisMonth(cat);
            int b = budgetFor(cat);
            LinearLayout top = row();
            top.addView(text(cat, 13, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(text(fmt(s) + (b > 0 ? " / " + fmt(b) : " KC"), 12, b > 0 && s > b ? RED : MUTED, true));
            cats.addView(top);
            addGap(cats, 4);
            if (b > 0) cats.addView(progress(Math.min(100, (int) Math.round(s * 100.0 / b)), s > b ? RED : ACCENT));
            else cats.addView(progress(0, ACCENT));
            addGap(cats, 9);
        }
        body.addView(cats);

        TextView recent = eyebrow("GẦN ĐÂY");
        recent.setPadding(dp(2), dp(5), 0, dp(8));
        body.addView(recent);
        JSONArray expenses = arr("expenses");
        if (expenses.length() == 0) body.addView(emptyCard("Chưa có khoản chi nào."));
        else {
            int min = Math.max(0, expenses.length() - 20);
            for (int i = expenses.length() - 1; i >= min; i--) {
                JSONObject e = expenses.optJSONObject(i);
                if (e != null) body.addView(expenseCard(e));
            }
        }
        return wrap(body);
    }

    private View expenseCard(JSONObject e) {
        LinearLayout c = card();
        LinearLayout top = row();
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(e.optString("name", "Chi tiêu"), 15, TEXT, true));
        labels.addView(text(e.optString("category", "Khác") + " · " + dateTime(e.optLong("time")), 11, MUTED, false));
        top.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(text("-" + fmt(e.optInt("amount")) + " KC", 16, RED, true));
        c.addView(top);
        addGap(c, 8);
        Button remove = secondary("Xóa khoản chi & hoàn KC");
        String id = e.optString("id");
        remove.setOnClickListener(v -> confirmRemoveExpense(id));
        c.addView(remove);
        return c;
    }

    private View buildSavingsPage() {
        LinearLayout body = pageBody("Tiết kiệm", "Khóa KC theo chiến dịch và biết mỗi ngày cần để dành bao nhiêu.");
        int saved = savedTotal();
        int target = campaignTargets();
        LinearLayout hero = heroCard();
        hero.addView(eyebrow("KC ĐANG KHÓA"));
        addGap(hero, 7);
        hero.addView(text(fmt(saved) + " KC", 31, GOLD, true));
        hero.addView(text(target > 0 ? "Tổng mục tiêu đang chạy: " + fmt(target) + " KC" : "Chưa có mục tiêu đang chạy", 12, MUTED, false));
        addGap(hero, 11);
        Button add = primary("+ Tạo chiến dịch mới");
        add.setOnClickListener(v -> showCampaignDialog());
        hero.addView(add);
        body.addView(hero);

        JSONArray campaigns = arr("campaigns");
        if (campaigns.length() == 0) {
            body.addView(emptyCard("Tạo chiến dịch như “Evo Gun”, “Vòng quay tháng 10” hoặc bất kỳ cái hố KC nào bạn đang nhắm tới."));
        } else {
            for (int i = campaigns.length() - 1; i >= 0; i--) {
                JSONObject c = campaigns.optJSONObject(i);
                if (c != null) body.addView(campaignCard(c));
            }
        }
        return wrap(body);
    }

    private View campaignCard(JSONObject c) {
        LinearLayout card = card();
        addCampaignSummary(card, c, true);
        addGap(card, 11);
        LinearLayout r = row();
        Button save = primary("+ Gửi KC");
        Button withdraw = secondary("Rút KC");
        r.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View sp = new View(this); r.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
        r.addView(withdraw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(r);
        addGap(card, 7);
        Button close = secondary("Đóng chiến dịch");
        String id = c.optString("id");
        save.setOnClickListener(v -> showSaveDialog(id));
        withdraw.setOnClickListener(v -> showWithdrawDialog(id));
        close.setOnClickListener(v -> confirmCloseCampaign(id));
        card.addView(close);
        return card;
    }

    private void addCampaignSummary(LinearLayout parent, JSONObject c, boolean detailed) {
        int target = Math.max(1, c.optInt("target", 1));
        int saved = Math.max(0, c.optInt("saved", 0));
        int pct = Math.min(100, (int) Math.round(saved * 100.0 / target));
        LinearLayout top = row();
        top.addView(text(c.optString("name", "Chiến dịch"), 16, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(pill(pct + "%", Color.rgb(52, 49, 24), GOLD));
        parent.addView(top);
        parent.addView(text(fmt(saved) + " / " + fmt(target) + " KC", 13, GOLD, true));
        addGap(parent, 7);
        parent.addView(progress(pct, GOLD));
        if (detailed) {
            addGap(parent, 7);
            long deadline = c.optLong("deadline", 0);
            int remaining = Math.max(0, target - saved);
            if (deadline > 0) {
                int daysLeft = Math.max(0, (int) Math.ceil((deadline - System.currentTimeMillis()) / (double) DAY_MS));
                int dailyNeed = daysLeft <= 0 ? remaining : (int) Math.ceil(remaining / (double) daysLeft);
                String line = daysLeft <= 0 ? "Đã tới hạn · còn thiếu " + fmt(remaining) + " KC" : "Còn " + daysLeft + " ngày · nên giữ khoảng " + fmt(dailyNeed) + " KC/ngày";
                parent.addView(text(line, 12, daysLeft <= 0 && remaining > 0 ? RED : MUTED, false));
            } else {
                parent.addView(text("Không đặt ngày hoàn thành · còn thiếu " + fmt(remaining) + " KC", 12, MUTED, false));
            }
        }
    }

    private View buildStatsPage() {
        LinearLayout body = pageBody("Thống kê", "Một ít số liệu để KC không biến mất trong làn sương sự kiện.");
        int owned = available() + savedTotal();
        int spent = spentThisMonth(null);
        int income = incomeThisMonth();

        LinearLayout hero = heroCard();
        hero.addView(eyebrow("BỨC TRANH THÁNG NÀY"));
        addGap(hero, 10);
        LinearLayout r1 = row();
        r1.addView(miniStat("KC vào", "+" + fmt(income), ACCENT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View a = new View(this); r1.addView(a, new LinearLayout.LayoutParams(dp(8), 1));
        r1.addView(miniStat("KC đã chi", "-" + fmt(spent), RED), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(r1);
        addGap(hero, 8);
        LinearLayout r2 = row();
        r2.addView(miniStat("Đang sở hữu", fmt(owned), TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View b = new View(this); r2.addView(b, new LinearLayout.LayoutParams(dp(8), 1));
        r2.addView(miniStat("Sau khi nhận thẻ", fmt(owned + pendingTotal()), GOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(r2);
        body.addView(hero);

        LinearLayout chartCard = card();
        chartCard.addView(eyebrow("CHI TIÊU 7 NGÀY GẦN NHẤT"));
        addGap(chartCard, 6);
        ExpenseChartView chart = new ExpenseChartView(this);
        chart.setData(last7ExpenseValues(), last7Labels());
        chartCard.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
        body.addView(chartCard);

        LinearLayout cats = card();
        cats.addView(eyebrow("PHÂN BỔ CHI TIÊU THÁNG"));
        addGap(cats, 8);
        int maxCat = 1;
        for (String cat : CATEGORIES) maxCat = Math.max(maxCat, spentThisMonth(cat));
        for (String cat : CATEGORIES) {
            int v = spentThisMonth(cat);
            LinearLayout line = row();
            line.addView(text(cat, 12, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            line.addView(text(fmt(v) + " KC", 12, MUTED, true));
            cats.addView(line);
            addGap(cats, 4);
            cats.addView(progress((int) Math.round(v * 100.0 / maxCat), BLUE));
            addGap(cats, 8);
        }
        body.addView(cats);

        LinearLayout data = card();
        data.addView(eyebrow("DỮ LIỆU & AN TOÀN"));
        addGap(data, 8);
        data.addView(text("Tất cả dữ liệu nằm trong máy. Bạn có thể xuất JSON để sao lưu rồi nhập lại khi đổi điện thoại.", 12, MUTED, false));
        addGap(data, 10);
        LinearLayout dr = row();
        Button export = secondary("Xuất backup");
        Button restore = secondary("Nhập backup");
        dr.addView(export, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View ds = new View(this); dr.addView(ds, new LinearLayout.LayoutParams(dp(8), 1));
        dr.addView(restore, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        data.addView(dr);
        addGap(data, 8);
        Button reset = danger("Đặt lại toàn bộ dữ liệu");
        data.addView(reset);
        export.setOnClickListener(v -> exportData());
        restore.setOnClickListener(v -> importData());
        reset.setOnClickListener(v -> confirmReset());
        body.addView(data);
        return wrap(body);
    }

    private View emptyCard(String message) {
        LinearLayout c = card();
        c.addView(text(message, 13, MUTED, false));
        return c;
    }

    private String fmt(int n) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(n);
    }

    private String dateKey(long when) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(when));
    }

    private String monthKey(long when) {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date(when));
    }

    private String date(long when) {
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(when));
    }

    private String dateTime(long when) {
        return new SimpleDateFormat("dd/MM HH:mm", new Locale("vi", "VN")).format(new Date(when));
    }

    private int savedTotal() {
        int total = 0;
        JSONArray a = arr("campaigns");
        for (int i = 0; i < a.length(); i++) {
            JSONObject c = a.optJSONObject(i);
            if (c != null) total += Math.max(0, c.optInt("saved", 0));
        }
        return total;
    }

    private int campaignTargets() {
        int total = 0;
        JSONArray a = arr("campaigns");
        for (int i = 0; i < a.length(); i++) {
            JSONObject c = a.optJSONObject(i);
            if (c != null) total += Math.max(0, c.optInt("target", 0));
        }
        return total;
    }

    private int pendingTotal() {
        return pendingFor("weekly") + pendingFor("monthly");
    }

    private int pendingFor(String type) {
        int total = 0;
        long now = System.currentTimeMillis();
        JSONArray a = arr("passes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p == null || !type.equals(p.optString("type"))) continue;
            int days = p.optInt("days");
            long end = p.optLong("start") + days * DAY_MS;
            if (now >= end) continue;
            JSONArray claimed = p.optJSONArray("claimed");
            int count = claimed == null ? 0 : claimed.length();
            total += Math.max(0, days - count) * p.optInt("daily");
        }
        return total;
    }

    private boolean contains(JSONArray a, String value) {
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) if (value.equals(a.optString(i))) return true;
        return false;
    }

    private boolean canClaim(JSONObject p) {
        int days = p.optInt("days");
        long now = System.currentTimeMillis();
        if (now >= p.optLong("start") + days * DAY_MS) return false;
        JSONArray claimed = p.optJSONArray("claimed");
        if (claimed != null && claimed.length() >= days) return false;
        return !contains(claimed, dateKey(now));
    }

    private int claimableCount() {
        int count = 0;
        JSONArray a = arr("passes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p != null && canClaim(p)) count++;
        }
        return count;
    }

    private int spentThisMonth(String category) {
        int total = 0;
        String month = monthKey(System.currentTimeMillis());
        JSONArray a = arr("expenses");
        for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.optJSONObject(i);
            if (e == null || !month.equals(monthKey(e.optLong("time")))) continue;
            if (category == null || category.equals(e.optString("category"))) total += Math.max(0, e.optInt("amount"));
        }
        return total;
    }

    private int incomeThisMonth() {
        int total = 0;
        String month = monthKey(System.currentTimeMillis());
        JSONArray a = arr("history");
        for (int i = 0; i < a.length(); i++) {
            JSONObject h = a.optJSONObject(i);
            if (h == null || !month.equals(monthKey(h.optLong("time")))) continue;
            int d = h.optInt("delta");
            if (d > 0) total += d;
        }
        return total;
    }

    private int budgetFor(String category) {
        return Math.max(0, budgets().optInt(category, 0));
    }

    private int budgetTotal() {
        int total = 0;
        for (String c : CATEGORIES) total += budgetFor(c);
        return total;
    }

    private int[] last7ExpenseValues() {
        int[] out = new int[7];
        JSONArray a = arr("expenses");
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_YEAR, -6);
        long startMs = start.getTimeInMillis();
        for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.optJSONObject(i);
            if (e == null) continue;
            long t = e.optLong("time");
            if (t < startMs) continue;
            int idx = (int) ((t - startMs) / DAY_MS);
            if (idx >= 0 && idx < 7) out[idx] += Math.max(0, e.optInt("amount"));
        }
        return out;
    }

    private String[] last7Labels() {
        String[] labels = new String[7];
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat f = new SimpleDateFormat("dd", Locale.US);
        for (int i = 0; i < 7; i++) {
            labels[i] = f.format(c.getTime());
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return labels;
    }

    private void activatePass(String type) {
        boolean weekly = "weekly".equals(type);
        int upfront = weekly ? 100 : 500;
        JSONObject p = new JSONObject();
        try {
            p.put("id", UUID.randomUUID().toString());
            p.put("type", type);
            p.put("start", System.currentTimeMillis());
            p.put("days", weekly ? 7 : 30);
            p.put("daily", weekly ? 50 : 70);
            p.put("claimed", new JSONArray());
            arr("passes").put(p);
            setAvailable(available() + upfront);
            addHistory(weekly ? "Kích hoạt thẻ tuần" : "Kích hoạt thẻ tháng", upfront);
            saveState();
            refresh();
            toast("Đã nhận ngay +" + upfront + " KC");
        } catch (Exception e) {
            toast("Không thể thêm thẻ");
        }
    }

    private void claimPass(String id) {
        JSONArray a = arr("passes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p == null || !id.equals(p.optString("id"))) continue;
            if (!canClaim(p)) { toast("Hôm nay chưa thể nhận thêm"); return; }
            JSONArray claimed = p.optJSONArray("claimed");
            if (claimed == null) claimed = new JSONArray();
            claimed.put(dateKey(System.currentTimeMillis()));
            try { p.put("claimed", claimed); } catch (Exception ignored) {}
            int daily = p.optInt("daily");
            setAvailable(available() + daily);
            addHistory("weekly".equals(p.optString("type")) ? "Nhận KC thẻ tuần" : "Nhận KC thẻ tháng", daily);
            saveState();
            refresh();
            toast("+" + daily + " KC");
            return;
        }
    }

    private void claimAllToday() {
        JSONArray a = arr("passes");
        int total = 0;
        int count = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p == null || !canClaim(p)) continue;
            JSONArray claimed = p.optJSONArray("claimed");
            if (claimed == null) claimed = new JSONArray();
            claimed.put(dateKey(System.currentTimeMillis()));
            try { p.put("claimed", claimed); } catch (Exception ignored) {}
            total += p.optInt("daily");
            count++;
        }
        if (total <= 0) { toast("Hôm nay không còn KC thẻ để nhận"); return; }
        setAvailable(available() + total);
        addHistory("Nhận tất cả KC thẻ · " + count + " thẻ", total);
        saveState();
        refresh();
        toast("+" + total + " KC từ " + count + " thẻ");
    }

    private void removePass(String id) {
        JSONArray a = arr("passes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p != null && id.equals(p.optString("id"))) {
                a.remove(i);
                saveState();
                refresh();
                return;
            }
        }
    }

    private LinearLayout dialogForm() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(22), dp(6), dp(22), 0);
        return l;
    }

    private EditText input(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setPadding(dp(5), dp(12), dp(5), dp(12));
        return e;
    }

    private int parsePositive(String raw) {
        try {
            long n = Long.parseLong(raw.trim());
            return n > 0 && n <= Integer.MAX_VALUE ? (int) n : -1;
        } catch (Exception e) { return -1; }
    }

    private int parseNonNegative(String raw) {
        try {
            long n = Long.parseLong(raw.trim());
            return n >= 0 && n <= Integer.MAX_VALUE ? (int) n : -1;
        } catch (Exception e) { return -1; }
    }

    private void showAddKcDialog() {
        LinearLayout form = dialogForm();
        EditText amount = input("Số KC", true);
        EditText note = input("Nguồn / ghi chú", false);
        form.addView(amount); form.addView(note);
        new AlertDialog.Builder(this)
                .setTitle("Thêm KC sẵn")
                .setView(form)
                .setPositiveButton("Thêm", (d, w) -> {
                    int n = parsePositive(amount.getText().toString());
                    if (n <= 0) { toast("Số KC không hợp lệ"); return; }
                    setAvailable(available() + n);
                    String label = note.getText().toString().trim();
                    addHistory(label.isEmpty() ? "Thêm KC sẵn" : label, n);
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showExpenseDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("Nội dung chi tiêu", false);
        EditText amount = input("Số KC", true);
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        form.addView(name); form.addView(amount); form.addView(category);
        new AlertDialog.Builder(this)
                .setTitle("Ghi chi tiêu")
                .setView(form)
                .setPositiveButton("Lưu", (d, w) -> {
                    String title = name.getText().toString().trim();
                    int n = parsePositive(amount.getText().toString());
                    if (title.isEmpty() || n <= 0) { toast("Nhập đủ nội dung và số KC"); return; }
                    if (n > available()) { toast("KC sẵn không đủ"); return; }
                    JSONObject e = new JSONObject();
                    try {
                        e.put("id", UUID.randomUUID().toString());
                        e.put("name", title);
                        e.put("amount", n);
                        e.put("category", String.valueOf(category.getSelectedItem()));
                        e.put("time", System.currentTimeMillis());
                        arr("expenses").put(e);
                    } catch (Exception ignored) {}
                    setAvailable(available() - n);
                    addHistory("Chi: " + title, -n);
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showBudgetDialog() {
        LinearLayout form = dialogForm();
        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        EditText amount = input("Ngân sách tháng (0 = tắt)", true);
        form.addView(category); form.addView(amount);
        new AlertDialog.Builder(this)
                .setTitle("Đặt ngân sách danh mục")
                .setView(form)
                .setPositiveButton("Lưu", (d, w) -> {
                    int n = parseNonNegative(amount.getText().toString());
                    if (n < 0) { toast("Ngân sách không hợp lệ"); return; }
                    String cat = String.valueOf(category.getSelectedItem());
                    try { budgets().put(cat, n); } catch (Exception ignored) {}
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void confirmRemoveExpense(String id) {
        JSONObject found = null;
        JSONArray a = arr("expenses");
        for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.optJSONObject(i);
            if (e != null && id.equals(e.optString("id"))) { found = e; break; }
        }
        if (found == null) return;
        final int amount = found.optInt("amount");
        new AlertDialog.Builder(this)
                .setTitle("Xóa khoản chi?")
                .setMessage("Hoàn " + fmt(amount) + " KC về số KC sẵn.")
                .setPositiveButton("Xóa & hoàn", (d, w) -> removeExpense(id))
                .setNegativeButton("Hủy", null).show();
    }

    private void removeExpense(String id) {
        JSONArray a = arr("expenses");
        for (int i = 0; i < a.length(); i++) {
            JSONObject e = a.optJSONObject(i);
            if (e == null || !id.equals(e.optString("id"))) continue;
            int amount = Math.max(0, e.optInt("amount"));
            setAvailable(available() + amount);
            addHistory("Hoàn tác chi: " + e.optString("name"), amount);
            a.remove(i);
            saveState(); refresh();
            return;
        }
    }

    private void showCampaignDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("Tên chiến dịch", false);
        EditText target = input("Mục tiêu KC", true);
        EditText days = input("Số ngày dự kiến (0 = không hạn)", true);
        form.addView(name); form.addView(target); form.addView(days);
        new AlertDialog.Builder(this)
                .setTitle("Tạo chiến dịch tiết kiệm")
                .setView(form)
                .setPositiveButton("Tạo", (d, w) -> {
                    String title = name.getText().toString().trim();
                    int t = parsePositive(target.getText().toString());
                    int dayCount = days.getText().toString().trim().isEmpty() ? 0 : parseNonNegative(days.getText().toString());
                    if (title.isEmpty() || t <= 0 || dayCount < 0) { toast("Thông tin chưa hợp lệ"); return; }
                    JSONObject c = new JSONObject();
                    long now = System.currentTimeMillis();
                    try {
                        c.put("id", UUID.randomUUID().toString());
                        c.put("name", title);
                        c.put("target", t);
                        c.put("saved", 0);
                        c.put("created", now);
                        c.put("deadline", dayCount > 0 ? now + dayCount * DAY_MS : 0);
                        arr("campaigns").put(c);
                    } catch (Exception ignored) {}
                    addHistory("Tạo chiến dịch: " + title, 0);
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private JSONObject findCampaign(String id) {
        JSONArray a = arr("campaigns");
        for (int i = 0; i < a.length(); i++) {
            JSONObject c = a.optJSONObject(i);
            if (c != null && id.equals(c.optString("id"))) return c;
        }
        return null;
    }

    private void showSaveDialog(String id) {
        JSONObject c = findCampaign(id);
        if (c == null) return;
        LinearLayout form = dialogForm();
        EditText amount = input("Số KC muốn gửi", true);
        form.addView(amount);
        new AlertDialog.Builder(this)
                .setTitle(c.optString("name"))
                .setMessage("KC sẵn: " + fmt(available()))
                .setView(form)
                .setPositiveButton("Gửi vào quỹ", (d, w) -> {
                    int n = parsePositive(amount.getText().toString());
                    if (n <= 0) { toast("Số KC không hợp lệ"); return; }
                    if (n > available()) { toast("KC sẵn không đủ"); return; }
                    try { c.put("saved", c.optInt("saved") + n); } catch (Exception ignored) {}
                    setAvailable(available() - n);
                    addHistory("Tiết kiệm: " + c.optString("name"), 0);
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void showWithdrawDialog(String id) {
        JSONObject c = findCampaign(id);
        if (c == null) return;
        LinearLayout form = dialogForm();
        EditText amount = input("Số KC muốn rút", true);
        form.addView(amount);
        new AlertDialog.Builder(this)
                .setTitle("Rút từ " + c.optString("name"))
                .setMessage("Trong quỹ: " + fmt(c.optInt("saved")) + " KC")
                .setView(form)
                .setPositiveButton("Rút", (d, w) -> {
                    int n = parsePositive(amount.getText().toString());
                    int saved = Math.max(0, c.optInt("saved"));
                    if (n <= 0 || n > saved) { toast("Số KC muốn rút không hợp lệ"); return; }
                    try { c.put("saved", saved - n); } catch (Exception ignored) {}
                    setAvailable(available() + n);
                    addHistory("Rút quỹ: " + c.optString("name"), 0);
                    saveState(); refresh();
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void confirmCloseCampaign(String id) {
        JSONObject c = findCampaign(id);
        if (c == null) return;
        int saved = Math.max(0, c.optInt("saved"));
        new AlertDialog.Builder(this)
                .setTitle("Đóng chiến dịch?")
                .setMessage("Hoàn " + fmt(saved) + " KC về số KC sẵn và xóa mục tiêu này.")
                .setPositiveButton("Đóng & hoàn KC", (d, w) -> closeCampaign(id))
                .setNegativeButton("Hủy", null).show();
    }

    private void closeCampaign(String id) {
        JSONArray a = arr("campaigns");
        for (int i = 0; i < a.length(); i++) {
            JSONObject c = a.optJSONObject(i);
            if (c == null || !id.equals(c.optString("id"))) continue;
            int saved = Math.max(0, c.optInt("saved"));
            setAvailable(available() + saved);
            addHistory("Đóng chiến dịch: " + c.optString("name"), 0);
            a.remove(i);
            saveState(); refresh();
            toast("Đã hoàn " + fmt(saved) + " KC");
            return;
        }
    }

    private void addHistory(String label, int delta) {
        JSONObject h = new JSONObject();
        try {
            h.put("label", label);
            h.put("delta", delta);
            h.put("time", System.currentTimeMillis());
            arr("history").put(h);
        } catch (Exception ignored) {}
    }

    private void addHistoryRows(LinearLayout parent, int limit) {
        JSONArray a = arr("history");
        if (a.length() == 0) {
            parent.addView(text("Chưa có biến động.", 12, MUTED, false));
            return;
        }
        int min = Math.max(0, a.length() - limit);
        for (int i = a.length() - 1; i >= min; i--) {
            JSONObject h = a.optJSONObject(i);
            if (h == null) continue;
            LinearLayout line = row();
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(h.optString("label"), 12, TEXT, true));
            left.addView(text(dateTime(h.optLong("time")), 10, MUTED, false));
            line.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            int d = h.optInt("delta");
            if (d != 0) line.addView(text((d > 0 ? "+" : "") + fmt(d), 13, d > 0 ? ACCENT : RED, true));
            parent.addView(line);
            if (i > min) addGap(parent, 10);
        }
    }

    private void exportData() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "kcff-backup-" + dateKey(System.currentTimeMillis()) + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    private void importData() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQ_EXPORT) {
                OutputStream out = getContentResolver().openOutputStream(data.getData());
                if (out != null) {
                    out.write(state.toString(2).getBytes("UTF-8"));
                    out.close();
                    toast("Đã xuất backup");
                }
            } else if (requestCode == REQ_IMPORT) {
                InputStream in = getContentResolver().openInputStream(data.getData());
                if (in == null) return;
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                JSONObject restored = new JSONObject(sb.toString());
                if (!restored.has("available")) throw new Exception("invalid");
                state = restored;
                migrateState();
                saveState();
                refresh();
                toast("Đã khôi phục dữ liệu");
            }
        } catch (Exception e) {
            toast("Không thể xử lý file backup");
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Đặt lại dữ liệu?")
                .setMessage("Xóa toàn bộ KC, thẻ, chi tiêu, ngân sách, chiến dịch và lịch sử trên máy.")
                .setPositiveButton("Xóa toàn bộ", (d, w) -> {
                    state = freshState();
                    saveState();
                    refresh();
                    toast("Đã đặt lại dữ liệu");
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
