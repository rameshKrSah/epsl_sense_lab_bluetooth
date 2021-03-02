package com.example.bluetoothtest;

import android.bluetooth.BluetoothDevice;

public interface BluetoothBaseListener {

    /**
        Callback when the Bluetooth power state is changed
        @param: preState: previous power state
        @param: state: current power state

        Possible values are STATE_OFF, STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF
     */
    void onActionStateChanged(int preState, int state);


    /**
        Callback when local Bluetooth adapter discovery process state changed.
        @param: discoveryState: state of local Bluetooth adapter discovery process.
        Possible values are ACTION_DISCOVERY_STARTED, ACTION_DISCOVERY_FINISHED.
     */
    void onActionDiscoveryStateChanged(String discoveryState);

    /**
        Callback when the current scan mode changed.
        @param: preScanMode: previous scan mode
        @param: scanMode: current scan mode

        Possible values are SCAN_MODE_NONE, SCAN_MODE_CONNECTABLE, SCAN_MODE_CONNECTABLE_DISCOVERABLE
     */
    void onActionScanModeChanged(int preScanMode, int scanMode);

    /**
        Callback when the connection state changed.
        @param state: connection state
        Possible values are STATE_NONE, STATE_LISTEN, STATE_CONNECTING, STATE_CONNECTED,
        STATE_DISCONNECTED, STATE_UNKNOWN
     */
    void onBluetoothServiceStateChanged(int state);


    /**
        Callback when a device is found during the scan.
        @param device: found remote device
        @param rssi: RSSI value of the remote device
     */
    void onActionDeviceFound(BluetoothDevice device, short rssi);



    /**
        Callback when a device bond state is changed when pairing.
        @param device: device whose bond state is changing
        @param bondState: device current bond state
        @param preBondState: device previous bond state
     */
    void onBondStateChanged(BluetoothDevice device, int bondState, int preBondState);


    /**
        Callback when remote device send data to current device
        @param: device, the remote connected device
        @param: data, the bytes to read
     */
    void onReadData(BluetoothDevice device, byte [] data);

}
