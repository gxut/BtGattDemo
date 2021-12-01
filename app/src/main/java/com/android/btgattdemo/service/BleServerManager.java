package com.android.btgattdemo.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.android.btgattdemo.ConstVar;

import java.util.Arrays;
import java.util.UUID;

public class BleServerManager {
    private static final String TAG = BleServerManager.class.getSimpleName();

    public final static String ACTION_GATT_CONNECTED = "com.gatt.bt.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.gatt.bt.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_CONNECTING = "com.gatt.bt.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.gatt.bt.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.gatt.bt.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.gatt.bt.le.EXTRA_DATA";
    public final static String ACTION_WRITE_CHARACTERISTIC = "com.gatt.ble.write";

    private static int MTU = 20;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public int mConnectionState = STATE_DISCONNECTED;

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;

    public int mReadProperty;
    public int mWriteProperty;
    public BluetoothGattCharacteristic mReadCharacteristic;
    public BluetoothGattCharacteristic mWriteCharacteristic;

    public BluetoothDevice mBluetoothDevice;
    public boolean mEndSending = true;
    public boolean mEcho;

    private BleServerManager() {
    }

    private static BleServerManager mBleServerManager;

    public static BleServerManager getInstance() {
        if (mBleServerManager == null) {
            mBleServerManager = new BleServerManager();
        }
        return mBleServerManager;
    }

    public boolean initialize(Context context, BluetoothManager bluetoothManager) {
        mContext = context;
        mBluetoothManager = bluetoothManager;

        return true;
    }

    public boolean openGattServer() {
        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        if (mGattServer == null) {
            return false;
        }

        return mGattServer.addService(getBluetoothGattService());
    }

    public void cancelConnection() {
        if (mGattServer != null && mBluetoothDevice != null) {
            mGattServer.cancelConnection(mBluetoothDevice);
            mGattServer.clearServices();
        }

        mGattServer = null;
        mBluetoothDevice = null;
    }

    private BluetoothGattService getBluetoothGattService() {
        if (mReadProperty == BluetoothGattCharacteristic.PROPERTY_NOTIFY || mReadProperty == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
            final UUID uuid = UUID.fromString(ConstVar.READ_UUID);
            mReadCharacteristic = new BluetoothGattCharacteristic(uuid, mReadProperty, BluetoothGattCharacteristic.PERMISSION_READ);
        } else {
            mReadCharacteristic = null;
        }

        if (mWriteProperty == BluetoothGattCharacteristic.PROPERTY_WRITE || mWriteProperty == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
            final UUID uuid = UUID.fromString(ConstVar.WRITE_UUID);
            mWriteCharacteristic = new BluetoothGattCharacteristic(uuid, mWriteProperty, BluetoothGattCharacteristic.PERMISSION_WRITE);
        } else {
            mWriteCharacteristic = null;
        }

        final UUID uuid = UUID.fromString(ConstVar.SERVICE_UUID);
        final BluetoothGattService mBatteryService = new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        if (mReadCharacteristic != null) {
            mBatteryService.addCharacteristic(mReadCharacteristic);
        }
        if (mWriteCharacteristic != null) {
            mBatteryService.addCharacteristic(mWriteCharacteristic);
        }

        return mBatteryService;
    }

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.e(TAG, "Connected to device : " + device.getAddress());
                    mConnectionState = STATE_CONNECTED;
                    mBluetoothDevice = device;

                    broadcastUpdate(ACTION_GATT_CONNECTED);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.e(TAG, "Disconnected from device");
                    mConnectionState = STATE_DISCONNECTED;

                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                }
            } else {
                Log.e(TAG, "Error when connecting: " + status);
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.e(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, "onCharacteristic Write request: " + Arrays.toString(value));

            if (value == null) {
                return;
            }

            broadcastUpdate(ACTION_DATA_AVAILABLE, value);

            //sendData(value);
            Log.e(TAG, "responseNeeded=" + responseNeeded);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, 0,
                        /* No need to respond with an offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite,
                                             boolean responseNeeded, int offset, byte[] value) {
            Log.e(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        /* No need to respond with offset */ 0,
                        /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.e(TAG, "onDescriptor read request");

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    /* No need to respond with offset */ 0,
                    /* No need to respond with a value */ null);

        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            MTU = mtu - 3;
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final byte[] value) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, value);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, int offset) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, offset);
        mContext.sendBroadcast(intent);
    }

    public void sendData(byte[] data) {
        mEndSending = false;
        if (mConnectionState != STATE_CONNECTED) {
            mEndSending = true;
            return;
        }

        mReadCharacteristic.setValue(data);
        if (mReadProperty == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            Log.e(TAG, "sending notify value");
            mGattServer.notifyCharacteristicChanged(mBluetoothDevice, mReadCharacteristic, false);
        } else if (mReadProperty == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
            Log.e(TAG, "sending indicate value");
            mGattServer.notifyCharacteristicChanged(mBluetoothDevice, mReadCharacteristic, true);
        } else {
            Log.e(TAG, "Not Set Characteristic");
        }
        broadcastUpdate(ACTION_WRITE_CHARACTERISTIC, data.length);
        mEndSending = true;
    }
}
