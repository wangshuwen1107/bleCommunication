package com.cheney.blelibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cheney.blelibrary.RKBinderManager;
import com.cheney.blelibrary.bean.BTDeviceBean;
import com.cheney.blelibrary.bluetooth.callBack.IBTConnectCallBack;
import com.cheney.blelibrary.bluetooth.exception.BleException;
import com.cheney.blelibrary.utils.AppUtils;
import com.cheney.blelibrary.utils.Logger;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by wangshuwen on 2017/3/24.
 */

public class BTMirror {
    /**
     * 蓝牙通道
     */
    private BluetoothGatt bluetoothGatt;

    private BluetoothGattCharacteristic newCharacteristic;
    /**
     * 蓝牙连接的callback
     */
    private IBTConnectCallBack mBTConnectCallBack;
    /**
     * 数据包的消息队列
     */
    private Queue<byte[]> msgQueue = new LinkedList<>();
    /**
     * 消息队列是否正在发送中
     */
    private boolean isSending;


    private int mRetryCount;


    private UUID serviceUUID;

    private UUID characteristicUUID;

    private int maxRetryCount = 2;

    interface Operation {
        int MSG_DEQUEUE = 0x01;
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Operation.MSG_DEQUEUE:
                    dequeue();
                default:
            }
        }
    };

    public void connect(String name, IBTConnectCallBack callback) {
        Logger.i("ble connect is called");
        this.mBTConnectCallBack = callback;
        mRetryCount = 0;
        connect(name);

    }

    private void connect(String name) {
        if (TextUtils.isEmpty(name)) {
            Logger.e("ble connect  address is null");
            callBackConnectFailed(name, BleException.ErrorCode.NAME_EMPTY);
            return;
        }
        String address = null;
        Map<String, String> deviceAddressMap = RKBinderManager.btScanner().getDeviceAddressMap();
        if (deviceAddressMap != null && deviceAddressMap.containsKey(name)) {
            address = deviceAddressMap.get(name);
        }

        if (TextUtils.isEmpty(address)) {
            Logger.i("ble connect - no address for name: " + name);
            callBackConnectFailed(name, BleException.ErrorCode.ADDRESS_EMPTY);
            return;
        }

        Logger.i("ble connect - get device: " + address + " by name: " + name);

        if (!BTAdapterHolder.getInstance().getBTStatus()) {
            Logger.i("ble connect blueTooth is off !!");
            callBackConnectFailed(name, BleException.ErrorCode.PHONE_BT_DISABLE);
            return;
        }

        //判断是不是第一次连接
        String currentBtName = BTStateManager.getInstance().getCurrentBtName();
        if (TextUtils.isEmpty(currentBtName)) {
            connectBlueTooth(name, address);
            return;
        }

        if (currentBtName.equals(name)) {
            String state = BTStateManager.getInstance().getState();
            switch (state) {
                case BTStateManager.GattStatus.CONNECT_SUCCESS:
                case BTStateManager.GattStatus.CHANNEL_ERROR:
                case BTStateManager.GattStatus.CHANNEL_SUCCESS:
                    Logger.i("connect  device Name is same states is " + state + " callBack success");
                    callBackConnectSuccess(true);
                    break;
                case BTStateManager.GattStatus.CONNECTED_ING:
                    Logger.i("connect  device Name is same device is connecting s" +
                            "o  ignore this action");
                    break;
                default:
                    Logger.i("connect  device Name is same state="
                            + state + " close previous gatt && connect BLE");
                    closeClient();
                    connectBlueTooth(name, address);
            }
        }
        if (!currentBtName.equals(name)) {
            closeClient();
            connectBlueTooth(name, address);
        }
    }


    private void retryConnect() {
        mRetryCount++;
        if (mRetryCount >= maxRetryCount) {
            Logger.w("retryConnect maxRetryCount =>" + maxRetryCount + " do nothing");
            return;
        }
        Logger.w("retryConnect is called maxRetryCount=" + mRetryCount);
        connect(BTStateManager.getInstance().getCurrentBtName());
    }


    public void releaseBT() {
        Logger.d("BTMirror releaseBT is called ");
        isSending = false;
        mRetryCount = 0;
        closeClient();
        BTStateManager.getInstance().clear();
    }


    private void connectBlueTooth(String deviceName, String address) {
        Logger.i("connectBlueTooth is called ");
        BluetoothDevice remoteDevice;
        BluetoothAdapter adapter = BTAdapterHolder.getInstance().getBluetoothAdapter();
        //设置当前连接的蓝牙名称
        BTStateManager.getInstance().setCurrentBtName(deviceName);

        if (adapter == null) {
            Logger.i("connectBlueTooth adapter is empty bluetooth is off");
            callBackConnectFailed(BTStateManager.getInstance().getCurrentBtName(),
                    BleException.ErrorCode.PHONE_BT_DISABLE);
            return;
        }

        try {
            remoteDevice = adapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Logger.e("connectBlueTooth adapter no such address callBack error");
            e.printStackTrace();
            callBackConnectFailed(BTStateManager.getInstance().getCurrentBtName(),
                    BleException.ErrorCode.REMOTE_DEVICE_NOT_FOUND);
            return;
        }
        //23以上使用高信道连接
        BTStateManager.getInstance().setState(BTStateManager.GattStatus.CONNECTED_ING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteDevice.connectGatt(AppUtils.getApplicationContext(),
                    false,
                    new MyBluetoothGattCallback(),
                    BluetoothDevice.TRANSPORT_LE);
        } else {
            remoteDevice.connectGatt(AppUtils.getApplicationContext(),
                    false,
                    new MyBluetoothGattCallback());
        }
    }


    /**
     * 在停止的基础上释放监听
     */
    private void closeClient() {
        Logger.i("closeClient is called");
        if (bluetoothGatt != null) {
            Logger.i("do close bluetooth gatt client");
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        BTStateManager.getInstance().setState(BTStateManager.GattStatus.IDLE);
    }


    /**
     * 发送数据
     */
    public void sendData(@NonNull List<byte[]> packageList) {

        for (byte[] bytes : packageList) {
            msgQueue.offer(bytes);
        }
        if (!isSending) {
            mHandler.sendEmptyMessage(Operation.MSG_DEQUEUE);
        }
    }


    /**
     * 包消息出列 发送
     */
    private void dequeue() {
        if (isMsgQueueEmpty()) {
            Logger.d("dequeue msg Queue is empty all msg finish");
            isSending = false;
            return;
        }
        byte[] poll = msgQueue.poll();

        String btState = BTStateManager.getInstance().getState();
        if (!BTStateManager.GattStatus.CHANNEL_SUCCESS.equals(btState)) {
            Logger.w("BT state =" + btState + " so handleCharacteristicFailed ");
            onBleException(BleException.ErrorCode.STATUS_ERROR);
            isSending = false;
            return;
        }
        isSending = true;
        Logger.d("dequeue is called ");
        if (!newCharacteristic.setValue(poll)) {
            Logger.e("dequeue  set value failed is error");
            RKBinderManager.getInstance().handleCharacteristicFailed(poll[0],
                    new BleException(BleException.ErrorCode.SEND_DATA_ERROR));
            mHandler.sendEmptyMessage(Operation.MSG_DEQUEUE);
            return;
        }
        if (!bluetoothGatt.writeCharacteristic(newCharacteristic)) {
            Logger.e("dequeue  writeCharacteristic  failed is error");
            RKBinderManager.getInstance().handleCharacteristicFailed(poll[0],
                    new BleException(BleException.ErrorCode.SEND_DATA_ERROR));
            mHandler.sendEmptyMessage(Operation.MSG_DEQUEUE);
        }

    }

    /**
     * 判断消息队列是否为空
     */
    private boolean isMsgQueueEmpty() {
        return null == msgQueue || msgQueue.isEmpty();
    }


    //蓝牙连接的状态的监听和写入数据成功的监听
    class MyBluetoothGattCallback extends BluetoothGattCallback {
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            byte[] value = characteristic.getValue();
            Logger.i("onCharacteristicWrite: status = " + status + " length = " + value.length + " uuid=" + characteristic.getUuid());
            handleV2WriteOperation(value, status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Logger.d(" onCharacteristicChanged --------- ");
            byte[] value = null;
            if (null == value) {
                return;
            }
            BTPackageCenter.regroupNotifyPackage(value);
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Logger.i("onServicesDiscovered  status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(serviceUUID);
                if (null == service) {
                    Logger.w("onServicesDiscovered service is null");
                    BTStateManager.getInstance().setState(BTStateManager.GattStatus.CHANNEL_ERROR);
                    return;
                }
                newCharacteristic = service.getCharacteristic(characteristicUUID);
                boolean isNotifyEnable = bluetoothGatt.setCharacteristicNotification(newCharacteristic, true);
                Logger.d("onServicesDiscovered notify Enable=" + isNotifyEnable);
                if (!isNotifyEnable) {
                    BTStateManager.getInstance().setState(BTStateManager.GattStatus.CHANNEL_ERROR);
                    return;
                }
            } else {
                BTStateManager.getInstance().setState(BTStateManager.GattStatus.CHANNEL_ERROR);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Logger.i("-------- onConnectionStateChange: status: " + status +
                    ", newState: " + newState);
            bluetoothGatt = gatt;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Logger.i("onConnectionStateChange: STATE_CONNECTED");
                        callBackConnectSuccess(false);
                        //发现服务
                        bluetoothGatt.discoverServices();
                        break;
                    //主动断开
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Logger.i("onConnectionStateChange: STATE_DISCONNECTED");
                        onDisconnect();
                        break;
                    default:
                }
            } else {
                Logger.e("onConnectionStateChange callBack connect failed Name="
                        + gatt.getDevice().getName());
                onDisconnect();
            }
        }
    }

    /**
     * 自己主动断开 被动断开
     */
    private void onDisconnect() {
        BTStateManager.getInstance().setState(BTStateManager.GattStatus.CONNECT_ERROR);
        //释放蓝牙通道
        closeClient();
        //新协议的断开处理
        onBleException(BleException.ErrorCode.STATUS_ERROR);
        //老协议的断开 失败的回调
        callBackConnectFailed(BTStateManager.getInstance().getCurrentBtName(),
                BleException.ErrorCode.CONNECT_ERROR);
    }

    private void handleV2WriteOperation(byte[] value, int status) {
        Logger.i("handleV2WriteOperation  status=" + status);
        mHandler.sendEmptyMessage(Operation.MSG_DEQUEUE);
    }


    /**
     * 连接成功的callBack
     */
    private void callBackConnectSuccess(boolean ignoreSet) {
        mRetryCount = 0;
        if (!ignoreSet) {
            BTStateManager.getInstance().setState(BTStateManager.GattStatus.CONNECT_SUCCESS);
        }
        if (null == mBTConnectCallBack) {
            Logger.e("IBTConnectCallBack is null connectFailed");
            return;
        }
        mBTConnectCallBack.onConnectSucceed(RKBinderManager.getInstance().generateCurrentBtDevice());
        mBTConnectCallBack = null;
    }

    /**
     * 连接失败callBack
     *
     * @param errorCode 错误码
     */
    private void callBackConnectFailed(String name, final String errorCode) {
        if (null == mBTConnectCallBack) {
            Logger.e("IBTConnectCallBack is null connectFailed");
            return;
        }
        if (mRetryCount + 1 < maxRetryCount) {
            Logger.w("callBackConnectFailed mRetryCount=" + mRetryCount + " <" + maxRetryCount + " do not callback");
            retryConnect();
            return;
        }
        BTDeviceBean btDeviceBean = new BTDeviceBean();
        btDeviceBean.setName(name);
        btDeviceBean.setAddress(RKBinderManager.btScanner().getAddressByName(name));
        mBTConnectCallBack.onConnectFailed(btDeviceBean, new BleException(errorCode));
        mBTConnectCallBack = null;
    }

    /**
     * 发送数据过程中蓝牙断开 异常
     */
    private void onBleException(String errorCode) {
        msgQueue.clear();
        isSending = false;
        RKBinderManager.getInstance().onBleException(new BleException(errorCode));
    }


    static class Builder {

        private BTMirror btMirror;

        public Builder() {
            btMirror = new BTMirror();
        }

        public Builder serviceUUID(UUID serviceUUID) {
            this.btMirror.serviceUUID = serviceUUID;
            return this;
        }

        public Builder characteristicUUID(UUID characteristicUUID) {
            this.btMirror.characteristicUUID = characteristicUUID;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.btMirror.maxRetryCount = retryCount;
            return this;
        }

        public BTMirror build() {
            RKBinderManager.getInstance().setBTMirror(btMirror);
            return btMirror;
        }
    }
}
