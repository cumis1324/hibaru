package com.theflexproject.thunder.fragments;

import static android.content.ContentValues.TAG;
import static android.content.Context.DOWNLOAD_SERVICE;
import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.fragments.EpisodeDetailsFragment.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaAspectRatio;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.BannerRecyclerAdapter;
import com.theflexproject.thunder.adapter.FileItemAdapter;
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.DownloadItem;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Genres;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.player.PlayerActivity;
import com.theflexproject.thunder.utils.MovieQualityExtractor;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.sizetoReadablesize;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

// Inside your Application class or MainActivity onCreate method

public class MovieDetailsFragment extends BaseFragment {

    MoreMoviesAdapterr moreMovieRecycler;
    RecyclerView moreMovieView;
    List<Movie> moreMovies, morebyId, moreRecom, movieFileList;
    MoreMoviesAdapterr.OnItemClickListener moreMoviesListener;
    BlurView blurView;
    ViewGroup rootView;

    int movieId;
    String movieFileName;
    ImageView logo, backdrop;
    ImageButton play, changeSource, addToList, download, shareButton;
    Movie movieDetails, largestFile, selectedFile;
    TextView quality;

    List<MyMedia> moreMovieList;
    BottomNavigationView botnav;
    MaterialButton rating;
    RelativeLayout titleLayout;

    // adview

    private Button saweria;

    private RewardedAd rewardedAd;
    FirebaseManager manager;
    private DatabaseReference databaseReference;
    View progressOverlay;
    MaterialTextView title;
    private TemplateView template;

    public MovieDetailsFragment() {

    }

    public MovieDetailsFragment(int id) {
        this.movieId = id;
    }

    public MovieDetailsFragment(String fileName) {
        this.movieFileName = fileName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_movie_details_new, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        botnav = mActivity.findViewById(R.id.bottom_navigation);
        botnav.setVisibility(View.GONE);
        manager = new FirebaseManager();
        WebView webView = view.findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(
                "https://stream.trakteer.id/running-text-default.html?rt_count=5&rt_speed=fast&rt_1_clr1=rgba%280%2C+0%2C+0%2C+1%29&rt_septype=image&rt_txtshadow=true&rt_showsuppmsg=true&creator_name=nfgplus-official&page_url=trakteer.id/nfgplusofficial&mod=3&key=trstream-hV0jDdrlk82mv3aZnzpA&hash=a6z74q7pkgn3mlqy");
        title = view.findViewById(R.id.title3);
        progressOverlay = view.findViewById(R.id.progress_overlay);
        quality = view.findViewById(R.id.fakebutton);
        moreMovieView = view.findViewById(R.id.recyclerEpisodes2);
        saweria = view.findViewById(R.id.saweria);
        template = view.findViewById(R.id.my_template);

        MobileAds.initialize(mActivity);
        loadNative();
        initWidgets(view);
        loadDetails();

    }

    @Override
    public void onDestroyView() {
        botnav.setVisibility(View.VISIBLE);
        super.onDestroyView();
    }

    private void loadReward() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(mActivity, "ca-app-pub-7142401354409440/7652952632",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Ad was loaded.");
                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d(TAG, "Ad was clicked.");
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                rewardedAd = null;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                rewardedAd = null;
                            }

