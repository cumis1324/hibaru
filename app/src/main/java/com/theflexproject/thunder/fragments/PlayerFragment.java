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

import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentTransaction;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.adapter.SimilarAdapter;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.MovieQualityExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class PlayerFragment extends BaseFragment implements PlayerControlView.VisibilityListener {

    private static final String TAG = "PlayerFragment", OFFLINE = "offline",
            HISTORY_PATH = "History/", LAST_POSITION = "lastPosition",
            KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters", KEY_ITEM_INDEX = "item_index",
            KEY_POSITION = "position", KEY_AUTO_PLAY = "auto_play";

    private int itemId;
    private boolean isMovie;
    private FirebaseManager manager;
    private DatabaseReference databaseReference;
    private String tmdbId, userId, urlString;

    private ExoPlayer player;
    private PlayerView playerView;
    private ImageButton playPauseButton, setting, fullscreen, cc, ff, bw, source;
    private SeekBar seekBar;
    private TextView timer, movietitle, epstitle;

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

    public PlayerFragment() {
        // Default constructor
    }

    public PlayerFragment(int itemId, boolean isMovie) {
        this.itemId = itemId;
        this.isMovie = isMovie;
    }

    // Konstruktor untuk Series
    public PlayerFragment(TVShow tvShowDetails, TVShowSeasonDetails seasonDetails, int episodeId) {
        this.isMovie = false;
        this.tvShowDetails = tvShowDetails;
        this.season = seasonDetails;
        this.episodeId = episodeId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isTVDevice(mActivity)){
            View view = inflater.inflate(R.layout.video_tv, container, false);
            initFirebase();
            initViews(view);
            navigationRailView = mActivity.findViewById(R.id.side_navigation);
            navigationRailView.setVisibility(View.GONE);
            fullscreen.setVisibility(View.GONE);
            initPlayerState(savedInstanceState);
            if (isMovie) {
                loadMovieDetails(itemId);
            }
            setControlListeners();
            movietitle.setVisibility(View.VISIBLE);
            return view;
        }else {
            View view = inflater.inflate(R.layout.video_player, container, false);
            initFirebase();
            initViews(view);
            initPlayerState(savedInstanceState);
            if (isMovie) {
                loadMovieDetails(itemId);
            }else {
                loadSeriesDetails(itemId);

            }
            setControlListeners();
            return view;
        }
    }
    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(final int movieId) {
        Movie movieDetails = DetailsUtils.getMovieSmallest(mActivity, movieId);
        if (movieDetails != null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getRelease_date();
            urlString = movieDetails.getUrlString();
            String yearCrop = year.substring(0,year.indexOf('-'));
            tmdbId = String.valueOf(movieDetails.getId());
            movietitle.setText(titleText + " ("+yearCrop+")");
            mActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new DetailFragment(movieId, true))
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
            initializePlayer(urlString);
        }
        sourceList = (List<MyMedia>)(List<?>)DetailsUtils.getSourceList(mActivity, movieId);
        loadSimilar(movieId);
    }

    private void loadSimilar(int id) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                List<Movie> similarMovie = DetailsUtils.getSimilarMovies(mActivity, id);
                List<Movie> recommendationMovie = DetailsUtils.getRecommendationMovies(mActivity, id);
                if (similarMovie!=null){
                    movieListener();
                    similarOrEpisode = new ArrayList<>();
                    similarOrEpisode.addAll(similarMovie);
                    similarOrEpisode.addAll(recommendationMovie);
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            similarView.setVisibility(View.VISIBLE);
                            ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                            similarView.setLayoutManager(linearLayoutManager);
                            SimilarAdapter moreMovieRecycler = new SimilarAdapter(mActivity, (List<MyMedia>) (List<?>) similarOrEpisode, similarListener);
                            similarView.setAdapter(moreMovieRecycler);
                            moreMovieRecycler.notifyDataSetChanged();

                        }
                    });
                }
            }});
        thread.start();
    }
    private void movieListener() {
        similarListener = new SimilarAdapter.OnItemClickListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onClick(View view, int position) {
                if (similarOrEpisode.get(position) instanceof Movie){
                    Movie movie = (Movie) similarOrEpisode.get(position);
                    String url = movie.getUrlString();
                    int movieId = movie.getId();
                    if (!Objects.equals(url, urlString)) {
                        initializePlayer(url);
                    }
                    newSource();
                    loadMovieDetails(movieId);
                }

            }
        };
    }

    @SuppressLint("SetTextI18n")
    private void loadSeriesDetails(int itemId) {

        if (tvShowDetails!=null){
            String title = tvShowDetails.getName();
            int seasonId = season.getId();
            TVShowSeasonDetails seasonDetails = DetailsUtils.getSeasonDetails(mActivity, seasonId);
            movietitle.setText(title);
            Episode nextEpisode = DetailsUtils.getNextEpisode(mActivity, episodeId);
            epstitle.setText("Season "+seasonDetails.getSeason_number()
                    +" Episode "+ nextEpisode.getEpisode_number());
            tmdbId = String.valueOf(nextEpisode.getId());
            urlString = nextEpisode.getUrlString();
            Toast.makeText(mActivity, "url eps "+urlString, Toast.LENGTH_SHORT).show();
            if(tvShowDetails.getBackdrop_path()!=null) {
                Glide.with(mActivity)
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getBackdrop_path())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            }else {
                if(tvShowDetails.getPoster_path()!=null) {
                    Glide.with(mActivity)
                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getPoster_path())
                            .apply(new RequestOptions()
                                    .fitCenter()
                                    .override(Target.SIZE_ORIGINAL))
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                }
            }
            initializePlayer(urlString);
            sourceList = (List<MyMedia>)(List<?>)DetailsUtils.getEpisodeSource(mActivity, nextEpisode.getId());
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
        bottomNavigationView.setVisibility(View.GONE);
        imageView = view.findViewById(R.id.background_image);
        customBufferingIndicator = view.findViewById(R.id.custom_buffering_indicator);
        ff = customControls.findViewById(R.id.btn_ff);
        bw = customControls.findViewById(R.id.btn_bw);
        source = customControls.findViewById(R.id.btn_src);
        Rational aspectRatio = new Rational(16, 9);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pipParams = new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();
        }
        similarView = view.findViewById(R.id.similarAndEpisode);


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
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        gestureDetector = new GestureDetector(mActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.d("GestureDetector", "Single Tap detected");
                return true; // Menangani tap tunggal (opsional)
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d("GestureDetector", "Double Tap detected");
                // Tentukan area tap: kiri untuk rewind, kanan untuk fast forward
                float screenWidth = playerView.getWidth();
                if (e.getX() < screenWidth / 2) {
                    Log.d("GestureDetector", "Double Tap Left - Rewind");
                    rewind(player, bw);
                } else {
                    Log.d("GestureDetector", "Double Tap Right - Fast Forward");
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
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("DetailFragment", "onDestroyView called");
        releasePlayer();
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
            cc.setImageResource(subOn(trackSelector) ? R.drawable.ic_cc : R.drawable.ic_cc_filled);
            setting.setOnClickListener(v -> loadSetting());
            Player.Listener.super.onTracksChanged(tracks);
        }
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            playPauseButton.requestFocus();
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
                ff.setOnClickListener(v -> fastForward(player, ff));
                bw.setOnClickListener(v -> rewind(player, bw));
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
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
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

        // Membuat daftar opsi kualitas untuk ditampilkan
        List<String> sourcesOptions = new ArrayList<>();
        for (MyMedia source : sourceList) {
            if (source instanceof Movie) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Movie) source).getFileName());
                sourcesOptions.add(qualityStr);
            }else if (source instanceof Episode) {
                String qualityStr = MovieQualityExtractor.extractQualtiy(((Episode) source).getFileName());
                sourcesOptions.add(qualityStr);
            } else {
                sourcesOptions.add("Unknown Source"); // Penanganan untuk tipe lain
            }
        }
        builder.setItems(sourcesOptions.toArray(new String[0]), (dialog, which) -> {
            MyMedia selectedSource = sourceList.get(which);
            if (selectedSource instanceof Movie) {
                String selectedUrl = ((Movie) selectedSource).getUrlString();
                if (!Objects.equals(selectedUrl, urlString)) {
                    newSource();
                    new Handler(Looper.getMainLooper()).post(() -> initializePlayer(selectedUrl));
                }
            }
            if (selectedSource instanceof Episode) {
                String selectedUrl = ((Episode) selectedSource).getUrlString();
                if (!Objects.equals(selectedUrl, urlString)) {
                    newSource();
                    new Handler(Looper.getMainLooper()).post(() -> initializePlayer(selectedUrl));
                }
            }
        });

        builder.create().show();
    }
    protected void newSource() {
        if (player != null) {
            saveResume(player, tmdbId);
            player.release();
            player = null;
            playerView.setPlayer(/* player= */ null);
            stopSeekBarUpdate();
            customBufferingIndicator.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }
    }

}
