package com.theflexproject.thunder.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.RefreshJobService;
import com.theflexproject.thunder.adapter.BannerRecyclerAdapter;
import com.theflexproject.thunder.adapter.DrakorBannerAdapter;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class HomeFragment extends BaseFragment {

    BannerRecyclerAdapter recentlyAddedRecyclerAdapter;
    MediaAdapter recentlyReleasedRecyclerViewAdapter;
    BannerRecyclerAdapter topRatedMoviesRecyclerViewAdapter;
    MediaAdapter trendingMoviesRecyclerAdapter;
    MediaAdapter lastPlayedMoviesRecyclerViewAdapter;
    MediaAdapter watchlistRecyclerViewAdapter;

    MediaAdapter topRatedShowsRecyclerAdapter;
    MediaAdapter newSeasonRecyclerAdapter;



    TextView recentlyAddedRecyclerViewTitle;
    RecyclerView recentlyAddedRecyclerView;

    TextView recentlyReleasedRecyclerViewTitle;
    RecyclerView recentlyReleasedRecyclerView;

    TextView topRatedMoviesRecyclerViewTitle;
    TextView trendingTitle, verifTitle;

    TextView filmIndoTitle;
    RecyclerView filmIndoView;
    MediaAdapter filmIndoAdapter;
    MediaAdapter.OnItemClickListener filmIndoListener;
    TextView drakorTitle;
    RecyclerView drakorView;
    DrakorBannerAdapter drakorAdapter;
    DrakorBannerAdapter.OnItemClickListener drakorListener;
    RecyclerView topRatedMoviesRecyclerView;
    RecyclerView trendingRecyclerView;

    TextView lastPlayedMoviesRecyclerViewTitle;
    RecyclerView lastPlayedMoviesRecyclerView;

    TextView watchlistRecyclerViewTitle;
    RecyclerView watchlistRecyclerView;

    TextView topRatedShowsRecyclerViewTitle;
    RecyclerView topRatedShowsRecyclerView;

    TextView newSeasonRecyclerViewTitle;
    RecyclerView newSeasonRecyclerView;

    List<Movie> recentlyAddedMovies;
    List<Movie> recentlyReleasedMovies;
    List<Movie> topRatedMovies;
    List<Movie> trending;
    List<Movie> lastPlayedList, fav, played;
    List<Movie> ogMovies;
    List<Movie> topOld;
    List<Movie> filmIndo;
    List<TVShow> drakor;
    List<MyMedia> ogtop;
    List<MyMedia> someRecom;
    List<TVShow> newSeason;
    List<TVShow> topRatedShows;

    BannerRecyclerAdapter.OnItemClickListener recentlyAddedListener;
    MediaAdapter.OnItemClickListener recentlyReleasedListener;
    BannerRecyclerAdapter.OnItemClickListener topRatedMoviesListener;
    MediaAdapter.OnItemClickListener trendingListener;
    MediaAdapter.OnItemClickListener lastPlayedListener;
    MediaAdapter.OnItemClickListener watchlistListener;
    MediaAdapter.OnItemClickListener topRatedShowsListener;
    MediaAdapter.OnItemClickListener newSeasonListener;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String itemId = intent.getStringExtra("itemId");
            openMovieDetailsFragment(itemId);
        }
    };


    private SwipeRefreshLayout swipeRefreshLayout;

