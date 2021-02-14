package com.example.bluetoothtest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    // Locally defined integer constant that must be greater than 0.
    public static final int REQUEST_ENABLE_BT = 100;
    public static final int REQUEST_DISCOVER_BT = 101;
    public static final int BT_CONN_STATUS = 102;

    // By default, we can make the device discoverable for 2 minutes. Here we have used 5 minutes.
    // The longest possible discoverable time is 1 hour. If the DISCOVERABLE_DURATION is set 0 then
    // the device is always discoverable.
    public static final int DISCOVERABLE_DURATION = 300;

    // Bluetooth Manager for the application
    private BluetoothController myBluetoothController;

    // The implementations of the Bluetooth broadcast listeners.
    private BluetoothListenersImplementation myBluetoothBCListeners;

    // One Plus MAC Address : "64:A2:F9:3E:95:9D"
    // Galaxy S4 MAC Address : "C4:50:06:83:F4:7E"
    // Camera MAC Address : "FC:F5:C4:0D:05:D6"

    // MAC Address for the server and the client devices. The phone will be server and the camera
    // will be client.
    final String serverMAC = "FC:F5:C4:0D:05:D6";
    final String clientMAC = "FC:F5:C4:0D:05:D6";

    // Bluetooth service that handle Bluetooth connections and data transmission
//    private BluetoothService myBluetoothService;

    // UI items
    Button onButton, offButton, discoverButton, scanButton, pairButton, pairedButton,
            connectButton, acceptButton, sendButton;
    TextView statusText;

    // Boolean for indicating whether we are accepting connections or not
    Boolean acceptingConnection = false;
    Boolean alreadyConnected = false;

    private static Boolean doWeHaveBluetooth = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the object of Bluetooth manager class
        myBluetoothController = new BluetoothController(this);

        // create the functions for the
        myBluetoothBCListeners = new BluetoothListenersImplementation(myBluetoothController);

        // set the broadcast listeners
        myBluetoothController.setBluetoothBroadcastListeners(myBluetoothBCListeners);

        // get the object of the Bluetooth service class
//        myBluetoothService = new BluetoothService(myBluetoothController.getMyBluetoothAdapter());

        // find the UI elements
        onButton = (Button) findViewById(R.id.onSwitch);
        offButton = (Button) findViewById(R.id.offSwitch);
        discoverButton = (Button) findViewById(R.id.discoverSwitch);
        scanButton = (Button) findViewById(R.id.scanSwitch);
        pairButton = (Button) findViewById(R.id.pairSwitch);
        pairedButton = (Button) findViewById(R.id.pairedSwitch);
        connectButton = findViewById(R.id.connectSwitch);
        acceptButton = (Button) findViewById(R.id.acceptSwitch);
        sendButton = findViewById(R.id.sendSwitch);
        statusText = (TextView) findViewById(R.id.statusText);

        // set the on click listeners for the button
        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetoothController.enableBluetooth();
            }
        });

        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetoothController.disableBluetooth();
            }
        });

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetoothController.makeDiscoverable();
            }
        });

        pairedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetoothController.listPairedDevices();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // scan for 10 seconds : After 10 seconds the scanning is automatically stopped.
                myBluetoothController.startScan(10000);
            }
        });

        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetoothController.pairBluetoothDevice(clientMAC);
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!alreadyConnected){
                    startClient();
                    connectButton.setText("Disconnect Now");
                    alreadyConnected = true;
                } else {
                    stopClient();
                    connectButton.setText("Connect Now");
                    alreadyConnected = false;
                }
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothController.isBTAvailable() && myBluetoothController.isBTEnabled()) {
                    if (!acceptingConnection) {
                        myBluetoothController.startAsServer();
                        acceptButton.setText("Decline Connections");
                        acceptingConnection = true;
                    } else {
                        myBluetoothController.stopServer();
                        acceptButton.setText("Accept Connections");
                        acceptingConnection = false;
                    }
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData();
            }
        });
    }

    /*
     Listeners for the Bluetooth events and more. The function is called after Bluetooth related
     operations are requested. For example turning on Bluetooth, making Bluetooth device
     discoverable, etc.
     */
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
        // register broadcast receivers
//        myBluetoothController.registerBroadcastReceivers();

    }

    @Override
    protected void onDestroy() {
        // unregister the broadcast receivers
//        myBluetoothController.unregisterBroadcastReceivers();
        myBluetoothController.unRegisterBluetoothBroadcastListeners();
        super.onDestroy();
    }

    // Function to set the text of statusText UI element
    public void setStatusText(String newText) {
        String oldText = (String) statusText.getText();
        String total = newText + "\n" + oldText;
        statusText.setText(total);
    }

    // Function that initiates the Bluetooth connection to a server
    private void startClient() {
        Log.d(TAG, "startConnection: Starting the RFCOMM BT connection");
        // Get the Bluetooth Device for the server MAC
//        BluetoothDevice serverDevice = myBluetoothController.findBTDeviceByMac(serverMAC);
        if(serverMAC != null) {
            myBluetoothController.startAsClient(serverMAC);
        } else {
            Log.d(TAG, "startConnection: Server is not paired yet.");
            setStatusText("Server is not paired yet.");
        }
    }

    // Function that stops the Bluetooth connection to a server
    private void stopClient() {
        Log.d(TAG, "stopClient: Stopping the Bluetooth client");
        if(myBluetoothController.isBTAvailable() && myBluetoothController.isBTEnabled()) {
            myBluetoothController.stopClient();
        }
    }

    // Function to send dummy data over Bluetooth
    private void sendData() {
        if(true) {
            final String text = "Testing Bluetooth Data Transmission";
            Log.d(TAG, "sendData: over Bluetooth" + text);
            myBluetoothController.sendData(text.getBytes());
        }
    }

}
