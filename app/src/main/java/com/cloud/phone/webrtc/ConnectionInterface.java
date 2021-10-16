package com.cloud.phone.webrtc;

import com.cloud.phone.model.SignalingMessage;
import com.cloud.phone.model.User;

import org.webrtc.IceCandidate;

import java.util.List;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public interface ConnectionInterface extends WebRtcInterface{
    void connectSuccess(User user);                                       //webSocket连接成功
    void remoteJoinToRoom(SignalingMessage user);                                     //有人加入房间
    void remoteOutRoom(SignalingMessage user);                                        //有人退出房间
    void onReceiveOffer(String userName, String sdp);                     //远端发起offer
    void onReceiveAnswer(String userName, String sdp);                    //远端响应了offer
    void onRemoteCandidate(String userName, IceCandidate iceCandidate);   //远端响应了Candidate

    void createConnection(List<SignalingMessage> members);                        //创建P2P连接
    int getConnectNum();                                                  //获取链接数
}
