package com.cloud.phone;

import android.app.Application;

import com.cloud.phone.util.PreferenceUtil;

public class CloudPhoneApplication extends Application {

    private static CloudPhoneApplication app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        PreferenceUtil.initPreference(this, "CloudPhone");
    }

    public static CloudPhoneApplication getInstance(){
        return app;
    }


}
