package com.codesw.fuelcontroller.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * SpUtils provides static convenience methods for SharedPreferences operations.
 * It simplifies getting and putting primitives into the default shared preferences.
 */
public class SpUtils {

    /**
     * Retrieves a string value from preferences.
     */
    public static String getString(Context context, String strKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(strKey, "");
    }

    /**
     * Retrieves a string value with a default fallback.
     */
    public static String getString(Context context, String strKey, String strDefault) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(strKey, strDefault);
    }

    /**
     * Persists a string value to preferences.
     */
    public static void putString(Context context, String strKey, String strData) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(strKey, strData);
        editor.apply();
    }

    /**
     * Retrieves a boolean value from preferences.
     */
    public static Boolean getBoolean(Context context, String strKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(strKey, false);
    }

    /**
     * Retrieves a boolean value with a default fallback.
     */
    public static Boolean getBoolean(Context context, String strKey, Boolean strDefault) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(strKey, strDefault);
    }


    /**
     * Persists a boolean value to preferences.
     */
    public static void putBoolean(Context context, String strKey, Boolean strData) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(strKey, strData);
        editor.apply();
    }

    /**
     * Retrieves an integer value from preferences.
     */
    public static int getInt(Context context, String strKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(strKey, 600);
    }

    /**
     * Retrieves an integer value with a default fallback.
     */
    public static int getInt(Context context, String strKey, int strDefault) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(strKey, strDefault);
    }

    /**
     * Persists an integer value to preferences.
     */
    public static void putInt(Context context, String strKey, int strData) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(strKey, strData);
        editor.apply();
    }

    /**
     * Removes a specific key from preferences.
     */
    public static void removeKey(Context context, String strKey) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(strKey);
        editor.apply();
    }
}
