package com.android.btgattdemo.client;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btgattdemo.R;
import com.android.btgattdemo.service.BleClientManager;

import java.util.Locale;

public class BleClientTestActivity extends AppCompatActivity {
    private static final String DEFAULT_INTERVAL = "200";
    private static final String DEFAULT_DATA = "ABCDEFG";

    private TextView mConnectState;
    private TextView mReadCharacteristic;
    private TextView mWriteCharacteristic;
    private EditText mDataToSend;
    private TextView mDataReceived;
    private ScrollView mTestLogScroll;
    private TextView mTestLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_client_test);
        initView();
        initData();
    }

    private void initView() {
        mConnectState = findViewById(R.id.connect_state);
        mReadCharacteristic = findViewById(R.id.read_characteristic);
        mWriteCharacteristic = findViewById(R.id.write_characteristic);
        mDataToSend = findViewById(R.id.data_to_send);
        mDataReceived = findViewById(R.id.data_received);
        mTestLogScroll = findViewById(R.id.test_log_scroll);
        mTestLog = findViewById(R.id.test_log);
    }

    private void initData() {
        showConnectionState();
        final BluetoothGattCharacteristic readChar = BleClientManager.getInstance().mReadChar;
        if (readChar == null) {
            mReadCharacteristic.setText(R.string.no_read_characteristic);
            mReadCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            final int readProperty = BleClientManager.getInstance().mReadProperty;
            switch (readProperty) {
                case BluetoothGattCharacteristic.PROPERTY_READ:
                    mReadCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.blue));
                    mReadCharacteristic.setText(R.string.read);
                    break;
                case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                    mReadCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.blue));
                    mReadCharacteristic.setText(R.string.notify);
                    break;
                case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                    mReadCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.blue));
                    mReadCharacteristic.setText(R.string.indicate);
                    break;
            }
        }
        final BluetoothGattCharacteristic writeChar = BleClientManager.getInstance().mWriteChar;
        if (writeChar == null) {
            mWriteCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.red));
            mWriteCharacteristic.setText(R.string.no_write_characteristic);
        } else {
            final int writeProperty = BleClientManager.getInstance().mWriteProperty;
            switch (writeProperty) {
                case BluetoothGattCharacteristic.PROPERTY_WRITE:
                    mWriteCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.blue));
                    mWriteCharacteristic.setText(R.string.write);
                    BleClientManager.getInstance().mWriteChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    break;
                case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                    mWriteCharacteristic.setTextColor(ContextCompat.getColor(this, R.color.blue));
                    mWriteCharacteristic.setText(R.string.write_no_response);
                    BleClientManager.getInstance().mWriteChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    break;
            }
        }

        BleClientManager.getInstance().setCharacteristicNotification(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleClientManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleClientManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleClientManager.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleClientManager.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleClientManager.ACTION_WRITE_CHARACTERISTIC);
        registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.send_clear:
                sendClear();
                break;
            case R.id.send_default:
                sendDefault();
                break;
            case R.id.send:
                sendData();
                break;
            case R.id.recv_clear:
                receiveClear();
                break;
            case R.id.test_log_clear:
                testLogClear();
                break;
        }
    }

    private void sendClear() {
        mDataToSend.setText("");
    }

    private void sendDefault() {
        mDataToSend.setText(DEFAULT_DATA);
    }

    private void receiveClear() {
        mDataReceived.setText("");
    }

    private void testLogClear() {
        mTestLog.setText("");
    }

    private void addTestLog(String str) {
        mTestLog.append(str + "\n");
        mTestLogScroll.post(new Runnable() {
            @Override
            public void run() {
                mTestLogScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void sendData() {
        if (BleClientManager.getInstance().mConnectionState != BleClientManager.STATE_CONNECTED) {
            addTestLog("Not connected");//当连接状态不为connected时，则显示未连接，并returun,不进行发数据
            return;
        }
        if (BleClientManager.getInstance().mWriteChar == null) {
            addTestLog("Write characteristic not set");  //写属性空时，显示未设置
            return;
        }
        if (mDataToSend.getText().toString().isEmpty()) {
            mDataToSend.setText(DEFAULT_DATA);
        }
        byte[] data = mDataToSend.getText().toString().getBytes();
        BleClientManager.getInstance().mEndSending = false;
        BleClientManager.getInstance().sendData(data);
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BleClientManager.ACTION_GATT_CONNECTED:
                case BleClientManager.ACTION_GATT_DISCONNECTED:
                case BleClientManager.ACTION_GATT_CONNECTING:
                    showConnectionState();
                    break;
                case BleClientManager.ACTION_GATT_SERVICES_DISCOVERED:
                    break;
                case BleClientManager.ACTION_DATA_AVAILABLE:
                    byte[] buf = intent.getByteArrayExtra(BleClientManager.EXTRA_DATA);
                    receiveData(buf);
                    break;
                case BleClientManager.ACTION_WRITE_CHARACTERISTIC:
                    int offset = intent.getIntExtra(BleClientManager.EXTRA_DATA, 0);
                    break;
                default:
                    break;
            }
        }
    };

    private void receiveData(byte[] buf) {
        final String str = new String(buf, 0, buf.length);
        mDataReceived.setText(str);
    }

    private void showConnectionState() {
        final int connectionState = BleClientManager.getInstance().mConnectionState;
        switch (connectionState) {
            case BleClientManager.STATE_CONNECTED:
                mConnectState.setText(R.string.connected);
                mConnectState.setTextColor(ContextCompat.getColor(this, R.color.blue));
                break;
            case BleClientManager.STATE_DISCONNECTED:
                mConnectState.setText(R.string.disconnected);
                mConnectState.setTextColor(ContextCompat.getColor(this, R.color.red));
                break;
            case BleClientManager.STATE_CONNECTING:
                mConnectState.setText(R.string.connecting);
                mConnectState.setTextColor(ContextCompat.getColor(this, R.color.red));
                break;

        }
    }

}
