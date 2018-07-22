package com.cheney.blelibrary;

import android.text.TextUtils;
import android.util.SparseArray;

import com.cheney.blelibrary.bean.BTDeviceBean;
import com.cheney.blelibrary.bluetooth.BTAdapterHolder;
import com.cheney.blelibrary.bluetooth.BTMirror;
import com.cheney.blelibrary.bluetooth.BTMsgIdFactory;
import com.cheney.blelibrary.bluetooth.BTPackageCenter;
import com.cheney.blelibrary.bluetooth.BTScanner;
import com.cheney.blelibrary.bluetooth.BTStateManager;
import com.cheney.blelibrary.bluetooth.callBack.IBTSendCallBack;
import com.cheney.blelibrary.bluetooth.callBack.IBTStateChangeListener;
import com.cheney.blelibrary.bluetooth.exception.BleException;
import com.cheney.blelibrary.utils.CollectionUtils;
import com.cheney.blelibrary.utils.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangshuwen on 2017/3/24.
 */

public class RKBinderManager {

    private static volatile RKBinderManager instance;
    /**
     * 消息id生成工厂
     */
    private BTMsgIdFactory msgIdFactory;
    /**
     * 消息callback组
     */
    private SparseArray<IBTSendCallBack> mBtSendCallbackMap;
    /**
     * 超时线程池
     */
    private ScheduledExecutorService mTimeoutExecutorService;
    /**
     * 超时任务
     */
    private Map<Integer, FutureTask> mTimeOutTaskMap = new HashMap<>();
    /**
     * 请求超时时间
     */
    private static final int REQUEST_TIMEOUT = 8000;

    private BTMirror mBTMirror;

    private BTScanner mBTScanner;

    private class TimeOutCall implements Callable<String> {

        private TimeOutCall(int msgId) {
            this.msgId = msgId;
        }

        private int msgId;

        @Override
        public String call() throws Exception {
            if (mTimeOutTaskMap.containsKey(msgId)) {
                Logger.d("removeTimeoutTask is called msgId: " + msgId);
                mTimeOutTaskMap.remove(msgId);
            }
            Logger.e("time out task is running  msgId=" + msgId);
            IBTSendCallBack ibtSendCallBack = mBtSendCallbackMap.get(msgId);
            if (null != ibtSendCallBack) {
                releaseMsgId(msgId);
                ibtSendCallBack.onSendFailed(generateCurrentBtDevice(),
                        new BleException(BleException.ErrorCode.REQUEST_TIME_OUT));
            }
            return null;
        }
    }

