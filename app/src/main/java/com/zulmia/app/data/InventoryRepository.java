package com.zulmia.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryRepository {
    private final DatabaseHelper dbHelper;

    public InventoryRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public boolean userExists() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0) > 0;
            }
            return false;
        } finally {
            c.close();
        }
    }

    public boolean createUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USERNAME, username);
        values.put(DatabaseHelper.COL_PASSWORD, password);
        long id = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        return id != -1;
    }

    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USERNAME + "=? AND " + DatabaseHelper.COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public long createInventory(String name, String user, long createdAtMs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_INV_NAME, name);
        values.put(DatabaseHelper.COL_INV_USER, user);
        values.put(DatabaseHelper.COL_INV_DATE, createdAtMs);
        values.put(DatabaseHelper.COL_INV_STATUS, 0);
        return db.insert(DatabaseHelper.TABLE_INVENTORIES, null, values);
    }

    public long createInventory(String name, String user, long createdAtMs, String defaultLocation) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_INV_NAME, name);
        values.put(DatabaseHelper.COL_INV_USER, user);
        values.put(DatabaseHelper.COL_INV_DATE, createdAtMs);
        values.put(DatabaseHelper.COL_INV_STATUS, 0);
        values.put(DatabaseHelper.COL_INV_DEFAULT_LOCATION, defaultLocation == null ? "" : defaultLocation);
        return db.insert(DatabaseHelper.TABLE_INVENTORIES, null, values);
    }

    public void setInventoryCsvName(long inventoryId, String csvName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_INV_CSV_NAME, csvName);
        db.update(DatabaseHelper.TABLE_INVENTORIES, v, DatabaseHelper.COL_INV_ID + "=?", new String[]{String.valueOf(inventoryId)});
    }

    public String getInventoryCsvName(long inventoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_INVENTORIES,
                new String[]{DatabaseHelper.COL_INV_CSV_NAME},
                DatabaseHelper.COL_INV_ID + "=?",
                new String[]{String.valueOf(inventoryId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        } finally {
            c.close();
        }
    }

    public boolean inventoryExists(String name, String user) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_INVENTORIES,
                new String[]{DatabaseHelper.COL_INV_ID},
                DatabaseHelper.COL_INV_NAME + "=? AND " + DatabaseHelper.COL_INV_USER + "=?",
                new String[]{name, user}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public void setInventoryStatus(long inventoryId, int status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_INV_STATUS, status);
        db.update(DatabaseHelper.TABLE_INVENTORIES, v, DatabaseHelper.COL_INV_ID + "=?", new String[]{String.valueOf(inventoryId)});
    }

    public static class InventoryRow {
        public long id;
        public String name;
        public String user;
        public long createdAtMs;
        public int status;
        public String defaultLocation;
        public String csvName;
    }

    public List<InventoryRow> getOpenInventories(String user) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_INVENTORIES,
                new String[]{DatabaseHelper.COL_INV_ID, DatabaseHelper.COL_INV_NAME, DatabaseHelper.COL_INV_USER, DatabaseHelper.COL_INV_DATE, DatabaseHelper.COL_INV_STATUS, DatabaseHelper.COL_INV_DEFAULT_LOCATION, DatabaseHelper.COL_INV_CSV_NAME},
                DatabaseHelper.COL_INV_USER + "=? AND " + DatabaseHelper.COL_INV_STATUS + "=0",
                new String[]{user}, null, null, DatabaseHelper.COL_INV_DATE + " DESC");
        List<InventoryRow> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                InventoryRow r = new InventoryRow();
                r.id = c.getLong(0);
                r.name = c.getString(1);
                r.user = c.getString(2);
                r.createdAtMs = c.getLong(3);
                r.status = c.getInt(4);
                r.defaultLocation = c.getString(5);
                r.csvName = c.getString(6);
                list.add(r);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public List<InventoryRow> getFinishedInventories(String user) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_INVENTORIES,
                new String[]{DatabaseHelper.COL_INV_ID, DatabaseHelper.COL_INV_NAME, DatabaseHelper.COL_INV_USER, DatabaseHelper.COL_INV_DATE, DatabaseHelper.COL_INV_STATUS, DatabaseHelper.COL_INV_DEFAULT_LOCATION, DatabaseHelper.COL_INV_CSV_NAME},
                DatabaseHelper.COL_INV_USER + "=? AND " + DatabaseHelper.COL_INV_STATUS + "=1",
                new String[]{user}, null, null, DatabaseHelper.COL_INV_DATE + " DESC");
        List<InventoryRow> list = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                InventoryRow r = new InventoryRow();
                r.id = c.getLong(0);
                r.name = c.getString(1);
                r.user = c.getString(2);
                r.createdAtMs = c.getLong(3);
                r.status = c.getInt(4);
                r.defaultLocation = c.getString(5);
                r.csvName = c.getString(6);
                list.add(r);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public long upsertScan(long inventoryId, String location, String shelf, String code, String user, long timestampMs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query(DatabaseHelper.TABLE_SCANS,
                new String[]{DatabaseHelper.COL_SCAN_ID, DatabaseHelper.COL_SCAN_QTY},
                DatabaseHelper.COL_SCAN_INV_ID + "=? AND " +
                        DatabaseHelper.COL_SCAN_LOCATION + "=? AND " +
                        DatabaseHelper.COL_SCAN_SHELF + "=? AND " +
                        DatabaseHelper.COL_SCAN_CODE + "=?",
                new String[]{String.valueOf(inventoryId), location, shelf, code}, null, null, null);
        try {
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                int qty = c.getInt(1) + 1;
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COL_SCAN_QTY, qty);
                values.put(DatabaseHelper.COL_SCAN_TIMESTAMP, timestampMs);
                db.update(DatabaseHelper.TABLE_SCANS, values, DatabaseHelper.COL_SCAN_ID + "=?", new String[]{String.valueOf(id)});
                return id;
            }
        } finally {
            c.close();
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SCAN_INV_ID, inventoryId);
        values.put(DatabaseHelper.COL_SCAN_LOCATION, location);
        values.put(DatabaseHelper.COL_SCAN_SHELF, shelf);
        values.put(DatabaseHelper.COL_SCAN_CODE, code);
        values.put(DatabaseHelper.COL_SCAN_QTY, 1);
        values.put(DatabaseHelper.COL_SCAN_USER, user);
        values.put(DatabaseHelper.COL_SCAN_TIMESTAMP, timestampMs);
        return db.insert(DatabaseHelper.TABLE_SCANS, null, values);
    }

    public static class AggregatedScanRow {
        public String code;
        public int qty;
        public String location;
        public String shelf;
        public String user;
        public long timestampMs;
    }

    public List<AggregatedScanRow> getAggregatedScans(long inventoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABLE_SCANS,
                new String[]{DatabaseHelper.COL_SCAN_CODE, DatabaseHelper.COL_SCAN_QTY, DatabaseHelper.COL_SCAN_LOCATION, DatabaseHelper.COL_SCAN_SHELF, DatabaseHelper.COL_SCAN_USER, DatabaseHelper.COL_SCAN_TIMESTAMP},
                DatabaseHelper.COL_SCAN_INV_ID + "=?",
                new String[]{String.valueOf(inventoryId)}, null, null, DatabaseHelper.COL_SCAN_TIMESTAMP + " DESC");
        List<AggregatedScanRow> rows = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                AggregatedScanRow row = new AggregatedScanRow();
                row.code = c.getString(0);
                row.qty = c.getInt(1);
                row.location = c.getString(2);
                row.shelf = c.getString(3);
                row.user = c.getString(4);
                row.timestampMs = c.getLong(5);
                rows.add(row);
            }
        } finally {
            c.close();
        }
        return rows;
    }

    public void updateScanQuantity(long inventoryId, String location, String shelf, String code, int newQty, long timestampMs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (newQty <= 0) {
            db.delete(DatabaseHelper.TABLE_SCANS,
                    DatabaseHelper.COL_SCAN_INV_ID + "=? AND " +
                            DatabaseHelper.COL_SCAN_LOCATION + "=? AND " +
                            DatabaseHelper.COL_SCAN_SHELF + "=? AND " +
                            DatabaseHelper.COL_SCAN_CODE + "=?",
                    new String[]{String.valueOf(inventoryId), location, shelf, code});
            return;
        }
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_SCAN_QTY, newQty);
        // Do NOT update timestamp on manual qty change to avoid reordering to top
        db.update(DatabaseHelper.TABLE_SCANS, v,
                DatabaseHelper.COL_SCAN_INV_ID + "=? AND " +
                        DatabaseHelper.COL_SCAN_LOCATION + "=? AND " +
                        DatabaseHelper.COL_SCAN_SHELF + "=? AND " +
                        DatabaseHelper.COL_SCAN_CODE + "=?",
                new String[]{String.valueOf(inventoryId), location, shelf, code});
    }

    public void updateScanCode(long inventoryId, String location, String shelf, String oldCode, String newCode) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_SCAN_CODE, newCode);
        db.update(DatabaseHelper.TABLE_SCANS, v,
                DatabaseHelper.COL_SCAN_INV_ID + "=? AND " +
                        DatabaseHelper.COL_SCAN_LOCATION + "=? AND " +
                        DatabaseHelper.COL_SCAN_SHELF + "=? AND " +
                        DatabaseHelper.COL_SCAN_CODE + "=?",
                new String[]{String.valueOf(inventoryId), location, shelf, oldCode});
    }

    public void deleteScan(long inventoryId, String location, String shelf, String code) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_SCANS,
                DatabaseHelper.COL_SCAN_INV_ID + "=? AND " +
                        DatabaseHelper.COL_SCAN_LOCATION + "=? AND " +
                        DatabaseHelper.COL_SCAN_SHELF + "=? AND " +
                        DatabaseHelper.COL_SCAN_CODE + "=?",
                new String[]{String.valueOf(inventoryId), location, shelf, code});
    }

    public int clearFinishedInventories(String user) {
        if (user == null) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Find finished inventory ids for this user
        Cursor c = db.query(DatabaseHelper.TABLE_INVENTORIES,
                new String[]{DatabaseHelper.COL_INV_ID},
                DatabaseHelper.COL_INV_USER + "=? AND " + DatabaseHelper.COL_INV_STATUS + "=1",
                new String[]{user}, null, null, null);
        int count = 0;
        try {
            while (c.moveToNext()) {
                long invId = c.getLong(0);
                db.delete(DatabaseHelper.TABLE_SCANS, DatabaseHelper.COL_SCAN_INV_ID + "=?", new String[]{String.valueOf(invId)});
                db.delete(DatabaseHelper.TABLE_INVENTORIES, DatabaseHelper.COL_INV_ID + "=?", new String[]{String.valueOf(invId)});
                count++;
            }
        } finally {
            c.close();
        }
        return count;
    }

    public int getInventoryTotalQuantity(long inventoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COL_SCAN_QTY + ") FROM " + DatabaseHelper.TABLE_SCANS +
                        " WHERE " + DatabaseHelper.COL_SCAN_INV_ID + "=?",
                new String[]{String.valueOf(inventoryId)});
        try {
            if (c.moveToFirst()) {
                if (c.isNull(0)) return 0;
                return c.getInt(0);
            }
            return 0;
        } finally {
            c.close();
        }
    }

    public void deleteInventory(long inventoryId, boolean finished) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_SCANS, DatabaseHelper.COL_SCAN_INV_ID + "=?", new String[]{String.valueOf(inventoryId)});
        db.delete(DatabaseHelper.TABLE_INVENTORIES, DatabaseHelper.COL_INV_ID + "=?", new String[]{String.valueOf(inventoryId)});
    }
}


