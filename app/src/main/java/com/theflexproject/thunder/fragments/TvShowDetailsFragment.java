package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    TextView tvShowTitleText;
    TextView titleOri;
    TextView numberOfSeasons;
    TextView numberOfEpisodes;

    TextView overview;
    TextView listOfFiles;
    ImageButton play;

    ImageButton addToList;
    ImageButton share;



    TableRow genres;
    TableRow status;
    TableRow type;
    TableRow votesAvg;
    TableRow votesCount;

    TextView statusText;
    TextView typeText;
    TextView genresText;

    ImageView logo;
    TextView continueWatching;
    ImageView dot3;
    ImageView dot1;
    TextView episodeTitle;


    TextView voteAvgText;
    TextView votesCountText;
    TextView ratingsText;
    ImageView ratings;

    ImageView poster;
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
        manager = new FirebaseManager();
        loadNative();

        initWidgets(view);
        loadDetails();
    }

    private void initWidgets(View view) {
        WebView webView = view.findViewById(R.id.webview2);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://stream.trakteer.id/running-text-default.html?rt_count=5&rt_speed=fast&rt_1_clr1=rgba%280%2C+0%2C+0%2C+1%29&rt_septype=image&rt_txtshadow=true&rt_showsuppmsg=true&creator_name=nfgplus-official&page_url=trakteer.id/nfgplusofficial&mod=3&key=trstream-hV0jDdrlk82mv3aZnzpA&hash=a6z74q7pkgn3mlqy");
        tvShowTitleText = view.findViewById(R.id.tvShowTitle);
        titleOri = view.findViewById(R.id.fakebutton2);
        logo = view.findViewById(R.id.tvLogo);
        numberOfSeasons = view.findViewById(R.id.noOfSeasons);
        numberOfEpisodes = view.findViewById(R.id.noOfEpisodes);
        overview = view.findViewById(R.id.overviewDescTVShow);
        backdrop = view.findViewById(R.id.tvShowBackdrop);
        genresText = view.findViewById(R.id.tvShowGenresText);
        continueWatching = view.findViewById(R.id.continueWatchingText);
        dot3 = view.findViewById(R.id.dot3);
        dot1 = view.findViewById(R.id.dot);
        episodeTitle = view.findViewById(R.id.episodeNameInTv);
        ratingsText = view.findViewById(R.id.ratingsTVText);
        play = view.findViewById(R.id.playInTVShowDetails);
        addToList = view.findViewById(R.id.addToListButtonTV);
        share = view.findViewById(R.id.shareButton);

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
                        @Override
                        public void run() {

                            String logoLink = tvShowDetails.getLogo_path();
                            System.out.println("Logo Link"+logoLink);
                            titleOri.setVisibility(View.VISIBLE);
                            titleOri.setText(tvShowDetails.getOriginal_name());

                            if(!logoLink.equals("")){
                                logo.setVisibility(View.VISIBLE);
                                tvShowTitleText.setText(tvShowDetails.getName());
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
                                tvShowTitleText.setVisibility(View.VISIBLE);
                                tvShowTitleText.setText(tvShowDetails.getName());
                                logo.setVisibility(View.GONE);
                            }
                            if(tvShowDetails.getGenres()!=null){
//                                genres.setVisibility(View.VISIBLE);
                                ArrayList<Genre> genres = tvShowDetails.getGenres();
                                StringBuilder sb = new StringBuilder();
                                for (int i=0;i<genres.size();i++) {
                                    Genre genre = genres.get(i);
                                    if(genre!=null){
                                        if(i== genres.size()-1){sb.append(genre.getName());}
                                        else {sb.append(genre.getName()).append(", ");}
                                    }
                                }
                                System.out.println("genres"+ genres);
                                genresText.setText(sb.toString());
                            }
                            if(tvShowDetails.getVote_average()!=0){
//                                ratings.setVisibility(View.VISIBLE);
                                dot1.setVisibility(View.VISIBLE);
                                ratingsText.setVisibility(View.VISIBLE);
                                String rating = (int)(tvShowDetails.getVote_average()*10)+"%" ;
                                ratingsText.setText(rating);
                            }
                            int number_of_seasons = tvShowDetails.getNumber_of_seasons();
                            int number_of_episodes = tvShowDetails.getNumber_of_episodes();
                            if(number_of_seasons!=0) {
                                numberOfSeasons.setVisibility(View.VISIBLE);
                                numberOfSeasons.setText(+number_of_seasons+" Seasons");
                                numberOfEpisodes.setVisibility(View.VISIBLE);
                                numberOfEpisodes.setText(number_of_episodes+" Episodes");
                            }
//                            if(tvShowDetails.getType()!=null){
//                                type.setVisibility(View.VISIBLE);
//                                typeText.setText(tvShowDetails.getType());
//                            }
//                            if(tvShowDetails.getStatus()!=null){
//                                status.setVisibility(View.VISIBLE);
//                                statusText.setText(tvShowDetails.getStatus());
//                            }
                            if(tvShowDetails.getOverview()!=null){overview.setText(tvShowDetails.getOverview());}
                            if(tvShowDetails.getPoster_path()!=null) {
//                                Glide.with(mActivity)
//                                        .load(TMDB_IMAGE_BASE_URL + tvShowDetails.getPoster_path())
//                                        .placeholder(new ColorDrawable(Color.BLACK))
//                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                                        .into(poster);
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
                                continueWatching.setText(buttonText);
//                            play.setText(buttonText);
                                if(nextEpisode.getName()!=null){
                                    dot3.setVisibility(View.VISIBLE);
                                    episodeTitle.setVisibility(View.VISIBLE);
                                    episodeTitle.setText(nextEpisode.getName());
                                }
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


        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (savedEXT) {
                    //External Player
                    addToLastPlayed();
                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse(nextEpisode.getUrlString()));
                    intent.setDataAndType(Uri.parse(nextEpisode.getUrlString()) , "video/*");
                    startActivity(intent);
                } else {
                    //Play video
                    addToLastPlayed();
                    Intent in = new Intent(getActivity() , PlayerActivity.class);
                    in.putExtra("url" , nextEpisode.getUrlString());
                    startActivity(in);
                    Toast.makeText(getContext() , "Play" , Toast.LENGTH_LONG).show();
                }
            }
            private void addToLastPlayed() {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                        String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
                        DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao().updatePlayed(nextEpisode.getId(), currentDateTime+" added");
                    }
                });
                thread.start();
            }
        });
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