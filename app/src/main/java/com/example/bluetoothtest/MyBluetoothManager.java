package com.example.bluetoothtest;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
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

import java.util.Set;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

public class MyBluetoothManager {
    private static final String TAG = "MyBluetoothManager";

    // Bluetooth adapter variable
    private BluetoothAdapter myBluetoothAdapter;

    // Bluetooth manager
    private BluetoothManager myBluetoothManager;

    // We need reference to MainActivity for context of the application.
    private MainActivity myMainActivity;

    // Locally defined integer constant that must be greater than 0.
    private static final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_DISCOVER_BT = 101;

    // By default, we can make the device discoverable for 2 minutes. Here we have used 5 minutes.
    // The longest possible discoverable time is 1 hour. If the DISCOVERABLE_DURATION is set 0 then
    // the device is always discoverable.
    public static final int DISCOVERABLE_DURATION = 300;

    // Variables for the Scanning operation
    private boolean mScanning;
    private long scanPeriod;
    private int signalStrength;

    // Constructor for the class
    public MyBluetoothManager(MainActivity mainActivity) {
        this.myMainActivity = mainActivity;

        // get the Bluetooth manager and Bluetooth adapter
        myBluetoothManager = (BluetoothManager) this.myMainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = myBluetoothManager.getAdapter();
    }

    /*
        Enable Bluetooth. Check whether Bluetooth is available on the device or not. If yes, check
        if it is enabled or not. If not then request Bluetooth.
     */
    public void enableBluetooth() {
        if (Utils.checkBluetooth(myBluetoothAdapter)){
            Utils.toast(myMainActivity.getApplicationContext(), "Bluetooth already on.");
        }
        else
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            myMainActivity.startActivityForResult(enableBtIntent, this.REQUEST_ENABLE_BT);

/*           REQUEST_ENABLE_BT is returned back by the system in onActivityResult as the request
             code parameter.

             If enabling Bluetooth succeeds, the activity will receives the RESULT_OK result
             code in the onActivityResult() callback. If the Bluetooth was not enabled due to
             an error (or the user responded "No") then the result code is RESULT_CANCELED.

             Optionally, we can listen for the ACTION_STATE_CHANGED broadcast Intent, which the
             system will broadcast whenever the Bluetooth state has changed. Listening for this
             broadcast can be useful to detect changes made to the Bluetooth state while the app
             is running.

             Also, enabling discoverability automatically enables the Bluetooth.*/
        }
    }

    /*
        Disable the Bluetooth if it is turned on. Else do nothing.
     */
    public void disableBluetooth() {
        if(myBluetoothAdapter.isEnabled()){
            myBluetoothAdapter.disable();
            Utils.toast(myMainActivity.getApplicationContext(), "Bluetooth is disabled :)");
        }
        else
        {
            Utils.toast(myMainActivity.getApplicationContext(),"Bluetooth is already disabled :)");
        }
    }

    /*
    The BroadcastReceiver for the ACTION_FOUND intent of the Bluetooth device scan function.
    When a device is found by the Bluetooth scanner, this function is called with found device
    details.
     */
    private final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the Bluetooth device details from the Intent.
                // To create a connection we just need the MAC address of the discovered device.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                setStatusText(deviceName + "\t " + deviceMAC);
            }
        }
    };

    // Make the Phone Bluetooth discoverable for other devices. For connecting to remote device we
    // don't need to make the device discoverable. Enabling discoverability is only necessary when
    // we want the app to host a server socket that accepts incoming connections. Also, once the remote
    // device is paired once, we don't need to make the device discoverable.
    private void makeDeviceDiscoverable() {
        if (Utils.checkBluetooth(this.myBluetoothAdapter)) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
            myMainActivity.startActivityForResult(discoverableIntent, this.REQUEST_DISCOVER_BT);
        }
    }

    /*
        We can find remote Bluetooth devices using BluetoothAdapter if the devices are made discoverable.
        If a device is discoverable, then it will respond to the discover request by sharing some information
        such as the device name, class, and its unique MAC address. Using these information, the device
        performing discovery can then choose to initiate a connection to the discovered device.

        When a device is paired, the basic information about the device (such as the device name, class,
        and MAC address) is saved and can be read using the Bluetooth APIs. Using the known MAC address
        for a remote device, a connection can be initiated with it at any time without performing discovery.

        To be paired and connected are two different things: to be paired means that two devices are
        aware of each other's existence, have a shared link-key that can be used for authentication, and
        are capable of establishing an encrypted connection with each other. To be connected means that
        the devices currently share an RFCOMM channel and are able to transmit data with each other.

        The Android Bluetooth API requires the devices to be paired before an RFCOMM connection can be
        established.
    */
    private void scanForBluetoothDevices() {
        /*
        Before performing device discovery, it is worth querying the set of paired devices to see if
        the desired device is already known.
         */
        Set<BluetoothDevice> pairedDevices = this.myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            setStatusText("Already paired devices are:");
            String [] deviceArray = new String[pairedDevices.size()];
            int i = 0;
            // if there are paired devices, then loop over them and get their attributes:
            for (BluetoothDevice device : pairedDevices) {
                deviceArray[i] = device.getName() + "\t\t " + device.getAddress();
                setStatusText(deviceArray[i]);
                i += 1;
            }
        }
        else {
            setStatusText("No paired devices found");
        }

        /*
        Performing device discovery consumes a lot of the Bluetooth adapters resources. After we have
        found a device to connect to, be certain to stop discovery with cancelDiscovery before
        attempting a connection. We should also not start discovery when connected to other devices.

        In order to receive information about each device discovered, we need to register a
        BroadcastReceiver for the ACTION_FOUND intent. The system broadcasts this intent for each
        device. The intent contains the extra fields EXTRA_DEVICE and EXTRA_CLASS, which contain a
        BluetoothDevice and a BluetoothClass.
         */
        if (this.myBluetoothAdapter != null) {
            if (this.myBluetoothAdapter.startDiscovery()) {
                setStatusText("Device discovery started");
            }
            else
            {
                setStatusText("Failed to start device discovery");
            }
        }

