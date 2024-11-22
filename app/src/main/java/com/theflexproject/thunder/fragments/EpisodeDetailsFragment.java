package com.theflexproject.thunder.fragments;

import static android.content.Context.DOWNLOAD_SERVICE;
import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;
import static com.theflexproject.thunder.player.PlayerActivity.KEY_AUTO_PLAY;
import static com.theflexproject.thunder.player.PlayerActivity.KEY_ITEM_INDEX;
import static com.theflexproject.thunder.player.PlayerActivity.KEY_POSITION;
import static com.theflexproject.thunder.player.PlayerActivity.KEY_TRACK_SELECTION_PARAMETERS;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.Constants;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.FileItemAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerActivity;
import com.theflexproject.thunder.player.PlayerHelper;
import com.theflexproject.thunder.player.VideoPlayer;
import com.theflexproject.thunder.utils.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


@UnstableApi
public class EpisodeDetailsFragment extends BaseFragment {


    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 0;
    TextView showName;
    TextView episodeName;
    ImageView episodeStill;
    ImageView poster;
    TextView seasonNumber;
    TextView runtime;
    TextView overview;
    TextView overviewText;
    ImageView ratings;
    TextView ratingsText;
    ImageButton play;
    ImageButton fullScreen;

    TableRow air_date;
    TextView air_date_text;

    ImageView logo;
    TextView continueWatching;
    ImageView dot1;
    ImageView dot2;
    ImageView dot3;
    ImageButton download;

    TextView episodeTitle;

    TVShow tvShow;
    TVShowSeasonDetails tvShowSeasonDetails;

    Episode largestFile;//default episode played on click of Play button
    Episode episode;

    RecyclerView recyclerViewEpisodeFiles;
    List<Episode> episodeFileList;
    FileItemAdapter fileAdapter;
    private PlayerView playerView;
    private TrackSelectionParameters trackSelectionParameters;
    private long startPosition;
    private boolean startAutoPlay;
    private int startItemIndex;
    private MediaItem mediaItem;
    FirebaseManager manager;
    private DatabaseReference databaseReference;
    private static final int FULLSCREEN_REQUEST_CODE = 1;

    private PlayerHelper playerHelper;
    // Set your media URI here
    private long currentPosition = 0;

    public EpisodeDetailsFragment() {
        // Required empty public constructor
    }

    public EpisodeDetailsFragment(TVShow tvShow , TVShowSeasonDetails tvShowSeasonDetails , Episode episode) {
        this.tvShow = tvShow;
        this.tvShowSeasonDetails = tvShowSeasonDetails;
        this.episode = episode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater , ViewGroup container ,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episode_details_new , container , false);
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        playerHelper = new PlayerHelper();
        initWidgets(view);
        loadDetails();
        manager = new FirebaseManager();
        String tmdbId = String.valueOf(episode.getId());
        databaseReference = FirebaseDatabase.getInstance().getReference("History/"+tmdbId);
        playerView = view.findViewById(R.id.player_view);



