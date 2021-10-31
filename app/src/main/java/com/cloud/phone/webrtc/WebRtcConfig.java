package com.cloud.phone.webrtc;

import com.cloud.phone.model.DeviceType;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 *
 * 这里的ip地址已经改成了测试地址，
 * 如果需要正常运行功能， 需要使用自己的nat地址，和websocket地址
 */
public class WebRtcConfig {
    //stun服务器配置
    public static final String STUN_URI = "stun:192.168.1.168:3478";
    public static final String STUN_USER_NAME = "";
    public static final String STUN_PASSWORD = "";
    //turn服务器配置
    public static final String TURN_URI = "turn:192.168.1.168:3478";
    public static final String TURN_USER_NAME = "admin";
    public static final String TURN_PASSWORD = "admin";
    //socket
    public static final String SOCKET_URI = "wss://192.168.1.168:18443/";

    //是否开启Candidate延迟队列
    public static final boolean enableDelayQueue = false;
}
