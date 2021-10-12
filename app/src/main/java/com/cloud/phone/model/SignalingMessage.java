package com.cloud.phone.model;

import java.util.ArrayList;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class SignalingMessage {
    public String id;
    public String name;
    public String room;
    public String url;
    public String mode;
    public boolean isLoop;

    public String sender;

    public String sdpOffer;
    public String sdpAnswer;
    public WebCandidate candidate;

    public boolean muteFlag;

    public ArrayList<String> data;
    public ArrayList<SignalingMessage> dataObj;
}
