package org.ohmage.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by jaredsieling on 8/21/15.
 */
public class AppLogManager {

    private static final String PREF_SAVED_ENTRIES = "app_log_records";
    private static final String TAG = "AppLogManager";

    public synchronized static void logInfo(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_INFO, event, msg);
        addEntry(mContext, entry);
    }

    public synchronized static void logWarning(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_WARNING, event, msg);
        addEntry(mContext, entry);
    }

    public synchronized static void logError(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_ERROR, event, msg);
        addEntry(mContext, entry);
    }

    public synchronized static void addEntry(Context mContext, AppLogEntry entry){
        Log.d(TAG, "Start addEntry()");
        ArrayList<AppLogEntry> entries = getAllEntries(mContext);
        entries.add(entry);
        saveAllEntries(mContext, entries);
        Log.d(TAG, "End addEntry()");
    }

    public synchronized static ArrayList<AppLogEntry> getAllEntries(Context mContext){
        Log.d(TAG, "Start getAllEntries()");
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

    private synchronized static void saveAllEntries(Context mContext, ArrayList<AppLogEntry> entries){
        Log.d(TAG, "Start saveAllEntries()");
        JSONArray jsons = new JSONArray();
        for(AppLogEntry it : entries){
            jsons.put(it.toJson());
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(PREF_SAVED_ENTRIES, jsons.toString()).commit();

    }

//    public synchronized static void removeAllEntries(Context mContext){
//        Log.d(TAG, "Start removeAllEntries()");
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        prefs.edit().putString(PREF_SAVED_ENTRIES, "[]").commit();
//    }

    public synchronized static void removeEntries(Context mContext, ArrayList<AppLogEntry> removeEntries){
        ArrayList<AppLogEntry> entries = AppLogManager.getAllEntries(mContext);
        entries.removeAll(removeEntries);
        AppLogManager.saveAllEntries(mContext, entries);
    }
}
