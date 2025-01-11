package com.theflexproject.thunder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "upload_today";
    private static int Notif_Id;


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String originalTitle = remoteMessage.getData().get("originalTitle");
        String overview = remoteMessage.getData().get("overview");
        String posterPath = remoteMessage.getData().get("posterPath");
        String deepLink = remoteMessage.getData().get("deepLink");
        Log.i("notif", title);

        // Unduh gambar poster untuk ditampilkan dalam notifikasi
        Bitmap posterBitmap = getBitmapFromURL(posterPath);
        Uri data = Uri.parse(deepLink);
        String itemId = data.getQueryParameter("id");
        Notif_Id = Integer.parseInt(itemId);

        // Membuat intent untuk membuka deep link ketika notifikasi diklik
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(deepLink));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        // Membuat dan menampilkan notifikasi
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nfgplus) // Ganti dengan icon aplikasi Anda
                .setContentTitle(title)
                .setContentText(overview)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Judul Asli: " + originalTitle + "\n" + overview))
                .setLargeIcon(posterBitmap)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(Notif_Id, builder.build());
    }

    // Membuat channel untuk notifikasi (diperlukan pada Android O ke atas)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Latest Update";
            String description = "Channel untuk notifikasi film & series baru";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Mengambil gambar dari URL
    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

