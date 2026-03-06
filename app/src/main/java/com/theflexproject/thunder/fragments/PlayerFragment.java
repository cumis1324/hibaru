package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.player.PlayerListener.fastForward;
import static com.theflexproject.thunder.player.PlayerListener.rewind;
import static com.theflexproject.thunder.player.PlayerListener.togglePlayback;
import static com.theflexproject.thunder.player.PlayerUtils.createMediaSourceFactory;
import static com.theflexproject.thunder.player.PlayerUtils.enterFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.exitFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.isTVDevice;
import static com.theflexproject.thunder.player.PlayerUtils.lastPositionListener;
import static com.theflexproject.thunder.player.PlayerUtils.load3ads;
import static com.theflexproject.thunder.player.PlayerUtils.resumePlayerState;
import static com.theflexproject.thunder.player.PlayerUtils.saveResume;
import static com.theflexproject.thunder.player.PlayerUtils.subOn;
import static com.theflexproject.thunder.player.PlayerUtils.updateTimer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.WindowCompat;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.widget.NestedScrollView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.theflexproject.thunder.adapter.SourceAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;

import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;

import androidx.media3.datasource.DataSource;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.theflexproject.thunder.utils.AdHelper;

import com.theflexproject.thunder.utils.UnityAdHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theflexproject.thunder.MainActivity;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.adapter.SimilarAdapter;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.RandomIndex;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.DemoUtil;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.AdHelper;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.MovieQualityExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class PlayerFragment extends BaseFragment
        implements PlayerControlView.VisibilityListener, MainActivity.OnUserLeaveHintListener {

    private static final String TAG = "PlayerFragment", OFFLINE = "offline",
            HISTORY_PATH = "History/", LAST_POSITION = "lastPosition",
            KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters", KEY_ITEM_INDEX = "item_index",
            KEY_POSITION = "position", KEY_AUTO_PLAY = "auto_play",
            PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    private int itemId;
    private boolean isMovie;
    private FirebaseManager manager;
    private DatabaseReference databaseReference;
    private String tmdbId, userId, urlString, localPath;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImageButton playPauseButton, setting, fullscreen, cc, ff, bw, source, btn_info;
    private SeekBar seekBar;
    private TextView timer, movietitle, epstitle, bufferText;

    private boolean startAutoPlay;
    private int startItemIndex;
    private long startPosition;

    private DataSource.Factory dataSourceFactory;
    private TrackSelectionParameters trackSelectionParameters;
    BottomNavigationView bottomNavigationView;
    private boolean isFullscreen = false;
    private FrameLayout playerFrame;
    private DefaultTrackSelector trackSelector;
    private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;
    private ImageView imageView;
    private RelativeLayout customBufferingIndicator;
    private FrameLayout detailContainer;
    private View topNavigationView;
    private ViewGroup rootView;
    private List<MyMedia> sourceList, similarOrEpisode;
    private GestureDetector gestureDetector;
    PictureInPictureParams pipParams;
    private Movie movieDetails;
    private TVShow tvShowDetails;
    private TVShowSeasonDetails season;
    private int episodeId;
    private RecyclerView similarView;
    private SimilarAdapter.OnItemClickListener similarListener;
    private View view;
    View customControls;
    private NestedScrollView nestedScrollView;
    private Episode episode;
    private boolean isSubscribed;
    private boolean isBuffering = true;

    private View decorView;
    private Intent intent;
    private RandomIndex loadBalancer = new RandomIndex();
    private String randomUrl;
    private String vastUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/23200225483/64&description_url=http%3A%2F%2Fwww.nfgplus.my.id&tfcd=0&npa=0&sz=400x300%7C640x480&gdfp_req=1&unviewed_position_start=1&output=vast&env=vp&impl=s&correlator=&vad_type=linear";

    public PlayerFragment() {
        // Default constructor
    }

    public PlayerFragment(int itemId, boolean isMovie) {
        this.itemId = itemId;
        this.isMovie = isMovie;
    }

    public void updateMovie(int itemId, boolean isMovie) {
        this.itemId = itemId;
        this.isMovie = isMovie;
        if (player != null) {
            newSource();
        }
        loadMovieDetails(itemId);
    }

    public PlayerFragment(TVShow tvShowDetails, TVShowSeasonDetails seasonDetails, int episodeId) {
        this.isMovie = false;
        this.tvShowDetails = tvShowDetails;
        this.season = seasonDetails;
        this.episodeId = episodeId;
    }

    public void updateEpisode(TVShow tvShowDetails, TVShowSeasonDetails seasonDetails, int episodeId) {
        this.isMovie = false;
        this.tvShowDetails = tvShowDetails;
        this.season = seasonDetails;
        this.episodeId = episodeId;
        if (player != null) {
            newSource();
        }
        episode = DetailsUtils.getNextEpisode(mActivity, episodeId);
        loadSeriesDetails(episode);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getInt("videoId");
            isMovie = getArguments().getBoolean("isMovie");
            localPath = getArguments().getString("localPath");

            // For TV episodes, read episodeId, tvShow, and season from arguments
            if (!isMovie) {
                episodeId = getArguments().getInt("episodeId", itemId);
                tvShowDetails = getArguments().getParcelable("tvShow");
                season = getArguments().getParcelable("season");

                if (tvShowDetails == null) {
                    int showId = getArguments().getInt("showId", -1);
                    if (showId != -1) {
                        tvShowDetails = DetailsUtils.getSeriesDetails(mActivity, showId);
                    }
                }
                if (season == null) {
                    int seasonId = getArguments().getInt("seasonId", -1);
                    if (seasonId != -1) {
                        season = DetailsUtils.getSeasonDetails(mActivity, seasonId);
                    }
                }

                Log.d(TAG, "onCreate: Episode mode - episodeId=" + episodeId
                        + ", tvShow=" + (tvShowDetails != null ? tvShowDetails.getName() : "NULL")
                        + ", season=" + (season != null ? season.getSeasonNumber() : "NULL"));
            } else {
                Log.d(TAG, "onCreate: Movie mode - itemId=" + itemId);
            }
        }
        SharedPreferences prefs = requireContext().getSharedPreferences("langgananUser", Context.MODE_PRIVATE);
        isSubscribed = prefs.getBoolean("isSubscribed", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((MainActivity) mActivity).setOnUserLeaveHintListener(this);
        intent = mActivity.getIntent();
        if (isTVDevice(mActivity)) {
            view = inflater.inflate(R.layout.video_tv, container, false);
        } else {
            view = inflater.inflate(R.layout.video_player, container, false);
        }
        loadBalancer = new RandomIndex();
        randomUrl = loadBalancer.getSelectedDomain();
        initFirebase();
        initViews(view);
        initPlayerState(savedInstanceState);
        if (isMovie) {
            Log.d(TAG, "onCreateView: Loading movie details for itemId=" + itemId);
            loadMovieDetails(itemId);
        } else {
            Log.d(TAG, "onCreateView: Loading episode details for episodeId=" + episodeId);
            episode = DetailsUtils.getNextEpisode(mActivity, episodeId);
            Log.d(TAG, "onCreateView: Episode loaded - "
                    + (episode != null ? "id=" + episode.getId() + ", name=" + episode.getName() : "NULL"));
            loadSeriesDetails(episode);
        }
        setControlListeners();

        return view;
    }

    private void initFirebase() {
        manager = new FirebaseManager();
        userId = manager.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference(HISTORY_PATH);
    }

    private void initViews(View view) {
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
        bufferText = view.findViewById(R.id.buffer_text);

        customControls = playerView.findViewById(R.id.custom_controlss);
        playPauseButton = customControls.findViewById(R.id.btn_play_pause);
        seekBar = customControls.findViewById(R.id.seek_bar);
        timer = customControls.findViewById(R.id.player_timer);
        setting = customControls.findViewById(R.id.btn_setting);
        fullscreen = customControls.findViewById(R.id.btn_fullscreen);
        movietitle = customControls.findViewById(R.id.playerTitle);
        epstitle = customControls.findViewById(R.id.playerEpsTitle);
        cc = customControls.findViewById(R.id.btn_cc);
        btn_info = customControls.findViewById(R.id.btn_info);
        setting = customControls.findViewById(R.id.btn_setting);
        imageView = view.findViewById(R.id.background_image);
        customBufferingIndicator = view.findViewById(R.id.custom_buffering_indicator);
        ff = customControls.findViewById(R.id.btn_ff);
        bw = customControls.findViewById(R.id.btn_bw);
        source = customControls.findViewById(R.id.btn_src);
        bottomNavigationView = mActivity.findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }

        Rational aspectRatio = new Rational(16, 9);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pipParams = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }
        similarView = view.findViewById(R.id.similarAndEpisode);
        detailContainer = view.findViewById(R.id.detail_container);
        if (isTVDevice(mActivity)) {
            topNavigationView = mActivity.findViewById(R.id.top_navigation);
            if (topNavigationView != null) {
                topNavigationView.setVisibility(View.GONE);
            }
            fullscreen.setVisibility(View.GONE);
            if (btn_info != null) {
                btn_info.setVisibility(View.VISIBLE);
            }

            // Remote Info key support
            playerView.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_INFO) {
                    if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                        showInfoSideSheet();
                    }
                    return true;
                }
                return false;
            });
        } else {
            if (btn_info != null) {
                btn_info.setVisibility(View.GONE);
            }
        }
        exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);

    }

    private void setControlListeners() {
        source.setOnClickListener(v -> showSources());
        cc.setOnClickListener(v -> {
            if (mappedTrackInfo != null) {
                showSubtitles();
            } else {
                Toast.makeText(mActivity, "Loading streams, please wait...", Toast.LENGTH_SHORT).show();
            }
        });
        setting.setOnClickListener(v -> {
            if (player != null) {
                showSettings();
            }
        });
        if (btn_info != null) {
            btn_info.setOnClickListener(v -> showInfoSideSheet());
        }
        playPauseButton.setOnClickListener(v -> togglePlayback(player, playPauseButton));
        fullscreen.setOnClickListener(v -> {
            if (isFullscreen) {
                exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);
                isFullscreen = false;
            } else {
                enterFullscreen(mActivity, playerFrame, movietitle, fullscreen);
                isFullscreen = true;
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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        gestureDetector = new GestureDetector(mActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true; // Menangani tap tunggal (opsional)
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isBuffering)
                    return false;
                // Tentukan area tap: kiri untuk rewind, kanan untuk fast forward
                float screenWidth = playerView.getWidth();
                if (e.getX() < screenWidth / 2) {
                    rewind(player, bw);
                } else {
                    fastForward(player, ff);
                }
                return true; // Menandakan double-tap berhasil diproses
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                // Tidak wajib, ini untuk event lebih lanjut terkait double-tap
                return super.onDoubleTapEvent(e);
            }
        });

        // Paksa listener sentuhan aktif sejak awal agar menu bisa muncul saat Loading
        playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
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

    private void initializePlayer(String urlString) {
        if (player == null) {
            if (urlString == null || urlString.isEmpty()) {
                return;
            }
            Uri uri = Uri.parse(urlString);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            trackSelector = new DefaultTrackSelector(mActivity);
            trackSelector.setParameters(
                    new DefaultTrackSelector.ParametersBuilder(mActivity)
                            .setExceedRendererCapabilitiesIfNecessary(false)
                            .build());
            ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(/* context= */ mActivity)
                    .setMediaSourceFactory(createMediaSourceFactory(mActivity));
            setRenderersFactory(playerBuilder, intent.getBooleanExtra(PREFER_EXTENSION_DECODERS_EXTRA, false));
            player = playerBuilder.setTrackSelector(trackSelector).build();

            player.setTrackSelectionParameters(trackSelectionParameters);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build();
            player.setAudioAttributes(audioAttributes, true);
            player.setPlayWhenReady(startAutoPlay);
            playerView.setPlayer(player);
            player.setMediaItem(mediaItem);
            player.prepare();
            resumePlayerState(player, tmdbId);
            if (startItemIndex != C.INDEX_UNSET) {
                player.seekTo(startItemIndex, startPosition);
            }
            player.addListener(new PlayerEventListener());
            btn_info.requestFocus();
            if (player.getPlayWhenReady()) {
                AdHelper.loadReward(mActivity, mActivity, player, playerView);
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setRenderersFactory(
            ExoPlayer.Builder playerBuilder, boolean preferExtensionDecoders) {
        RenderersFactory renderersFactory = DemoUtil.buildRenderersFactory(/* context= */ mActivity,
                preferExtensionDecoders);
        playerBuilder.setRenderersFactory(renderersFactory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releasePlayer();
    }

    @Override
    public void onVisibilityChange(int visibility) {
        // Handle UI visibility change
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
            if (bottomNavigationView != null) {
                bottomNavigationView.setVisibility(View.GONE);
            }
            if (isTVDevice(mActivity)) {
                if (topNavigationView != null) {
                    topNavigationView.setVisibility(View.GONE);
                }
            }
            startSeekBarUpdate();
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
                updateWatchNext();
                handler.postDelayed(this, 1000); // Update
                                                 // setiap
                                                 // detik
                load3ads(mActivity, mActivity, player, playerView);
            }
        }
    };

    private void startSeekBarUpdate() {
        handler.post(updateSeekBar);
    }

    private void stopSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    public void onUserLeaveHint() {
        // handleUserLeaveHint();

    }

    private void handleUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mActivity.enterPictureInPictureMode(pipParams);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode && player != null) {
            customControls.setVisibility(View.GONE);
        } else {
            if (!isTVDevice(mActivity)) {
                // Terapkan logika orientasi untuk perangkat non-TV
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            customControls.setVisibility(View.VISIBLE);
        }
        if (!isInPictureInPictureMode && player != null) {
            // Ketika keluar dari mode PIP, destroy player
            destroyAll();
        }

    }

    private void setAdsState() {
        SharedPreferences prefs = mActivity.getSharedPreferences("load4Ads", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("adStart", false);
        editor.putBoolean("ad25", false);
        editor.putBoolean("ad50", false);
        editor.putBoolean("ad75", false);
        editor.apply();
    }

    private class PlayerEventListener implements Player.Listener {
        @Override
        public void onTracksChanged(Tracks tracks) {
            mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            cc.setImageResource(subOn(trackSelector) ? R.drawable.ic_cc : R.drawable.ic_cc_filled);
            Player.Listener.super.onTracksChanged(tracks);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            playPauseButton.requestFocus();
            if (isPlaying) {
                startSeekBarUpdate(); // Memulai update seekbar
            } else {
                stopSeekBarUpdate(); // Menghentikan update seekbar
            }
        }

        @Override
        public void onPlaybackStateChanged(@Player.State int playbackState) {
            if (playbackState == Player.STATE_READY) {
                isBuffering = false;
                mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                startSeekBarUpdate();
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);
                ff.setOnClickListener(v -> fastForward(player, ff));
                bw.setOnClickListener(v -> rewind(player, bw));

                // Aktifkan kembali kontrol playback
                playPauseButton.setEnabled(true);
                playPauseButton.setAlpha(1.0f);
                seekBar.setEnabled(true);
                seekBar.setAlpha(1.0f);
                ff.setEnabled(true);
                ff.setAlpha(1.0f);
                bw.setEnabled(true);
                bw.setAlpha(1.0f);

                if (isTVDevice(mActivity)) {
                    movietitle.setVisibility(View.VISIBLE);
                    if (playPauseButton != null) {
                        playPauseButton.requestFocus();
                    }
                }
            } else if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE) {
                isBuffering = true;
                customBufferingIndicator.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);

                // Matikan kontrol playback agar tidak mengganggu loading
                playPauseButton.setEnabled(false);
                playPauseButton.setAlpha(0.5f);
                seekBar.setEnabled(false);
                seekBar.setAlpha(0.5f);
                ff.setEnabled(false);
                ff.setAlpha(0.5f);
                bw.setEnabled(false);
                bw.setAlpha(0.5f);

                if (isTVDevice(mActivity) && btn_info != null) {
                    btn_info.requestFocus();
                }
            } else if (playbackState == Player.STATE_ENDED) {
                isBuffering = false;
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e("ExoPlayerError", error.getMessage());
            bufferText.setText(error.getMessage());
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
            }
        }

    }

    private void showSubtitles() {
        if (mappedTrackInfo == null)
            return;

        if (isTVDevice(mActivity)) {
            SubtitleSideSheetDialogFragment subtitleSheet = SubtitleSideSheetDialogFragment.Companion.newInstance(
                    mappedTrackInfo,
                    trackSelector);
            subtitleSheet.show(getChildFragmentManager(), "subtitle_side_sheet");
        } else {
            SubtitleBottomSheetDialogFragment subtitleSheet = SubtitleBottomSheetDialogFragment.Companion.newInstance(
                    mappedTrackInfo,
                    trackSelector);
            subtitleSheet.show(getChildFragmentManager(), "subtitle_bottom_sheet");
        }
    }

    private void showSettings() {
        if (player == null)
            return;

        if (isTVDevice(mActivity)) {
            SettingsSideSheetDialogFragment settingsSheet = SettingsSideSheetDialogFragment.Companion.newInstance(
                    trackSelector,
                    mappedTrackInfo,
                    player);
            settingsSheet.show(getChildFragmentManager(), "settings_side_sheet");
        } else {
            SettingsBottomSheetDialogFragment settingsSheet = SettingsBottomSheetDialogFragment.Companion.newInstance(
                    trackSelector,
                    mappedTrackInfo,
                    player);
            settingsSheet.show(getChildFragmentManager(), "settings_bottom_sheet");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullscreen(mActivity, playerFrame, movietitle, fullscreen);
            isFullscreen = true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);
            isFullscreen = false;
        }
    }

    private void showSources() {
        if (sourceList == null || sourceList.isEmpty()) {
            Toast.makeText(mActivity, "No sources available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isTVDevice(mActivity)) {
            // TV Strategy: Use Side Sheet
            SourceSideSheetDialogFragment sourceSheet = SourceSideSheetDialogFragment.Companion.newInstance(
                    sourceList,
                    urlString,
                    selectedSource -> {
                        String selectedUrlRaw = "";
                        if (selectedSource instanceof Movie)
                            selectedUrlRaw = ((Movie) selectedSource).getUrlString();
                        else if (selectedSource instanceof Episode)
                            selectedUrlRaw = ((Episode) selectedSource).getUrlString();

                        String newUrl = selectedUrlRaw.replaceAll("drive\\d*\\.nfgplusmirror\\.workers.dev", randomUrl);

                        if (!Objects.equals(selectedUrlRaw, urlString)) {
                            switchSource(newUrl);
                            urlString = selectedUrlRaw;
                        }
                        return null;
                    });
            sourceSheet.show(getChildFragmentManager(), "source_side_sheet");
        } else {
            // Mobile Strategy: Use modern Compose-based Bottom Sheet
            SourceBottomSheetDialogFragment sourceSheet = SourceBottomSheetDialogFragment.Companion.newInstance(
                    sourceList,
                    urlString,
                    selectedSource -> {
                        String selectedUrlRaw = "";
                        if (selectedSource instanceof Movie)
                            selectedUrlRaw = ((Movie) selectedSource).getUrlString();
                        else if (selectedSource instanceof Episode)
                            selectedUrlRaw = ((Episode) selectedSource).getUrlString();

                        String newUrl = selectedUrlRaw.replaceAll("drive\\d*\\.nfgplusmirror\\.workers.dev", randomUrl);

                        if (!Objects.equals(selectedUrlRaw, urlString)) {
                            switchSource(newUrl);
                            urlString = selectedUrlRaw;
                        }
                        return null;
                    });
            sourceSheet.show(getChildFragmentManager(), "source_bottom_sheet");
        }
    }

    private void switchSource(String newUrl) {
        if (player != null) {
            long currentPos = player.getCurrentPosition();
            boolean wasPlaying = player.getPlayWhenReady();

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(newUrl));
            player.setMediaItem(mediaItem, false); // false = don't reset position if possible
            player.seekTo(currentPos);
            player.prepare();
            player.setPlayWhenReady(wasPlaying);

            customBufferingIndicator.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    protected void newSource() {
        if (player != null) {
            setAdsState();
            saveResume(player, tmdbId);
            player.release();
            player = null;
            playerView.setPlayer(/* player= */ null);
            stopSeekBarUpdate();
            customBufferingIndicator.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            saveResume(player, tmdbId);
            player.release();
            player = null;
            playerView.setPlayer(null);
            stopSeekBarUpdate();
            destroyAll();
        }
    }

    private void destroyAll() {
        setAdsState();
        decorView = mActivity.getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);
        rootView.setPadding(0, 0, 0, 0);
        ((MainActivity) mActivity).setOnUserLeaveHintListener(null);
        if (lastPositionListener != null) {
            databaseReference.removeEventListener(lastPositionListener);
        }
        if (isFullscreen) {
            exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);
        }

        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(isTVDevice(mActivity) ? View.GONE : View.VISIBLE);
        }

        if (topNavigationView != null) {
            topNavigationView.setVisibility(isTVDevice(mActivity) ? View.VISIBLE : View.GONE);
        }
    }

    private void showInfoSideSheet() {
        if (isTVDevice(mActivity)) {
            InfoSideSheetDialogFragment infoSheet;
            if (isMovie) {
                infoSheet = InfoSideSheetDialogFragment.Companion.newInstance(itemId);
            } else {
                if (tvShowDetails != null) {
                    infoSheet = InfoSideSheetDialogFragment.Companion.newTvInstance(tvShowDetails.getId());
                } else {
                    return; // Nowhere to get info from yet
                }
            }
            infoSheet.show(getChildFragmentManager(), "info_side_sheet");
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(final int movieId) {
        movieDetails = DetailsUtils.getMovieSmallest(mActivity, movieId);
        if (movieDetails != null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getReleaseDate();
            urlString = movieDetails.getUrlString();
            String newUrl = urlString.replaceAll(
                    "drive\\d*\\.nfgplusmirror\\.workers.dev",
                    randomUrl);
            String yearCrop = year.substring(0, year.indexOf('-'));
            tmdbId = String.valueOf(movieDetails.getId());
            movietitle.setText(titleText + " (" + yearCrop + ")");
            if (!isTVDevice(mActivity)) {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container,
                                com.theflexproject.thunder.ui.detail.DetailFragment.Companion.newInstance(movieId))
                        .commit();
            }
            if (movieDetails.getBackdropPath() != null) {
                Glide.with(mActivity)
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getBackdropPath())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else {
                if (movieDetails.getPosterPath() != null) {
                    Glide.with(mActivity)
                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPosterPath())
                            .apply(new RequestOptions()
                                    .fitCenter()
                                    .override(Target.SIZE_ORIGINAL))
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                }
            }
            if (localPath != null && !localPath.isEmpty()) {
                initializePlayer(localPath);
            } else if (com.theflexproject.thunder.Constants.isAdmin(userId)) {
                initializePlayer(urlString);
            } else {
                initializePlayer(newUrl);
            }
        }
        sourceList = (List<MyMedia>) (List<?>) DetailsUtils.getSourceList(mActivity, movieId);
    }

    @SuppressLint("SetTextI18n")
    private void loadSeriesDetails(Episode episode) {

        if (tvShowDetails != null) {
            String title = tvShowDetails.getName();
            int seasonId = season.getId();
            TVShowSeasonDetails seasonDetails = DetailsUtils.getSeasonDetails(mActivity, seasonId);
            movietitle.setText(title);

            // Load DetailFragment with tvShowId (like movies do)
            Log.d(TAG, "loadSeriesDetails: Loading DetailFragment with tvShowId=" + tvShowDetails.getId());
            if (!isTVDevice(mActivity)) {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container,
                                com.theflexproject.thunder.ui.detail.DetailFragment.Companion
                                        .newTvInstance(tvShowDetails.getId()))
                        .commit();
            }

            if (episode != null) {
                epstitle.setText("Season: " + seasonDetails.getSeasonNumber()
                        + " Episode: " + episode.getName());
                epstitle.setVisibility(View.VISIBLE);
                tmdbId = String.valueOf(episode.getId());
                urlString = episode.getUrlString();
                String newUrl = urlString.replaceAll(
                        "drive\\d*\\.nfgplusmirror\\.workers.dev",
                        randomUrl);
                if (localPath != null && !localPath.isEmpty()) {
                    initializePlayer(localPath);
                } else if (com.theflexproject.thunder.Constants.isAdmin(userId)) {
                    initializePlayer(urlString);
                } else {
                    initializePlayer(newUrl);
                }
                sourceList = (List<MyMedia>) (List<?>) DetailsUtils.getEpisodeSource(mActivity, episode.getId());
            } else {
                Toast.makeText(mActivity, "File Not Found", Toast.LENGTH_SHORT).show();
            }
            if (tvShowDetails.getBackdropPath() != null) {
                Glide.with(mActivity)
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getBackdropPath())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else {
                if (tvShowDetails.getPosterPath() != null) {
                    Glide.with(mActivity)
                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getPosterPath())
                            .apply(new RequestOptions()
                                    .fitCenter()
                                    .override(Target.SIZE_ORIGINAL))
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                }
            }
        }
    }

    private void updateWatchNext() {
        if (isTVDevice(mActivity) && player != null && (movieDetails != null || episode != null)) {
            com.theflexproject.thunder.utils.WatchNextHelper.INSTANCE.updateWatchNextProgram(
                    mActivity,
                    movieDetails,
                    episode,
                    player.getCurrentPosition(),
                    player.getDuration());
        }
    }

    public void toggleSideSheetResize(boolean shrink) {
        if (!isTVDevice(mActivity) || playerFrame == null)
            return;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int targetWidth = shrink ? (int) (screenWidth * 0.65) : screenWidth;
        int startWidth = playerFrame.getWidth();

        ValueAnimator animator = ValueAnimator.ofInt(startWidth, targetWidth);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = playerFrame.getLayoutParams();
            params.width = val;
            params.height = (int) (val / 16.0 * 9.0);
            playerFrame.setLayoutParams(params);
        });

        // Detail container is only for mobile now
        if (!isTVDevice(mActivity) && detailContainer != null) {
            if (shrink) {
                detailContainer.animate().alpha(0f).setDuration(200)
                        .withEndAction(() -> detailContainer.setVisibility(View.GONE)).start();
            } else {
                detailContainer.setVisibility(View.VISIBLE);
                detailContainer.animate().alpha(1f).setDuration(200).start();
            }
        }

        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

}
