# BLE GATT Server/Client

BLE GATT Server/Client传输示例

## 一、简介

* BLE是个通用的技术术语，与平台无关；BLE即Bluetooth LE（Low Energy简称LE）  
* 蓝牙协议包括两种技术：Basic Rate(简称BR)和Low Energy(简称LE)  
* Basic Rate是正宗的蓝牙技术，可以包括可选的EDR(Enhanced Data Rate)技术，以及交替使用的MAC(Media Access Control)层和PHY层扩展(简称AMP)  
* BR和EDR是可以同时存在的，但BR/EDR和AMP只能二选一  
* BR/EDR和AMP传输速率的关系为：BR < EDR < AMP（因为AMP借用了WIFI的MAC和物理层）  
* BR的功耗比较严重，而在某些场景是比较关注设备的功耗的，于是BLE就产生了，也即Bluetooth LE  
* Generic Attribute Profile (GATT)--通过BLE连接，读写属性类小数据的Profile通用规范。现在所有的BLE应用Profile都是基于GATT的  
* Attribute Protocol (ATT)--GATT是基于ATTProtocol的。ATT针对BLE设备做了专门的优化，具体就是在传输过程中使用尽量少的数据。每个属性都有一个唯一的UUID，属性将以characteristics and services的形式传输。  
* Characteristic可以理解为一个数据类型，它包括一个value和0至多个对次value的描述（Descriptor）  
* GATT的服务（service）是为了实现设备的某些功能或特征，是一系列数据和行为的集合  
* BluetoothGatt类：符合BLE GATT协议，封装了与其他蓝牙设备通信功能的一个类。可以通过BluetoothDevice的connectGatt(Context, boolean, BluetoothGattCallback)得到其实例  
* BluetoothGatt常用的几个方法说明  
  * connect() ：连接远程设备  
  * discoverServices() : 搜索连接设备所支持的service  
  * disconnect()：断开与远程设备的GATT连接  
  * close()：关闭GATTClient端  
  * readCharacteristic(characteristic) ：读取指定的characteristic  
  * setCharacteristicNotification(characteristic, enabled)：设置当指定characteristic值变化时，发出通知  
  * getServices() ：获取远程设备所支持的services  

## 二、Server连接流程

* 检测蓝牙状态

```java
    mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    //若蓝牙未打开，则打开蓝牙
    private void enableBt() {
        if (mBtAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    updateBtStatus(BluetoothAdapter.STATE_ON);
                    updateButtonStatus(true);
                } else {
                    updateBtStatus(BluetoothAdapter.STATE_OFF);
                    updateButtonStatus(false);
                }
                break;
            default:
                break;
        }
    }
```

* 设置BluetoothGattCharacteristic并创建BluetoothGattServer

  * 设置BluetoothGattCharacteristic

  ```java
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
        leServerManager.getInstance().mWriteProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    } else {
        BleServerManager.getInstance().mWriteProperty = 0;
    }
  ```

  * 创建BluetoothGattServer

  ```java
    public boolean openGattServer() {
        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);
        if (mGattServer == null) {
            return false;
        }

        return mGattServer.addService(getBluetoothGattService());
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
  ```

* 设置Server端蓝牙可见，让Client端能搜索到Server端

```java
   private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(mContext, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.ble_not_supported), Toast.LENGTH_LONG).show();
            }
        }

   }

   private void startAdvertising() {
        Log.e(TAG, "Service: Starting Advertising");
        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdvertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
            }
        }
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        settingsBuilder.setConnectable(true);
        return settingsBuilder.build();
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);
        return dataBuilder.build();
    }
```

* 等待Client连接

```java
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BleServerManager.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BleServerManager.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BleServerManager.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BleServerManager.ACTION_DATA_AVAILABLE);
    intentFilter.addAction(BleServerManager.ACTION_WRITE_CHARACTERISTIC);
    registerReceiver(mGattUpdateReceiver, intentFilter);

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
```

