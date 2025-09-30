package com.zulmia.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "zulmia.db";
    public static final int DATABASE_VERSION = 4;

    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    public static final String TABLE_INVENTORIES = "inventories";
    public static final String COL_INV_ID = "id";
    public static final String COL_INV_NAME = "name";
    public static final String COL_INV_USER = "user";
    public static final String COL_INV_DATE = "created_at";
    public static final String COL_INV_STATUS = "status"; // 0=open, 1=completed
    public static final String COL_INV_DEFAULT_LOCATION = "default_location";
    public static final String COL_INV_CSV_NAME = "csv_name";

    public static final String TABLE_SCANS = "scans";
    public static final String COL_SCAN_ID = "id";
    public static final String COL_SCAN_INV_ID = "inventory_id";
    public static final String COL_SCAN_LOCATION = "location";
    public static final String COL_SCAN_SHELF = "shelf";
    public static final String COL_SCAN_CODE = "code";
    public static final String COL_SCAN_QTY = "qty";
    public static final String COL_SCAN_USER = "user";
    public static final String COL_SCAN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INVENTORIES + " (" +
                COL_INV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_INV_NAME + " TEXT NOT NULL, " +
                COL_INV_USER + " TEXT NOT NULL, " +
                COL_INV_DATE + " INTEGER NOT NULL, " +
                COL_INV_STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                COL_INV_DEFAULT_LOCATION + " TEXT DEFAULT '', " +
                COL_INV_CSV_NAME + " TEXT )");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SCANS + " (" +
                COL_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SCAN_INV_ID + " INTEGER NOT NULL, " +
                COL_SCAN_LOCATION + " TEXT, " +
                COL_SCAN_SHELF + " TEXT, " +
                COL_SCAN_CODE + " TEXT NOT NULL, " +
                COL_SCAN_QTY + " INTEGER NOT NULL DEFAULT 1, " +
                COL_SCAN_USER + " TEXT NOT NULL, " +
                COL_SCAN_TIMESTAMP + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_SCAN_INV_ID + ") REFERENCES " + TABLE_INVENTORIES + "(" + COL_INV_ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_INVENTORIES + " ADD COLUMN " + COL_INV_STATUS + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_INVENTORIES + " ADD COLUMN " + COL_INV_DEFAULT_LOCATION + " TEXT DEFAULT ''");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_INVENTORIES + " ADD COLUMN " + COL_INV_CSV_NAME + " TEXT");
        }
    }
}


