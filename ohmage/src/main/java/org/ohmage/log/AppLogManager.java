package org.ohmage.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by jaredsieling on 8/21/15.
 */
public class AppLogManager {

    private static final String PREF_SAVED_ENTRIES = "app_log_records";

    public static void logInfo(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_INFO, event, msg);
        addEntry(mContext, entry);
    }

    public static void logWarning(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_WARNING, event, msg);
        addEntry(mContext, entry);
    }

    public static void logError(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_ERROR, event, msg);
        addEntry(mContext, entry);
    }

    public synchronized static void addEntry(Context mContext, AppLogEntry entry){
        ArrayList<AppLogEntry> entries = getAllEntries(mContext);
        entries.add(entry);
        saveAllEntries(mContext, entries);
    }

    public static ArrayList<AppLogEntry> getAllEntries(Context mContext){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ArrayList<AppLogEntry> entries = new ArrayList<AppLogEntry>();
        try {
            JSONArray jsons = new JSONArray(prefs.getString(PREF_SAVED_ENTRIES, "[]"));
            for(int i = 0; i < jsons.length(); i++){
                AppLogEntry r = new AppLogEntry();
                r.fromJson(jsons.getJSONObject(i));
                entries.add(r);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return entries;
    }

    private static void saveAllEntries(Context mContext, ArrayList<AppLogEntry> entries){
        JSONArray jsons = new JSONArray();
        for(AppLogEntry it : entries){
            jsons.put(it.toJson());
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(PREF_SAVED_ENTRIES, jsons.toString()).commit();

    }

    public static void removeAllEntries(Context mContext){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(PREF_SAVED_ENTRIES, "[]").commit();
    }
}
