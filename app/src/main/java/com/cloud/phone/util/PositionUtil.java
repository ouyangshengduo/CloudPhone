package com.cloud.phone.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public class PositionUtil {

    private static int screenWith = 0;//屏幕宽

    public static int getWith(Context context,int size){
        if (screenWith == 0){
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWith = metrics.widthPixels;
        }
        if (size<4){
            return screenWith/2;
        }else {
            return screenWith/3;
        }
    }

    public static int getX(Context context,int size,int index){
        if (screenWith == 0){
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWith = metrics.widthPixels;
        }
        if (size<=4){
            if (size == 3 && index == 2){
                return screenWith/4;
            }
            return (index%2) * screenWith/2;
        }else if (size<=9){
            if (size == 5){
                if (index == 3){
                    return screenWith/6;
                }
                if (index == 4){
                    return screenWith/2;
                }
            }
            if (size == 7 && index == 6){
                return screenWith/3;
            }
            return (index%3) * screenWith/3;
        }
        return 0;
    }

    public static int getY(Context context,int size, int index) {
        if (screenWith == 0){
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWith = metrics.widthPixels;
        }

        if (size < 3) {
            return screenWith / 4;
        } else if (size < 5) {
            if (index < 2) {
                return 0;
            } else {
                return screenWith / 2;
            }
        } else if (size < 7) {
            if (index < 3) {
                return screenWith / 2 - (screenWith / 3);
            } else {
                return screenWith / 2;
            }
        } else if (size <= 9) {
            if (index < 3) {
                return 0;
            } else if (index < 6) {
                return screenWith / 3;
            } else {
                return screenWith / 3 * 2;
            }

        }
        return 0;
    }
}
