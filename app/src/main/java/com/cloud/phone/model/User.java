package com.cloud.phone.model;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class User {

    private String userId;         //用户id
    private String userName;       //用户名
    private DeviceType deviceType; //使用的终端类型

    public User(){}

    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
