package com.cloud.phone.webrtc;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.cloud.phone.model.Message;
import com.cloud.phone.model.RoomType;
import com.cloud.phone.model.SignalingMessage;
import com.cloud.phone.model.User;
import com.cloud.phone.ui.ScreenShareActivity;
import com.cloud.phone.ui.ViewCallback;
import com.cloud.phone.util.LogUtil;
import com.cloud.phone.util.PreferenceUtil;
import com.cloud.phone.websocket.SocketInterface;
import com.cloud.phone.websocket.WebSocket;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;

import java.util.List;


/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class WebRtcManager implements WebRtcInterface,ConnectionInterface, SocketInterface, ViewCallback {
    private static WebRtcManager webRtcManager = null;
    private SocketInterface socketInterface;          //Socket层接口
    private ConnectionInterface connectionInterface;  //Connection层接口
    private ViewCallback viewCallback;                //view层回掉接口
    private Context context;                          //上下文，TODO 内存泄漏警告，但是请注意挂断时释放了context，并不是说单列模式一定会造成内存泄漏，感觉没毛病！！！
    private EglBase eglBase;                          //EglBase可以获取openGL上下文，利用这个上下文可以直接进行渲染
    private String roomId;                            //房间号
    private RoomType roomType;                        //房间类型
    public Intent projectionResultData;
    public static final String SELF_NAME = "ouyang";
    private String webSocketAddress;

    private WebRtcManager(ViewCallback viewCallback, EglBase eglBase){
        init(viewCallback,eglBase);
    }

    private void init(ViewCallback viewCallback,EglBase eglBase){
        this.viewCallback = viewCallback;
        this.context = (Context) viewCallback;
        this.eglBase = eglBase;

        webSocketAddress = PreferenceUtil.getInstance().getString("webSocketAddress","");

    }

    public static WebRtcManager getInstance(ViewCallback viewCallback,EglBase eglBase){
        if (webRtcManager == null){
            synchronized (WebRtcManager.class){
                if (webRtcManager == null){
                    webRtcManager = new WebRtcManager(viewCallback,eglBase);
                }
            }
        }else {
            synchronized (WebRtcManager.class){
                webRtcManager.init(viewCallback,eglBase);
            }
        }
        return webRtcManager;
    }

    public static WebRtcManager getInstance(){
        if (webRtcManager == null || webRtcManager.viewCallback == null){
            throw new RuntimeException("please use getInstance which has parameter first before use");
        }
        return webRtcManager;
    }

    //TODO 这个context将会在PeerConnectionManager创建必要资源是使用
    //TODO 转换成ApplicationContext非常重要，不转换会出现各种bug，根本无法定位到！！！
    public Context getContext(){
        return context.getApplicationContext();
    }

    public EglBase getEglBase(){
        return eglBase;
    }

    //向服务器发起建立WebSocket连接的请求
    private void connect(){
        if (socketInterface == null){
            socketInterface = new WebSocket();
            connectionInterface = PeerConnectionManager.getInstance(roomType);
        }
        LogUtil.d("webSocketAddress = " + webSocketAddress);
        if(webSocketAddress != null && !webSocketAddress.isEmpty()) {
            socketInterface.connect(webSocketAddress);
        }else{
            String uri = WebRtcConfig.SOCKET_URI;//+ "/" + WebRtcConfig.USER_ID + "/" + WebRtcConfig.DEVICE;
            socketInterface.connect(uri);
        }
    }

    /**==================================WebRtcInterface===========================*/
    //静音切换
    @Override
    public void switchMute(boolean mute) {
        connectionInterface.switchMute(mute);
    }
    //前置摄像头切换
    @Override
    public void switchCamera() {
        connectionInterface.switchCamera();
    }
    //免提
    @Override
    public void switchHandsfree(boolean handsfree) {
        connectionInterface.switchHandsfree(handsfree);
    }
    //摄像头开关
    @Override
    public void powerCamera(boolean enableCamera) {
        connectionInterface.powerCamera(enableCamera);
    }
    //主动挂断，这时应该解除对象的引用
    //顺序不能变，因为viewCallback不需要再回调了
    @Override
    public void hangUp() {
        viewCallback = null;
        context = null;
        eglBase = null;
        if (connectionInterface != null){
            connectionInterface.hangUp();
            connectionInterface = null;
        }
    }

    //发起聊天请求
    @Override
    public void chatRequest(RoomType roomType, String roomId) {
        this.roomType = roomType;
        this.roomId = roomId;
        connect();
    }

    //socket连接建立后申请加入房间
    @Override
    public void joinRoom() {

        SignalingMessage message = new SignalingMessage();
        message.id = "joinRoom";
        message.name = SELF_NAME;
        message.room = roomId;
        message.url = "webcam";
        message.mode = "video-and-audio";
        message.isLoop = true;
        socketInterface.joinRoom(message);
    }

    /**==================================WebRtcInterface===========================*/

    /**====================================ConnectionInterface============================*/
    //socket建立成功，启动activity并发起加入房间申请
    @Override
    public void connectSuccess(User user) {

        //将服务器返回的数据保存起来，其中最重要的就是userId
        connectionInterface.connectSuccess(user);

        //根据不同的房间类型启动不同的Activity
        switch (roomType) {
            case MEETING:
                ScreenShareActivity.startSelf(context);
                break;
            default:
                break;
        }
    }

    @Override
    public void remoteJoinToRoom(SignalingMessage user) {
        connectionInterface.remoteJoinToRoom(user);
    }

    @Override
    public void remoteOutRoom(SignalingMessage user) {
        connectionInterface.remoteOutRoom(user);
    }

    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        connectionInterface.onReceiveOffer(socketId,sdp);
    }

    @Override
    public void onReceiveAnswer(String socketId, String sdp) {
        connectionInterface.onReceiveAnswer(socketId,sdp);
    }

    @Override
    public void onRemoteCandidate(String socketId, IceCandidate iceCandidate) {
        connectionInterface.onRemoteCandidate(socketId,iceCandidate);
    }

    //创建P2P连接
    @Override
    public void createConnection(List<SignalingMessage> members) {
        connectionInterface.createConnection(members);
    }

    //获取链接数
    @Override
    public int getConnectNum() {
        return connectionInterface.getConnectNum();
    }

    /**====================================ConnectionInterface============================*/

    /**====================================SocketInterface============================*/
    @Override
    public void connect(String uri) {

    }

    @Override
    public void sendOffer(String socketId, SessionDescription localDescription) {
        socketInterface.sendOffer(socketId,localDescription);
    }

    @Override
    public void sendAnswer(String socketId, SessionDescription localDescription) {
        socketInterface.sendAnswer(socketId,localDescription);
    }

    @Override
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        socketInterface.sendIceCandidate(socketId,iceCandidate);
    }

    @Override
    public void close() {
        LogUtil.i("WebRtcManager close...");
        if (socketInterface != null){
            socketInterface.close();
            socketInterface = null;
        }
    }

    @Override
    public void joinRoom(SignalingMessage message) {

    }
    /**====================================SocketInterface============================*/


    /**====================================ViewCallback============================*/
    @Override
    public void socketCallback(Message message) {
        if (viewCallback == null)return;
        viewCallback.socketCallback(message);
        ScreenShareActivity.startSelf(context);
    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {
        viewCallback.setLocalStream(localStream,selfId);
    }

    @Override
    public void addRemoteStream(MediaStream remoteStream, String socketId) {
        viewCallback.addRemoteStream(remoteStream,socketId);
    }

    @Override
    public void closeWindow(String socketId) {
        if (viewCallback != null){
            viewCallback.closeWindow(socketId);
        }
    }
    /**====================================ViewCallback============================*/

    //Toast
    public void showMessage(String message){
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