//    List<PairMovies> pairMoviesList;
//    List<PairTvShows> pairTvShowsList;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        verifTitle = view.findViewById(R.id.verifTitle);
        FirebaseManager firebaseManager;
        firebaseManager = new FirebaseManager();
        FirebaseUser currentUser;
        currentUser = firebaseManager.getCurrentUser();
        if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
            verifTitle.setVisibility(View.VISIBLE);
        }
        watchlistRecyclerView = view.findViewById(R.id.watchListMediaRecycler);
        trendingRecyclerView = view.findViewById(R.id.trendingRecycler);
        recentlyAddedRecyclerView = view.findViewById(R.id.recentlyAddedRecycler);
        recentlyReleasedRecyclerView = view.findViewById(R.id.recentlyReleasedMoviesRecycler);
        topRatedMoviesRecyclerView = view.findViewById(R.id.topRatedMoviesRecycler);
        lastPlayedMoviesRecyclerView = view.findViewById(R.id.lastPlayedMoviesRecycler);
        filmIndoView = view.findViewById(R.id.filmIndoRecycler);
        trendingTitle = view.findViewById(R.id.trending);
        recentlyReleasedRecyclerViewTitle = view.findViewById(R.id.newReleasesMovies);
        topRatedMoviesRecyclerViewTitle = view.findViewById(R.id.topRatedMovies);
        recentlyAddedRecyclerViewTitle = view.findViewById(R.id.recentlyAdded);
        lastPlayedMoviesRecyclerViewTitle = view.findViewById(R.id.lastPlayedMovies2);
        watchlistRecyclerViewTitle = view.findViewById(R.id.watchListMedia1);
        filmIndoTitle = view.findViewById(R.id.filmIndo);
        loadTrending();
        loadRecentlyAddedMovies();
        loadRecentlyReleasedMovies();
        loadTopRatedMovies();

        loadLastPlayedMovies();
        loadWatchlist();

        loadFilmIndo();
        setOnClickListner();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your refresh logic here
                refreshData();

            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("MovieDetailsFragment");
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(receiver);
    }

    private void openMovieDetailsFragment(String itemId) {
        // Create a Bundle to pass data to the fragment
        Bundle bundle = new Bundle();
        bundle.putString("movieId", itemId);

        // Create an instance of your MovieDetailsFragment
        MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment();
        movieDetailsFragment.setArguments(bundle);

        // Use a FragmentManager to replace or add the fragment
        FragmentManager fragmentManager = getChildFragmentManager(); // Use getChildFragmentManager()

        // Example: Replace the current fragment with MovieDetailsFragment
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.container, movieDetailsFragment)
                .addToBackStack(null)
                .commit();
    }


    private void loadWatchlist() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ogMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getOgMovies();
                topOld = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getTopOld();
                ogtop = new ArrayList<>();
                ogtop.addAll(topOld);
                ogtop.addAll(ogMovies);

                if(ogtop!=null && ogtop.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Random random = new Random();

                            Collections.shuffle(ogtop);

                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                           watchlistRecyclerViewTitle.setVisibility(View.VISIBLE);

                            //watchlistRecyclerView.setVisibility(View.VISIBLE);
                            watchlistRecyclerView.setLayoutManager(linearLayoutManager3);
                            watchlistRecyclerView.setHasFixedSize(true);
                            watchlistRecyclerViewAdapter = new MediaAdapter(getContext() ,ogtop , watchlistListener);
                            watchlistRecyclerView.setAdapter(watchlistRecyclerViewAdapter);
                        }
                    });

                }
            }});
        thread.start();
    }

    //load refresh
    private void refreshData() {
        // Implement your refresh logic here
        // For example, you can re-fetch the data or perform any necessary updates
        // Once the refresh is complete, call setRefreshing(false) on the SwipeRefreshLayout
        // to indicate that the refresh has finished.
        loadRecentlyAddedMovies();
        loadRecentlyReleasedMovies();
        loadTopRatedMovies();
        loadLastPlayedMovies();
        loadWatchlist();

        loadTrending();

        loadFilmIndo();

        swipeRefreshLayout.setRefreshing(false);

    }
    // RECYLER MENU HOME
    private void  loadRecentlyAddedMovies() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recentlyAddedMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getTrending();
                if(recentlyAddedMovies!=null && recentlyAddedMovies.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            recentlyAddedRecyclerViewTitle.setVisibility(View.VISIBLE);

                            recentlyAddedRecyclerView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            recentlyAddedRecyclerView.setHasFixedSize(true);
                            recentlyAddedRecyclerAdapter = new BannerRecyclerAdapter(getContext(), recentlyAddedMovies , recentlyAddedListener);
                            recentlyAddedRecyclerView.setAdapter(recentlyAddedRecyclerAdapter);
                            recentlyAddedRecyclerView.setNestedScrollingEnabled(false);
                        }
                    });
                }

           }});
        thread.start();

    }
    private void loadRecentlyReleasedMovies () {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recentlyReleasedMovies  = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getrecentreleases();
                if(recentlyReleasedMovies!=null && recentlyReleasedMovies.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager1 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            recentlyReleasedRecyclerViewTitle.setVisibility(View.VISIBLE);

                            recentlyReleasedRecyclerView.setLayoutManager(linearLayoutManager1);
                            recentlyReleasedRecyclerView.setHasFixedSize(true);
                            recentlyReleasedRecyclerViewAdapter = new MediaAdapter(getContext(),(List<MyMedia>)(List<?>) recentlyReleasedMovies, recentlyReleasedListener);
                            recentlyReleasedRecyclerView.setAdapter(recentlyReleasedRecyclerViewAdapter);
                            recentlyReleasedRecyclerView.setNestedScrollingEnabled(false);
                        }
                    });

                }
            }});
        thread.start();
    }
    private void loadTopRatedMovies()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                topRatedMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getTopRated();
                if(topRatedMovies!=null && topRatedMovies.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            topRatedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);

                            topRatedMoviesRecyclerView.setLayoutManager(linearLayoutManager2);
                            topRatedMoviesRecyclerView.setHasFixedSize(true);
                            topRatedMoviesRecyclerViewAdapter = new BannerRecyclerAdapter(getContext(), topRatedMovies , topRatedMoviesListener);
                            topRatedMoviesRecyclerView.setAdapter(topRatedMoviesRecyclerViewAdapter);
                            topRatedMoviesRecyclerView.setNestedScrollingEnabled(false);
                        }
                    });
                }
            }});
        thread.start();
    }
    private void loadTrending()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                trending = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getrecentlyadded();
                if(trending!=null && trending.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            trendingTitle.setVisibility(View.VISIBLE);

                            trendingRecyclerView.setLayoutManager(linearLayoutManager2);
                            trendingRecyclerView.setHasFixedSize(true);
                            trendingMoviesRecyclerAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) trending , trendingListener);
                            trendingRecyclerView.setAdapter(trendingMoviesRecyclerAdapter);
                            trendingRecyclerView.setNestedScrollingEnabled(false);
                        }
                    });
                }

            }});
        thread.start();
    }
    private void loadLastPlayedMovies() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lastPlayedList = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getrecomendation();

                played = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getMoreMovied();

                fav = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getRecombyfav();

                someRecom = new ArrayList<>();
                someRecom.addAll(played);
                someRecom.addAll(fav);
                someRecom.addAll(lastPlayedList);
                if(someRecom!=null && someRecom.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            lastPlayedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(someRecom);
                            //lastPlayedMoviesRecyclerView.setVisibility(View.VISIBLE);
                            lastPlayedMoviesRecyclerView.setLayoutManager(linearLayoutManager3);
                            lastPlayedMoviesRecyclerView.setHasFixedSize(true);
                            lastPlayedMoviesRecyclerViewAdapter = new MediaAdapter(getContext() ,someRecom , lastPlayedListener);
                            lastPlayedMoviesRecyclerView.setAdapter(lastPlayedMoviesRecyclerViewAdapter);
                            lastPlayedMoviesRecyclerView.setNestedScrollingEnabled(false);
                        }
                    });

                }
            }});
        thread.start();
    }
    private void loadFilmIndo()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                filmIndo = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getFilmIndo();
                if(filmIndo!=null && filmIndo.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            filmIndoTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(filmIndo);
                            filmIndoView.setLayoutManager(linearLayoutManager2);
                            filmIndoView.setHasFixedSize(true);
                            filmIndoAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) filmIndo , filmIndoListener);
                            filmIndoView.setAdapter(filmIndoAdapter);
                            filmIndoView.setNestedScrollingEnabled(false);
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
                            drakorTitle = mActivity.findViewById(R.id.drakor);
                            drakorTitle.setVisibility(View.VISIBLE);
                            drakorView = mActivity.findViewById(R.id.drakorRecycler);
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
                            newSeasonRecyclerViewTitle = mActivity.findViewById(R.id.newSeason);
                            newSeasonRecyclerViewTitle.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView = mActivity.findViewById(R.id.newSeasonRecycler);
                            newSeasonRecyclerView.setVisibility(View.VISIBLE);
                            newSeasonRecyclerView = mActivity.findViewById(R.id.newSeasonRecycler);
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
                if(topRatedShows!=null && topRatedShows.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);
                            topRatedShowsRecyclerViewTitle = mActivity.findViewById(R.id.topRatedTVShows);
                            topRatedShowsRecyclerViewTitle.setVisibility(View.VISIBLE);
                            topRatedShowsRecyclerView = mActivity.findViewById(R.id.topRatedTVShowsRecycler);
                            topRatedShowsRecyclerView.setLayoutManager(linearLayoutManager3);
                            topRatedShowsRecyclerView.setHasFixedSize(true);
                            topRatedShowsRecyclerAdapter = new MediaAdapter(getContext() , (List<MyMedia>)(List<?>) topRatedShows , topRatedShowsListener);
                            topRatedShowsRecyclerView.setAdapter(topRatedShowsRecyclerAdapter);
                        }
                    });
                }
            }});
        thread.start();
    }


    //KLIK LISTENER
    private void setOnClickListner() {
        filmIndoListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(filmIndo.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();

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
        recentlyAddedListener = new BannerRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(recentlyAddedMovies.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
            }
        };
        recentlyReleasedListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(recentlyReleasedMovies.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();

            }
        };
        topRatedMoviesListener =  new BannerRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(topRatedMovies.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
            }
        };
        trendingListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(trending.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
            }
        };
        lastPlayedListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                Movie recomId = ((Movie) someRecom.get(position));
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(recomId.getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
            }
        };

        watchlistListener = new MediaAdapter.OnItemClickListener() {

            @Override
            public void onClick(View view, int position) {
                Movie ogId = ((Movie) ogtop.get(position));
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(ogId.getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,movieDetailsFragment).addToBackStack(null).commit();
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
        topRatedShowsListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(topRatedShows.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };
    }


}