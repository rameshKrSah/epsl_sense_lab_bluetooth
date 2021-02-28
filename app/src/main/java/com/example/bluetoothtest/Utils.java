package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Utils {
    private static final String TAG = "Utils";

        /*
    Check whether Bluetooth is available on the device or not and if yes whether it is turned on or
    not.
//     */
//    public static boolean checkBluetooth(BluetoothAdapter adapter) {
//        if (adapter == null || !adapter.isEnabled()) {
//            return false;
//        }
//        else {
//            return true;
//        }
//    }


    /**
     * Generate Toast Message.
     * @param context
     * @param text
     */
    public static void toast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
//        toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    /**
     * Return a sting for the connection state of the Bluetooth adapter.
     * @param state
     * @return String
     */
    public static String btConnStateAsString(int state){
        String result;
        if (state == BluetoothState.STATE_NONE) {
            result = "NONE";
        } else if (state == BluetoothState.STATE_LISTEN) {
            result = "LISTEN";
        } else if (state == BluetoothState.STATE_CONNECTING) {
            result = "CONNECTING";
        } else if (state == BluetoothState.STATE_CONNECTED) {
            result = "CONNECTED";
        } else if (state == BluetoothState.STATE_DISCONNECTED){
            result = "DISCONNECTED";
        }
        else{
            result = "UNKNOWN";
        }
        return result;
    }

    /**
     * Return a string for the state of the Bluetooth adapter.
     * @param state
     * @return String
     */
    public static String btStateAsString(int state){
        String result = "UNKNOWN";
        if (state == BluetoothAdapter.STATE_TURNING_ON) {
            result = "TURNING_ON";
        } else if (state == BluetoothAdapter.STATE_ON) {
            result = "ON";
        } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
            result = "TURNING_OFF";
        }else if (state == BluetoothAdapter.STATE_OFF) {
            result = "OFF";
        }
        return result;
    }

    /**
     * Get Bond state of a Bluetooth device
     * @param state
     * @return String
     */
    public static String btBondStateAsString(int state){
        String result = "UNKNOWN";
        if (state == BluetoothDevice.BOND_NONE) {
            result = "NONE";
        } else if (state == BluetoothDevice.BOND_BONDING) {
            result = "BONDING";
        } else if (state == BluetoothDevice.BOND_BONDED) {
            result = "BONDED";
        }

        return result;
    }

    /**
     * Get the current time in millis
     * @return long
     */
    public static long getCurrentTime() {
        // the current time as UTC milliseconds from the epoch.
//        Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss");
//        String strDate = "Current Time : " + mdformat.format(calendar.getTime());
//        Log.d(TAG, "getCurrentTime: " + strDate);
        return Calendar.getInstance().getTimeInMillis();
    }
}
