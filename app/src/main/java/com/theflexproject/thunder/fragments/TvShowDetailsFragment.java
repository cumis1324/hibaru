package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Genre;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerActivity;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.firebase.database.FirebaseDatabase;

public class TvShowDetailsFragment extends BaseFragment {

    int tvShowId;

    TextView titleOri, title;




    ImageButton addToList,share;


    ImageView logo;
    RelativeLayout titleLayout;
    MaterialButton rating;



    ImageView backdrop;
    TVShow tvShowDetails;

    RecyclerView recyclerViewSeasons;
    List<TVShowSeasonDetails> seasonsList;
    MediaAdapter mediaAdapter;
    MediaAdapter.OnItemClickListener listenerSeasonItem;

    Episode nextEpisode;
    private Button saweria;
    private TemplateView template;
    FirebaseManager manager;
    DatabaseReference databaseReference;
    BottomNavigationView botnav;


    public TvShowDetailsFragment() {
        // Required empty public constructor
    }
    public TvShowDetailsFragment(int tvShowId) {
        this.tvShowId = tvShowId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater , ViewGroup container ,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_show_details_new , container , false);
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view , savedInstanceState);
        saweria = view.findViewById(R.id.saweria);
        template = view.findViewById(R.id.my_template);
        botnav = mActivity.findViewById(R.id.bottom_navigation);
        botnav.setVisibility(View.GONE);
        manager = new FirebaseManager();
        loadNative();

        initWidgets(view);
        loadDetails();
    }
    private boolean isTVDevice() {
        UiModeManager uiModeManager = (UiModeManager) mActivity.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    @Override
    public void onDestroyView() {
        if(isTVDevice()) {
            botnav.setVisibility(View.GONE);
        }
        else {
            botnav.setVisibility(View.VISIBLE);
        }
        super.onDestroyView();
    }

    private void initWidgets(View view) {
        WebView webView = view.findViewById(R.id.webview2);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://stream.trakteer.id/running-text-default.html?rt_count=5&rt_speed=fast&rt_1_clr1=rgba%280%2C+0%2C+0%2C+1%29&rt_septype=image&rt_txtshadow=true&rt_showsuppmsg=true&creator_name=nfgplus-official&page_url=trakteer.id/nfgplusofficial&mod=3&key=trstream-hV0jDdrlk82mv3aZnzpA&hash=a6z74q7pkgn3mlqy");
        titleOri = view.findViewById(R.id.fakebutton2);
        logo = view.findViewById(R.id.tvLogo);
        backdrop = view.findViewById(R.id.tvShowBackdrop);
        addToList = view.findViewById(R.id.addToListButtonTV);
        share = view.findViewById(R.id.shareButton);
        titleLayout = view.findViewById(R.id.titleLayout2);
        titleLayout.requestFocus();
        title = view.findViewById(R.id.titleShow);
        rating = view.findViewById(R.id.ratingsShow);
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


    private void loadDetails(){
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    tvShowDetails = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .tvShowDao()
                            .find(tvShowId);

                    nextEpisode = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .episodeDao()
                            .getNextEpisodeInTVShow(tvShowDetails.getId());
                    if(nextEpisode==null){
                        nextEpisode = DatabaseClient.getInstance(getContext())
                                .getAppDatabase()
                                .episodeDao()
                                .getFirstAvailableEpisode(tvShowDetails.getId());
                    }

                    Log.i("tvShowDetails Object",tvShowDetails.toString());
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            String ratings = String.valueOf((int)(tvShowDetails.getVote_average()*10));
                            String year = tvShowDetails.getFirst_air_date();
                            String yearCrop = year.substring(0, year.indexOf('-'));
                            String result = String.valueOf(tvShowDetails.getNumber_of_seasons());
                            String logoLink = tvShowDetails.getLogo_path();
                            title.setText(tvShowDetails.getName() + " (" + yearCrop + ") ");
                            rating.setText(ratings + " - " + result + " Season" + " ... Selengkapnya");
                            System.out.println("Logo Link"+logoLink);
                            titleOri.setVisibility(View.VISIBLE);
                            titleOri.setText(tvShowDetails.getOriginal_name());
                            titleLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    VideoDetailsBottomSheet bottomSheet = VideoDetailsBottomSheet.newInstance(tvShowDetails.getId(), "tvshow");
                                    bottomSheet.show( mActivity.getSupportFragmentManager(), "VideoDetailsBottomSheet");
                                }
                            });

                            if(!logoLink.equals("")){
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
                            if(logoLink.equals("")&&tvShowDetails.getName()!=null){
                                logo.setVisibility(View.GONE);
                            }

                            if(tvShowDetails.getBackdrop_path()!=null){
                                Glide.with(mActivity)
                                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getBackdrop_path())
                                        .placeholder(new ColorDrawable(Color.BLACK))
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(backdrop);
                            }
                            else if(tvShowDetails.getPoster_path()!=null){
                                    Glide.with(mActivity)
                                            .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShowDetails.getPoster_path())
                                            .placeholder(new ColorDrawable(Color.BLACK))
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(backdrop);
                            }
                            if((nextEpisode!=null && nextEpisode.getStill_path()!=null)) {
                                Glide.with(mActivity)
                                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + nextEpisode.getStill_path())
                                        .placeholder(new ColorDrawable(Color.BLACK))
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(backdrop);
                            }

                            if (nextEpisode != null) {
                                String buttonText = "S" + nextEpisode.getSeason_number() + " E" + nextEpisode.getEpisode_number();
                                System.out.println(buttonText);
                            }

                        }
                    });

                    loadSeasonRecycler();

                }});
            thread.start();


        }catch (NullPointerException exception){Log.i("Error",exception.toString());}
    }

    private void loadSeasonRecycler() {

        setOnClickListner();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(" ", "in thread");
                seasonsList = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowSeasonDetailsDao()
                        .findByShowId(tvShowDetails.getId());

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("SeasonList in loadSeas", seasonsList.toString());
                        recyclerViewSeasons = mActivity.findViewById(R.id.recyclerSeasons);
//                        ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL,false);


                        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
                        if(dpWidth>600f)
                        {
                            recyclerViewSeasons.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));
                        }else {
                            recyclerViewSeasons.setLayoutManager(new GridLayoutManager(getContext(),3));
                        }


