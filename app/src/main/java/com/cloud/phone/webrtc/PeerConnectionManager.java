package com.cloud.phone.webrtc;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.cloud.phone.CloudPhoneApplication;
import com.cloud.phone.model.RoomType;
import com.cloud.phone.model.SignalingMessage;
import com.cloud.phone.model.User;
import com.cloud.phone.util.LogUtil;
import com.cloud.phone.util.PreferenceUtil;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class PeerConnectionManager implements ConnectionInterface{
    private static PeerConnectionManager perConnectionManager = null;
    private WebRtcManager manager;                       //中转类
    private RoomType roomType;                           //房间类型
    public String selfId;                                //自己的ID
    private ArrayList<SignalingMessage> socketIds;       //房间内其他人的id
    private Map<String,Peer> peerConnectionMap;          //会议室所有的P2P连接
    private ExecutorService executorService;             //线程池
    private PeerConnectionFactory peerConnectionFactory; //peerConnection工长
    private List<PeerConnection.IceServer> iceServers;   //ICE服务器结合，服务器有可能有多个
    private MediaStream localStream;                     //本地音视频流
    private AudioSource audioSource;                     //音频源
    private VideoSource videoSource;                     //视频源
    private AudioTrack audioTrack;                       //音轨
    private VideoTrack videoTrack;                       //视频轨
    private VideoCapturer videoCapturer;                 //视频捕获器
    private SurfaceTextureHelper surfaceTextureHelper;   //Surface帮助类，用于渲染
    private enum Role{caller,receiver,none}              //角色定义，如果是请求加入房间则是caller，如果别人加入房间则是receiver
    private Role role = Role.none;
    private AudioManager audioManager;

    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";//回音消除
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";//噪声抑制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"; //自动增益
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";   //高通滤波
    private static int width = 720;
    private static int height = 1280;
    private static int fps = 30;

    private static final String LOCAL_STREAM_ID = "ARDAMS";                      //本地流ID
    private static final String AUDIO_TRACK_ID = "ARDAMS-AUDIO";                 //音频轨ID
    private static final String VIDEO_TRACK_ID = "ARDAMS-VIDEO";                 //视频轨ID
    private static final String SURFACE_THREAD_NAME = "surfaceCaptureThread";    //线程名

    private PeerConnectionManager(RoomType roomType){
        init(roomType);
    }

    private void init(RoomType roomType){
        this.manager = WebRtcManager.getInstance();
        this.roomType = roomType;
        socketIds = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        peerConnectionMap = new HashMap<>();
        iceServers = new ArrayList<>();

        //创建stun服务器信息-打洞
        PeerConnection.IceServer stun = PeerConnection.IceServer.
                builder(WebRtcConfig.STUN_URI)
                .setUsername(WebRtcConfig.STUN_USER_NAME)
                .setPassword(WebRtcConfig.STUN_PASSWORD)
                .createIceServer();
        //创建turn服务器信息-转发
        PeerConnection.IceServer turn = PeerConnection.IceServer.
                builder(WebRtcConfig.TURN_URI)
                .setUsername(WebRtcConfig.TURN_USER_NAME)
                .setPassword(WebRtcConfig.TURN_PASSWORD)
                .createIceServer();
        iceServers.add(stun);
        iceServers.add(turn);

        String widthStr = PreferenceUtil.getInstance().getString("width","");
        String heightStr = PreferenceUtil.getInstance().getString("height","");
        String fpsStr = PreferenceUtil.getInstance().getString("fps","");

        LogUtil.d(" widthStr = " + widthStr + " heightStr = " + heightStr + " fpsStr = " + fpsStr);

        if(!widthStr.isEmpty() && !heightStr.isEmpty()){
            width = Integer.valueOf(widthStr);
            height = Integer.valueOf(heightStr);
        }

        if(!fpsStr.isEmpty()){
            fps = Integer.valueOf(fpsStr);
        }
    }

    public static PeerConnectionManager getInstance(RoomType roomType){
        if (perConnectionManager == null){
            synchronized (PeerConnectionManager.class){
                if (perConnectionManager == null){
                    perConnectionManager = new PeerConnectionManager(roomType);
                }
            }
        }else {
            synchronized (PeerConnectionManager.class){
                perConnectionManager.init(roomType);
            }
        }
        return perConnectionManager;
    }

    public static PeerConnectionManager getInstance(){
        if (perConnectionManager == null || perConnectionManager.manager == null){
            throw new RuntimeException("please use getInstance which has parameter first before use");
        }
        return perConnectionManager;
    }

    /**==================================ConnectionInterface===========================*/
    //webSocket连接成功,获取自己的id信息
    @Override
    public void connectSuccess(User user) {
        LogUtil.d("connectSuccess,userId: " + user.getUserId());
        this.selfId = user.getUserId();
        if (selfId == null || selfId.length() == 0){
            manager.showMessage("服务器返回了错误的userId！");
        }
    }

    //远端有人加入房间
    @Override
    public void remoteJoinToRoom(SignalingMessage user) {
        LogUtil.i("remoteJoinRoom");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (localStream == null){
                    createLocalStream();
                }
                Peer peer = new Peer(user.name);
                peer.peerConnection.addStream(localStream);
                socketIds.add(user);
                peerConnectionMap.put(user.name,peer);
            }
        });
    }
    //有人离开房间
    @Override
    public void remoteOutRoom(SignalingMessage user) {
        LogUtil.d("remoteOutRoom,userId: " + user.name);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                closePeerConnection(user.name);
            }
        });
    }
    //有人发起了SDP Offer
    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        LogUtil.d("receiveOffer,userId: " + socketId + " sdp: " + sdp);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                role = Role.receiver;
                Peer peer = peerConnectionMap.get(socketId);
                if (peer != null){
                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER,sdp);
                    peer.peerConnection.setRemoteDescription(peer,sessionDescription);
                }
            }
        });
    }
    //有人回应了SDP
    @Override
    public void onReceiveAnswer(String socketId, String sdp) {
        LogUtil.d("receiveAnswer,userId: " + socketId + " sdp: " + sdp);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER,sdp);
                currentPeer.peerConnection.setRemoteDescription(currentPeer,sessionDescription);
            }
        });
    }
    //有人回应了Candidate
    //TODO 特别注意！！！，在信令交换过程中务必保证IceCandidate中sdpMid和sdp是非null的，
    //TODO 否则在调用peerConnection.addIceCandidate(iceCandidate)时会发生空指针异常，这个错误非常难以排查
    @Override
    public void onRemoteCandidate(String socketId, IceCandidate iceCandidate) {
        LogUtil.d("receive IceCandidate,userId: " + socketId + " sdpMid: " + iceCandidate.sdpMid + " sdpMLineIndex: " + iceCandidate.sdpMLineIndex + " sdp: " + iceCandidate.sdp);
        currentPeer.peerConnection.addIceCandidate(iceCandidate);
    }

    //开始初始化数据并创建P2P连接
    @Override
    public void createConnection(List<SignalingMessage> members) {
        if(role == Role.none)role = Role.caller;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionFactory == null){
                    peerConnectionFactory = createPeerConnectionFactory();//创建工厂
                }
                if (localStream == null){
                    createLocalStream();     //创建本地流
                }
                createPeerConnections();     //创建p2p连接
                addStream();                 //为每个连接添加自己的音视频流，准备推流
                createOffers();              //创建SDP会话描述
            }
        });
    }

    @Override
    public void switchMute(boolean mute) {
        if (audioTrack != null){
            audioTrack.setEnabled(mute);
        }
    }

    @Override
    public void switchCamera() {
        if (videoCapturer == null)return;
        if (videoCapturer instanceof CameraVideoCapturer){
            ((CameraVideoCapturer)videoCapturer).switchCamera(null);
        }
    }

    @Override
    public void switchHandsfree(boolean handsfree) {
        if (audioManager == null){
            audioManager = (AudioManager) manager.getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(handsfree);
    }

    @Override
    public void powerCamera(boolean enable) {
        if (videoTrack != null){
            videoTrack.setEnabled(enable);
        }
    }
    //挂断,这会请求网络，是个耗时操作
    @Override
    public void hangUp() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                LogUtil.i("hangUp...");

                //关闭socket
                manager.close();

                if(currentPeer != null){
                    currentPeer.quitQueue();
                    currentPeer.peerConnection.close();
                }
                //关闭音频
                if (audioSource != null){
                    audioSource.dispose();
                    audioSource = null;
                }
                //关闭视频
                if (videoSource != null){
                    videoSource.dispose();
                    videoSource = null;
                }
                //关闭摄像头预览
                if (videoCapturer != null){
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
                //关闭surface帮助类
                if(surfaceTextureHelper != null){
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
                //关闭工厂
                if (peerConnectionFactory != null){
                    peerConnectionFactory.dispose();
                    peerConnectionFactory = null;
                }
                //清空列表
                if (iceServers != null){
                    iceServers.clear();
                    iceServers = null;
                }
                localStream = null;
                audioManager = null;

            }
        });
    }

    private void closePeerConnection(String socketId){
        LogUtil.i("closePeerConnection...");
        Peer peer = currentPeer;
        if (peer != null){
            peer.quitQueue();
            peer.peerConnection.close();
            peerConnectionMap.remove(socketId);
            socketIds.remove(socketId);
            manager.closeWindow(socketId);
        }
    }

    @Override
    public void chatRequest(RoomType roomType, String roomId) {

    }

    @Override
    public void joinRoom() {

    }



    //获取连接数，这里应该处理线程同步问题
    public int getConnectNum(){
        return peerConnectionMap.size();
    }
    /**==================================ConnectionInterface===========================*/

    //先创建PeerConnection工厂，用于创建PeerConnection
    private PeerConnectionFactory createPeerConnectionFactory(){
        VideoEncoderFactory videoEncoderFactory = null;
        VideoDecoderFactory videoDecoderFactory = null;
        if (roomType != RoomType.SINGLE_AUDIO){
            //创建视频编码器工厂并开启v8和h264编码，webrtc会自动选择最优的，当然也可以只开启其中一个
//            videoEncoderFactory = new HardwareVideoEncoderFactory(manager.getEglBase().getEglBaseContext(),false,false);
//            videoDecoderFactory = new HardwareVideoDecoderFactory(manager.getEglBase().getEglBaseContext());
            videoEncoderFactory = new DefaultVideoEncoderFactory(manager.getEglBase().getEglBaseContext(),true,true);
            //创建视频解密器工厂
            videoDecoderFactory = new DefaultVideoDecoderFactory(manager.getEglBase().getEglBaseContext());
        }
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        //将其他参数设置成默认值
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(manager.getContext()).createInitializationOptions());
        return PeerConnectionFactory.builder()
                .setOptions(options)          //设置网络类型，这是使用options自动判断
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(manager.getContext()).setAudioSource(MediaRecorder.AudioSource.MIC).setAudioFormat(AudioFormat.ENCODING_PCM_16BIT).createAudioDeviceModule()) //设置音频类型
                .setVideoEncoderFactory(videoEncoderFactory)//设置视频编码工厂
                .setVideoDecoderFactory(videoDecoderFactory)//设置视频解码工厂
                .createPeerConnectionFactory();
    }

    //创建本地音视频流
    private void createLocalStream(){
        //调用工厂方法创建流，其中label标签必须以ARDAMS开头，可以有后缀
        //localStream是音视频的承载，后面需要将音视频轨设置到其中
        localStream = peerConnectionFactory.createLocalMediaStream("102");

        //音频
        audioSource = peerConnectionFactory.createAudioSource(createMediaConstraints());    //创建音频源,并设置约束属性
        audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID,audioSource);    //采集音频
        localStream.addTrack(audioTrack);                                                   //将音轨设置到localStream里

        //视频
        if (roomType == RoomType.SINGLE_AUDIO)return;

        if(roomType == RoomType.CAMERA1 || roomType == RoomType.CAMERA2){
            videoCapturer = createCameraCapturer();
        }else if(roomType == RoomType.SCREEN){
            videoCapturer = createScreenCapture();
        }
        if(videoCapturer == null){
            LogUtil.e("videoCapturer = null");
            return;
        }


        //进行摄像头预览的设置，因为聊天室也需要显示自己的图像，这是借助了SurfaceTextureHelper通过openGL进行渲染，聊天室内每一个窗口的渲染会单独开一个线程
        surfaceTextureHelper = SurfaceTextureHelper.create(SURFACE_THREAD_NAME,manager.getEglBase().getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());//创建视频源
        videoCapturer.initialize(surfaceTextureHelper,manager.getContext(),videoSource.getCapturerObserver());//初始化videoCapturer
        LogUtil.d("width = " + width + " height = " + height + " fps = " + fps);
        videoCapturer.startCapture(width,height,fps);//开始采集 i:宽，i1:高，i2:帧率
        videoTrack = peerConnectionFactory.createVideoTrack("100",videoSource);    //创建视频轨

        localStream.addTrack(videoTrack);//将视频轨设置到localStream中

    }


    private Peer currentPeer;
    //创建p2p连接，注意这里的逻辑：
    //在开始加入房间的时候服务器会返回房间内所有人的id，
    //因为自己需要和每个人都要通信，因此必须和每个人都建立一个P2P连接，
    //通过id循环与房间内的每个人建立P2P连接
    private void createPeerConnections(){
        currentPeer = new Peer(WebRtcManager.SELF_NAME + CloudPhoneApplication.getInstance().getShareID());
    }

    //将本地流加入peerConnection中
    private void addStream(){
        if (localStream == null){
            createLocalStream();
        }
        LogUtil.d("add localStream");
        currentPeer.peerConnection.addStream(localStream);
    }

    //给每一个人发送邀请,附带自己流媒体信息，这里其实只是创建了一个SDP会话描述而已，
    //创建成功后onCreateSuccess被调用，接着设置SDP，最后才将这个SDP信息发送给对等方
    private void createOffers(){
        MediaConstraints mediaConstraints;
        if (roomType == RoomType.SINGLE_AUDIO){
            mediaConstraints = createMediaConstraintsForOfferAnswer(true,false);
        }else {
            mediaConstraints = createMediaConstraintsForOfferAnswer(true,true);
        }
        currentPeer.peerConnection.createOffer(currentPeer,mediaConstraints);
    }


    //在发起请求之前设置一些约束信息
    //可以控制是否输入音频及视频
    //MediaConstraints在设置到offer或answer中时的意思是“我需不需要音频或视频”
    private MediaConstraints createMediaConstraintsForOfferAnswer(boolean enableAudio,boolean enableVideo){
        MediaConstraints mediaConstraints = new MediaConstraints();
        List<MediaConstraints.KeyValuePair> keyValuePairList = new ArrayList<>();
        //控制音频传输
        keyValuePairList.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",String.valueOf(enableAudio)));
        //控制视频传输
        keyValuePairList.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",String.valueOf(enableVideo)));
        keyValuePairList.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement","true"));
        mediaConstraints.mandatory.addAll(keyValuePairList);
        return mediaConstraints;
    }

    //创建MediaConstraints，用于设置噪声抑制、回声消除、自动增益、高通滤波等各种约束
    private MediaConstraints createMediaConstraints(){
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT,"false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT,"true"));
        return audioConstraints;
    }

    private VideoCapturer createScreenCapture(){
        Intent intent = manager.projectionResultData;
        VideoCapturer videoCapturer =  new ScreenCapturerAndroid(intent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        });
        return videoCapturer;
    }

    //创建videoCapturer，设备有可能有camera1和camera2,每个camera有可能有前置和后置摄像头
    private VideoCapturer createCameraCapturer(){
        VideoCapturer videoCapturer;
        CameraEnumerator cameraEnumerator;
        //如果支持则默认使用camera2
        if (camera2Support()){
            cameraEnumerator = new Camera2Enumerator(manager.getContext());
            videoCapturer = createCameraCapturer(cameraEnumerator);
        }else {
            cameraEnumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapturer(cameraEnumerator);
        }
        return videoCapturer;
    }

    //根据CameraEnumerator类型创建相应VideoCapturer
    private VideoCapturer createCameraCapturer(CameraEnumerator cameraEnumerator){
        VideoCapturer capturer = null;
        String[] deviceNames = cameraEnumerator.getDeviceNames();

        if(roomType == RoomType.CAMERA1) {
            for (String name : deviceNames) {
                //默认优先使用前置摄像头
                if (cameraEnumerator.isFrontFacing(name)) {
                    capturer = cameraEnumerator.createCapturer(name, null);
                    if (capturer != null) {
                        return capturer;
                    }
                }
            }
            return null;
        }else if(roomType == RoomType.CAMERA2){
            for (String name:deviceNames){
                //否则使用后置摄像头
                if (!cameraEnumerator.isFrontFacing(name)){
                    capturer = cameraEnumerator.createCapturer(name,null);
                    if (capturer != null){
                        return capturer;
                    }
                }
            }
            return null;
        }

        return null;
    }

    //判断是否支持Camera2
    private boolean camera2Support(){
        return Camera2Enumerator.isSupported(manager.getContext());
    }

    //P2P连接封装类
    private class Peer implements PeerConnection.Observer, SdpObserver {
        private PeerConnection peerConnection;               //跟远端用户的一个连接
        private String socketId;                             //远端用户的id
        private DelayQueue<IceCandidate> delayQueue;//延迟队列

        public Peer(String socketId){
            this.socketId = socketId;
            //创建ice服务器信息配置,即stun和turn，从这里我们可以看出，p2p连接的建立先进行了内网穿透
            PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            configuration.iceTransportsType = PeerConnection.IceTransportsType.RELAY;
            configuration.disableIpv6 = true;
            configuration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
            //通过工厂创建连接,并设置回调
            peerConnection = peerConnectionFactory.createPeerConnection(configuration,this);
            if (WebRtcConfig.enableDelayQueue){
                delayQueue = new DelayQueue<>(50);
            }
        }

        /**==================================PeerConnection.Observer==================================*/

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            LogUtil.d("onSignalingChange signalingState = " + signalingState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            LogUtil.d("onIceConnectionChange iceConnectionState = " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            LogUtil.d("onIceConnectionReceivingChange b = " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            LogUtil.d("onIceGatheringChange iceGatheringState = " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            //如果开启了延迟队列将数据加入到延时队列中
            if (WebRtcConfig.enableDelayQueue){
                delayQueue.push(iceCandidate);
            }
            //否则直接发送
            else {
                manager.sendIceCandidate(socketId,iceCandidate);
                //manager.sendIceCandidate(selfId,iceCandidate);
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        //p2p连接建立成功后回调，mediaStream封装了音视频流，这时就可以回到ui层进行对方音视频的播放和显示了
        @Override
        public void onAddStream(MediaStream mediaStream) {
            LogUtil.d("onAddStream");
            manager.addRemoteStream(mediaStream,socketId);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            LogUtil.d("onRemoveStream");
            manager.closeWindow(socketId);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

        /**==================================SdpObserver==================================*/

        /**
         * 新的理解：
         * SA/SB:createOffer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，
         *  在实测中发现setLocalDescription调用后会触发内网穿透，即请求ICE服务器开始打洞，
         *  并且这个过程和接下来的SDP信息交换是异步执行的，
         *  在setLocalDescription()方法被调用前RTCPeerConnection都不会开始收集candidates,这是JSEP IRTF draft中要求的
         * @param sessionDescription 其中的description描述了媒体协商数据
         */
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            //设置本地sdp，如果设置成功则回调onSetSuccess
            peerConnection.setLocalDescription(this,sessionDescription);
        }

        /**
         * SA:
         * （7）setLocalDescription成功后onSetSuccess被调用，通过webSocket将自己的SDP信息发送给对等方
         * SB:
         * （5）setRemoteDescription设置成功后onSetSuccess被调用，调用createAnswer创建SDP会话描述
         * （6）createAnswer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，setLocalDescription触发内网穿透
         * （7）setLocalDescription设置成功后onSetSuccess再次被调用，响应对方的SDP offer，即通过webSocket将自己的SDP信息发送给对方
         *  一个SDP交换完成
         */
        @Override
        public void onSetSuccess() {
            //TODO 通过socket交换sdp,这里目前并没有很好的理解，先实现功能，后期再研究并注释
            if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER){
                MediaConstraints mediaConstraints;
                if (roomType == RoomType.SINGLE_AUDIO){
                    mediaConstraints = createMediaConstraintsForOfferAnswer(true,false);
                }else {
                    mediaConstraints = createMediaConstraintsForOfferAnswer(true,true);
                }
                peerConnection.createAnswer(this,mediaConstraints);
            }else if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER){
                //接收者，这个方法在实测时一直没有被触发，不知道为什么，注释后也能正常运行
                if (role == Role.receiver){
                    manager.sendAnswer(socketId,peerConnection.getLocalDescription());
                    //manager.sendAnswer(selfId,peerConnection.getLocalDescription());
                }
                //发送者
                else if (role == Role.caller){
                    manager.sendOffer(socketId,peerConnection.getLocalDescription());
                    //manager.sendOffer(selfId,peerConnection.getLocalDescription());
                }
            }
            else if (peerConnection.signalingState() == PeerConnection.SignalingState.STABLE){
                //这一句非常重要，在有人加入房间后，收到offer -> setRemoteDescription -> onCreateSuccess -> setLocalDescription,然后被调用
                //实测在注释掉之后通信无法建立
                if (role == Role.receiver){
                    manager.sendAnswer(socketId,peerConnection.getLocalDescription());
                    //manager.sendAnswer(selfId,peerConnection.getLocalDescription());
                }
            }

            //媒体协商完成，开始进行网络协商
            if ((role == Role.caller && peerConnection.getRemoteDescription() != null)
                    || (role == Role.receiver && peerConnection.getLocalDescription() != null)){
                //TODO 应当注意的是在没有使用延迟队列的时候媒体协商和网络协商是异步的，
                //TODO 在使用了延迟队列后媒体协商交换完成再进行网络协商，但是这么设计并不是唯一的
                sendCandidateFromQueue(socketId);
                //sendCandidateFromQueue(selfId);
            }
        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }

        //从队列中发送candidate
        private void sendCandidateFromQueue(String socketId){
            if (WebRtcConfig.enableDelayQueue){
                if (delayQueue.isRunning())return;
                delayQueue.run(delayQueueElement -> manager.sendIceCandidate(socketId,delayQueueElement.getData()));
            }
        }

        //终止队列
        public void quitQueue(){
            if (delayQueue != null){
                delayQueue.quit();
            }
        }
    }

    public String getSelfId(){
        return selfId;
    }

    public RoomType getRoomType(){
        return roomType;
    }
}
