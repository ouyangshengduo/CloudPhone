package com.cloud.phone.webrtc;

import com.cloud.phone.model.DeviceType;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class WebRtcConfig {
    //stun服务器配置
    public static final String STUN_URI = "stun:1.14.19.123:3478";
    public static final String STUN_USER_NAME = "";
    public static final String STUN_PASSWORD = "";
    //turn服务器配置
    public static final String TURN_URI = "turn:1.14.19.123:3478";
    public static final String TURN_USER_NAME = "admin";
    public static final String TURN_PASSWORD = "admin";
    //socket
    public static final String USER_ID = "0";                    //userId设置为"0",默认小于6位长度由服务器自动产生
    public static final int DEVICE = DeviceType.PHONE.getCode(); //设备类型为手机
    public static final String SOCKET_URI = "wss://1.14.19.123:8443/groupcall";
    //摄像头配置
    public static final int CAPTURE_WIDTH = 320;  //宽
    public static final int CAPTURE_HEIGHT = 240; //高
    public static final int CAPTURE_FPS = 10;     //帧率
    //是否开启Candidate延迟队列
    public static final boolean enableDelayQueue = false;
    //log-TAG
    public static final String LOG_TAG = "CloudPhone";
}
