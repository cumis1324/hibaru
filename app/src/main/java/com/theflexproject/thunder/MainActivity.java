package com.theflexproject.thunder;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
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
import com.theflexproject.thunder.database.AppDatabase;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.fragments.DownloadFragment;
import com.theflexproject.thunder.fragments.HomeFragment;
import com.theflexproject.thunder.fragments.HomeNewFragment;
import com.theflexproject.thunder.fragments.LibraryFragment;
import com.theflexproject.thunder.fragments.MovieDetailsFragment;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
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
    DownloadFragment downloadFragment = new DownloadFragment();

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
    AppDatabase dbs;

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
        if (!isNotificationEnabled()) {
            showNotificationPermissionDialog();
        } else {
            Toast.makeText(this, "Notifikasi diizinkan!", Toast.LENGTH_SHORT).show();
        }
        // Daftarkan receiver untuk menerima pembaruan dari ModifiedCheckWorker


    }
    private boolean isNotificationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return notificationManager != null && notificationManager.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return true; // Di bawah Android O, dianggap sudah diizinkan
    }

    private void showNotificationPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Izinkan Notifikasi")
                .setMessage("Kami membutuhkan izin Anda untuk mengirim notifikasi.")
                .setPositiveButton("Izinkan", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Arahkan pengguna ke pengaturan aplikasi untuk mengizinkan notifikasi
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Tolak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Menutup dialog
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleSignIn(){
        if (currentUser != null) {
            dbs = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
            checkForAppUpdate();

            setContentView(R.layout.activity_main);
            initWidgets();
            setUpBottomNavigationView();
            handleDemoUser();

            // Mulai proses restore dengan WorkManager
            //AppDatabase db = Room.databaseBuilder(getApplicationContext() ,
                           // AppDatabase.class , "MyToDos")
                    //.build();

        }
        else {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));

        }
    }

    private void bukaIntent() {
        if (getIntent().getData() != null) {
            Uri data = getIntent().getData();
            String itemId = data.getQueryParameter("id");
            String itemType = data.getQueryParameter("type");

            if (itemId != null && itemType != null) {
                openFragmentBasedOnType(itemId, itemType);
            }else {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();
            }
        }else {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFragment).commit();
        }
    }

    private void openFragmentBasedOnType(String itemId, String itemType) {
        if (Objects.equals(itemType, "movie")){
            int id = Integer.parseInt(itemId);
            MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(id);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
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
            bukaIntent();

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
            }else if(item.getItemId()==R.id.downloadFragment){
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                        .replace(R.id.container , downloadFragment)
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



    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseClient.getInstance(getApplicationContext()).closeDatabase();
    }
}





