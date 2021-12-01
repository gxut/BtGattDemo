package com.android.btgattdemo.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btgattdemo.R;
import com.android.btgattdemo.service.BleServerManager;

public class BleServerActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothStatus;
    private TextView mReadProperty;
    private TextView mWriteProperty;

    private CheckBox mNotify;
    private CheckBox mIndicate;
    private CheckBox mWrite;
    private CheckBox mWriteNoResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_server);
        initView();
        initData();
    }

    private void initView() {
        mBluetoothStatus = findViewById(R.id.bluetooth_status);
        mReadProperty = findViewById(R.id.read_property);
        mWriteProperty = findViewById(R.id.write_property);
        mNotify = findViewById(R.id.notify);
        mIndicate = findViewById(R.id.indicate);
        mWrite = findViewById(R.id.write);
        mWriteNoResponse = findViewById(R.id.write_no_response);
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mBluetoothStatus.setText(R.string.bluetooth_disabled);
            mBluetoothStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (null !=  mBluetoothAdapter && !mBluetoothAdapter.isEnabled()) {
            mBluetoothStatus.setText(R.string.bluetooth_disabled);
            mBluetoothStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.notify:
                onClickNotify();
                break;
            case R.id.indicate:
                onClickIndicate();
                break;
            case R.id.write:
                onClickWrite();
                break;
            case R.id.write_no_response:
                onClickWriteNoResponse();
                break;
            case R.id.start_test:
                doTest();
                break;
        }
    }

    private void onClickNotify() {
        final boolean checked = mNotify.isChecked();
        if (checked) {
            mIndicate.setChecked(false);
        }
        showReadPropertyStatus();
    }

    private void onClickIndicate() {
        final boolean checked = mIndicate.isChecked();
        if (checked) {
            mNotify.setChecked(false);
        }
        showReadPropertyStatus();
    }

    private void onClickWrite() {
        final boolean checked = mWrite.isChecked();
        if (checked) {
            mWriteNoResponse.setChecked(false);
        }
        showWritePropertyStatus();
    }

    private void onClickWriteNoResponse() {
        final boolean checked = mWriteNoResponse.isChecked();
        if (checked) {
            mWrite.setChecked(false);
        }
        showWritePropertyStatus();
    }

    private void showReadPropertyStatus() {
        if (mNotify.isChecked()) {
            mReadProperty.setText(R.string.read_property_notify);
            mReadProperty.setTextColor(ContextCompat.getColor(this, R.color.blue));
        } else if (mIndicate.isChecked()) {
            mReadProperty.setText(R.string.read_property_indicate);
            mReadProperty.setTextColor(ContextCompat.getColor(this, R.color.blue));
        } else {
            mReadProperty.setText(R.string.read_property_nil);
            mReadProperty.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private void showWritePropertyStatus() {
        if (mWrite.isChecked()) {
            mWriteProperty.setText(R.string.write_property_write);
            mWriteProperty.setTextColor(ContextCompat.getColor(this, R.color.blue));
        } else if (mWriteNoResponse.isChecked()) {
            mWriteProperty.setText(R.string.write_property_write_no_response);
            mWriteProperty.setTextColor(ContextCompat.getColor(this, R.color.blue));
        } else {
            mWriteProperty.setText(R.string.write_property_nil);
            mWriteProperty.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private void doTest() {
        if (mNotify.isChecked()) {
            BleServerManager.getInstance().mReadProperty = BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        } else if (mIndicate.isChecked()) {
            BleServerManager.getInstance().mReadProperty = BluetoothGattCharacteristic.PROPERTY_INDICATE;
        } else {
            BleServerManager.getInstance().mReadProperty = 0;
        }
        if (mWrite.isChecked()) {
            BleServerManager.getInstance().mWriteProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
        } else if (mWriteNoResponse.isChecked()) {
            BleServerManager.getInstance().mWriteProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        } else {
            BleServerManager.getInstance().mWriteProperty = 0;
        }
        final Intent intent = new Intent(this, BleServerTestActivity.class);
        startActivity(intent);
    }
}