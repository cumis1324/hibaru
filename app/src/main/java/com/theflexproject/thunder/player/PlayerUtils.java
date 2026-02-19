package com.theflexproject.thunder.player;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.theflexproject.thunder.player.PlayerListener.fastForward;
import static com.theflexproject.thunder.player.PlayerListener.rewind;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.PlayerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.DownloadItem;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.utils.AdHelper;
import com.theflexproject.thunder.utils.LanguageUtils;
import com.theflexproject.thunder.utils.MovieQualityExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PlayerUtils {
    static final String HISTORY_PATH = "History/", LAST_POSITION = "lastPosition", OFFLINE = "offline";
    static FirebaseManager manager = new FirebaseManager();
    static DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(HISTORY_PATH);
    static String userId = manager.getCurrentUser().getUid();
    public static ValueEventListener lastPositionListener;
    private static boolean adStart;
    private static boolean ad25;
    private static boolean ad50;
    private static boolean ad75;
    private static List<DownloadItem> downloadItems = new ArrayList<>();
    private static View decorView;
    private static ViewGroup rootView;

    public static void enterFullscreen(Activity mActivity, FrameLayout playerFrame, TextView movietitle,
            ImageButton fullscreen) {
        int uiOptions;
        decorView = mActivity.getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);
        rootView.setPadding(0, 0, 0, 0);

        if (!isTVDevice(mActivity)) {
            // Safe alternative to MATCH_PARENT for mobile
            // Setting it to screen height ensures it fills the screen in landscape
            playerFrame.post(() -> {
                ViewGroup.LayoutParams params = playerFrame.getLayoutParams();
                // Use display height to avoid 'infinite' constraints in Compose
                android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                params.height = displayMetrics.heightPixels;
                playerFrame.setLayoutParams(params);
            });
        }

        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
        movietitle.setVisibility(View.VISIBLE);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        fullscreen.setImageResource(R.drawable.ic_exit_fullscreen);
    }

    public static void exitFullscreen(Activity mActivity, FrameLayout playerFrame, TextView movietitle,
            ImageButton fullscreen) {
        decorView = mActivity.getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);
        // Remove padding to prevent black bar at top and spacing issues
        rootView.setPadding(0, 0, 0, 0);
        rootView.setBackgroundColor(Color.BLACK);
        movietitle.setVisibility(View.GONE);
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        // Set height kembali ke 250dp
        playerFrame.post(() -> {
            int width = playerFrame.getWidth(); // Lebar FrameLayout
            int height = (int) (width / 16.0 * 9.0); // Hitung tinggi sesuai rasio 16:9
            ViewGroup.LayoutParams params = playerFrame.getLayoutParams();
            params.height = height;
            playerFrame.setLayoutParams(params);
        });

        decorView.setSystemUiVisibility(uiOptions);

        if (!isTVDevice(mActivity)) {
            // Terapkan logika orientasi untuk perangkat non-TV
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        fullscreen.setImageResource(R.drawable.ic_fullscreen);
    }

    public static boolean isTVDevice(Activity mActivity) {
        UiModeManager uiModeManager = (UiModeManager) mActivity.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static boolean subOn(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.Parameters builders = trackSelector.getParameters();
        return builders.getRendererDisabled(C.TRACK_TYPE_VIDEO);
    }

    public static void resumePlayerState(Player player, String tmdbId) {
        if (tmdbId != null && !OFFLINE.equals(tmdbId)) {
            DatabaseReference lastPositionRef = databaseReference.child(userId).child(tmdbId).child(LAST_POSITION);
            lastPositionListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Long lastPosition = snapshot.getValue(Long.class);
                        if (lastPosition != null && player != null) {
                            player.seekTo(lastPosition);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            lastPositionRef.addListenerForSingleValueEvent(lastPositionListener);
        }
    }

    public static void saveResume(Player player, String tmdbId) {
        long startPosition = Math.max(0, player.getContentPosition());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
        if (tmdbId != null) {
            if (!tmdbId.equals(OFFLINE)) {
                DatabaseReference userReference = databaseReference.child(userId).child(tmdbId);
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("lastPosition", startPosition);
                userMap.put("lastPlayed", currentDateTime);
                userReference.setValue(userMap);
            }
        }
    }

    private static int getStatusBarHeight(Activity mActivity) {
        int result = 0;
        int resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = mActivity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @SuppressLint("SetTextI18n")
    public static void updateTimer(TextView timer, long currentPosition, long duration) {
        String current = formatTime(currentPosition);
        String total = formatTime(duration);
        timer.setText(current + " / " + total);
    }

    @SuppressLint("DefaultLocale")
    static String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static MediaSource.Factory createMediaSourceFactory(Context mActivity) {
        DataSource.Factory dataSourceFactory = DemoUtil.getDataSourceFactory(mActivity);
        DefaultDrmSessionManagerProvider drmProvider = new DefaultDrmSessionManagerProvider();
        drmProvider.setDrmHttpDataSourceFactory(DemoUtil.getHttpDataSourceFactory(mActivity));
        return new DefaultMediaSourceFactory(mActivity)
                .setDataSourceFactory(dataSourceFactory)
                .setDrmSessionManagerProvider(drmProvider);
    }

    public static void load3ads(Context mCtx, Activity activity, Player player, PlayerView playerView) {
        SharedPreferences prefs = mCtx.getSharedPreferences("load4Ads", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        ad25 = prefs.getBoolean("ad25", false);
        ad50 = prefs.getBoolean("ad50", false);
        ad75 = prefs.getBoolean("ad75", false);
        if (player != null) {
            long cp = player.getCurrentPosition();
            long tp = player.getDuration();
            if (tp > 0) {
                if (!ad25 && cp >= tp * 0.25 && adStart) {
                    AdHelper.loadReward(mCtx, activity, player, playerView);
                    editor.putBoolean("ad25", true);
                    editor.apply();
                }
                if (!ad50 && cp >= tp * 0.50 && ad25) {
                    AdHelper.loadReward(mCtx, activity, player, playerView);
                    editor.putBoolean("ad50", true);
                    editor.apply();
                }
                if (!ad75 && cp >= tp * 0.75 && ad50) {
                    AdHelper.loadReward(mCtx, activity, player, playerView);
                    editor.putBoolean("ad75", true);
                    editor.apply();
                }
            }
        }
    }

    public static void download(Context mActivity, List<MyMedia> sourceList, TVShow tvshow,
            TVShowSeasonDetails season) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Select File to Download");
        List<String> sourcesOptions = new ArrayList<>();
        for (MyMedia source : sourceList) {
            if (source instanceof Movie) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Movie) source).getFileName());
                sourcesOptions.add(qualityStr);
            } else if (source instanceof Episode) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Episode) source).getFileName());
                sourcesOptions.add(qualityStr);
            } else {
                sourcesOptions.add("Unknown Source");
            }
        }
        final int[] selectedIndex = { 0 };
        builder.setSingleChoiceItems(sourcesOptions.toArray(new String[0]), selectedIndex[0], (dialog, which) -> {
            selectedIndex[0] = which;
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            MyMedia selectedSource = sourceList.get(selectedIndex[0]);
            String customFolderPath = "nfgplus/downloads";
            File privateFolder = new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    customFolderPath);
            if (!privateFolder.exists())
                privateFolder.mkdirs();

            DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);

            if (selectedSource instanceof Movie) {
                Movie selectedFile = (Movie) selectedSource;
                String quality = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());

                try {
                    String finalUrl = selectedFile.getUrlString();
                    if (finalUrl != null) {
                        finalUrl = finalUrl.replace(" ", "%20");
                    }
                    Uri uri = Uri.parse(finalUrl);
                    DownloadManager.Request request = new DownloadManager.Request(uri);

                    File publicFolder = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                            customFolderPath);
                    File destFile = new File(publicFolder, selectedFile.getFileName());

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                            customFolderPath + "/" + selectedFile.getFileName());

                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            .setTitle(selectedFile.getTitle())
                            .setVisibleInDownloadsUi(true)
                            .setDescription("Downloading " + selectedFile.getTitle() + " " + quality);

                    long downloadId = manager.enqueue(request);

                    // Save to local database
                    AsyncTask.execute(() -> {
                        selectedFile.setDownloadId(downloadId);
                        selectedFile.setLocalPath(destFile.getAbsolutePath());
                        com.theflexproject.thunder.database.DatabaseClient.getInstance(mActivity)
                                .getAppDatabase().movieDao().insert(selectedFile);
                    });

                    Toast.makeText(mActivity, "Download started: " + selectedFile.getTitle(), Toast.LENGTH_SHORT)
                            .show();
                } catch (Exception e) {
                    Log.e("PlayerUtils", "Enqueue Error (Movie): " + e.getMessage(), e);
                    Toast.makeText(mActivity, "Error starting download: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            } else if (selectedSource instanceof Episode) {
                Episode selectedFile = (Episode) selectedSource;
                String quality = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());

                try {
                    String finalUrl = selectedFile.getUrlString();
                    if (finalUrl != null) {
                        finalUrl = finalUrl.replace(" ", "%20");
                    }
                    Uri uri = Uri.parse(finalUrl);
                    DownloadManager.Request request = new DownloadManager.Request(uri);

                    File publicFolder = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                            customFolderPath);
                    File destFile = new File(publicFolder, selectedFile.getFileName());

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                            customFolderPath + "/" + selectedFile.getFileName());

                    String title = tvshow.getName() + " S" + season.getSeasonNumber() + " E"
                            + selectedFile.getEpisodeNumber() + ": " + selectedFile.getName();

                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            .setTitle(title)
                            .setVisibleInDownloadsUi(true)
                            .setDescription("Downloading " + title + " " + quality);

                    long downloadId = manager.enqueue(request);

                    // Save to local database
                    AsyncTask.execute(() -> {
                        selectedFile.setDownloadId(downloadId);
                        selectedFile.setLocalPath(destFile.getAbsolutePath());
                        com.theflexproject.thunder.database.DatabaseClient.getInstance(mActivity)
                                .getAppDatabase().episodeDao().insert(selectedFile);
                    });

                    Toast.makeText(mActivity, "Download started: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("PlayerUtils", "Enqueue Error (Episode): " + e.getMessage(), e);
                    Toast.makeText(mActivity, "Error starting download: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.create().show();
    }

    public static void share(Context mActivity, Activity activity, List<MyMedia> sourceList, TVShow tvShow,
            TVShowSeasonDetails season) {
        for (MyMedia source : sourceList) {
            if (source instanceof Movie) {
                Movie movieDetails = (Movie) source;
                String title = movieDetails.getTitle();
                String originalTitle = movieDetails.getOriginalTitle();
                String date = movieDetails.getReleaseDate();
                String year = date.substring(0, date.indexOf('-'));
                String overview = movieDetails.getOverview();
                String posterPath = "https://image.tmdb.org/t/p/w500" + movieDetails.getPosterPath();
                String movieId = String.valueOf(movieDetails.getId());
                String deepLink = "https://nfgplus.my.id/reviews.html?id=" + movieId + "&type=movie";
                String shareText = title + " (" + year + ")\n" + "\n" +
                        "Judul Asli: " + originalTitle + "\n" +
                        "Deskripsi: " + overview + "\n" +
                        deepLink + "\n";
                new Thread(() -> {
                    try {
                        // Mengunduh gambar dari URL
                        URL url = new URL(posterPath);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);

                        // Menyimpan gambar ke penyimpanan lokal
                        File file = new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "shared_image.jpg");
                        FileOutputStream outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();

                        // Dapatkan URI lokal menggunakan FileProvider
                        Uri imageUri = FileProvider.getUriForFile(mActivity,
                                mActivity.getPackageName() + ".fileprovider", file);

                        // Buat intent untuk berbagi teks dan gambar
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                        shareIntent.setType("image/*");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Menjalankan intent di UI thread
                        activity.runOnUiThread(
                                () -> mActivity.startActivity(Intent.createChooser(shareIntent, "Bagikan " + title)));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else if (source instanceof Episode) {
                Episode episode = (Episode) source;
                String title = tvShow.getName() + ": Season " + season.getSeasonNumber() + " Episode "
                        + episode.getEpisodeNumber(); // Ganti dengan movieDetails.getTitle()
                String originalTitle = episode.getName(); // Ganti dengan movieDetails.getOriginalTitle()
                String overview = tvShow.getOverview(); // Ganti dengan movieDetails.getOverview()
                String posterPath = "https://image.tmdb.org/t/p/w500/" + tvShow.getPosterPath();
                String movieId = String.valueOf(tvShow.getId()); // Ganti dengan movieDetails.getId()

                // Tautan deep link lengkap
                String deepLink = "https://nfgplus.my.id/reviews.html?id=" + movieId + "&type=tv";

                // Menyusun teks yang ingin dibagikan
                String shareText = title + "\n" +
                        "Judul Asli: " + originalTitle + "\n" +
                        "Deskripsi: " + overview + "\n" +
                        deepLink + "\n";

                new Thread(() -> {
                    try {
                        // Mengunduh gambar dari URL
                        URL url = new URL(posterPath);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);

                        // Menyimpan gambar ke penyimpanan lokal
                        File file = new File(mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "shared_image.jpg");
                        FileOutputStream outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();

                        // Dapatkan URI lokal menggunakan FileProvider
                        Uri imageUri = FileProvider.getUriForFile(mActivity,
                                mActivity.getPackageName() + ".fileprovider", file);

                        // Buat intent untuk berbagi teks dan gambar
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                        shareIntent.setType("image/*");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Menjalankan intent di UI thread
                        activity.runOnUiThread(
                                () -> mActivity.startActivity(Intent.createChooser(shareIntent, "Bagikan " + title)));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

            }
        }

    }

    public static void watchlist(Context mActivity, List<MyMedia> sourceList, TVShow tvShowDetails,
            TVShowSeasonDetails season) {
        for (MyMedia source : sourceList) {
            if (source instanceof Movie) {
                Movie movieDetails = (Movie) source;
                String tmdbId = String.valueOf(movieDetails.getId());
                String userId = manager.getCurrentUser().getUid();
                DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Favorit").child(userId)
                        .child(tmdbId);
                DatabaseReference value = userReference.child("value");
                value.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("value", 1);
                            userReference.setValue(userMap);

                            Toast.makeText(mActivity, "Added To List", Toast.LENGTH_LONG).show();

                        } else {
                            userReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        System.out.println("Favorit dihapus");
                                    } else {
                                        System.out.println("Favorit dihapus");
                                    }
                                }
                            });

                            Toast.makeText(mActivity, "Removed From List", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            } else if (source instanceof Episode) {
                String tmdbId = String.valueOf(tvShowDetails.getId());
                String userId = manager.getCurrentUser().getUid();
                DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Favorit").child(userId)
                        .child(tmdbId);
                DatabaseReference value = userReference.child("value");
                value.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("value", 1);
                            userReference.setValue(userMap);

                            Toast.makeText(mActivity, "Added To List", Toast.LENGTH_LONG).show();

                        } else {
                            userReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        System.out.println("Favorit dihapus");
                                    } else {
                                        System.out.println("Favorit dihapus");
                                    }
                                }
                            });

                            Toast.makeText(mActivity, "Removed From List", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }
}