                            @Override
                            public void onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });
                        if (rewardedAd != null) {
                            rewardedAd.show(mActivity, new OnUserEarnedRewardListener() {
                                @Override
                                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                    // Handle the reward.
                                    Log.d(TAG, "The user earned the reward.");
                                    int rewardAmount = rewardItem.getAmount();
                                    String rewardType = rewardItem.getType();
                                    Toast.makeText(getContext(), rewardType + movieDetails.getTitle(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                });

    }

    private void loadNative() {

        AdLoader adLoader = new AdLoader.Builder(mActivity, "ca-app-pub-7142401354409440/7261340471")
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                        template.setStyles(styles);
                        template.setNativeAd(nativeAd);
                    }
                })
                .build();
        template.setVisibility(View.VISIBLE);
        adLoader.loadAd(new AdRequest.Builder().build());

    }

    private void initWidgets(View view) {
        logo = view.findViewById(R.id.movieLogo);

        backdrop = view.findViewById(R.id.movieBackdrop);
        play = view.findViewById(R.id.play);
        play.requestFocus();
        download = view.findViewById(R.id.downloadButton);
        shareButton = view.findViewById(R.id.shareButton);
        addToList = view.findViewById(R.id.addToListButton);
        changeSource = view.findViewById(R.id.changeSourceButton);
        rating = view.findViewById(R.id.ratingsSheet1);
        titleLayout = view.findViewById(R.id.titleLayout);
        // changeTMDB = view.findViewById(R.id.changeTMDBId);

    }

    private void loadDetails() {
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(" ", "in thread");

                    movieFileList = DatabaseClient
                            .getInstance(mActivity)
                            .getAppDatabase()
                            .movieDao()
                            .getAllById(movieId);

                    Log.i("movieId", movieId + "");

                    movieDetails = DatabaseClient
                            .getInstance(mActivity)
                            .getAppDatabase()
                            .movieDao()
                            .byId(movieId);
                    if (movieDetails == null) {
                        movieDetails = DatabaseClient
                                .getInstance(mActivity)
                                .getAppDatabase()
                                .movieDao()
                                .getByFileName(movieFileName);
                    }

                    if (movieDetails != null) {
                        loadMoreMovies();
                        Log.i("insideLoadDetails", movieDetails.toString());
                        mActivity.runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                String titleText = movieDetails.getTitle();
                                String year = movieDetails.getReleaseDate();
                                String yearCrop = year.substring(0, year.indexOf('-'));
                                String deskripsi = movieDetails.getOverview();
                                String ratings = (int) (movieDetails.getVoteAverage() * 10) + "%";
                                String result = StringUtils.runtimeIntegerToString(movieDetails.getRuntime());
                                rating.setText(ratings + " - " + result + " ...Selengkapnya");
                                title.setText(titleText + " (" + yearCrop + ")");
                                rating.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        VideoDetailsBottomSheet bottomSheet = new VideoDetailsBottomSheet(
                                                movieDetails.getId(), true);
                                        bottomSheet.show(mActivity.getSupportFragmentManager(),
                                                "VideoDetailsBottomSheet");
                                    }
                                });
                                titleLayout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        VideoDetailsBottomSheet bottomSheet = new VideoDetailsBottomSheet(
                                                movieDetails.getId(), true);
                                        bottomSheet.show(mActivity.getSupportFragmentManager(),
                                                "VideoDetailsBottomSheet");
                                    }
                                });

                                String tmdbId = String.valueOf(movieDetails.getId());
                                databaseReference = FirebaseDatabase.getInstance().getReference("History/");
                                String userId = manager.getCurrentUser().getUid();
                                DatabaseReference userReference = databaseReference.child(userId).child(tmdbId)
                                        .child("lastPosition");
                                DatabaseReference lastP = databaseReference.child(userId).child(tmdbId)
                                        .child("lastPlayed");
                                userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            Long lastPosition = dataSnapshot.getValue(Long.class);
                                            if (lastPosition != null) {
                                                long runtime = (long) movieDetails.getRuntime() * 60 * 1000;
                                                double progress = (double) lastPosition / runtime;
                                                int progressWidth = (int) (backdrop.getWidth() * progress);
                                                progressOverlay.getLayoutParams().width = progressWidth;
                                                progressOverlay.requestLayout();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle onCancelled event
                                    }
                                });

                                // Listener for lastPlayed
                                lastP.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            String lastPlayed = dataSnapshot.getValue(String.class);
                                            if (lastPlayed != null) {
                                                DateTimeFormatter formatter = DateTimeFormatter
                                                        .ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                                                String currentDateTime = ZonedDateTime
                                                        .now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
                                                // Update the played field in your local database asynchronously
                                                AsyncTask.execute(() -> {
                                                    DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao()
                                                            .updatePlayed(movieDetails.getId(), lastPlayed + " added");
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        // Handle onCancelled event
                                    }
                                });
                                quality.setText(movieDetails.getOriginalTitle());
                                quality.setVisibility(View.VISIBLE);

                                String logoLink = movieDetails.getLogoPath();
                                System.out.println("Logo Link" + logoLink);

                                if (logoLink != null && !logoLink.equals("")) {
                                    logo.setVisibility(View.VISIBLE);
                                    Glide.with(mActivity)
                                            .load(logoLink)
                                            .apply(new RequestOptions()
                                                    .fitCenter()
                                                    .override(Target.SIZE_ORIGINAL))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                                            .into(logo);
                                }
                                if (logoLink != null && logoLink.equals("") && movieDetails.getTitle() != null) {
                                    logo.setVisibility(View.GONE);
                                }

                                if (movieDetails.getBackdropPath() != null) {
                                    //

                                    Glide.with(mActivity)
                                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getBackdropPath())
                                            .apply(new RequestOptions()
                                                    .fitCenter()
                                                    .override(Target.SIZE_ORIGINAL))
                                            // .apply(bitmapTransform(new BlurTransformation(5, 3)))
                                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(backdrop);
                                } else {
                                    if (movieDetails.getPosterPath() != null) {
                                        Glide.with(mActivity)
                                                .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPosterPath())
                                                .apply(new RequestOptions()
                                                        .fitCenter()
                                                        .override(Target.SIZE_ORIGINAL))
                                                // .apply(bitmapTransform(new BlurTransformation(5, 3)))
                                                .placeholder(new ColorDrawable(Color.TRANSPARENT))
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .into(backdrop);
                                    }
                                }

                            }
                        });
                    }

                }
            });
            thread.start();
        } catch (NullPointerException exception) {
            Log.i("Error", exception.toString());
        }
        setMyOnClickListeners();
    }

    String urlLogo;

    // to blur the backdrop
    void blurBottom() {

        ((Activity) mActivity).getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ((Activity) mActivity).getWindow().setStatusBarColor(Color.TRANSPARENT);
        final float radius = 14f;
        final Drawable windowBackground = ((Activity) mActivity).getWindow().getDecorView().getBackground();

        blurView.setupWith(rootView, new RenderScriptBlur(mActivity))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);
    }

    private void loadMoreMovies() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                morebyId = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getmorebyid(movieId);
                moreMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getMoreMovied();
                moreRecom = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getrecomendation(10, 0);
                moreMovieList = new ArrayList<>();
                moreMovieList.addAll(morebyId);
                moreMovieList.addAll(moreMovies);
                moreMovieList.addAll(moreRecom);
                if (moreMovieList != null && moreMovieList.size() > 0) {
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            moreMovieView.setVisibility(View.VISIBLE);
                            ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(
                                    getContext(), LinearLayoutManager.VERTICAL, false);
                            moreMovieView.setLayoutManager(linearLayoutManager);
                            moreMovieView.setHasFixedSize(true);
                            moreMovieRecycler = new MoreMoviesAdapterr(mActivity,
                                    (List<MyMedia>) (List<?>) moreMovieList, moreMoviesListener);
                            moreMovieView.setAdapter(moreMovieRecycler);
                            moreMovieRecycler.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        thread.start();

    }

    private void setMyOnClickListeners() {

        saweria.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://trakteer.id/nfgplusofficial/tip"))));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                largestFile = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .byIdLargest(movieId);
            }
        });
        thread.start();

        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING", false);

        if (savedEXT) {
            // External Player
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(largestFile.getUrlString()));
                    intent.setDataAndType(Uri.parse(largestFile.getUrlString()), "video/*");
                    startActivity(intent);
                    loadReward();
                }
            });
        } else {
            // Play video
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        CustomFileListDialogFragment dialog = new CustomFileListDialogFragment(mActivity, changeSource,
                                (List<MyMedia>) (List<?>) movieFileList);

                        dialog.show(mActivity.getSupportFragmentManager(), "CustomFileListDialogFragment");
                        dialog.mOnInputListener = new CustomFileListDialogFragment.OnInputListener() {
                            @Override
                            public void sendInput(int selection) {
                                selectedFile = movieFileList.get(selection);
                                String huntu = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());
                                System.out.println("selected file" + selectedFile.getFileName());
                                Intent in = new Intent(getActivity(), PlayerActivity.class);
                                in.putExtra("url", selectedFile.getUrlString());
                                in.putExtra("title", selectedFile.getTitle());
                                String tmdbId = String.valueOf(selectedFile.getId());
                                in.putExtra("tmdbId", tmdbId);
                                String inYear = selectedFile.getReleaseDate();
                                in.putExtra("year", inYear.substring(0, inYear.indexOf('-')));
                                startActivity(in);
                                Toast.makeText(getContext(), "Playing " + movieDetails.getTitle() + " " + huntu,
                                        Toast.LENGTH_LONG).show();
                                Toast.makeText(mActivity, selectedFile.getTitle() + huntu + " Selected",
                                        Toast.LENGTH_LONG).show();

                            }
                        };

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Share button click listener

        // Start download
        download.setOnClickListener(new View.OnClickListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 32) {
                    // Check if the app has the WRITE_EXTERNAL_STORAGE permission
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Request the permission if it is not granted
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
                    } else {
                        // Permission is already granted, proceed with the download
                        startDownload();
                    }
                } else {
                    // Permission is already granted, proceed with the download
                    startDownload();
                }
            }

            private void startDownload() {
                CustomFileListDialogFragment downdialog = new CustomFileListDialogFragment(mActivity, changeSource,
                        (List<MyMedia>) (List<?>) movieFileList);

                downdialog.show(mActivity.getSupportFragmentManager(), "CustomFileListDialogFragment");
                downdialog.mOnInputListener = new CustomFileListDialogFragment.OnInputListener() {
                    @Override
                    public void sendInput(int selection) {
                        selectedFile = movieFileList.get(selection);
                        String huntu = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());
                        System.out.println("selected file" + selectedFile.getFileName());

                        String customFolderPath = "/nfgplus/movies/";
                        DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse(selectedFile.getUrlString());
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                                customFolderPath + selectedFile.getFileName());
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                .setTitle(selectedFile.getTitle())
                                .setVisibleInDownloadsUi(true)
                                .setDescription("Downloading " + selectedFile.getTitle() + " " + huntu);
                        long downloadId = manager.enqueue(request);
                        List<DownloadItem> downloadItems = new ArrayList<>();
                        downloadItems.add(new DownloadItem(
                                selectedFile.getFileName(),
                                downloadId,
                                selectedFile.getTitle(),
                                -1,
                                0));
                        Toast.makeText(getContext(), "Download Started", Toast.LENGTH_LONG).show();
                    }
                };
            }
        });

        addToList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

            }
        });

        final int[] checkedItem = { -1 };
        int indexSelected = 0;
        changeSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomFileListDialogFragment dialog = new CustomFileListDialogFragment(mActivity, changeSource,
                        (List<MyMedia>) (List<?>) movieFileList);

                dialog.show(mActivity.getSupportFragmentManager(), "CustomFileListDialogFragment");
                dialog.mOnInputListener = new CustomFileListDialogFragment.OnInputListener() {
                    @Override
                    public void sendInput(int selection) {
                        selectedFile = movieFileList.get(selection);
                        String huntu = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());
                        System.out.println("selected file" + selectedFile.getFileName());
                        Toast.makeText(mActivity, selectedFile.getTitle() + huntu + " Selected", Toast.LENGTH_LONG)
                                .show();

                    }
                };
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String itemId = String.valueOf(movieDetails.getId()); // Replace with the actual item ID
                generateAndShareDynamicLink(itemId);

            }
        });
        moreMoviesListener = new MoreMoviesAdapterr.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                Movie more = ((Movie) moreMovieList.get(position));
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(more.getId());
                mActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.container, movieDetailsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        };
    }

    // Function to save poster image to a temporary file

    private void generateAndShareDynamicLink(String itemId) {
        // Set up the dynamic link components
        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://nfgplus.page.link/share/" + itemId))
                .setDomainUriPrefix("https://nfgplus.page.link/share/")
                .buildDynamicLink();

        // Generate the short dynamic link
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(dynamicLink.getUri())
                .buildShortDynamicLink()
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Short link created successfully
                        Uri shortLink = task.getResult().getShortLink();
                        // Now you can use the 'shortLink' to share with others
                        shareDynamicLink(shortLink.toString());
                    } else {
                        // Handle the error
                        Exception e = task.getException();
                        // Log or display an error message
                        // For now, you can still proceed with the long link if short link creation
                        // fails
                        shareDynamicLink(dynamicLink.getUri().toString());
                    }
                });
    }

    // Method to share the dynamic link
    private void shareDynamicLink(String dynamicLink) {
        // Create a share intent
        String title = movieDetails.getTitle();
        String originalTitle = movieDetails.getOriginalTitle();
        String overview = movieDetails.getOverview();
        String posterPath = "https://image.tmdb.org/t/p/w500" + movieDetails.getPosterPath();
        String movieId = String.valueOf(movieDetails.getId());

        // Tautan deep link lengkap
        String deepLink = "https://nfgplus.my.id/reviews.html?id=" + movieId + "&type=movie";

        // Menyusun teks yang ingin dibagikan
        String shareText = title + "\n" +
                "Judul Asli: " + originalTitle + "\n" +
                "Deskripsi: " + overview + "\n" +
                deepLink + "\n";

        // Membuat Intent untuk membagikan konten
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain"); // Menggunakan teks biasa

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);

        startActivity(Intent.createChooser(shareIntent, "Bagikan " + title));

    }

    private void addToLastPlayed() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });
        thread.start();
    }

}
