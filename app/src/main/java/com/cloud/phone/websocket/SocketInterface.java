package com.cloud.phone.websocket;

import com.cloud.phone.model.SignalingMessage;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public interface SocketInterface {
    void connect(String uri);//发起加入房间请求,成功后服务器返回应答onMessage被调用，带有房间里的人的id和自己的id
    void sendOffer(String userName, SessionDescription localDescription); //向房间的其他成员发送自己的SDP信息
    void sendAnswer(String userName, SessionDescription localDescription);//发送应答
    void sendIceCandidate(String userName, IceCandidate iceCandidate);    //向房间的其他成员发送自己的iceCandidate信息
    void close();                                                         //关闭socket
    void joinRoom(SignalingMessage message);                            //请求加入房间
}
