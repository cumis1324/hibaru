package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.player.PlayerListener.fastForward;
import static com.theflexproject.thunder.player.PlayerListener.rewind;
import static com.theflexproject.thunder.player.PlayerListener.showSettingsDialog;
import static com.theflexproject.thunder.player.PlayerListener.showSubtitleSelectionDialog;
import static com.theflexproject.thunder.player.PlayerListener.togglePlayback;
import static com.theflexproject.thunder.player.PlayerUtils.createMediaSourceFactory;
import static com.theflexproject.thunder.player.PlayerUtils.enterFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.exitFullscreen;
import static com.theflexproject.thunder.player.PlayerUtils.isTVDevice;
import static com.theflexproject.thunder.player.PlayerUtils.lastPositionListener;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.widget.NestedScrollView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
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
    private String tmdbId, userId, urlString;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImageButton playPauseButton, setting, fullscreen, cc, ff, bw, source;
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
    private NavigationRailView navigationRailView;
    private ViewGroup rootView;
    private Spinner spinnerAudioTrack, spinnerSource;
    private List<MyMedia> sourceList, similarOrEpisode;
    private GestureDetector gestureDetector;
    PictureInPictureParams pipParams;
    View dialogView;
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

    private View decorView;
    private Intent intent;
    private RandomIndex loadBalancer = new RandomIndex();
    private String randomUrl;
    private static final String ADMIN_USER_UID = "M20Oxpp64gZ480Lqus4afv6x2n63";
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
            loadMovieDetails(itemId);
        } else {
            episode = DetailsUtils.getNextEpisode(mActivity, episodeId);
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
        setting = customControls.findViewById(R.id.btn_setting);
        imageView = view.findViewById(R.id.background_image);
        customBufferingIndicator = view.findViewById(R.id.custom_buffering_indicator);
        ff = customControls.findViewById(R.id.btn_ff);
        bw = customControls.findViewById(R.id.btn_bw);
        source = customControls.findViewById(R.id.btn_src);
        bottomNavigationView = mActivity.findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(View.GONE);
        Rational aspectRatio = new Rational(16, 9);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pipParams = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }
        similarView = view.findViewById(R.id.similarAndEpisode);
        if (isTVDevice(mActivity)) {
            navigationRailView = mActivity.findViewById(R.id.side_navigation);
            navigationRailView.setVisibility(View.GONE);
            fullscreen.setVisibility(View.GONE);
        }
        exitFullscreen(mActivity, playerFrame, movietitle, fullscreen);

    }

    private void setControlListeners() {
        source.setOnClickListener(v -> showSources());
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
            loadReward();
        }
    }

    private void loadReward() {
        UnityAdHelper.INSTANCE.showRewardedAd(mActivity, new UnityAdHelper.AdCallback() {
            @Override
            public void onAdComplete() {
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
            }

            @Override
            public void onAdFailed() {
                if (player != null) {
                    player.setPlayWhenReady(true);
                }
            }
        });
        if (player != null) {
            player.setPlayWhenReady(false);
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
            bottomNavigationView.setVisibility(View.GONE);
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
                handler.postDelayed(this, 1000); // Update
                                                 // setiap
                                                 // detik
                if (!isSubscribed) {
                    // Unity Ads are handled in loadReward() or elsewhere
                }
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
            cc.setOnClickListener(v -> showSubtitleSelectionDialog(mActivity, mappedTrackInfo, trackSelector));
            cc.setImageResource(subOn(trackSelector) ? R.drawable.ic_cc : R.drawable.ic_cc_filled);
            setting.setOnClickListener(v -> loadSetting());
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
                mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                cc.setOnClickListener(v -> showSubtitleSelectionDialog(mActivity, mappedTrackInfo, trackSelector));
                startSeekBarUpdate();
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);
                ff.setOnClickListener(v -> fastForward(player, ff));
                bw.setOnClickListener(v -> rewind(player, bw));
                if (isTVDevice(mActivity)) {
                    movietitle.setVisibility(View.VISIBLE);
                }

                playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
            }
            if (playbackState != Player.STATE_READY) {
                customBufferingIndicator.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
            }
            if (playbackState == Player.STATE_ENDED) {
                imageView.setVisibility(View.GONE);
                customBufferingIndicator.setVisibility(View.GONE);

            }
            if (playbackState == Player.STATE_BUFFERING) {

                // Tampilkan indikator buffering
                customBufferingIndicator.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
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

    private void loadSetting() {
        showSettingsDialog(mActivity, spinnerAudioTrack, spinnerSource, dialogView, trackSelector, mappedTrackInfo,
                player);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Select Source");
        List<String> sourcesOptions = new ArrayList<>();
        for (MyMedia source : sourceList) {
            if (source instanceof Movie) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Movie) source).getFileName());
                sourcesOptions.add(qualityStr);
            } else if (source instanceof Episode) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Episode) source).getFileName());
                sourcesOptions.add(qualityStr);
            } else {
                sourcesOptions.add("Unknown Source"); // Penanganan untuk tipe lain
            }
        }
        final int[] selectedIndex = { 0 };
        builder.setSingleChoiceItems(sourcesOptions.toArray(new String[0]), selectedIndex[0], (dialog, which) -> {
            selectedIndex[0] = which; // Simpan indeks pilihan terbaru
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            MyMedia selectedSource = sourceList.get(selectedIndex[0]);
            if (selectedSource instanceof Movie) {
                String selectedUrl2 = ((Movie) selectedSource).getUrlString();
                String newUrl = selectedUrl2.replaceAll(
                        "drive\\d*\\.nfgplusmirror\\.workers.dev",
                        randomUrl);
                if (!Objects.equals(selectedUrl2, urlString)) {
                    newSource();
                    new Handler(Looper.getMainLooper()).post(() -> initializePlayer(newUrl));
                }
            } else if (selectedSource instanceof Episode) {
                String selectedUrl2 = ((Episode) selectedSource).getUrlString();
                String newUrl = selectedUrl2.replaceAll(
                        "drive\\d*\\.nfgplusmirror\\.workers.dev",
                        randomUrl);
                if (!Objects.equals(selectedUrl2, urlString)) {
                    newSource();
                    new Handler(Looper.getMainLooper()).post(() -> initializePlayer(newUrl));
                }
            }
        });
        builder.create().show();

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
        bottomNavigationView.setVisibility(View.VISIBLE);

        if (isTVDevice(mActivity)) {
            navigationRailView.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.GONE);

        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(final int movieId) {
        Movie movieDetails = DetailsUtils.getMovieSmallest(mActivity, movieId);
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
            mActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new DetailFragment(movieId, true))
                    .commit();
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
            if (ADMIN_USER_UID.equals(userId)) {
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

            if (episode != null) {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container, new DetailFragment(tvShowDetails, seasonDetails, episode))
                        .commit();
                epstitle.setText("Season: " + seasonDetails.getSeasonNumber()
                        + " Episode: " + episode.getName());
                epstitle.setVisibility(View.VISIBLE);
                tmdbId = String.valueOf(episode.getId());
                urlString = episode.getUrlString();
                String newUrl = urlString.replaceAll(
                        "drive\\d*\\.nfgplusmirror\\.workers.dev",
                        randomUrl);
                if (ADMIN_USER_UID.equals(userId)) {
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

}
