package com.codesw.fuelcontroller;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.codesw.fuelcontroller.fragments.DevicesFragment;
import com.codesw.fuelcontroller.fragments.Frag1FragmentActivity;
import com.codesw.fuelcontroller.fragments.Frag2FragmentActivity;
import com.codesw.fuelcontroller.fragments.MapsFragment;
import com.codesw.fuelcontroller.fragments.SettingsFragment;
import com.codesw.fuelcontroller.receiver.UrlBroadcastReceiver;
import com.codesw.fuelcontroller.service.Checker;
import com.codesw.fuelcontroller.utils.FirebaseSyncManager;
import com.codesw.fuelcontroller.utils.SQLiteHandler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements UrlBroadcastReceiver.UrlBroadcastReceiverListener {
    private static final String TAG = "MainActivity";
    private BottomNavigationView mainBnv;
    private LinearLayout main;
    private Toolbar toolbar;

    private SQLiteHandler db;
    private final boolean configured = true;
    private BroadcastReceiver receiver;
    private IntentFilter filter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainBnv = (BottomNavigationView) findViewById(R.id.main_bnv);
        main = (LinearLayout) findViewById(R.id.container);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _v) {
                onBackPressed();
            }
        });

        FragmentManager fragmentTransaction = getSupportFragmentManager();


        mainBnv.setOnItemSelectedListener(item -> {
            FragmentManager fm = getSupportFragmentManager();
            androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out);
            
            String tag = "";
            androidx.fragment.app.Fragment fragment = null;

            switch (item.getItemId()) {
                case R.id.home:
                    tag = "Home";
                    fragment = new Frag1FragmentActivity();
                    break;
                case R.id.analytics:
                    tag = "Analytics";
                    fragment = new Frag2FragmentActivity();
                    break;
                case R.id.map:
                    tag = "Map";
                    fragment = new MapsFragment();
                    break;
                case R.id.devices:
                    tag = "Devices";
                    fragment = new DevicesFragment();
                    break;
                case R.id.settings:
                    tag = "Settings";
                    fragment = new SettingsFragment();
                    break;
            }
            
            if (fragment != null) {
                ft.replace(R.id.container, fragment, tag).commit();
            }
            invalidateOptionsMenu();
            return true;
        });
        // Setup Broadcast and SQLite Handler
        mainBnv.setSelectedItemId(R.id.home);
        receiver = new UrlBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction(Checker.URL_FILTER);
        db = SQLiteHandler.getInstance(getApplicationContext());
        
        // Start Firebase real-time sync if user is signed in
        if (FirebaseSyncManager.isSignedIn()) {
            FirebaseSyncManager.setupRealtimePull(db, com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());
        }

        initializeDemoData();

        _changeActivityFont("ubuntu_medium");

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            }
        }




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isDevicesScreen = mainBnv != null && mainBnv.getSelectedItemId() == R.id.devices;

        menu.findItem(R.id.action_refresh).setVisible(isDevicesScreen);
        menu.findItem(R.id.action_add_device).setVisible(isDevicesScreen);
        menu.findItem(R.id.action_fix_device).setVisible(isDevicesScreen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent;

        switch (id) {
            case R.id.action_refresh:

                //item.setEnabled(false);
                DevicesFragment devicesFragment = (DevicesFragment) getSupportFragmentManager().findFragmentByTag("Devices");
                if (devicesFragment != null) {
                    devicesFragment.refreshDevices();
                }
                return true;
            case R.id.action_add_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", true);
                startActivity(intent);
                return true;
            case R.id.action_fix_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", false);
                startActivity(intent);
                return true;
            case R.id.export:
                exportDB();
                return true;
            case R.id.simulateData:
                simulateDB();
        }

        return super.onOptionsItemSelected(item);
    }

    private void simulateDB() {
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_edit_text, null);
        Calendar c = Calendar.getInstance();
        // Find the TextInputLayouts inside the dialog layout
        final TextInputLayout textInputLayout1 = dialogView.findViewById(R.id.text_input_layout1);
        final TextInputLayout textInputLayout2 = dialogView.findViewById(R.id.text_input_layout2);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setTitle("Simulate")
                .setView(dialogView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text1 = Objects.requireNonNull(textInputLayout1.getEditText()).getText().toString();// Data
                        String text2 = Objects.requireNonNull(textInputLayout2.getEditText()).getText().toString(); // type
                        Toast.makeText(MainActivity.this, text1, Toast.LENGTH_SHORT).show();
                        db.addLogs(new SimpleDateFormat("hh:mm a").format(c.getTimeInMillis()), new SimpleDateFormat("dd-MM-yy").format(c.getTimeInMillis()), text1, text2);
                        // Do something with the text input data
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

        // Show the dialog
        /*Dialog dialog = builder.create();
        dialog.show();*/
    }


    public void handleModeSwitch(boolean isRealMode) {
        if (isRealMode) {
            // Real Mode ON: Clear demo data to prepare for live data
            db.deleteLogs();
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("demo_data_initialized", false)
                    .apply();
            Toast.makeText(this, "Switched to Real Mode. Data cleared.", Toast.LENGTH_SHORT).show();
        } else {
            // Real Mode OFF: Re-initialize demo data
            initializeDemoData();
            Toast.makeText(this, "Switched to Demo Mode. Data initialized.", Toast.LENGTH_SHORT).show();
        }
        
        // Refresh fragment if it exists
        Frag1FragmentActivity home = (Frag1FragmentActivity) getSupportFragmentManager().findFragmentByTag("Home");
        if (home != null && home.isResumed()) {
            home.onResume(); // Force refresh stats
        }
    }

    private void initializeDemoData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Only initialize if Real Mode is OFF
        boolean realMode = prefs.getBoolean("real_mode", false);
        if (realMode) {
            Log.d(TAG, "Real Mode active. Skipping demo data initialization.");
            return;
        }

        if (!prefs.getBoolean("demo_data_initialized", false)) {
            Log.d(TAG, "Initializing demo data for the first time...");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());

            // Clear any partial data if it exists
            db.deleteLogs();

            // Generate logs for the last 30 days
            // We insert from past to present so getLastInput/Output works correctly
            for (int i = 30; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DATE, -i);
                String date = dateFormat.format(cal.getTime());

                // Random cumulative refill data (in)
                // Assuming data is cumulative for the logic 'current - last' to work
                double baseRefill = 1000 + (30 - i) * 50; // Starting from 1000L
                double refill = baseRefill + (Math.random() * 20); 
                db.addLogs("08:00 AM", date, String.valueOf(refill), "in");

                // Random cumulative consumption data (out)
                for (int hour = 10; hour < 22; hour += 4) {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    double baseConsumed = 500 + (30 - i) * 30; // Starting from 500L
                    double consumed = baseConsumed + (Math.random() * 10);
                    db.addLogs(timeFormat.format(cal.getTime()), date, String.valueOf(consumed), "out");
                }
            }

            prefs.edit().putBoolean("demo_data_initialized", true).apply();
            Log.d(TAG, "Demo data initialization complete.");
        }
    }


    /**
     * Update device's title (Display Name) if it is available
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                Bundle bundle = data.getExtras();
                String title = bundle.getString("title");
                int deviceID = bundle.getInt("deviceID");
                //TODO: send this in DeviceFragment
                //Device device = (Device) deviceList.findViewById(deviceID);
                //device.setText(title);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Add support for screen rotations.
     * There are some problems in this guideActivity which needs further developments
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void urlReceived(String counter) {
        Log.d(TAG, "urlReceived: main: " + counter);
        if (counter == null || !counter.contains("/")) {
            sendDataFrag(counter, counter);
            return;
        }

        String[] telemetryParts = counter.split("/");
        if (telemetryParts.length >= 2) {
            String inData = telemetryParts[0];
            sendDataFrag(inData, counter);
        }
    }

    public void sendDataFrag(String data, String homeData) {
        // Find the currently active fragment in the container
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        
        if (currentFragment instanceof Frag1FragmentActivity) {
            ((Frag1FragmentActivity) currentFragment).setProgress(homeData);
        } else if (currentFragment instanceof Frag2FragmentActivity) {
            ((Frag2FragmentActivity) currentFragment).setData(data);
        } else if (currentFragment instanceof MapsFragment) {
            ((MapsFragment) currentFragment).setProgress(data);
        }
    }
    public void exportDB() {
        String databaseName = "data.db";
        File dbFile = getDatabasePath(databaseName);

        if (!dbFile.exists()) {
            Toast.makeText(this, "Database file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "FuelGuard_Backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".db";
            ContentResolver resolver = getContentResolver();
            Uri uri = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (uri != null) {
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = resolver.openOutputStream(uri)) {
                    if (out != null) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        Toast.makeText(this, "Database exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Fallback for older versions or if URI creation failed
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File exportFile = new File(downloadsDir, fileName);
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = new FileOutputStream(exportFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    Toast.makeText(this, "Database exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exportDB Error: ", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void _changeActivityFont(final String fontName) {
        String fontPath = "fonts/".concat(fontName.concat(".ttf"));
        overrideFonts(this.findViewById(android.R.id.content), fontPath);
    }

    private void overrideFonts(final View v, String fontPath) {
        try {
            Typeface typeface = Typeface.createFromAsset(getAssets(), fontPath);
            if (v instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(child, fontPath);
                }
            } else if (v instanceof TextView) {
                ((TextView) v).setTypeface(typeface);
            }
        } catch(Exception e) {
            // Quiet fail
        }
    }
}
