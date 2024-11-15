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
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
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

    MaterialButton drakorTitle,trendingTitle, newSeasonRecyclerViewTitle,
            recommendedText;
    RecyclerView drakorView, trendingView,
            newSeasonRecyclerView, recommendedView;
    MediaAdapter drakorAdapter, newSeasonRecyclerAdapter, recommendedAdapter;
    MediaAdapter.OnItemClickListener drakorListener,newSeasonListener, recommendedListener;


    DrakorBannerAdapter trendingAdapter;
    DrakorBannerAdapter.OnItemClickListener trendingListener;
    List<TVShow> seriesTrending,drakor,newSeason,topRatedShows,recommendSeries;
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

        refreshData();
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
                List<TVShow> limitedTrending = drakor.size() > 10 ? drakor.subList(0, 10) : drakor;
                List<MyMedia> all = new ArrayList<>(drakor);
                if(drakor!=null && drakor.size()>0){
                    mActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            drakorTitle.setVisibility(View.VISIBLE);
                            drakorView.setLayoutManager(linearLayoutManager2);
                            drakorView.setHasFixedSize(true);
                            drakorAdapter = new MediaAdapter(getContext(), (List<MyMedia>)(List<?>) limitedTrending , drakorListener);
                            drakorView.setAdapter(drakorAdapter);
                            drakorTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ShowAllFragment showAllFragment = new ShowAllFragment(all);
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.add(R.id.container, showAllFragment); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });
                }
                else {
                    drakorTitle.setVisibility(View.GONE);
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
                List<TVShow> limitedTrending = newSeason.size() > 10 ? newSeason.subList(0, 10) : newSeason;
                List<MyMedia> all = new ArrayList<>(newSeason);
                if(newSeason!=null && newSeason.size()>0){
                    mActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            newSeasonRecyclerViewTitle.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView.setLayoutManager(linearLayoutManager3);
                            newSeasonRecyclerView.setHasFixedSize(true);
                            newSeasonRecyclerAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) limitedTrending , newSeasonListener);
                            newSeasonRecyclerView.setAdapter(newSeasonRecyclerAdapter);
                            newSeasonRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ShowAllFragment showAllFragment = new ShowAllFragment(all);
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.add(R.id.container, showAllFragment); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
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
                List<MyMedia> limitedTrending = recommended.size() > 10 ? recommended.subList(0, 10) : recommended;
                if(recommended!=null && recommended.size()>0){

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            recommendedText.setVisibility(View.VISIBLE);
                            Collections.shuffle(recommended);
                            Collections.shuffle(limitedTrending);
                            recommendedView.setLayoutManager(linearLayoutManager3);
                            recommendedView.setHasFixedSize(true);
                            recommendedAdapter = new MediaAdapter(getContext() ,limitedTrending , recommendedListener);
                            recommendedView.setAdapter(recommendedAdapter);
                            recommendedText.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ShowAllFragment showAllFragment = new ShowAllFragment(recommended);
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.add(R.id.container, showAllFragment); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
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
        drakorListener = new MediaAdapter.OnItemClickListener() {
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
