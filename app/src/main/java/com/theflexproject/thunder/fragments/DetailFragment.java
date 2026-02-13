package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.Constants.language;
import static com.theflexproject.thunder.fragments.EpisodeDetailsFragment.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION;
import static com.theflexproject.thunder.player.PlayerUtils.isTVDevice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.CreditsAdapter;
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.adapter.SimilarAdapter;
import com.theflexproject.thunder.model.Cast;
import com.theflexproject.thunder.model.Credits;
import com.theflexproject.thunder.model.Crew;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.AdHelper;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.Translate;
import com.theflexproject.thunder.utils.pembayaran.BillingManager;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DetailFragment extends BaseFragment implements BillingManager.BillingCallback{
    private Movie movieDetails;
    private int id;
    private boolean isMovie;
    private MaterialTextView deskripsi, judul;
    private ShapeableImageView poster;
    private MaterialButton rating, donasi, watchlist, download, share, subscribe;
    private RecyclerView moreItem;
    RelativeLayout frameDeskripsi;
    private TemplateView template, templateBesar;
    private RecyclerView similarView, castView;
    private CreditsAdapter creditsAdapter;
    private SimilarAdapter.OnItemClickListener similarListener;
    private List<MyMedia> similarOrEpisode;
    private BillingManager billingManager;
    private TVShow tvShowDetails; private TVShowSeasonDetails season; private Episode episode;
    private List<SkuDetails> skuDetailsList = new ArrayList<>();
    private List<SkuDetails> skuSubList = new ArrayList<>();
    private AdRequest adRequest;
    private List<MyMedia> sources;
    private List<MyMedia> mediaList, castList;
    private boolean isSubscribed;
    private Credits credits;
    public DetailFragment(){
    }
    public DetailFragment(TVShow tvShowDetails, TVShowSeasonDetails seasonDetails, Episode episode){
        this.isMovie = false;
        this.tvShowDetails = tvShowDetails;
        this.season = seasonDetails;
        this.episode = episode;
    }
    public DetailFragment (int id, boolean isMovie){
        this.id = id;
        this.isMovie = isMovie;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireContext().getSharedPreferences("langgananUser", Context.MODE_PRIVATE);
        isSubscribed = prefs.getBoolean("isSubscribed", false);
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate layout untuk fragment
        View view = inflater.inflate(R.layout.detail_item, container, false);
        initWidget(view);
        if (isMovie) {
            loadMovieDetails(id);
        }
        else {
            loadTvDetails();
        }
        listener();
        buttonListener();

        return view;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void buttonListener() {
        download.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < 32) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
                }else {
                    PlayerUtils.download(mActivity, sources, tvShowDetails, season);
                }
            }
            else {
                PlayerUtils.download(mActivity, sources, tvShowDetails, season);
            }
        });
        share.setOnClickListener(v -> PlayerUtils.share(mActivity, mActivity, mediaList, tvShowDetails, season));
        watchlist.setOnClickListener(v -> PlayerUtils.watchlist(mActivity, mediaList, tvShowDetails, season));
    }


    private void listener() {
        frameDeskripsi.setOnClickListener(v ->  {
            if (isMovie){
                VideoDetailsBottomSheet bottomSheet = new VideoDetailsBottomSheet(movieDetails.getId(), true);
                bottomSheet.show( mActivity.getSupportFragmentManager(), "VideoDetailsBottomSheet");
            }else {
                VideoDetailsBottomSheet bottomSheet = new VideoDetailsBottomSheet(tvShowDetails);
                bottomSheet.show( mActivity.getSupportFragmentManager(), "VideoDetailsBottomSheet");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadTvDetails() {
        if (tvShowDetails!=null) {
            String titleText = tvShowDetails.getName();
            String year = tvShowDetails.getFirstAirDate();
            String yearCrop = year.substring(0,year.indexOf('-'));
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            String ratings = (decimalFormat.format(tvShowDetails.getVoteAverage()));
            String result = StringUtils.runtimeIntegerToString(tvShowDetails.getVoteCount());
            rating.setText(ratings + " from " + result + " Votes");
            judul.setText(titleText + " ("+yearCrop+")" +"\n" + "Season " + season.getSeasonNumber() + " Episode " + episode.getEpisodeNumber());
            deskripsi.setText(tvShowDetails.getOverview());
            Glide.with(mActivity)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getPosterPath())
                    .apply(new RequestOptions()
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);
            sources = (List<MyMedia>)(List<?>)DetailsUtils.getEpisodeSource(mActivity, episode.getId());
            mediaList = new ArrayList<>();
            mediaList.add(episode);
            loadEpisodes(season.getId());
            loadCast(tvShowDetails.getId(), false);
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(int id) {
        movieDetails = DetailsUtils.getMovieDetails(mActivity, id);
        if (movieDetails!=null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getReleaseDate();
            String yearCrop = year.substring(0,year.indexOf('-'));
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            String ratings = (decimalFormat.format(movieDetails.getVoteAverage()));
            String result = StringUtils.runtimeIntegerToString(movieDetails.getVoteCount());
            rating.setText(ratings + " from " + result + " Votes");
            judul.setText(titleText +"\n" + movieDetails.getOriginalTitle() + " ("+yearCrop+")");
            String description = movieDetails.getOverview();
            deskripsi.setText(description);
            Glide.with(mActivity)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPosterPath())
                    .apply(new RequestOptions()
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);
            sources = (List<MyMedia>)(List<?>)DetailsUtils.getSourceList(mActivity, id);
            mediaList = new ArrayList<>();
            mediaList.add(movieDetails);
            loadSimilar(id);
            loadCast(movieDetails.getId(), true);

        }

    }

    private void loadCast(int id, boolean isMovie) {
        new Thread(() -> {
            tmdbTrending credit = new tmdbTrending();
            if (isMovie){
            credits = credit.getMovieCredits(id);
            }else {
                credits = credit.getTvCredits(id);
            }
            List<Cast> cast = credits.getCastList();
            int limit = Math.min(cast.size(), 5);
            List<Cast> limitedCast = cast.subList(0, limit);
            castList = new ArrayList<>();
            castList.addAll(limitedCast);
            getActivity().runOnUiThread(() -> {
                FragmentManager fragmentManager;
                fragmentManager = getActivity().getSupportFragmentManager();
                castView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                castView.setHasFixedSize(true);
                creditsAdapter = new CreditsAdapter(getContext(), castList , fragmentManager);
                castView.setAdapter(creditsAdapter);
                castView.setNestedScrollingEnabled(false);
            });
        }).start();
    }


    @SuppressLint("SetTextI18n")
    private void initWidget(View view) {
        template = view.findViewById(R.id.iklan_kecil);
        templateBesar = view.findViewById(R.id.iklan_besar);
        deskripsi = view.findViewById(R.id.deskJudul);
        judul = view.findViewById(R.id.judulNama);
        poster = view.findViewById(R.id.gambarPoster);
        donasi = view.findViewById(R.id.donasi);
        subscribe = view.findViewById(R.id.subscibe);
        watchlist = view.findViewById(R.id.watchlist);
        download = view.findViewById(R.id.download);
        share = view.findViewById(R.id.share);
        frameDeskripsi = view.findViewById(R.id.framedeskripsi);
        rating = view.findViewById(R.id.ratingsIkon);
        castView = view.findViewById(R.id.cast);
        billingManager = new BillingManager(mActivity, this);
        billingManager.startConnection();
        donasi.setText("Send Gift");
        donasi.setOnClickListener(v -> {
            showThankYouOptions();
        });
        subscribe.setOnClickListener(v -> {

            showSubscriptionOptions();
        });
        if (isSubscribed) {
            subscribe.setText("Subscribed");
            subscribe.setEnabled(false);
            // Jika berlangganan, sembunyikan AdView
            template.setVisibility(View.GONE);
            templateBesar.setVisibility(View.GONE);

        } else {
            adRequest = AdHelper.getAdRequest(mActivity);
            AdHelper.loadNative(mActivity, adRequest, template);
            AdHelper.loadNative(mActivity, adRequest, templateBesar);
        }
        similarView = view.findViewById(R.id.similarAndEpisode);


    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onProductsLoaded(List<SkuDetails> products) {
        skuDetailsList.clear();
        skuDetailsList.addAll(products);
        Log.d("DetailFragment", "Products loaded: " + skuDetailsList.size());
    }

    @Override
    public void onSubscriptionLoaded(List<SkuDetails> subscriptions) {
        skuSubList.clear();
        skuSubList.addAll(subscriptions);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPurchaseCompleted(Purchase purchase) {
        Log.d("DetailFragment", "Purchase completed: " + purchase.getSkus());
        Toast.makeText(mActivity, "Pembayaran berhasil", Toast.LENGTH_SHORT).show();
        billingManager.startChecking(mActivity);
    }

    @Override
    public void onSubscriptionStatus(boolean isSubscribed) {
        SharedPreferences prefs = mActivity.getSharedPreferences("langgananUser", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isSubscribed", isSubscribed);
        editor.apply();
        this.isSubscribed = isSubscribed;
    }

    private void showThankYouOptions() {
        if (skuDetailsList.isEmpty()) {
            Toast.makeText(requireContext(), "Produk belum tersedia. Coba lagi nanti.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_thank_you, null);
        LinearLayout container = bottomSheetView.findViewById(R.id.skuContainer);

        for (int i = 0; i < skuDetailsList.size(); i++) {
            SkuDetails skuDetails = skuDetailsList.get(i);
            MaterialButton priceButton = new MaterialButton(getContext());
            priceButton.setText(skuDetails.getPrice());
            priceButton.setBackgroundColor(getResources().getColor(R.color.blue)); // Button color
            priceButton.setTextColor(getResources().getColor(R.color.white));
            priceButton.setOnClickListener(v -> billingManager.startPurchase(mActivity, skuDetails));
            container.addView(priceButton);
        }

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    @SuppressLint("SetTextI18n")
    @OptIn(markerClass = UnstableApi.class)
    private void showSubscriptionOptions() {
        if (skuSubList == null || skuSubList.isEmpty()) {
            Log.d("BillingManager", "No subscription details available.");
            return; // Jangan lanjut jika tidak ada data
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_thank_you, null);
        TextView title = bottomSheetView.findViewById(R.id.title);
        LinearLayout container = bottomSheetView.findViewById(R.id.skuContainer);

        // Set title


        // Gunakan for loop dengan index untuk menghindari ConcurrentModificationException
        for (int i = 0; i < skuSubList.size(); i++) {
            SkuDetails skuDetails = skuSubList.get(i);
            title.setText("Langganan "+ skuDetails.getPrice() +" perbulan \n untuk menikmati nfgplus tanpa iklan");
            // Create ImageView for displaying the image
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            imageLayoutParams.setMargins(0, 16, 0, 16); // Optional margin
            imageView.setLayoutParams(imageLayoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Add padding to ImageView
            int paddingInPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5,
                    getResources().getDisplayMetrics()
            );
            imageView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

            // Load image using Glide
            String imageUrl = "https://drive3.nfgplusmirror.workers.dev/0:/photo_2024-11-01_18-36-55_7432395722672046100.jpg"; // Replace with your image URL
            Glide.with(getContext())
                    .load(imageUrl)
                    .into(imageView);

            // Add ImageView to container before the button
            container.addView(imageView);

            // Create subscribe button
            MaterialButton subscribeButton = new MaterialButton(getContext());
            subscribeButton.setText(skuDetails.getPrice());
            subscribeButton.setBackgroundColor(getResources().getColor(R.color.blue)); // Button color
            subscribeButton.setTextColor(getResources().getColor(R.color.white)); // Button text color
            subscribeButton.setOnClickListener(v -> billingManager.startSubscription(getActivity(), skuDetails));


            // Add margins and layout params to button
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            buttonLayoutParams.setMargins(0, 16, 0, 16); // Optional margin for button
            subscribeButton.setLayoutParams(buttonLayoutParams);

            // Add button to container
            container.addView(subscribeButton);
        }

        // Set the content view for the BottomSheetDialog
        bottomSheetDialog.setContentView(bottomSheetView);

        // Show the BottomSheetDialog
        bottomSheetDialog.show();
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        billingManager.endConnection();
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
                            ScaleCenterItemLayoutManager linearLayoutManager;
                            if (isTVDevice(mActivity)){
                                linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                            }
                            else {
                                linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                            }
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
    private void loadEpisodes(int seasonId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Episode> listEpisode = DetailsUtils.getListEpisode(mActivity, tvShowDetails.getId(), seasonId);
                if (listEpisode!=null){
                    movieListener();
                    similarOrEpisode = new ArrayList<>();
                    similarOrEpisode.addAll(listEpisode);
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            similarView.setVisibility(View.VISIBLE);
                            ScaleCenterItemLayoutManager linearLayoutManager;
                            if (isTVDevice(mActivity)){

                                linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                            }
                            else {
                                linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                            }
                            similarView.setLayoutManager(linearLayoutManager);
                            SimilarAdapter moreMovieRecycler = new SimilarAdapter(mActivity, (List<MyMedia>) (List<?>) similarOrEpisode, tvShowDetails, similarListener);
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
                    Fragment currentFragment = mActivity.getSupportFragmentManager().findFragmentById(R.id.container);
                    if (currentFragment instanceof PlayerFragment) {
                        ((PlayerFragment) currentFragment).updateMovie(movieId, true);
                    } else {
                        Log.e("Error", "Current fragment is not PlayerFragment");
                    }
                }
                if (similarOrEpisode.get(position) instanceof Episode){
                    Episode episodeSelected = (Episode) similarOrEpisode.get(position);
                    String url = episodeSelected.getUrlString();

                    episode = episodeSelected;
                    Fragment currentFragment = mActivity.getSupportFragmentManager().findFragmentById(R.id.container);
                    if (currentFragment instanceof PlayerFragment) {
                        ((PlayerFragment) currentFragment).updateEpisode(tvShowDetails, season, episodeSelected.getId());
                    } else {
                        Log.e("Error", "Current fragment is not PlayerFragment");
                    }

                }

            }
        };
    }
}

