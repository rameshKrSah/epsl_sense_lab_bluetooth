package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;


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

For connecting to remote device we don't need to make the device discoverable.
Enabling discoverability is only necessary when we want the app to host a server socket that
accepts incoming connections. Also, once the remote device is paired once, make sure to cancel
device discovery.
*/


public class BluetoothController {
    private static final String TAG = "BluetoothController";

    // Bluetooth service object
    private BluetoothService myBluetoothService;

    // Bluetooth adapter variable
    private BluetoothAdapter myBluetoothAdapter;

    // Broadcast listeners
    protected BluetoothBaseListener myBaselisteners;
    protected BluetoothBroadcastReceiver myReceivers;

    // Bluetooth manager
    private BluetoothManager myBluetoothManager;

    // We need reference to MainActivity for context of the application.
    private MainActivity myMainActivity;
    private Context myContext;

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

    // Handler object used for various functions.
    private Handler myHandler;

    // Array to store the found device during scanning
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    // Constructor for the class
    public BluetoothController(MainActivity mainActivity) {
        this.myMainActivity = mainActivity;
        this.myContext = myMainActivity.getApplicationContext();

        // get the default Bluetooth adapter
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (myBluetoothAdapter == null) {
            Utils.toast(myContext, "Bluetooth not available.");
            return;
        }

        // get the Bluetooth service instance
        myBluetoothService = new BluetoothService(myBluetoothAdapter);

        // get a new Handler
        myHandler = new Handler();
    }

    /*
        Register broadcast listeners for the current context.
     */
    protected void registerBroadCastReceivers(){
        if (myBaselisteners == null || myContext == null){
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        myReceivers = new BluetoothBroadcastReceiver(myBaselisteners);
        myContext.registerReceiver(myReceivers, filter);
    }

    /*
    Scan for Bluetooth devices and cancel the discovery after the scan period.
    */
    private void scanForBluetoothDevices() {
        // if not already scanning, start the scan with a runnable which is called automatically
        // after the scan period to cancel discovery
        if (!this.mScanning) {
            // reset the Bluetooth devices list so that we can populate this with new found devices
            mBTDevices = new ArrayList<>();
            setStatusText("Starting scan for devices ..");

            // we will use the handler to stop the scanning after scan period set by the caller.
            this.myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setStatusText("Stopping the scan..");
                    mScanning = false;
                    myBluetoothAdapter.cancelDiscovery();
                    stopScan();
                }
            }, this.scanPeriod);

