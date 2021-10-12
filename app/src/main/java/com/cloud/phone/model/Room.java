package com.cloud.phone.model;

import java.util.ArrayList;
import java.util.List;


/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class Room {

    private RoomType roomType;        //房间类型
    private String roomId;            //房间id
    private String createTime;        //创建时间
    private String createUserId;      //创建用户id
    private List<String> membersId;   //房间里的成员id，包括创建用户
    private int maxSize;              //房间最大能容纳的人数

    private Room(RoomType roomType){
        membersId = new ArrayList<>();
        this.roomType = roomType;
        switch (roomType){
            //如果是一对一通信，房间的最大值为2
            case SINGLE:
            case SINGLE_AUDIO:
                this.maxSize = 2;
                break;
            //否则为100
            default:
                this.maxSize = 100;
                break;
        }
    }

    //创建一个房间
    public static Room createRoom(RoomType roomType){
        return new Room(roomType);
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public List<String> getMembers() {
        return membersId;
    }

    public void addMemberId(String userId){
        this.membersId.add(userId);
    }

    public void addMembersId(List<String> membersId){
        this.membersId.addAll(membersId);
    }

    public void remove(String userId){
        membersId.remove(userId);
    }

    //房间最大容纳值
    public int maxSize() {
        return this.maxSize;
    }

    //当前房间人数
    public int currentSize(){
        return this.membersId.size();
    }

    //房间剩余大小
    public int leftSize(){
        return this.maxSize - this.membersId.size();
    }
}
