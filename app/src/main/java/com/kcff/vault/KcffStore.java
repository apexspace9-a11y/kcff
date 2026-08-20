package com.kcff.vault;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class KcffStore {
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_SPEND = "SPEND";

    public static final class Vault {
        public long id;
        public String name;
        public String event;
        public int target;
        public int balance;
        public long createdAt;
        public long endAt;

        public Vault(long id, String name, String event, int target, int balance, long createdAt, long endAt) {
            this.id = id;
            this.name = name;
            this.event = event;
            this.target = target;
            this.balance = balance;
            this.createdAt = createdAt;
            this.endAt = endAt;
        }
    }

    public static final class Tx {
        public long id;
        public String type;
        public int amount;
        public String note;
        public long vaultId;
        public long time;

        public Tx(long id, String type, int amount, String note, long vaultId, long time) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.note = note;
            this.vaultId = vaultId;
            this.time = time;
        }
    }

    private static final String PREFS = "kcff_store_v1";
    private static final String KEY_VAULTS = "vaults";
    private static final String KEY_TX = "transactions";

    private final SharedPreferences prefs;
    private final ArrayList<Vault> vaults = new ArrayList<>();
    private final ArrayList<Tx> transactions = new ArrayList<>();

    public KcffStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public List<Vault> vaults() {
        return Collections.unmodifiableList(vaults);
    }

    public List<Tx> transactions() {
        ArrayList<Tx> out = new ArrayList<>(transactions);
        out.sort((a, b) -> Long.compare(b.time, a.time));
        return out;
    }

    public Vault findVault(long id) {
        for (Vault vault : vaults) {
            if (vault.id == id) return vault;
        }
        return null;
    }

    public void createVault(String name, String event, int target, long endAt) {
        long now = System.currentTimeMillis();
        long id = now;
        while (findVault(id) != null) id++;
        vaults.add(new Vault(id, name.trim(), event.trim(), Math.max(1, target), 0, now, endAt));
        save();
    }

    public void updateVault(long id, String name, String event, int target, long endAt) {
        Vault vault = findVault(id);
        if (vault == null) return;
        vault.name = name.trim();
        vault.event = event.trim();
        vault.target = Math.max(1, target);
        vault.endAt = endAt;
        save();
    }

    public void deleteVault(long id) {
        for (int i = vaults.size() - 1; i >= 0; i--) {
            if (vaults.get(i).id == id) vaults.remove(i);
        }
        save();
    }

    public boolean addTransaction(String type, int amount, String note, long vaultId) {
        if (amount <= 0) return false;
        Vault vault = vaultId == 0 ? null : findVault(vaultId);
        if (TYPE_DEPOSIT.equals(type) && vault == null) return false;

        if (vault != null) {
            if (TYPE_DEPOSIT.equals(type)) {
                vault.balance += amount;
            } else {
                if (amount > vault.balance) return false;
                vault.balance -= amount;
            }
        }

        long now = System.currentTimeMillis();
        long id = now;
        while (findTx(id) != null) id++;
        transactions.add(new Tx(id, type, amount, note == null ? "" : note.trim(), vaultId, now));
        save();
        return true;
    }

    public void deleteTransaction(long id) {
        Tx tx = findTx(id);
        if (tx == null) return;

        Vault vault = tx.vaultId == 0 ? null : findVault(tx.vaultId);
        if (vault != null) {
            if (TYPE_DEPOSIT.equals(tx.type)) {
                vault.balance = Math.max(0, vault.balance - tx.amount);
            } else {
                vault.balance += tx.amount;
            }
        }
        transactions.remove(tx);
        save();
    }

    public int totalBalance() {
        int total = 0;
        for (Vault vault : vaults) total += vault.balance;
        return total;
    }

    public int totalTarget() {
        int total = 0;
        for (Vault vault : vaults) total += vault.target;
        return total;
    }

    public int totalSpent() {
        int total = 0;
        for (Tx tx : transactions) if (TYPE_SPEND.equals(tx.type)) total += tx.amount;
        return total;
    }

    public int spentSince(long from) {
        int total = 0;
        for (Tx tx : transactions) {
            if (TYPE_SPEND.equals(tx.type) && tx.time >= from) total += tx.amount;
        }
        return total;
    }

    private Tx findTx(long id) {
        for (Tx tx : transactions) if (tx.id == id) return tx;
        return null;
    }

    private void load() {
        vaults.clear();
        transactions.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_VAULTS, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                vaults.add(new Vault(
                        o.getLong("id"),
                        o.optString("name", "Két KC"),
                        o.optString("event", ""),
                        o.optInt("target", 1),
                        o.optInt("balance", 0),
                        o.optLong("createdAt", System.currentTimeMillis()),
                        o.optLong("endAt", 0)
                ));
            }
            JSONArray b = new JSONArray(prefs.getString(KEY_TX, "[]"));
            for (int i = 0; i < b.length(); i++) {
                JSONObject o = b.getJSONObject(i);
                transactions.add(new Tx(
                        o.getLong("id"),
                        o.optString("type", TYPE_SPEND),
                        o.optInt("amount", 0),
                        o.optString("note", ""),
                        o.optLong("vaultId", 0),
                        o.optLong("time", System.currentTimeMillis())
                ));
            }
        } catch (Exception ignored) {
            vaults.clear();
            transactions.clear();
        }
        vaults.sort(Comparator.comparingLong(v -> v.createdAt));
    }

    private void save() {
        try {
            JSONArray a = new JSONArray();
            for (Vault vault : vaults) {
                JSONObject o = new JSONObject();
                o.put("id", vault.id);
                o.put("name", vault.name);
                o.put("event", vault.event);
                o.put("target", vault.target);
                o.put("balance", vault.balance);
                o.put("createdAt", vault.createdAt);
                o.put("endAt", vault.endAt);
                a.put(o);
            }
            JSONArray b = new JSONArray();
            for (Tx tx : transactions) {
                JSONObject o = new JSONObject();
                o.put("id", tx.id);
                o.put("type", tx.type);
                o.put("amount", tx.amount);
                o.put("note", tx.note);
                o.put("vaultId", tx.vaultId);
                o.put("time", tx.time);
                b.put(o);
            }
            prefs.edit()
                    .putString(KEY_VAULTS, a.toString())
                    .putString(KEY_TX, b.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }
}
