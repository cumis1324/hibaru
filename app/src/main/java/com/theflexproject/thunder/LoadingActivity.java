package com.theflexproject.thunder;

import android.app.ActivityManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.database.AppDatabase;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.utils.LoadingForegroundService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoadingActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private static FirebaseUser currentUser;
    private ViewGroup rootView;
    private ProgressBar progressBar;
    private static final String LAST_MODIFIED_PREF = "last_modified_pref";
    private static final String LAST_MODIFIED_KEY = "last_modified_key";
    private static final String TAG = "loading";
    private TextView pesan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, LoadingForegroundService.class));
        }
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        progressBar = findViewById(R.id.progress_datar);
        pesan = findViewById(R.id.loading_message);
        // Pastikan ada ProgressBar di layout Anda

        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("PROGRESS_UPDATE"));

        // Ambil URL backup dan masukkan ke dalam WorkManager
        String backupFileUrl = getBackupFileUrl();
        Uri deepLinkData = getIntent().getData();

        // Kirim URL melalui InputData ke Worker
        Data inputData = new Data.Builder()
                .putString("backup_file_url", backupFileUrl)
                .putString("deeplink", String.valueOf(deepLinkData))
                .build();


        // Mulai pengecekan modifikasi dengan WorkManager
        ModifiedCheckWorker.enqueueWork(this, inputData);


    }

    // Receiver untuk menerima pembaruan progress
    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            String pesanIntent = intent.getStringExtra("pesan");
            progressBar.setProgress(progress);
            pesan.setText(pesanIntent);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }

    // Method untuk mengambil URL file backup
    private static String getBackupFileUrl() {
        if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
            return "https://drive3.nfgplusmirror.workers.dev/0:/database/demo.db";
        }
        return "https://drive3.nfgplusmirror.workers.dev/0:/database/nfg.db";
    }

    public static class RestoreDatabaseWorker extends Worker {

        public RestoreDatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                setForegroundAsync(createForegroundInfo("Updating database..."));
                // Ambil URL dari InputData
                String backupFileUrl = getInputData().getString("backup_file_url");
                Uri deepLinkData = Uri.parse(getInputData().getString("deeplink"));

                // Download file dari URL dan restore database
                File downloadedFile = downloadFileFromUrl(backupFileUrl);


                if (downloadedFile != null) {
                    restoreDatabase(downloadedFile, deepLinkData);
                    return Result.success();
                } else {
                    // Set lastModifiedSaved to empty if download is interrupted or canceled
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                    prefs.edit().putString(LAST_MODIFIED_KEY, "").apply();
                    doWork();
                    return Result.failure();
                }
            } catch (IOException e) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                prefs.edit().putString(LAST_MODIFIED_KEY, "").apply();
                doWork();
                e.printStackTrace();
                return Result.failure();
            }
        }

        // Method untuk mendownload file dari URL ke penyimpanan lokal
        private File downloadFileFromUrl(String fileUrl) throws IOException {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file: " + connection.getResponseMessage());
            }

            // Simpan file ke penyimpanan lokal
            File localFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "demo.db");
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(localFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                int totalBytesRead = 0;
                int fileLength = connection.getContentLength();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    // Update progress
                    int progress = (int) ((totalBytesRead / (float) fileLength) * 100);
                    updateProgressBar(progress);
                }
            }catch (IOException e) {
                localFile.delete();
                // Set lastModifiedSaved to empty if download is interrupted or canceled
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                prefs.edit().putString(LAST_MODIFIED_KEY, "").apply();
                throw e;
            } finally {
                connection.disconnect();
            }
            return localFile;
        }
        @Override
        public void onStopped(){
            super.onStopped();
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
            prefs.edit().putString(LAST_MODIFIED_KEY, "").apply();
        }

        // Method untuk restore database dari file lokal
        private void restoreDatabase(File file, Uri deepLinkData) throws IOException {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos").build();
            db.close();
            File dbFile = getApplicationContext().getDatabasePath("MyToDos");

            // Hapus database lama jika ada
            if (dbFile.exists()) {
                dbFile.delete();
            }

            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
            }

            // Salin database baru dari file lokal ke database aplikasi
            try (InputStream is = new FileInputStream(file);
                 OutputStream os = new FileOutputStream(dbFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            }
            Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos").build();
            launchMainActivity(deepLinkData);
        }

        private void launchMainActivity(Uri deepLinkData) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Context context = getApplicationContext();
                // Memastikan bahwa aplikasi berada di latar depan sebelum membuka Activity
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                if (appProcesses != null) {
                    for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                        if (appProcess.processName.equals(context.getPackageName()) &&
                                appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

                            // Jika aplikasi berada di latar depan, buka MainActivity
                            Intent intnt = new Intent(context, MainActivity.class);
                            if (deepLinkData != null) {
                                intnt.setData(deepLinkData);
                            }
                            intnt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intnt);
                            break;
                        }
                    }
                }
            });
        }

        public static void enqueueWork(Context context, Data inputData) {
            // Penjadwalan restore menggunakan WorkManager dengan InputData
            OneTimeWorkRequest restoreRequest = new OneTimeWorkRequest.Builder(RestoreDatabaseWorker.class)
                    .setInputData(inputData)
                    .build();
            WorkManager.getInstance(context).enqueue(restoreRequest);
        }

        private void updateProgressBar(int progress) {
            Intent intent = new Intent("PROGRESS_UPDATE");
            if (progress <= 50){
                String pesan1 = "Updating database, please wait ....";
                intent.putExtra("pesan", pesan1);
                intent.putExtra("progress", progress);
            }
            String pesan = "Updating, Do not close the app....";
            intent.putExtra("pesan", pesan);
            intent.putExtra("progress", progress);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        private ForegroundInfo createForegroundInfo(String message) {
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), "loading_channel")
                    .setContentTitle("Database Restoration")
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_import_export)
                    .build();
            return new ForegroundInfo(1, notification);
        }
    }

    public static class ModifiedCheckWorker extends Worker {

        private static final String LAST_MODIFIED_PREF = "last_modified_pref";
        private static final String LAST_MODIFIED_KEY = "last_modified_key"; // Ganti dengan user ID yang sesuai
        private static final String TAG = "ModifiedCheckWorker";

        public ModifiedCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                setForegroundAsync(createForegroundInfo("Finding Database Update"));
                String backupFileUrl = getInputData().getString("backup_file_url");

                Log.d(TAG, "Backup file URL: " + backupFileUrl);
                // Ambil lastModified secara sinkron
                String lastModified = getLastModifiedFromUrl();
                if (lastModified == null) {
                    Log.e(TAG, "Failed to get last modified from Firebase.");
                    return Result.failure();
                }
                Log.d(TAG, "Last modified from URL: " + lastModified);
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                String lastModifiedSaved = prefs.getString(LAST_MODIFIED_KEY, "");

                if (lastModifiedSaved.isEmpty() || !lastModified.equals(lastModifiedSaved)) {
                    Intent intent = new Intent("PROGRESS_UPDATE");
                    String pesan = "Updated database found";
                    int progress = 100;
                    intent.putExtra("pesan", pesan);
                    intent.putExtra("progress", progress);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    prefs.edit().putString(LAST_MODIFIED_KEY, lastModified).apply();
                    Log.d(TAG, "Saving new last modified to Pref: " + lastModified);
                    RestoreDatabaseWorker.enqueueWork(getApplicationContext(), getInputData());
                } else {
                    Intent intent = new Intent("PROGRESS_UPDATE");
                    String pesan = "Loading database, please wait ....";
                    int progress = 50;
                    intent.putExtra("pesan", pesan);
                    intent.putExtra("progress", progress);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    isDatabaseCorrupt();

                }

                return Result.success();
            } catch (IOException e) {
                Log.e(TAG, "Error in doWork", e);
                return Result.failure();
            }
        }

        private boolean isDatabaseCorrupt() {
            AppDatabase db = null;
            try {
                // Inisialisasi database Room
                db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos")
                        .allowMainThreadQueries() // Hanya untuk testing, hindari di thread utama dalam produksi
                        .build();

                // Coba melakukan query sederhana pada tabel
                db.indexLinksDao().getAll();
                Log.i(TAG, "DB Aman!");
                // Jika query berhasil, maka database tidak corrupt
                return false;
            } catch (SQLiteException e) {
                if (e instanceof android.database.sqlite.SQLiteDatabaseCorruptException) {
                    Log.e(TAG, "Database is corrupt!", e);
                    RestoreDatabaseWorker.enqueueWork(getApplicationContext(), getInputData());
                    Log.d(TAG, "mendownload ulang");
                    return true; // Database corrupt
                } else {
                    Log.e(TAG, "Database is corrupt!", e);
                    RestoreDatabaseWorker.enqueueWork(getApplicationContext(), getInputData());
                    Log.d(TAG, "mendownload ulang");
                }
            } finally {
                if (db != null) {
                    Intent intent = new Intent("PROGRESS_UPDATE");
                    String pesan = "Loading database, please wait ....";
                    int progress = 100;
                    intent.putExtra("pesan", pesan);
                    intent.putExtra("progress", progress);
                    Uri deepLinkData = Uri.parse(getInputData().getString("deeplink"));
                    // Menggunakan Handler untuk memastikan eksekusi pada thread utama
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Context context = getApplicationContext();
                        // Memastikan bahwa aplikasi berada di latar depan sebelum membuka Activity
                        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                        if (appProcesses != null) {
                            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                                if (appProcess.processName.equals(context.getPackageName()) &&
                                        appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

                                    // Jika aplikasi berada di latar depan, buka MainActivity
                                    Intent intnt = new Intent(context, MainActivity.class);
                                    if (deepLinkData != null) {
                                        intnt.setData(deepLinkData);
                                    }
                                    intnt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intnt);
                                    break;
                                }
                            }
                        }
                    });
                    db.close(); // Tutup koneksi database
                }else{

                    RestoreDatabaseWorker.enqueueWork(getApplicationContext(), getInputData());
                    Log.d(TAG, "mendownload ulang");
                }
            }
            return false;
        }
        private ForegroundInfo createForegroundInfo(String message) {
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), "loading_channel")
                    .setContentTitle("Database Restoration")
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_import_export)
                    .build();
            return new ForegroundInfo(1, notification);
        }

        @Nullable
        private String getLastModifiedFromUrl() throws IOException {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Data").child("nfgdb").child("lastModified");
            final TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String lastMod = snapshot.getValue(String.class);
                        taskCompletionSource.setResult(lastMod);
                    } else {
                        taskCompletionSource.setResult(null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    taskCompletionSource.setException(new IOException("Failed to retrieve last modified date"));
                }
            });

            try {
                Intent intent = new Intent("PROGRESS_UPDATE");
                String pesan = "Finding updated database";
                int progress = 20;
                intent.putExtra("pesan", pesan);
                intent.putExtra("progress", progress);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                // Tunggu hasil dari Firebase (maksimum 5 detik)
                return Tasks.await(taskCompletionSource.getTask(), 5, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                Log.e(TAG, "Error fetching last modified date", e);
                return null;
            }
        }

        public static void enqueueWork(Context context, Data inputData) {
            PeriodicWorkRequest checkRequest = new PeriodicWorkRequest.Builder(ModifiedCheckWorker.class, 12, TimeUnit.HOURS)
                    .setInputData(inputData)
                    .build();
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("ModifiedCheckWork", ExistingPeriodicWorkPolicy.REPLACE, checkRequest);
        }
    }

}
