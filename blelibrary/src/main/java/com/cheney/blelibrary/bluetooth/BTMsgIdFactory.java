package com.cheney.blelibrary.bluetooth;

import com.cheney.blelibrary.utils.Logger;

import java.util.LinkedList;

/**
 * Created by wangshuwen on 2018/6/9.
 */

public class BTMsgIdFactory {

    private LinkedList<Integer> msgIdList = new LinkedList<>();


    public BTMsgIdFactory() {
        init();
    }

    private void init() {
        msgIdList.clear();
        for (int i = -127; i < 128; i++) {
            msgIdList.add(i);
        }
    }

    public int generateMsgId() {
        return msgIdList.poll();
    }


    public void recyclingMsgId(int msgId) {
        Logger.d();
        msgIdList.add(msgId);
    }

    public void release() {
        init();
    }

}
