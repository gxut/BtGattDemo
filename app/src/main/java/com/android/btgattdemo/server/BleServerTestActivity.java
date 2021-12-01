package com.android.btgattdemo.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btgattdemo.R;
import com.android.btgattdemo.service.AdvertiserService;
import com.android.btgattdemo.service.BleServerManager;

import java.util.Arrays;
import java.util.Locale;

public class BleServerTestActivity extends AppCompatActivity {
    private static final String DEFAULT_DATA = "012456";
    private AdvertiserService mAdvertiserService;
    private TextView mConnectState;
    private TextView mConnectedDevice;
    private EditText mDataToSend;
    private TextView mDataReceived;
    private ScrollView mTestLogScroll;
    private TextView mTestLog;
    private Button mAdvertiseBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_server_test);
        initView();
        initData();
    }

    private void initView() {
        mConnectState = findViewById(R.id.connect_state);
        mConnectedDevice = findViewById(R.id.connected_device);
        mDataToSend = findViewById(R.id.data_to_send);
        mDataReceived = findViewById(R.id.data_received);
        mTestLogScroll = findViewById(R.id.test_log_scroll);
        mTestLog = findViewById(R.id.test_log);
        mAdvertiseBtn = findViewById(R.id.advertising);
        mAdvertiseBtn.setOnClickListener(this::onClick);
    }

    private void initData() {
        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            mAdvertiserService = new AdvertiserService(this);
        } catch (NoSuchMethodError e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.bt_ads_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        BleServerManager.getInstance().initialize(this, mBluetoothManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BleServerManager.getInstance().openGattServer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mAdvertisingReceiver);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(AdvertiserService.ADVERTISING_FAILED);
        filter.addAction(AdvertiserService.ADVERTISING_SUCCEEDED);
        registerReceiver(mAdvertisingReceiver, filter);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleServerManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleServerManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleServerManager.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleServerManager.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleServerManager.ACTION_WRITE_CHARACTERISTIC);
        registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAdvertising();
        BleServerManager.getInstance().cancelConnection();
    }

    /**
     * Starts BLE Advertising by starting {@code AdvertiserService}.
     */
    private void startAdvertising() {
        mAdvertiserService.onCreate();
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        if (mAdvertiserService != null) {
            mAdvertiserService.onDestroy();
        }
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
            case R.id.advertising:
                if (AdvertiserService.mIsAdvertising) {
                    stopAdvertising();
                    mAdvertiseBtn.setText("START ADVERTISING");
                } else {
                    startAdvertising();
                    mAdvertiseBtn.setText("STOP ADVERTISING");
                }
                break;
            default:
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

    private void receiveData(byte[] buf) {
        final String str = new String(buf, 0, buf.length);
        mDataReceived.setText(str);
    }

    private void sendData() {
        if (BleServerManager.getInstance().mConnectionState != BleServerManager.STATE_CONNECTED) {
            addTestLog("Not connected");
            return;
        }
        if (BleServerManager.getInstance().mReadCharacteristic == null) {
            addTestLog("Read characteristic not set");
            return;
        }
        if (!BleServerManager.getInstance().mEndSending) {
            addTestLog("Cannot send as a sending thread is running");
            return;
        }

        if (mDataToSend.getText().toString().isEmpty()) {
            mDataToSend.setText(DEFAULT_DATA);
        }
        byte[] data = mDataToSend.getText().toString().getBytes();
        MyThread thread = new MyThread(data);
        thread.start();
    }

    private class MyThread extends Thread {
        private byte[] mData;

        public MyThread(byte[] data) {
            mData = Arrays.copyOfRange(data, 0, data.length);
        }

        @Override
        public void run() {
            super.run();
            BleServerManager.getInstance().sendData(mData);
        }
    }

    private BroadcastReceiver mAdvertisingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case AdvertiserService.ADVERTISING_FAILED:
                    int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);
                    String errorMessage = getString(R.string.start_error_prefix);
                    switch (errorCode) {
                        case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                            errorMessage += " " + getString(R.string.start_error_already_started);
                            break;
                        case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE://广播数字包长度最大31字节
                            errorMessage += " " + getString(R.string.start_error_too_large);
                            break;
                        case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                            errorMessage += " " + getString(R.string.start_error_unsupported);
                            break;
                        case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                            errorMessage += " " + getString(R.string.start_error_internal);
                            break;
                        case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                            errorMessage += " " + getString(R.string.start_error_too_many);
                            break;
                        default:
                            errorMessage += " " + getString(R.string.start_error_unknown);
                    }

                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    mAdvertiseBtn.setText("START ADVERTISING");
                    break;
                case AdvertiserService.ADVERTISING_SUCCEEDED:
                    mAdvertiseBtn.setText("START ADVERTISING");
                    break;
            }
        }
    };

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BleServerManager.ACTION_GATT_CONNECTED:
                    showConnectionState();
                    stopAdvertising();
                    break;
                case BleServerManager.ACTION_GATT_DISCONNECTED:
                case BleServerManager.ACTION_GATT_CONNECTING:
                    showConnectionState();
                    break;
                case BleServerManager.ACTION_DATA_AVAILABLE:
                    byte[] buf = intent.getByteArrayExtra(BleServerManager.EXTRA_DATA);
                    receiveData(buf);
                    break;
                default:
                    break;
            }
        }
    };

    private void showConnectionState() {
        final int connectionState = BleServerManager.getInstance().mConnectionState;
        switch (connectionState) {
            case BleServerManager.STATE_CONNECTED:
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                mConnectState.setText(R.string.connected);
                mConnectedDevice.setText(BleServerManager.getInstance().mBluetoothDevice.getAddress());
                break;
            case BleServerManager.STATE_DISCONNECTED:
                mConnectState.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                mConnectState.setText(R.string.disconnected);
                mConnectedDevice.setText("");
                break;
            case BleServerManager.STATE_CONNECTING:
                mConnectState.setText(R.string.connecting);
                mConnectedDevice.setText("");
                break;
        }
    }
}
