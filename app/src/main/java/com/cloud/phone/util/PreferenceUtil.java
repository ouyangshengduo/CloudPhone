package com.cloud.phone.util;

import android.content.Context;
import android.content.SharedPreferences;
/**
 * @author senduo.ouyang
 * @date 2020-10-14
 * @version 1.0
 */
public class PreferenceUtil {
    private static PreferenceUtil preference = null;
    private SharedPreferences sharedPreference;
    private String packageName = "";

    public PreferenceUtil(Context context, String parentFileName) {
        this.packageName = parentFileName;
        this.sharedPreference = context.getSharedPreferences(this.packageName, 0);
    }

    public static PreferenceUtil getInstance() {
        return preference;
    }

    public static PreferenceUtil initPreference(Context context, String parentFileName) {
        if(preference == null) {
            preference = new PreferenceUtil(context, parentFileName);
        }
        return preference;
    }

    public String getString(String name, String defaultvalue) {
        return this.sharedPreference.getString(name, defaultvalue);
    }

    public void remove(String name) {
        SharedPreferences.Editor edit = this.sharedPreference.edit();
        edit.remove(name);
        edit.commit();
    }

    public void setString(String name, String value) {
        SharedPreferences.Editor edit = this.sharedPreference.edit();
        edit.putString(name, value);
        edit.commit();
    }

    public boolean getBoolean(String name, boolean defaultvalue) {
        return this.sharedPreference.getBoolean(name, defaultvalue);
    }

    public void setBoolean(String name, boolean value) {
        SharedPreferences.Editor edit = this.sharedPreference.edit();
        edit.putBoolean(name, value);
        edit.commit();
    }
}
