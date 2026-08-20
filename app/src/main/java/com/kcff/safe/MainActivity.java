package com.kcff.safe;

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
import android.widget.AdapterView;
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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(7,17,31);
    private static final int CARD = Color.rgb(17,31,49);
    private static final int CARD_2 = Color.rgb(23,42,63);
    private static final int YELLOW = Color.rgb(255,201,40);
    private static final int TEXT = Color.rgb(244,247,251);
    private static final int MUTED = Color.rgb(157,174,193);
    private static final int GREEN = Color.rgb(55,211,153);
    private static final int RED = Color.rgb(255,103,107);
    private static final Locale VI = new Locale("vi", "VN");

    private final List<Campaign> campaigns = new ArrayList<>();
    private final List<Txn> txns = new ArrayList<>();
    private LinearLayout content;
    private TextView title;
    private Button navHome, navCampaign, navHistory;
    private int currentTab = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        load();
        seedIfNeeded();
        setContentView(buildShell());
        renderHome();
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(NAVY);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(16), dp(12));
        TextView logo = text("KC", 17, NAVY, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(YELLOW, 12));
        header.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        title = text("Két Sắt KC", 22, TEXT, true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, dp(48), 1);
        tl.leftMargin = dp(12);
        header.addView(title, tl);
        Button add = button("+", YELLOW, NAVY);
        add.setTextSize(25);
        add.setOnClickListener(v -> showQuickAdd());
        header.addView(add, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        FrameLayout frame = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(10), dp(8), dp(10), dp(10));
        nav.setBackgroundColor(Color.rgb(9,22,38));
        navHome = navButton("Tổng quan");
        navCampaign = navButton("Chiến dịch");
        navHistory = navButton("Giao dịch");
        navHome.setOnClickListener(v -> renderHome());
        navCampaign.setOnClickListener(v -> renderCampaigns());
        navHistory.setOnClickListener(v -> renderHistory());
        nav.addView(navHome, new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(navCampaign, new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(navHistory, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(nav);
        return root;
    }

    private void renderHome() {
        currentTab = 0; title.setText("Két Sắt KC"); markNav(); content.removeAllViews();
        long budget = 0, spent = 0;
        for (Campaign c : campaigns) budget += c.budget;
        for (Txn t : txns) spent += t.amount;
        long remain = budget - spent;

        LinearLayout hero = card(18, YELLOW);
        TextView eyebrow = text("SỐ DƯ KÉT SẮT", 12, Color.rgb(70,57,10), true);
        hero.addView(eyebrow);
        TextView money = text(kc(remain), 36, NAVY, true);
        money.setPadding(0, dp(5), 0, dp(4));
        hero.addView(money);
        hero.addView(text("Ngân sách " + kc(budget) + "  •  Đã dùng " + kc(spent), 13, Color.rgb(70,57,10), false));
        content.addView(hero, lpMatch(dp(16)));
        ObjectAnimator.ofFloat(hero, "alpha", 0.25f, 1f).setDuration(450).start();

        LinearLayout stats = new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(stat("Chiến dịch", String.valueOf(campaigns.size()), GREEN), new LinearLayout.LayoutParams(0, dp(92), 1));
        View spacer = new View(this); stats.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));
        stats.addView(stat("Giao dịch", String.valueOf(txns.size()), YELLOW), new LinearLayout.LayoutParams(0, dp(92), 1));
        content.addView(stats, lpMatch(dp(14)));

        sectionTitle("Tiến độ chiến dịch");
        if (campaigns.isEmpty()) empty("Chưa có chiến dịch nào.");
        for (Campaign c : campaigns) content.addView(campaignProgress(c), lpMatch(dp(10)));

        sectionTitle("Giao dịch gần đây");
        List<Txn> recent = sortedTxns();
        if (recent.isEmpty()) empty("Két sắt đang yên tĩnh đến đáng ngờ.");
        for (int i=0;i<Math.min(4,recent.size());i++) content.addView(txnRow(recent.get(i), false), lpMatch(dp(8)));
    }

    private void renderCampaigns() {
        currentTab = 1; title.setText("Chiến dịch"); markNav(); content.removeAllViews();
        Button create = button("+ Tạo két theo sự kiện", YELLOW, NAVY);
        create.setOnClickListener(v -> showCampaignDialog(null));
        content.addView(create, new LinearLayout.LayoutParams(-1, dp(54)));
        sectionTitle("Két đang quản lý");
        if (campaigns.isEmpty()) empty("Tạo chiến dịch đầu tiên để bắt đầu giữ KC.");
        for (Campaign c : campaigns) content.addView(campaignCard(c), lpMatch(dp(12)));
    }

    private void renderHistory() {
        currentTab = 2; title.setText("Giao dịch KC"); markNav(); content.removeAllViews();
        LinearLayout action = new LinearLayout(this); action.setOrientation(LinearLayout.HORIZONTAL);
        Button spend = button("Ghi chi KC", YELLOW, NAVY);
        Button income = button("Nạp ngân sách", CARD_2, TEXT);
        spend.setOnClickListener(v -> showTxnDialog(true));
        income.setOnClickListener(v -> showTxnDialog(false));
        action.addView(spend, new LinearLayout.LayoutParams(0, dp(52), 1));
        View gap = new View(this); action.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        action.addView(income, new LinearLayout.LayoutParams(0, dp(52), 1));
        content.addView(action);
        sectionTitle("Lịch sử");
        List<Txn> all = sortedTxns();
        if (all.isEmpty()) empty("Chưa có giao dịch.");
        for (Txn t : all) content.addView(txnRow(t, true), lpMatch(dp(8)));
    }

    private View campaignCard(Campaign c) {
        LinearLayout box = card(16, CARD);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(c.name, 18, TEXT, true); top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text(c.active ? "ĐANG CHẠY" : "ĐÃ KHÓA", 10, c.active ? GREEN : MUTED, true);
        badge.setPadding(dp(8), dp(5), dp(8), dp(5)); badge.setBackground(round(CARD_2, 10)); top.addView(badge);
        box.addView(top);
        TextView meta = text(c.event + "  •  " + date(c.start) + " → " + date(c.end), 12, MUTED, false); meta.setPadding(0,dp(5),0,dp(10)); box.addView(meta);
        long used = spentFor(c.id), remain = c.budget - used;
        box.addView(text("Còn " + kc(remain), 25, YELLOW, true));
        ProgressBar p = progress(c.budget == 0 ? 0 : (int)Math.min(100, used * 100 / c.budget)); box.addView(p, lpMatch(dp(10)));
        box.addView(text(kc(used) + " / " + kc(c.budget), 12, MUTED, false));
        LinearLayout row = new LinearLayout(this); row.setPadding(0,dp(12),0,0);
        Button add = button("Ghi chi", YELLOW, NAVY); add.setOnClickListener(v -> showTxnDialogFor(true,c.id));
        Button edit = button("Sửa", CARD_2, TEXT); edit.setOnClickListener(v -> showCampaignDialog(c));
        row.addView(add, new LinearLayout.LayoutParams(0,dp(44),1)); View g=new View(this);row.addView(g,new LinearLayout.LayoutParams(dp(8),1)); row.addView(edit,new LinearLayout.LayoutParams(dp(84),dp(44)));
        box.addView(row);
        return box;
    }

    private View campaignProgress(Campaign c) {
        LinearLayout box = card(14, CARD);
        LinearLayout line = new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(text(c.name, 15, TEXT, true), new LinearLayout.LayoutParams(0,-2,1));
        long used=spentFor(c.id); int pct=c.budget==0?0:(int)Math.min(100,used*100/c.budget);
        line.addView(text(pct+"%",13,pct>=90?RED:YELLOW,true)); box.addView(line);
        ProgressBar p=progress(pct); box.addView(p, lpMatch(dp(8)));
        box.addView(text("Còn "+kc(c.budget-used)+" • "+date(c.end),12,MUTED,false));
        box.setOnClickListener(v -> { currentTab=1; renderCampaigns(); });
        return box;
    }

    private View txnRow(Txn t, boolean deletable) {
        LinearLayout box = card(12, CARD);
        box.setOrientation(LinearLayout.HORIZONTAL); box.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text(t.spend ? "−" : "+", 22, t.spend ? RED : GREEN, true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(CARD_2,12));
        box.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout mid=new LinearLayout(this);mid.setOrientation(LinearLayout.VERTICAL);mid.setPadding(dp(12),0,dp(8),0);
        mid.addView(text(t.note,15,TEXT,true)); mid.addView(text(campaignName(t.campaignId)+" • "+date(t.time),11,MUTED,false)); box.addView(mid,new LinearLayout.LayoutParams(0,-2,1));
        TextView val=text((t.spend?"− ":"+ ")+kc(t.amount),14,t.spend?RED:GREEN,true); box.addView(val);
        if (deletable) box.setOnLongClickListener(v -> { confirmDeleteTxn(t); return true; });
        return box;
    }

    private void showQuickAdd() {
        new AlertDialog.Builder(this).setTitle("Thêm nhanh")
            .setItems(new String[]{"Ghi chi KC","Nạp ngân sách","Tạo chiến dịch"}, (d,w)-> {
                if(w==0)showTxnDialog(true); else if(w==1)showTxnDialog(false); else showCampaignDialog(null);
            }).show();
    }

    private void showCampaignDialog(Campaign edit) {
        LinearLayout form=form();
        EditText name=input("Tên chiến dịch",false); EditText event=input("Tên sự kiện",false); EditText budget=input("Ngân sách KC",true);
        Button start=button("Ngày bắt đầu",CARD_2,TEXT), end=button("Ngày kết thúc",CARD_2,TEXT);
        final long[] dates={System.currentTimeMillis(),System.currentTimeMillis()+7L*86400000L};
        if(edit!=null){name.setText(edit.name);event.setText(edit.event);budget.setText(String.valueOf(edit.budget));dates[0]=edit.start;dates[1]=edit.end;}
        start.setText(date(dates[0])); end.setText(date(dates[1]));
        start.setOnClickListener(v->pickDate(dates,0,start)); end.setOnClickListener(v->pickDate(dates,1,end));
        form.addView(name);form.addView(event);form.addView(budget);form.addView(start,lpMatch(dp(8)));form.addView(end,lpMatch(dp(8)));
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(edit==null?"Tạo két chiến dịch":"Sửa chiến dịch").setView(form)
            .setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim(), e=event.getText().toString().trim(); long b=parseLong(budget);
            if(n.isEmpty()||e.isEmpty()||b<=0||dates[1]<dates[0]){toast("Kiểm tra tên, sự kiện, ngân sách và ngày.");return;}
            if(edit==null) campaigns.add(new Campaign(id(),n,e,b,dates[0],dates[1],true)); else {edit.name=n;edit.event=e;edit.budget=b;edit.start=dates[0];edit.end=dates[1];}
            save();dlg.dismiss();renderCampaigns();
        })); dlg.show();
    }

    private void showTxnDialog(boolean spend){showTxnDialogFor(spend,campaigns.isEmpty()?null:campaigns.get(0).id);}
    private void showTxnDialogFor(boolean spend,String selectedId){
        if(campaigns.isEmpty()){toast("Hãy tạo chiến dịch trước.");showCampaignDialog(null);return;}
        LinearLayout form=form(); Spinner sp=new Spinner(this); List<String> names=new ArrayList<>(); int selected=0;
        for(int i=0;i<campaigns.size();i++){names.add(campaigns.get(i).name);if(campaigns.get(i).id.equals(selectedId))selected=i;}
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);sp.setAdapter(a);sp.setSelection(selected);
        EditText amount=input(spend?"Số KC đã chi":"Số KC bổ sung",true); EditText note=input(spend?"Nội dung chi":"Ghi chú",false);
        form.addView(sp);form.addView(amount);form.addView(note);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(spend?"Ghi chi KC":"Nạp ngân sách").setView(form).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            long amt=parseLong(amount);String n=note.getText().toString().trim();Campaign c=campaigns.get(sp.getSelectedItemPosition());
            if(amt<=0||n.isEmpty()){toast("Nhập số KC và nội dung.");return;}
            if(spend && amt>c.budget-spentFor(c.id)){toast("Khoản chi vượt số KC còn lại của chiến dịch.");return;}
            if(spend)txns.add(new Txn(id(),c.id,amt,n,true,System.currentTimeMillis())); else c.budget+=amt;
            save();dlg.dismiss(); if(currentTab==2)renderHistory();else renderHome();
        }));dlg.show();
    }

    private void confirmDeleteTxn(Txn t){new AlertDialog.Builder(this).setTitle("Xóa giao dịch?").setMessage(t.note+" • "+kc(t.amount)).setNegativeButton("Giữ lại",null).setPositiveButton("Xóa",(d,w)->{txns.remove(t);save();renderHistory();}).show();}

    private void pickDate(long[] dates,int idx,Button target){Calendar c=Calendar.getInstance();c.setTimeInMillis(dates[idx]);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.set(y,m,d,12,0,0);dates[idx]=n.getTimeInMillis();target.setText(date(dates[idx]));},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}

    private LinearLayout form(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),dp(8),dp(20),0);return l;}
    private EditText input(String hint,boolean number){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(NAVY);e.setHintTextColor(Color.GRAY);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setSingleLine(true);e.setPadding(dp(4),dp(12),dp(4),dp(12));return e;}
    private Button navButton(String s){Button b=button(s,Color.TRANSPARENT,MUTED);b.setTextSize(12);return b;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextColor(fg);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setBackground(round(bg,14));b.setPadding(dp(10),0,dp(10),0);return b;}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout card(int radius,int color){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(15),dp(16),dp(15));l.setBackground(round(color,radius));return l;}
    private View stat(String label,String value,int accent){LinearLayout x=card(14,CARD);TextView v=text(value,26,accent,true);x.addView(v);x.addView(text(label,12,MUTED,false));return x;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private ProgressBar progress(int pct){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(100);p.setProgress(pct);p.setProgressTintList(android.content.res.ColorStateList.valueOf(pct>=90?RED:YELLOW));p.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(CARD_2));return p;}
    private LinearLayout.LayoutParams lpMatch(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=top;return p;}
    private void sectionTitle(String s){TextView t=text(s,15,TEXT,true);t.setPadding(dp(2),dp(22),0,dp(8));content.addView(t);}
    private void empty(String s){TextView t=text(s,13,MUTED,false);t.setGravity(Gravity.CENTER);t.setPadding(dp(20),dp(30),dp(20),dp(30));t.setBackground(round(CARD,14));content.addView(t);}
    private void markNav(){Button[] bs={navHome,navCampaign,navHistory};for(int i=0;i<bs.length;i++){bs[i].setTextColor(i==currentTab?YELLOW:MUTED);bs[i].setBackground(round(i==currentTab?CARD_2:Color.TRANSPARENT,14));}}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private String kc(long v){return NumberFormat.getNumberInstance(VI).format(Math.max(0,v))+" KC";}
    private String date(long ts){return new SimpleDateFormat("dd/MM/yyyy",VI).format(new Date(ts));}
    private String id(){return Long.toHexString(System.nanoTime())+Integer.toHexString((int)(Math.random()*65535));}
    private long parseLong(EditText e){try{return Long.parseLong(e.getText().toString().replace(".","").replace(",",""));}catch(Exception ex){return 0;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private long spentFor(String id){long s=0;for(Txn t:txns)if(t.spend&&t.campaignId.equals(id))s+=t.amount;return s;}
    private String campaignName(String id){for(Campaign c:campaigns)if(c.id.equals(id))return c.name;return "Chiến dịch";}
    private List<Txn> sortedTxns(){List<Txn>x=new ArrayList<>(txns);Collections.sort(x,(a,b)->Long.compare(b.time,a.time));return x;}

    private void seedIfNeeded(){if(!campaigns.isEmpty()||getPreferences(0).getBoolean("seeded",false))return;getPreferences(0).edit().putBoolean("seeded",true).apply();}
    private void save(){try{JSONArray cs=new JSONArray();for(Campaign c:campaigns)cs.put(c.json());JSONArray ts=new JSONArray();for(Txn t:txns)ts.put(t.json());getSharedPreferences("kcff",0).edit().putString("campaigns",cs.toString()).putString("txns",ts.toString()).apply();}catch(Exception ignored){}}
    private void load(){try{String c=getSharedPreferences("kcff",0).getString("campaigns","[]"),t=getSharedPreferences("kcff",0).getString("txns","[]");JSONArray ca=new JSONArray(c);for(int i=0;i<ca.length();i++)campaigns.add(Campaign.from(ca.getJSONObject(i)));JSONArray ta=new JSONArray(t);for(int i=0;i<ta.length();i++)txns.add(Txn.from(ta.getJSONObject(i)));}catch(Exception ignored){campaigns.clear();txns.clear();}}

    static class Campaign{String id,name,event;long budget,start,end;boolean active;Campaign(String i,String n,String e,long b,long s,long en,boolean a){id=i;name=n;event=e;budget=b;start=s;end=en;active=a;}JSONObject json()throws Exception{JSONObject o=new JSONObject();o.put("id",id);o.put("name",name);o.put("event",event);o.put("budget",budget);o.put("start",start);o.put("end",end);o.put("active",active);return o;}static Campaign from(JSONObject o)throws Exception{return new Campaign(o.getString("id"),o.getString("name"),o.getString("event"),o.getLong("budget"),o.getLong("start"),o.getLong("end"),o.optBoolean("active",true));}}
    static class Txn{String id,campaignId,note;long amount,time;boolean spend;Txn(String i,String c,long a,String n,boolean s,long t){id=i;campaignId=c;amount=a;note=n;spend=s;time=t;}JSONObject json()throws Exception{JSONObject o=new JSONObject();o.put("id",id);o.put("campaignId",campaignId);o.put("amount",amount);o.put("note",note);o.put("spend",spend);o.put("time",time);return o;}static Txn from(JSONObject o)throws Exception{return new Txn(o.getString("id"),o.getString("campaignId"),o.getLong("amount"),o.getString("note"),o.getBoolean("spend"),o.getLong("time"));}}
}
