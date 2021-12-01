package com.android.btgattdemo.client;

import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.btgattdemo.R;
import com.android.btgattdemo.service.BleClientManager;

import java.util.ArrayList;
import java.util.List;

public class CharacteristicActivity extends ListActivity {
    private static final String TAG = CharacteristicActivity.class.getSimpleName();
    private List<BluetoothGattCharacteristic> mCharacteristicList;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    private void initData() {
        final BluetoothGattService service = BleClientManager.getInstance().mCurrentBluetoothGattService;
        if (service == null) {
            return;
        }
        mCharacteristicList = service.getCharacteristics();
        Log.e(TAG, "Characteristic number =" + mCharacteristicList.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter = new MyAdapter(mCharacteristicList);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.clear();
    }


    private String getProperties(BluetoothGattCharacteristic characteristic) {
        String str = "";
        final int properties = characteristic.getProperties();
        if ((properties &  BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            str += "Read ";
        }
        if ((properties &  BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            str += "Notify ";
        }
        if ((properties &  BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            str += "Indicate ";
        }
        if ((properties &  BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            str += "Write ";
        }
        if ((properties &  BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            str += "WriteNoResponse ";
        }
        return str;
    }

    private class MyAdapter extends BaseAdapter {
        private List<BluetoothGattCharacteristic> mCharacteristicList;
        private CheckBox mLastReadChecked;
        private CheckBox mLastWriteChecked;

        public MyAdapter(List<BluetoothGattCharacteristic> characteristicList) {
            mCharacteristicList = new ArrayList<>();
            mCharacteristicList.addAll(characteristicList);
        }

        public void clear() {
            mCharacteristicList.clear();
        }

        @Override
        public int getCount() {
            return mCharacteristicList.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mCharacteristicList.get(position);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (view == null) {
                view = View.inflate(parent.getContext(), R.layout.characteristic_item, null);
                viewHolder = new ViewHolder();
                viewHolder.uuid = view.findViewById(R.id.uuid);
                viewHolder.properties = view.findViewById(R.id.properties);
                viewHolder.notify = view.findViewById(R.id.notify);
                viewHolder.indicate = view.findViewById(R.id.indicate);
                viewHolder.write = view.findViewById(R.id.write);
                viewHolder.writeNoResponse = view.findViewById(R.id.write_no_response);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final BluetoothGattCharacteristic characteristic = mCharacteristicList.get(position);
            viewHolder.uuid.setText(characteristic.getUuid().toString());
            viewHolder.properties.setText(getProperties(characteristic));
            viewHolder.notify.setEnabled((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
            viewHolder.indicate.setEnabled((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0);
            viewHolder.write.setEnabled((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
            viewHolder.writeNoResponse.setEnabled((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0);

            viewHolder.notify.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "viewHolder.notify checked: " + viewHolder.notify.isChecked());
                    final boolean isChecked = viewHolder.notify.isChecked();
                    if (isChecked) {
                        if (mLastReadChecked != null) {
                            mLastReadChecked.setChecked(false);
                        }
                        mLastReadChecked = viewHolder.notify;
                        BleClientManager.getInstance().mReadChar = characteristic;
                        BleClientManager.getInstance().mReadProperty = BluetoothGattCharacteristic.PROPERTY_NOTIFY;
                    } else {
                        if (mLastReadChecked != null && mLastReadChecked.equals(viewHolder.notify)) {
                            viewHolder.notify.setChecked(true);
                        }
                    }
                }
            });

            viewHolder.indicate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "viewHolder.indicate checked: " + viewHolder.indicate.isChecked());
                    final boolean isChecked = viewHolder.indicate.isChecked();
                    if (isChecked) {
                        if (mLastReadChecked != null) {
                            mLastReadChecked.setChecked(false);
                        }
                        mLastReadChecked = viewHolder.indicate;
                        BleClientManager.getInstance().mReadChar = characteristic;
                        BleClientManager.getInstance().mReadProperty = BluetoothGattCharacteristic.PROPERTY_INDICATE;
                    } else {
                        if (mLastReadChecked != null && mLastReadChecked.equals(viewHolder.indicate)) {
                            viewHolder.indicate.setChecked(true);
                        }
                    }
                }
            });

            viewHolder.write.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "viewHolder.write checked: " + viewHolder.write.isChecked());
                    final boolean isChecked = viewHolder.write.isChecked();
                    if (isChecked) {
                        if (mLastWriteChecked != null) {
                            mLastWriteChecked.setChecked(false);
                        }
                        mLastWriteChecked = viewHolder.write;
                        BleClientManager.getInstance().mWriteChar = characteristic;
                        BleClientManager.getInstance().mWriteProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
                    } else {
                        if (mLastWriteChecked != null && mLastWriteChecked.equals(viewHolder.write)) {
                            viewHolder.write.setChecked(true);
                        }
                    }
                }
            });

            viewHolder.writeNoResponse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "viewHolder.writeNoResponse checked: " + viewHolder.writeNoResponse.isChecked());
                    final boolean isChecked = viewHolder.writeNoResponse.isChecked();
                    if (isChecked) {
                        if (mLastWriteChecked != null) {
                            mLastWriteChecked.setChecked(false);
                        }
                        mLastWriteChecked = viewHolder.writeNoResponse;
                        BleClientManager.getInstance().mWriteChar = characteristic;
                        BleClientManager.getInstance().mWriteProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                    } else {
                        if (mLastWriteChecked != null && mLastWriteChecked.equals(viewHolder.writeNoResponse)) {
                            viewHolder.writeNoResponse.setChecked(true);
                        }
                    }
                }
            });
            return view;
        }
    }

    static class ViewHolder {
        TextView uuid;
        TextView properties;
        CheckBox notify;
        CheckBox indicate;
        CheckBox write;
        CheckBox writeNoResponse;
    }
}