* Server端发送接收数据  
  * Server端接收数据

  ```java
  private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                ......
                case BleServerManager.ACTION_DATA_AVAILABLE:
                    byte[] buf = intent.getByteArrayExtra(BleServerManager.EXTRA_DATA);
                    receiveData(buf);
                    break;
                default:
                    break;
            }
        }
    };
  ```

  * Server端发送数据

  ```java
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
  ```

## 三、Client连接流程

* 检测蓝牙状态

```java
    mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    //若蓝牙未打开，则打开蓝牙
    private void enableBt() {
        if (mBtAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    updateBtStatus(BluetoothAdapter.STATE_ON);
                    updateButtonStatus(true);
                } else {
                    updateBtStatus(BluetoothAdapter.STATE_OFF);
                    updateButtonStatus(false);
                }
                break;
            default:
                break;
        }
    }
```

* 扫描周围的BLE蓝牙设备

```java
    private void initData() {
        mHandler = new Handler();
        mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = mBtManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }
```

* 与server端建立连接，建立GATT连接

```java
    //选择你需要连接的client进行连接
     protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mBLEDeviceListAdapter.getDevice(position);
        if (device == null) {
            return;
        }
        final Intent intent = new Intent(this, BleClientActivity.class);
        intent.putExtra(ConstVar.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ConstVar.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    //与server建立连接
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

    public boolean connect(final String address) {
        ......
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        ......
        //连接远程设备，建立GATT连接
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        ......
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "onConnectionStateChange");
            String intentAction;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    ......
                    //搜索连接设备所支持的service
                    final boolean flag = mBluetoothGatt.discoverServices();
                    Log.e(TAG, "mBluetoothGatt.discoverServices() return " + flag);
                    break;
               ......
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onServicesDiscovered");
                if (mBluetoothGatt != null) {
                    //获取远程设备所支持的services
                    setBluetoothGattServices(mBluetoothGatt.getServices());
                }
                ......

            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        ......
    };
```

* 等待与server端连接成功广播，并获取BluetoothGattService

```java
    //注册广播监听
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BleClientManager.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BleClientManager.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BleClientManager.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BleClientManager.ACTION_DATA_AVAILABLE);
    registerReceiver(mGattUpdateReceiver, intentFilter);

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                .......
                case BleClientManager.ACTION_GATT_DISCONNECTED:
                    showServiceList();
                    break;
                case BleClientManager.ACTION_GATT_SERVICES_DISCOVERED:
                    showServiceList();
                    break;
                ......
                default:
                    break;
            }
        }
    };

    private void showServiceList() {
        final ListView servicesView = findViewById(R.id.services_view);
        final ArrayList<Map<String, String>> listItems = new ArrayList<>();
        //远程设备所支持的services
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
```

* client接收和发送数据
  * client接收数据

  ```java
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BleClientManager.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BleClientManager.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BleClientManager.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BleClientManager.ACTION_DATA_AVAILABLE);
    intentFilter.addAction(BleClientManager.ACTION_WRITE_CHARACTERISTIC);
    registerReceiver(mGattUpdateReceiver, intentFilter);

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                ......
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
  ```

  * client发送数据
  
  ```java
    private void sendData() {
        if (BleClientManager.getInstance().mConnectionState != BleClientManager.STATE_CONNECTED) {//当连接状态不为connected时，则显示未连接，并returun,不进行发数据
            return;
        }
        if (BleClientManager.getInstance().mWriteChar == null) {//写属性空时，显示未设置
            return;
        }
        if (mDataToSend.getText().toString().isEmpty()) {
            mDataToSend.setText(DEFAULT_DATA);
        }
        byte[] data = mDataToSend.getText().toString().getBytes();
        BleClientManager.getInstance().mEndSending = false;
        BleClientManager.getInstance().sendData(data);
    }
  ```

