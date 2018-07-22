package com.cheney.blelibrary.bluetooth;

import android.text.TextUtils;
import android.util.SparseArray;

import com.cheney.blelibrary.RKBinderManager;
import com.cheney.blelibrary.utils.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by wangshuwen on 2018/6/8.
 */

public class BTPackageCenter {

    private static int TOTAL_PACKAGE_SIZE = 20;

    private static int DATA_PACKAGE_SIZE = 17;

    private static int HEAD_PACKAGE_SIZE = 3;

    private static SparseArray<List<byte[]>> notifySuccessPkgMap = new SparseArray<>();

    private static SparseArray<List<byte[]>> writeSuccessPkgMap = new SparseArray<>();


    public static void removePkgList(int msgId) {
        if (null != notifySuccessPkgMap) {
            notifySuccessPkgMap.remove(msgId);
        }
    }

    public static void release() {
        notifySuccessPkgMap.clear();
        writeSuccessPkgMap.clear();
    }

    /**
     * 分包
     *
     * @param msgId 每个包的id
     * @param data  真实的数据源
     * @return
     */
    public static List<byte[]> subPackage(int msgId, String data) {
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        List<byte[]> byteList = new ArrayList<>();
        //第一位
        byte[] bytes = data.getBytes();
        //如果一个包就能装下
        if (bytes.length <= DATA_PACKAGE_SIZE) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(data.length() + HEAD_PACKAGE_SIZE);
            byteBuffer.put(endPackage(msgId));
            byteBuffer.put(data.getBytes());
            byteList.add(byteBuffer.array());
            return byteList;
        }
        //分包
        int packageSize = data.getBytes().length / DATA_PACKAGE_SIZE;
        int moreThan = data.getBytes().length % DATA_PACKAGE_SIZE;
        if (moreThan != 0) {
            ++packageSize;
        }
        byte[] dataBytes = data.getBytes();
        for (int i = 0; i < packageSize; i++) {
            //标志位
            ByteBuffer byteBuffer;
            //真实数据包
            byte[] dataByte;
            //每个包的真实数据size
            int realSize;
            //最后一个包
            if (i == packageSize - 1) {
                if (moreThan == 0) {
                    byteBuffer = ByteBuffer.allocate(TOTAL_PACKAGE_SIZE);
                    byteBuffer.put(endPackage(msgId));
                    realSize = DATA_PACKAGE_SIZE;
                } else {
                    byteBuffer = ByteBuffer.allocate(HEAD_PACKAGE_SIZE + moreThan);
                    byteBuffer.put(endPackage(msgId));
                    realSize = moreThan;
                }
            } else {
                byteBuffer = ByteBuffer.allocate(TOTAL_PACKAGE_SIZE);
                byteBuffer.put(normalPackage(msgId));
                realSize = DATA_PACKAGE_SIZE;
            }
            dataByte = new byte[realSize];
            for (int k = DATA_PACKAGE_SIZE * i; k < i * DATA_PACKAGE_SIZE + realSize; k++) {
                int index = (k) % DATA_PACKAGE_SIZE;
                dataByte[index] = dataBytes[k];
            }
            //拷贝真实数据包
            byteBuffer.put(dataByte);
            byteList.add(byteBuffer.array());
        }
        Logger.d("total package size=" + byteList.size());
        for (byte[] packageByteArray : byteList) {
            Logger.d("package " + byteList.indexOf(packageByteArray) + " size=" + packageByteArray.length);
            for (byte b : packageByteArray) {
                Logger.d(byteToBit(b));
            }
            Logger.d("----------");
        }
        return byteList;
    }

    /**
     * 重组包
     *
     * @param bytes
     */
    private synchronized static void regroupPackage(SparseArray<List<byte[]>> map, byte[] bytes, boolean isWirte) {
        if (null == bytes || bytes.length == 0) {
            return;
        }
        //取包的id
        byte msgIdByte = bytes[0];
        int msgId = (int) (msgIdByte);
        //标志位
        byte markByte = bytes[1];
        //取包的内容
        byte[] dataByte = new byte[bytes.length - HEAD_PACKAGE_SIZE];
        for (int i = 0; i < dataByte.length; i++) {
            dataByte[i] = bytes[HEAD_PACKAGE_SIZE + i];
        }
        List<byte[]> packageList = map.get(msgId);
        if (packageList == null || packageList.size() == 0) {
            packageList = new ArrayList<>();
        }
        packageList.add(dataByte);
        Logger.d("regroupPackage msgId=" + msgId
                + " ;data=" + new String(dataByte)
                +"  ;index="+printIndex(markByte,bytes[2])
                + " isWrite=" + isWirte);
        map.put(msgId, packageList);
        if (isEndPackage(markByte)) {
            map.remove(msgId);

            int byteSize = 0;
            for (byte[] packageByteArray : packageList) {
                byteSize += packageByteArray.length;
            }
            ByteBuffer allocate = ByteBuffer.allocate(byteSize);
            for (byte[] packageByteArray : packageList) {
                allocate.put(packageByteArray);
            }
            String responseData = new String(allocate.array());
            Logger.d("msgId=" + msgId + "😊😊 the package is finish =" + responseData + " isWrite=" + isWirte);
            RKBinderManager.getInstance().handleCompleteResponse(msgId, responseData, isWirte);
        }
    }


    public static void regroupWritePackage(byte[] bytes) {
        regroupPackage(writeSuccessPkgMap, bytes, true);
    }

    public static void regroupNotifyPackage(byte[] bytes) {
        regroupPackage(notifySuccessPkgMap, bytes, false);
    }


    /**
     * 解析 发送数据
     */
    public static Queue<String> getOldBinderData(String data) {
        Queue<String> dataList = new LinkedList<>();
        // 数据分包个数
        int subpackageSize = 0;
        // 当前处理游标
        int currentIndex = 0;
        // 循环处理，直到游标结束
        while (data.length() > currentIndex) {
            StringBuilder subSB = new StringBuilder();
            for (int index = 0; index < 15; ) {
                String subStr = data.substring(currentIndex, currentIndex + 1);
                if (subSB.toString().getBytes().length + subStr.getBytes().length > 15) {
                    break;
                }

                subSB.append(subStr);
                currentIndex++;
                index += subStr.getBytes().length;

                if (currentIndex >= data.length()) {
                    break;
                }
            }

            // 累加分包
            subpackageSize++;
            dataList.add(subSB.toString());
        }

        // 列表顶部 加入分包个数
        ((LinkedList) dataList).add(0, "size:" + subpackageSize);

        for (String str : dataList) {
            Logger.i("init data string i=" + str);
        }

        return dataList;
    }


    static boolean isHandShakingPkg(byte[] packages) {
        if (null == packages) {
            return false;
        }
        if (packages.length == 8 && (char) (packages[0]) == 'R' && (char) (packages[1]) == 'K') {
            return true;
        }
        return false;
    }

    private synchronized static boolean isEndPackage(byte mark) {
        byte[] booleanArray = getBooleanArray(mark);
        return booleanArray[0] == 1;
    }


    private static byte[] getBooleanArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    /**
     * 生成尾包 标志byte
     *
     * @param msgId id
     * @return
     */
    private static byte[] endPackage(int msgId) {
        byte[] signByte = new byte[3];
        signByte[0] = ((byte) (msgId));
        signByte[1] = (((byte) (0b10000000)));
        signByte[2] = (((byte) (0b00000000)));
        return signByte;
    }


    /**
     * 生成正常包 标志byte
     *
     * @param msgId id
     * @return
     */
    private static byte[] normalPackage(int msgId) {
        byte[] signByte = new byte[3];
        signByte[0] = ((byte) (msgId));
        signByte[1] = (((byte) (0b00000000)));
        signByte[2] = (((byte) (0b00000000)));
        return signByte;
    }

    /**
     * 把byte转为字符串的bit
     */
    static String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);
    }


    static int printIndex(byte b1, byte b2) {
        byte[] signByte = new byte[2];
        //System.out.println("之前 =" + byteToBit((byte) a));
        //System.out.println("之后 =" + byteToBit((byte) b));
        signByte[0] = (byte) (b1 & 0b01111111);
        signByte[1] = b2;
        //System.out.println("index="+fromByteArray(signByte));
        return fromByteArray(signByte);
    }

    static int fromByteArray(byte[] bytes) {
        return (bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF);
    }


}
