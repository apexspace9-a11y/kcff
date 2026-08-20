package com.kcff.vault;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 14, 22);
    private static final int PANEL = Color.rgb(21, 25, 39);
    private static final int PANEL_2 = Color.rgb(29, 34, 50);
    private static final int TEXT = Color.rgb(246, 247, 251);
    private static final int MUTED = Color.rgb(157, 165, 185);
    private static final int ACCENT = Color.rgb(255, 213, 74);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int RED = Color.rgb(255, 99, 111);
    private static final int GREEN = Color.rgb(77, 219, 147);
    private static final long DAY = 24L * 60L * 60L * 1000L;

    private KcffStore store;
    private FrameLayout content;
    private LinearLayout nav;
    private int currentScreen = 0;
    private TextView navOverview;
    private TextView navVaults;
    private TextView navLedger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        store = new KcffStore(this);
        setContentView(buildShell());
        render();
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(8), dp(10), dp(8));
        nav.setBackground(gradient(new int[]{Color.rgb(16, 19, 30), Color.rgb(11, 14, 22)}, 0));

        navOverview = navItem("TỔNG QUAN", 0);
        navVaults = navItem("KÉT", 1);
        navLedger = navItem("GIAO DỊCH", 2);

        nav.addView(navOverview, weight());
        nav.addView(navVaults, weight());
        nav.addView(navLedger, weight());

        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        return root;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
    }

    private TextView navItem(String text, int screen) {
        TextView v = text(text, 11, MUTED, true);
        v.setGravity(Gravity.CENTER);
        v.setOnClickListener(x -> {
            currentScreen = screen;
            render();
        });
        return v;
    }

    private void render() {
        content.removeAllViews();
        if (currentScreen == 0) content.addView(overview());
        else if (currentScreen == 1) content.addView(vaultsPage());
        else content.addView(ledgerPage());
        updateNav();
    }

    private void updateNav() {
        TextView[] items = {navOverview, navVaults, navLedger};
        for (int i = 0; i < items.length; i++) {
            items[i].setTextColor(i == currentScreen ? ACCENT : MUTED);
            items[i].setBackground(i == currentScreen
                    ? solid(Color.rgb(33, 36, 48), dp(14))
                    : solid(Color.TRANSPARENT, dp(14)));
        }
    }

    private View overview() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout page = page();
        scroll.addView(page);

        page.addView(header("KÉT SẮT KC", "FREE FIRE • QUẢN LÝ MỤC TIÊU"));

        int saved = store.totalBalance();
        int target = store.totalTarget();
        int pct = target == 0 ? 0 : Math.min(100, (int) Math.round(saved * 100.0 / target));

        LinearLayout hero = card();
        hero.setBackground(gradient(new int[]{Color.rgb(40, 33, 20), Color.rgb(22, 24, 35)}, dp(24)));
        hero.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView cap = text("TỔNG KC ĐANG GIỮ", 12, Color.rgb(227, 198, 104), true);
        hero.addView(cap);

        LinearLayout balanceRow = new LinearLayout(this);
        balanceRow.setOrientation(LinearLayout.HORIZONTAL);
        balanceRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView gem = new ImageView(this);
        gem.setImageResource(com.kcff.vault.R.drawable.ic_kc);
        balanceRow.addView(gem, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView balance = text(formatKC(saved), 34, TEXT, true);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bp.leftMargin = dp(10);
        balanceRow.addView(balance, bp);
        TextView pctText = pill(pct + "%", ACCENT, Color.rgb(49, 43, 26));
        balanceRow.addView(pctText);
        hero.addView(balanceRow);

        ProgressBar progress = progress(pct);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        pp.topMargin = dp(12);
        hero.addView(progress, pp);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(12), 0, 0);
        stats.addView(stat("MỤC TIÊU", formatKC(target) + " KC"), weight());
        stats.addView(stat("ĐÃ CHI", formatKC(store.totalSpent()) + " KC"), weight());
        stats.addView(stat("SỐ KÉT", String.valueOf(store.vaults().size())), weight());
        hero.addView(stats);

        page.addView(hero, marginTop(dp(16)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView create = action("＋  TẠO KÉT", ACCENT, Color.rgb(43, 38, 22));
        create.setOnClickListener(v -> showVaultDialog(null));
        TextView add = action("◆  NẠP KC", TEXT, PANEL_2);
        add.setOnClickListener(v -> showTransactionDialog(KcffStore.TYPE_DEPOSIT, 0));
        actions.addView(create, weightGap());
        actions.addView(add, weight());
        page.addView(actions, marginTop(dp(14)));

        page.addView(sectionTitle("CHIẾN DỊCH ĐANG CHẠY"), marginTop(dp(24)));

        if (store.vaults().isEmpty()) {
            page.addView(emptyState(
                    "Chưa có két nào",
                    "Tạo két theo event, đặt mục tiêu KC và theo dõi tiến độ từng ngày.",
                    "TẠO KÉT ĐẦU TIÊN",
                    v -> showVaultDialog(null)
            ), marginTop(dp(10)));
        } else {
            int shown = Math.min(3, store.vaults().size());
            for (int i = 0; i < shown; i++) {
                page.addView(vaultCard(store.vaults().get(i), true), marginTop(dp(10)));
            }
            if (store.vaults().size() > shown) {
                TextView all = action("XEM TẤT CẢ KÉT", ACCENT, PANEL);
                all.setOnClickListener(v -> {
                    currentScreen = 1;
                    render();
                });
                page.addView(all, marginTop(dp(10)));
            }
        }

        page.addView(sectionTitle("NHỊP CHI TIÊU"), marginTop(dp(24)));
        page.addView(spendCard(), marginTop(dp(10)));

        LinearLayout.LayoutParams end = new LinearLayout.LayoutParams(1, dp(18));
        page.addView(new View(this), end);
        return scroll;
    }

    private View vaultsPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = page();
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("KÉT CHIẾN DỊCH", 24, TEXT, true));
        titles.addView(text("Mỗi event một két, đỡ tiêu KC như nước.", 13, MUTED, false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = pill("＋ TẠO", ACCENT, Color.rgb(43, 38, 22));
        add.setPadding(dp(16), dp(10), dp(16), dp(10));
        add.setOnClickListener(v -> showVaultDialog(null));
        top.addView(add);
        page.addView(top);

        if (store.vaults().isEmpty()) {
            page.addView(emptyState(
                    "Két đang trống",
                    "Tạo chiến dịch đầu tiên để gom KC theo mục tiêu.",
                    "TẠO KÉT",
                    v -> showVaultDialog(null)
            ), marginTop(dp(20)));
        } else {
            for (KcffStore.Vault vault : store.vaults()) {
                page.addView(vaultCard(vault, false), marginTop(dp(14)));
            }
        }
        page.addView(new View(this), new LinearLayout.LayoutParams(1, dp(18)));
        return scroll;
    }

    private View ledgerPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = page();
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("SỔ GIAO DỊCH", 24, TEXT, true));
        titles.addView(text("Nạp, chi và lịch sử KC của bạn.", 13, MUTED, false));
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = pill("＋ GHI", ACCENT, Color.rgb(43, 38, 22));
        add.setPadding(dp(16), dp(10), dp(16), dp(10));
        add.setOnClickListener(v -> showTransactionDialog(null, 0));
        top.addView(add);
        page.addView(top);

        LinearLayout summary = card();
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(stat("ĐANG GIỮ", formatKC(store.totalBalance()) + " KC"), weight());
        summary.addView(stat("TỔNG ĐÃ CHI", formatKC(store.totalSpent()) + " KC"), weight());
        page.addView(summary, marginTop(dp(16)));

        List<KcffStore.Tx> txs = store.transactions();
        if (txs.isEmpty()) {
            page.addView(emptyState(
                    "Chưa có giao dịch",
                    "Các lần nạp và chi KC sẽ nằm gọn ở đây.",
                    "GHI GIAO DỊCH",
                    v -> showTransactionDialog(null, 0)
            ), marginTop(dp(18)));
        } else {
            page.addView(sectionTitle("GẦN ĐÂY"), marginTop(dp(22)));
            for (KcffStore.Tx tx : txs) {
                page.addView(transactionRow(tx), marginTop(dp(8)));
            }
        }
        page.addView(new View(this), new LinearLayout.LayoutParams(1, dp(18)));
        return scroll;
    }

    private View header(String title, String sub) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.kcff.vault.R.drawable.ic_kc);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(52));
        row.addView(logo, lp);

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(10), 0, 0, 0);
        words.addView(text(title, 23, TEXT, true));
        words.addView(text(sub, 11, MUTED, true));
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = pill("KC", ACCENT, Color.rgb(43, 38, 22));
        row.addView(badge);
        return row;
    }

    private View spendCard() {
        LinearLayout card = card();
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        long weekStart = System.currentTimeMillis() - 7 * DAY;
        int week = store.spentSince(weekStart);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(text("7 NGÀY GẦN NHẤT", 11, MUTED, true));
        l.addView(text(formatKC(week) + " KC", 25, TEXT, true));
        top.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int avg = week / 7;
        top.addView(pill("TB " + formatKC(avg) + "/ngày", PURPLE, Color.rgb(39, 32, 58)));
        card.addView(top);

        TextView hint;
        if (week == 0) {
            hint = text("Chưa phát sinh chi tiêu trong tuần này.", 13, MUTED, false);
        } else {
            hint = text("Theo dõi nhịp chi để không rút két trước ngày event.", 13, MUTED, false);
        }
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(10);
        card.addView(hint, hp);
        return card;
    }

    private View vaultCard(KcffStore.Vault vault, boolean compact) {
        LinearLayout box = card();
        box.setPadding(dp(16), dp(15), dp(16), dp(15));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(vault.name, 18, TEXT, true));
        String event = vault.event.isEmpty() ? "CHIẾN DỊCH KC" : vault.event.toUpperCase(Locale.ROOT);
        names.addView(text(event, 11, MUTED, true));
        top.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int pct = Math.min(100, (int) Math.round(vault.balance * 100.0 / Math.max(1, vault.target)));
        top.addView(pill(pct + "%", pct >= 100 ? GREEN : ACCENT,
                pct >= 100 ? Color.rgb(24, 53, 42) : Color.rgb(49, 43, 26)));
        box.addView(top);

        LinearLayout amount = new LinearLayout(this);
        amount.setGravity(Gravity.BOTTOM);
        amount.setPadding(0, dp(10), 0, 0);
        amount.addView(text(formatKC(vault.balance), 28, TEXT, true));
        TextView slash = text(" / " + formatKC(vault.target) + " KC", 14, MUTED, false);
        slash.setPadding(3, 0, 0, dp(3));
        amount.addView(slash);
        box.addView(amount);

        ProgressBar bar = progress(pct);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
        bp.topMargin = dp(10);
        box.addView(bar, bp);

        String timing = vaultTiming(vault);
        TextView meta = text(timing, 12, MUTED, false);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.topMargin = dp(8);
        box.addView(meta, mp);

        if (!compact) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setPadding(0, dp(13), 0, 0);

            TextView deposit = miniAction("NẠP", ACCENT);
            deposit.setOnClickListener(v -> showTransactionDialog(KcffStore.TYPE_DEPOSIT, vault.id));
            TextView spend = miniAction("CHI", RED);
            spend.setOnClickListener(v -> showTransactionDialog(KcffStore.TYPE_SPEND, vault.id));
            TextView edit = miniAction("SỬA", TEXT);
            edit.setOnClickListener(v -> showVaultDialog(vault));
            TextView delete = miniAction("XÓA", MUTED);
            delete.setOnClickListener(v -> confirmDeleteVault(vault));

            buttons.addView(deposit, weightTiny());
            buttons.addView(spend, weightTiny());
            buttons.addView(edit, weightTiny());
            buttons.addView(delete, weight());
            box.addView(buttons);
        } else {
            box.setOnClickListener(v -> {
                currentScreen = 1;
                render();
            });
        }
        return box;
    }

    private View transactionRow(KcffStore.Tx tx) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        boolean deposit = KcffStore.TYPE_DEPOSIT.equals(tx.type);
        TextView icon = pill(deposit ? "＋" : "−",
                deposit ? GREEN : RED,
                deposit ? Color.rgb(24, 53, 42) : Color.rgb(59, 29, 34));
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(38), dp(38));
        row.addView(icon, ip);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setPadding(dp(11), 0, dp(8), 0);

        KcffStore.Vault vault = store.findVault(tx.vaultId);
        String title = tx.note.isEmpty()
                ? (deposit ? "Nạp KC" : "Chi KC")
                : tx.note;
        String sub = tx.vaultId == 0
                ? "Chi tiêu chung"
                : (vault == null ? "Két đã xóa" : vault.name);
        center.addView(text(title, 14, TEXT, true));
        center.addView(text(sub + " • " + formatDateTime(tx.time), 11, MUTED, false));
        row.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setGravity(Gravity.END);
        right.setOrientation(LinearLayout.VERTICAL);
        TextView amount = text((deposit ? "+" : "−") + formatKC(tx.amount), 15,
                deposit ? GREEN : RED, true);
        amount.setGravity(Gravity.END);
        right.addView(amount);
        TextView del = text("xóa", 10, MUTED, false);
        del.setGravity(Gravity.END);
        del.setPadding(dp(8), dp(5), 0, 0);
        del.setOnClickListener(v -> confirmDeleteTx(tx));
        right.addView(del);
        row.addView(right);
        return row;
    }

    private View emptyState(String title, String body, String button, View.OnClickListener listener) {
        LinearLayout box = card();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(22), dp(28), dp(22), dp(28));

        ImageView gem = new ImageView(this);
        gem.setImageResource(com.kcff.vault.R.drawable.ic_kc);
        box.addView(gem, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView t = text(title, 18, TEXT, true);
        t.setGravity(Gravity.CENTER);
        box.addView(t, marginTop(dp(10)));

        TextView b = text(body, 13, MUTED, false);
        b.setGravity(Gravity.CENTER);
        box.addView(b, marginTop(dp(6)));

        TextView a = action(button, ACCENT, Color.rgb(43, 38, 22));
        a.setOnClickListener(listener);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        ap.topMargin = dp(16);
        box.addView(a, ap);
        return box;
    }

    private View stat(String label, String value) {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.addView(text(label, 10, MUTED, true));
        s.addView(text(value, 14, TEXT, true));
        return s;
    }

    private void showVaultDialog(KcffStore.Vault editing) {
        LinearLayout form = dialogForm();

        EditText name = field("Tên két", InputType.TYPE_CLASS_TEXT);
        EditText event = field("Tên event / chiến dịch", InputType.TYPE_CLASS_TEXT);
        EditText target = field("Mục tiêu KC", InputType.TYPE_CLASS_NUMBER);

        if (editing != null) {
            name.setText(editing.name);
            event.setText(editing.event);
            target.setText(String.valueOf(editing.target));
        }

        form.addView(label("TÊN KÉT"));
        form.addView(name);
        form.addView(label("SỰ KIỆN"));
        form.addView(event);
        form.addView(label("MỤC TIÊU KC"));
        form.addView(target);

        final long[] endAt = {editing == null ? 0 : editing.endAt};
        TextView date = action(
                endAt[0] == 0 ? "CHỌN NGÀY KẾT THÚC" : "KẾT THÚC: " + formatDate(endAt[0]),
                TEXT, PANEL_2);
        date.setOnClickListener(v -> pickDate(endAt, date));
        form.addView(label("THỜI HẠN"));
        form.addView(date);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing == null ? "Tạo két chiến dịch" : "Chỉnh sửa két")
                .setView(form)
                .setNegativeButton("HỦY", null)
                .setPositiveButton(editing == null ? "TẠO KÉT" : "LƯU", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String e = event.getText().toString().trim();
            int goal = parsePositive(target.getText().toString());
            if (n.isEmpty()) {
                name.setError("Nhập tên két");
                return;
            }
            if (goal <= 0) {
                target.setError("Mục tiêu phải lớn hơn 0");
                return;
            }
            if (editing == null) store.createVault(n, e, goal, endAt[0]);
            else store.updateVault(editing.id, n, e, goal, endAt[0]);
            dialog.dismiss();
            render();
            toast(editing == null ? "Đã tạo két" : "Đã cập nhật két");
        }));
        dialog.show();
    }

    private void showTransactionDialog(String presetType, long presetVaultId) {
        if (store.vaults().isEmpty() && KcffStore.TYPE_DEPOSIT.equals(presetType)) {
            toast("Tạo két trước khi nạp KC");
            showVaultDialog(null);
            return;
        }

        LinearLayout form = dialogForm();

        Spinner type = new Spinner(this);
        String[] types = {"Nạp KC", "Chi KC"};
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        if (KcffStore.TYPE_SPEND.equals(presetType)) type.setSelection(1);

        Spinner vault = new Spinner(this);
        ArrayList<String> vaultNames = new ArrayList<>();
        vaultNames.add("Chi tiêu chung");
        int selected = 0;
        List<KcffStore.Vault> vaults = store.vaults();
        for (int i = 0; i < vaults.size(); i++) {
            vaultNames.add(vaults.get(i).name);
            if (vaults.get(i).id == presetVaultId) selected = i + 1;
        }
        vault.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vaultNames));
        vault.setSelection(selected);

        EditText amount = field("Số KC", InputType.TYPE_CLASS_NUMBER);
        EditText note = field("Ghi chú, ví dụ: skin / vòng quay", InputType.TYPE_CLASS_TEXT);

        form.addView(label("LOẠI"));
        form.addView(type);
        form.addView(label("KÉT"));
        form.addView(vault);
        form.addView(label("SỐ KC"));
        form.addView(amount);
        form.addView(label("GHI CHÚ"));
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ghi giao dịch")
                .setView(form)
                .setNegativeButton("HỦY", null)
                .setPositiveButton("LƯU", null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean isDeposit = type.getSelectedItemPosition() == 0;
            int value = parsePositive(amount.getText().toString());
            int vaultPosition = vault.getSelectedItemPosition();
            long vaultId = vaultPosition == 0 ? 0 : vaults.get(vaultPosition - 1).id;

            if (value <= 0) {
                amount.setError("Nhập số KC lớn hơn 0");
                return;
            }
            if (isDeposit && vaultId == 0) {
                toast("Nạp KC cần chọn một két");
                return;
            }
            boolean ok = store.addTransaction(
                    isDeposit ? KcffStore.TYPE_DEPOSIT : KcffStore.TYPE_SPEND,
                    value,
                    note.getText().toString(),
                    vaultId
            );
            if (!ok) {
                toast(isDeposit ? "Không thể nạp KC" : "Số KC chi vượt quá số dư của két");
                return;
            }
            dialog.dismiss();
            render();
            pulseContent();
            toast(isDeposit ? "Đã nạp KC" : "Đã ghi chi tiêu");
        }));
        dialog.show();
    }

    private void pickDate(long[] endAt, TextView date) {
        Calendar now = Calendar.getInstance();
        if (endAt[0] > 0) now.setTimeInMillis(endAt[0]);

        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 23, 59, 59);
                    c.set(Calendar.MILLISECOND, 999);
                    endAt[0] = c.getTimeInMillis();
                    date.setText("KẾT THÚC: " + formatDate(endAt[0]));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        picker.show();
    }

    private void confirmDeleteVault(KcffStore.Vault vault) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa két “" + vault.name + "”?")
                .setMessage("Lịch sử giao dịch vẫn được giữ.")
                .setNegativeButton("HỦY", null)
                .setPositiveButton("XÓA", (d, w) -> {
                    store.deleteVault(vault.id);
                    render();
                    toast("Đã xóa két");
                })
                .show();
    }

    private void confirmDeleteTx(KcffStore.Tx tx) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch?")
                .setMessage("Số dư két sẽ được hoàn tác theo giao dịch này.")
                .setNegativeButton("HỦY", null)
                .setPositiveButton("XÓA", (d, w) -> {
                    store.deleteTransaction(tx.id);
                    render();
                    toast("Đã xóa giao dịch");
                })
                .show();
    }

    private String vaultTiming(KcffStore.Vault vault) {
        int remain = Math.max(0, vault.target - vault.balance);
        if (vault.balance >= vault.target) return "Đã đạt mục tiêu • sẵn sàng cho event";
        if (vault.endAt <= 0) return "Còn thiếu " + formatKC(remain) + " KC • chưa đặt hạn";
        long diff = vault.endAt - System.currentTimeMillis();
        if (diff <= 0) return "Đã tới hạn • còn thiếu " + formatKC(remain) + " KC";
        long days = Math.max(1, (long) Math.ceil(diff / (double) DAY));
        int daily = (int) Math.ceil(remain / (double) days);
        return "Còn " + days + " ngày • cần " + formatKC(daily) + " KC/ngày";
    }

    private ProgressBar progress(int percent) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        p.setProgress(Math.max(0, Math.min(1000, percent * 10)));
        p.getProgressDrawable().setTint(ACCENT);
        if (p.getProgressBackgroundTintList() == null) {
            p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(52, 55, 66)));
        } else {
            p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(52, 55, 66)));
        }
        return p;
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(10));
        page.setBackgroundColor(BG);
        return page;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(solid(PANEL, dp(20)));
        box.setElevation(dp(2));
        return box;
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        return form;
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setPadding(dp(12), dp(11), dp(12), dp(11));
        return e;
    }

    private TextView label(String s) {
        TextView v = text(s, 10, MUTED, true);
        v.setPadding(0, dp(14), 0, dp(4));
        return v;
    }

    private TextView sectionTitle(String s) {
        return text(s, 12, MUTED, true);
    }

    private TextView action(String s, int color, int bg) {
        TextView v = text(s, 12, color, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(solid(bg, dp(14)));
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        return v;
    }

    private TextView miniAction(String s, int color) {
        TextView v = text(s, 11, color, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(solid(PANEL_2, dp(11)));
        v.setPadding(dp(8), dp(9), dp(8), dp(9));
        return v;
    }

    private TextView pill(String s, int color, int bg) {
        TextView v = text(s, 11, color, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(solid(bg, dp(100)));
        v.setPadding(dp(10), dp(6), dp(10), dp(6));
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable solid(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private GradientDrawable gradient(int[] colors, float radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radius);
        return d;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        return p;
    }

    private LinearLayout.LayoutParams weightGap() {
        LinearLayout.LayoutParams p = weight();
        p.rightMargin = dp(8);
        return p;
    }

    private LinearLayout.LayoutParams weightTiny() {
        LinearLayout.LayoutParams p = weight();
        p.rightMargin = dp(6);
        return p;
    }

    private int parsePositive(String raw) {
        try {
            long n = Long.parseLong(raw.trim());
            return n > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) n);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatKC(int amount) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount);
    }

    private String formatDate(long time) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(time));
    }

    private String formatDateTime(long time) {
        return new SimpleDateFormat("dd/MM • HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private void pulseContent() {
        ObjectAnimator a = ObjectAnimator.ofFloat(content, View.ALPHA, 0.7f, 1f);
        a.setDuration(260);
        a.setInterpolator(new DecelerateInterpolator());
        a.start();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
