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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
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
import com.theflexproject.thunder.model.Genre;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.player.PlayerActivity;
import com.theflexproject.thunder.utils.MovieQualityExtractor;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.sizetoReadablesize;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;


// Inside your Application class or MainActivity onCreate method


public class MovieDetailsFragment extends BaseFragment{

    BannerRecyclerAdapter moreRecomRecycler;
    MoreMoviesAdapterr moreMovieRecycler;
    TextView moreMoviesTitle;
    RecyclerView moreRecomView;
    RecyclerView moreMovieView;
    List<Movie> moreMovies;
    List<Movie> morebyId;
    List<Movie> moreRecom;
    MoreMoviesAdapterr.OnItemClickListener moreMoviesListener;
    BannerRecyclerAdapter.OnItemClickListener moreRecomListener;
    BlurView blurView;
    ViewGroup rootView;
    View decorView;


    int movieId;
    String movieFileName;
    ImageView logo;
    TextView titleText;
    TextView yearText;
    TextView runtime;
    ImageButton play;
    ImageButton changeSource;
    ImageButton addToList;
    ImageButton download;
    TextView overview;
//    Button changeTMDB;
//    TextView size;
//    Button externalPlayer;


    TableRow director;
    TableRow writer;
    TableRow genres;




    TextView directorText;
    TextView writerText;
    TextView genresText;
    TextView ratingsText;
    ImageView dot1;
    ImageView ratings;

    ImageView backdrop;
    Movie movieDetails;
    TextView size;
    TextView quality;
    ImageButton shareButton;


    List<Movie> movieFileList;
    List<MyMedia> moreMovieList;
    FileItemAdapter fileAdapter;
    FileItemAdapter.OnItemClickListener listenerFileItem;
    Movie largestFile;
    Movie selectedFile;


    //adview
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741";
    private AdView adView;
    private FrameLayout adContainerView;
    private RelativeLayout relativeContainer;

    private ImageButton saweria;
    private ImageButton paypal;
    private ImageButton dana;
    private ImageButton spay;
    private TemplateView template;
    private InterstitialAd mInterstitialAd;
    private RewardedAd rewardedAd;



    public MovieDetailsFragment() {
        // Required empty public constructor
    }
    public MovieDetailsFragment(int id){
        this.movieId =id;
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

        size = view.findViewById(R.id.sizeTextInFileItem1);
        quality = view.findViewById(R.id.videoQualityTextInFileItem1);
        relativeContainer = view.findViewById(R.id.relativeContainer);
        moreMovieView = view.findViewById(R.id.recyclerEpisodes2);
        saweria = view.findViewById(R.id.downloadButton2);
        paypal = view.findViewById(R.id.changeSourceButton2);
        dana = view.findViewById(R.id.addToListButton2);
        spay = view.findViewById(R.id.shareButton2);
        template = view.findViewById(R.id.my_template);
        MobileAds.initialize(mActivity);
        loadNative();
        initWidgets(view);
        loadDetails();

    }

