package com.example.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
    private BluetoothBaseListener mBaseListner;

    public BluetoothBroadcastReceiver(BluetoothBaseListener baseListener) {
        mBaseListner = baseListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || mBaseListner == null) {
            return;
        }

        String action = intent.getAction();
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                int preState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);
                mBaseListner.onActionStateChanged(preState, state);
                break;

            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                mBaseListner.onActionDiscoveryStateChanged(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                break;

            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                mBaseListner.onActionDiscoveryStateChanged(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                break;

            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
                mBaseListner.onActionDeviceFound(device, rssi);
                break;

            case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                int preScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, 0);
                mBaseListner.onActionScanModeChanged(scanMode, preScanMode);
                break;

            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                BluetoothDevice deviceBond = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                int preBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0);
                mBaseListner.onBondStateChanged(deviceBond, bondState, preBondState);
                break;

        }
    }
}
