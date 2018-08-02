package cwm.androidbleperiphial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private TextView textview;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;

    private static final int REQUEST_ENABLE_BT = 1;
    private static UUID UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String TAG = "bernie";

    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattCharacteristic characteristicRead;

    private Thread thread;
    private boolean RUN_THREAD = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textview = (TextView) findViewById(R.id.textview1);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        textview.setText("");
        showText("starting ...");
        initGATTServer();

    }

    private void initGATTServer(){
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .build();
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID_SERVER))
                .setIncludeTxPowerLevel(false)
                .build();

        AdvertiseCallback callback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                showText("1. initGATTServer success");
                initServices(getApplicationContext());
            }

            @Override
            public void onStartFailure(int errorCode) {
                showText("1. initGATTServer failure");
            }
        };

        BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);
    }

    private void showText(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textview.append(msg + "\r\n");
            }
        });
    }

    public void initServices(Context context){
        bluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //add a write characteristic.
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

        //add a descriptor
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicWrite.addDescriptor(descriptor);

        service.addCharacteristic(characteristicWrite);

        bluetoothGattServer.addService(service);

        showText("2. initServices ok");


    }
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.e(TAG, String.format("1.onConnectionStateChange:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("1.onConnectionStateChange:status = %s, newState =%s ", status, newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, String.format("onServiceAdded：status = %s", status));

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, String.format("onCharacteristicReadRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onCharacteristicReadRequest:requestId = %s, offset = %s", requestId, offset));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest:requestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(requestBytes)));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);
            //4.处理响应内容
            onResponseToClient(requestBytes, device, requestId, characteristic);
        }
int i = 0;
        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.e(TAG, String.format("2.onDescriptorWriteRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("2.onDescriptorWriteRequest:requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(value)));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            if (value[0] == 1) { //enable notification
                RUN_THREAD = true;
                thread = new Thread() {
                    @Override
                    public void run() {
                        while (RUN_THREAD) {
                            try {
                                Thread.sleep(10);
                                notifyData(device, new byte[]{(byte) i}, false);
                                i++;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                thread.start();
            } else { //disable notification
                  RUN_THREAD = false;
                  thread.interrupt();
                  thread = null;
            }


        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.e(TAG, String.format("onDescriptorReadRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onDescriptorReadRequest:requestId = %s", requestId));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
           public void onNotificationSent(BluetoothDevice device, int status) {
            Log.e(TAG, String.format("onNotificationSent  name = %s, address = %s", device.getName(), device.getAddress()));
        }
    };

    private void onResponseToClient(byte[] reqeustBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, String.format("4.onResponseToClient:device name = %s, address = %s", device.getName(), device.getAddress()));
        Log.e(TAG, String.format("4.onResponseToClient:requestId = %s", requestId));
        String msg = OutputStringUtil.transferForPrint(reqeustBytes);
        showText("4.收到:" + msg);

        String str = new String(reqeustBytes) + " hello>";
        characteristicRead.setValue(str.getBytes());
        bluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);

        showText("4.回應:" + str);
    }

    private void notifyData(final BluetoothDevice device, byte[] value, final boolean confim) {

        BluetoothGattCharacteristic characteristic = null;
        for (BluetoothGattService service : bluetoothGattServer.getServices()) {
            for (BluetoothGattCharacteristic mcharacteristic : service.getCharacteristics()) {
                if(mcharacteristic.getDescriptor(UUID_DESCRIPTOR) != null)
                    characteristic = mcharacteristic;
                    break;
            }
        }
        if (characteristic != null) {
            characteristic.setValue(value);
            bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, confim);

        }
    }

}
