package com.cloud.phone.util;

import android.util.Log;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class LogUtil {

    private static final int VERBOSE = 1;
    private static final int DEBUG = 2;
    private static final int INFO = 3;
    private static final int WARN = 4;
    private static final int ERROR = 5;
    private static final int NOTHING = 6;
    private static int level = DEBUG;
    private static String defaultTag = "CloudPhone";

    public static void v(String tag, String msg){
        if(level <= VERBOSE){
            Log.v(tag,msg);
        }
    }

    public static void d(String tag, String msg){
        if(level <= DEBUG){
            Log.d(tag,msg);
        }
    }

    public static void i(String tag, String msg){
        if(level <= INFO){
            Log.i(tag,msg);
        }
    }

    public static void w(String tag, String msg){
        if(level <= WARN){
            Log.w(tag,msg);
        }
    }

    public static void e(String tag, String msg){
        if(level <= ERROR){
            Log.e(tag,msg);
        }
    }

    public static void v(String msg){
        if(level <= VERBOSE){
            Log.v(defaultTag,msg);
        }
    }

    public static void d(String msg){
        if(level <= DEBUG){
            Log.d(defaultTag,msg);
        }
    }

    public static void i(String msg){
        if(level <= INFO){
            Log.i(defaultTag,msg);
        }
    }

    public static void w(String msg){
        if(level <= WARN){
            Log.w(defaultTag,msg);
        }
    }

    public static void e(String msg){
        if(level <= ERROR){
            Log.e(defaultTag,msg);
        }
    }

}