        super.onViewCreated(view , savedInstanceState);
    }


    private void initWidgets(View view) {
        fullScreen = view.findViewById(R.id.fullScreen);
        showName = view.findViewById(R.id.tvShowTitleInEpisodeDetails);
        logo = view.findViewById(R.id.tvLogoInEp);
        episodeName = view.findViewById(R.id.episodeNameInEpisodeDetails);
        poster = view.findViewById(R.id.tvShowPosterInEpisodeDetails);
        episodeStill = view.findViewById(R.id.stillInEpisodeDetails);
        seasonNumber = view.findViewById(R.id.seasonNumberInEpisodeDetails);
        runtime = view.findViewById(R.id.runtimeInEpisodeDetails);
//        ratings = view.findViewById(R.id.ratingsInEpisodeDetails);
        ratingsText = view.findViewById(R.id.ratingsTextInEpisodeDetails);
        overview = view.findViewById(R.id.overviewInEpisodeDetails);
        overviewText = view.findViewById(R.id.overviewDescInEpisodeDetails);

        air_date = view.findViewById(R.id.episodeAirDate);
        air_date_text = view.findViewById(R.id.episodeAirDateText);

        continueWatching = view.findViewById(R.id.continueWatchingText);
        dot1 = view.findViewById(R.id.dot);
        dot2 = view.findViewById(R.id.dot2);
        dot3 = view.findViewById(R.id.dot3);


        play = view.findViewById(R.id.playInEpisodeDetails);
        download = view.findViewById(R.id.downloadButton3);


    }

    private void loadDetails() {


        String logoLink = tvShow.getLogo_path();
        System.out.println("Logo Link"+logoLink);

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
        if(logoLink.equals("")&&tvShow.getName()!=null){
            showName.setVisibility(View.VISIBLE);
            showName.setText(tvShow.getName());
        }


        String buttonText = "S" + episode.getSeason_number() + " E" + episode.getEpisode_number();
        System.out.println(buttonText);
        continueWatching.setText(buttonText);
//                            play.setText(buttonText);
        if(episode.getName()!=null){
            dot3.setVisibility(View.VISIBLE);
            episodeName.setVisibility(View.VISIBLE);
            episodeName.setText(episode.getName());
        }

//        if (episode.getName() != null) {
//            episodeName.setText(episode.getName());
//        }
        else {
            String name = "Episode" + episode.getEpisode_number();
            episodeName.setText(name);
        }
        if (episode.getVote_average() > 0) {
//            ratings.setVisibility(View.VISIBLE);
            dot1.setVisibility(View.VISIBLE);
            ratingsText.setVisibility(View.VISIBLE);
            String rating =(int) (episode.getVote_average() * 10) + "%";
            ratingsText.setText(rating);
        }
//        if (tvShowSeasonDetails.getPoster_path() != null) {
//            Glide.with(mActivity)
//                    .load(TMDB_IMAGE_BASE_URL + tvShowSeasonDetails.getPoster_path())
//                    .placeholder(new ColorDrawable(Color.BLACK))
//                    .into(poster);
//        } else {
//            if (tvShow.getPoster_path() != null) {
//                Glide.with(mActivity)
//                        .load(TMDB_IMAGE_BASE_URL + tvShow.getPoster_path())
//                        .placeholder(new ColorDrawable(Color.BLACK))
//                        .into(poster);
//            }
//        }
        if (episode.getStill_path() != null) {
            Glide.with(mActivity.getApplicationContext())
                    .load(Constants.TMDB_BACKDROP_IMAGE_BASE_URL + episode.getStill_path())
                    .placeholder(new ColorDrawable(Color.BLACK))
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                    .into(episodeStill);
        } else {
            if (tvShow.getPoster_path() != null) {
                Glide.with(mActivity)
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + tvShow.getPoster_path())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .into(episodeStill);
            }
        }
        if (episode.getOverview() != null) {
//            overview.setVisibility(View.VISIBLE);
            overviewText.setVisibility(View.VISIBLE);
            overviewText.setText(episode.getOverview());
        }
        if (episode.getAir_date() != null) {
            air_date.setVisibility(View.VISIBLE);
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd" , Locale.ENGLISH);
                Date date = formatter.parse(episode.getAir_date());
                SimpleDateFormat formatter2 = new SimpleDateFormat("MMMM dd , yyyy", Locale.ENGLISH);
                String strDate = formatter2.format(date);
                air_date_text.setText(strDate);
            } catch (ParseException e) {
                System.out.println("parse Exception Date"+e);
            }
        }
        if (episode.getRuntime() != 0) {
            String result = StringUtils.runtimeIntegerToString(episode.getRuntime());
            runtime.setVisibility(View.VISIBLE);
            runtime.setText(result);
        }
        if (!(tvShowSeasonDetails.getSeason_number() < 0)) {
            String season = "Season " + tvShowSeasonDetails.getSeason_number();
            seasonNumber.setText(season);
        }
        loadEpisodeFilesRecycler();

    }

    private void loadEpisodeFilesRecycler() {
        setOnClickListner();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(" " , "in thread");
                episodeFileList = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .episodeDao()
                        .byEpisodeId(episode.getId());
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("episodeFileList" , episodeFileList.toString());
                        recyclerViewEpisodeFiles = mActivity.findViewById(R.id.recyclerAvailableEpisodeFiles);
                        ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.VERTICAL , false);
