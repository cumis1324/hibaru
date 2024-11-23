package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL;
import static com.theflexproject.thunder.player.PlayerListener.showSettingsDialog;
import static com.theflexproject.thunder.player.PlayerListener.showSubtitleSelectionDialog;
import static com.theflexproject.thunder.player.PlayerListener.togglePlayback;
import static com.theflexproject.thunder.player.PlayerUtils.createMediaSourceFactory;
import static com.theflexproject.thunder.player.PlayerUtils.lastPositionListener;
import static com.theflexproject.thunder.player.PlayerUtils.resumePlayerState;
import static com.theflexproject.thunder.player.PlayerUtils.updateTimer;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;

import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;

import androidx.media3.datasource.DataSource;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.player.DemoUtil;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.DetailsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class PlayerFragment extends BaseFragment implements PlayerControlView.VisibilityListener {

    private static final String TAG = "PlayerFragment", OFFLINE = "offline",
            HISTORY_PATH = "History/", LAST_POSITION = "lastPosition",
            KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters", KEY_ITEM_INDEX = "item_index",
            KEY_POSITION = "position", KEY_AUTO_PLAY = "auto_play";

    private int movieId;
    private String type = "type";
    private FirebaseManager manager;
    private DatabaseReference databaseReference;
    private String urlString, tmdbId, userId;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImageButton playPauseButton, setting, fullscreen, cc;
    private SeekBar seekBar;
    private TextView timer, movietitle, epstitle;

    private boolean startAutoPlay;
    private int startItemIndex;
    private long startPosition;

    private DataSource.Factory dataSourceFactory;
    private TrackSelectionParameters trackSelectionParameters;
    BottomNavigationView bottomNavigationView;
    private boolean isFullscreen = false;
    FrameLayout playerFrame;
    private DefaultTrackSelector trackSelector;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private ImageView imageView;
    private RelativeLayout customBufferingIndicator;
    private NavigationRailView navigationRailView;
    private Spinner spinnerAudioTrack, spinnerSource;
    View dialogView;

    public PlayerFragment() {
        // Default constructor
    }

    public PlayerFragment(int movieId, String type) {
        this.movieId = movieId;
        this.type = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isTVDevice()){
            View view = inflater.inflate(R.layout.video_tv, container, false);
            initFirebase();
            initViews(view);
            navigationRailView = mActivity.findViewById(R.id.side_navigation);
            navigationRailView.setVisibility(View.GONE);
            fullscreen.setVisibility(View.GONE);
            initPlayerState(savedInstanceState);
            if ("movie".equals(type)) {
                loadMovieDetails(movieId);
            }
            setControlListeners();
            movietitle.setVisibility(View.VISIBLE);
            return view;
        }else {
            View view = inflater.inflate(R.layout.video_player, container, false);
            initFirebase();
            initViews(view);
            initPlayerState(savedInstanceState);
            if ("movie".equals(type)) {
                loadMovieDetails(movieId);
            }
            setControlListeners();
            return view;
        }
    }

    private void initFirebase() {
        manager = new FirebaseManager();
        userId = manager.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference(HISTORY_PATH);
    }

    private void initViews(View view) {
        LayoutInflater inflater = getLayoutInflater();
        dialogView = inflater.inflate(R.layout.dialog_setting, null);
        spinnerAudioTrack = dialogView.findViewById(R.id.spinnerAudioTrack);
        spinnerSource = dialogView.findViewById(R.id.speed);
        playerFrame = view.findViewById(R.id.playerFrame);
        playerFrame.post(() -> {
            int width = playerFrame.getWidth(); // Lebar FrameLayout
            int height = (int) (width / 16.0 * 9.0); // Hitung tinggi sesuai rasio 16:9
            ViewGroup.LayoutParams params = playerFrame.getLayoutParams();
            params.height = height;
            playerFrame.setLayoutParams(params);
        });
        playerView = view.findViewById(R.id.video_view);
        playerView.setControllerVisibilityListener(this);

        View customControls = playerView.findViewById(R.id.custom_controlss);
        playPauseButton = customControls.findViewById(R.id.btn_play_pause);
        seekBar = customControls.findViewById(R.id.seek_bar);
        timer = customControls.findViewById(R.id.player_timer);
        setting = customControls.findViewById(R.id.btn_setting);
        fullscreen = customControls.findViewById(R.id.btn_fullscreen);
        movietitle = customControls.findViewById(R.id.playerTitle);
        epstitle = customControls.findViewById(R.id.playerEpsTitle);
        cc = customControls.findViewById(R.id.btn_cc);
        setting = customControls.findViewById(R.id.btn_setting);
        bottomNavigationView = mActivity.findViewById(R.id.bottom_navigation);
        imageView = view.findViewById(R.id.background_image);
        customBufferingIndicator = view.findViewById(R.id.custom_buffering_indicator);



    }

    private void setControlListeners() {
        playPauseButton.setOnClickListener(v -> togglePlayback(player, playPauseButton));
        fullscreen.setOnClickListener(v -> {
            if (isFullscreen) {
                exitFullscreen();
            } else {
                enterFullscreen();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && fromUser) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }



    private void initPlayerState(Bundle savedInstanceState) {


        if (savedInstanceState != null) {
            trackSelectionParameters = TrackSelectionParameters.fromBundle(
                    Objects.requireNonNull(savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)));
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectionParameters = new TrackSelectionParameters.Builder(mActivity).build();
            clearStartPosition();
        }
    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startItemIndex = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(final int movieId) {
        bottomNavigationView.setVisibility(View.GONE);
        Movie movieDetails = DetailsUtils.getMovieSmallest(mActivity, movieId);
        if (movieDetails != null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getRelease_date();
            String yearCrop = year.substring(0,year.indexOf('-'));
            urlString = movieDetails.getUrlString();
            tmdbId = String.valueOf(movieDetails.getId());
            movietitle.setText(titleText + " ("+yearCrop+")");
            mActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new DetailFragment(movieId, "movie"))
                    .commit();
            if(movieDetails.getBackdrop_path()!=null) {
                Glide.with(mActivity)
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getBackdrop_path())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            }else {
                if(movieDetails.getPoster_path()!=null) {
                    Glide.with(mActivity)
                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPoster_path())
                            .apply(new RequestOptions()
                                    .fitCenter()
                                    .override(Target.SIZE_ORIGINAL))
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                }
            }
            initializePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {
            if (urlString == null || urlString.isEmpty()) {
                Log.e(TAG, "Invalid URL string");
                return;
            }
            Uri uri = Uri.parse(urlString);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            trackSelector = new DefaultTrackSelector(mActivity);
            trackSelector.setParameters(
                    new DefaultTrackSelector.ParametersBuilder(mActivity)
                            .setForceHighestSupportedBitrate(true) // Pilih bitrate tertinggi
                            .build()
            );
            player = new ExoPlayer.Builder(mActivity)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(createMediaSourceFactory(mActivity))
                    .setRenderersFactory(new DefaultRenderersFactory(mActivity)
                            .setEnableDecoderFallback(true)
                            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF))
                    .build();

            player.setTrackSelectionParameters(trackSelectionParameters);
            player.setAudioAttributes(AudioAttributes.DEFAULT, true);
            player.setPlayWhenReady(startAutoPlay);
            playerView.setPlayer(player);
            player.setMediaItem(mediaItem);
            player.prepare();
            resumePlayerState(player, tmdbId);
            if (startItemIndex != C.INDEX_UNSET) {
                player.seekTo(startItemIndex, startPosition);
            }
            player.addListener(new PlayerEventListener());
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
            player = null;
            stopSeekBarUpdate();
        }
        if (lastPositionListener != null) {
            databaseReference.removeEventListener(lastPositionListener);
        }
        bottomNavigationView.setVisibility(View.VISIBLE);
        if (isTVDevice()){
            navigationRailView.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {
        // Handle UI visibility change
    }
    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                seekBar.setProgress((int) player.getCurrentPosition());
                seekBar.setMax((int) player.getDuration());
                updateTimer(timer, player.getCurrentPosition(), player.getDuration());
                handler.postDelayed(this, 1000); // Update setiap detik
            }
        }
    };

    private void startSeekBarUpdate() {
        handler.post(updateSeekBar);
    }

    private void stopSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBar);
    }
    private class PlayerEventListener implements Player.Listener {
        @Override
        public void onTracksChanged(Tracks tracks) {
            mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            cc.setOnClickListener(v -> showSubtitleSelectionDialog(mActivity, mappedTrackInfo, trackSelector));
            cc.setImageResource(subOn() ? R.drawable.ic_cc : R.drawable.ic_cc_filled);
            setting.setOnClickListener(v -> loadSetting());
            Player.Listener.super.onTracksChanged(tracks);
        }
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            playPauseButton.requestFocus();
        }

        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition,
                                            Player.PositionInfo newPosition,
                                            int reason) {

        }
        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            playerView.onPause();
            if (playbackState == Player.STATE_READY) {
                mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                cc.setOnClickListener(v -> showSubtitleSelectionDialog(mActivity, mappedTrackInfo, trackSelector));
                startSeekBarUpdate();
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);
                playPauseButton.setVisibility(View.VISIBLE);
            }
            if (playbackState != Player.STATE_READY) {
                customBufferingIndicator.setVisibility(View.VISIBLE);
                playPauseButton.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
            }
            if (playbackState == Player.STATE_ENDED) {
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);
                playPauseButton.setVisibility(View.VISIBLE);
            }
            if (playbackState == Player.STATE_BUFFERING) {
                playPauseButton.setVisibility(View.GONE);
                // Tampilkan indikator buffering
                customBufferingIndicator.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public void onPlayerError(PlaybackException error) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
            } else {

            }
        }

    }

    private void loadSetting() {
        showSettingsDialog(mActivity, spinnerAudioTrack, spinnerSource, dialogView, trackSelector, mappedTrackInfo, player);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullscreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            exitFullscreen();
        }
    }
    private void enterFullscreen() {

        View decorView;
        int uiOptions;
        decorView = mActivity.getWindow().getDecorView();
        // Set height ke match_parent
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) playerFrame.getLayoutParams();
        params.height = LinearLayout.LayoutParams.MATCH_PARENT;
        playerFrame.setLayoutParams(params);

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
        isFullscreen = true;
    }
    private void exitFullscreen() {
        View decorView = mActivity.getWindow().getDecorView();
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

        if (!isTVDevice()) {
            // Terapkan logika orientasi untuk perangkat non-TV
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        fullscreen.setImageResource(R.drawable.ic_fullscreen);

        isFullscreen = false; // Kembali ke mode normal
    }
    private boolean isTVDevice() {
        UiModeManager uiModeManager = (UiModeManager) mActivity.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
    private boolean subOn(){
        DefaultTrackSelector.Parameters builders = trackSelector.getParameters();
        return builders.getRendererDisabled(C.TRACK_TYPE_VIDEO);
    }

}
