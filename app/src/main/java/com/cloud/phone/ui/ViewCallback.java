package com.cloud.phone.ui;


import com.cloud.phone.model.Message;

import org.webrtc.MediaStream;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public interface ViewCallback {
    void socketCallback(Message message);                      //socket状态回调
    void setLocalStream(MediaStream localStream,String userName); //添加本地流进行预览
    void addRemoteStream(MediaStream remoteStream,String userName);//添加远端的流进行显示
    void closeWindow(String userName);                          //关闭窗口
}