    private void loadReward(){
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
                                    Toast.makeText(getContext(), rewardType + movieDetails.getTitle(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }

                });

    }
    private void loadAds(){
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(mActivity, "ca-app-pub-7142401354409440/5207281951", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");

                        // Set full-screen content callback
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
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
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                mInterstitialAd = null;
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

                        // Show the ad
                        if (mInterstitialAd != null) {
                            mInterstitialAd.show(mActivity);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private void loadNative() {

        AdLoader adLoader = new AdLoader.Builder(mActivity, "ca-app-pub-7142401354409440/7261340471")
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        NativeTemplateStyle styles = new
                                NativeTemplateStyle.Builder().build();
                        template.setStyles(styles);
                        template.setNativeAd(nativeAd);
                    }
                })
                .build();
        template.setVisibility(View.VISIBLE);
        adLoader.loadAd(new AdRequest.Builder().build());
        
    }


    private void initWidgets(View view) {
        titleText = view.findViewById(R.id.title_text);
        logo = view.findViewById(R.id.movieLogo);
        yearText = view.findViewById(R.id.year_text);
        runtime = view.findViewById(R.id.RuntimeText);
        overview = view.findViewById(R.id.overviewdesc);
        backdrop = view.findViewById(R.id.movieBackdrop);
        director = view.findViewById(R.id.Director);
        writer = view.findViewById(R.id.WrittenBy);
        genres = view.findViewById(R.id.Genres);
        directorText = view.findViewById(R.id.DirectorText);
        writerText = view.findViewById(R.id.WrittenByText);
        genresText = view.findViewById(R.id.GenresText);
        ratings = view.findViewById(R.id.ratings);
        dot1 = view.findViewById(R.id.dot);
        ratingsText = view.findViewById(R.id.ratingsText);
        play = view.findViewById(R.id.play);
        download = view.findViewById(R.id.downloadButton);
        shareButton = view.findViewById(R.id.shareButton);
        addToList = view.findViewById(R.id.addToListButton);
        changeSource = view.findViewById(R.id.changeSourceButton);
//        changeTMDB = view.findViewById(R.id.changeTMDBId);


    }

    private void loadDetails(){
       try {
           Thread thread = new Thread(new Runnable() {
               @Override
               public void run() {
                   Log.i(" " , "in thread");

                       movieFileList = DatabaseClient
                               .getInstance(mActivity)
                               .getAppDatabase()
                               .movieDao()
                               .getAllById(movieId);

                   Log.i("movieId",movieId+"");

                   movieDetails = DatabaseClient
                           .getInstance(mActivity)
                           .getAppDatabase()
                           .movieDao()
                           .byId(movieId);
                   if(movieDetails==null){
                       movieDetails = DatabaseClient
                               .getInstance(mActivity)
                               .getAppDatabase()
                               .movieDao()
                               .getByFileName(movieFileName);
                   }

                   if(movieDetails!=null){
                       loadMoreMovies();
                       Log.i("insideLoadDetails",movieDetails.toString());
                       mActivity.runOnUiThread(new Runnable() {
                           @Override
                           public void run() {

                               size.setText(sizetoReadablesize.humanReadableByteCountBin(Long.parseLong(((Movie) movieDetails).getSize())));

                               String qualityStr = MovieQualityExtractor.extractQualtiy(((Movie) movieDetails).getFileName());
                               if (qualityStr != null) {
                                   quality.setVisibility(View.VISIBLE);
                                   quality.setText(qualityStr);
                               }

                               String logoLink = movieDetails.getLogo_path();
                               System.out.println("Logo Link"+logoLink);

                               if(logoLink!=null && !logoLink.equals("")){
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
                               if(logoLink!=null && logoLink.equals("") && movieDetails.getTitle()!=null){
                                   titleText.setVisibility(View.VISIBLE);
                                   titleText.setText(movieDetails.getTitle());
                               }else {
                                   titleText.setVisibility(View.VISIBLE);
                                   titleText.setText(movieFileName);
                               }

                               if(movieDetails.getRuntime()>0){
                                   String result = StringUtils.runtimeIntegerToString(movieDetails.getRuntime());
                                   runtime.setVisibility(View.VISIBLE);
                                   runtime.setText(result);
                               }
                               if(movieDetails.getGenres()!=null){
                                   genresText.setVisibility(View.VISIBLE);
                                   ArrayList<Genre> genres = movieDetails.getGenres();
                                   StringBuilder sb = new StringBuilder();
                                   for (int i=0;i<genres.size();i++) {
                                       Genre genre = genres.get(i);
                                       if(i == genres.size()-1 && genre != null){sb.append(genre.getName());}
                                       else if(genre!= null){sb.append(genre.getName()).append(", ");}
                                   }
                                   genresText.setText(sb.toString());
                               }
                               if(movieDetails.getVote_average()!=0){
//                                   ratings.setVisibility(View.VISIBLE);
                                   dot1.setVisibility(View.VISIBLE);
                                   ratingsText.setVisibility(View.VISIBLE);
                                   String rating = (int)(movieDetails.getVote_average()*10)+"%";
                                   ratingsText.setText(rating);
                               }
                               String year = movieDetails.getRelease_date();
                               if(movieDetails.getRelease_date()!=null && movieDetails.getRelease_date().length()>1) {
                                   yearText.setVisibility(View.VISIBLE);
                                   yearText.setText(year.substring(0,year.indexOf('-')));
                               }
                               if(movieDetails.getOverview()!=null){overview.setVisibility(View.VISIBLE); overview.setText(movieDetails.getOverview());}
                               if(movieDetails.getPoster_path()!=null) {
//                                           Glide.with(mActivity)
//                                                   .load(TMDB_IMAGE_BASE_URL + movieDetails.getPoster_path())
//                                                   .diskCacheStrategy(DiskCacheStrategy.ALL)
//                                                   .placeholder(new ColorDrawable(Color.TRANSPARENT))
//                                                   .into(poster);
                               }
                               if(movieDetails.getBackdrop_path()!=null) {
//                                   Glide.with(mActivity)
//                                           .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getBackdrop_path())
//                                           .apply(bitmapTransform(new BlurTransformation(10, 3)))
//                                           .diskCacheStrategy(DiskCacheStrategy.ALL)
//                                           .into(backdrop);

                                           Glide.with(mActivity)
                                                   .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getBackdrop_path())
                                                   .apply(new RequestOptions()
                                                           .fitCenter()
                                                           .override(Target.SIZE_ORIGINAL))
//                                                   .apply(bitmapTransform(new BlurTransformation(5, 3)))
                                                   .placeholder(new ColorDrawable(Color.TRANSPARENT))
                                                   .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                   .into(backdrop);
                               }else {
                                   if(movieDetails.getPoster_path()!=null) {
                                               Glide.with(mActivity)
                                                       .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPoster_path())
                                                       .apply(new RequestOptions()
                                                               .fitCenter()
                                                               .override(Target.SIZE_ORIGINAL))
//                                                       .apply(bitmapTransform(new BlurTransformation(5, 3)))
                                                       .placeholder(new ColorDrawable(Color.TRANSPARENT))
                                                       .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                       .into(backdrop);
                                   }
                               }

                           }
                       });
                   }

               }});
           thread.start();
       }catch (NullPointerException exception){Log.i("Error",exception.toString());}
        setMyOnClickListeners();
    }

    String urlLogo ;


    //to blur the backdrop
    void blurBottom(){

        ((Activity) mActivity).getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        ((Activity) mActivity).getWindow().setStatusBarColor(Color.TRANSPARENT);
        final float radius = 14f;
        final Drawable windowBackground = ((Activity) mActivity).getWindow().getDecorView().getBackground();

        blurView.setupWith(rootView, new RenderScriptBlur(mActivity))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius);
        blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        blurView.setClipToOutline(true);
    }
    private void  loadMoreMovies() {
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
                        .getrecomendation();
                moreMovieList = new ArrayList<>();
                moreMovieList.addAll(morebyId);
                moreMovieList.addAll(moreMovies);
                moreMovieList.addAll(moreRecom);
                if(moreMovieList!=null && moreMovieList.size()>0) {
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            moreMovieView.setVisibility(View.VISIBLE);
                            ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.VERTICAL , false);
                            moreMovieView.setLayoutManager(linearLayoutManager);
                            moreMovieView.setHasFixedSize(true);
                            moreMovieRecycler = new MoreMoviesAdapterr(mActivity, (List<MyMedia>)(List<?>) moreMovieList, moreMoviesListener);
                            moreMovieView.setAdapter(moreMovieRecycler);
                            moreMovieRecycler.notifyDataSetChanged();
                        }
                    });
                }
            }});
        thread.start();

    }


    private void setMyOnClickListeners(){

        spay.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://sppay.shopee.co.id/qr/00ccc6eccf9a0f4cc900"))));
        dana.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://link.dana.id/qr/685glq8"))));
        saweria.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/nfgplus"))));
        paypal.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/paypalme/nfgplus?country.x=ID&locale.x=en_US"))));
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

        if(savedEXT){
            //External Player
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToLastPlayed();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(largestFile.getUrlString()));
                    intent.setDataAndType(Uri.parse(largestFile.getUrlString()), "video/*");
                    startActivity(intent);
                    loadReward();
                }
            });
        }else {
            //Play video
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        CustomFileListDialogFragment dialog =
                                new CustomFileListDialogFragment(mActivity,changeSource,
                                        (List<MyMedia>)(List<?>) movieFileList);

                        mActivity.
                                getSupportFragmentManager()
                                .beginTransaction()
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .add(android.R.id.content, dialog)
                                .addToBackStack(null)
                                .commit();
                        dialog.mOnInputListener = new CustomFileListDialogFragment.OnInputListener() {
                            @Override
                            public void sendInput(int selection) {
                                selectedFile = movieFileList.get(selection);
                                String huntu = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());
                                System.out.println("selected file"+selectedFile.getFileName());
                                Intent in = new Intent(getActivity(), PlayerActivity.class);
                                in.putExtra("url", selectedFile.getUrlString());
                                startActivity(in);
                                Toast.makeText(getContext(), "Playing " + movieDetails.getTitle() + " " + huntu, Toast.LENGTH_LONG).show();
                                Toast.makeText(mActivity , selectedFile.getTitle() + huntu + " Selected" , Toast.LENGTH_LONG).show();

                            }
                        };
                        //if (selectedFile != null) {
                          //  addToLastPlayed();
                           // Intent in = new Intent(getActivity(), PlayerActivity.class);
                           // in.putExtra("url", selectedFile.getUrlString());
                           // startActivity(in);
                           // Toast.makeText(getContext(), "Playing " + movieDetails.getTitle(), Toast.LENGTH_LONG).show();
                       // } else {
                            addToLastPlayed();
                         //   Intent in = new Intent(getActivity(), PlayerActivity.class);
                           // in.putExtra("url", movieDetails.getUrlString());
                           // startActivity(in);
                           // Toast.makeText(getContext(), "Playing " + movieDetails.getTitle(), Toast.LENGTH_LONG).show();
                       // }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        // Share button click listener


