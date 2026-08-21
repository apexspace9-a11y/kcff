package com.kcff.safe;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(11, 15, 20);
    private static final int SURFACE = Color.rgb(20, 26, 34);
    private static final int SURFACE_2 = Color.rgb(29, 37, 48);
    private static final int ACCENT = Color.rgb(255, 184, 0);
    private static final int ACCENT_SOFT = Color.rgb(48, 41, 20);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(145, 155, 169);
    private static final int GREEN = Color.rgb(76, 203, 145);
    private static final int RED = Color.rgb(255, 107, 114);
    private static final int STROKE = Color.rgb(43, 52, 65);
    private static final Locale VI = new Locale("vi", "VN");

    private final List<Campaign> campaigns = new ArrayList<>();
    private final List<Txn> txns = new ArrayList<>();

    private LinearLayout content;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private ScrollView scroll;
    private int currentTab = 0;
    private String historyFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        load();
        setContentView(buildShell());
        navigateTo(R.id.nav_home);
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Két Sắt KC");
        toolbar.setTitleTextColor(TEXT);
        toolbar.setSubtitleTextColor(MUTED);
        toolbar.setBackgroundColor(BG);
        toolbar.setPadding(dp(8), 0, dp(4), 0);
        toolbar.inflateMenu(R.menu.top_actions);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                showQuickAdd();
                return true;
            }
            return false;
        });
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNavigation = new BottomNavigationView(this);
        bottomNavigation.setBackgroundColor(SURFACE);
        bottomNavigation.inflateMenu(R.menu.bottom_nav);
        bottomNavigation.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNavigation.setItemActiveIndicatorColor(ColorStateList.valueOf(ACCENT_SOFT));

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] navColors = new int[] { ACCENT, MUTED };
        ColorStateList navTint = new ColorStateList(states, navColors);
        bottomNavigation.setItemIconTintList(navTint);
        bottomNavigation.setItemTextColor(navTint);
        bottomNavigation.setOnItemSelectedListener(item -> {
            showTab(item.getItemId(), true);
            return true;
        });
        root.addView(bottomNavigation, new LinearLayout.LayoutParams(-1, dp(68)));
        return root;
    }

    private void navigateTo(int itemId) {
        if (bottomNavigation.getSelectedItemId() == itemId) {
            showTab(itemId, true);
        } else {
            bottomNavigation.setSelectedItemId(itemId);
        }
    }

    private void showTab(int itemId, boolean resetScroll) {
        if (itemId == R.id.nav_campaigns) renderCampaigns();
        else if (itemId == R.id.nav_history) renderHistory();
        else renderHome();
        if (resetScroll) scroll.post(() -> scroll.scrollTo(0, 0));
    }

    private void renderHome() {
        currentTab = 0;
        toolbar.setTitle("Két Sắt KC");
        toolbar.setSubtitle("Tổng quan");
        content.removeAllViews();

        long budget = totalBudget();
        long spent = totalSpent();
        long remain = Math.max(0, budget - spent);
        int usage = budget <= 0 ? 0 : (int) Math.min(100, spent * 100 / budget);

        MaterialCardView hero = card(SURFACE, 18, 1);
        LinearLayout heroBody = vertical(dp(18));
        heroBody.addView(text("SỐ DƯ KHẢ DỤNG", 11, MUTED, true));
        TextView balance = text(kc(remain), 32, TEXT, true);
        balance.setPadding(0, dp(6), 0, dp(3));
        heroBody.addView(balance);
        heroBody.addView(text("Tổng ngân sách  " + kc(budget), 12, MUTED, false));
        heroBody.addView(progress(usage, usage >= 90 ? RED : ACCENT, SURFACE_2), lp(-1, dp(6), dp(16)));

        LinearLayout usageRow = new LinearLayout(this);
        usageRow.setGravity(Gravity.CENTER_VERTICAL);
        usageRow.addView(text("Đã dùng " + kc(spent), 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        usageRow.addView(text(usage + "%", 12, usage >= 90 ? RED : ACCENT, true));
        heroBody.addView(usageRow);
        hero.addView(heroBody);
        content.addView(hero, lp(-1, -2, dp(4)));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton spend = button("Ghi chi", ACCENT, BG, false);
        spend.setIconResource(R.drawable.ic_minus);
        spend.setIconTint(ColorStateList.valueOf(BG));
        spend.setOnClickListener(v -> showTxnDialog(true));
        MaterialButton topup = button("Nạp KC", SURFACE_2, TEXT, true);
        topup.setIconResource(R.drawable.ic_add);
        topup.setIconTint(ColorStateList.valueOf(GREEN));
        topup.setOnClickListener(v -> showTxnDialog(false));
        quick.addView(spend, new LinearLayout.LayoutParams(0, dp(48), 1));
        quick.addView(space(dp(10), 1));
        quick.addView(topup, new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(quick, lp(-1, -2, dp(12)));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metricCard("Chiến dịch", String.valueOf(campaigns.size()), "đang lưu", GREEN), new LinearLayout.LayoutParams(0, dp(90), 1));
        metrics.addView(space(dp(10), 1));
        metrics.addView(metricCard("Đã chi", compactKc(spent), "KC", RED), new LinearLayout.LayoutParams(0, dp(90), 1));
        content.addView(metrics, lp(-1, -2, dp(10)));

        sectionTitle("Chiến dịch gần đây", campaigns.isEmpty() ? null : "Xem tất cả");
        if (campaigns.isEmpty()) {
            content.addView(emptyState("Chưa có chiến dịch", "Tạo ngân sách cho một sự kiện để bắt đầu theo dõi KC.", "Tạo chiến dịch", v -> showCampaignDialog(null)), lp(-1, -2, 0));
        } else {
            List<Campaign> sorted = new ArrayList<>(campaigns);
            Collections.sort(sorted, (a, b) -> Boolean.compare(b.active, a.active));
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                content.addView(compactCampaignCard(sorted.get(i)), lp(-1, -2, dp(8)));
            }
        }

        sectionTitle("Giao dịch gần đây", null);
        List<Txn> recent = sortedTxns();
        if (recent.isEmpty()) {
            content.addView(emptyState("Chưa có giao dịch", "Các khoản chi và nạp KC sẽ xuất hiện ở đây.", null, null), lp(-1, -2, 0));
        } else {
            for (int i = 0; i < Math.min(5, recent.size()); i++) {
                content.addView(txnRow(recent.get(i), false), lp(-1, -2, dp(7)));
            }
        }
    }

    private void renderCampaigns() {
        currentTab = 1;
        toolbar.setTitle("Chiến dịch");
        toolbar.setSubtitle("Ngân sách theo sự kiện");
        content.removeAllViews();

        MaterialButton create = button("Tạo chiến dịch", ACCENT, BG, false);
        create.setIconResource(R.drawable.ic_add);
        create.setIconTint(ColorStateList.valueOf(BG));
        create.setOnClickListener(v -> showCampaignDialog(null));
        content.addView(create, new LinearLayout.LayoutParams(-1, dp(48)));

        long active = 0;
        for (Campaign c : campaigns) if (c.active) active++;
        sectionTitle("Danh sách", campaigns.isEmpty() ? "0 chiến dịch" : active + " đang hoạt động");

        if (campaigns.isEmpty()) {
            content.addView(emptyState("Chưa có chiến dịch", "Mỗi chiến dịch có ngân sách, thời hạn và lịch sử riêng.", null, null), lp(-1, -2, 0));
            return;
        }

        List<Campaign> sorted = new ArrayList<>(campaigns);
        Collections.sort(sorted, (a, b) -> {
            if (a.active != b.active) return a.active ? -1 : 1;
            return Long.compare(a.end, b.end);
        });
        for (Campaign c : sorted) content.addView(campaignCard(c), lp(-1, -2, dp(9)));
    }

    private void renderHistory() {
        currentTab = 2;
        toolbar.setTitle("Giao dịch");
        toolbar.setSubtitle("Lịch sử KC");
        content.removeAllViews();

        MaterialCardView summary = card(SURFACE, 18, 1);
        LinearLayout body = vertical(dp(16));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout spentBlock = new LinearLayout(this);
        spentBlock.setOrientation(LinearLayout.VERTICAL);
        spentBlock.addView(text("TỔNG ĐÃ CHI", 10, MUTED, true));
        spentBlock.addView(text(kc(totalSpent()), 24, TEXT, true));
        top.addView(spentBlock, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout countBlock = new LinearLayout(this);
        countBlock.setOrientation(LinearLayout.VERTICAL);
        countBlock.setGravity(Gravity.END);
        TextView countLabel = text("GIAO DỊCH", 10, MUTED, true);
        countLabel.setGravity(Gravity.END);
        TextView countValue = text(String.valueOf(txns.size()), 24, ACCENT, true);
        countValue.setGravity(Gravity.END);
        countBlock.addView(countLabel);
        countBlock.addView(countValue);
        top.addView(countBlock, new LinearLayout.LayoutParams(0, -2, 1));
        body.addView(top);
        summary.addView(body);
        content.addView(summary, lp(-1, -2, dp(4)));

        ChipGroup filters = new ChipGroup(this);
        filters.setSingleSelection(true);
        filters.setSelectionRequired(true);
        filters.setChipSpacingHorizontal(dp(8));
        filters.addView(filterChip("Tất cả", "all"));
        filters.addView(filterChip("Chi KC", "spend"));
        filters.addView(filterChip("Nạp KC", "income"));
        content.addView(filters, lp(-1, -2, dp(12)));

        sectionTitle("Lịch sử", null);
        List<Txn> all = sortedTxns();
        int shown = 0;
        for (Txn t : all) {
            if ("spend".equals(historyFilter) && !t.spend) continue;
            if ("income".equals(historyFilter) && t.spend) continue;
            content.addView(txnRow(t, true), lp(-1, -2, dp(7)));
            shown++;
        }
        if (shown == 0) {
            content.addView(emptyState("Không có giao dịch", "Đổi bộ lọc hoặc thêm giao dịch mới.", "Thêm giao dịch", v -> showQuickAdd()), lp(-1, -2, 0));
        }
    }

    private Chip filterChip(String label, String filter) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTextSize(12);
        chip.setChipStrokeWidth(dp(1));
        chip.setChipStrokeColor(ColorStateList.valueOf(STROKE));
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        chip.setChipBackgroundColor(new ColorStateList(states, new int[] { ACCENT_SOFT, SURFACE }));
        chip.setTextColor(new ColorStateList(states, new int[] { ACCENT, TEXT }));
        chip.setChecked(filter.equals(historyFilter));
        chip.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !filter.equals(historyFilter)) {
                historyFilter = filter;
                renderHistory();
            }
        });
        return chip;
    }

    private View compactCampaignCard(Campaign c) {
        MaterialCardView box = card(SURFACE, 15, 1);
        LinearLayout body = vertical(dp(14));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        nameBlock.addView(text(c.name, 15, TEXT, true));
        nameBlock.addView(text(c.event, 11, MUTED, false));
        top.addView(nameBlock, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(statusChip(c.active));
        body.addView(top);

        long used = spentFor(c.id);
        int pct = c.budget == 0 ? 0 : (int) Math.min(100, used * 100 / c.budget);
        body.addView(progress(pct, pct >= 90 ? RED : ACCENT, SURFACE_2), lp(-1, dp(5), dp(11)));

        LinearLayout info = new LinearLayout(this);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.addView(text(kc(Math.max(0, c.budget - used)) + " còn lại", 12, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        info.addView(text(daysLabel(c), 11, c.active ? MUTED : RED, false));
        body.addView(info);
        box.addView(body);
        box.setClickable(true);
        box.setOnClickListener(v -> navigateTo(R.id.nav_campaigns));
        return box;
    }

    private View campaignCard(Campaign c) {
        MaterialCardView box = card(SURFACE, 16, 1);
        LinearLayout body = vertical(dp(16));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.addView(text(c.name, 17, TEXT, true));
        TextView event = text(c.event, 11, MUTED, false);
        event.setPadding(0, dp(2), 0, 0);
        titleBlock.addView(event);
        top.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(statusChip(c.active));
        body.addView(top);

        long used = spentFor(c.id);
        long remain = Math.max(0, c.budget - used);
        int pct = c.budget == 0 ? 0 : (int) Math.min(100, used * 100 / c.budget);
        TextView balance = text(kc(remain), 23, TEXT, true);
        balance.setPadding(0, dp(13), 0, dp(2));
        body.addView(balance);
        body.addView(text("còn lại / " + kc(c.budget), 11, MUTED, false));
        body.addView(progress(pct, pct >= 90 ? RED : ACCENT, SURFACE_2), lp(-1, dp(6), dp(12)));

        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.addView(text("Đã dùng " + kc(used) + "  •  " + pct + "%", 11, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        meta.addView(text(daysLabel(c), 11, c.active ? GREEN : MUTED, true));
        body.addView(meta);
        TextView dates = text(date(c.start) + "  →  " + date(c.end), 10, MUTED, false);
        dates.setPadding(0, dp(6), 0, 0);
        body.addView(dates);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(13), 0, 0);
        MaterialButton spend = button("Ghi chi", ACCENT, BG, false);
        spend.setEnabled(c.active);
        spend.setOnClickListener(v -> showTxnDialogFor(true, c.id));
        MaterialButton topup = button("Nạp KC", SURFACE_2, TEXT, true);
        topup.setOnClickListener(v -> showTxnDialogFor(false, c.id));
        MaterialButton manage = button("Quản lý", SURFACE_2, TEXT, true);
        manage.setOnClickListener(v -> showCampaignActions(c));
        actions.addView(spend, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(space(dp(7), 1));
        actions.addView(topup, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(space(dp(7), 1));
        actions.addView(manage, new LinearLayout.LayoutParams(0, dp(44), 1));
        body.addView(actions);
        box.addView(body);
        return box;
    }

    private View txnRow(Txn t, boolean deletable) {
        MaterialCardView box = card(SURFACE, 14, 1);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(11), dp(13), dp(11));

        TextView icon = text(t.spend ? "−" : "+", 20, t.spend ? RED : GREEN, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundColor(SURFACE_2, 12));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.setPadding(dp(11), 0, dp(8), 0);
        middle.addView(text(t.note, 13, TEXT, true));
        middle.addView(text(campaignName(t.campaignId) + "  •  " + dateTime(t.time), 10, MUTED, false));
        row.addView(middle, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text((t.spend ? "− " : "+ ") + kc(t.amount), 13, t.spend ? RED : GREEN, true));
        box.addView(row);
        if (deletable) box.setOnLongClickListener(v -> { confirmDeleteTxn(t); return true; });
        return box;
    }

    private View metricCard(String title, String value, String sub, int accent) {
        MaterialCardView box = card(SURFACE, 14, 1);
        LinearLayout body = vertical(dp(13));
        body.addView(text(title.toUpperCase(VI), 9, MUTED, true));
        TextView v = text(value, 18, accent, true);
        v.setPadding(0, dp(4), 0, 0);
        body.addView(v);
        body.addView(text(sub, 10, MUTED, false));
        box.addView(body);
        return box;
    }

    private Chip statusChip(boolean active) {
        Chip chip = new Chip(this);
        chip.setText(active ? "HOẠT ĐỘNG" : "ĐÃ KHÓA");
        chip.setTextSize(9);
        chip.setTextColor(active ? GREEN : MUTED);
        chip.setChipBackgroundColor(ColorStateList.valueOf(SURFACE_2));
        chip.setClickable(false);
        chip.setCheckable(false);
        return chip;
    }

    private View emptyState(String heading, String detail, String action, View.OnClickListener listener) {
        MaterialCardView box = card(SURFACE, 16, 1);
        LinearLayout body = vertical(dp(18));
        body.addView(text(heading, 16, TEXT, true));
        TextView d = text(detail, 12, MUTED, false);
        d.setPadding(0, dp(5), 0, 0);
        body.addView(d);
        if (action != null && listener != null) {
            MaterialButton b = button(action, SURFACE_2, TEXT, true);
            b.setOnClickListener(listener);
            body.addView(b, lp(-1, dp(44), dp(13)));
        }
        box.addView(body);
        return box;
    }

    private void sectionTitle(String label, String trailing) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(18), 0, dp(8));
        row.addView(text(label, 15, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (trailing != null) {
            TextView right = text(trailing, 11, ACCENT, true);
            if ("Xem tất cả".equals(trailing)) right.setOnClickListener(v -> navigateTo(R.id.nav_campaigns));
            row.addView(right);
        }
        content.addView(row);
    }

    private void showQuickAdd() {
        String[] items = campaigns.isEmpty()
                ? new String[] { "Tạo chiến dịch" }
                : new String[] { "Ghi chi KC", "Nạp KC", "Tạo chiến dịch" };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm")
                .setItems(items, (dialog, which) -> {
                    if (campaigns.isEmpty()) showCampaignDialog(null);
                    else if (which == 0) showTxnDialog(true);
                    else if (which == 1) showTxnDialog(false);
                    else showCampaignDialog(null);
                }).show();
    }

    private void showCampaignActions(Campaign c) {
        String stateAction = c.active ? "Khóa chiến dịch" : "Mở lại chiến dịch";
        new MaterialAlertDialogBuilder(this)
                .setTitle(c.name)
                .setItems(new String[] { "Chỉnh sửa", stateAction, "Xóa chiến dịch" }, (dialog, which) -> {
                    if (which == 0) showCampaignDialog(c);
                    else if (which == 1) {
                        c.active = !c.active;
                        save();
                        renderCurrent();
                    } else confirmDeleteCampaign(c);
                }).show();
    }

    private void showCampaignDialog(Campaign edit) {
        LinearLayout form = form();
        TextInputEditText name = field(form, "Tên chiến dịch", false);
        TextInputEditText event = field(form, "Tên sự kiện", false);
        TextInputEditText budget = field(form, "Ngân sách KC", true);
        final long[] dates = new long[] { System.currentTimeMillis(), System.currentTimeMillis() + 7L * 86400000L };
        if (edit != null) {
            name.setText(edit.name);
            event.setText(edit.event);
            budget.setText(String.valueOf(edit.budget));
            dates[0] = edit.start;
            dates[1] = edit.end;
        }

        MaterialButton start = button("Bắt đầu: " + date(dates[0]), SURFACE_2, TEXT, true);
        MaterialButton end = button("Kết thúc: " + date(dates[1]), SURFACE_2, TEXT, true);
        start.setOnClickListener(v -> pickDate(dates, 0, start, "Bắt đầu: "));
        end.setOnClickListener(v -> pickDate(dates, 1, end, "Kết thúc: "));
        form.addView(start, lp(-1, dp(46), dp(7)));
        form.addView(end, lp(-1, dp(46), dp(7)));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(edit == null ? "Tạo chiến dịch" : "Chỉnh sửa chiến dịch")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = value(name);
            String e = value(event);
            long b = parseLong(budget);
            if (n.isEmpty() || e.isEmpty() || b <= 0 || dates[1] < dates[0]) {
                toast("Kiểm tra tên, sự kiện, ngân sách và thời gian.");
                return;
            }
            if (edit != null && b < spentFor(edit.id)) {
                toast("Ngân sách không thể thấp hơn số KC đã chi.");
                return;
            }
            if (edit == null) campaigns.add(new Campaign(id(), n, e, b, dates[0], dates[1], true));
            else {
                edit.name = n;
                edit.event = e;
                edit.budget = b;
                edit.start = dates[0];
                edit.end = dates[1];
            }
            save();
            dialog.dismiss();
            navigateTo(R.id.nav_campaigns);
        }));
        dialog.show();
    }

    private void showTxnDialog(boolean spend) {
        showTxnDialogFor(spend, campaigns.isEmpty() ? null : campaigns.get(0).id);
    }

    private void showTxnDialogFor(boolean spend, String selectedId) {
        if (campaigns.isEmpty()) {
            toast("Tạo chiến dịch trước khi ghi giao dịch.");
            showCampaignDialog(null);
            return;
        }

        LinearLayout form = form();
        TextInputLayout campaignLayout = inputLayout("Chiến dịch");
        campaignLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        AutoCompleteTextView campaignInput = new AutoCompleteTextView(this);
        campaignInput.setTextColor(TEXT);
        campaignInput.setInputType(InputType.TYPE_NULL);
        campaignLayout.addView(campaignInput, new LinearLayout.LayoutParams(-1, -2));
        form.addView(campaignLayout, lp(-1, -2, dp(7)));

        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < campaigns.size(); i++) {
            names.add(campaigns.get(i).name);
            if (campaigns.get(i).id.equals(selectedId)) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        campaignInput.setAdapter(adapter);
        campaignInput.setText(names.get(selected), false);
        final int[] selectedIndex = new int[] { selected };
        campaignInput.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);

        TextInputEditText amount = field(form, spend ? "Số KC đã chi" : "Số KC nạp thêm", true);
        TextInputEditText note = field(form, spend ? "Nội dung chi" : "Ghi chú", false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(spend ? "Ghi chi KC" : "Nạp KC")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            long amt = parseLong(amount);
            String n = value(note);
            Campaign c = campaigns.get(selectedIndex[0]);
            if (amt <= 0 || n.isEmpty()) {
                toast("Nhập số KC và nội dung giao dịch.");
                return;
            }
            if (spend) {
                long now = System.currentTimeMillis();
                if (!c.active) { toast("Chiến dịch đang bị khóa."); return; }
                if (now < c.start || now > c.end) { toast("Chiến dịch chưa mở hoặc đã kết thúc."); return; }
                long available = c.budget - spentFor(c.id);
                if (amt > available) { toast("Khoản chi vượt quá số KC còn lại."); return; }
            } else {
                c.budget += amt;
            }
            txns.add(new Txn(id(), c.id, amt, n, spend, System.currentTimeMillis()));
            save();
            dialog.dismiss();
            renderCurrent();
        }));
        dialog.show();
    }

    private void confirmDeleteTxn(Txn t) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa giao dịch?")
                .setMessage(t.note + "\n" + kc(t.amount))
                .setNegativeButton("Giữ lại", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (!t.spend) {
                        Campaign c = campaignById(t.campaignId);
                        if (c != null) c.budget = Math.max(spentFor(c.id), c.budget - t.amount);
                    }
                    txns.remove(t);
                    save();
                    renderHistory();
                }).show();
    }

    private void confirmDeleteCampaign(Campaign c) {
        long count = 0;
        for (Txn t : txns) if (c.id.equals(t.campaignId)) count++;
        String message = count == 0
                ? "Chiến dịch này chưa có giao dịch."
                : "Xóa chiến dịch sẽ xóa luôn " + count + " giao dịch liên quan.";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa " + c.name + "?")
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    campaigns.remove(c);
                    for (int i = txns.size() - 1; i >= 0; i--) {
                        if (c.id.equals(txns.get(i).campaignId)) txns.remove(i);
                    }
                    save();
                    renderCurrent();
                }).show();
    }

    private void pickDate(long[] dates, int index, MaterialButton button, String prefix) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(dates[index]);
        new DatePickerDialog(this, (view, year, month, day) -> {
            java.util.Calendar picked = java.util.Calendar.getInstance();
            picked.set(year, month, day, 12, 0, 0);
            dates[index] = picked.getTimeInMillis();
            button.setText(prefix + date(dates[index]));
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private TextInputEditText field(LinearLayout form, String hint, boolean number) {
        TextInputLayout layout = inputLayout(hint);
        TextInputEditText edit = new TextInputEditText(this);
        edit.setTextColor(TEXT);
        edit.setTextSize(15);
        edit.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        form.addView(layout, lp(-1, -2, dp(7)));
        return edit;
    }

    private TextInputLayout inputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxBackgroundColor(Color.TRANSPARENT);
        layout.setBoxStrokeColor(ACCENT);
        layout.setHintTextColor(ColorStateList.valueOf(MUTED));
        return layout;
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(8), dp(8), 0);
        return form;
    }

    private MaterialCardView card(int color, int radiusDp, int strokeDp) {
        MaterialCardView box = new MaterialCardView(this);
        box.setCardBackgroundColor(color);
        box.setRadius(dp(radiusDp));
        box.setCardElevation(0);
        if (strokeDp > 0) {
            box.setStrokeWidth(dp(strokeDp));
            box.setStrokeColor(STROKE);
        }
        return box;
    }

    private MaterialButton button(String label, int background, int foreground, boolean outlined) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTextColor(foreground);
        button.setAllCaps(false);
        button.setCornerRadius(dp(12));
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        if (outlined) {
            button.setStrokeWidth(dp(1));
            button.setStrokeColor(ColorStateList.valueOf(STROKE));
        }
        return button;
    }

    private LinearProgressIndicator progress(int value, int indicator, int track) {
        LinearProgressIndicator p = new LinearProgressIndicator(this);
        p.setMax(100);
        p.setTrackColor(track);
        p.setIndicatorColor(indicator);
        p.setProgressCompat(Math.max(0, Math.min(100, value)), false);
        return p;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(padding, padding, padding, padding);
        return l;
    }

    private View space(int width, int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        return v;
    }

    private android.graphics.drawable.GradientDrawable roundColor(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private LinearLayout.LayoutParams lp(int width, int height, int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.topMargin = topMargin;
        return p;
    }

    private void renderCurrent() {
        if (currentTab == 1) renderCampaigns();
        else if (currentTab == 2) renderHistory();
        else renderHome();
    }

    private long totalBudget() {
        long total = 0;
        for (Campaign c : campaigns) total += c.budget;
        return total;
    }

    private long totalSpent() {
        long total = 0;
        for (Txn t : txns) if (t.spend) total += t.amount;
        return total;
    }

    private long spentFor(String campaignId) {
        long total = 0;
        for (Txn t : txns) if (t.spend && campaignId.equals(t.campaignId)) total += t.amount;
        return total;
    }

    private Campaign campaignById(String id) {
        for (Campaign c : campaigns) if (c.id.equals(id)) return c;
        return null;
    }

    private String campaignName(String id) {
        Campaign c = campaignById(id);
        return c == null ? "Chiến dịch đã xóa" : c.name;
    }

    private List<Txn> sortedTxns() {
        List<Txn> out = new ArrayList<>(txns);
        Collections.sort(out, Comparator.comparingLong((Txn t) -> t.time).reversed());
        return out;
    }

    private String daysLabel(Campaign c) {
        if (!c.active) return "Đã khóa";
        long now = System.currentTimeMillis();
        if (now > c.end) return "Đã hết hạn";
        if (now < c.start) return "Còn " + Math.max(1, (c.start - now + 86399999L) / 86400000L) + " ngày mở";
        long days = Math.max(0, (c.end - now + 86399999L) / 86400000L);
        return days == 0 ? "Kết thúc hôm nay" : "Còn " + days + " ngày";
    }

    private String kc(long amount) {
        return NumberFormat.getIntegerInstance(VI).format(Math.max(0, amount)) + " KC";
    }

    private String compactKc(long value) {
        long v = Math.max(0, value);
        if (v >= 1_000_000) return String.format(VI, "%.1fM", v / 1_000_000f);
        if (v >= 1_000) return String.format(VI, "%.1fK", v / 1_000f);
        return String.valueOf(v);
    }

    private String date(long time) {
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(new Date(time));
    }

    private String dateTime(long time) {
        return new SimpleDateFormat("dd/MM • HH:mm", VI).format(new Date(time));
    }

    private long parseLong(TextInputEditText edit) {
        try {
            return Long.parseLong(value(edit).replace(".", "").replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String value(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    private String id() {
        return String.valueOf(System.currentTimeMillis()) + "-" + Math.abs((int) (Math.random() * 100000));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void save() {
        try {
            JSONArray c = new JSONArray();
            for (Campaign item : campaigns) c.put(item.json());
            JSONArray t = new JSONArray();
            for (Txn item : txns) t.put(item.json());
            getSharedPreferences("kcff", MODE_PRIVATE)
                    .edit()
                    .putString("campaigns", c.toString())
                    .putString("txns", t.toString())
                    .apply();
        } catch (Exception ignored) { }
    }

    private void load() {
        campaigns.clear();
        txns.clear();
        try {
            JSONArray c = new JSONArray(getSharedPreferences("kcff", MODE_PRIVATE).getString("campaigns", "[]"));
            JSONArray t = new JSONArray(getSharedPreferences("kcff", MODE_PRIVATE).getString("txns", "[]"));
            for (int i = 0; i < c.length(); i++) campaigns.add(Campaign.from(c.getJSONObject(i)));
            for (int i = 0; i < t.length(); i++) txns.add(Txn.from(t.getJSONObject(i)));
        } catch (Exception ignored) { }
    }

    static class Campaign {
        String id, name, event;
        long budget, start, end;
        boolean active;

        Campaign(String id, String name, String event, long budget, long start, long end, boolean active) {
            this.id = id;
            this.name = name;
            this.event = event;
            this.budget = budget;
            this.start = start;
            this.end = end;
            this.active = active;
        }

        JSONObject json() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("event", event);
            o.put("budget", budget);
            o.put("start", start);
            o.put("end", end);
            o.put("active", active);
            return o;
        }

        static Campaign from(JSONObject o) throws Exception {
            return new Campaign(
                    o.getString("id"),
                    o.getString("name"),
                    o.getString("event"),
                    o.getLong("budget"),
                    o.getLong("start"),
                    o.getLong("end"),
                    o.optBoolean("active", true));
        }
    }

    static class Txn {
        String id, campaignId, note;
        long amount, time;
        boolean spend;

        Txn(String id, String campaignId, long amount, String note, boolean spend, long time) {
            this.id = id;
            this.campaignId = campaignId;
            this.amount = amount;
            this.note = note;
            this.spend = spend;
            this.time = time;
        }

        JSONObject json() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("campaignId", campaignId);
            o.put("amount", amount);
            o.put("note", note);
            o.put("spend", spend);
            o.put("time", time);
            return o;
        }

        static Txn from(JSONObject o) throws Exception {
            return new Txn(
                    o.getString("id"),
                    o.getString("campaignId"),
                    o.getLong("amount"),
                    o.getString("note"),
                    o.getBoolean("spend"),
                    o.getLong("time"));
        }
    }
}
