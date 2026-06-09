package com.codesw.fuelcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * This Activity will show the Logs of daily monthly like Sectioned
 * in a RecyclerView, and The data Will be get from Sql handler
 */
public class LogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
    }
}