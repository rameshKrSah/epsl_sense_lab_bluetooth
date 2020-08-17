/*
package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothServerAcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private static final String TAG = "BluetoothServerAcceptTh";
    private final String NAME = "";

    // UUID will be configurable from the device settings menu. Each camera module will have unique
    // UUID and the app needs to know that to establish a connection.
    private final UUID MY_UUID = new UUID();

    public BluetoothServerAcceptThread(BluetoothAdapter bluetoothAdapter) {
        // use a temporary object that is later assigned to mmServerSocket, because mmServerSocket is final
        BluetoothServerSocket tmp = null;

        try {
            // MY_UUID is the app's UUID string, also used by the client device.
            tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed.", e);
        }

        // once we get the BluetoothServerSocket
        mmServerSocket = tmp;
    }



}
*/
