package com.theflexproject.thunder.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.theflexproject.thunder.R;

public class LoadingForegroundService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = createForegroundNotification("Loading, please wait...");
        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    private Notification createForegroundNotification(String content) {
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("loading_channel", "Loading Activity", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "loading_channel")
                .setContentTitle("App Loading")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_import_export)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

