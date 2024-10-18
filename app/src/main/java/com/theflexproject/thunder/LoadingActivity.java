package com.theflexproject.thunder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.model.FirebaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class LoadingActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private static FirebaseUser currentUser;
    private ViewGroup rootView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        progressBar = findViewById(R.id.progress_datar); // Pastikan ada ProgressBar di layout Anda

        // Daftarkan receiver untuk menerima pembaruan progress
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("PROGRESS_UPDATE"));

        // Ambil URL backup dan masukkan ke dalam WorkManager
        String backupFileUrl = getBackupFileUrl();

        // Kirim URL melalui InputData ke Worker
        Data inputData = new Data.Builder()
                .putString("backup_file_url", backupFileUrl)
                .build();

        // Mulai proses restore dengan WorkManager
        LoadingActivity.RestoreDatabaseWorker.enqueueWork(this, inputData);
    }

    // Receiver untuk menerima pembaruan progress
    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            progressBar.setProgress(progress);
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
                // Ambil URL dari InputData
                String backupFileUrl = getInputData().getString("backup_file_url");

                // Download file dari URL dan restore database
                File downloadedFile = downloadFileFromUrl(backupFileUrl);

                if (downloadedFile != null) {
                    restoreDatabase(downloadedFile);
                    launchMainActivity();
                    return Result.success();
                } else {
                    return Result.failure();
                }
            } catch (IOException e) {
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
            }

            connection.disconnect();
            return localFile;
        }

        // Method untuk restore database dari file lokal
        private void restoreDatabase(File file) throws IOException {
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
        }

        private void launchMainActivity() {
            // Gunakan Handler untuk beralih ke UI thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Context context = getApplicationContext();
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
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
            intent.putExtra("progress", progress);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
