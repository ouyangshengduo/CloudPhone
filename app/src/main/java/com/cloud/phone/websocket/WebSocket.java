package com.cloud.phone.websocket;

import com.cloud.phone.CloudPhoneApplication;
import com.cloud.phone.model.BaseMessage;
import com.cloud.phone.model.CandidateMessage;
import com.cloud.phone.model.Event;
import com.cloud.phone.model.Message;
import com.cloud.phone.model.MessageType;
import com.cloud.phone.model.OfferOrAnswerMessage;
import com.cloud.phone.model.SignalingMessage;
import com.cloud.phone.model.User;
import com.cloud.phone.model.WebCandidate;
import com.cloud.phone.util.LogUtil;
import com.cloud.phone.webrtc.WebRtcManager;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class WebSocket implements SocketInterface {

    private static final String TAG = "WebSocket";

    private WebRtcManager manager;
    private WebSocketClient webSocketClient;
    private Gson gson = new Gson();
    private Timer timer = null;

    public WebSocket(){
        this.manager = WebRtcManager.getInstance();
    }

    //请求服务器建立socket连接
    @Override
    public void connect(String socketUri) {
        URI uri = null;
        try {
            uri = new URI(socketUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocketClient = new WebSocketClient(uri) {
            //连接成功
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                LogUtil.d("onOpen");
                handleMessage(MessageType.SOCKET_OPEN,null);
            }
            //消息推送
            @Override
            public void onMessage(String message) {
                LogUtil.d("receive onMessage: " + message);

                SignalingMessage signalingMessage = gson.fromJson(message,SignalingMessage.class);
                Event event = new Event();
                event.objA = signalingMessage;
                event.objB = message;
                MessageType messageType = MessageType.getMessageType(signalingMessage.id);
                LogUtil.d("message type: " + messageType.getType());
                handleMessage(messageType,event);
            }
            //关闭
            @Override
            public void onClose(int code, String reason, boolean remote) {
                LogUtil.d("onClose-reason: " + reason + " code: " + code + " remote: " + remote);
                Event event = new Event();
                event.code = code;
                event.message = reason;
                handleMessage(MessageType.SOCKET_CLOSE,event);
            }
            //连接失败
            @Override
            public void onError(Exception ex) {
                LogUtil.e("error: " + ex.getMessage());
                Event event = new Event();
                event.message = ex.getMessage();
                handleMessage(MessageType.SOCKET_ERROR,event);
            }
        };

        if(socketUri.startsWith("wss")){
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                TrustManager[] trustManager = new TrustManager[]{new TrustManagerTest()};
                context.init(null,trustManager,new SecureRandom());
                SSLSocketFactory factory = context.getSocketFactory();
                if (factory != null){
                    webSocketClient.setSocketFactory(factory);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LogUtil.i("websocket connecting... (uri: " + socketUri + ")");
        webSocketClient.connect();//建立连接
    }

    private void heartbeatStart(){
         if(timer != null)return;
         final String heart = "{\"id\":\"heartbeat\"}";
         timer = new Timer();
         timer.schedule(new TimerTask() {
             @Override
             public void run() {
                 if(webSocketClient == null){
                     timer.cancel();
                     return;
                 }
                 if(!webSocketClient.isOpen()){
                     LogUtil.w("socket is not open！");
                 }else{
                     //LogUtil.d("socket is open！");
                     LogUtil.d("send heartbeat: " + heart);
                     webSocketClient.send(heart);
                 }
             }
         },500,10000);
    }

    //关闭webSocket连接，这个关闭动作会被服务器监听到，
    //服务器会给处理关闭者的其他人发送消息，告诉大家有人离开了房间
    @Override
    public void close() {
        LogUtil.d("WebSocket close...");
        if (webSocketClient != null){
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    //消息分发
    private void handleMessage(MessageType messageType, Event event){
        switch (messageType){
            case SOCKET_OPEN:
                socketOpen();
                break;
            case SOCKET_CLOSE:
                socketClose(event);
                break;
            case SOCKET_ERROR:
                socketError(event);
                break;
            case CONNECT_OK:
                socketConnectOk(event);
                break;

            case JOIN:
                //comingSelf(event);
                break;
            case PARTICIPANTS:
                comingSelf(event);
                break;
            case NEW_PEER:
                someoneJoin(event);
                break;

            case OFFER:
                someoneSendOffer(event);
                break;
            case ANSWER:
                someoneAnswerOffer(event);
                break;
            case CANDIDATE:
                candidateSwitch(event);
                break;

            case LEAVE:
                someoneLeave(event);
                break;
            case HEART_BEAT:
                LogUtil.d((String)event.objB);
                break;

            case ROOM_FULL:
                roomFull(event);
                break;
        }
    }

    /**==================================处理webSocket消息========================*/

    //socket打开，也就是连接成功了，将信息回掉出去
    private void socketOpen(){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_OPEN);
        manager.socketCallback(message);

    }

    //socket关闭，也就是断开连接了，将信息回掉出去
    private void socketClose(Event event){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_CLOSE);
        message.setMessage(event.message);
        manager.socketCallback(message);
    }

    //socket发生错误
    private void socketError(Event event){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_ERROR);
        message.setMessage(event.message);
        manager.socketCallback(message);
    }


    private void socketConnectOk(Event event){
        Message message = (Message) event.objA;
        BaseMessage<User,Object> baseMessage = message.transForm(new BaseMessage<User, Object>() {});
        manager.connectSuccess(baseMessage.getMessage());
    }

    //自己加入房间后得到的服务器响应，服务器会返回房间信息,根据房间成员id分别建立Connection
    private void comingSelf(Event event){
        manager.createConnection(null);
    }

    //有人加入了房间
    private void someoneJoin(Event event){
        LogUtil.d("someone join");
        SignalingMessage message = (SignalingMessage)event.objA;
        manager.remoteJoinToRoom(message);
    }

    //有人离开房间
    private void someoneLeave(Event event){
        SignalingMessage message = (SignalingMessage)event.objA;
        manager.remoteOutRoom(message);
    }

    //有人发起了媒体协商,即收到了别人主动给自己发送的媒体协商数据
    private void someoneSendOffer(Event event){
        SignalingMessage message = (SignalingMessage)event.objA;
        manager.onReceiveOffer(message.name,message.sdpOffer);
    }

    //有人响应了媒体协商，即自己主动给别人发送媒体协商数据后得到了对方的响应
    private void someoneAnswerOffer(Event event){
        SignalingMessage message = (SignalingMessage)event.objA;
        manager.onReceiveAnswer(message.name,message.sdpAnswer);
    }

    private void candidateSwitch(Event event){
        LogUtil.d("receive candidate message: " + event.objB);
        SignalingMessage message = (SignalingMessage)event.objA;
        WebCandidate webCandidate = message.candidate;
        IceCandidate iceCandidate = new IceCandidate(webCandidate.sdpMid,webCandidate.sdpMLineIndex,webCandidate.candidate);

        manager.onRemoteCandidate(message.name,iceCandidate);
    }

    //房间已满
    private void roomFull(Event event){
        Message message = (Message) event.objA;
        manager.socketCallback(message);
    }

    /**==================================处理webSocket消息========================*/


    /**============================通过webSocket发送消息========================*/
    //发起加入房间请求
    @Override
    public void joinRoom(SignalingMessage signalingMessage){
        String jsonData = gson.toJson(signalingMessage);
        LogUtil.d("send joinRoom,json: " + jsonData);
        if(webSocketClient.isOpen()) {
            webSocketClient.send(jsonData);
            heartbeatStart();
        }
    }

    //向房间的其他成员发送自己的SDP信息
    @Override
    public void sendOffer(String socketId, SessionDescription localDescription) {

        OfferOrAnswerMessage message = new OfferOrAnswerMessage();
        message.id = "receiveVideoFrom";
        message.sender = socketId;
        message.sdpOffer = localDescription.description;
        String jsonData = gson.toJson(message);
        LogUtil.d("sendOffer,json: " + jsonData);
        if(webSocketClient.isOpen()) {
            webSocketClient.send(jsonData);
        }
    }

    //发送应答,告诉对方自己的SDP
    @Override
    public void sendAnswer(String socketId, SessionDescription localDescription) {
        OfferOrAnswerMessage message = new OfferOrAnswerMessage();
        message.id = "receiveVideoAnswer";
        message.sender = socketId;
        message.sdpOffer = localDescription.description;
        String jsonData = gson.toJson(message);
        LogUtil.d("sendAnswer,json: " + jsonData);
        if(webSocketClient.isOpen()) {
            webSocketClient.send(jsonData);
        }
    }

    //向房间的其他成员发送自己的iceCandidate信息
    @Override
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {

        WebCandidate candidate = new WebCandidate();
        candidate.sdpMid = iceCandidate.sdpMid;
        candidate.sdpMLineIndex = iceCandidate.sdpMLineIndex;
        candidate.candidate = iceCandidate.sdp;

        CandidateMessage message = new CandidateMessage();
        message.id = "onIceCandidate";
        message.name = WebRtcManager.SELF_NAME + CloudPhoneApplication.getInstance().getShareID();
        message.candidate = candidate;
        String jsonData = gson.toJson(message);
        LogUtil.d("sendIceCandidate,json: " + jsonData);
        if(!webSocketClient.isOpen()){
            LogUtil.w(TAG, "sendIceCandidate websocket is not open!");
        }
        if(webSocketClient.isOpen()) {
            webSocketClient.send(jsonData);
        }
    }
    /**============================通过webSocket发送消息========================*/

    //实现一个接口什么都不做，相当于忽略整数
    class TrustManagerTest implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
