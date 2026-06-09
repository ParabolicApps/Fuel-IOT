package com.codesw.fuelcontroller;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.codesw.fuelcontroller.network.Network;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.codesw.fuelcontroller.fragments.SettingsFragment;
import com.codesw.fuelcontroller.fragments.DevicesFragment;
import com.codesw.fuelcontroller.fragments.HomeFragment;
public class MainActivity extends AppCompatActivity {
private BottomNavigationView mainBnv;
private FrameLayout mainFl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainBnv = (BottomNavigationView) findViewById(R.id.main_bnv);
        mainFl = (FrameLayout) findViewById(R.id.container);
		
        FragmentManager fragmentTransaction = getSupportFragmentManager();
        fragmentTransaction.beginTransaction().add(R.id.container, new HomeFragment()).commit();

        // TODO: 2022/6/28 BottomNavigationView点击事件
        mainBnv.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.homepage:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new HomeFragment()).addToBackStack(null).commit();
                    break;
                case R.id.devices:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new DevicesFragment()).addToBackStack(null).commit();
                    //customToast("敬请期待～");
                    break;
                case R.id.settings:
                    fragmentTransaction.beginTransaction().setCustomAnimations(R.anim.alpha_in, R.anim.alpha_out).replace(R.id.container, new SettingsFragment()).addToBackStack(null).commit();
                    break;
            }
            return true;
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
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
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_refresh:
                
                item.setEnabled(false);
				
                return true;
            case R.id.action_add_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", true);
                startActivity(intent);
                return  true;
            case R.id.action_fix_device:
                intent = new Intent(this, GuideActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.first_config", false);
                startActivity(intent);
                return  true;
/*            case R.id.action_settings:
                return true;*/
        }

        return super.onOptionsItemSelected(item);
    }

    /*
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
				//TODO: make this in DeviceFragment
                //Device device = (Device) deviceList.findViewById(deviceID);
                //device.setText(title);
                break;
            default:
                break;
        }
    }

    /*
     * Add support for screen rotations.
     * There are some problems in this guideActivity which needs further developments
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}