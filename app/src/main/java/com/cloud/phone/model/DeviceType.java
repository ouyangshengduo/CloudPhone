package com.cloud.phone.model;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public enum DeviceType {

    COMPUTER(0,"COMPUTER"),         //电脑
    PHONE(1,"PHONE"),               //手机
    DEFAULT(-1,"DEFAULT");          //默认值

    private int code;
    private String type;

    DeviceType(int code, String type){
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public static DeviceType getDeviceType(int code){
        for (DeviceType type:values()){
            if (type.getCode() == code){
                return type;
            }
        }
        return DEFAULT;
    }
}
