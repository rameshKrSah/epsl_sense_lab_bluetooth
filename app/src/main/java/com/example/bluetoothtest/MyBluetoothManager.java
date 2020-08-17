package com.example.bluetoothtest;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/*
    - BluetoothAdapter represents the local Bluetooth adapter or radio and is the entry-point for all
    Bluetooth interaction. Using this we can discover other Bluetooth devices, query a list of bonded
    devices, instantiate a Bluetooth device using a known MAC address, and create a BluetoothServerSocket
    to listen for communications from other devices.

    - BluetoothDevice represents a remote Bluetooth device and is used to request a connection with
    a remote device through BluetoothSocket or query information about the device such as its name,
    address, class and bonding state.

    - BluetoothSocket represents the interface for a Bluetooth socket and is the connection point
    for data exchange with another Bluetooth device via InputStream and OutputStream.

    - BluetoothServerSocket represents an open server that listens for incoming requests in order to
    connect two Bluetooth devices. When a remote Bluetooth device makes a connection request to server
    , BluetoothServerSocket will return a connected BluetoothSocket when the connection is accepted.

    =========================================== XXX ===============================================
    In order to use Bluetooth features, we need these permissions:

     1. <uses-permission android:name="android.permission.BLUETOOTH"/>

     2. <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

    <!-- If the app targets Android 9 or lower we can use Coarse Location -->
     3. <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 */

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import static androidx.core.app.ActivityCompat.startActivityForResult;

public class MyBluetoothManager extends MainActivity{
    private static final String TAG = "MyBluetoothManager";

    // Bluetooth adapter variable
    private BluetoothAdapter myBluetoothAdapter;

    // Bluetooth manager
    private BluetoothManager myBluetoothManager;

    private Context myContext;

    // Locally defined integer constant that must be greater than 0.
    private static final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_DISCOVER_BT = 101;

    // Constructor for the class
    public MyBluetoothManager(Context context) {
//        myBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // get the Bluetooth adapter : BluetoothAdapter is required for any and all Bluetooth activity.
        // There is one BluetoothAdapter for the entire system.
        this.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.myContext = context;
    }

    // Enable Bluetooth
    public void enableBluetooth() {
        if (myBluetoothAdapter == null) {
            // device doesn't support Bluetooth
            showToast("Bluetooth is not supported on this device.");
        }
        else
        {
            // ensure that Bluetooth is enabled. If not ask the user to turn it on.
            if (!myBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (enableBtIntent != null) {
                    startActivityForResult(enableBtIntent, this.REQUEST_ENABLE_BT);
                }

                // REQUEST_ENABLE_BT is returned back by the system in onActivityResult as the request
                // code parameter.

                // If enabling Bluetooth succeeds, the activity will receives the RESULT_OK result
                // code in the onActivityResult() callback. If the Bluetooth was not enabled due to
                // an error (or the user responded "No") then the result code is RESULT_CANCELED.

                // Optionally, we can listen for the ACTION_STATE_CHANGED broadcast Intent, which the
                // system will broadcast whenever the Bluetooth state has changed. Listening for this
                // broadcast can be useful to detect changes made to the Bluetooth state while the app
                // is running.

                // Also, enabling discoverability automatically enables the Bluetooth.
            }
        }
    }

    public void disableBluetooth() {
        if(myBluetoothAdapter.isEnabled()){
            myBluetoothAdapter.disable();
            showToast("Bluetooth is disabled :)");
        }
        else
        {
            showToast("Bluetooth is already disabled :)");
        }
    }

    // Listeners for the Bluetooth events and more
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    showToast("Bluetooth is on");
                }
                else {
                    statusText.setText("Failed to turn on the Bluetooth. Try Again !!");
                }

            case REQUEST_DISCOVER_BT:
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    // toast message function
    private void showToast(String msg) {
        Toast.makeText(this.myContext, msg, Toast. LENGTH_SHORT).show();
    }
}
