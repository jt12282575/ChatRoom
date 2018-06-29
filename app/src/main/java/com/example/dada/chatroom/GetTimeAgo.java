package com.example.dada.chatroom;

import android.app.Application;
import android.content.Context;

/**
 * Created by dada on 2018/5/19.
 */

public class GetTimeAgo extends Application {
    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimeAgo(long time, Context ctx){
        if(time < 1000000000000L){
            //if timestamps given in seconds , convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if(time > now || time < 0){
            return null;
        }

        final long diff = now - time;
        if(diff < MINUTE_MILLIS){
            return "不久前";
        }else if(diff< 2*MINUTE_MILLIS){
            return "1分鐘前";
        }else if(diff < 50 * MINUTE_MILLIS){
            return diff / MINUTE_MILLIS + "分鐘前";
        }else if(diff < 90 * MINUTE_MILLIS){
            return "1小時前";
        }else if(diff < 24 * HOUR_MILLIS){
            return diff/HOUR_MILLIS +"小時前";
        }else if(diff < 48 * HOUR_MILLIS){
            return "昨天";
        }else{
            return diff/ DAY_MILLIS+"天前";
        }

    }


}
