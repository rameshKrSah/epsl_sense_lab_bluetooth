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
import java.util.Arrays;
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

public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static final String appNAME = "EPSLSenseLab";
    private static final boolean D = true;

    /*
        The camera can send up to 5Kb at once, but the input stream read can only give us 1058 bytes
        at one time. It does not read all the bytes before returning.
     */
    private static final int BufferSize = 1024 * 2;         // 2 kb bytes

    // string equivalent of ESP32 UUID: 00001101-0000-1000-8000-00805f9b34fb
    // this is the generic UUID for Bluetooth Serial communication
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Bluetooth vars
    private final BluetoothAdapter myBTAdapter;

    // Service and Thread
    private AcceptThread myAcceptThread;
    private ConnectThread myConnectThread;
    private ConnectedThread myConnectedThread;

    // these two variables are used when the phone want to connect to a Bluetooth device as a client.
    // Most probably we will not be using these in our application of phone and camera communication
    // since, phone will be the server and the camera client always.
    private BluetoothDevice myRemoteBTDevice;

    // State variable, that tracks the state of Bluetooth process currently running
    private int myState;

    // Bluetooth listeners
    private BluetoothBaseListener myBaseListeners;

    // Constructor
    public BluetoothService(BluetoothAdapter mBTAdapter) {
        this.myBTAdapter = mBTAdapter;
        this.myState = BluetoothState.STATE_NONE;
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
        if(myBaseListeners != null) {
            myBaseListeners.onBluetoothServiceStateChanged(mS);
        }
    }

    /*
        Set Bluetooth listeners
        @param: BluetoothBaseListener
     */
    public synchronized void setBluetoothListeners(BluetoothBaseListener listener) {
        this.myBaseListeners = listener;
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
    public synchronized void startServer() {

        // close any thread that is trying to connect to other servers
        stopConnectingThread();

        // cancel any thread currently running a connection
        stopConnectedThread();

        // start the thread to listen for incoming connections
        if(myBTAdapter != null) {
            if (myAcceptThread == null) {
                if(D)
                    Log.d(TAG, "starting accept thread");
                myAcceptThread = new AcceptThread();
                myAcceptThread.start();
                setState(BluetoothState.STATE_LISTEN);
            }
        } else {
            if(D)
                Log.d(TAG, "startServer: null Bluetooth adapter");
        }
    }

    /*
    Stop accepting bluetooth connections from remote devices.
 */
    public synchronized void stopServer() {
        // close accept threads if any
        stopAcceptThread();

        // close any connected thread running a connection
        stopConnectedThread();

        // set the state to none, if the state was listening
        if (myState == BluetoothState.STATE_LISTEN) {
            setState(BluetoothState.STATE_NONE);
        }
    }

    /*
    Starts the connect thread to initiate connection to a Bluetooth server. The function calling
    device will be the client and the accept thread running device will be the server.
    */
    public synchronized void startClient(final BluetoothDevice serverDevice) {
        if(D)
            Log.d(TAG, "startClient: starting connect thread");

        // cancel any thread attempting to make a connection
        if(myState == BluetoothState.STATE_CONNECTING && myConnectThread != null) {
                myConnectThread.close();
                myConnectThread = null;
        }

        // close any connected thread running a connection
        stopConnectedThread();

        // start the thread to connect to a bluetooth server
        myConnectThread = new ConnectThread(serverDevice);
        myConnectThread.start();
        setState(BluetoothState.STATE_CONNECTING);
    }

    /*
        Stop the Bluetooth client from connecting to the remote device or close the connected
        connection to the remote device.
     */
    public synchronized void stopClient() {
        // stop any connecting thread if any
        stopConnectingThread();

        // also stop any connected thread
        stopConnectedThread();

        // set state to none if it was connecting
        if (myState == BluetoothState.STATE_CONNECTING) {
            setState(BluetoothState.STATE_NONE);
        }
    }

    /*
    This function is called once a bluetooth connection is established. Starts the connected thread
    to manage a Bluetooth connection.
     */
    private synchronized void manageConnectedSocket(BluetoothSocket mmSocket) {
        if(D)
            Log.d(TAG, "manageConnectedSocket: starting manage connected connection thread");

        // close any thread trying to connect to the remote device
        stopConnectingThread();

        // close any thread currently managing a connected Bluetooth socket
        stopConnectedThread();

        // close any thread accepting connections from remote devices
        stopAcceptThread();

        // start the thread to manage the connection and perform transmission
        myConnectedThread = new ConnectedThread(mmSocket);
        myConnectedThread.start();
        myRemoteBTDevice = mmSocket.getRemoteDevice();
        setState(BluetoothState.STATE_CONNECTED);
    }


    /*
        Stop any thread that is running which accepts connections from remote devices.
     */
    private synchronized void stopAcceptThread() {
        if(myAcceptThread != null) {
            if (D)
                Log.d(TAG, "closeAcceptThread: stopping accept thread");
            myAcceptThread.close();
            myAcceptThread = null;
        }
    }

    /*
        Stop the connecting thread, which attempts to connect to the remote device.
     */
    private synchronized void stopConnectingThread() {

        if(myConnectThread != null){
            if(D)
                Log.d(TAG, "stopClient: stopping connect thread");
            myConnectThread.close();
            myConnectThread = null;
        }
    }

    /*
    Close any thread that is currently running bluetooth connections.
     */
    private synchronized void stopConnectedThread() {
        if(myConnectedThread != null) {
            if(D)
                Log.d(TAG, "closeConnectedSocket: stopping manage connected connection thread");
            myConnectedThread.close();
            myConnectedThread = null;
        }

        // if the state was connected, set it to none
        if (myState == BluetoothState.STATE_CONNECTED) {
            setState(BluetoothState.STATE_NONE);
        }
    }


    /*
        Stop all Bluetooth threads.
     */
    public synchronized void stopAllThread() {
        if(D)
            Log.d(TAG, "stopAllThread: stopping all threads");

        // stop any thread trying to connect as client to a server
        stopConnectingThread();

        // stop any thread that is currently running a bluetooth connection
        stopConnectedThread();

        // stop any thread that is waiting for a bluetooth connection from a client
        stopServer();

        myRemoteBTDevice = null;
        setState(BluetoothState.STATE_NONE);
    }


    /*
    Write data to the connected thread in an unsynchronized manner
     */
    public void writeBytes(byte[] outBuffer) {
        ConnectedThread r;
        // Synchronized a copy of the Connected Thread
        synchronized (this) {
            if (myState != BluetoothState.STATE_CONNECTED){
                if (D)
                    Log.d(TAG, "writeBytes: not connected state");
                return;
            }
            r = myConnectedThread;
        }

        // send the data
        r.write(outBuffer, 0, outBuffer.length);
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
                    BluetoothService.this.startClient(myRemoteBTDevice);
                }
            }
        }, 5000);
    }

    /*
        Connection lost to the remote device. Start the bluetooth server again and wait for the
        incoming connection.
     */
    private void connectionLost() {
        BluetoothService.this.startServer();
    }

    /*
        Get remote connected device
     */
    public BluetoothDevice getRemoteConnectedDevice() { return myRemoteBTDevice;}

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
            while (myState != BluetoothState.STATE_CONNECTED) {
                try {
                    // this is a blocking call and hence must be ran on a separate thread.
                    // Also call close() on BluetoothServerSocket or BluetoothSocket to exit out of it.
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
                    synchronized (BluetoothService.this) {
                        switch (myState){
                            case BluetoothState.STATE_NONE:
                            case BluetoothState.STATE_LISTEN:
                            case BluetoothState.STATE_CONNECTING:
                                // Normal case, start the connected thread to manage the connection
                                manageConnectedSocket(socket);
                                break;
                            case BluetoothState.STATE_CONNECTED:
                                // Either the socket is not ready or already connected.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "could not close unwanted socket in accept thread", e);
                                }
                                break;


                        }
                    }
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
        
        public ConnectThread (BluetoothDevice device) {
            // we will need the Bluetooth device and UUID of the server we want to connect to
            if(D)
                Log.d(TAG, "run: Connect Thread");

            // save the provided device as the remote device
            myRemoteBTDevice = device;

            BluetoothSocket tmp = null;

            // try to get a Bluetooth socket for the given Bluetooth device
            if(D)
                Log.d(TAG, "run: Connecting with device " + myRemoteBTDevice.getAddress());

            try {
                tmp = myRemoteBTDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "run: Failed to create a RFCOMM socket", e);
            }

            // assign the socket
            mmSocket = tmp;
        }

        public void run() {

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

                // TODO connection failed. Do you want to retry ?
//                connectionFailed();

                // for now just return from here without going to the manage connection thread
                return;
            }

            // reset the connect thread because we are done with this thread
            synchronized (BluetoothService.this) {
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
            int nBytes;

            // keep listening to the Input Stream while connected or until an exception occurs
            while(true) {
                try {
                    if(D)
                        Log.d(TAG, "connected thread: waiting for data over receive stream");

                    // read data from input stream, until the buffer is full or no more bytes are left to read
                    nBytes = mmInputStream.read(buffer,  0, 1024 * 2);

                    // log the read data: We assume the read bytes is text
                    if(nBytes > 0) {
//                        String inMessage = new String(buffer, 0, nBytes);
//                        Log.d(TAG, "run: Read Data: " + inMessage);

                        // in here call the on data listener to process the received data.
                        byte[] data = Arrays.copyOf(buffer, nBytes);
                        if (myBaseListeners != null) {
                            ((BluetoothBaseListener) myBaseListeners).onReadData(mmSocket.getRemoteDevice(), data);
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "run: Error reading data from BT device", e);

                    //TODO Connection lost. Do you want to start the service again or not??
                    connectionLost();

                    // need to first verify the state variable
//                    setState(BluetoothState.STATE_DISCONNECTED);
                    break;
                }
            }
        }

        // Function to write data to the connected Bluetooth device
        public void write(byte[] buffer, int start, int end) {
            // print out the buffer content
            if(D){
                String txt = new String(buffer, Charset.defaultCharset());
                Log.d(TAG, "connected thread: writing data " + txt);
            }

            // write the data to the output stream
            try {
                mmOutputStream.write(buffer, start, end);
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
}
