package com.cloud.phone.ui;

import com.cloud.phone.util.LogUtil;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class ProxyVideoSink implements VideoSink {
    private VideoSink target;

    @Override
    synchronized public void onFrame(VideoFrame videoFrame) {
        if (target == null){
            LogUtil.d("Dropping frame in proxy because target is null!");
            return;
        }
        target.onFrame(videoFrame);
    }

    synchronized void setTarget(VideoSink videoSink){
        this.target = videoSink;
    }
}
