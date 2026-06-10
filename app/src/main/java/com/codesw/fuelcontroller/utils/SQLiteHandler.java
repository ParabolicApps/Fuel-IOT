package com.codesw.fuelcontroller.utils;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.Log;
import android.content.ContentValues;

import com.codesw.fuelcontroller.model.DbModel;
import com.codesw.fuelcontroller.utils.FirebaseSyncManager;

/**
 * SQLiteHandler manages the persistence layer for the Fuel Guard application.
 * It provides high-performance methods for logging real-time telemetry, 
 * aggregating historical usage, and generating time-series data for analytics.
 *
 * Tables:
 * - log: Raw timestamped entries of every fuel level change (Refills and Consumption).
 * - total: Daily aggregated totals used for performance-optimized charting.
 */
public class SQLiteHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "data.db";
    private static final String DB_PATH_SUFFIX = "/databases/";

    private static final String TABLE_LOG = "log";
    private static final String TABLE_TOTAL = "total";

    private static final String KEY_SR = "sr";
    private static final String KEY_TIME = "time";
    private static final String KEY_DATE = "date";
    private static final String KEY_DATA = "data";
    private static final String KEY_TYPE = "type";
    
    private final Calendar calendar = Calendar.getInstance();

    static Context mCtx;
    private static SQLiteOpenHelper dbInstance;

    public static final String TAG = "SQLiteHandler";

    public SQLiteHandler(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mCtx = context;
    }

    /**
     * Singleton accessor for the database handler.
     */
    public SQLiteOpenHelper getInstance(Context context) {
        if (dbInstance == null)
            dbInstance = new SQLiteHandler(context);
        return dbInstance;
    }


    private static String getDatabasePath() {
        return mCtx.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DATABASE_NAME;
    }

    /**
     * Initializes the database schema.
     * Creates both raw log and aggregated total tables.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOG_TABLE = "CREATE TABLE " + TABLE_LOG + "("
                + KEY_SR + " INTEGER PRIMARY KEY," + KEY_TIME + " TEXT,"
                + KEY_DATE + " TEXT ," + KEY_DATA + " TEXT,"
                + KEY_TYPE + " TEXT" + ")";
        String CREATE_TOTAL_TABLE = "CREATE TABLE " + TABLE_TOTAL + "("
                + KEY_SR + " INTEGER PRIMARY KEY,"
                + KEY_DATE + " TEXT ," + KEY_DATA + " TEXT,"
                + KEY_TYPE + " TEXT" + ")";
        db.execSQL(CREATE_LOG_TABLE);
        db.execSQL(CREATE_TOTAL_TABLE);
        Log.d(TAG, "Database tables created");
    }

    /**
     * Retrieves filtered logs based on a column condition.
     * 
     * @param condition The column name to filter by.
     * @param situation The value to match.
     * @return List of matching database models.
     */
    public ArrayList<DbModel> getLogData(String condition, String situation) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE " + condition + " = " + situation, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Returns all historical fuel change logs.
     */
    public ArrayList<DbModel> getLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Searches for logs matching a specific partial time string.
     */
    public ArrayList<DbModel> getSessions(String time) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE (time LIKE '%" + time + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Aggregates fuel totals for the past 7 days for a specific data type.
     * 
     * @param type The data category ("in" for refills, "out" for consumption).
     * @return A list of 7 daily totals in reverse chronological order.
     */
    public ArrayList<Float> getWeekly(String type) {
        ArrayList<Float> result = new ArrayList<>(Collections.nCopies(7, 0.0f));
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
        SQLiteDatabase db = this.getReadableDatabase();

        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());
            Cursor cursor = db.rawQuery("SELECT data FROM total WHERE date = ? AND type = ?", new String[]{date, type});
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result.set(i, cursor.getFloat(0));
                }
                cursor.close();
            }
            calendar.add(Calendar.DATE, -1);
        }
        db.close();
        return result;
    }

    /**
     * Default weekly refill accessor.
     */
    public ArrayList<Float> getWeekly() {
        return getWeekly("in");
    }

    /**
     * Processes hourly fuel usage for the current day.
     * Performs interpolation to handle hours with missing data points.
     * 
     * @param type Data category ("in" or "out").
     * @return List of maps containing "hour" and aggregated "value".
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<HashMap<String, Object>> getDaily(String type) {
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());

        SQLiteDatabase db = this.getReadableDatabase();
        String day = dateFormat.format(calendar.getTime());
        
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE date = ? AND type = ?", new String[]{day, type});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("time", cursor.getString(1));
                map.put("date", cursor.getString(2));
                map.put("data", cursor.getString(3));
                data.add(map);
            }
            cursor.close();
        }
        db.close();


        Map<Integer, List<Double>> hourlyData = new HashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        for (Map<String, Object> item : data) {
            LocalTime time = LocalTime.parse(item.get("time").toString(), timeFormatter);
            int hour = time.getHour();
            double dataValue = Double.parseDouble(item.get("data").toString());
            List<Double> dataList = hourlyData.get(hour);
            if (dataList == null) {
                dataList = new ArrayList<>();
                hourlyData.put(hour, dataList);
            }
            dataList.add(dataValue);
        }

        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            List<Double> dataValues = hourlyData.get(hour);

            if (dataValues != null) {
                double firstValue = dataValues.get(0);
                double lastValue = dataValues.get(dataValues.size() - 1);

                // Hour-to-hour interpolation logic
                if (lastValue == firstValue) {
                    int k = 1;
                    List<Double> lastDataValues = hourlyData.get(hour - 1);
                    while (lastDataValues == null && (hour - k) >= 0) {
                        lastDataValues = hourlyData.get(hour - k);
                        k++;
                    }
                    if (lastDataValues != null) {
                        firstValue = lastDataValues.get(lastDataValues.size() - 1);
                    }
                }
                
                double diff = lastValue - firstValue;
                HashMap<String, Object> mapdata = new HashMap<>();
                mapdata.put("hour", hour);
                mapdata.put("value", diff);
                result.add(mapdata);
            }
        }
        return result;
    }

    /**
     * Default daily refill accessor.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<HashMap<String, Object>> getDaily() {
        return getDaily("in");
    }


    /**
     * Retrieves all logs for a specific month.
     */
    public ArrayList<DbModel> getMonthly(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE (date LIKE '%" + month + "-23" + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Returns all aggregated daily totals.
     */
    public ArrayList<DbModel> getTotals() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM total", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), "", cursor.getString(1), cursor.getString(2), cursor.getString(3));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Calculates the sum total of fuel activity for a specific month.
     */
    public String getMonthlyTotal(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        double count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM total WHERE (date LIKE '%" + month + "-23" + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count = count + Double.parseDouble(cursor.getString(2));
            }
            cursor.close();
            db.close();
        }
        return String.valueOf(count);
    }

    /**
     * Inserts a new telemetry log and updates the aggregated daily total.
     * 
     * @param time Timestamp of the event.
     * @param date Date of the event.
     * @param data Raw fuel level string.
     * @param type Category ("in" or "out").
     */
    public void addLogs(String time, String date, String data, String type) {
        Log.d(TAG, "Add Log Attempt: Date=" + date + " Type=" + type);

        ContentValues values = new ContentValues();
        values.put(KEY_TIME, time); 
        values.put(KEY_DATE, date); 
        values.put(KEY_DATA, data); 
        values.put(KEY_TYPE, type); 

        // Update daily total based on delta from last known value
        if (isTotalRowExist(date, type)) {
            double lastVal = "in".equals(type) ? getLastInput() : getLastOutput();
            double currentVal = Double.parseDouble(data);
            updateLogsTotal(date, String.valueOf(currentVal - lastVal), type);
        } else {
            double lastVal = "in".equals(type) ? getLastInput() : getLastOutput();
            double currentVal = Double.parseDouble(data);
            addLogsTotal(date, String.valueOf(currentVal - lastVal), type);
        }

        SQLiteDatabase db = this.getWritableDatabase();
        long rowId = db.insert(TABLE_LOG, null, values);
        db.close();

        if (rowId >= 0) {
            FirebaseSyncManager.syncLog(String.valueOf(rowId), time, date, data, type);
        }
        FirebaseSyncManager.syncTotal(date, type, getLogsTotal(date, type));
    }

    /**
     * Verifies if an aggregated row already exists for a specific day and type.
     */
    public boolean isTotalRowExist(String date, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM total WHERE date = ? AND type = ?", new String[]{date, type});
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    /**
     * Creates a new aggregated daily entry.
     */
    public void addLogsTotal(String date, String data, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues totalValues = new ContentValues();
        totalValues.put(KEY_DATE, date);
        totalValues.put(KEY_DATA, data); 
        totalValues.put(KEY_TYPE, type); 
        db.insert(TABLE_TOTAL, null, totalValues);
    }

    /**
     * Updates an existing aggregated entry with a new delta.
     */
    public void updateLogsTotal(String date, String data, String type) {
        Calendar c = Calendar.getInstance();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues totalValues = new ContentValues();
        totalValues.put(KEY_DATE, date);
        double currentTotal = getLogsTotal(new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(c.getTimeInMillis()), type);
        totalValues.put(KEY_DATA, Double.parseDouble(data) + currentTotal); 
        totalValues.put(KEY_TYPE, type); 

        String selection = KEY_DATE + " = ? AND " + KEY_TYPE + " = ?";
        String[] selectionArgs = {date, type};
        db.update(TABLE_TOTAL, totalValues, selection, selectionArgs);
    }

    /**
     * Retrieves the specific aggregated total for a date and type.
     */
    public double getLogsTotal(String date, String type) {
        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String[] selectionArgs = {date, type};
        Cursor cursor = db.rawQuery("SELECT data FROM total WHERE date = ? AND type = ?", selectionArgs);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = Double.parseDouble(cursor.getString(0));
            }
            cursor.close();
        }
        return count;
    }

    /**
     * Check if a specific row exists in a table.
     */
    public boolean isRowExist(String tableName, String columnName, String columnValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " WHERE " + columnName + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{columnValue});
        boolean rowExists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return rowExists;
    }

    /**
     * Retrieves the most recent fuel refill level from history.
     */
    public double getLastInput() {
        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT data FROM log WHERE type = \"in\" ORDER BY sr DESC LIMIT 1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = Double.parseDouble(cursor.getString(0));
            }
            cursor.close();
        }
        return count;
    }

    /**
     * Retrieves the most recent fuel consumption level from history.
     */
    public double getLastOutput() {
        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT data FROM log WHERE type = \"out\" ORDER BY sr DESC LIMIT 1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = Double.parseDouble(cursor.getString(0));
            }
            cursor.close();
        }
        return count;
    }

    /**
     * Convenience method to get total consumption for today.
     */
    public double getTodayTotalUsage() {
        String day = new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(calendar.getTimeInMillis());
        return getLogsTotal(day, "out");
    }

    /**
     * Convenience method to get total refill volume for today.
     */
    public double getTodayTotalInput() {
        String day = new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(calendar.getTimeInMillis());
        return getLogsTotal(day, "in");
    }

    /**
     * Executes a raw SQL query safely.
     */
    public void setData(String sqlQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(sqlQuery, null);
        if (c != null) {
            c.moveToFirst();
            c.close();
        }
    }

    /**
     * Purges all historical logs.
     */
    public void deleteLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOG, null, null);
        db.delete(TABLE_TOTAL, null, null);
        db.close();
        Log.d(TAG, "Database purged.");
    }

    /**
     * Standard database upgrade procedure.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOTAL);
        onCreate(db);
    }

    /**
     * Directly inserts a log entry from a remote source (Deduplicated via sr check).
     */
    public void importLog(String sr, String time, String date, String data, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SR, sr);
        values.put(KEY_TIME, time);
        values.put(KEY_DATE, date);
        values.put(KEY_DATA, data);
        values.put(KEY_TYPE, type);
        db.insert(TABLE_LOG, null, values);
        db.close();
    }

    /**
     * Directly inserts a total entry from a remote source (Deduplicated via date+type check).
     */
    public void importTotal(String date, String data, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_DATE, date);
        values.put(KEY_DATA, data);
        values.put(KEY_TYPE, type);
        db.insert(TABLE_TOTAL, null, values);
        db.close();
    }
}
