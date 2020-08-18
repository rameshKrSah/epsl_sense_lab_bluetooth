package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BCR_BL_State_changed extends BroadcastReceiver {
    private static final String TAG = "BCR_BL_State_changed";
    private Context myContext;

    public BCR_BL_State_changed(Context activityContext) {
        this.myContext = activityContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG, "onReceive: Bluetooth OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "onReceive: Bluetooth ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "onReceive: Bluetooth turning OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "onReceive: Bluetooth turning ON");
                    break;
                case BluetoothAdapter.ERROR:
                    Log.d(TAG, "onReceive: Bluetooth ERROR");
                    break;
            }
        }
    }
}
