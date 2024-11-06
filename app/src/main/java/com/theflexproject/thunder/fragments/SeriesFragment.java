package com.theflexproject.thunder.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.BannerRecyclerAdapter;
import com.theflexproject.thunder.adapter.DrakorBannerAdapter;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SeriesFragment extends BaseFragment{

    TextView drakorTitle;
    RecyclerView drakorView;
    DrakorBannerAdapter drakorAdapter;
    DrakorBannerAdapter.OnItemClickListener drakorListener;
    TextView trendingTitle;
    RecyclerView trendingView;
    DrakorBannerAdapter trendingAdapter;
    DrakorBannerAdapter.OnItemClickListener trendingListener;
    List<TVShow> seriesTrending;


    TextView newSeasonRecyclerViewTitle;
    RecyclerView newSeasonRecyclerView;
    MediaAdapter newSeasonRecyclerAdapter;
    MediaAdapter.OnItemClickListener topRatedShowsListener;
    MediaAdapter.OnItemClickListener newSeasonListener;
    TextView recommendedText;
    RecyclerView recommendedView;
    MediaAdapter recommendedAdapter;
    MediaAdapter.OnItemClickListener recommendedListener;
    List<TVShow> drakor;
    List<TVShow> newSeason;
    List<TVShow> topRatedShows;
    List<TVShow> recommendSeries;
    List<MyMedia> recommended;



    private SwipeRefreshLayout swipeRefreshLayout;

//    List<PairMovies> pairMoviesList;
//    List<PairTvShows> pairTvShowsList;

    public SeriesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_series_home, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout2);
        trendingTitle = view.findViewById(R.id.trendingSeries);
        trendingView = view.findViewById(R.id.trendingSeriesRecycler);
        drakorTitle = view.findViewById(R.id.drakor);
        drakorView = view.findViewById(R.id.drakorRecycler);
        newSeasonRecyclerViewTitle = view.findViewById(R.id.newSeason);
        newSeasonRecyclerView = view.findViewById(R.id.newSeasonRecycler);
        recommendedText = view.findViewById(R.id.topRatedTVShows);
        recommendedView = view.findViewById(R.id.topRatedTVShowsRecycler);

        loadTrendingSeries();
        loadDrakor();
        loadNewSeason();
        loadTopRatedShows();
        setOnClickListner();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your refresh logic here
                refreshData();

            }
        });
    }
    private void refreshData() {
        // Implement your refresh logic here
        // For example, you can re-fetch the data or perform any necessary updates
        // Once the refresh is complete, call setRefreshing(false) on the SwipeRefreshLayout
        // to indicate that the refresh has finished.

        loadTrendingSeries();
        loadTopRatedShows();
        loadNewSeason();
        loadDrakor();


        swipeRefreshLayout.setRefreshing(false);

    }

    public boolean onBackPressed(){

        return false;
    }

    private void  loadTrendingSeries() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tmdbTrending tvTrending = new tmdbTrending();
                List<String> trendingIds = tvTrending.getSeriesTrending();
                seriesTrending = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .loadAllByIds(trendingIds);
                if(seriesTrending!=null && seriesTrending.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            trendingTitle.setVisibility(View.VISIBLE);
                            trendingView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            trendingView.setHasFixedSize(true);
                            trendingAdapter = new DrakorBannerAdapter(getContext(), seriesTrending , trendingListener);
                            trendingView.setAdapter(trendingAdapter);
                        }
                    });
                }

            }});
        thread.start();

    }


    private void loadDrakor()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                drakor = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .getDrakor();
                if(drakor!=null && drakor.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            drakorTitle.setVisibility(View.VISIBLE);
                            drakorView.setLayoutManager(linearLayoutManager2);
                            drakorView.setHasFixedSize(true);
                            drakorAdapter = new DrakorBannerAdapter(getContext(), drakor , drakorListener);
                            drakorView.setAdapter(drakorAdapter);
                        }
                    });
                }
            }});
        thread.start();
    }
    private void loadNewSeason(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                newSeason = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .getNewShows();
                if(newSeason!=null && newSeason.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            newSeasonRecyclerViewTitle.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView.setLayoutManager(linearLayoutManager3);
                            newSeasonRecyclerView.setHasFixedSize(true);
                            newSeasonRecyclerAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) newSeason , newSeasonListener);
                            newSeasonRecyclerView.setAdapter(newSeasonRecyclerAdapter);
                        }
                    });
                }
            }});
        thread.start();
    }
    private void loadTopRatedShows(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                topRatedShows = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .getTopRated();
                recommendSeries = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .getrecomendation();
                recommended = new ArrayList<>();
                recommended.addAll(recommendSeries);
                recommended.addAll(topRatedShows);
                if(recommended!=null && recommended.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            recommendedText.setVisibility(View.VISIBLE);
                            Collections.shuffle(recommended);
                            recommendedView.setLayoutManager(linearLayoutManager3);
                            recommendedView.setHasFixedSize(true);
                            recommendedAdapter = new MediaAdapter(getContext() ,recommended , recommendedListener);
                            recommendedView.setAdapter(recommendedAdapter);
                        }
                    });
                }
            }});
        thread.start();
    }

    private void setOnClickListner() {
        trendingListener = new DrakorBannerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(seriesTrending.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };
        drakorListener = new DrakorBannerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(drakor.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };


        newSeasonListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(newSeason.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };
        recommendedListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                TVShow recomId = ((TVShow) recommended.get(position));
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(recomId.getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };
    }
}
