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
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.Log;
import android.content.ContentValues;

import com.codesw.fuelcontroller.model.DbModel;

/**
 * SQLite Handler used to get Daily monthly Weekly logs
 * Set data from CheckerService
 */
public class SQLiteHandler extends SQLiteOpenHelper {

    // Database file info
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "data.db";
    private static final String DB_PATH_SUFFIX = "/databases/";


    // Log table name
    private static final String TABLE_LOG = "log";
    private static final String TABLE_TOTAL = "total";


    // Log Table Columns names
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

    public SQLiteOpenHelper getInstance(Context context) {
        if (dbInstance == null)
            dbInstance = new SQLiteHandler(context);
        return dbInstance;
    }


    private static String getDatabasePath() {
        return mCtx.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DATABASE_NAME;
    }

    // Creating Tables
    // Create Two tables Total_In and Total_Out or Create additional Field in every row "type" = "in"/'out"
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
     * Get all data by condition,
     * example: condition can be the "date" column
     * and situation can be today date("12-05-23" format),
     * which means, get all the log of today
     * some other usage of this method IE: This Month and This Day
     *
     * @param condition
     * @param situation
     * @return
     */
    public ArrayList<DbModel> getLogData(String condition, String situation) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE " + condition + " = " + situation, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {

                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * returns all the log data of the fuel changes
     *
     * @return
     */
    public ArrayList<DbModel> getLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {

                DbModel count = new DbModel(cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * SELECT * FROM log WHERE (time LIKE '%10:%')
     * TODO: WIP and Next Release
     */
    public ArrayList<DbModel> getSessions(String time) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE (time LIKE '%" + time + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }
        return modelList;
    }

    /**
     * Get all weekly inputs from total table
     * Weekly Usage
     */
    public ArrayList<Float> getWeekly() {
        // initialize an arraylist as 0 and item is 7
        ArrayList<Float> result = new ArrayList<>(Collections.nCopies(7, 0.0f));

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

        SQLiteDatabase db = this.getReadableDatabase();
        // Get Weekly Data from The SQLite
        for (int i = 0; i < 7; i++) {

            String date = dateFormat.format(calendar.getTime());
            Log.d(TAG, "getWeekly: " + date);
            Cursor cursor = db.rawQuery("SELECT * FROM total WHERE (date = '" + date + "')", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {

                    float amount = Float.valueOf(cursor.getString(2));
                    result.set(i, amount);
                    Log.d(TAG, "getWeekly: Amount " + amount);
                }
                cursor.close();

            }
            // With Today Date Also
            calendar.add(Calendar.DATE, -1);
        }
        db.close();
        return result;

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<HashMap<String, Object>> getDaily() {
        // initialize an arraylist
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

        SQLiteDatabase db = this.getReadableDatabase();
        // Get Hourly Data from The SQLite


        String day = dateFormat.format(calendar.getTime());
        Log.d(TAG, "getDaily: " + day);
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE (date = '" + day + "')" + " AND type = " + "\"in\"", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("time", cursor.getString(1));
                map.put("date", cursor.getString(2));
                map.put("data", cursor.getString(3));
                Log.d(TAG, "getDaily: map: "+map);
                data.add(map);
            }
            cursor.close();

        }
        // With Today Date Also

        db.close();

        Map<Integer, List<Double>> hourlyData = new HashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        for (Map<String, Object> item : data) {
            LocalTime time = LocalTime.parse(item.get("time").toString(), timeFormatter);
            int hour = time.getHour();
            // Use Double.parseDouble instead of Integer.parseInt to handle decimals
            double dataValue = Double.parseDouble(item.get("data").toString());
            List<Double> dataList = hourlyData.get(hour);
            if (dataList == null) {
                dataList = new ArrayList<Double>();
                hourlyData.put(hour, dataList);
            }
            dataList.add(dataValue);
        }

        ArrayList<HashMap<String, Object>> result = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            List<Double> dataValues = hourlyData.get(hour);
            //System.out.println("HourlyData "+hourlyData);
            //System.out.println("Hour "+hour);

            if (dataValues != null) {
                //System.out.println("dataValues "+dataValues);

                double firstValue = dataValues.get(0);
                double lastValue = dataValues.get(dataValues.size() - 1);

                //If only one logs in the hour
                if (lastValue == firstValue) {

                    //System.out.println("Critical 1 ");
                    //Get Previous Hour TODO: if no previous hour?
                    //also if previous hour (11 as previous of 12) don't have logs

                    // This k must be 1 otherwise it will fall to
                    //default(current problematic hour) instead of going to previous hour
                    int k = 1;
                    List<Double> lastDataValues = hourlyData.get(hour - 1);

                    while (lastDataValues == null && (hour - k) >= 0) {
                        //System.out.println("Trying "+ (hour-k));
                        lastDataValues = hourlyData.get(hour - k);
                        k++;
                    }
                    
                    if (lastDataValues != null) {
                        //Getting last value of previous hour
                        firstValue = lastDataValues.get(lastDataValues.size() - 1);
                    }
                }
                //System.out.println("last "+lastValue+", first "+ firstValue);
                double diff = lastValue - firstValue;
                HashMap<String, Object> mapdata = new HashMap<>();
                mapdata.put("hour", hour);
                mapdata.put("value", diff);
                Log.d(TAG, "getDaily: mapdata:"+mapdata);

                result.add(mapdata);
                //System.out.printf("%02d:00 - %02d:59: %d\n", hour, hour, diff);
            }
        }



        return result;

    }


