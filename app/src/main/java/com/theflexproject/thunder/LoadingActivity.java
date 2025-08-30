package com.theflexproject.thunder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadingActivity extends AppCompatActivity {

    // PERBAIKAN: Pindahkan semua string hardcoded ke konstanta.
    // Ini membuat kode lebih mudah dibaca, diubah, dan dikelola.
    private static final String TAG = LoadingActivity.class.getSimpleName(); // Gunakan nama kelas untuk TAG log
    private static final String LAST_MODIFIED_PREF = "last_modified_pref";
    private static final String LAST_MODIFIED_KEY = "last_modified_key";
    private static final String DB_NAME = "MyToDos";
    private static final String ADMIN_USER_UID = "M20Oxpp64gZ480Lqus4afv6x2n63"; // UID admin
    private static final String DEMO_DB_URL = "https://drive4.nfgplusmirror.workers.dev/0:/database/demo.db";
    private static final String MAIN_DB_URL = "https://drive4.nfgplusmirror.workers.dev/0:/database/nfg.db";
    private static final int REQUEST_MEDIA_PERMISSION = 100;
    private TextView appVersion;

    // PERBAIKAN: Gunakan ExecutorService untuk mengelola background thread.
    // Ini jauh lebih baik daripada `new Thread()` karena memberikan kontrol lebih,
    // dapat dihentikan (shutdown), dan lebih efisien.
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // PERBAIKAN: Hilangkan `static` dari `currentUser` untuk menghindari memory leak.
    private FirebaseUser currentUser;

    private ProgressBar progressBar;
    private TextView loadingMessageTextView;
    private LinearLayout loadingLayout;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        setupWindow();

        // Inisialisasi UI
        progressBar = findViewById(R.id.progress_datar);
        loadingMessageTextView = findViewById(R.id.loading_message);
        loadingLayout = findViewById(R.id.loadingLayout);
        Glide.with(this)
                .load(Constants.background)
                .error(R.drawable.bd)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        loadingLayout.setBackground(resource);
                    }
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Ini akan dipanggil jika `.load()` gagal dan `.error()` telah diatur.
                        // errorDrawable akan menjadi drawable yang Anda berikan di `.error()`.
                        // Di sini kita sudah mengaturnya melalui .error(), jadi cukup set background.
                        loadingLayout.setBackground(errorDrawable);
                        Log.e(TAG, "Failed to load background image from URL, using fallback.");
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Kosongkan jika tidak perlu
                    }
                });

        // PERBAIKAN: Dapatkan currentUser dari instance FirebaseAuth.
        // Hindari penggunaan kelas manager custom jika tidak benar-benar kompleks.
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        appVersion = findViewById(R.id.app_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            appVersion.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            // Tampilkan teks fallback jika terjadi error
            appVersion.setText("");
        }
        // CATATAN: Jika currentUser bisa null, harus ada penanganan di sini.
        // Misalnya, kembali ke layar login.
        if (currentUser == null) {
            // TODO: Arahkan ke LoginActivity atau tampilkan error
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkPermissions();
        subscribeToFcmTopic();

        // Ambil URL backup dan mulai proses pengecekan modifikasi
        String backupFileUrl = getBackupFileUrlForCurrentUser();
        Uri deepLinkData = getIntent().getData();

        checkForDatabaseUpdate(backupFileUrl, deepLinkData);
    }

    private void setupWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void checkPermissions() {
        if (!isNotificationPermissionGranted()) {
            showNotificationPermissionDialog();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestMediaPermissions();
        }
    }

    private void subscribeToFcmTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("latest_update")
                .addOnCompleteListener(task -> {
                    String msg = task.isSuccessful() ? "Successfully subscribed to topic" : "Failed to subscribe to topic";
                    Log.d(TAG, msg);
                });
    }

    private String getBackupFileUrlForCurrentUser() {
        // PERBAIKAN: Gunakan konstanta untuk perbandingan UID.
        if (ADMIN_USER_UID.equals(currentUser.getUid())) {
            return DEMO_DB_URL;
        }
        return MAIN_DB_URL;
    }

    /**
     * Memulai alur utama untuk memeriksa pembaruan database di background thread.
     */
    private void checkForDatabaseUpdate(String backupFileUrl, Uri deepLinkData) {
        executorService.execute(() -> {
            try {
                // 1. Dapatkan timestamp 'last modified' dari Firebase
                String remoteLastModified = fetchLastModifiedFromFirebase();

                // 2. Dapatkan timestamp yang tersimpan secara lokal
                SharedPreferences prefs = getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
                String localLastModified = prefs.getString(LAST_MODIFIED_KEY, "");

                // 3. Bandingkan timestamp
                if (localLastModified.isEmpty() || !remoteLastModified.equals(localLastModified)) {
                    // Jika berbeda atau baru pertama kali, unduh database baru
                    updateProgressOnMainThread(10, "New database version found. Updating...");
                    downloadAndRestoreDatabase(backupFileUrl, remoteLastModified, deepLinkData);
                } else {
                    // Jika sama, periksa apakah database lokal korup
                    updateProgressOnMainThread(50, "Loading database, please wait...");
                    if (!isDatabaseCorrupt()) {
                        // Jika aman, langsung ke MainActivity
                        updateProgressOnMainThread(100, "Enjoy!");
                        launchMainActivity(deepLinkData);
                    } else {
                        // Jika korup, paksa unduh ulang
                        updateProgressOnMainThread(75, "Database corrupted. Recovering...");
                        downloadAndRestoreDatabase(backupFileUrl, remoteLastModified, deepLinkData);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to check for database update.", e);
                updateProgressOnMainThread(100, "Error: Could not connect to update server.");
                // CATATAN: Di sini bisa ditambahkan logika retry atau menampilkan dialog error.
            }
        });
    }

    /**
     * Mengunduh dan merestore database.
     */
    private void downloadAndRestoreDatabase(String fileUrl, String newLastModified, Uri deepLinkData) {
        try {
            File downloadedFile = downloadFile(fileUrl);
            restoreDatabase(downloadedFile);

            // Simpan timestamp baru setelah berhasil restore
            SharedPreferences prefs = getSharedPreferences(LAST_MODIFIED_PREF, Context.MODE_PRIVATE);
            prefs.edit().putString(LAST_MODIFIED_KEY, newLastModified).apply();

            updateProgressOnMainThread(100, "Database updated successfully!");
            launchMainActivity(deepLinkData);
        } catch (IOException e) {
            Log.e(TAG, "Failed to download or restore database.", e);
            updateProgressOnMainThread(100, "Error: Update failed. Check Your Connection.");
            // PERBAIKAN: Hindari infinite loop. Tampilkan pesan error dan berhenti.
            // Jika ingin retry, gunakan mekanisme yang lebih terkontrol (misal, dengan delay).
        }
    }

    private File downloadFile(String fileUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File localFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "temp_db.db");

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            int fileLength = connection.getContentLength();
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(localFile);

            byte[] buffer = new byte[4096]; // PERBAIKAN: Buffer lebih besar untuk performa I/O
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (fileLength > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileLength);
                    updateProgressOnMainThread(progress, "Downloading update... " + progress + "%");
                }
            }
            return localFile;
        } finally {
            // PERBAIKAN: Pastikan semua stream ditutup dengan benar
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        }
    }

    private void restoreDatabase(File downloadedFile) throws IOException {
        File dbFile = getDatabasePath(DB_NAME);
        Log.i(TAG, "Restoring database to: " + dbFile.getAbsolutePath());

        // Hapus cache dan file database lama
        deleteDir(getCacheDir());
        if (dbFile.exists()) {
            dbFile.delete();
        }
        dbFile.getParentFile().mkdirs();

        // PERBAIKAN: Gunakan try-with-resources untuk menyalin file, lebih aman dan bersih.
        try (InputStream is = new FileInputStream(downloadedFile);
             OutputStream os = new FileOutputStream(dbFile)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            // Hapus file sementara setelah selesai
            downloadedFile.delete();
        }
        Log.d(TAG, "Input size: " + downloadedFile.length() + ", Output size: " + dbFile.length());
    }

    private String fetchLastModifiedFromFirebase() throws IOException {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Data").child("nfgdb").child("lastModified");
        try {
            // PERBAIKAN: Menggunakan GMS Tasks API secara langsung untuk kode yang lebih sinkron dan bersih.
            Task<DataSnapshot> task = ref.get();
            DataSnapshot dataSnapshot = Tasks.await(task, 15, TimeUnit.SECONDS); // Timeout 15 detik
            if (dataSnapshot.exists()) {
                String lastModified = dataSnapshot.getValue(String.class);
                if (lastModified != null) {
                    return lastModified;
                }
            }
            throw new IOException("Last modified value is null or does not exist in Firebase.");
        } catch (Exception e) {
            throw new IOException("Failed to fetch last modified value from Firebase.", e);
        }
    }

    private boolean isDatabaseCorrupt() {
        File dbFile = getDatabasePath(DB_NAME);

        // Jika file tidak ada atau ukurannya 0, anggap korup/perlu diunduh.
        if (!dbFile.exists() || dbFile.length() == 0) {
            Log.w(TAG, "Database file does not exist or is empty.");
            return true;
        }

        AppDatabase db = null;
        try {
            // PERBAIKAN: Jangan gunakan allowMainThreadQueries().
            // Karena ini sudah di dalam ExecutorService, kita aman.
            db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, DB_NAME).build();
            // Coba lakukan operasi baca sederhana untuk memvalidasi database.
            db.indexLinksDao().getAll(); // Asumsi ada DAO bernama indexLinksDao
            Log.i(TAG, "Database integrity check passed.");
            return false;
        } catch (SQLiteException e) {
            // PERBAIKAN: Tangkap semua SQLiteException sebagai indikasi korupsi/masalah.
            Log.e(TAG, "Database is considered corrupt!", e);
            return true;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    private void launchMainActivity(Uri deepLinkData) {
        mainThreadHandler.post(() -> {
            // Cek jika activity masih berjalan untuk menghindari crash
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Intent intent = new Intent(this, MainActivity.class);
            if (deepLinkData != null) {
                intent.setData(deepLinkData);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Tutup LoadingActivity
        });
    }

    private void updateProgressOnMainThread(int progress, String message) {
        mainThreadHandler.post(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
            if (loadingMessageTextView != null) {
                loadingMessageTextView.setText(message);
            }
        });
    }

    // --- Metode utilitas dan izin (sebagian besar tidak berubah, hanya dirapikan) ---

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    if (!deleteDir(new File(dir, child))) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // PERBAIKAN: Matikan ExecutorService saat Activity dihancurkan.
        // Ini penting untuk menghentikan thread dan mencegah memory leak.
        executorService.shutdownNow();
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // Untuk versi di bawah Tiramisu, izin notifikasi diberikan secara default.
        return true;
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission")
                .setMessage("To receive updates and alerts, please allow notification access in the settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkAndRequestMediaPermissions() {
        String[] permissions = {
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
        };
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_MEDIA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Media permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Media permissions are required for some features.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
