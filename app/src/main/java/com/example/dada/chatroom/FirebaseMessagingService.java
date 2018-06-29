package com.example.dada.chatroom;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by dada on 2018/3/11.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String notifycation_title = remoteMessage.getNotification().getTitle();
        String notifycation_body = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();
        String from_user_id = remoteMessage.getData().get("from_user_id");
        Log.i("ok","from user id : "+ from_user_id);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notifycation_title)
                .setContentText(notifycation_body);

        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("user_id",from_user_id);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);


        int notificationId = (int) System.currentTimeMillis();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, mBuilder.build());

    }
}
