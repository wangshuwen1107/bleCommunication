package com.cheney.blelibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.cheney.blelibrary.bluetooth.callBack.IBTStateChangeListener;
import com.cheney.blelibrary.utils.AppUtils;
import com.cheney.blelibrary.utils.Logger;


/**
 * the blue tooth adapter holder
 */
public class BTAdapterHolder {

    //系统管理蓝牙的服务
    private BluetoothManager bluetoothManager;

    //蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;

    //系统蓝牙是否开启
    private IBTStateChangeListener iBluetoothListener;

    //蓝牙开关状态的广播
    BTAdapterHolder.BluetoothReceiver bluetoothReceiver;

    private static volatile BTAdapterHolder instance;

    private BTAdapterHolder() {
        bluetoothManager = ((BluetoothManager) AppUtils.getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE));
        if (null != bluetoothManager) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }


    public static BTAdapterHolder getInstance() {
        if (instance == null) {
            synchronized (BTAdapterHolder.class) {
                if (instance == null) {
                    instance = new BTAdapterHolder();
                }
            }
        }
        return instance;
    }


    public void registerReceiver(IBTStateChangeListener listener) {
        Logger.i("registerReceiver is called");
        this.iBluetoothListener = listener;
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
         AppUtils.getApplicationContext().registerReceiver(bluetoothReceiver, bluetoothFilter);
    }

    public void unRegisterReceiver() {
        Logger.i("unRegisterReceiver is called");
        this.iBluetoothListener = null;
        if (bluetoothReceiver != null) {
             AppUtils.getApplicationContext().unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        } else {
            Logger.w("bluetoothReceiver has been unregister so do nothing");
        }
    }

    //获取蓝牙状态
    public boolean getBTStatus() {
        if (bluetoothAdapter == null) {
            Logger.w("getBTStatus is called blueTooth is off !");
            return false;
        } else {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (isEnabled) {
                Logger.i("getBTStatus is called blueTooth is on !");
                return true;
            } else {
                Logger.w("getBTStatus is called blueTooth is off !");
                return false;
            }
        }
    }


    private void refreshBluetoothState(int bluetoothState) {
        if (null == iBluetoothListener) {
            Logger.e("bluetoothListener is null reference");
            return;
        }
        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                iBluetoothListener.onBluetoothStateChanged(true);
                break;
            case BluetoothAdapter.STATE_OFF:
                iBluetoothListener.onBluetoothStateChanged(false);
                break;
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.i("onReceive BluetoothAdapter Status Change Broadcast");
            if (intent != null && intent.getAction() != null && BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (bluetoothState == BluetoothAdapter.STATE_ON) {
                    bluetoothManager = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE));
                    if (null != bluetoothManager) {
                        bluetoothAdapter = bluetoothManager.getAdapter();
                    }
                    refreshBluetoothState(BluetoothAdapter.STATE_ON);
                    Logger.i("onReceive: is called  blue tooth on !");
                } else if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                    bluetoothAdapter = null;
                    refreshBluetoothState(BluetoothAdapter.STATE_OFF);
                    Logger.i("onReceive: is called  blue tooth off !");
                }
            }
        }
    }

}
