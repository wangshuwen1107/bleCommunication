package com.cheney.blelibrary.bluetooth.callBack;

import com.cheney.blelibrary.bean.BTDeviceBean;
import com.cheney.blelibrary.bluetooth.exception.BleException;

/**
 * Created by wangshuwen on 2017/3/28.
 */

public interface IBTConnectCallBack {

    void onConnectSucceed(BTDeviceBean btDeviceBean);

    void onConnectFailed(BTDeviceBean btDeviceBean, BleException bleException);
}
