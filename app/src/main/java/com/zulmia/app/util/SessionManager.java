package com.zulmia.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "session_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_INVENTORY_ID = "inventory_id";
    private static final String KEY_INVENTORY_NAME = "inventory_name";
    private static final String KEY_DEFAULT_LOCATION = "default_location";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void setInventory(long id, String name) {
        prefs.edit().putLong(KEY_INVENTORY_ID, id).putString(KEY_INVENTORY_NAME, name).apply();
    }

    public void setInventory(long id, String name, String defaultLocation) {
        prefs.edit()
                .putLong(KEY_INVENTORY_ID, id)
                .putString(KEY_INVENTORY_NAME, name)
                .putString(KEY_DEFAULT_LOCATION, defaultLocation)
                .apply();
    }

    public long getInventoryId() {
        return prefs.getLong(KEY_INVENTORY_ID, -1);
    }

    public String getInventoryName() {
        return prefs.getString(KEY_INVENTORY_NAME, null);
    }

    public String getDefaultLocation() {
        return prefs.getString(KEY_DEFAULT_LOCATION, "");
    }

    public void clearInventory() {
        prefs.edit().remove(KEY_INVENTORY_ID).remove(KEY_INVENTORY_NAME).remove(KEY_DEFAULT_LOCATION).apply();
    }

    public void clearUser() {
        prefs.edit().remove(KEY_USERNAME).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_USERNAME).remove(KEY_INVENTORY_ID).remove(KEY_INVENTORY_NAME).remove(KEY_DEFAULT_LOCATION).apply();
    }
}


