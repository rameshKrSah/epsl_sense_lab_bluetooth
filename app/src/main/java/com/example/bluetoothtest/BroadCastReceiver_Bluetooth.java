package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadCastReceiver_Bluetooth extends BroadcastReceiver {
    private Context myContext;

    public BroadCastReceiver_Bluetooth(Context activityContext) {
        this.myContext = activityContext;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Utils.toast(this.myContext, "Bluetooth is OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Utils.toast(this.myContext, "Bluetooth is ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Utils.toast(this.myContext, "Bluetooth turning OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Utils.toast(this.myContext, "Bluetooth turning ON");
                    break;
                case BluetoothAdapter.ERROR:
                    Utils.toast(this.myContext, "Bluetooth ERROR");
                    break;
            }
        }
    }
}
