package com.theflexproject.thunder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.database.AppDatabase;
import com.theflexproject.thunder.model.FirebaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
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
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        progressBar = findViewById(R.id.progress_datar);
        // Pastikan ada ProgressBar di layout Anda

        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("PROGRESS_UPDATE"));

        // Ambil URL backup dan masukkan ke dalam WorkManager
        String backupFileUrl = getBackupFileUrl();

        // Kirim URL melalui InputData ke Worker
        Data inputData = new Data.Builder()
                .putString("backup_file_url", backupFileUrl)
                .build();

        // Mulai pengecekan modifikasi dengan WorkManager
        ModifiedCheckWorker.enqueueWork(this, inputData);


    }

    // Receiver untuk menerima pembaruan progress
    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            progressBar.setProgress(progress);
        }
    };

    public void loginDulu(){
        if (currentUser != null) {
            // Daftarkan receiver untuk menerima pembaruan progress


        }else {
            startActivity(new Intent(LoadingActivity.this, SignInActivity.class));
        }
    }

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

    public static class ModifiedCheckWorker extends Worker {

        private static final String LAST_MODIFIED_KEY = "last_modified_key";
        private static final String USER_ID = currentUser.getUid(); // Ganti dengan user ID yang sesuai
        private static final String TAG = "ModifiedCheckWorker";

        public ModifiedCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                // Ambil URL dari InputData
                String backupFileUrl = getInputData().getString("backup_file_url");
                Log.d(TAG, "Backup file URL: " + backupFileUrl);

                // Cek Last-Modified header
                String lastModified = getLastModifiedFromUrl(backupFileUrl);
                Log.d(TAG, "Last modified from URL: " + lastModified);

                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Data").child(USER_ID).child("lastModified");

                // Menggunakan CountDownLatch untuk menunggu pengambilan nilai dari Firebase selesai
                final CountDownLatch latch = new CountDownLatch(1);
                final String[] lastModifiedSaved = {""};

                databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            lastModifiedSaved[0] = snapshot.getValue(String.class);
                            Log.d(TAG, "Last modified saved in Firebase: " + lastModifiedSaved[0]);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read last modified from Firebase", error.toException());
                        latch.countDown();
                    }
                });

                // Tunggu hingga pengambilan nilai selesai
                latch.await();

                // Jika lastModifiedSaved kosong, artinya aplikasi baru diinstall atau tidak pernah menyimpan Last-Modified sebelumnya
                if (lastModifiedSaved[0].isEmpty() || !lastModified.equals(lastModifiedSaved[0])) {
                    // Last-Modified berubah atau pertama kali dijalankan, simpan nilai Last-Modified baru
                    Log.d(TAG, "Saving new last modified to Firebase: " + lastModified);
                    databaseRef.setValue(lastModified);

                    // Panggil RestoreDatabaseWorker
                    RestoreDatabaseWorker.enqueueWork(getApplicationContext(), getInputData());
                }

                return Result.success();
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "Error in doWork", e);
                return Result.failure();
            }
        }

        // Method untuk mendapatkan Last-Modified header dari URL
        @NonNull
        private String getLastModifiedFromUrl(String fileUrl) throws IOException {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // Tambahkan log untuk memeriksa status koneksi
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            // Jika response OK, ambil header
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String eTag = connection.getHeaderField("ETag");
                String contentType = connection.getHeaderField("Content-Type");
                String contentLength = connection.getHeaderField("Content-Length");
                String lastModified = connection.getHeaderField("Last-Modified");

                // Logging metadata yang diperoleh
                Log.d(TAG, "ETag: " + eTag);
                Log.d(TAG, "Content-Type: " + contentType);
                Log.d(TAG, "Content-Length: " + contentLength);
                Log.d(TAG, "Last-Modified: " + lastModified);

                // Anda bisa menyimpan atau mengembalikan data ini sesuai kebutuhan
                // Misalnya, bisa mengembalikan ETag jika diperlukan
                return lastModified != null ? lastModified : "";
            } else {
                Log.d(TAG, "Failed to fetch metadata: " + responseCode);
                return ""; // Kembalikan string kosong jika permintaan gagal
            }
        }


        public static void enqueueWork(Context context, Data inputData) {
            // Penjadwalan pengecekan modifikasi menggunakan WorkManager dengan InputData
            PeriodicWorkRequest checkRequest = new PeriodicWorkRequest.Builder(ModifiedCheckWorker.class, 12, TimeUnit.HOURS)
                    .setInputData(inputData)
                    .build();
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("ModifiedCheckWork", ExistingPeriodicWorkPolicy.REPLACE, checkRequest);
        }
    }
}
