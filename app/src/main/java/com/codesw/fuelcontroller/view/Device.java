package com.codesw.fuelcontroller.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.codesw.fuelcontroller.ConfigureDeviceActivity;
import com.codesw.fuelcontroller.R;
import com.codesw.fuelcontroller.fragments.DevicesFragment;
import com.codesw.fuelcontroller.network.PostTask;
import com.google.android.material.card.MaterialCardView;

/**
 * A layout show Devices
 */

public class Device extends RelativeLayout{
    private TextView deviceName;
    private TextView deviceAddress;
    private MaterialCardView cardView;
    private Switch deviceSwitch;
    public String ipAddr;
    private Context myContext;
    public int id;

    public Device(Context context) {
        this(context,null);
        myContext = context;
    }
    public Device(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }
    private void initView(final Context context) {
        View.inflate(context, R.layout.device, this);
        deviceName = this.findViewById(R.id.deviceName);
        deviceAddress = this.findViewById(R.id.lv_item_tv_subtitle);
        cardView = this.findViewById(R.id.device_card);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ipAddr != null) {
                    String fullUrl = "http://" + ipAddr + "/data";
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putString("server_url", fullUrl)
                            .apply();
                    Toast.makeText(context, "Selected: " + deviceName.getText() + " (" + ipAddr + ")", Toast.LENGTH_SHORT).show();
                    
                    // Notify fragment to refresh glow
                    if (myContext != null) {
                        // We can just update all children of the parent view.
                        if (getParent() instanceof android.view.ViewGroup) {
                            android.view.ViewGroup parent = (android.view.ViewGroup) getParent();
                            for (int i = 0; i < parent.getChildCount(); i++) {
                                View child = parent.getChildAt(i);
                                if (child instanceof Device) {
                                    ((Device) child).refreshSelectionGlow();
                                }
                            }
                        }
                    }
                }
            }
        });

        deviceName.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(myContext, ConfigureDeviceActivity.class);
                intent.putExtra("com.codesw.fuelcontroller.deviceID", id);
                intent.putExtra("com.codesw.fuelcontroller.ipAddr", ipAddr);
                intent.putExtra("com.codesw.fuelcontroller.title", deviceName.getText());
                Activity activity = (Activity) myContext;
                activity.startActivityForResult(intent, id);
                return false;
            }
        });
        //theres just a switch in the layout to just turn on and off the bulb,
        //but this will not gonna use anymore. im gonna remove this mechanism
        // Ill Implemnt the only the response from the device
        
        /*deviceSwitch = (Switch) this.findViewById(R.id.deviceSwitch);
        deviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SetDevice setDevice = new SetDevice();
                setDevice.deviceAddress = "http://"+ ipAddr +"/setDevice";
                if (isChecked) {
                    setDevice.devicePostText = "LightBulb=on";
                } else {
                    setDevice.devicePostText = "LightBulb=off";
                }
                setDevice.execute();
            }
        });*/
    }

    public void setText(String text) {
        deviceName.setText(text);
    }
    public void setIpAddrText(String text){
        deviceAddress.setText(text);

    }

    public void setChecked(boolean checked) {
        deviceSwitch.setChecked(checked);
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        deviceName.setOnLongClickListener(listener);
    }

    public void setDeviceEnabled(boolean status) {
        deviceName.setEnabled(status);
        //deviceSwitch.setEnabled(status);
        refreshSelectionGlow();
    }

    public void refreshSelectionGlow() {
        String currentUrl = PreferenceManager.getDefaultSharedPreferences(myContext).getString("server_url", "");
        if (ipAddr != null && currentUrl.contains(ipAddr)) {
            cardView.setBackgroundResource(R.drawable.card_glow_blue);
        } else {
            cardView.setBackgroundResource(R.drawable.button_shape);
        }
    }

    private class SetDevice extends PostTask {
        @Override
        protected void onPostExecute(String deviceTitle) {
            if (!deviceTitle.isEmpty()) {
                Log.e("SetDevice", "Succeed!");
            } else {
                setDeviceEnabled(false);
                Toast.makeText(myContext, myContext.getString(R.string.set_device_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
