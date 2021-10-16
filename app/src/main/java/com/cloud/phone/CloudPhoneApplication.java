package com.cloud.phone;

import android.app.Application;

import com.cloud.phone.util.PreferenceUtil;

import java.util.UUID;

public class CloudPhoneApplication extends Application {

    private static CloudPhoneApplication app;
    private String shareID;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        shareID = UUID.randomUUID().toString();
        PreferenceUtil.initPreference(this, "CloudPhone");

    }

    public static CloudPhoneApplication getInstance(){
        return app;
    }

    public String getShareID(){
        return shareID;
    }


}
