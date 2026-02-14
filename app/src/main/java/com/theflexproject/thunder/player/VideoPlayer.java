package com.theflexproject.thunder.player;

import static android.content.ContentValues.TAG;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@UnstableApi
public class VideoPlayer extends AppCompatActivity
        implements View.OnClickListener, PlayerView.ControllerVisibilityListener {

    public static final String KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters";
    public static final String KEY_ITEM_INDEX = "item_index";
    public static final String KEY_POSITION = "position";
    public static final String KEY_AUTO_PLAY = "auto_play";

    public static final String PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    protected PlayerView playerView;

    protected LinearLayout debugRootView;

    private DataSource.Factory dataSourceFactory;
    private MediaItem mediaItem;
    private TrackSelectionParameters trackSelectionParameters;
    private boolean startAutoPlay;
    private int startItemIndex;
    private long startPosition;
    private TextView nfgpluslog;
    private static final int REQUEST_CODE_PICTURE_IN_PICTURE = 1;

    private ImageButton buttonAspectRatio;
    private TextView playerTitle;
    private TextView playerEpsTitle;
    Intent intent;
    int uiOptions;
    View decorView;
    private String TAG = "PlayerActivity";
    FirebaseManager manager;
    private PlayerHelper playerHelper;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        intent = getIntent();
        manager = new FirebaseManager();
        String tmdbId = intent.getStringExtra("tmdbId");
        databaseReference = FirebaseDatabase.getInstance().getReference("History/" + tmdbId);
        decorView = getWindow().getDecorView();
        playerView = findViewById(R.id.player_view);
        playerHelper = new PlayerHelper();

        String mediaUri = getIntent().getStringExtra("mediaUri");
        long position = getIntent().getLongExtra("position", 0);

        playerHelper.initializePlayer(this, playerView, mediaUri);
        playerHelper.seekTo(position);

        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        playerTitle = findViewById(R.id.playerTitle);
        nfgpluslog = findViewById(R.id.nfgpluslogo);
        playerEpsTitle = findViewById(R.id.playerEpsTitle);
        playerView.setControllerVisibilityListener(this);
        playerView.requestFocus();
        loadTitle();
        Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }

    }

    private void loadReward() {
        // Using Unity Ads instead of AdMob
        com.theflexproject.thunder.utils.UnityAdHelper.INSTANCE.loadRewardedAd();

        if (playerHelper != null) {
            playerHelper.setPlayWhenReady(false);
        }

        com.theflexproject.thunder.utils.UnityAdHelper.INSTANCE.showRewardedAd(VideoPlayer.this,
                new com.theflexproject.thunder.utils.UnityAdHelper.AdCallback() {
                    @Override
                    public void onAdComplete() {
                        Log.d(TAG, "Reward ad completed successfully");
                        if (playerHelper != null) {
                            playerHelper.setPlayWhenReady(true);
                        }
                        if (playerView != null) {
                            playerView.onResume();
                        }
                    }

                    @Override
                    public void onAdFailed() {
                        Log.e(TAG, "Reward ad failed");
                        if (playerHelper != null) {
                            playerHelper.setPlayWhenReady(true);
                        }
                        if (playerView != null) {
                            playerView.onResume();
                        }
                    }
                });
    }

    private void loadTitle() {
        String titleString = intent.getStringExtra("title");
        String yearString = intent.getStringExtra("year");
        String seasonString = intent.getStringExtra("season");
        String epsnumString = intent.getStringExtra("number");
        String titleEpisode = intent.getStringExtra("episode");
        if (yearString != null) {
            playerTitle.setText(titleString + " (" + yearString + ")");
            playerEpsTitle.setVisibility(View.GONE);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Check if device orientation is landscape
        if (newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // Show rewarded ad if loaded
            loadReward();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();

        clearStartPosition();
        setIntent(intent);
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
        if (Build.VERSION.SDK_INT <= 23 || playerHelper == null) {

            if (playerView != null) {
                playerView.onResume();
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onBackPressed() {
        long currentPosition = playerHelper.getCurrentPosition();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("position", currentPosition);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();

        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters.toBundle());
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_ITEM_INDEX, startItemIndex);
        outState.putLong(KEY_POSITION, startPosition);
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    // OnClickListener methods

    // StyledPlayerView.ControllerVisibilityListener implementation

    @Override
    public void onVisibilityChanged(int visibility) {
        playerTitle.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
        playerEpsTitle.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View view) {

    }

    public void addToPlayed() {
        Integer tmdbId = Integer.valueOf(intent.getStringExtra("tmdbId"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
        // Update the played field in your local database asynchronously
        AsyncTask.execute(() -> {
            String yearString = intent.getStringExtra("year");
            if (yearString != null) {
                DatabaseClient.getInstance(getApplicationContext()).getAppDatabase().movieDao().updatePlayed(tmdbId,
                        currentDateTime + " added");
            } else {
                DatabaseClient.getInstance(getApplicationContext()).getAppDatabase().episodeDao().updatePlayed(tmdbId,
                        currentDateTime + " added");
            }
        });
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

    private MediaSource.Factory createMediaSourceFactory() {
        DefaultDrmSessionManagerProvider drmSessionManagerProvider = new DefaultDrmSessionManagerProvider();
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(
                DemoUtil.getHttpDataSourceFactory(/* context= */ this));
        return new DefaultMediaSourceFactory(/* context= */ this)
                .setDataSourceFactory(dataSourceFactory)
                .setDrmSessionManagerProvider(drmSessionManagerProvider);
    }

    private void setRenderersFactory(
            ExoPlayer.Builder playerBuilder, boolean preferExtensionDecoders) {
        RenderersFactory renderersFactory = DemoUtil.buildRenderersFactory(/* context= */ this,
                preferExtensionDecoders);
        playerBuilder.setRenderersFactory(renderersFactory);
    }

    protected void releasePlayer() {
        if (playerHelper != null) {
            updateTrackSelectorParameters();
            playerHelper.releasePlayer();
            playerHelper = null;
            playerView.setPlayer(/* player= */ null);

        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void updateTrackSelectorParameters() {
        if (playerHelper != null) {
            trackSelectionParameters = playerHelper.getTrackSelectionParameters();
        }
    }

    private void updateStartPosition() {
        if (playerHelper != null) {
            addToPlayed();
            startAutoPlay = playerHelper.getPlayWhenReady();
            startItemIndex = playerHelper.getCurrentMediaItemIndex();
            startPosition = Math.max(0, playerHelper.getContentPosition());
            String userId = manager.getCurrentUser().getUid();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
            DatabaseReference userReference = databaseReference.child(userId);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("lastPosition", startPosition);
            userMap.put("lastPlayed", currentDateTime);
            userReference.setValue(userMap);

        }
    }

    protected void clearStartPosition() {
        startAutoPlay = true;
        startItemIndex = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void showControls() {

    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            playerView.onPause();
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            decorView.setSystemUiVisibility(uiOptions);
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                playerHelper.seekToDefaultPosition();
                playerHelper.prepare();
            } else {
                showControls();
            }
        }

    }
}
