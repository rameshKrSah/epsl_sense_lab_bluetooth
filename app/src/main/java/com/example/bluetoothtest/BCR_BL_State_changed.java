//package com.example.bluetoothtest;
//
//import android.bluetooth.BluetoothAdapter;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//public class BCR_BL_State_changed extends BroadcastReceiver {
//    private static final String TAG = "BCR_BL_State_changed";
//    private Context myContext;
//
//    public BCR_BL_State_changed(Context activityContext) {
//        this.myContext = activityContext;
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        final String action = intent.getAction();
//
//        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
//            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//            switch (state) {
//                case BluetoothAdapter.STATE_OFF:
//                    Log.d(TAG, "onReceive: Bluetooth OFF");
//                    break;
//                case BluetoothAdapter.STATE_ON:
//                    Log.d(TAG, "onReceive: Bluetooth ON");
//                    break;
//                case BluetoothAdapter.STATE_TURNING_OFF:
//                    Log.d(TAG, "onReceive: Bluetooth turning OFF");
//                    break;
//                case BluetoothAdapter.STATE_TURNING_ON:
//                    Log.d(TAG, "onReceive: Bluetooth turning ON");
//                    break;
//                case BluetoothAdapter.ERROR:
//                    Log.d(TAG, "onReceive: Bluetooth ERROR");
//                    break;
//            }
//        }
//    }
//}


//    /*
//    Broadcast receiver for the ACTION_FOUND intent of Bluetooth device scan function.
//    When a device is found by the Bluetooth scanner, this function is called with found device
//    details.
//     */
//    private final BroadcastReceiver bluetoothDeviceFoundReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Discovery has found a device. Get the Bluetooth device details from the Intent.
//                // To create a connection we just need the MAC address of the discovered device.
//                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                final BluetoothClass dvClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
//
//                // device details
//                String deviceName = device.getName();
//                String deviceMAC = device.getAddress();
//                ParcelUuid[] uuid = device.getUuids();
//                setStatusText(deviceName + "\t " + deviceMAC + "\t " + device.getBondState());
//
//                // here we can post a runnable to save the details of each found device.
//                myHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                         addDiscoveredBTDevice(device);
//                    }
//                });
//            }
//        }
//    };
//
//
//    /*
//    Broadcast receiver for Bluetooth state change intent.
//     */
//    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//
//            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
//                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//                switch (state) {
//                    case BluetoothAdapter.STATE_OFF:
//                        Log.d(TAG, "bluetoothStateReceiver: STATE OFF");
//                        break;
//                    case BluetoothAdapter.STATE_ON:
//                        Log.d(TAG, "bluetoothStateReceiver: STATE ON");
//                        break;
//                    case BluetoothAdapter.STATE_TURNING_OFF:
//                        Log.d(TAG, "bluetoothStateReceiver: STATE TURNING OFF");
//                        break;
//                    case BluetoothAdapter.STATE_TURNING_ON:
//                        Log.d(TAG, "bluetoothStateReceiver: STATE TURNING ON");
//                        break;
//                    case BluetoothAdapter.ERROR:
//                        Log.d(TAG, "bluetoothStateReceiver: ERROR");
//                        break;
//                }
//            }
//        }
//    };
//
//
//    // BroadcastReceiver for ACTION_SCAN_MODE_CHANGED intent of the make device discoverable function.
//    private final BroadcastReceiver bluetoothDiscoverableReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//
//            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
//                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
//                        BluetoothAdapter.ERROR);
//
//                switch(scanMode) {
//                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
//                        Log.d(TAG, "bluetoothDiscoverableReceiver: SCAN MODE CONNECTABLE DISCOVERABLE");
//                        break;
//                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
//                        Log.d(TAG, "bluetoothDiscoverableReceiver: SCAN MODE CONNECTABLE");
//                        break;
//                    case BluetoothAdapter.SCAN_MODE_NONE:
//                        Log.d(TAG, "bluetoothDiscoverableReceiver: SCAN MODE NONE");
//                        break;
//                    case BluetoothAdapter.STATE_CONNECTED:
//                        Log.d(TAG, "bluetoothDiscoverableReceiver: STATE CONNECTED");
//                        break;
//                    case BluetoothAdapter.STATE_CONNECTING:
//                        Log.d(TAG, "bluetoothDiscoverableReceiver: STATE CONNECTING");
//                        break;
//                }
//                /*
//                These two extras can have these values:
//                    SCAN_MODE_CONNECTABLE_DISCOVERABLE : The device is in discoverable mode.
//                    SCAN_MODE_CONNECTABLE : The device isn't discoverable but can still receive connections.
//                    SCAN_MODE_NONE : The device isn't in discoverable mode and cannot receive connections.
//                */
//            }
//        }
//    };
//
//    private final BroadcastReceiver blueoothPairingReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//
//            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
//                    Log.d(TAG, "blueoothPairingReceiver: BOND BONDED");
//                }
//
//                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
//                    Log.d(TAG, "blueoothPairingReceiver: BOND BONDING");
//                }
//
//                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
//                    Log.d(TAG, "blueoothPairingReceiver: BOND NONE");
//                }
//            }
//        }
//    };
//
//
//    // Register the broadcast receivers for the Bluetooth
//    public void registerBroadcastReceivers() {
//        // register the Bluetooth ACTION_STATE_CHANGED Broadcast Receiver : Monitors the Bluetooth
//        // module state
//        myMainActivity.registerReceiver(bluetoothStateReceiver, new IntentFilter(
//                BluetoothAdapter.ACTION_STATE_CHANGED));
//
//        // register the BroadcastReceiver for the ACTION_FOUND intent of the device scan function.
//        // Calls the registered function for every Bluetooth device found while scanning.
//        myMainActivity.registerReceiver(bluetoothDeviceFoundReceiver, new IntentFilter(
//                BluetoothDevice.ACTION_FOUND));
//
//        // register the BroadcastReceiver for the ACTION_SCAN_MODE_CHANGED intent of the device
//        // discoverable function.
//        myMainActivity.registerReceiver(bluetoothDiscoverableReceiver,
//                new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
//
//        // register a broadcast receiver for ACTION_BOND_STATE_CHANGED intent used for pairing devices
//        myMainActivity.registerReceiver(blueoothPairingReceiver, new IntentFilter(
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED));
//    }
//
//    // Unregister all broadcast receivers for the Bluetooth
//    public void unregisterBroadcastReceivers() {
//        myMainActivity.unregisterReceiver(bluetoothStateReceiver);
//        myMainActivity.unregisterReceiver(bluetoothDiscoverableReceiver);
//        myMainActivity.unregisterReceiver(bluetoothDeviceFoundReceiver);
//        myMainActivity.unregisterReceiver(blueoothPairingReceiver);
//    }
//



/*
Enable Bluetooth using the intent. This ask the user to enable the Bluetooth by showing
a pop box.
*/
//private void enableBluetoothWithIntent() {
//if(isBTAvailable() && isBTEnabled()){
//Utils.toast(myContext, "Bluetooth already on.");
//}
//else
//{
//        /*
//             REQUEST_ENABLE_BT is returned back by the system in onActivityResult as the request
//             code parameter.
//
//             If enabling Bluetooth succeeds, the activity will receives the RESULT_OK result
//             code in the onActivityResult() callback. If the Bluetooth was not enabled due to
//             an error (or the user responded "No") then the result code is RESULT_CANCELED.
//
//             Also, enabling discoverability automatically enables the Bluetooth.
//         */
//
//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//myMainActivity.startActivityForResult(enableBtIntent, this.REQUEST_ENABLE_BT);
//}
//}

