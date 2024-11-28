package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.pembayaran.BillingManager;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailFragment extends BaseFragment{
    private Movie movieDetails;
    private int id;
    private boolean isMovie;
    private MaterialTextView deskripsi, judul;
    private ShapeableImageView poster;
    private MaterialButton rating, donasi, watchlist, download, share;
    private RecyclerView moreItem;
    RelativeLayout frameDeskripsi;
    private TemplateView template;
    private List<Movie> similarMovie;
    MoreMoviesAdapterr.OnItemClickListener moreMoviesListener;
    private BillingManager billingManager;
    private TVShow tvShowDetails; private TVShowSeasonDetails season; private Episode episode;
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
        loadNative();
        return view;
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
            String year = tvShowDetails.getFirst_air_date();
            String yearCrop = year.substring(0,year.indexOf('-'));
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            String ratings = (decimalFormat.format(tvShowDetails.getVote_average()));
            String result = StringUtils.runtimeIntegerToString(tvShowDetails.getVote_count());
            rating.setText(ratings + " from " + result + " Votes");
            judul.setText(titleText + " ("+yearCrop+")" +"\n" + "Season " + season.getSeason_number() + " Episode " + episode.getEpisode_number());
            deskripsi.setText(tvShowDetails.getOverview());
            Glide.with(mActivity)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getPoster_path())
                    .apply(new RequestOptions()
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);

        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(int id) {
        movieDetails = DetailsUtils.getMovieDetails(mActivity, id);
        if (movieDetails!=null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getRelease_date();
            String yearCrop = year.substring(0,year.indexOf('-'));
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            String ratings = (decimalFormat.format(movieDetails.getVote_average()));
            String result = StringUtils.runtimeIntegerToString(movieDetails.getVote_count());
            rating.setText(ratings + " from " + result + " Votes");
            judul.setText(titleText + " ("+yearCrop+")" +"\n" + movieDetails.getOriginal_title());
            deskripsi.setText(movieDetails.getOverview());
            Glide.with(mActivity)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPoster_path())
                    .apply(new RequestOptions()
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);


        }

    }


    private void initWidget(View view) {
        deskripsi = view.findViewById(R.id.deskJudul);
        judul = view.findViewById(R.id.judulNama);
        poster = view.findViewById(R.id.gambarPoster);
        donasi = view.findViewById(R.id.donasi);
        watchlist = view.findViewById(R.id.watchlist);
        download = view.findViewById(R.id.download);
        share = view.findViewById(R.id.share);
        moreItem = view.findViewById(R.id.moreItem);
        frameDeskripsi = view.findViewById(R.id.framedeskripsi);
        rating = view.findViewById(R.id.ratingsIkon);
        template = view.findViewById(R.id.iklan_kecil);
        billingManager = new BillingManager(mActivity);
        donasi.setOnClickListener(v -> billingManager.startPurchase(mActivity));

    }
    private void loadNative() {
        MobileAds.initialize(mActivity);
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

        adLoader.loadAd(new AdRequest.Builder().build());

    }

}
