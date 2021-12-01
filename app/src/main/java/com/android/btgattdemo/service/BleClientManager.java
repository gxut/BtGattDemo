package com.android.btgattdemo.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BleClientManager {
    private final static String TAG = BleClientManager.class.getSimpleName();
    private static int MTU = 20;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public int mConnectionState = STATE_DISCONNECTED;
    public final static String ACTION_GATT_CONNECTED = "com.gatt.bt.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.gatt.bt.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_CONNECTING = "com.gatt.bt.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.gatt.bt.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.gatt.bt.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.gatt.bt.le.EXTRA_DATA";
    public final static String ACTION_WRITE_CHARACTERISTIC = "com.gatt.ble.write";
    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    public List<BluetoothGattService> mBluetoothGattServices;
    public BluetoothGattService mCurrentBluetoothGattService;
    public BluetoothGattCharacteristic mReadChar;
    public BluetoothGattCharacteristic mWriteChar;
    public int mReadProperty;
    public int mWriteProperty;
    public boolean mEndSending = true;
    public byte[] mData;

    private BleClientManager() {
    }

    private static BleClientManager mBleClientManager;

    public static BleClientManager getInstance() {
        if (mBleClientManager == null) {
            mBleClientManager = new BleClientManager();
        }
        return mBleClientManager;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(Context context) {
        mContext = context;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;

        mBluetoothGattServices = null;
        mReadChar = null;
        mWriteChar = null;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        String intentAction;
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            intentAction = ACTION_GATT_DISCONNECTED;
            broadcastUpdate(intentAction);
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.e(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                Log.e(TAG, "The connection attempt was initiated successfully");
                mConnectionState = STATE_CONNECTING;
                intentAction = ACTION_GATT_CONNECTING;
                broadcastUpdate(intentAction);
                return true;
            } else {
                Log.e(TAG, "The connection attempt was initiated failed");
                mConnectionState = STATE_DISCONNECTED;

                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                return false;
            }
        } else {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.e(TAG, "Device not found.  Unable to connect.");
                mConnectionState = STATE_DISCONNECTED;

                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                return false;
            }
            mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
            Log.e(TAG, "Trying to create a new connection.");
            mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
            intentAction = ACTION_GATT_CONNECTING;
            broadcastUpdate(intentAction);
            return true;
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange");
            String intentAction;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(TAG, "Connected to GATT server. Attempting to start service discovery");
                    intentAction = ACTION_GATT_CONNECTED;
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(intentAction);
                    final boolean flag = mBluetoothGatt.discoverServices();
                    Log.e(TAG, "mBluetoothGatt.discoverServices() return " + flag);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "Disconnected from GATT server.");
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    mBluetoothGattServices = null;
                    broadcastUpdate(intentAction);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    Log.e(TAG, "Connecting to GATT server.");
                    intentAction = ACTION_GATT_CONNECTING;
                    mConnectionState = STATE_CONNECTING;
                    broadcastUpdate(intentAction);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onServicesDiscovered");
                if (mBluetoothGatt != null) {
                    setBluetoothGattServices(mBluetoothGatt.getServices());
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            writeCharacteristic();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            MTU = mtu - 3;
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private synchronized void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, data);
            if (mWriteChar != null) {
                sendData(data);
            }
        }
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final int param1) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, param1);
        mContext.sendBroadcast(intent);
    }


    /**
     * Enables or disables notification/indication on a give characteristic.
     * <p>
     * //     * @param characteristic Characteristic to act on.
     *
     * @param enabled If true, enable notification/indication.  False otherwise.
     */
    public boolean setCharacteristicNotification(boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if (mReadChar == null) {
            Log.e(TAG, "Bluetooth characteristic not set");
            return false;
        }
        mBluetoothGatt.setCharacteristicNotification(mReadChar, enabled);

        final UUID uuid = mReadChar.getUuid();
        if (uuid != null) {
            List<BluetoothGattDescriptor> Descriptors = mReadChar.getDescriptors();
            for (BluetoothGattDescriptor des : Descriptors) {
                if (mReadProperty == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                    des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else if (mReadProperty == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                    des.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                } else {
                    Log.e(TAG, "Property error!");
                }
                mBluetoothGatt.writeDescriptor(des);
            }
        }
        return true;
    }

    public void sendData(byte[] data) {
        mData = Arrays.copyOfRange(data, 0, data.length);
        writeCharacteristic();
    }

    private void writeCharacteristic() {
        if (mConnectionState != STATE_CONNECTED) {
            mEndSending = true;
            return;
        }

        mWriteChar.setValue(mData);
        boolean writeFlag = mBluetoothGatt.writeCharacteristic(mWriteChar);
        if (writeFlag) {
            broadcastUpdate(ACTION_WRITE_CHARACTERISTIC, mData.length);
        }
        mEndSending = true;
    }

    private void setBluetoothGattServices(List<BluetoothGattService> services) {
        mBluetoothGattServices = new ArrayList<>();
        for (BluetoothGattService service : services) {
            if (!mBluetoothGattServices.contains(service)) {
                mBluetoothGattServices.add(service);
            }
        }
    }
}
