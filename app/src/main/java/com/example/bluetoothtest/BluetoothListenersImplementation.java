package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BluetoothListenersImplementation implements BluetoothBaseListener {
    private static final String TAG = "BluetoothListenersImple";

    private static BluetoothController myBTController;
    private static BluetoothDataParser myBTDataParser;

    public BluetoothListenersImplementation(BluetoothController controller, BluetoothDataParser dataParser) {
        myBTController = controller;
        myBTDataParser = dataParser;
    }

    @Override
    public void onActionStateChanged(int preState, int state) {
        Log.d(TAG, "onActionStateChanged: Previous state: " +Utils.btStateAsString(preState));
        Log.d(TAG, "onActionStateChanged: Current state: " +Utils.btStateAsString(state));
    }

    @Override
    public void onActionDiscoveryStateChanged(String discoveryState) {
        if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
            Log.d(TAG, "onActionDiscoveryStateChanged: scan started!");
        } else if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            Log.d(TAG, "onActionDiscoveryStateChanged: scan completed!!");
        }
    }

    @Override
    public void onActionScanModeChanged(int preScanMode, int scanMode) {
        /// not necessary callback
        Log.d(TAG, "onActionScanModeChanged: previous scan mode: " + preScanMode +
                " current scan mode: " + scanMode);
    }

    @Override
    public void onBluetoothServiceStateChanged(int state) {
        // this is a very important callback which tracks the connection stage of Bluetooth
        Log.d(TAG, "onBluetoothServiceStateChanged: State " + Utils.btConnStateAsString(state));

        // do something with the state information here.
    }

    @Override
    public void onActionDeviceFound(BluetoothDevice device, short rssi) {
        // this callback will be called when a device is found during scan operation.
        Log.d(TAG, "onActionDeviceFound: Device name: "+device.getName()+", address: "
                +device.getAddress()+", bond state: "+Utils.btBondStateAsString(device.getBondState()));

        // call a function that handles whatever operation when a device is found.
        if(myBTController != null) {
            myBTController.addDiscoveredBTDevice(device);
        }
    }

    @Override
    public void onBondStateChanged(BluetoothDevice device, int bondState, int preBondState) {
        Log.d(TAG, "onBondStateChanged: Device name: " + device.getName() + ", address "
        +device.getAddress());

        Log.d(TAG, "onBondStateChanged: Previous bond state: "
                +Utils.btBondStateAsString(preBondState));
        Log.d(TAG, "onBondStateChanged: Current bond state: "
                +Utils.btBondStateAsString(bondState));
    }

    @Override
    public void onReadData(BluetoothDevice device, byte[] data) {
        Log.d(TAG, "onReadData: Data received from " + device.getName()
                + " of length " + data.length + "\n\n");

        // forward the data to the parser, which copies the data and posts a runnable to parse
        // the data
        if (data.length > 0){
            myBTDataParser.dataParser(data);
        }
    }
}
