package com.example.bluetoothtest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import static androidx.core.app.ActivityCompat.startActivityForResult;


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

/*
    Notes
    1. Classic Bluetooth is the for battery-intensive operations. From Android 4.3 (API 18) Bluetooth
    low energy is also supported.

    2. First form a Bluetooth channel between the pairing devices.
    3. Another device finds the discoverable device using a service discovery process.
    4. After the discoverable device accepts the pairing request, the two device complete a bonding
    process where they exchange security keys. The device caches these keys for later use.
    5. After pairing and bonding data can be sent and received over the established channel.
    6. After the session is complete, the device which initiated the pairing process releases the
    channel but the two device remains bonded so that they can reconnect automatically if they are in
    range.

    We need three permissions:
    1. BLUETOOTH :: For Bluetooth functionality
    2. BLUETOOTH_ADMIN :: To initiate device discovery or manipulate Bluetooth settings.
    3. ACCESS_FINE_LOCATION


    How to set up the Bluetooth ?
    1. Verify Bluetooth is present in the device. Use BluetoothAdapter
    2. Check if Bluetooth is enable or not. If not ask user to enable it.


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
    private static final int REQUEST_ENABLE_BT = 100;
    private static final int REQUEST_DISCOVER_BT = 101;
    public static final int DISCOVERABLE_DURATION = 300; // 5 minutes, by default it is 2 minutes

    // UI items
    Button onButton, offButton, discoverButton, scanButton;
    TextView statusText;

    private BroadCastReceiver_Bluetooth myBTCastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the Bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter == null) {
            Utils.toast(this, "Bluetooth is not available on this device.");
            finish();
        }

        // start the Bluetooth broadcast receiver
        myBTCastReceiver = new BroadCastReceiver_Bluetooth(getApplicationContext());

        // register the BroadcastReceiver for the ACTION_FOUND intent of the device discover function.
        // make sure to unregister the receiver in the onDestroy function
        IntentFilter discoverIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceFoundReceiver, discoverIntentFilter);

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

    // Make the Phone Bluetooth discoverable for other devices.
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
     */
    private final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the Bluetooth device details from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMAC = device.getAddress();
                setStatusText(deviceName + "\t " + deviceMAC);
            }
        }
    };


    /*
   In order to create a connection between two devices, we must implement both the server side and
   client side mechanisms, because one device must open a server socket, and the other one must
   initiate the connection using the server device's MAC address.

   The server receives socket information when an incoming connection is accepted. The client provides
   socket information when it opens an RFCOMM channel to the server. The server and client are
   considered connected to each other when they each have a connected BluetoothSocket on the same
   RFCOMM channel. Now, each device can obtain input and output streams, and transfer data.
     */

    /*
    We can prepare each device as a server so that each device has a server socket open and is
    listening for connections. Now, either device can initiate a connection with the other and
    become the client. Or one device can explicitly host the connection and open a server socket on
    demand, and the other device initiates the connection.
     */

    /*
    When we want to connect two devices, one must act as a server by holding an open
    BluetoothServerSocket. BluetoothServerSocket listens for incoming connection requests and provide
    a connected BluetoothSocket once a request is accepted. The accept() call is blocking and hence
    should not be executed on the UI thread. It usually makes sense to do all work that involves a
    BluetoothServerSocket and BluetoothSocket in a new thread managed by the application.

    To abort a blocked call such as accept(), call close() on the BluetoothServerSocket or
    BluetoothSocket from another thread. All methods in BluetoothServerSocket and BluetoothSocket
    are thread safe.

    To use a matching UUID, hard-code the UUID string into your application, and then reference it
    from both the server and client code.
     */

    // Listeners for the Bluetooth events and more
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
    protected void onDestroy() {
        // unregister the bluetooth device
        unregisterReceiver(bluetoothDeviceFoundReceiver);
        super.onDestroy();
    }

    // Function to set the text of statusText UI element
    private void setStatusText(String newText) {
        String oldText = (String) statusText.getText();
        String total = newText + "\n" + oldText;
        statusText.setText(total);
    }
}