//                        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));
                        recyclerViewEpisodeFiles.setLayoutManager(linearLayoutManager);
                        recyclerViewEpisodeFiles.setHasFixedSize(true);
                        fileAdapter = new FileItemAdapter(getContext() ,(List<MyMedia>)(List<?>) episodeFileList);
                        recyclerViewEpisodeFiles.setAdapter(fileAdapter);
                        fileAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
        thread.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FULLSCREEN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                currentPosition = data.getLongExtra("position", 0);
            }
        }
    }
    private void setOnClickListner() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                largestFile = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .episodeDao()
                        .byEpisodeIdLargest(episode.getId());
            }
        });
        thread.start();


        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("Settings" , Context.MODE_PRIVATE);
        boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING" , false);

        fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (savedEXT) {
                    //External Player
                    addToLastPlayed();
                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse(largestFile.getUrlString()));
                    intent.setDataAndType(Uri.parse(largestFile.getUrlString()) , "video/*");
                    startActivity(intent);
                } else {
                    //Play video

                    Intent in = new Intent(getActivity() , VideoPlayer.class);
                    in.putExtra("mediaUri", largestFile.getUrlString());
                    currentPosition = playerHelper.getCurrentPosition();
                    in.putExtra("position", currentPosition);
                    in.putExtra("url" , largestFile.getUrlString());
                    String season = String.valueOf(episode.getSeason_number());
                    String epsnum = String.valueOf(episode.getEpisode_number());
                    in.putExtra("season" , season);
                    in.putExtra("number" , epsnum);
                    in.putExtra("episode" , episode.getName());
                    in.putExtra("title" , tvShow.getName());
                    String tmdbId = String.valueOf(episode.getId());
                    in.putExtra("tmdbId", tmdbId);
                    startActivityForResult(in, FULLSCREEN_REQUEST_CODE);
                    Toast.makeText(getContext() , "Play" , Toast.LENGTH_LONG).show();
                    addToLastPlayed();
                }
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (savedEXT) {
                    // External Player
                    addToLastPlayed();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(largestFile.getUrlString()));
                    intent.setDataAndType(Uri.parse(largestFile.getUrlString()), "video/*");
                    startActivity(intent);
                } else {
                    // Play video using ExoPlayer within the fragment
                    playVideo(largestFile.getUrlString());
                    Toast.makeText(getContext(), "Play", Toast.LENGTH_LONG).show();
                    addToLastPlayed();
                    play.setVisibility(View.GONE);
                }
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 32) {
                    // Check if the app has the WRITE_EXTERNAL_STORAGE permission
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Request the permission if it is not granted
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);

                    } else {
                        // Permission is already granted, proceed with the download
                        startDownload();
                    }
                }else {
                    startDownload();
                }

            }


        });


    }

    private void startDownload() {
        String customFolderPath = "/nfgplus/series/";
        DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(largestFile.getUrlString());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, customFolderPath + largestFile.getFileName());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDescription("Downloading");
        long reference = manager.enqueue(request);
        Toast.makeText(getContext(), "Download Started", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the download
                startDownload();
            } else {
                // Permission denied, show a message or take appropriate action
                Toast.makeText(getContext(), "Write external storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void addToLastPlayed() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
                DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao().updatePlayed(episode.getId(), currentDateTime+" added");
            }
        });
        thread.start();
    }

    private void playVideo(String videoUrl) {
            playerHelper.initializePlayer(getContext(), playerView, videoUrl);
            playerHelper.seekTo(currentPosition);
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            fullScreen.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
        });
        String userId = manager.getCurrentUser().getUid();
        DatabaseReference userReference = databaseReference.child(userId).child("lastPosition");
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the last position from the database
                    Long lastPosition = dataSnapshot.getValue(Long.class);
                    if (lastPosition != null) {
                        // Update the startPosition with the retrieved value
                        startPosition = lastPosition;

                        // Seek the player to the last position
                        playerHelper.seekTo(startPosition);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
            }
        });

        boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;

        playerView.setVisibility(View.VISIBLE);


    }


    @Override
    public void onStop() {
        super.onStop();

            updateTrackSelectorParameters();
            updateStartPosition();
            currentPosition = playerHelper.getCurrentPosition();
            onPause();

    }
    public void onResume() {
        super.onResume();


            if (playerView != null) {
                playerView.onResume();
                playerHelper.seekTo(currentPosition);
            }


    }


    private void updateTrackSelectorParameters() {

    }

    private void updateStartPosition() {

            addToLastPlayed();
            startAutoPlay = playerHelper.getPlayWhenReady();
            startItemIndex = playerHelper.getCurrentMediaItemIndex();
            startPosition = Math.max(0, playerHelper.getCurrentPosition());
            String userId = manager.getCurrentUser().getUid();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
            DatabaseReference userReference = databaseReference.child(userId);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("lastPosition", startPosition);
            userMap.put("lastPlayed", currentDateTime);
            userReference.setValue(userMap);



    }


    @Override
    public void onPause() {
        super.onPause();

    }


}