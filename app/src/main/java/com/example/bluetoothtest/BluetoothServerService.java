package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;

/*
    This is how the phone and the camera module will communicate.

    1. The phone will have Bluetooth always turned on, and will listen for incoming connection in the
        background (in a separate thread).
    2. Once a connection is established, the connected device needs to be verified. The phone will
        use the MAC address of the connected device for this.
    3. The camera goes to sleep and wakes up intermittently so the Bluetooth connection will be
        initiated by the camera module because the phone does not know when the camera is alive and
        when it is in sleep mode. For this UUID used in the phone app for Bluetooth connection will
        be shared with the camera module and also both devices will know each other MAC address. The
        MAC address of the camera can be saved in the setting page of the phone app and the MAC
        address of the phone can be hard coded in the firmware of the camera module or we can have a
        configurable option (preferred).
    4. Once the connection is established, the phone will ask the camera module for update. For example
        we can transmit the the battery percentage of the camera and inform the user through the app
        if the battery needs to be charged. The phone will then issue any new commands to change the
        settings of the camera module. With acknowledgment of successful reception of the settings,
        the camera will inform the phone about the images that needs to be transmitted. The phone
        will make preparation for images reception (send one (or more images) --> Ack the reception
        --> Repeat) and instruct the camera to transmit the images.
    5. After image is transmitted (needs to have a measure to indicate this; transmit all images,
        transmit recent images, transmit for 1 minutes etc) the camera goes to sleep and phone waits
        for the camera to connect again.

 */

public class BluetoothServerService {
    private static final String TAG = "BluetoothServerService";

    // name for the app
    private static final String appNAME = "EPSL Sense Lab Bluetooth";

    // UUID will be configurable from the device settings menu. Each camera module will have unique
    // UUID and the app needs to know that to establish a connection.
    private static final UUID MY_UUID = UUID.fromString("a65c117d-4633-4072-9d38-3e15808c140e");

    // Bluetooth vars
    private BluetoothAdapter mBTAdapter;
    
    // these two variables are used when the phone want to connect to a Bluetooth device as a client.
    // Most probably we will not be using these in our application of phone and camera communication
    // since, phone is the server and the camera client always. 
    private BluetoothDevice mBTDevice;
    private UUID mBTDeviceUUID;

    // Context
    private Context myContext;

    // Service and Thread
    private AcceptThread myAcceptThread;
    private ConnectThread myConnectThread;
    private ConnectedThread myConnectedThread;

    // Constructor
    public BluetoothServerService(BluetoothAdapter mBTAdapter, Context context) {
        this.mBTAdapter = mBTAdapter;
        this.myContext = context;
    }

    /*
    1. One device opens a server socket, and the other must initiate the connection using the server
    device MAC address : Phone will be the server and will listen for incoming connection. Camera
    will initiate the connection using the Phone MAC address.

    2. The server and client are considered connected to each other when they each have a connected
    BluetoothSocket on the same RFCOMM channel.

    3. If the devices that are trying to connect are not paired, then pairing dialog is shown automatically.

    4. Server holds an open BluetoothServerSocket open and listen for incoming connection. Once a
    request is accepted, server returns a connected BluetoothSocket. After this BluetoothServerSocket
    should be discarded.
     */
    /*
    This thread runs while listening for incoming Bluetooth connections. This thread makes the phone
    a sever and the camera module will be the client. It runs until a connection is accepted or
    (or until canceled).
     */
    private class AcceptThread extends Thread {
        /*
        1. Get a BluetoothServerSocket
        2. Start listening for connection requests by calling accept()
        3. Close the server socket once connection is accepted.
         */

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // use a temporary object that is later assigned to mmServerSocket because mmServerSocket
            // is final
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBTAdapter.listenUsingInsecureRfcommWithServiceRecord(appNAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: Socket's listen method failed", e);
            }
            mmServerSocket = tmp;
        }

