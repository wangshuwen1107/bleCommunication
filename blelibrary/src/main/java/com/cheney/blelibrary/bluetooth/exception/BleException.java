package com.cheney.blelibrary.bluetooth.exception;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangshuwen on 2017/11/22.
 */

public class BleException extends Exception {

    private static Map<String, String> errorMap = new HashMap<>();

    private String errorCode;

    private String errorMessage;

    static {
        errorMap.put(ErrorCode.PHONE_BT_DISABLE, "蓝牙是关闭状态");

        errorMap.put(ErrorCode.BIND_DATA_ILLEGAL, "wifi名和密码为空");

        errorMap.put(ErrorCode.SEND_DATA_ERROR, "发送数据错误");

        errorMap.put(ErrorCode.ADDRESS_EMPTY, "设备地址为空");

        errorMap.put(ErrorCode.NAME_EMPTY, "设备名为空");

        errorMap.put(ErrorCode.REMOTE_DEVICE_NOT_FOUND, "蓝牙设备获取不到");

        errorMap.put(ErrorCode.CONNECT_ERROR, "蓝牙连接失败");

        errorMap.put(ErrorCode.REQUEST_TIME_OUT, "调用超时");

        errorMap.put(ErrorCode.VERSION_ERROR, "蓝牙版本错误");

        errorMap.put(ErrorCode.STATUS_ERROR, "蓝牙状态错误");
    }

    public interface ErrorCode {
        //绑定数据非法
        String BIND_DATA_ILLEGAL = "bind_data_illegal";
        //蓝牙关闭
        String PHONE_BT_DISABLE = "phone_ble_disable";
        //发送数据失败
        String SEND_DATA_ERROR = "send_data_error";
        //状态错误
        String STATUS_ERROR="ble_status_error";
        //调用超时
        String REQUEST_TIME_OUT = "request time out";
        //蓝牙版本错误
        String VERSION_ERROR = "ble_version_error";


        //address为空
        String ADDRESS_EMPTY = "ble_address_empty";
        //蓝牙名字为空
        String NAME_EMPTY = "ble_name_empty";
        //未发现蓝牙设备
        String REMOTE_DEVICE_NOT_FOUND = "ble_device_not_found";
        //连接失败
        String CONNECT_ERROR = "ble_connect_error";
    }

    private static String getMessage(String errorCode) {
        if (TextUtils.isEmpty(errorCode)) {
            return null;
        }
        return errorMap.get(errorCode);
    }

    public BleException(String errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = getMessage(errorCode);
    }

    public BleException(String errorMessage, String errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }


    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "BleException{" +
                "errorCode='" + errorCode + '\'' +
                "errorMessage='" + errorMessage + '\'' +
                '}';
    }

}
