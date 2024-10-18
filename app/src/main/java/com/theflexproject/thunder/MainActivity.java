package com.theflexproject.thunder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.fragments.HomeFragment;
import com.theflexproject.thunder.fragments.HomeNewFragment;
import com.theflexproject.thunder.fragments.LibraryFragment;
import com.theflexproject.thunder.fragments.SearchFragment;
import com.theflexproject.thunder.fragments.SettingsFragment;
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

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    HomeNewFragment homeFragment = new HomeNewFragment();
    HomeFragment    homeVerif = new HomeFragment();
    SearchFragment searchFragment = new SearchFragment();
    LibraryFragment libraryFragment = new LibraryFragment();
    SettingsFragment settingsFragment = new SettingsFragment();

    BlurView blurView;
    ViewGroup rootView;
    View decorView;
    FirebaseManager firebaseManager;

    public static Context context;

    private FirebaseAnalytics mFirebaseAnalytics;
    private static final int UPDATE_REQUEST_CODE = 123;
    Button scanButton;
    Button seriesButton;
    ProgressBar loadingScan;
    FrameLayout scanContainer;
    static FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        FirebaseApp.initializeApp(this);
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        loadAd();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // Check if the user is signed in
        handleSignIn();
        File dbFile = getApplicationContext().getDatabasePath("MyToDos");
        if (!dbFile.exists()) {
            startActivity(new Intent(MainActivity.this, LoadingActivity.class));
        }

    }

    private void handleSignIn(){
        if (currentUser != null) {
            checkForAppUpdate();
            Intent intent = getIntent();
            Uri data = intent.getData();
            setContentView(R.layout.activity_main);
            initWidgets();
            setUpBottomNavigationView();
            handleDemoUser();
            // Mulai proses restore dengan WorkManager
            MainActivity.RestoreDatabaseWorker.scheduleDailyRestore(this);
            //AppDatabase db = Room.databaseBuilder(getApplicationContext() ,
                           // AppDatabase.class , "MyToDos")
                    //.build();
        }
        else {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));

        }
    }
    private void handleDemoUser() {
        // User is signed in
        currentUser.getUid();
        currentUser.getEmail();
        currentUser.getIdToken(true).toString();
        if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, homeVerif).commit();
        }
        else {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();
        }
    }


    private void loadAd() {
        MyApplication myApplication = (MyApplication) getApplication();
        myApplication.loadAd();
    }



    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStackImmediate();
        else super.onBackPressed();
    }

    private void initWidgets() {
        blurView = findViewById(R.id.blurView);
        decorView = getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        scanContainer = findViewById(R.id.scanContainer);
        loadingScan = findViewById(R.id.loadingScan);
        scanButton = findViewById(R.id.floating_scan);
        seriesButton = findViewById(R.id.scanSeries);
        blurBottom();
    }

    private void setUpBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if(item.getItemId()==R.id.homeFragment){
                if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left, R.anim.to_right)
                            .replace(R.id.container, homeVerif)
                            .commit();
                }
                else{
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left, R.anim.to_right)
                            .replace(R.id.container, homeFragment)
                            .commit();
                }
                return true;
            }else if(item.getItemId()==R.id.searchFragment){
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                        .replace(R.id.container , searchFragment)
                        .commit();
                return true;
            }else if(item.getItemId()==R.id.libraryFragment){
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                        .replace(R.id.container , libraryFragment)
                        .commit();
                return true;
            }else if(item.getItemId()==R.id.settingsFragment){
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                        .replace(R.id.container , settingsFragment)
                        .commit();
                return true;
            }
            return false;
        });

    }

    private void blurBottom() {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS , WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        final float radius = 12f;
        final Drawable windowBackground = getWindow().getDecorView().getBackground();

        blurView.setupWith(rootView , new RenderScriptBlur(this))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);


    }
    private void checkForAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Request the update.
                startUpdateFlow(appUpdateManager, appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateManager appUpdateManager, AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    UPDATE_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // If the update is cancelled or fails, you may want to retry
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

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

        public static void scheduleDailyRestore(Context context) {
            Data inputData = new Data.Builder()
                    .putString("backup_file_url", getBackupFileUrl()) // Pastikan untuk mengubah ini sesuai kebutuhan
                    .build();

            PeriodicWorkRequest restoreRequest = new PeriodicWorkRequest.Builder(MainActivity.RestoreDatabaseWorker.class, 1, TimeUnit.DAYS)
                    .setInputData(inputData)
                    .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS) // Mengatur penundaan awal
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "daily_restore", // Nama unik untuk pekerjaan ini
                    ExistingPeriodicWorkPolicy.KEEP, // Menghindari duplikat pekerjaan
                    restoreRequest
            );

        }
        private static long calculateInitialDelay() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            // Set jam dan menit ke 12 malam
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            // Jika waktu sekarang sudah lewat dari 12 malam, tambahkan satu hari
            if (System.currentTimeMillis() >= calendar.getTimeInMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            return calendar.getTimeInMillis() - System.currentTimeMillis();
        }
    }
}