//        Start download
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the app has the WRITE_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request the permission if it is not granted
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
                } else {
                    // Permission is already granted, proceed with the download
                    startDownload();
                }


            }

            private void startDownload() {
                String customFolderPath = "/nfgplus/movies/";
                DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);
                Uri uri = Uri.parse(largestFile.getUrlString());
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, customFolderPath + largestFile.getFileName());
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDescription("Downloading");
                long reference = manager.enqueue(request);
                Toast.makeText(getContext(), "Download Started", Toast.LENGTH_LONG).show();
            }
        });


        addToList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(movieDetails.getAddToList()!=1){

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DatabaseClient
                                    .getInstance(mActivity)
                                    .getAppDatabase()
                                    .movieDao()
                                    .updateAddToList(movieDetails.getId());
                        }
                    }).start();

                    Toast.makeText(mActivity , "Added To List" , Toast.LENGTH_LONG).show();
                }
                else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DatabaseClient
                                    .getInstance(mActivity)
                                    .getAppDatabase()
                                    .movieDao()
                                    .updateRemoveFromList(movieId);
                        }
                    }).start();

                    Toast.makeText(mActivity , "Removed From List" , Toast.LENGTH_LONG).show();
                }

            }
        });



        final int[] checkedItem = {-1};
        int indexSelected = 0;
        changeSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomFileListDialogFragment dialog =
                        new CustomFileListDialogFragment(mActivity,changeSource,
                                (List<MyMedia>)(List<?>) movieFileList);

               mActivity.
                       getSupportFragmentManager()
                       .beginTransaction()
                       .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                       .add(android.R.id.content, dialog)
                       .addToBackStack(null)
                       .commit();
                dialog.mOnInputListener = new CustomFileListDialogFragment.OnInputListener() {
                    @Override
                    public void sendInput(int selection) {
                        selectedFile = movieFileList.get(selection);
                        String huntu = MovieQualityExtractor.extractQualtiy(selectedFile.getFileName());
                        System.out.println("selected file"+selectedFile.getFileName());
                        Toast.makeText(mActivity , selectedFile.getTitle() + huntu + " Selected" , Toast.LENGTH_LONG).show();

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
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.container,movieDetailsFragment)
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
                        // For now, you can still proceed with the long link if short link creation fails
                        shareDynamicLink(dynamicLink.getUri().toString());
                    }
                });
    }

    // Method to share the dynamic link
    private void shareDynamicLink(String dynamicLink) {
        // Create a share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Include movie details in the share text
        String shareText =
                movieDetails.getTitle()
                        + "\n \n Watch this movie in your app "
                        + "\n \n Overview: " + movieDetails.getOverview()
                        + "\n \n" + dynamicLink;

        // Set the share text as the data
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share This Movie"));
    }


    private void addToLastPlayed() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao().updatePlayed(movieDetails.getId());
            }
        });
        thread.start();
    }


}