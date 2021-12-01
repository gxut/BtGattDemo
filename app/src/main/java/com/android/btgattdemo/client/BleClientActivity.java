package com.android.btgattdemo.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.btgattdemo.ConstVar;
import com.android.btgattdemo.R;
import com.android.btgattdemo.service.BleClientManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleClientActivity extends AppCompatActivity {
    private String mDeviceAddress;  // bluetooth device mac address

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_client);
        initView();
        initData();
    }

    private void initView() {
        final Intent intent = getIntent();
        final String mDeviceName = intent.getStringExtra(ConstVar.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(ConstVar.EXTRAS_DEVICE_ADDRESS);

        final TextView deviceName = findViewById(R.id.device_name);
        final TextView deviceAddress = findViewById(R.id.device_address);
        if (deviceName != null) {
            deviceName.setText(mDeviceName);
        }
        if (deviceAddress != null) {
            deviceAddress.setText(mDeviceAddress);
        }
    }

    private void initData() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectionState();
        showServiceList();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleClientManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleClientManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleClientManager.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleClientManager.ACTION_DATA_AVAILABLE);
        registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleClientManager.getInstance().close();
    }

    private void connect() {
        BleClientManager manager = BleClientManager.getInstance();
        if (!manager.initialize(this)) {
            finish();
            return;
        }
        if (mDeviceAddress != null) {
            manager.connect(mDeviceAddress);
        }
    }


    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.start_test:
                final Intent intent = new Intent(v.getContext(), BleClientTestActivity.class);
                startActivity(intent);
                break;
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BleClientManager.ACTION_GATT_CONNECTED:
                case BleClientManager.ACTION_GATT_CONNECTING:
                    showConnectionState();
                    break;
                case BleClientManager.ACTION_GATT_DISCONNECTED:
                    showConnectionState();
                    showServiceList();
                    break;
                case BleClientManager.ACTION_GATT_SERVICES_DISCOVERED:
                    showServiceList();
                    break;
                case BleClientManager.ACTION_DATA_AVAILABLE:
                    break;
                default:
                    break;
            }
        }
    };

    private void showConnectionState() {
        final TextView tv = (TextView) findViewById(R.id.connect_state);
        final int connectionState = BleClientManager.getInstance().mConnectionState;
        switch (connectionState) {
            case BleClientManager.STATE_CONNECTED:
                if (tv != null) {
                    tv.setText(R.string.connected);
                    tv.setTextColor(ContextCompat.getColor(this, R.color.blue));
                }
                break;
            case BleClientManager.STATE_DISCONNECTED:
                if (tv != null) {
                    tv.setText(R.string.disconnected);
                    tv.setTextColor(ContextCompat.getColor(this, R.color.red));
                }
                break;
            case BleClientManager.STATE_CONNECTING:
                if (tv != null) {
                    tv.setText(R.string.connecting);
                    tv.setTextColor(ContextCompat.getColor(this, R.color.red));
                }
                break;
        }
    }

    private void showServiceList() {
        final ListView servicesView = findViewById(R.id.services_view);
        final ArrayList<Map<String, String>> listItems = new ArrayList<>();
        final List<BluetoothGattService> services = BleClientManager.getInstance().mBluetoothGattServices;
        if (services != null) {
            for (BluetoothGattService service : services) {
                String uuid = service.getUuid().toString();
                Map<String, String> map = new HashMap<>();
                map.put("uuid", uuid);
                listItems.add(map);
            }
        }

        final SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItems, R.layout.ble_service_list, new String[]{"uuid"}, new int[]{R.id.service_uuid});
        if (servicesView != null) {
            servicesView.setAdapter(simpleAdapter);

            servicesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final List<BluetoothGattService> services = BleClientManager.getInstance().mBluetoothGattServices;

                    if (services == null) {
                        return;
                    }
                    BleClientManager.getInstance().mCurrentBluetoothGattService = services.get(position);
                    final Intent intent = new Intent(view.getContext(), CharacteristicActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}