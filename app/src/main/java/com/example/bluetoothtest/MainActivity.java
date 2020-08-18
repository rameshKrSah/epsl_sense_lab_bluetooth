package com.example.bluetoothtest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Set;


/*
    TOOD:
    1. Scan for Bluetooth devices
    2. Query Bluetooth adapter for paired Bluetooth devices.
    3. Establish RFCOMM channels/sockets.
    4. Connect to specified sockets on other devices.
    5. Transfer data to and from other devices.
    6. Manage multiple connections.
 */

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    // Bluetooth adapter variable
    private BluetoothAdapter myBluetoothAdapter;

    // Locally defined integer constant that must be greater than 0.
    public static final int REQUEST_ENABLE_BT = 100;
    public static final int REQUEST_DISCOVER_BT = 101;

    // By default, we can make the device discoverable for 2 minutes. Here we have used 5 minutes.
    // The longest possible discoverable time is 1 hour. If the DISCOVERABLE_DURATION is set 0 then
    // the device is always discoverable.
    public static final int DISCOVERABLE_DURATION = 300;

    // UI items
    Button onButton, offButton, discoverButton, scanButton;
    TextView statusText;

    private BCR_BL_State_changed myBTCastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the Bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            Utils.toast(this, "Bluetooth is not available on this device.");

            // TODO Make sure to change this finish into return or similar to exit out of bluetooth functions.
            finish();
        }

        // Get an object of Bluetooth Broadcast Receiver class. This broadcast receiver monitors the
        // state of the Bluetooth module.
        myBTCastReceiver = new BCR_BL_State_changed(getApplicationContext());

        // find the UI elements
        onButton = (Button) findViewById(R.id.onSwitch);
        offButton = (Button) findViewById(R.id.offSwitch);
        discoverButton = (Button) findViewById(R.id.discoverSwitch);
        scanButton = (Button) findViewById(R.id.scanSwitch);
        statusText = (TextView) findViewById(R.id.statusText);

        // set the on click listeners for the button
        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetooth();
            }
        });

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDeviceDiscoverable();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanForBluetoothDevices();
            }
        });
    }

    // Enable Bluetooth
    public void enableBluetooth() {
        if (Utils.checkBluetooth(myBluetoothAdapter)){
            Utils.toast(this, "Bluetooth already on.");
        }
        else
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, this.REQUEST_ENABLE_BT);

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

    // Disable Bluetooth
    public void disableBluetooth() {
        if(myBluetoothAdapter.isEnabled()){
            myBluetoothAdapter.disable();
            setStatusText("Bluetooth disabled");
        }
        else
        {
            Utils.toast(this, "Bluetooth already disabled.");
        }
    }

    // Make the Phone Bluetooth discoverable for other devices. For connecting to remote device we
    // don't need to make the device discoverable. Enabling discoverability is only necessary when
    // we want the app to host a server socket that accepts incoming connections. Also, once the remote
    // device is paired once, we don't need to make the device discoverable.
    private void makeDeviceDiscoverable() {
        if (Utils.checkBluetooth(this.myBluetoothAdapter)) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
            startActivityForResult(discoverableIntent, this.REQUEST_DISCOVER_BT);
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
                BluetoothClass dvClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                ParcelUuid[] uuid = device.getUuids();
                setStatusText(deviceName + "\t " + deviceMAC);
                if (uuid != null) {
                    setStatusText("UUID : " + uuid.toString());
                }
            }
        }
    };

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

    // Listeners for the Bluetooth events and more. The function is called after Bluetooth related
    // operations are requested. For example turning on Bluetooth, making Bluetooth device
    // discoverable, etc.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    setStatusText("Bluetooth turned on");
                }
                else if (resultCode == RESULT_CANCELED){
                    setStatusText("Failed to turn on the Bluetooth. Try Again !!");
                }
                break;

            case REQUEST_DISCOVER_BT:
                if (resultCode == RESULT_OK || resultCode == DISCOVERABLE_DURATION) {
                    setStatusText("Bluetooth is discoverable");
                }
                else if (resultCode == RESULT_CANCELED) {
                    setStatusText("Failed to make the Bluetooth discoverable.");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register the Bluetooth ACTION_STATE_CHANGED Broadcast Receiver : Monitors the Bluetooth
        // module state
        registerReceiver(myBTCastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // register the BroadcastReceiver for the ACTION_FOUND intent of the device scan function.
        // Calls the registered function for every Bluetooth device found while scanning.
        registerReceiver(bluetoothDeviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // register the BroadcastReceiver for the ACTION_SCAN_MODE_CHANGED intent of the device
        // discoverable function.
        registerReceiver(bluetoothDeviceDiscoverableReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    @Override
    protected void onDestroy() {
        // unregister the broadcast receivers
        unregisterReceiver(bluetoothDeviceFoundReceiver);
        unregisterReceiver(bluetoothDeviceDiscoverableReceiver);
        unregisterReceiver(myBTCastReceiver);

        super.onDestroy();
    }

    // Function to set the text of statusText UI element
    public void setStatusText(String newText) {
        String oldText = (String) statusText.getText();
        String total = newText + "\n" + oldText;
        statusText.setText(total);
    }
}
