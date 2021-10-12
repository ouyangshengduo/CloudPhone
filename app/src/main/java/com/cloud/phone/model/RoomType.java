package com.cloud.phone.model;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public enum RoomType {

    MEETING(0,"MEETING"),                //会议室、多人聊天
    SINGLE(1,"SINGLE"),                  //一对一音视频聊天
    SINGLE_AUDIO(2,"SINGLE_AUDIO"),      //一对一音频聊天
    LIVE(3,"LIVE"),                      //直播
    DEFAULT(-1,"DEFAULT");               //默认值

    private int code;
    private String type;

    RoomType(int code, String type){
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public static RoomType getRooType(int code){
        for (RoomType type:values()){
            if (type.getCode() == code){
                return type;
            }
        }
        return DEFAULT;
    }
}
