package com.cloud.phone.webrtc;


import com.cloud.phone.model.RoomType;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public interface WebRtcInterface {
    void switchMute(boolean mute);           //静音切换
    void switchCamera();                     //前后摄像头切换
    void switchHandsfree(boolean handsfree); //免提切换
    void powerCamera(boolean enable);        //摄像头开关
    void hangUp();
    void chatRequest(RoomType roomType);//发起聊天请求，建立socket连接
    void joinRoom();                                          //请求加入房间
}
