package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.fragments.EpisodeDetailsFragment.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION;

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
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.AdHelper;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.pembayaran.BillingManager;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
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
    private List<Movie> similarMovie;
    MoreMoviesAdapterr.OnItemClickListener moreMoviesListener;
    private BillingManager billingManager;
    private TVShow tvShowDetails; private TVShowSeasonDetails season; private Episode episode;
    private List<SkuDetails> skuDetailsList = new ArrayList<>();
    private AdRequest adRequest;
    private List<MyMedia> sources;
    private List<MyMedia> mediaList;
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
        adRequest = AdHelper.getAdRequest(mActivity);
        initWidget(view);
        if (isMovie) {
            loadMovieDetails(id);
        }
        else {
            loadTvDetails();
        }
        listener();
        SharedPreferences prefs = requireContext().getSharedPreferences("langgananUser", Context.MODE_PRIVATE);
        boolean isSubscribed = prefs.getBoolean("isSubscribed", false);
        if (!isSubscribed) {
            AdHelper.loadNative(mActivity, adRequest, template);
            AdHelper.loadNative(mActivity, adRequest, templateBesar);
        } else {
            // Jika berlangganan, sembunyikan AdView
            template.setVisibility(View.GONE);
            templateBesar.setVisibility(View.GONE);
        }
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
            sources = (List<MyMedia>)(List<?>)DetailsUtils.getEpisodeSource(mActivity, episode.getId());
            mediaList = new ArrayList<>();
            mediaList.add(episode);

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
            sources = (List<MyMedia>)(List<?>)DetailsUtils.getSourceList(mActivity, id);
            mediaList = new ArrayList<>();
            mediaList.add(movieDetails);

        }

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
        billingManager = new BillingManager(mActivity, this);
        billingManager.startConnection();
        donasi.setText("Send Gift");
        donasi.setOnClickListener(v -> showThankYouOptions());
        subscribe.setOnClickListener(v -> billingManager.loadSubscriptionDetails(this::showSubscriptionOptions));

    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onProductsLoaded(List<SkuDetails> products) {
        skuDetailsList.clear();
        skuDetailsList.addAll(products);
        Log.d("DetailFragment", "Products loaded: " + skuDetailsList.size());
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPurchaseCompleted(Purchase purchase) {
        Log.d("DetailFragment", "Purchase completed: " + purchase.getSkus());
        Toast.makeText(mActivity, "Pembayaran berhasil", Toast.LENGTH_SHORT).show();
    }
    private void showThankYouOptions() {
        if (skuDetailsList.isEmpty()) {
            Toast.makeText(requireContext(), "Produk belum tersedia. Coba lagi nanti.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_thank_you, null);
        LinearLayout container = bottomSheetView.findViewById(R.id.skuContainer);

        for (SkuDetails skuDetails : skuDetailsList) {
            MaterialButton priceButton = new MaterialButton(mActivity, null, R.style.CustomMaterialButton);
            priceButton.setText(skuDetails.getPrice());
            priceButton.setOnClickListener(v -> billingManager.startPurchase(mActivity, skuDetails));
            container.addView(priceButton);
        }

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }
    private void showSubscriptionOptions(List<SkuDetails> skuDetailsList) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_thank_you, null);
        TextView title = bottomSheetView.findViewById(R.id.title);
        LinearLayout container = bottomSheetView.findViewById(R.id.skuContainer);

        for (SkuDetails skuDetails : skuDetailsList) {
            title.setText("Langganan 15ribu perbulan \n untuk menikmati nfgplus tanpa iklan \n" + skuDetails.getTitle());
            // Buat ImageView untuk menampilkan gambar dari URL
            ImageView imageView = new ImageView(requireContext());
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,  // width match_parent
                    ViewGroup.LayoutParams.WRAP_CONTENT  // height wrap_content
            );
            imageLayoutParams.setMargins(0, 16, 0, 16); // Tambahkan margin opsional
            imageView.setLayoutParams(imageLayoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Atur skala gambar agar terlihat proporsional

            // Tambahkan padding 5dp ke ImageView
            int paddingInPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5,
                    requireContext().getResources().getDisplayMetrics()
            );
            imageView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);

            // Muat gambar dari URL menggunakan Glide
            Glide.with(requireContext())
                    .load("https://drive3.nfgplusmirror.workers.dev/0:/photo_2024-11-01_18-36-55_7432395722672046100.jpg") // Ganti dengan URL gambar
                    .into(imageView);

            // Tambahkan ImageView ke container sebelum tombol
            container.addView(imageView);

            // Buat tombol untuk langganan
            MaterialButton subscribeButton = new MaterialButton(requireContext());
            subscribeButton.setText(skuDetails.getPrice());
            subscribeButton.setOnClickListener(v -> billingManager.startSubscription(requireActivity(), skuDetails));
            container.addView(subscribeButton);
        }

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        billingManager.endConnection();
    }
}
