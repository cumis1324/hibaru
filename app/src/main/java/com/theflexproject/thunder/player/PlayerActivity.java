package com.theflexproject.thunder.player;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.utils.pembayaran.IklanPremium;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class PlayerActivity extends AppCompatActivity
        implements View.OnClickListener {

    public static final String KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters";
    public static final String KEY_ITEM_INDEX = "item_index";
    public static final String KEY_POSITION = "position";
    public static final String KEY_AUTO_PLAY = "auto_play";

    public static final String PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    protected VLCVideoLayout playerView;

    protected LinearLayout debugRootView;
    protected @Nullable MediaPlayer player;
    private LibVLC libVLC;

    private String tmdbId;
    private Intent intent;
    private FirebaseManager manager;
    private DatabaseReference databaseReference;
    private String offline;
    private View decorView;
    private int uiOptions;

    private TextView playerTitle;
    private TextView playerEpsTitle;
    private View nfgpluslog;

    private boolean startAutoPlay;
    private long startPosition;
    private int startItemIndex;
    private static final String TAG = "PlayerActivity";
    private boolean ad25, ad50, ad75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        intent = getIntent();
        manager = new FirebaseManager();
        offline = "offline";
        String tmdbId = intent.getStringExtra("tmdbId");
        databaseReference = FirebaseDatabase.getInstance().getReference("History/");
        decorView = getWindow().getDecorView();

        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--aout=android_audiotrack");
        options.add("--audio-time-stretch");
        options.add("-vv");
        libVLC = new LibVLC(this, options);

        playerView = findViewById(R.id.player_view);
        playerTitle = findViewById(R.id.playerTitle);
        nfgpluslog = findViewById(R.id.nfgpluslogo);
        playerEpsTitle = findViewById(R.id.playerEpsTitle);
        loadTitle();
        Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }

        if (savedInstanceState != null) {
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            clearStartPosition();
        }

    }

    private void loadReward() {
        // Using Unity Ads instead of AdMob
        com.theflexproject.thunder.utils.UnityAdHelper.INSTANCE.loadRewardedAd();

        if (player != null) {
            player.pause();
        }

        com.theflexproject.thunder.utils.UnityAdHelper.INSTANCE.showRewardedAd(PlayerActivity.this,
                new com.theflexproject.thunder.utils.UnityAdHelper.AdCallback() {
                    @Override
                    public void onAdComplete() {
                        Log.d(TAG, "Reward ad completed successfully");
                        if (player != null) {
                            player.play();
                        }
                    }

                    @Override
                    public void onAdFailed() {
                        Log.e(TAG, "Reward ad failed");
                        if (player != null) {
                            player.play();
                        }
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void loadTitle() {
        String titleString = intent.getStringExtra("title");
        String yearString = intent.getStringExtra("year");
        String seasonString = intent.getStringExtra("season");
        String epsnumString = intent.getStringExtra("number");
        String titleEpisode = intent.getStringExtra("episode");
        if (yearString != null) {
            if (yearString.equals(offline)) {
                playerTitle.setText(titleString);
                playerEpsTitle.setVisibility(View.GONE);
            } else {
                playerTitle.setText(titleString + " (" + yearString + ")");
                playerEpsTitle.setVisibility(View.GONE);
            }
        } else {
            playerTitle.setText(titleString);
            playerEpsTitle.setText("Season " + seasonString + " Episode " + epsnumString + " : " + titleEpisode);
            playerEpsTitle.setVisibility(View.VISIBLE);
        }

        playerTitle.setVisibility(View.VISIBLE);
    }

    private void savePlayed() {

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();

        clearStartPosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }
        if (isInPictureInPictureMode) {
            // Hide unnecessary UI elements for Picture-in-Picture mode
            // Example: controlView.setVisibility(View.GONE);

            nfgpluslog.setVisibility(View.GONE);
        } else {
            // Restore UI elements when exiting Picture-in-Picture mode
            // Example: controlView.setVisibility(View.VISIBLE);
            nfgpluslog.setVisibility(View.VISIBLE);
            showControls();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateStartPosition();
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_ITEM_INDEX, startItemIndex);
        outState.putLong(KEY_POSITION, startPosition);
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onClick(View view) {

    }

    public void addToPlayed() {
        Integer tmdbId = Integer.valueOf(intent.getStringExtra("tmdbId"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
        // Update the played field in your local database asynchronously
        if (!Objects.equals(tmdbId, "offline")) {
            AsyncTask.execute(() -> {
                String yearString = intent.getStringExtra("year");
                if (yearString != null) {
                    DatabaseClient.getInstance(getApplicationContext()).getAppDatabase().movieDao().updatePlayed(tmdbId,
                            currentDateTime + " added");
                } else {
                    DatabaseClient.getInstance(getApplicationContext()).getAppDatabase().episodeDao()
                            .updatePlayed(tmdbId, currentDateTime + " added");
                }
            });
        }
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    // Internal methods

    /**
     * @return Whether initialization was successful.
     */
    protected boolean initializePlayer() {
        if (player == null) {
            player = new MediaPlayer(libVLC);
            player.attachViews(playerView, null, true, false);

            String urlString = intent.getStringExtra("url");
            Uri uri = Uri.parse(urlString);
            Log.i("Inside Player", uri.toString());
            Media media = new Media(libVLC, uri);

            // Cek setting Passthrough (Consistency with PlayerFragment)
            android.content.SharedPreferences settings = getSharedPreferences("PlayerSettings",
                    android.content.Context.MODE_PRIVATE);
            boolean passthrough = settings.getBoolean("audio_passthrough", false);
            if (passthrough) {
                media.addOption(":audio-passthrough");
                Log.d("PlayerActivity", "Audio Passthrough ENABLED");
            }

            player.setMedia(media);
            media.release();

            player.setEventListener(new PlayerEventListener());
        }

        String tmdbId = intent.getStringExtra("tmdbId");
        if (tmdbId != null && !tmdbId.equals(offline)) {
            PlayerUtils.resumePlayerState(this, player, tmdbId);
            if (startAutoPlay) {
                player.play();
            }
        } else if (startAutoPlay) {
            player.play();
        }

        return true;
    }

    private void createMediaSourceFactory() {
    } // Stub

    private void setRenderersFactory() {
    } // Stub

    protected void releasePlayer() {
        if (player != null) {
            updateStartPosition();
            player.stop();
            player.release();
            player = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.isPlaying();
            startPosition = Math.max(0, player.getTime());
            String tmdbId = intent.getStringExtra("tmdbId");
            if (tmdbId != null && !tmdbId.equals(offline)) {
                PlayerUtils.saveResume(getApplicationContext(), startPosition, player.getLength(), tmdbId);
            }
        }
    }

    protected void clearStartPosition() {
        startAutoPlay = true;
        startPosition = -1;
    }

    private void showControls() {
    }

    private class PlayerEventListener implements MediaPlayer.EventListener {
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Buffering:
                    break;
                case MediaPlayer.Event.EndReached:
                    showControls();
                    break;
                case MediaPlayer.Event.EncounteredError:
                    showControls();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                    decorView.setSystemUiVisibility(uiOptions);
                    break;
            }
        }
    }

    private void load3ads() {
        if (player != null) {
            long cp = player.getTime();
            long tp = player.getLength();
            if (tp > 0) {
                if (!ad25 && cp >= tp * 0.25) {
                    loadReward();
                    ad25 = true;
                }
                if (!ad50 && cp >= tp * 0.50) {
                    loadReward();
                    ad50 = true;
                }
                if (!ad75 && cp >= tp * 0.75) {
                    loadReward();
                    ad75 = true;
                }
            }
        }
    }
}
