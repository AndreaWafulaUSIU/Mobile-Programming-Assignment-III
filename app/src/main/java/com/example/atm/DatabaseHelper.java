package com.example.atm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * All data for the ATM system lives here (SQLite).
 * Tables: accounts, transactions. PINs are stored hashed, never as plain text.
 * Money is kept as whole Kenya shillings (long) to avoid rounding errors.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "atm.db";
    private static final int DB_VERSION = 1;

    // Login outcomes
    public static final int LOGIN_OK = 0;
    public static final int LOGIN_NO_ACCOUNT = 1;
    public static final int LOGIN_WRONG_PIN = 2;
    public static final int LOGIN_LOCKED = 3;

    public static final int MAX_ATTEMPTS = 3;          // wrong PINs allowed
    public static final long LOCK_MILLIS = 60 * 1000;  // lockout lasts 60 seconds

    /** Result of a login attempt. */
    public static class LoginResult {
        public int status;
        public int attemptsLeft;
        public long secondsLeft;
    }

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts ("
                + "acc_no INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "pin_hash TEXT NOT NULL, "
                + "balance INTEGER NOT NULL, "
                + "failed_attempts INTEGER NOT NULL DEFAULT 0, "
                + "locked_until INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "acc_no INTEGER NOT NULL, "
                + "type TEXT NOT NULL, "
                + "amount INTEGER NOT NULL, "          // positive = money in, negative = money out
                + "balance_after INTEGER NOT NULL, "
                + "created TEXT NOT NULL)");

        // Demo accounts so the app can be tested straight away
        insertAccount(db, 1001, "Demo User One", "1234", 50000);
        insertAccount(db, 1002, "Demo User Two", "4321", 20000);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS accounts");
        onCreate(db);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** SHA-256 of the PIN (with a fixed prefix) written as hex text. */
    public static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(("atm-salt:" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertAccount(SQLiteDatabase db, long accNo, String name, String pin, long opening) {
        ContentValues v = new ContentValues();
        v.put("acc_no", accNo);
        v.put("name", name);
        v.put("pin_hash", hashPin(pin));
        v.put("balance", opening);
        db.insert("accounts", null, v);
        logTransaction(db, accNo, "OPENING BALANCE", opening);
    }

    /** Records a transaction. amount is positive for money in, negative for money out. */
    private static void logTransaction(SQLiteDatabase db, long accNo, String type, long amount) {
        ContentValues v = new ContentValues();
        v.put("acc_no", accNo);
        v.put("type", type);
        v.put("amount", amount);
        v.put("balance_after", queryBalance(db, accNo));
        v.put("created", new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(new Date()));
        db.insert("transactions", null, v);
    }

    private static long queryBalance(SQLiteDatabase db, long accNo) {
        Cursor c = db.rawQuery("SELECT balance FROM accounts WHERE acc_no = ?",
                new String[]{String.valueOf(accNo)});
        try {
            return c.moveToFirst() ? c.getLong(0) : -1;
        } finally {
            c.close();
        }
    }

    /** Takes money out only if the balance covers it. Returns true if it worked. */
    private static boolean debit(SQLiteDatabase db, long accNo, long amount) {
        SQLiteStatement st = db.compileStatement(
                "UPDATE accounts SET balance = balance - ? WHERE acc_no = ? AND balance >= ?");
        try {
            st.bindLong(1, amount);
            st.bindLong(2, accNo);
            st.bindLong(3, amount);
            return st.executeUpdateDelete() == 1;
        } finally {
            st.close();
        }
    }

    private static void credit(SQLiteDatabase db, long accNo, long amount) {
        db.execSQL("UPDATE accounts SET balance = balance + ? WHERE acc_no = ?",
                new Object[]{amount, accNo});
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    public boolean accountExists(long accNo) {
        return queryBalance(getReadableDatabase(), accNo) >= 0;
    }

    /** Returns the balance, or -1 if the account does not exist. */
    public long getBalance(long accNo) {
        return queryBalance(getReadableDatabase(), accNo);
    }

    public String getName(long accNo) {
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM accounts WHERE acc_no = ?",
                new String[]{String.valueOf(accNo)});
        try {
            return c.moveToFirst() ? c.getString(0) : "";
        } finally {
            c.close();
        }
    }

    /** Creates a new account and returns its account number. */
    public long createAccount(String name, String pin, long openingDeposit) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long next = DatabaseUtils.longForQuery(db,
                    "SELECT IFNULL(MAX(acc_no), 1000) + 1 FROM accounts", null);
            insertAccount(db, next, name, pin, openingDeposit);
            db.setTransactionSuccessful();
            return next;
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------------
    // Login with lockout: 3 wrong PINs lock the account for 60 seconds
    // ------------------------------------------------------------------

    public LoginResult login(long accNo, String pin) {
        LoginResult result = new LoginResult();
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(accNo)};

        Cursor c = db.rawQuery(
                "SELECT pin_hash, failed_attempts, locked_until FROM accounts WHERE acc_no = ?", args);
        try {
            if (!c.moveToFirst()) {
                result.status = LOGIN_NO_ACCOUNT;
                return result;
            }
            String storedHash = c.getString(0);
            int failed = c.getInt(1);
            long lockedUntil = c.getLong(2);
            long now = System.currentTimeMillis();

            if (lockedUntil > now) {
                result.status = LOGIN_LOCKED;
                result.secondsLeft = (lockedUntil - now + 999) / 1000;
                return result;
            }

            ContentValues v = new ContentValues();
            if (storedHash.equals(hashPin(pin))) {
                v.put("failed_attempts", 0);
                v.put("locked_until", 0);
                db.update("accounts", v, "acc_no = ?", args);
                result.status = LOGIN_OK;
                return result;
            }

            failed++;
            if (failed >= MAX_ATTEMPTS) {
                v.put("failed_attempts", 0);
                v.put("locked_until", now + LOCK_MILLIS);
                db.update("accounts", v, "acc_no = ?", args);
                result.status = LOGIN_LOCKED;
                result.secondsLeft = LOCK_MILLIS / 1000;
            } else {
                v.put("failed_attempts", failed);
                db.update("accounts", v, "acc_no = ?", args);
                result.status = LOGIN_WRONG_PIN;
                result.attemptsLeft = MAX_ATTEMPTS - failed;
            }
            return result;
        } finally {
            c.close();
        }
    }

    // ------------------------------------------------------------------
    // Money movements (each one is a single all-or-nothing database transaction)
    // ------------------------------------------------------------------

    public boolean deposit(long accNo, long amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            credit(db, accNo, amount);
            logTransaction(db, accNo, "DEPOSIT", amount);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Returns false (and changes nothing) if the balance is too low. */
    public boolean withdraw(long accNo, long amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!debit(db, accNo, amount)) {
                return false;
            }
            logTransaction(db, accNo, "WITHDRAWAL", -amount);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Returns false (and changes nothing) if the balance is too low or the target is missing. */
    public boolean transfer(long fromAcc, long toAcc, long amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (fromAcc == toAcc || queryBalance(db, toAcc) < 0) {
                return false;
            }
            if (!debit(db, fromAcc, amount)) {
                return false;
            }
            credit(db, toAcc, amount);
            logTransaction(db, fromAcc, "TRANSFER TO " + toAcc, -amount);
            logTransaction(db, toAcc, "TRANSFER FROM " + fromAcc, amount);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------------
    // Mini statement and PIN change
    // ------------------------------------------------------------------

    /** The most recent transactions, newest first, each as a ready-to-display text. */
    public List<String> getMiniStatement(long accNo, int limit) {
        List<String> lines = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT type, amount, balance_after, created FROM transactions "
                        + "WHERE acc_no = ? ORDER BY id DESC LIMIT ?",
                new String[]{String.valueOf(accNo), String.valueOf(limit)});
        try {
            while (c.moveToNext()) {
                String type = c.getString(0);
                long amount = c.getLong(1);
                long balanceAfter = c.getLong(2);
                String when = c.getString(3);
                String sign = amount >= 0 ? "+" : "-";
                lines.add(when + "\n" + type + "   " + sign + Util.money(Math.abs(amount))
                        + "\nBalance: " + Util.money(balanceAfter));
            }
        } finally {
            c.close();
        }
        return lines;
    }

    /** Returns false if the old PIN is wrong. */
    public boolean changePin(long accNo, String oldPin, String newPin) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(accNo)};
        Cursor c = db.rawQuery("SELECT pin_hash FROM accounts WHERE acc_no = ?", args);
        try {
            if (!c.moveToFirst() || !c.getString(0).equals(hashPin(oldPin))) {
                return false;
            }
        } finally {
            c.close();
        }
        ContentValues v = new ContentValues();
        v.put("pin_hash", hashPin(newPin));
        db.update("accounts", v, "acc_no = ?", args);
        return true;
    }
}
