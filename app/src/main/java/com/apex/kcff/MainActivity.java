package com.apex.kcff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS = "kcff_store";
    private static final String KEY_STATE = "state";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private final int BG = Color.rgb(7, 17, 31);
    private final int PANEL = Color.rgb(15, 29, 48);
    private final int PANEL_2 = Color.rgb(20, 38, 62);
    private final int TEXT = Color.rgb(240, 247, 255);
    private final int MUTED = Color.rgb(154, 174, 199);
    private final int ACCENT = Color.rgb(79, 209, 197);
    private final int GOLD = Color.rgb(255, 196, 87);
    private final int RED = Color.rgb(255, 108, 122);

    private SharedPreferences prefs;
    private JSONObject state;
    private LinearLayout root;
    private TextView totalView;
    private TextView availableView;
    private TextView weeklyView;
    private TextView monthlyView;
    private LinearLayout passesBox;
    private LinearLayout campaignsBox;
    private LinearLayout expensesBox;
    private LinearLayout historyBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadState();
        buildUi();
        render();
    }

    private void loadState() {
        String raw = prefs.getString(KEY_STATE, "");
        try {
            state = raw.isEmpty() ? freshState() : new JSONObject(raw);
        } catch (Exception e) {
            state = freshState();
        }
        ensureArrays();
    }

    private JSONObject freshState() {
        JSONObject s = new JSONObject();
        try {
            s.put("available", 0);
            s.put("passes", new JSONArray());
            s.put("campaigns", new JSONArray());
            s.put("expenses", new JSONArray());
            s.put("history", new JSONArray());
        } catch (Exception ignored) {}
        return s;
    }

    private void ensureArrays() {
        try {
            if (!state.has("available")) state.put("available", 0);
            if (!state.has("passes")) state.put("passes", new JSONArray());
            if (!state.has("campaigns")) state.put("campaigns", new JSONArray());
            if (!state.has("expenses")) state.put("expenses", new JSONArray());
            if (!state.has("history")) state.put("history", new JSONArray());
        } catch (Exception ignored) {}
    }

    private void saveState() {
        prefs.edit().putString(KEY_STATE, state.toString()).apply();
    }

    private JSONArray arr(String key) {
        JSONArray a = state.optJSONArray(key);
        return a == null ? new JSONArray() : a;
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

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(12));
        l.setLayoutParams(p);
        return l;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.BLACK);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundColor(ACCENT);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        return b;
    }

    private Button smallButton(String label) {
        Button b = button(label);
        b.setTextSize(13);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(6), dp(4), 0, dp(4));
        b.setLayoutParams(p);
        return b;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(Color.rgb(43, 62, 84));
        d.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return d;
    }

    private void addGap(LinearLayout parent, int h) {
        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        parent.addView(gap);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(text("KCFF", 32, TEXT, true));
        root.addView(text("Quản lý kim cương Free Fire • OFFLINE", 14, MUTED, false));
        addGap(root, 16);

        LinearLayout summary = box();
        summary.setBackgroundColor(PANEL_2);
        summary.addView(text("TỔNG QUAN", 12, ACCENT, true));
        addGap(summary, 8);
        totalView = text("0 KC", 30, TEXT, true);
        summary.addView(totalView);
        summary.addView(text("Kim cương tổng", 13, MUTED, false));
        addGap(summary, 12);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        availableView = text("Sẵn: 0", 15, TEXT, true);
        weeklyView = text("Tuần: 0", 15, TEXT, true);
        monthlyView = text("Tháng: 0", 15, TEXT, true);
        row.addView(availableView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(weeklyView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(monthlyView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        summary.addView(row);
        addGap(summary, 12);

        Button addKc = button("+ Thêm KC sẵn");
        addKc.setOnClickListener(v -> showAddKcDialog());
        summary.addView(addKc);
        root.addView(summary);

        root.addView(sectionTitle("THẺ THÀNH VIÊN", "Thưởng định kỳ"));
        LinearLayout passActions = box();
        passActions.addView(text("Thẻ tuần • 7 ngày\n+100 KC ngay, sau đó 50 KC/ngày × 7", 15, TEXT, true));
        addGap(passActions, 8);
        Button weeklyBtn = button("+ Thêm thẻ tuần");
        weeklyBtn.setOnClickListener(v -> activatePass("weekly"));
        passActions.addView(weeklyBtn);
        addGap(passActions, 14);
        passActions.addView(divider());
        addGap(passActions, 14);
        passActions.addView(text("Thẻ tháng • 30 ngày\n+500 KC ngay, sau đó 70 KC/ngày × 30 = 2.600 KC", 15, TEXT, true));
        addGap(passActions, 8);
        Button monthlyBtn = button("+ Thêm thẻ tháng");
        monthlyBtn.setOnClickListener(v -> activatePass("monthly"));
        passActions.addView(monthlyBtn);
        root.addView(passActions);

        passesBox = new LinearLayout(this);
        passesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(passesBox);

        root.addView(sectionTitle("CHI TIÊU", "Ghi lại KC đã dùng"));
        LinearLayout expenseActions = box();
        Button addExpense = button("+ Ghi chi tiêu");
        addExpense.setOnClickListener(v -> showExpenseDialog());
        expenseActions.addView(addExpense);
        root.addView(expenseActions);
        expensesBox = new LinearLayout(this);
        expensesBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(expensesBox);

        root.addView(sectionTitle("CHIẾN DỊCH TIẾT KIỆM", "Giữ KC cho mục tiêu"));
        LinearLayout campaignActions = box();
        Button addCampaign = button("+ Tạo chiến dịch");
        addCampaign.setOnClickListener(v -> showCampaignDialog());
        campaignActions.addView(addCampaign);
        root.addView(campaignActions);
        campaignsBox = new LinearLayout(this);
        campaignsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(campaignsBox);

        root.addView(sectionTitle("LỊCH SỬ", "Biến động gần đây"));
        historyBox = new LinearLayout(this);
        historyBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyBox);

        LinearLayout resetBox = box();
        Button reset = button("Đặt lại toàn bộ dữ liệu");
        reset.setBackgroundColor(RED);
        reset.setTextColor(Color.WHITE);
        reset.setOnClickListener(v -> confirmReset());
        resetBox.addView(reset);
        root.addView(resetBox);

        TextView foot = text("Dữ liệu chỉ nằm trên thiết bị này. App không có quyền Internet và không liên kết Garena/Free Fire.", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot);
    }

    private View sectionTitle(String eyebrow, String title) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(2), dp(12), dp(2), dp(8));
        l.addView(text(eyebrow, 11, ACCENT, true));
        l.addView(text(title, 20, TEXT, true));
        return l;
    }

    private String fmt(int n) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(n);
    }

    private String dateKey(long when) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(when));
    }

    private String dateTime(long when) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date(when));
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

    private int pendingFor(String type) {
        int total = 0;
        long now = System.currentTimeMillis();
        JSONArray a = arr("passes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p == null || !type.equals(p.optString("type"))) continue;
            int days = p.optInt("days");
            long start = p.optLong("start");
            if (now >= start + days * DAY_MS) continue;
            JSONArray claimed = p.optJSONArray("claimed");
            int count = claimed == null ? 0 : claimed.length();
            total += Math.max(0, days - count) * p.optInt("daily");
        }
        return total;
    }

    private void render() {
        int ready = available();
        int saved = savedTotal();
        totalView.setText(fmt(ready + saved) + " KC");
        availableView.setText("Sẵn: " + fmt(ready));
        weeklyView.setText("Tuần: " + fmt(pendingFor("weekly")));
        monthlyView.setText("Tháng: " + fmt(pendingFor("monthly")));
        renderPasses();
        renderExpenses();
        renderCampaigns();
        renderHistory();
    }

    private void activatePass(String type) {
        boolean weekly = "weekly".equals(type);
        int upfront = weekly ? 100 : 500;
        int daily = weekly ? 50 : 70;
        int days = weekly ? 7 : 30;
        JSONObject p = new JSONObject();
        try {
            p.put("id", UUID.randomUUID().toString());
            p.put("type", type);
            p.put("start", System.currentTimeMillis());
            p.put("days", days);
            p.put("daily", daily);
            p.put("claimed", new JSONArray());
            arr("passes").put(p);
            setAvailable(available() + upfront);
            addHistory((weekly ? "Thêm thẻ tuần" : "Thêm thẻ tháng") + " • thưởng ngay", upfront);
            saveState();
            render();
            toast("Đã cộng " + upfront + " KC");
        } catch (Exception e) {
            toast("Không thể thêm thẻ");
        }
    }

    private void renderPasses() {
        passesBox.removeAllViews();
        JSONArray a = arr("passes");
        if (a.length() == 0) {
            passesBox.addView(emptyBox("Chưa có thẻ nào."));
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject p = a.optJSONObject(i);
            if (p == null) continue;
            LinearLayout card = box();
            boolean weekly = "weekly".equals(p.optString("type"));
            int days = p.optInt("days");
            int daily = p.optInt("daily");
            long start = p.optLong("start");
            long end = start + days * DAY_MS;
            JSONArray claimed = p.optJSONArray("claimed");
            int claimedCount = claimed == null ? 0 : claimed.length();
            boolean active = now < end;
            int daysLeft = active ? Math.max(1, (int) Math.ceil((end - now) / (double) DAY_MS)) : 0;

            card.addView(text((weekly ? "THẺ TUẦN" : "THẺ THÁNG") + (active ? " • đang hoạt động" : " • đã hết hạn"), 14, active ? ACCENT : MUTED, true));
            card.addView(text("Đã nhận " + claimedCount + "/" + days + " ngày • " + daily + " KC/ngày", 14, TEXT, false));
            card.addView(text(active ? "Còn khoảng " + daysLeft + " ngày" : "Không còn thưởng chờ nhận", 12, MUTED, false));
            addGap(card, 8);

            Button claim = button("Nhận " + daily + " KC hôm nay");
            String id = p.optString("id");
            boolean claimedToday = contains(claimed, dateKey(now));
            claim.setEnabled(active && !claimedToday && claimedCount < days);
            claim.setAlpha(claim.isEnabled() ? 1f : 0.45f);
            if (claimedToday) claim.setText("Hôm nay đã nhận");
            if (!active) claim.setText("Thẻ đã hết hạn");
            claim.setOnClickListener(v -> claimPass(id));
            card.addView(claim);
            passesBox.addView(card);
        }
    }

    private boolean contains(JSONArray a, String value) {
        if (a == null) return false;
        for (int i = 0; i < a.length(); i++) {
            if (value.equals(a.optString(i))) return true;
        }
        return false;
    }

    private void claimPass(String id) {
        JSONArray a = arr("passes");
        long now = System.currentTimeMillis();
        for (int i = 0; i < a.length(); i++) {
            JSONObject p = a.optJSONObject(i);
            if (p == null || !id.equals(p.optString("id"))) continue;
            int days = p.optInt("days");
            long start = p.optLong("start");
            JSONArray claimed = p.optJSONArray("claimed");
            if (claimed == null) claimed = new JSONArray();
            if (now >= start + days * DAY_MS || claimed.length() >= days) {
                toast("Thẻ đã hết hạn");
                return;
            }
            String today = dateKey(now);
            if (contains(claimed, today)) {
                toast("Hôm nay đã nhận rồi");
                return;
            }
            int daily = p.optInt("daily");
            claimed.put(today);
            try { p.put("claimed", claimed); } catch (Exception ignored) {}
            setAvailable(available() + daily);
            addHistory(("weekly".equals(p.optString("type")) ? "Nhận thẻ tuần" : "Nhận thẻ tháng"), daily);
            saveState();
            render();
            toast("+" + daily + " KC");
            return;
        }
    }

    private void showAddKcDialog() {
        LinearLayout form = dialogForm();
        EditText amount = input("Số KC", true);
        EditText note = input("Ghi chú (VD: nạp KC)", false);
        form.addView(amount);
        form.addView(note);
        new AlertDialog.Builder(this)
                .setTitle("Thêm KC sẵn")
                .setView(form)
                .setPositiveButton("Thêm", (d, w) -> {
                    int n = parsePositive(amount.getText().toString());
                    if (n <= 0) { toast("Số KC không hợp lệ"); return; }
                    setAvailable(available() + n);
                    String label = note.getText().toString().trim();
                    addHistory(label.isEmpty() ? "Thêm KC sẵn" : label, n);
                    saveState();
                    render();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private LinearLayout dialogForm() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(22), dp(8), dp(22), 0);
        return l;
    }

    private EditText input(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setPadding(dp(4), dp(12), dp(4), dp(12));
        return e;
    }

    private int parsePositive(String raw) {
        try {
            long n = Long.parseLong(raw.trim());
            return n > 0 && n <= Integer.MAX_VALUE ? (int) n : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void showExpenseDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("Nội dung chi tiêu", false);
        EditText amount = input("Số KC", true);
        Spinner category = new Spinner(this);
        String[] categories = {"Vòng quay", "Trang phục", "Vũ khí", "Pet", "Sự kiện", "Khác"};
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        form.addView(name);
        form.addView(amount);
        form.addView(category);

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
                    saveState();
                    render();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void renderExpenses() {
        expensesBox.removeAllViews();
        JSONArray a = arr("expenses");
        if (a.length() == 0) {
            expensesBox.addView(emptyBox("Chưa có khoản chi."));
            return;
        }
        int start = Math.max(0, a.length() - 8);
        for (int i = a.length() - 1; i >= start; i--) {
            JSONObject e = a.optJSONObject(i);
            if (e == null) continue;
            LinearLayout card = box();
            card.addView(text(e.optString("name"), 15, TEXT, true));
            card.addView(text(e.optString("category") + " • " + dateTime(e.optLong("time")), 12, MUTED, false));
            card.addView(text("-" + fmt(e.optInt("amount")) + " KC", 16, RED, true));
            expensesBox.addView(card);
        }
    }

    private void showCampaignDialog() {
        LinearLayout form = dialogForm();
        EditText name = input("Tên chiến dịch", false);
        EditText target = input("Mục tiêu KC", true);
        form.addView(name);
        form.addView(target);
        new AlertDialog.Builder(this)
                .setTitle("Tạo chiến dịch tiết kiệm")
                .setView(form)
                .setPositiveButton("Tạo", (d, w) -> {
                    String title = name.getText().toString().trim();
                    int n = parsePositive(target.getText().toString());
                    if (title.isEmpty() || n <= 0) { toast("Tên hoặc mục tiêu chưa hợp lệ"); return; }
                    JSONObject c = new JSONObject();
                    try {
                        c.put("id", UUID.randomUUID().toString());
                        c.put("name", title);
                        c.put("target", n);
                        c.put("saved", 0);
                        c.put("time", System.currentTimeMillis());
                        arr("campaigns").put(c);
                    } catch (Exception ignored) {}
                    addHistory("Tạo chiến dịch: " + title, 0);
                    saveState();
                    render();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void renderCampaigns() {
        campaignsBox.removeAllViews();
        JSONArray a = arr("campaigns");
        if (a.length() == 0) {
            campaignsBox.addView(emptyBox("Chưa có chiến dịch tiết kiệm."));
            return;
        }
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject c = a.optJSONObject(i);
            if (c == null) continue;
            int target = Math.max(1, c.optInt("target"));
            int saved = Math.max(0, c.optInt("saved"));
            int pct = Math.min(100, (int) Math.round(saved * 100.0 / target));
            LinearLayout card = box();
            card.addView(text(c.optString("name"), 16, TEXT, true));
            card.addView(text(fmt(saved) + " / " + fmt(target) + " KC • " + pct + "%", 14, GOLD, true));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button save = smallButton("+ Bỏ KC vào");
            Button close = smallButton("Đóng quỹ");
            close.setBackgroundColor(Color.rgb(59, 77, 99));
            close.setTextColor(TEXT);
            String id = c.optString("id");
            save.setOnClickListener(v -> showSaveDialog(id));
            close.setOnClickListener(v -> confirmCloseCampaign(id));
            actions.addView(save);
            actions.addView(close);
            card.addView(actions);
            campaignsBox.addView(card);
        }
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
        EditText amount = input("Số KC muốn để dành", true);
        LinearLayout form = dialogForm();
        form.addView(amount);
        new AlertDialog.Builder(this)
                .setTitle(c.optString("name"))
                .setMessage("KC sẵn: " + fmt(available()))
                .setView(form)
                .setPositiveButton("Chuyển vào quỹ", (d, w) -> {
                    int n = parsePositive(amount.getText().toString());
                    if (n <= 0) { toast("Số KC không hợp lệ"); return; }
                    if (n > available()) { toast("KC sẵn không đủ"); return; }
                    try { c.put("saved", c.optInt("saved") + n); } catch (Exception ignored) {}
                    setAvailable(available() - n);
                    addHistory("Tiết kiệm: " + c.optString("name"), 0);
                    saveState();
                    render();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmCloseCampaign(String id) {
        JSONObject c = findCampaign(id);
        if (c == null) return;
        int saved = c.optInt("saved");
        new AlertDialog.Builder(this)
                .setTitle("Đóng chiến dịch?")
                .setMessage("Hoàn " + fmt(saved) + " KC về số KC sẵn.")
                .setPositiveButton("Đóng & hoàn KC", (d, w) -> closeCampaign(id))
                .setNegativeButton("Hủy", null)
                .show();
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
            saveState();
            render();
            toast("Đã hoàn " + saved + " KC");
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

    private void renderHistory() {
        historyBox.removeAllViews();
        JSONArray a = arr("history");
        if (a.length() == 0) {
            historyBox.addView(emptyBox("Chưa có biến động."));
            return;
        }
        int start = Math.max(0, a.length() - 12);
        for (int i = a.length() - 1; i >= start; i--) {
            JSONObject h = a.optJSONObject(i);
            if (h == null) continue;
            LinearLayout card = box();
            card.addView(text(h.optString("label"), 14, TEXT, true));
            card.addView(text(dateTime(h.optLong("time")), 12, MUTED, false));
            int delta = h.optInt("delta");
            if (delta != 0) {
                card.addView(text((delta > 0 ? "+" : "") + fmt(delta) + " KC", 15, delta > 0 ? ACCENT : RED, true));
            }
            historyBox.addView(card);
        }
    }

    private View emptyBox(String message) {
        LinearLayout l = box();
        l.addView(text(message, 13, MUTED, false));
        return l;
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Đặt lại dữ liệu?")
                .setMessage("Xóa toàn bộ KC, thẻ, chi tiêu, chiến dịch và lịch sử trên máy.")
                .setPositiveButton("Xóa", (d, w) -> {
                    state = freshState();
                    saveState();
                    render();
                    toast("Đã đặt lại dữ liệu");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
