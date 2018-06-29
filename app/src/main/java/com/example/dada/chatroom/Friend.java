package com.example.dada.chatroom;

import android.util.Log;

/**
 * Created by dada on 2018/5/10.
 */
public class Friend {
    public String date;

    public Friend(){
        Log.i("ok","Friends()");

    }

    public Friend(String date){
        Log.i("ok","Friends(String)");
        this.date = date;
    }

    public String getDate(){return date;}

    public void setDate(){this.date = date;}


}

