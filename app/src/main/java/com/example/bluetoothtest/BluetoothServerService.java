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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
        we can transmit the battery percentage of the camera and inform the user through the app
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
    // Debugging
    private static final String TAG = "BluetoothServerService";
    private static final String appNAME = "EPSLSenseLabBluetooth";
    private static final boolean D = true;
    private static final int BufferSize = 1024;

    // name for the app

    // We will use one UUID across all devices, both cameras and phones
//    private static final UUID MY_UUID = UUID.fromString("a65c117d-4633-4072-9d38-3e15808c140e");

//    byte [] byte_uuid_esp32 = {0x00, 0x00, 0x11, 0x01, 0x00, 0x00, 0x10, 0x00,
//            0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB};
    // string equivalent of ESP32 UUID: 00001101-0000-1000-8000-00805f9b34fb
    // this is the generic UUID for Bluetooth Serial communication
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Bluetooth vars
    private final BluetoothAdapter myBTAdapter;
    
    // these two variables are used when the phone want to connect to a Bluetooth device as a client.
    // Most probably we will not be using these in our application of phone and camera communication
    // since, phone will be the server and the camera client always.
    private BluetoothDevice myRemoteBTDevice;
    private UUID myRemoteBTDeviceUUID;

//    // Context
//    private Context myContext;

    // Service and Thread
    private AcceptThread myAcceptThread;
    private ConnectThread myConnectThread;
    private ConnectedThread myConnectedThread;

    // Constants that indicates the current connection state
    public static final int STATE_NONE = 0; // bluetooth is inactive
    public static final int STATE_ACCEPTING = 1; // accepting new connections
    public static final int STATE_CONNECTED = 2; // bluetooth connection established
    public static final int STATE_CONNECTING = 3; // connecting as a bluetooth client

    // State variable
    private int myState = STATE_NONE;

    // Constructor
    public BluetoothServerService(BluetoothAdapter mBTAdapter, Context context) {
        this.myBTAdapter = mBTAdapter;
//        this.myContext = context;
        myState = STATE_NONE;
        // we may need to pass an handler to inform the UI thread about the Bluetooth events
    }

    /*
    Return the current connection state
     */
    public synchronized int getState() {
        return myState;
    }

    /*
    Set the current connection state
     */
    public synchronized void setState(int mS) {
        myState = mS;
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
    Start accepting bluetooth connections. The phone will be server and waits for the client
    (camera) to initiate the connection.
     */
    public synchronized void startAcceptingConnection() {

        // cancel any thread that is trying to connect to other servers, just in case
        stopClient();

        // cancel any thread currently running a connection
        closeConnectedSocket();

        // start the thread to listen for incoming connections
        if(myBTAdapter != null) {
            if (myAcceptThread == null) {
                if(D)
                    Log.d(TAG, "starting accept thread");
                myAcceptThread = new AcceptThread();
                myAcceptThread.start();
                setState(STATE_ACCEPTING);
            }
        } else {
            if(D)
                Log.d(TAG, "startAcceptingConnection: null bluetooth adapter");
        }
    }

    /*
    Stop accepting bluetooth connections.
     */
    public synchronized void stopAcceptingConnection() {
        if(myAcceptThread != null){
            if(D)
                Log.d(TAG, "stopAcceptingConnection: stopping accept thread");
            myAcceptThread.close();
            myAcceptThread = null;
            setState(STATE_NONE);
        }

        // close any connected thread running a connection
//        closeConnectedSocket();
    }


    /*
    Starts the connect thread to initiate connection to a Bluetooth server. The function calling
    device will be the client and the accept thread running device will be the server.
    */
    public synchronized void startClient(final BluetoothDevice serverDevice) {

        // cancel any thread attempting to make a connection
        if(myState == STATE_CONNECTING) {
            if(myConnectThread != null) {
                if(D)
                    Log.d(TAG, "startClient: starting connect thread");
                myConnectThread.close();
                myConnectThread = null;
            }
        }

        // close any connected thread running a connection
        closeConnectedSocket();

        // start the thread to connect to a bluetooth server
        myConnectThread = new ConnectThread(serverDevice, MY_UUID);
        myConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /*
    Stops the bluetooth client operations.
     */
    public synchronized void stopClient() {

        if(myConnectThread != null){
            if(D)
                Log.d(TAG, "stopClient: stopping connect thread");
            myConnectThread.close();
            myConnectThread = null;
        }

        // close any connected thread running a connection
//        closeConnectedSocket();
    }

    /*
    This function is called once a bluetooth connection is established. Starts the connected thread
    to manage a Bluetooth connection.
     */
    private synchronized void manageConnectedSocket(BluetoothSocket mmSocket) {
        if(D)
            Log.d(TAG, "manageConnectedSocket: starting manage connected connection thread");

        // cancel any thread that is trying to establish a connection
        stopClient();

        // cancel any thread currently running a bluetooth connection
        closeConnectedSocket();

        // cancel accepting connections
        stopAcceptingConnection();

        // start the thread to manage the connection and perform transmission
        myConnectedThread = new ConnectedThread(mmSocket);
        myConnectedThread.start();
        setState(STATE_CONNECTED);

        // When done with the connected socket, always call close on the socket to release the resources.
//        try {
//            mmSocket.close();
//        } catch (IOException e) {
//            Log.e(TAG, "manageConnectedSocket: Failed to close a connected socket ", e);
//        }
    }

    /*
    Close any thread that is currently running bluetooth connections.
     */
    public synchronized void closeConnectedSocket() {

        if(myConnectedThread != null) {
            if(D)
                Log.d(TAG, "closeConnectedSocket: stopping manage connected connection thread");
            myConnectedThread.close();
            myConnectedThread = null;
        }

//        setState(STATE_NONE);
    }


    /*
    Stop all Bluetooth thread.
     */
    public synchronized void stopAllThread() {
        if(D)
            Log.d(TAG, "stopAllThread: stopping all threads");

        // stop any thread trying to connect as client to a server
        stopClient();

        // stop any thread that is currently running a bluetooth connection
        closeConnectedSocket();

        // stop any thread that is waiting for a bluetooth connection from a client
        stopAcceptingConnection();

        setState(STATE_NONE);
    }


    /*
    Write data to the connected thread in an unsynchronized manner
     */
    public void writeBytes(byte[] outBuffer) {
        // create temporary object
        ConnectedThread r;

        // Synchronized a copy of the Connected Thread
        synchronized (this) {
            if (myState != STATE_CONNECTED)
                return;
            r = myConnectedThread;
        }

        // write data over the connected connection
        r.write(outBuffer);
    }

    /*
        This function is called to re-establish the bluetooth connection with the camera module
        if the connection was failed to be established. We try to connect to the camera after 5
        seconds.
     */
    private void connectionFailed() {
        // after sometime (5 seconds) again try to connect to the same remote device
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (myRemoteBTDevice != null){
                    BluetoothServerService.this.startClient(myRemoteBTDevice);
                }
            }
        }, 5000);
    }

    /*
        Connection lost to the remote device. Start the bluetooth server again and wait for the
        incoming connection.
     */
    private void connectionLost() {
        BluetoothServerService.this.startAcceptingConnection();
    }

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
                tmp = myBTAdapter.listenUsingInsecureRfcommWithServiceRecord(appNAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: Socket's listen method failed", e);
            }
            mmServerSocket = tmp;
        }

        // run method to listen for incoming connections
        public void run() {
            if(D)
                Log.d(TAG, "run: AcceptThread Running");

            BluetoothSocket socket = null;

            // keep listening until exception occurs or a socket is returned.
            while (myState != STATE_CONNECTED) {
                try {
                    // this is a blocking call and hence must be ran on a separate thread.
                    // Also call close() on BluetoothServerSocket or BluetoothSocket to exit our of it.
                    if(D)
                        Log.d(TAG, "run: RFCOMM server socket accepting connections");

                    socket = mmServerSocket.accept();

                    if(D)
                        Log.d(TAG, "run: RFCOMM server accepted connection");
                } catch (IOException e) {
                    Log.e(TAG, "run: Socket's accept method failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothServerService.this) {
                        switch (myState){
                            case STATE_NONE:
                            case STATE_ACCEPTING:
                            case STATE_CONNECTING:
                                // Normal case, start the connected thread to manage the connection
                                manageConnectedSocket(socket);
                                break;
                            case STATE_CONNECTED:
                                // Either the socket is not ready or already connected.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "could not close unwanted socket in accept thread", e);
                                }
                                break;


                        }
                    }
//                    // A connection was accepted. Perform work associated with the connection in a
////                    // separate thread.
////                    close();
////                    break;
                }
            }
            if(D)
                Log.d(TAG, "run: End of Accept Thread");
        }

        // provide the close method to close the BluetoothServerSocket or BluetoothSocket
        public void close() {
            try {
                if(D)
                    Log.d(TAG, "close: Closing the accept socket.");
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "run: Socket close method failed", e);
            }
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
            if(D)
                Log.d(TAG, "run: Connect Thread");
            myRemoteBTDevice = device;
            myRemoteBTDeviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;

            // try to get a Bluetooth socket for the given Bluetooth device
            if(D)
                Log.d(TAG, "run: Connecting with device " + myRemoteBTDevice.getAddress());

            try {
                tmp = myRemoteBTDevice.createRfcommSocketToServiceRecord(myRemoteBTDeviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to create a RFCOMM socket", e);
            }
            
            mmSocket = tmp;
            
            // cancel device discovery because it will slow down a connection
            myBTAdapter.cancelDiscovery();
            
            // Make a connection to the Bluetooth Socket
            try {
                // this is a blocking call and will only return on a successful connection or an 
                // exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to connect with the device", e);

                // no matter what close the socket, even if there was an exception
                close();

                // TODO connection failed. Do you want to retry ??
//                connectionFailed();

                // for now just return from here without going to the manage connection thread
                return;
            }

            // reset the connect thread because we are done with this thread
            synchronized (BluetoothServerService.this) {
                myConnectThread = null;
            }

            // we have a connected socket, now manage the connection. Start the connected thread
            manageConnectedSocket(mmSocket);
        }
        
        // Function to cancel the connect blocking call
        public void close() {
            try {
                if(D)
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
            if(D)
                Log.d(TAG, "ConnectedThread: started");

            // save the socket
            mmSocket = socket;

            // get the input and output stream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // get the input and output stream
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Failed to get the input/output streams", e);
            }

            this.mmInputStream = tmpIn;
            this.mmOutputStream = tmpOut;
            if(D)
                Log.d(TAG, "ConnectedThread: Input output stream set");
        }

        public void run() {
            // buffer to store the input data
            byte[] buffer = new byte[BufferSize];
            int nBytes, availableBytes;

            // keep listening to the Input Stream while connected or until an exception occurs
            while(true) {
                try {
                    if(D)
                        Log.d(TAG, "connected thread: waiting for data over receive stream");

//                    availableBytes = mmInputStream.available();
//                    Log.d(TAG, "run: available data " + availableBytes);

                    // read data from input stream, until the buffer is full or no more bytes are left to read
                    nBytes = mmInputStream.read(buffer,  0, buffer.length);
//                    int bytesRead = inputStream.Read(buffer, 0, buffer.Length);

                    // log the read data: We assume the read bytes is text
                    if(nBytes != -1) {
                        String inMessage = new String(buffer, 0, nBytes);
                        Log.d(TAG, "run: Read Data: " + inMessage);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "run: Error reading data from BT device", e);

                    //TODO Connection lost. Do you want to start the service again or not??
                    connectionLost();
                    break;
                }
            }
        }

        // Function to write data to the connected Bluetooth device
        public void write(byte[] buffer) {
            // print out the buffer content
            if(D) {
                String txt = new String(buffer, Charset.defaultCharset());
                Log.d(TAG, "connected thread: writing data " + txt);
            }

            // write the data to the output stream
            try {
                mmOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "connected thread: failed sending data", e);
            }
        }

        // Function to close the blocking read and write calls
        public void close() {
            try {
                if(D)
                    Log.d(TAG, "connected thread: Closing the connected socket");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "connected thread: Failed to close the socket", e);
            }
        }
    }

    // Send dummy data via Bluetooth
    public void write(byte[] outBuffer) {
        if(myConnectedThread != null) {
            myConnectedThread.write(outBuffer);
        }
    }

}