    /**
     * SELECT * FROM log WHERE (date LIKE '%05-23%')
     * returns All data as A Collection of in and Out
     * Might be used for Graphs
     */
    public ArrayList<DbModel> getMonthly(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<DbModel> modelList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM log WHERE (date LIKE '%" + month + "-23" + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                DbModel count = new DbModel(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                modelList.add(count);
            }
            cursor.close();
            db.close();
        }


        return modelList;

    }

    /**
     * Returns a String of Total Monthly Usage
     *
     * @param month
     * @return
     */
    public String getMonthlyTotal(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        String total;
        // if doesn't Exist Then Return 0
        double count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM total WHERE (date LIKE '%" + month + "-23" + "%')", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Log.d(TAG, "getMonthlyTotal: "+count+" + "+Double.parseDouble(cursor.getString(2)));
                count = count + Double.parseDouble(cursor.getString(2));

            }
            cursor.close();
            db.close();
        }


        return String.valueOf(count);

    }

    /**
     * Storing logs in database
     * we will not differentiate the inputs, Instead here we will add Directly logs
     */
    public void addLogs(String time, String date, String data, String type) {
        Log.d(TAG, "Add Log Attempt: Date=" + date + " Type=" + type);

        ContentValues values = new ContentValues();
        values.put(KEY_TIME, time); // Time
        values.put(KEY_DATE, date); // Date
        values.put(KEY_DATA, data); // Data
        values.put(KEY_TYPE, type); // Type

        // Correctly check if row exists for the SPECIFIED Date and Type in the total table
        if (isTotalRowExist(date, type)) {
            Log.d(TAG, "addLogs: Row Exist for date: " + date);
            // Calculate increment based on last entry
            double lastVal = "in".equals(type) ? getLastInput() : getLastOutput();
            double currentVal = Double.parseDouble(data);
            updateLogsTotal(date, String.valueOf(currentVal - lastVal), type);
        } else {
            Log.d(TAG, "addLogs: Row Doesn't Exist for date: " + date);
            double lastVal = "in".equals(type) ? getLastInput() : getLastOutput();
            double currentVal = Double.parseDouble(data);
            addLogsTotal(date, String.valueOf(currentVal - lastVal), type);
        }

        // Inserting Row to all Logs Table
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert(TABLE_LOG, null, values);
        db.close();

        Log.d(TAG, "New logs inserted into sqlite: " + id);
    }

    /**
     * Helper to check if a row exists in TABLE_TOTAL for specific date and type
     */
    private boolean isTotalRowExist(String date, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM total WHERE date = ? AND type = ?", new String[]{date, type});
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    /**
     * @param date
     * @param data
     * @param type
     */
    public void addLogsTotal(String date, String data, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        //
        ContentValues totalValues = new ContentValues();
        totalValues.put(KEY_DATE, date); // Date
        totalValues.put(KEY_DATA, data); // Data
        totalValues.put(KEY_TYPE, type); // Type

        db.insert(TABLE_TOTAL, null, totalValues);
        //db.close();
    }

    /**
     * If already total log of the day is exist then just update changed value
     * @param date
     * @param data
     * @param type
     */
    public void updateLogsTotal(String date, String data, String type) {
        Calendar c = Calendar.getInstance();
        SQLiteDatabase db = this.getWritableDatabase();
        // Updated values
        ContentValues totalValues = new ContentValues();
        totalValues.put(KEY_DATE, date); // Date
        Log.d(TAG, "updateLogsTotal: Total: " + getLogsTotal(new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()), type)+ " Add: "+data+ " Type: "+type+ " Date: " +date);
        totalValues.put(KEY_DATA, Double.parseDouble(data) + getLogsTotal(new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()), type)); // Data
        totalValues.put(KEY_TYPE, type); // Type

        // Update data Where Date and type Matching
        String selection = KEY_DATE + " = ? AND " + KEY_TYPE + " = ?";
        String[] selectionArgs = {date, type};

        db.update(TABLE_TOTAL, totalValues, selection, selectionArgs);
        //db.close();
    }

    /**
     * TODO: Returning 0 even if the data exist
     *
     * @param date
     * @param type
     * @return
     */
    public double getLogsTotal(String date, String type) {
        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        //"SELECT * FROM total WHERE date = " + date + " AND type = " + "\""+type+"\""
        //"SELECT data FROM total WHERE date = " + date + " AND type = '" + type + "'"
        //"SELECT data FROM total WHERE date = " + date + " AND type = " + type
        //"SELECT data FROM total WHERE date = " + date + " AND type = '\""+type+"\"'"
        //"SELECT data FROM total WHERE date = " + date + " AND type = " + "in"

        String[] selectionArgs = {date, type};
        Cursor cursor = db.rawQuery("SELECT data FROM total WHERE date = ? AND type = ?", selectionArgs);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.d(TAG, "getLogsTotal: Cursor Data Exist, Size " + cursor.getCount() + " Column: " + cursor.getColumnCount());
                count = Double.parseDouble(cursor.getString(0));
                Log.d(TAG, "getLogsTotal: Count " + count);
            }
            cursor.close();
        }
        Log.d(TAG, "getLogsTotal: " + count + " Date: " + date + " Type: " + type);
        return count;
    }

    /**
     * @param tableName
     * @param columnName
     * @param columnValue
     * @return
     */
    public boolean isRowExist(String tableName, String columnName, String columnValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " WHERE " + columnName + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{columnValue});
        boolean rowExists;
        rowExists = cursor.moveToFirst();
        cursor.close();
        //db.close();
        return rowExists;
    }

    /**
     * Get last input value based on serial
     *
     * @param
     * @return
     */
    public double getLastInput() {

        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT data FROM log WHERE type = \"in\" ORDER BY sr DESC LIMIT 1", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count = Double.parseDouble(cursor.getString(0));
                Log.d(TAG, "getLastInput: " + count);
            }
            cursor.close();
            db.close();
        }
        return count;
    }

    /**
     * @return
     */
    public double getLastOutput() {
        double count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT data FROM log WHERE type = \"out\" ORDER BY sr DESC LIMIT 1", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count = Double.parseDouble(cursor.getString(0));
                Log.d(TAG, "getLastOutput: " + count);
            }
            cursor.close();
            db.close();
        }

        return count;
    }


    /**
     * simple and Easier Access, I'll keep for later
     *
     * @param
     * @return
     */
    public double getTodayTotalUsage() {
        if (isRowExist("total", "date", new SimpleDateFormat("dd-MM-yy").format(calendar.getTimeInMillis())) && isRowExist("total", "type", "\"out\"")) {
            Log.d(TAG, "getTodayTotalUsage: " + "Daily Exist");
            return getLogsTotal(new SimpleDateFormat("dd-MM-yy").format(calendar.getTimeInMillis()), "\"out\"");
        } else {
            Log.d(TAG, "getTodayTotalUsage: " + "Daily not Exist");
            return 0;
        }

    }

    public double getTodayTotalInput() {
        if (isRowExist("total", "date", new SimpleDateFormat("dd-MM-yy").format(calendar.getTimeInMillis())) && isRowExist("total", "type", "\"in\"")) {
            Log.d(TAG, "getTodayTotalInput: " + " Daily Exist");
            return getLogsTotal(new SimpleDateFormat("dd-MM-yy").format(calendar.getTimeInMillis()), "\"in\"");
        } else {
            Log.d(TAG, "getTodayTotalInput: " + " Daily Doesn't Exist");
            return 0;
        }
    }

    /**
     * Set Data Based on SQL Query
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
     * Re-create database Delete all tables and create them again
     */
    public void deleteLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_LOG, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }

    /**
     * Get The SQL Data instantly from Assets
     *
     * @throws IOException
     */
    public void CopyDatabaseFromAssets() throws IOException {
        InputStream myInput = mCtx.getAssets().open(DATABASE_NAME);
        String outFileName = getDatabasePath();
        File file = new File(mCtx.getApplicationInfo().dataDir + DB_PATH_SUFFIX);
        if (!file.exists())
            file.mkdir();
        OutputStream myOutput = new FileOutputStream(outFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0)
            myOutput.write(buffer, 0, length);
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    /**
     * Not Used anymore, or didn't implemented well.
     *
     * @return
     * @throws SQLException
     */
    public SQLiteDatabase openDatabase() throws SQLException {
        File dbFile = mCtx.getDatabasePath(DATABASE_NAME);
        if (!dbFile.exists()) {
            try {
                CopyDatabaseFromAssets();
                Toast.makeText(mCtx, "Copying success from assets to folder", Toast.LENGTH_SHORT);
            } catch (IOException e) {
                throw new RuntimeException("Error Creating database", e);
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.CREATE_IF_NECESSARY);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);

        // Create tables again
        onCreate(db);
    }
}

