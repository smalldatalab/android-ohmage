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
public class AppLogManager{

    private static final String PREF_SAVED_ENTRIES = "app_log_records";
    private static final String TAG = "AppLogManager";
    private static final String REMOVE_ENTRY = "remove";
    private static final String ADD_ENTRY = "add";

    private static AppLogManager instance;

    public static AppLogManager getInstance(){
        if(instance == null) {
            instance = new AppLogManager();
        }
        return instance;
    }

    public void logInfo(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_INFO, event, msg);
        addEntry(mContext, entry);
    }

    public void logWarning(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_WARNING, event, msg);
        addEntry(mContext, entry);
    }

    public void logError(Context mContext, String event, String msg){
        AppLogEntry entry = new AppLogEntry(AppLogEntry.LEVEL_ERROR, event, msg);
        addEntry(mContext, entry);
    }

    public void addEntry(Context mContext, AppLogEntry entry){
        updateEntries(mContext, entry, ADD_ENTRY);
    }

    public ArrayList<AppLogEntry> getAllEntries(Context mContext){
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

    private void saveAllEntries(Context mContext, ArrayList<AppLogEntry> entries){
        JSONArray jsons = new JSONArray();
        for(AppLogEntry it : entries){
            jsons.put(it.toJson());
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(PREF_SAVED_ENTRIES, jsons.toString()).commit();

    }

    // IMPORTANT: Do add and remove methods in here, so they are synchronized with each other
    private synchronized void updateEntries(Context mContext, AppLogEntry entry, String action){
        ArrayList<AppLogEntry> entries = getAllEntries(mContext);
        if(action == ADD_ENTRY){
            Log.d(TAG, "Adding log entry " + entry.event);
            entries.add(entry);
        } else if(action == REMOVE_ENTRY){
            Log.d(TAG, "Removing log entry " + entry.event);
            entries.remove(entry);
        }
        saveAllEntries(mContext, entries);
    }

//    public synchronized static void removeAllEntries(Context mContext){
//        Log.d(TAG, "Start removeAllEntries()");
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        prefs.edit().putString(PREF_SAVED_ENTRIES, "[]").commit();
//    }

    public void removeEntries(Context mContext, ArrayList<AppLogEntry> removeEntries){
        for(AppLogEntry entry : removeEntries) {
            updateEntries(mContext, entry, REMOVE_ENTRY);
        }
    }
}
