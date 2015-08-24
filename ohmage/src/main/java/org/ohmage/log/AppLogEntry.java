package org.ohmage.log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.models.OmhDataPointHeader;
import org.ohmage.models.SchemaId;

import java.util.UUID;

/**
 * Created by jaredsieling on 8/21/15.
 */
public class AppLogEntry {

    // Main fields used for logging.
    String level;
    String event;
    String msg;
    DateTime logTime;

    public static final String LEVEL_INFO = "info";
    public static final String LEVEL_WARNING = "warning";
    public static final String LEVEL_ERROR = "error";

    private static final String TAG = "AppLog";

    private static final String KEY_LEVEL = "level";
    private static final String KEY_EVENT = "event";
    private static final String KEY_MSG = "msg";
    private static final String KEY_LOG_TIME = "log_time";

    public AppLogEntry() {
    }

    public AppLogEntry(String level, String event, String msg) {
        this.level = level;
        this.event = event;
        this.msg = msg;
        this.logTime = DateTime.now();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_LEVEL, level);
            json.put(KEY_EVENT, event);
            json.put(KEY_MSG, msg);

            DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withOffsetParsed();
            String timestamp = dateTimeFormatter.print(logTime);
            json.put(KEY_LOG_TIME, timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public void fromJson(JSONObject json) {
        try {
            this.level = json.getString(KEY_LEVEL);
            this.event = json.getString(KEY_EVENT);
            this.msg = json.getString(KEY_MSG);

            DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withOffsetParsed();
            this.logTime = DateTime.parse(json.getString(KEY_LOG_TIME), dateTimeFormatter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public AppLogTypedOutput toOmhTypedOutput() {
        JsonObject body = new JsonObject();
        body.addProperty(KEY_LEVEL, level);
        body.addProperty(KEY_EVENT, event);
        body.addProperty(KEY_MSG, msg);


        OmhDataPointHeader header = new OmhDataPointHeader();
        header.schemaId = new SchemaId("io.smalldata:app-log", "1");
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withOffsetParsed();
        header.creationDateTime = dateTimeFormatter.print(logTime);
        header.id = UUID.randomUUID().toString();

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        JsonObject headerJson = (JsonObject) gson.toJsonTree(header);
        AppLogTypedOutput point = new AppLogTypedOutput(headerJson, body);
        return point;
    }

}
