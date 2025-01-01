package com.theflexproject.thunder;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
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
    private static final String TAG = "huntu";
    private TextView pesan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        FirebaseMessaging.getInstance().subscribeToTopic("latest_update")
                .addOnCompleteListener(task -> {
                    String msg = "Berhasil berlangganan topik";
                    if (!task.isSuccessful()) {
                        msg = "Gagal berlangganan topik";
                    }
                    Log.d(TAG, msg);
                });
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        progressBar = findViewById(R.id.progress_datar);
        pesan = findViewById(R.id.loading_message);
        // Pastikan ada ProgressBar di layout Anda

        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("PROGRESS_UPDATE"));

        // Ambil URL backup dan mulai proses pengecekan modifikasi
        String backupFileUrl = getBackupFileUrl();
        Uri deepLinkData = getIntent().getData();

        checkForModifiedData(backupFileUrl, deepLinkData);


    }

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
    }

    private static String getBackupFileUrl() {
        if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
            return "https://drive3.nfgplusmirror.workers.dev/0:/database/demo.db";
        }
        return "https://drive3.nfgplusmirror.workers.dev/0:/database/nfg.db";
    }

    private void checkForModifiedData(String backupFileUrl, Uri deepLinkData) {
        new Thread(() -> {
            try {
                String lastModified = getLastModifiedFromUrl();
                if (lastModified == null) {
                    Log.e(TAG, "Failed to get last modified from Firebase.");
                    return;
                }
                SharedPreferences prefs = getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                String lastModifiedSaved = prefs.getString(LAST_MODIFIED_KEY, "");

                if (lastModifiedSaved.isEmpty() || !lastModified.equals(lastModifiedSaved)) {
                    runOnUiThread(() -> updateProgressBar(100, "Updated database found"));
                    prefs.edit().putString(LAST_MODIFIED_KEY, lastModified).apply();
                    restoreDatabaseFromUrl(backupFileUrl, deepLinkData);
                } else {
                    runOnUiThread(() -> updateProgressBar(50, "Loading database, please wait ...."));
                    if (!isDatabaseCorrupt(backupFileUrl, deepLinkData)) {
                        runOnUiThread(() -> updateProgressBar(100, "Enjoy"));
                        launchMainActivity(deepLinkData);
                    }else {
                        runOnUiThread(() -> updateProgressBar(100, "Database corrupted, Recovering database...."));
                        prefs.edit().putString(LAST_MODIFIED_KEY, lastModified).apply();
                        restoreDatabaseFromUrl(backupFileUrl, deepLinkData);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error in checkForModifiedData", e);
            }
        }).start();
    }

    private void restoreDatabaseFromUrl(String fileUrl, Uri deepLinkData) {
        new Thread(() -> {
            try {
                File downloadedFile = downloadFileFromUrl(fileUrl);
                if (downloadedFile != null) {
                    restoreDatabase(downloadedFile, deepLinkData);
                }else {
                    SharedPreferences prefs = getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);

                    prefs.edit().putString(LAST_MODIFIED_KEY, "").apply();
                    checkForModifiedData(fileUrl, deepLinkData);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error in restoreDatabaseFromUrl", e);
            }
        }).start();
    }

    private File downloadFileFromUrl(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download file: " + connection.getResponseMessage());
        }

        File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "demo.db");
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = new FileOutputStream(localFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytesRead = 0;
            int fileLength = connection.getContentLength();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                int progress = (int) ((totalBytesRead / (float) fileLength) * 100);
                updateProgressBar(progress, "Updating database, do not close the app .... " + progress + "%");
            }
        } catch (IOException e) {
            localFile.delete();
            throw e;
        } finally {
            connection.disconnect();
        }
        return localFile;
    }

    private void restoreDatabase(File file, Uri deepLinkData) throws IOException {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos").build();
        db.close();
        File dbFile = getDatabasePath("MyToDos");

        if (dbFile.exists()) {
            dbFile.delete();
        }

        if (!dbFile.exists()) {
            File cacheDir = getCacheDir();
            deleteDir(cacheDir);
            dbFile.getParentFile().mkdirs();
            dbFile.createNewFile();
        }

        File dbDir = getDatabasePath("MyToDos").getParentFile();
        long freeSpace = dbDir.getFreeSpace();
        Log.d("Storage", "Available space: " + freeSpace + " bytes");

        new Thread(() -> {
            try (InputStream is = new FileInputStream(file);
                 OutputStream os = new FileOutputStream(dbFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                long inputFileSize = file.length();
                long outputFileSize = dbFile.length();

                Log.d("FileSize", "Input size: " + inputFileSize + ", Output size: " + outputFileSize);

                file.delete();
                Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos").build();
                launchMainActivity(deepLinkData);
            }
        }).start();
    }
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete(); // Hapus file atau direktori kosong
    }
    private void launchMainActivity(Uri deepLinkData) {
        HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.processName.equals(getPackageName()) &&
                            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        Intent intent = new Intent(this, MainActivity.class);
                        if (deepLinkData != null) {
                            intent.setData(deepLinkData);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    }
                }
            }
        });
    }

    private void updateProgressBar(int progress, String message) {
        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            pesan.setText(message);
        });
    }

    private boolean isDatabaseCorrupt(String backupFileUrl, Uri deepLinkData) {
        AppDatabase db = null;
        File dbFile = getDatabasePath("MyToDos");

        // Periksa jika file tidak ada
        if (!dbFile.exists()) {
            Log.e(TAG, "Database does not exist.");
            return true;
        } else {
            // Periksa jika ukuran database adalah 0MB (0 bytes)
            if (dbFile.length() == 0) {
                Log.e(TAG, "Database is empty (0MB).");
                return true;
            }

            try {
                db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "MyToDos")
                        .allowMainThreadQueries()
                        .build();

                // Cek apakah database dapat diakses
                db.indexLinksDao().getAll();
                Log.i(TAG, "DB Aman!");
                return false;
            } catch (SQLiteException e) {
                if (e instanceof android.database.sqlite.SQLiteDatabaseCorruptException) {
                    Log.e(TAG, "Database is corrupt!", e);
                    return true;
                } else {
                    Log.e(TAG, "Database is corrupt!", e);
                }
            } finally {
                if (db != null) {
                    db.close();
                }
            }
            return false;
        }
    }
    @NonNull
    private String getLastModifiedFromUrl() throws IOException {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Data").child("nfgdb").child("lastModified");

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String lastModified = snapshot.getValue(String.class);
                if (lastModified != null) {
                    taskCompletionSource.setResult(lastModified);
                } else {
                    taskCompletionSource.setException(new IOException("Last modified value is null"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                taskCompletionSource.setException(error.toException());
            }
        };
        ref.addListenerForSingleValueEvent(valueEventListener);

        try {
            return Tasks.await(taskCompletionSource.getTask(), 10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException("Failed to get last modified value from Firebase", e);
        }
    }
}