    private RKBinderManager() {
        mBTScanner = new BTScanner();
        msgIdFactory = new BTMsgIdFactory();
        mBtSendCallbackMap = new SparseArray<>();
        mTimeoutExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public static RKBinderManager getInstance() {
        if (instance == null) {
            synchronized (RKBinderManager.class) {
                if (instance == null) {
                    instance = new RKBinderManager();
                }
            }
        }
        return instance;
    }


    public void setBTMirror(BTMirror mBTMirror) {
        this.mBTMirror = mBTMirror;
    }

    /**
     * 开启超时任务
     *
     * @param msgId
     */
    private void startTimeoutTask(int msgId) {
        Logger.i("startTimeoutTask is called msgId: " + msgId);
        TimeOutCall timeoutCall = new TimeOutCall(msgId);
        FutureTask timeoutTask = new FutureTask<>(timeoutCall);
        mTimeOutTaskMap.put(msgId, timeoutTask);
        mTimeoutExecutorService.schedule(timeoutTask, REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * 移除超时任务
     *
     * @param msgId
     */
    private void removeTimeoutTask(int msgId) {
        Logger.i("removeTimeoutTask is called msgId: " + msgId);
        FutureTask task = mTimeOutTaskMap.remove(msgId);
        if (null != task) {
            Logger.i("removeTimeoutTask cancel is real call is called msgId=" + msgId);
            task.cancel(true);
        }
    }

    /**
     * 注册系统蓝牙开关状态的改变的监听
     *
     * @param listener
     */
    public void registerBTStateChangeListener(IBTStateChangeListener listener) {
        Logger.i("registerBTStateChangeListener is called");
        BTAdapterHolder.getInstance().registerReceiver(listener);
    }

    /**
     * 注销系统蓝牙开关状态改变的监听
     */
    private void unRegisterBTStateChangeListener() {
        Logger.i("unRegisterBTStateChangeListener is called");
        BTAdapterHolder.getInstance().unRegisterReceiver();
    }

    /**
     * 获取扫描器
     */
    public static BTScanner btScanner() {
        return getInstance().mBTScanner;
    }


    /**
     * 执行ble 请求
     *
     * @param requestStr 请求json
     * @param callBack   回调
     */
    public void executeRequest(String requestStr,
                               final IBTSendCallBack callBack) {
        if (null == mBTMirror) {
            callBack.onSendFailed(generateCurrentBtDevice(),
                    new BleException("-1", "btMirror is empty please buildMirror first"));
            return;
        }
        //生成ID
        int msgId = msgIdFactory.generateMsgId();
        //分包
        List<byte[]> byteList = BTPackageCenter.subPackage(msgId, requestStr);
        if (CollectionUtils.isEmpty(byteList)) {
            callBack.onSendFailed(generateCurrentBtDevice(),
                    new BleException("-1", "sub pkg byteList empty"));
            releaseMsgId(msgId);
            return;
        }
        Logger.d("-------requestStr=" + requestStr + " msgId=" + msgId);
        //加入 callbackMap
        mBtSendCallbackMap.put(msgId, callBack);
        startTimeoutTask(msgId);
        mBTMirror.sendData(byteList);
    }


    /**
     * 获取当前ble设备
     *
     * @return 蓝牙的名字 和 地址
     */
    public BTDeviceBean generateCurrentBtDevice() {
        BTDeviceBean btDeviceBean = new BTDeviceBean();
        String currentBtName = BTStateManager.getInstance().getCurrentBtName();
        btDeviceBean.setName(currentBtName);
        btDeviceBean.setAddress(mBTScanner.getAddressByName(currentBtName));
        return btDeviceBean;
    }


    /**
     * 写操作失败
     */
    public void handleCharacteristicFailed(int msgId, BleException bleException) {
        Logger.e("handleCharacteristicFailed msgId=" + msgId
                + "bleErrorCode= " + bleException);
        IBTSendCallBack ibtSendCallBack = mBtSendCallbackMap.get(msgId);
        if (null != ibtSendCallBack) {
            releaseMsgId(msgId);
            removeTimeoutTask(msgId);
            ibtSendCallBack.onSendFailed(generateCurrentBtDevice(), bleException);
        }
    }


    /**
     * 回收msgId相关
     *
     * @param msgId
     */
    private void releaseMsgId(int msgId) {
        Logger.d("releaseMsgId is called msgId=" + msgId);
        msgIdFactory.recyclingMsgId(msgId);
        mBtSendCallbackMap.remove(msgId);
        BTPackageCenter.removePkgList(msgId);
    }


    /**
     * 收到msgId的所有包
     *
     * @param msgId    请求的唯一id
     * @param response 响应的数据
     */
    public void handleCompleteResponse(int msgId, String response, boolean isWrite) {
        Logger.d("handleCompleteResponse msgId=" + msgId + " response=" + response);
        if (TextUtils.isEmpty(response)) {
            return;
        }
        IBTSendCallBack ibtSendCallBack = mBtSendCallbackMap.get(msgId);
        if (null != ibtSendCallBack) {
            releaseMsgId(msgId);
            removeTimeoutTask(msgId);
            ibtSendCallBack.onResponse(generateCurrentBtDevice(), response);
        }
    }


    /**
     * 发送的过程中 ble断开 ble服务发现错 特征值发现错误 都会触发
     *
     * @param bleException 蓝牙异常
     */
    public void onBleException(BleException bleException) {
        Logger.e("onBleException so callback pending request all failed and clear map 😭😭😭");
        //回调callBack 失败
        for (int i = 0; i < mBtSendCallbackMap.size(); i++) {
            int msgId = mBtSendCallbackMap.keyAt(i);
            IBTSendCallBack ibtSendCallBack = mBtSendCallbackMap.get(msgId);
            Logger.e("msgId = " + msgId + " handleCharacteristicFailed");
            ibtSendCallBack.onSendFailed(generateCurrentBtDevice(), bleException);
            removeTimeoutTask(msgId);
        }
        //清空回调map
        mBtSendCallbackMap.clear();
        msgIdFactory.release();
        BTPackageCenter.release();
    }


    public void releaseBT() {
        Logger.d("releaseBT is called --------");
        mBTMirror.releaseBT();
        mBTScanner.release();
        for (Map.Entry<Integer, FutureTask> taskEntry : mTimeOutTaskMap.entrySet()) {
            taskEntry.getValue().cancel(true);
        }
        mTimeOutTaskMap.clear();
        msgIdFactory.release();
        BTPackageCenter.release();
        unRegisterBTStateChangeListener();
    }

}
