package com.cheney.blelibrary.bluetooth;

import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.cheney.blelibrary.utils.Logger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by wangshuwen on 2017/3/25.
 */

public class BTStateManager {

    /**
     * 当前蓝牙连接的设备
     */
    private String currentBtName;

    /**
     * 当前蓝牙连接的状态
     */
    private AtomicReference<String> currentState = new AtomicReference<>(GattStatus.IDLE);


    @StringDef({GattStatus.IDLE,
            GattStatus.CONNECT_ERROR,
            GattStatus.CONNECT_SUCCESS,
            GattStatus.CONNECTED_ING,
            GattStatus.CHANNEL_ERROR,
            GattStatus.CHANNEL_SUCCESS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GattStatus {
        /**
         * 空闲
         */
        String IDLE = "IDLE";
        /**
         * 连接中
         */
        String CONNECTED_ING = "CONNECTED_ING";

        /**
         * 连接成功
         */
        String CONNECT_SUCCESS = "CONNECT_SUCCESS";
        /**
         * 连接错误
         */
        String CONNECT_ERROR = "CONNECT_ERROR";

        /**
         * 服务发现错误 特征值发现错误 开启通知失败 握手失败
         */
        String CHANNEL_ERROR = "CHANNEL_ERROR";
        /**
         * 握手成功
         */
        String CHANNEL_SUCCESS = "CHANNEL_SUCCESS";

    }


    private static volatile BTStateManager instance;

    private BTStateManager() {
    }

    public static BTStateManager getInstance() {
        if (instance == null) {
            synchronized (BTStateManager.class) {
                if (instance == null) {
                    instance = new BTStateManager();
                }
            }
        }
        return instance;
    }

    public String getState() {
        return currentState.get();
    }

    public void setState(String state) {
        currentState.set(state);
        Logger.d("bt State: " + state);
    }

    public String getCurrentBtName() {
        Logger.d("getCurrentBtName: ", currentBtName);
        return currentBtName;
    }


    void setCurrentBtName(String device) {
        Logger.d("currentRokidDeviceId: ", device);
        if (!TextUtils.isEmpty(device) && device.equals(currentBtName)) {
            Logger.d("BT state manager setCurrentDevice is same state have no change");
            return;
        }
        this.currentBtName = device;
        setState(GattStatus.IDLE);
    }

    public void clear() {
        this.currentBtName = null;
        setState(GattStatus.IDLE);
    }
}
