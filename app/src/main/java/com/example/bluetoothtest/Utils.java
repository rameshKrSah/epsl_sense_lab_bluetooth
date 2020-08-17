package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class Utils {

        /*
    Check whether Bluetooth is available on the device or not and if yes whether it is turned on or
    not.
     */
    public static boolean checkBluetooth(BluetoothAdapter adapter) {
        if (adapter == null || !adapter.isEnabled()) {
            return false;
        }
        else {
            return true;
        }
    }


    /*
    Generate Toast Message.
    Input: Context of the calling activity and the text message.
     */
    public static void toast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
//        toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 0);
        toast.show();
    }
}
