package com.cloud.phone.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cloud.phone.model.BaseMessage;
import com.cloud.phone.model.Message;
import com.cloud.phone.model.Room;
import com.cloud.phone.model.RoomType;
import com.cloud.phone.util.LogUtil;
import com.cloud.phone.util.PositionUtil;
import com.cloud.phone.webrtc.WebRtcInterface;
import com.cloud.phone.webrtc.WebRtcManager;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenShareActivity extends AppCompatActivity implements WebRtcInterface, ViewCallback {

    private FrameLayout videoFrameLayout;
    private WebRtcInterface manager;
    private EglBase eglBase;
    private SurfaceViewRenderer surfaceViewRenderer;
    private ProxyVideoSink videoSink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_share);
        LogUtil.d("ScreenShareActivity start " + System.currentTimeMillis());

        videoFrameLayout = findViewById(R.id.video_frame_layout);
        eglBase = EglBase.create();
        manager = WebRtcManager.getInstance(this,eglBase);
        manager.joinRoom();
    }

    /**===============================WebRtcInterface========================*/
    @Override
    public void switchMute(boolean mute) {
        manager.switchMute(mute);
    }

    @Override
    public void switchCamera() {
        manager.switchCamera();
    }

    @Override
    public void switchHandsfree(boolean handsfree) {
        manager.switchHandsfree(handsfree);
    }

    @Override
    public void powerCamera(boolean enable) {
        manager.powerCamera(enable);
    }

    //挂断时释放自己的资源，并关闭webSocket,
    //webSocket的关闭将会得到远端的响应
    @Override
    public void hangUp() {
        LogUtil.d("hangUp");
        exitRoom();
        finish();
    }

    @Override
    public void chatRequest(RoomType roomType, String roomId) {

    }

    @Override
    public void joinRoom() {

    }
    /**===============================WebRtcInterface========================*/


    /**================================ViewCallback==========================*/

    @Override
    public void socketCallback(Message message) {
        switch (message.getMessageType()){
            case SOCKET_OPEN:
                break;
            case SOCKET_CLOSE:
                break;
            case SOCKET_ERROR:
                break;
            case ROOM_FULL:
                showFullMessage(message);
                break;
            default:
                break;
        }
    }

    private void showFullMessage(Message message){
        BaseMessage<Room,Object> baseMessage = message.transForm(new BaseMessage<Room, Object>() {});
        String msg = "房间已满，房间号： " + baseMessage.getMessage().getRoomId() + " maxSize: " + baseMessage.getMessage().maxSize() + " currentSize: " + baseMessage.getMessage().currentSize();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ScreenShareActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 调用这个方法，创建surfaceView并渲染刷新界面，即将自己摄像头信息显示在屏幕上
     * @param localStream 本地流
     * @param selfId 用户id,请求建立连接后服务器返回的id
     */
    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {
        LogUtil.d("setLocalStream");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(localStream,selfId);
            }
        });
    }

    /**
     * 调用这个方法，创建surfaceView并渲染刷新界面，即将远端摄像头信息显示在屏幕上
     * @param remoteStream
     * @param socketId
     */
    @Override
    public void addRemoteStream(MediaStream remoteStream, String socketId) {
        LogUtil.d("addRemoteStream");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(remoteStream,socketId);
            }
        });
    }

    //当远端有人退出房间是这个方法会被回调，则将窗口关闭
    @Override
    public void closeWindow(String socketId) {
        LogUtil.d("closeWindow");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeView(socketId);
            }
        });
    }
    /**================================ViewCallback==========================*/

    /**
     * 当有人离开退出房间是则移除对应的窗口
     * @param socketId
     */
    private void removeView(String socketId){
        if (surfaceViewRenderer != null){
            surfaceViewRenderer.release();
        }
        if (videoSink != null){
            videoSink.setTarget(null);
        }
        videoFrameLayout.removeView(surfaceViewRenderer);
        rePlaceView();
    }

    /**
     * 每当有一个人加入房间这个方法就会被调用一次，
     * 创建surfaceView，添加到窗口中，并重新计算宽高设置摆放位置
     * @param stream 媒体流（本地或远端）
     * @param userId 用户id
     */
    public void addView(MediaStream stream,String userId){

        //创建surfaceView并初始化
        surfaceViewRenderer = new SurfaceViewRenderer(this);//采用webrtc中的surfaceView
        surfaceViewRenderer.init(eglBase.getEglBaseContext(),null);       //初始化surfaceView
        //surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT); //设置缩放模式 SCALE_ASPECT_FIT:按照view宽高设置，SCALE_ASPECT_FILL：按照摄像头预览画面设置
        surfaceViewRenderer.setMirror(true);                                             //镜像翻转

        videoSink = new ProxyVideoSink();
        videoSink.setTarget(surfaceViewRenderer);

        //将摄像头数据渲染到surfaceView
        if (stream.videoTracks.size()>0){
            stream.videoTracks.get(0).addSink(surfaceViewRenderer);
        }

        //将surfaceView添加到窗口中
        videoFrameLayout.addView(surfaceViewRenderer);

        //保存数据

        rePlaceView();
    }

    //重新计算宽高，设置摆放位置
    private void rePlaceView(){
        if (surfaceViewRenderer != null){
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.width = PositionUtil.getWith(this,1);
            layoutParams.height = PositionUtil.getWith(this,1);
            layoutParams.leftMargin = PositionUtil.getX(this,1,0);
            layoutParams.topMargin = PositionUtil.getY(this,1,0);
            surfaceViewRenderer.setLayoutParams(layoutParams);
        }
    }

    //退出房间
    private void exitRoom(){
        manager.hangUp();
        surfaceViewRenderer.release();
        videoSink.setTarget(null);
        eglBase = null;
    }


    public static void startSelf(Context context){
        Intent intent = new Intent(context,ScreenShareActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitRoom();
    }


}