package com.cheney.blelibrary.bluetooth.callBack;


import com.cheney.blelibrary.bean.BTDeviceBean;

/**
 * Created by wangshuwen on 2017/3/28.
 */

public interface IBTScanCallBack {

    void onNewDevicesFound(BTDeviceBean btDeviceBean);

}

