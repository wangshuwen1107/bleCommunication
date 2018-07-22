package com.cheney.blelibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.cheney.blelibrary.bean.BTDeviceBean;
import com.cheney.blelibrary.bluetooth.callBack.IBTScanCallBack;
import com.cheney.blelibrary.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by wangshuwen on 2018/6/11.
 */

public class BTScanner implements BluetoothAdapter.LeScanCallback {

    /**
     * 设备address列表防止重复添加
     */
    private Map<String, String> deviceAddressMap = new HashMap<>();

    /**
     * 是否在扫描
     */
    private boolean isScan;

    /**
     * 蓝牙扫描的callback
     */
    private IBTScanCallBack mBTScanCallBack;


    public BTScanner() {
    }

    public void startBTScan(UUID[] uuidArray, IBTScanCallBack scanCallBack) {
        Logger.i("startScan is called ");
        deviceAddressMap.clear();
        this.mBTScanCallBack = scanCallBack;
        if (isScan) {
            Logger.w("startScan is running do no thing");
            return;
        }
        if (!BTAdapterHolder.getInstance().getBTStatus()) {
            Logger.i("startScan blue tooth is off !!");
            return;
        }
        final BluetoothAdapter adapter = BTAdapterHolder.getInstance().getBluetoothAdapter();

        isScan = adapter.startLeScan(uuidArray,BTScanner.this);
    }


    //停止扫描
    public void stopBTScan() {
        Logger.i("stopBTScan is called");
        isScan = false;
        mBTScanCallBack = null;
        BluetoothAdapter bluetoothAdapter = BTAdapterHolder.getInstance().getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(this);
        }
    }


    //停止扫描和清楚列表
    public void stopBTScanAndClearList() {
        Logger.i("stopBTScanAndClearList is called");
        isScan = false;
        deviceAddressMap.clear();
        mBTScanCallBack = null;
        BluetoothAdapter bluetoothAdapter = BTAdapterHolder.getInstance().getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(this);
        }
    }


    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (null == device || TextUtils.isEmpty(device.getName()) || TextUtils.isEmpty(device.getAddress())) {
            return;
        }
        //不包含才说明 发现了新的设备
        if (!deviceAddressMap.containsKey(device.getName())) {
            if (mBTScanCallBack != null) {
                BTDeviceBean deviceBean = new BTDeviceBean();
                deviceBean.setAddress(device.getAddress());
                deviceBean.setName(device.getName());

                Logger.d(" 🍌 BLE new Device Found name=" + device.getName()
                        + " ;address=" + device.getAddress());
                mBTScanCallBack.onNewDevicesFound(deviceBean);
            }
        }
        deviceAddressMap.put(device.getName(), device.getAddress());
    }


    /**
     * 通过设备名字获取设备地址
     *
     * @param deviceName 蓝牙名字
     * @return 蓝牙地址
     */
    public String getAddressByName(String deviceName) {
        if (TextUtils.isEmpty(deviceName)) {
            return "";
        }
        return deviceAddressMap.get(deviceName);
    }


    Map<String, String> getDeviceAddressMap() {
        return deviceAddressMap;
    }


    public void release() {
        Logger.d("BTScanner release ");
        stopBTScanAndClearList();
    }
}