//        All that is needed from the BluetoothDevice object in order to initiate a connection is the
//        MAC address.
    }


    //    BroadcastReceiver for ACTION_SCAN_MODE_CHANGED intent of the make device discoverable function.
    private final BroadcastReceiver bluetoothDeviceDiscoverableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                String scanMode = intent.getParcelableExtra(BluetoothAdapter.EXTRA_SCAN_MODE);
                String previousScanMode = intent.getParcelableExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE);
                /*
                These two extras can have these values:
                    SCAN_MODE_CONNECTABLE_DISCOVERABLE : The device is in discoverable mode.
                    SCAN_MODE_CONNECTABLE : The device isn't discoverable but can still receive connections.
                    SCAN_MODE_NONE : The device isn't in discoverable mode and cannot receive connections.
                */
            }
        }
    };

//    // Listeners for the Bluetooth events and more. The function is called after Bluetooth related
//    // operations are requested. For example turning on Bluetooth, making Bluetooth device
//    // discoverable, etc.
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        switch (requestCode) {
//            case REQUEST_ENABLE_BT:
//                if (resultCode == RESULT_OK) {
//                    setStatusText("Bluetooth turned on");
//                }
//                else if (resultCode == RESULT_CANCELED){
//                    setStatusText("Failed to turn on the Bluetooth. Try Again !!");
//                }
//                break;
//
//            case REQUEST_DISCOVER_BT:
//                if (resultCode == RESULT_OK || resultCode == DISCOVERABLE_DURATION) {
//                    setStatusText("Bluetooth is discoverable");
//                }
//                else if (resultCode == RESULT_CANCELED) {
//                    setStatusText("Failed to make the Bluetooth discoverable.");
//                }
//                break;
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }


    // Wrapper function for the status text field in the main activity. Comment out in final version.
    private void setStatusText(String str) {
        myMainActivity.setStatusText(str);
    }
}
