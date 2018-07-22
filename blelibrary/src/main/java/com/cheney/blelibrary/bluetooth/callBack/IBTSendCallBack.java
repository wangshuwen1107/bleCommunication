package com.cheney.blelibrary.bluetooth.callBack;


import com.cheney.blelibrary.bean.BTDeviceBean;
import com.cheney.blelibrary.bluetooth.exception.BleException;

/**
 * Created by wangshuwen on 2017/3/28.
 */

public interface IBTSendCallBack {

    void onResponse(BTDeviceBean btDeviceBean, String data);

    void onSendFailed(BTDeviceBean btDeviceBean, BleException bleException);
}