//                        recyclerViewSeasons.setLayoutManager(linearLayoutManager);
                        recyclerViewSeasons.setHasFixedSize(true);
                        mediaAdapter = new MediaAdapter(getContext(),(List<MyMedia>)(List<?>) seasonsList,listenerSeasonItem);
                        recyclerViewSeasons.setAdapter(mediaAdapter);
                        mediaAdapter.notifyDataSetChanged();
                    }
                });
            }});
        thread.start();
    }

    private void setOnClickListner(){
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("Settings" , Context.MODE_PRIVATE);
        boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING" , false);

        saweria.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://trakteer.id/nfgplusofficial/tip"))));

        addToList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String tmdbId = String.valueOf(tvShowId);
                String userId = manager.getCurrentUser().getUid();
                DatabaseReference userReference = FirebaseDatabase.getInstance().getReference("Favorit").child(userId).child(tmdbId);
                DatabaseReference value = userReference.child("value");
                value.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(!snapshot.exists()){

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("value", 1);
                            userReference.setValue(userMap);

                            Toast.makeText(mActivity , "Added To List" , Toast.LENGTH_LONG).show();

                        }else{
                            userReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        System.out.println("Favorit dihapus");
                                    }
                                    else {
                                        System.out.println("Favorit dihapus");
                                    }
                                }
                            });

                            Toast.makeText(mActivity , "Removed From List" , Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareIt();
            }
        });





        listenerSeasonItem = (view , position) -> {
            SeasonDetailsFragment seasonDetailsFragment = new SeasonDetailsFragment(tvShowDetails,seasonsList.get(position));
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .add(R.id.container,seasonDetailsFragment).addToBackStack(null).commit();
        };
    }

    private void shareIt() {
        String title = tvShowDetails.getName();
        String originalTitle = tvShowDetails.getOriginal_name();
        String overview = tvShowDetails.getOverview();
        String posterPath = "https://image.tmdb.org/t/p/w500" + tvShowDetails.getPoster_path();
        String movieId = String.valueOf(tvShowDetails.getId());

        // Tautan deep link lengkap
        String deepLink = "https://nfgplus.my.id/reviews.html?id=" + movieId + "&type=tv";

        // Menyusun teks yang ingin dibagikan
        String shareText = title + "\n" +
                "Judul Asli: " + originalTitle + "\n" +
                "Deskripsi: " + overview + "\n" +
                deepLink + "\n" ;

        // Membuat Intent untuk membagikan konten
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain"); // Menggunakan teks biasa

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);

        startActivity(Intent.createChooser(shareIntent, "Bagikan " + title));
    }
}