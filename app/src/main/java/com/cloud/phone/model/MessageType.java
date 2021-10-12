package com.cloud.phone.model;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public enum MessageType {

    SOCKET_OPEN(0,"SOCKET_OPEN"),       //webSocket打开
    SOCKET_MESSAGE(1,"SOCKET_MESSAGE"), //webSocket发送消息
    SOCKET_CLOSE(2,"SOCKET_CLOSE"),     //webSocket关闭
    SOCKET_ERROR(3,"SOCKET_ERROR"),     //webSocket错误

    CONNECT_OK(4,"CONNECT_OK"),         //webSocket连接成功

    JOIN(10,"joinRoom"),                    //自己加入房间
    PARTICIPANTS(11,"existingParticipants"),//房间里的成员信息
    NEW_PEER(12,"newParticipantArrived"),  //新人加入房间（自己已经在房间里）

    OFFER(13,"receiveVideoFrom"),           //有人发起了媒体协商，即有人向自己发送了他的媒体协商数据
    ANSWER(14,"receiveVideoAnswer"),        //响应媒体协商，即自主动给别人发送了媒体协商数据后对方给自己响应他的媒体协商数据
    CANDIDATE(15,"iceCandidate"),         //网络协商

    LEAVE(16,"participantLeft"),            //有人离开房间，即有人离开了自己所在的房间
    HEART_BEAT(17,"heartbeat"),             //心跳

    ROOM_FULL(18,"ROOM_FULL"),          //房间已满

    CUSTOM(1001,"CUSTOM"),              //自定义消息类型，这样的消息服务器只做转发不做任何处理，为客户端信息交换扩展而设计

    DEFAULT(-1,"DEFAULT");              //默认值

    private int code;
    private String type;

    MessageType(int code, String type){
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public static MessageType getMessageType(int code){
        for (MessageType type:values()){
            if (type.getCode() == code){
                return type;
            }
        }
        return DEFAULT;
    }

    public static MessageType getMessageType(String type){
        for(MessageType messageType:values()){
           if(type.equals(messageType.getType())){
               return messageType;
           }
        }
        return DEFAULT;
    }
}