            // start discovery or scan for Bluetooth devices
            mScanning = true;
            myBluetoothAdapter.startDiscovery();
        }
    }

    /*
        Check if a Bluetooth device is already paired or not.
        @param: deviceMAC: MAC address
    */
    private boolean isBTDevicePaired(final String deviceMAC) {
        Set<BluetoothDevice> pairedDevices = this.myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // if there are paired devices, then loop over them and check for address match
            for (BluetoothDevice dv : pairedDevices) {
                if (dv.getAddress().equals(deviceMAC)) {
                    return true;
                }
            }
        }
        else {
            setStatusText("No paired devices found");
        }

        return false;
    }

    /*
        Wrapper function for the status text field in the main activity.
        Comment out in final version.
    */
    private void setStatusText(String str) {
        myMainActivity.setStatusText(str);
    }


    /*
        Unregister the Bluetooth broadcast listeners.
     */
    public void unRegisterBluetoothBroadcastListeners(){
        myContext.unregisterReceiver(myReceivers);
    }

    /*
        Register the Bluetooth broadcast listeners.
        @param listener, a BluetoothBaseListener
     */
    public void setBluetoothBroadcastListeners(BluetoothBaseListener listener) {
        this.myBaselisteners = listener;
        registerBroadCastReceivers();
        if (myBluetoothService != null) {
//            myBluetoothService.setBluetoothListeners(myBaselisteners);
        }
    }

    /*
        Return the current device Bluetooth adapter.
     */
    public BluetoothAdapter getMyBluetoothAdapter() {
        return myBluetoothAdapter;
    }

    /*
        Is current device's Bluetooth available or not.
     */
    public boolean isBTAvailable() {
        return myBluetoothAdapter != null;
    }

    /*
        Is current device's Bluetooth enabled or not.
     */
    public boolean isBTEnabled() {
        if (isBTAvailable()) {
            return myBluetoothAdapter.isEnabled();
        }
        return false;
    }

    /*
        Enable Bluetooth. Check whether Bluetooth is available on the device or not. If yes, check
        if it is enabled or not. If not then enable Bluetooth.
     */
    public boolean enableBluetooth() {
        // lets not use the intent to enable the Bluetooth. Since we know when we are enabling the
        // Bluetooth and also for what purposes. We will just enable it without asking the user.
        if (!isBTAvailable()) {
            return false;
        }
        return myBluetoothAdapter.enable();
    }

    /*
        Disable the Bluetooth if it is turned on. Else do nothing.
     */
    public boolean disableBluetooth() {
        if(isBTAvailable() && isBTEnabled()){
            setStatusText("Bluetooth turned off");
            return myBluetoothAdapter.disable();
        }

        Utils.toast(myContext,"Bluetooth is already off");
        return false;
    }

    /*
        Make the phone Bluetooth discoverable for other devices.
    */
    public void makeDiscoverable() {
        if (isBTAvailable() && isBTEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
            myMainActivity.startActivityForResult(discoverableIntent, this.REQUEST_DISCOVER_BT);
        } else {
            Utils.toast(myContext, "Turn on the Bluetooth first.");
        }
    }

    /*
        Start scan for Bluetooth devices.
        @param: scanPeriod: the time for which the scan will run.
        For each device found, a callback (Broadcast receiver) will be called with device details.
     */
    public void startScan(long scanPeriod) {
        this.scanPeriod = scanPeriod;

        if(isBTEnabled() && isBTEnabled()){
            scanForBluetoothDevices();
        } else {
            Utils.toast(myContext, "Turn on the Bluetooth first.");
        }
    }

    /*
        Stop the scan for Bluetooth devices.
     */
    public void stopScan() {
        if(isBTEnabled() && isBTEnabled()){
            mScanning = false;
            myBluetoothAdapter.cancelDiscovery();
        } else {
            Utils.toast(myContext, "Turn on the Bluetooth first.");
        }
    }

    /*
        Save a Bluetooth device found during a scan in an array.
     */
    public void addDiscoveredBTDevice(BluetoothDevice dv) {
        Log.d(TAG, "addDevice: " + dv.getAddress());
        mBTDevices.add(dv);
    }

    /*
        Check whether the Bluetooth is scanning or not.
     */
    public boolean isScanning() { return this.mScanning; }

    /*
        List the paired Bluetooth devices.
     */
    public void listPairedDevices() {
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
    }

    /*
        Given a MAC address returns the Bluetooth device object for that MAC.
        @param: MAC: MAC address
     */
    public BluetoothDevice findBTDeviceByMac(final String MAC) {
        if (isBTAvailable() && isBTEnabled()) {
            return myBluetoothAdapter.getRemoteDevice(MAC);
        }
        else {
            Utils.toast(myContext, "Turn on the Bluetooth first.");
            return null;
//            throw new RuntimeException("Bluetooth is not available. Can't find device by MAC");
        }
//        Set<BluetoothDevice> pairedDevices = this.myBluetoothAdapter.getBondedDevices();
//        if (pairedDevices.size() > 0) {
//            // if there are paired devices, then loop over them and get their attributes:
//            for (BluetoothDevice device : pairedDevices) {
//                if (device.getAddress().equals(MAC)) {
//                    return device;
//                }
//            }
//        }
    }

    /*
        Given a MAC address, pair with a Bluetooth device with this MAC address.
        @param: deviceMAC: MAC address of the device we want to pair with.
     */
    public void pairBluetoothDevice(final String deviceMAC) {
        Log.d(TAG, "pairBluetoothDevice: " + deviceMAC);

        // first check if the device is already paired or not.
        if(!isBTDevicePaired(deviceMAC)) {
            // device is not paired. Now scan for nearby Bluetooth devices for 10 seconds
            stopScan();
            startScan(10000);

            // post a delayed runnable for pairing after the scan is finished.
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // check if there is a discovered device with the matching MAC address
                    for(BluetoothDevice dv: mBTDevices) {
                        if (dv.getAddress().equals(deviceMAC)) {
                            Log.d(TAG, "pairBluetoothDevice: Found the device. ");

                            // yes a device was found with the specified mac. Now bond with the device
                            Log.d(TAG, "pairBluetoothDevice: Pairing now.. ");

                            // first stop scanning if not stopped yet.
                            stopScan();

                            // try to pair with the device
                            dv.createBond();

                            // no need to loop over other devices.
                            break;
                        }
                    }
                }
            }, 10000);

        } else {
            setStatusText("Device with MAC " + deviceMAC + " already paired.");
        }
    }


    /*
        Start the Bluetooth connection as the server, that a client can connect to
     */
    public void startAsServer(){
        myBluetoothService.startServer();
    }

    /*
        Stop the running Bluetooth server if any.
     */
    public void stopServer() {
        myBluetoothService.stopServer();
    }

    /*
        Start the Bluetooth connection as the client, which connects to remote servers.
     */
    public void startAsClient(String remoteMac) {
        myBluetoothService.startClient(findBTDeviceByMac(remoteMac));
    }

    /*
        Stop the Bluetooth client connection if any.
     */
    public void stopClient(){
        myBluetoothService.stopClient();
    }

    /*
        Send data (write) over any Bluetooth connection that is running.
     */
    public void sendData(byte [] data) {
        myBluetoothService.writeBytes(data);
    }
}
