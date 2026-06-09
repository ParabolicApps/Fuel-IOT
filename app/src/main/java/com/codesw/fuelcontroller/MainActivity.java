package com.codesw.fuelcontroller;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.view.View;
import com.codesw.fuelcontroller.R;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.codesw.fuelcontroller.receiver.UrlBroadcastReceiver;
import com.codesw.fuelcontroller.service.Checker;
import com.codesw.fuelcontroller.utils.SQLiteHandler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.codesw.fuelcontroller.fragments.SettingsFragment;
import com.codesw.fuelcontroller.fragments.DevicesFragment;
import com.codesw.fuelcontroller.fragments.HomeFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
            switch (item.getItemId()) {
                case R.id.homepage:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new HomeFragment(), "Home").addToBackStack(null).commit();
                    break;
                case R.id.devices:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new DevicesFragment(), "Devices").addToBackStack(null).commit();

                    break;
                case R.id.settings:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new SettingsFragment()).addToBackStack(null).commit();
                    break;
            }
            return true;
        });
        // Setup Broadcast and SQLite Handler
        mainBnv.setSelectedItemId(R.id.homepage);
        receiver = new UrlBroadcastReceiver();
        filter = new IntentFilter();
        filter.addAction(Checker.URL_FILTER);
        db = new SQLiteHandler(getApplicationContext());
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
        //refreshItem = (MenuItem) menu.findItem(R.id.action_refresh);
        return true;
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
                //TODO: Send Data test
                sendDataFrag("35");
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
        registerReceiver(receiver, filter);
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
        Log.d(TAG, "urlReceived: main: "+counter);
        Log.d(TAG, "urlReceived: input: "+counter.split("/")[0]);
        String inData = counter.split("/")[0];
        String outData = counter.split("/")[1];

        // Send Data Activity to Fragment
        sendDataFrag(inData);
        Log.d(TAG, "urlReceived: output: "+counter.split("/")[1]);
    }
    public void sendDataFrag(String data){
        HomeFragment fragment1 = (HomeFragment) getSupportFragmentManager().findFragmentByTag("Home");
        //DevicesFragment fragment1 = (DevicesFragment) getSupportFragmentManager().findFragmentByTag("Devices");
        if (fragment1 != null){
            fragment1.setProgress(data);
        }

    }
    public void exportDB(){
        String DatabaseName = "data.db";
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        java.nio.channels.FileChannel source=null;
        java.nio.channels.FileChannel destination=null;
        String currentDBPath = "/data/"+ "com.codesw.fuelcontroller" +"/databases/"+DatabaseName ;
        String backupDBPath = "exportedController";
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Log.d(TAG, "exportDB: Your Database is Exported !!");
        } catch(IOException e) {

            Log.e(TAG, "exportDB: "+ e);
            e.printStackTrace();
        }

    }
}