        // run method to listen for incoming connections
        public void run() {
            Log.d(TAG, "run: AcceptThread Running");

            BluetoothSocket socket = null;

            // keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    // this is a blocking call and hence must be ran on a separate thread.
                    // Also call close() on BluetoothServerSocket or BluetoothSocket to exit our of it.
                    Log.d(TAG, "run: RFCOMM server socket accepting connections");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "run: RFCOMM sercer accepted connection");
                } catch (IOException e) {
                    Log.e(TAG, "run: Socket's accept method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with the connection in a s
                    // separate thread.
                    manageConnectedSocket(socket);
                    close();
                    break;
                }
            }

            Log.d(TAG, "run: End of Accept Thread");
        }

        // provide the close method to close the BluetoothServerSocket or BluetoothSocket
        public void close() {
            try {
                Log.d(TAG, "close: Closing the accept socket.");
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "run: Socket close method failed", e);

            }
        }
    }

    // call this function to start accepting Bluetooth connection
    public void startAcceptingConnection() {
        if(mBTAdapter != null) {
            myAcceptThread = new AcceptThread();
            myAcceptThread.start();
        } else {
            Log.d(TAG, "startAcceptingConnection: Null BT Adapter");
        }
    }

    // call this method from another threads to stop accepting Bluetooth connection
    public void stopAcceptingConnection() {
        if(myAcceptThread != null){
            myAcceptThread.close();
            myAcceptThread = null;
        } else {
            Log.d(TAG, "stopAcceptingConnection: Cannot close a non-accepting socket");
        }
    }

    /*
    This thread runs while attempting to make outgoing connection with a device. Here, the phone
    is the client that want to connect to a Bluetooth server. This thread runs straight through and
    the connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        
        public ConnectThread (BluetoothDevice device, UUID uuid) {
            // we will need the Bluetooth device and UUID of the server we want to connect to
            mBTDevice = device;
            mBTDeviceUUID = uuid;
        }
        
        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "run: Connect Thread");
            
            // try to get a Bluetooth socket for the given Bluetooth device
            Log.d(TAG, "run: Connecting with device " + mBTDevice.getAddress());
            try {
                tmp = mBTDevice.createRfcommSocketToServiceRecord(mBTDeviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to create a RFCOMM socket", e);
            }
            
            mmSocket = tmp;
            
            // cancel device discovery
            mBTAdapter.cancelDiscovery();
            
            // Make a connection to the Bluetooth Socket
            try {
                // this is a blocking call and will only return on a successful connection or an 
                // exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to connect with the device", e);

                // no matter what close the socket, even if there was an exception
                close();
            }
            
            // we have a connected socket, do something with it. Send or receive data and more
            manageConnectedSocket(mmSocket);
        }
        
        // Function to cancel the connect blocking call
        public void close() {
            try {
                Log.d(TAG, "cancel: Closing the connect socket");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to close the socket", e);
            }
        }
    }

    /*
    This thread is responsible for maintaining the Bluetooth connection, sending the data, and
    receiving the data.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInputStream;
        private final OutputStream mmOutputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.mmSocket = socket;

            // get the input and output stream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Failed to get the input/output streams", e);
            }

            this.mmInputStream = tmpIn;
            this.mmOutputStream = tmpOut;
        }

        public void run() {
            // buffer to store the input data
            byte[] buffer = new byte[1024];
            int nBytes;

            // keep reading the input stream until an exception occurs
            while(true) {
                try {
                    nBytes = mmInputStream.read(buffer);
                    String inMessage = new String(buffer, 0, nBytes);
                    Log.d(TAG, "run: Read Data: " + inMessage);
                } catch (IOException e) {
                    Log.e(TAG, "run: Error reading data from BT device", e);
                    break;
                }
            }
        }

        // Function to write data to the connected Bluetooth device
        public void write(byte[] buffer) {
            String txt = new String(buffer, Charset.defaultCharset());
            Log.d(TAG, "write: Sending " + txt);

            try {
                mmOutputStream.write(buffer);
            } catch (IOException e) {
                Log.d(TAG, "write: Failed sending data to BT device", e);
            }
        }

        // Function to close the blocking read and write calls
        public void close() {
            try {
                Log.d(TAG, "cancel: Closing the connected socket");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to close the socket", e);
            }
        }
    }
    
    // function to manage the connected Bluetooth socket.
    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        Log.d(TAG, "manageConnectedSocket: Connection established");
        myConnectedThread = new ConnectedThread(mmSocket);
        myConnectedThread.start();

        // When done with the connected socket, always call close on the socket to release the resources.
//        try {
//            mmSocket.close();
//        } catch (IOException e) {
//            Log.e(TAG, "manageConnectedSocket: Failed to close a connected socket ", e);
//        }
    }
    
    // Close the connected socket if any
    public void closeConnectedSocket() {
        if(myConnectedThread != null) {
            myConnectedThread.close();
            myConnectedThread = null;
        } else {
            Log.d(TAG, "closeConnectedSocket: Cannot close a not connected socket");
        }
    }

}
