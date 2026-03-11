package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.player.PlayerListener.fastForward;
import static com.theflexproject.thunder.player.PlayerListener.rewind;
import static com.theflexproject.thunder.player.PlayerListener.togglePlayback;
import static com.theflexproject.thunder.player.PlayerUtils.enterFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.exitFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.isTVDevice;
import static com.theflexproject.thunder.player.PlayerUtils.lastPositionListener;
import static com.theflexproject.thunder.player.PlayerUtils.load3ads;
import static com.theflexproject.thunder.player.PlayerUtils.resumePlayerState;
import static com.theflexproject.thunder.player.PlayerUtils.saveResume;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.media.AudioManager;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.widget.NestedScrollView;
// import androidx.media3.ui.PlayerView;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

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
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.MovieQualityExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerFragment extends BaseFragment
        implements MainActivity.OnUserLeaveHintListener {

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

    private LibVLC libVLC;
    private MediaPlayer player;
    private VLCVideoLayout videoLayout;
    private ImageButton playPauseButton, setting, fullscreen, cc, ff, bw, source, btn_info;
    private SeekBar seekBar;
    private TextView timer, movietitle, epstitle, bufferText;
    private ProgressBar bufferProgress;

    private boolean startAutoPlay;
    private int startItemIndex;
    private long startPosition;

    BottomNavigationView bottomNavigationView;
    private boolean isFullscreen = false;
    private FrameLayout playerFrame;
    private ImageView imageView;
    private View customBufferingIndicator;
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

    private boolean isLocked = false;
    private float volumeAccumulator = 0f;
    private int currentResizeMode = 0; // FIT
    private ImageButton btnLock;
    private View judulUtama, settingContainer, middleControls, bottomControls;

    private final Handler hideControlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;

    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private final Runnable sleepTimerRunnable = () -> {
        if (player != null && player.isPlaying()) {
            player.pause();
            Toast.makeText(mActivity, "Sleep timer: Playback paused", Toast.LENGTH_LONG).show();
        }
    };

    private final android.content.BroadcastReceiver sleepTimerReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int minutes = intent.getIntExtra("minutes", 0);
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            if (minutes > 0) {
                sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L);
            }
        }
    };

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
        Log.d(TAG, "PlayerFragment WAKES UP - onCreate called");
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
            long intentPos = getArguments().getLong("startPos", -1);
            if (intentPos > 0) {
                startPosition = intentPos;
                Log.d(TAG, "onCreate: Received startPos from intent: " + intentPos);
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

        android.content.IntentFilter filter = new android.content.IntentFilter(
                "com.theflexproject.thunder.ACTION_SLEEP_TIMER");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mActivity.registerReceiver(sleepTimerReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            mActivity.registerReceiver(sleepTimerReceiver, filter);
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
        videoLayout = view.findViewById(R.id.video_layout);
        bufferText = view.findViewById(R.id.buffer_text);

        customControls = view.findViewById(R.id.player_controls);
        judulUtama = customControls.findViewById(R.id.judulUtama);
        settingContainer = customControls.findViewById(R.id.setting);
        middleControls = customControls.findViewById(R.id.middle_controls);
        bottomControls = customControls.findViewById(R.id.player_timer).getParent().getParent() instanceof View
                ? (View) customControls.findViewById(R.id.player_timer).getParent().getParent()
                : null;

        btnLock = customControls.findViewById(R.id.btn_lock);
        if (!isTVDevice(mActivity)) {
            btnLock.setVisibility(View.VISIBLE);
            btnLock.setOnClickListener(v -> toggleLock());
        } else {
            btnLock.setVisibility(View.GONE);
        }

        loadResizeMode();
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
        bufferProgress = view.findViewById(R.id.buffer_progress);
        bufferText = view.findViewById(R.id.buffer_text);
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

            videoLayout.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != android.view.KeyEvent.ACTION_DOWN)
                    return false;

                boolean controlsVisible = customControls != null && customControls.getVisibility() == View.VISIBLE;

                // Reset hide timer on any key press if controls are visible
                if (controlsVisible) {
                    startAutoHideControls();
                }

                switch (keyCode) {
                    case android.view.KeyEvent.KEYCODE_INFO:
                        showInfoSideSheet();
                        return true;
                    case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
                    case android.view.KeyEvent.KEYCODE_ENTER:
                        if (controlsVisible) {
                            return false; // Let the focused button handle the click
                        } else {
                            toggleControls();
                            return true;
                        }
                    case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (controlsVisible)
                            return false; // Let native focus handle it
                        fastForward(player, ff);
                        showControls();
                        return true;
                    case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                        if (controlsVisible)
                            return false; // Let native focus handle it
                        rewind(player, bw);
                        showControls();
                        return true;
                    case android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        fastForward(player, ff);
                        startAutoHideControls();
                        return true;
                    case android.view.KeyEvent.KEYCODE_MEDIA_REWIND:
                        rewind(player, bw);
                        startAutoHideControls();
                        return true;
                    case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case android.view.KeyEvent.KEYCODE_SPACE:
                        togglePlayback(player, playPauseButton);
                        startAutoHideControls();
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

        if (!isTVDevice(mActivity)) {
            videoLayout.setClickable(true);
            videoLayout.setFocusable(true);
        }
        customControls.setOnClickListener(v -> toggleControls());
        startAutoHideControls();
    }

    private void toggleControls() {
        if (isLocked) {
            if (btnLock.getVisibility() == View.VISIBLE) {
                btnLock.setVisibility(View.GONE);
            } else {
                btnLock.setVisibility(View.VISIBLE);
                startAutoHideControls();
            }
            return;
        }

        if (customControls.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        customControls.setVisibility(View.VISIBLE);
        if (isTVDevice(mActivity)) {
            playPauseButton.requestFocus();
        }
        startAutoHideControls();
    }

    private void hideControls() {
        if (isLocked) {
            btnLock.setVisibility(View.GONE);
            return;
        }
        customControls.setVisibility(View.GONE);
    }

    private void startAutoHideControls() {
        if (isBuffering)
            return; // Keep controls visible while buffering
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
        hideControlsHandler.postDelayed(hideControlsRunnable, 3500);
    }

    private void setControlListeners() {
        source.setOnClickListener(v -> {
            showSources();
            startAutoHideControls();
        });
        ff.setOnClickListener(v -> {
            fastForward(player, ff);
            startAutoHideControls();
        });
        bw.setOnClickListener(v -> {
            rewind(player, bw);
            startAutoHideControls();
        });
        cc.setOnClickListener(v -> {
            if (player != null) {
                showSubtitles();
            } else {
                Toast.makeText(mActivity, "Loading streams, please wait...", Toast.LENGTH_SHORT).show();
            }
            startAutoHideControls();
        });
        setting.setOnClickListener(v -> {
            if (player != null) {
                showSettings();
            }
            startAutoHideControls();
        });
        if (btn_info != null) {
            btn_info.setOnClickListener(v -> {
                showInfoSideSheet();
                startAutoHideControls();
            });
        }
        playPauseButton.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                    playPauseButton.setImageResource(R.drawable.ic_play);
                } else {
                    player.play();
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                }
            }
            startAutoHideControls();
        });
        fullscreen.setOnClickListener(v -> {
            if (isFullscreen) {
                exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);
                isFullscreen = false;
            } else {
                enterFullscreen(mActivity, playerFrame, movietitle, fullscreen);
                isFullscreen = true;
            }
            startAutoHideControls();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && fromUser) {
                    player.setTime(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                hideControlsHandler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startAutoHideControls();
            }
        });
        gestureDetector = new GestureDetector(mActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true; // Must return true to track gestures
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked || isBuffering)
                    return false;
                // Tentukan area tap: kiri untuk rewind, kanan untuk fast forward
                float screenWidth = videoLayout.getWidth();
                if (e.getX() < screenWidth / 2) {
                    rewind(player, bw);
                } else {
                    fastForward(player, ff);
                }
                return true; // Menandakan double-tap berhasil diproses
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isLocked || isTVDevice(mActivity) || !isFullscreen)
                    return false;

                float screenWidth = videoLayout.getWidth();
                float screenHeight = videoLayout.getHeight();

                if (e1.getX() < screenWidth / 2) {
                    // Left side: Brightness
                    adjustBrightness(distanceY / screenHeight);
                } else {
                    // Right side: Volume
                    adjustVolume(distanceY / screenHeight);
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                // Tidak wajib, ini untuk event lebih lanjut terkait double-tap
                return super.onDoubleTapEvent(e);
            }
        });

        // Paksa listener sentuhan aktif sejak awal agar menu bisa muncul saat Loading
        videoLayout.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            return handled || v.performClick();
        });
    }

    private void initPlayerState(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            clearStartPosition();
        }
    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startItemIndex = -1;
        startPosition = -1;
    }

    private void initializePlayer(final String urlString) {
        if (player != null)
            return;
        if (urlString == null || urlString.isEmpty())
            return;

        // post() ensures the view is fully attached and measured before VLC starts.
        // video_tv.xml now uses match_parent so the size is guaranteed valid.
        videoLayout.post(() -> {
            if (player != null)
                return; // guard against double-init
            Log.d(TAG, "initializePlayer: starting VLC, surface=" + videoLayout.getWidth() + "x"
                    + videoLayout.getHeight());
            doInitializePlayer(urlString);
        });
    }

    private void doInitializePlayer(String urlString) {
        if (player != null)
            return;

        // Sanitize URL: Replace spaces with %20 for VLC MRL compatibility
        if (urlString != null) {
            urlString = urlString.replace(" ", "%20");
        }

        ArrayList<String> options = new ArrayList<>();
        options.add("--http-reconnect");
        options.add("--network-caching=3000");
        options.add("--aout=android_audiotrack");
        options.add("--audio-time-stretch");
        options.add("-vvv");

        Log.d(TAG, "doInitializePlayer: " + urlString);
        libVLC = new LibVLC(mActivity, options);
        player = new MediaPlayer(libVLC);
        player.attachViews(videoLayout, null, false, false);
        Media media = new Media(libVLC, Uri.parse(urlString));
        applyMediaOptions(media);
        player.setMedia(media);
        media.release();

        player.setEventListener(new PlayerEventListener());
        player.play();

        if (startPosition <= 0) {
            startPosition = PlayerUtils.getResumePosition(mActivity, tmdbId);
        }
        // PlayerEventListener handles the initial seek and Firebase resume check
        // once the player reaches the Playing state.

        if (btn_info != null)
            btn_info.requestFocus();
        Log.d(TAG, "doInitializePlayer: VLC initiated and playing");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releasePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            long currentPos = player.getTime();
            Log.d(TAG, "onPause: Saving resume position: " + currentPos + " for " + tmdbId);
            PlayerUtils.saveResume(mActivity, currentPos, player.getLength(), tmdbId);
            player.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
            if (bottomNavigationView != null) {
                bottomNavigationView.setVisibility(View.GONE);
            }
            if (isTVDevice(mActivity)) {
                if (topNavigationView != null) {
                    topNavigationView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void startSeekBarUpdate() {
        handler.post(updateSeekBar);
    }

    private void stopSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBar);
    }

    private class PlayerEventListener implements MediaPlayer.EventListener {
        private boolean initialSeekDone = false;
        private boolean firebaseResumeChecked = false;

        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    isBuffering = false;
                    customBufferingIndicator.setVisibility(View.GONE);
                    playPauseButton.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    startSeekBarUpdate();

                    // Handle initial seek from startPosition if provided
                    if (!initialSeekDone && startPosition > 0) {
                        player.setTime(startPosition);
                        initialSeekDone = true;
                        Log.d(TAG, "PlayerEventListener: Playing, applying startPosition: " + startPosition);
                    }

                    if (!firebaseResumeChecked) {
                        PlayerUtils.resumePlayerState(getContext(), player, tmdbId);
                        firebaseResumeChecked = true;
                        Log.d(TAG, "PlayerEventListener: Checking Firebase sync for resume");
                    }

                    // Enable controls
                    playPauseButton.setEnabled(true);
                    playPauseButton.setAlpha(1.0f);
                    seekBar.setEnabled(true);
                    seekBar.setAlpha(1.0f);
                    ff.setEnabled(true);
                    ff.setAlpha(1.0f);
                    bw.setEnabled(true);
                    bw.setAlpha(1.0f);

                    updateWatchNext();
                    break;
                case MediaPlayer.Event.Paused:
                    playPauseButton.setImageResource(R.drawable.ic_play);
                    stopSeekBarUpdate();
                    break;
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.EndReached:
                    isBuffering = false;
                    stopSeekBarUpdate();
                    if (isTVDevice(mActivity) && tmdbId != null) {
                        com.theflexproject.thunder.utils.WatchNextHelper.INSTANCE.removeFromWatchNext(mActivity,
                                tmdbId);
                    }
                    // Auto-play Next Episode
                    if (!isMovie && episode != null && similarOrEpisode != null && !similarOrEpisode.isEmpty()) {
                        int currentIndex = -1;
                        for (int i = 0; i < similarOrEpisode.size(); i++) {
                            if (((Episode) similarOrEpisode.get(i)).getId() == episode.getId()) {
                                currentIndex = i;
                                break;
                            }
                        }
                        if (currentIndex != -1 && currentIndex < similarOrEpisode.size() - 1) {
                            Episode nextEp = (Episode) similarOrEpisode.get(currentIndex + 1);
                            Toast.makeText(mActivity, "Playing Next Episode: " + nextEp.getName(), Toast.LENGTH_SHORT)
                                    .show();
                            updateEpisode(tvShowDetails, season, nextEp.getId());
                        }
                    }
                    break;
                case MediaPlayer.Event.Buffering:
                    float buffering = event.getBuffering();
                    if (buffering < 100f) {
                        isBuffering = true;
                        customBufferingIndicator.setVisibility(View.VISIBLE);
                    } else {
                        isBuffering = false;
                        customBufferingIndicator.setVisibility(View.GONE);
                        // Also check for initial seek when buffering finishes
                        if (!initialSeekDone && startPosition > 0) {
                            player.setTime(startPosition);
                            initialSeekDone = true;
                            Log.d(TAG, "PlayerEventListener: Buffering 100%, applying startPosition: " + startPosition);
                        }
                        if (!firebaseResumeChecked) {
                            PlayerUtils.resumePlayerState(getContext(), player, tmdbId);
                            firebaseResumeChecked = true;
                        }
                    }
                    break;
                case MediaPlayer.Event.Vout:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Log.e("VLCError", "An error occurred");
                    break;
            }
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long currentTime = player.getTime();
                long totalTime = player.getLength();
                if (totalTime > 0) {
                    seekBar.setMax((int) totalTime);
                    seekBar.setProgress((int) currentTime);
                    updateTimer(timer, currentTime, totalTime);
                }
                updateWatchNext();

                handler.postDelayed(this, 1000);
                PlayerUtils.load3ads(mActivity, mActivity, player, videoLayout);
            }
        }
    };

    @Override
    public void onUserLeaveHint() {
        handleUserLeaveHint();
    }

    private void handleUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mActivity.enterPictureInPictureMode(pipParams);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            if (customControls != null)
                customControls.setVisibility(View.GONE);
            if (bottomNavigationView != null)
                bottomNavigationView.setVisibility(View.GONE);
            if (topNavigationView != null)
                topNavigationView.setVisibility(View.GONE);
        } else {
            if (!isTVDevice(mActivity)) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if (customControls != null)
                customControls.setVisibility(View.VISIBLE);
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

    private void showSubtitles() {
        if (player == null)
            return;

        if (isTVDevice(mActivity)) {
            SubtitleSideSheetDialogFragment subtitleSheet = SubtitleSideSheetDialogFragment.Companion
                    .newInstance(player);
            subtitleSheet.show(getChildFragmentManager(), "subtitle_side_sheet");
        } else {
            SubtitleBottomSheetDialogFragment subtitleSheet = SubtitleBottomSheetDialogFragment.Companion
                    .newInstance(player);
            subtitleSheet.show(getChildFragmentManager(), "subtitle_bottom_sheet");
        }
    }

    private void showSettings() {
        if (player == null)
            return;

        if (isTVDevice(mActivity)) {
            SettingsSideSheetDialogFragment settingsSheet = SettingsSideSheetDialogFragment.Companion
                    .newInstance(player);
            settingsSheet.show(getChildFragmentManager(), "settings_side_sheet");
        } else {
            SettingsBottomSheetDialogFragment settingsSheet = SettingsBottomSheetDialogFragment.Companion
                    .newInstance(player);
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
        if (player != null && newUrl != null) {
            long currentPos = player.getTime();
            boolean wasPlaying = player.isPlaying();

            // Sanitize URL: Replace spaces with %20
            String sanitizedUrl = newUrl.replace(" ", "%20");

            Media media = new Media(libVLC, Uri.parse(sanitizedUrl));
            applyMediaOptions(media);
            player.setMedia(media);
            media.release();
            player.play(); // Start playback immediately after setting new media

            if (currentPos > 0) {
                PlayerUtils.saveResume(mActivity, currentPos, player.getLength(), tmdbId);
                player.setTime(currentPos);
            }
        }

        // Also ensure we sync resume state for new source if needed
        PlayerUtils.resumePlayerState(requireContext(), player, tmdbId);

        customBufferingIndicator.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.VISIBLE);
    }

    private void applyMediaOptions(Media media) {
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=3000");
        media.addOption(":file-caching=3000");

        // Improve subtitle detection
        media.addOption(":sub-autodetect-file");
        media.addOption(":sub-autodetect-fuzzy=1");

        SharedPreferences settings = mActivity.getSharedPreferences("PlayerSettings", Context.MODE_PRIVATE);
        boolean passthrough = settings.getBoolean("audio_passthrough", false);
        if (passthrough) {
            media.addOption(":audio-passthrough");
            Log.d(TAG, "applyMediaOptions: Audio Passthrough ENABLED");
        } else {
            Log.d(TAG, "applyMediaOptions: Audio Passthrough DISABLED");
        }
    }

    protected void newSource() {
        if (player != null) {
            setAdsState();
            long currentPos = player.getTime();
            Log.d(TAG, "newSource: Saving old position: " + currentPos + " for " + tmdbId);
            PlayerUtils.saveResume(mActivity, currentPos, player.getLength(), tmdbId);
            player.stop();
            player.detachViews();
            player.release();
            player = null;
            if (libVLC != null) {
                libVLC.release();
                libVLC = null;
            }
            stopSeekBarUpdate();
            clearStartPosition(); // CRITICAL: Reset startPosition for the new movie/episode
            customBufferingIndicator.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            Log.d(TAG, "newSource: Old player released, startPosition cleared");
        }
    }

    private void releasePlayer() {
        if (player != null) {
            long currentPos = player.getTime();
            Log.d(TAG, "releasePlayer: Final save for " + tmdbId + " at " + currentPos);
            PlayerUtils.saveResume(mActivity, currentPos, player.getLength(), tmdbId);
            player.stop();
            player.detachViews();
            player.release();
            player = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
        try {
            mActivity.unregisterReceiver(sleepTimerReceiver);
        } catch (Exception ignored) {
        }
        stopSeekBarUpdate();
        destroyAll();
    }

    public void reloadPlayback() {
        if (player != null) {
            long currentPos = player.getTime();
            PlayerUtils.saveResume(mActivity, currentPos, player.getLength(), tmdbId);
            // Stop and release everything
            player.stop();
            player.release();
            player = null;
            if (libVLC != null) {
                libVLC.release();
                libVLC = null;
            }
            stopSeekBarUpdate();

            // Set startPosition to current so it resumes exactly where it was
            this.startPosition = currentPos;

            // Re-initialize
            if (isMovie) {
                loadMovieDetails(itemId);
            } else {
                loadSeriesDetails(episode);
            }
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

        if (episode != null) {
            // Bug 4: Ensure tvShowDetails and season are loaded (essential for navigation
            // from Home)
            if (tvShowDetails == null) {
                tvShowDetails = DetailsUtils.getSeriesDetails(mActivity, (int) episode.getShowId());
            }
            if (season == null) {
                season = DetailsUtils.getSeasonDetails(mActivity, episode.getSeasonId());
            }
        }

        if (tvShowDetails != null) {
            String title = tvShowDetails.getName();
            TVShowSeasonDetails seasonDetails = season != null ? season
                    : (episode != null ? DetailsUtils.getSeasonDetails(mActivity, episode.getSeasonId()) : null);
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
                int seasonNum = (seasonDetails != null) ? seasonDetails.getSeasonNumber() : episode.getSeasonNumber();
                epstitle.setText("Season: " + seasonNum + " Episode: " + episode.getName());
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
                // Load all episodes of this season for auto-play navigation
                if (seasonDetails != null) {
                    similarOrEpisode = (List<MyMedia>) (List<?>) DetailsUtils.getListEpisode(mActivity,
                            (int) tvShowDetails.getId(), seasonDetails.getSeasonNumber());
                }
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
                    player.getTime(),
                    player.getLength());
        }
    }

    public void toggleSideSheetResize(boolean shrink) {
        if (!isTVDevice(mActivity) || playerFrame == null)
            return;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int targetWidth = shrink ? (int) (screenWidth * 0.62) : screenWidth;
        int startWidth = playerFrame.getWidth();

        // Hide controls during resize transition
        if (shrink) {
            customControls.setVisibility(View.GONE);
        }

        ValueAnimator animator = ValueAnimator.ofInt(startWidth, targetWidth);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = playerFrame.getLayoutParams();
            params.width = val;
            params.height = (int) (val / 16.0 * 9.0);
            playerFrame.setLayoutParams(params);
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Show controls again when sidesheet is dismissed
                if (!shrink) {
                    showControls();
                }
            }
        });

        // Cross-fade the player frame alpha for smooth feel
        playerFrame.animate()
                .alpha(shrink ? 0.85f : 1.0f)
                .setDuration(220)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();

        animator.setDuration(280);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.start();
    }

    public void toggleLock() {
        isLocked = !isLocked;
        updateLockUI();
    }

    private void updateLockUI() {
        if (isLocked) {
            btnLock.setImageResource(R.drawable.ic_lock);
            judulUtama.setVisibility(View.INVISIBLE);
            settingContainer.setVisibility(View.INVISIBLE);

            if (middleControls != null)
                middleControls.setVisibility(View.INVISIBLE);
            if (bottomControls != null)
                bottomControls.setVisibility(View.INVISIBLE);

        } else {
            btnLock.setImageResource(R.drawable.ic_unlock);
            judulUtama.setVisibility(View.VISIBLE);
            settingContainer.setVisibility(View.VISIBLE);

            if (middleControls != null)
                middleControls.setVisibility(View.VISIBLE);
            if (bottomControls != null)
                bottomControls.setVisibility(View.VISIBLE);
        }
    }

    private void adjustVolume(float percent) {
        AudioManager audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // Accumulate the scroll delta (percent is typically small)
        volumeAccumulator += (percent * maxVolume * 1.5f); // Sensitivity factor

        int delta = (int) volumeAccumulator;
        if (delta != 0) {
            int newVolume = Math.max(0, Math.min(maxVolume, currentVolume + delta));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI);
            // Deduct the applied delta from the accumulator
            volumeAccumulator -= delta;
        }
    }

    private void adjustBrightness(float percent) {
        Window window = mActivity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        float currentBrightness = layoutParams.screenBrightness;
        if (currentBrightness < 0)
            currentBrightness = 0.5f; // Initial default

        float newBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + (percent * 1.2f)));
        layoutParams.screenBrightness = newBrightness;
        window.setAttributes(layoutParams);
    }

    public void setResizeMode(int mode) {
        this.currentResizeMode = mode;
        if (player != null) {
            switch (mode) {
                case 0: // RESIZE_MODE_FIT (Best Fit - letterbox)
                    player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
                    break;
                case 1: // RESIZE_MODE_STRETCH (Fill entire screen)
                    player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
                    break;
                case 3: // RESIZE_MODE_ZOOM (Original size but scaled up)
                    player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
                    player.setScale(1.3f);
                    break;
                case 4: // RESIZE_MODE_CENTER_CROP
                    player.setVideoScale(MediaPlayer.ScaleType.SURFACE_ORIGINAL);
                    break;
            }
        }
        saveResizeMode(mode);
    }

    private void saveResizeMode(int mode) {
        SharedPreferences.Editor editor = mActivity.getSharedPreferences("PlayerSettings", Context.MODE_PRIVATE).edit();
        editor.putInt("resize_mode", mode);
        editor.apply();
    }

    private void loadResizeMode() {
        SharedPreferences prefs = mActivity.getSharedPreferences("PlayerSettings", Context.MODE_PRIVATE);
        currentResizeMode = prefs.getInt("resize_mode", 0); // Default to FIT
        setResizeMode(currentResizeMode);
    }

    public int getCurrentResizeMode() {
        return currentResizeMode;
    }

    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Handles key events forwarded from MainActivity.dispatchKeyEvent on TV
     * devices.
     * Returns true if the event was consumed (D-pad controls handled here).
     */
    public boolean handleKeyEvent(android.view.KeyEvent event) {
        if (event.getAction() != android.view.KeyEvent.ACTION_DOWN)
            return false;

        boolean controlsVisible = customControls != null && customControls.getVisibility() == View.VISIBLE;

        // Reset hide timer on any key press if controls are visible
        if (controlsVisible) {
            startAutoHideControls();
        }

        switch (event.getKeyCode()) {
            case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
            case android.view.KeyEvent.KEYCODE_ENTER:
                if (controlsVisible) {
                    return false; // Let the focused button handle the click
                } else {
                    toggleControls();
                    return true;
                }
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                if (controlsVisible)
                    return false; // Let native focus handle it
                fastForward(player, ff);
                showControls();
                return true;
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                if (controlsVisible)
                    return false; // Let native focus handle it
                rewind(player, bw);
                showControls();
                return true;
            case android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                fastForward(player, ff);
                startAutoHideControls();
                return true;
            case android.view.KeyEvent.KEYCODE_MEDIA_REWIND:
                rewind(player, bw);
                startAutoHideControls();
                return true;
            case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case android.view.KeyEvent.KEYCODE_SPACE:
                togglePlayback(player, playPauseButton);
                startAutoHideControls();
                return true;
            case android.view.KeyEvent.KEYCODE_INFO:
                showInfoSideSheet();
                startAutoHideControls();
                return true;
        }
        return false;
    }

}
