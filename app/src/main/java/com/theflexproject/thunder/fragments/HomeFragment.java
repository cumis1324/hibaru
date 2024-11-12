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
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
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
import com.theflexproject.thunder.utils.tmdbTrending;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class HomeFragment extends BaseFragment {

    BannerRecyclerAdapter recentlyAddedRecyclerAdapter, topRatedMoviesRecyclerViewAdapter;
    MediaAdapter recentlyReleasedRecyclerViewAdapter, trendingMoviesRecyclerAdapter,
            lastPlayedMoviesRecyclerViewAdapter, watchlistRecyclerViewAdapter,
            filmIndoAdapter;

    TextView recentlyAddedRecyclerViewTitle,
            verifTitle;
    RecyclerView recentlyAddedRecyclerView, recentlyReleasedRecyclerView,
            filmIndoView,topRatedMoviesRecyclerView, trendingRecyclerView,
            lastPlayedMoviesRecyclerView, watchlistRecyclerView;

    MaterialButton trendingTitle,lastPlayedMoviesRecyclerViewTitle,
            topRatedMoviesRecyclerViewTitle, recentlyReleasedRecyclerViewTitle, filmIndoTitle,
            watchlistRecyclerViewTitle;


    List<Movie> recentlyAddedMovies, recentlyReleasedMovies, topRatedMovies,
            trending, lastPlayedList, fav, played, ogMovies, topOld, filmIndo;
    List<MyMedia> ogtop, someRecom;

    BannerRecyclerAdapter.OnItemClickListener recentlyAddedListener,topRatedMoviesListener;
    MediaAdapter.OnItemClickListener recentlyReleasedListener,
            trendingListener,lastPlayedListener,watchlistListener,filmIndoListener;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String itemId = intent.getStringExtra("itemId");
            openMovieDetailsFragment(itemId);
        }
    };


    private SwipeRefreshLayout swipeRefreshLayout;

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
        FirebaseManager firebaseManager;
        firebaseManager = new FirebaseManager();
        FirebaseUser currentUser;
        currentUser = firebaseManager.getCurrentUser();
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        verifTitle = view.findViewById(R.id.verifTitle);
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
                else {
                    watchlistRecyclerViewTitle.setVisibility(View.GONE);
                    watchlistRecyclerView.setVisibility(View.GONE);
                }
            }});
        thread.start();
    }

    //load refresh

    // RECYLER MENU HOME
    private void  loadRecentlyAddedMovies() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tmdbTrending movieTrending = new tmdbTrending();
                List<String> trendingIds = movieTrending.getMovieTrending();
                recentlyAddedMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .loadAllByIds(trendingIds);
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
                else {
                    recentlyAddedRecyclerViewTitle.setVisibility(View.GONE);
                    recentlyAddedRecyclerView.setVisibility(View.GONE);
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
                            List<Movie> limitedTrending = recentlyAddedMovies.size() > 10 ? recentlyAddedMovies.subList(0, 10) : recentlyAddedMovies;
                            List<MyMedia> all = new ArrayList<>(recentlyAddedMovies);
                            ScaleCenterItemLayoutManager linearLayoutManager1 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            recentlyReleasedRecyclerViewTitle.setVisibility(View.VISIBLE);

                            recentlyReleasedRecyclerView.setLayoutManager(linearLayoutManager1);
                            recentlyReleasedRecyclerView.setHasFixedSize(true);
                            recentlyReleasedRecyclerViewAdapter = new MediaAdapter(getContext(),(List<MyMedia>)(List<?>) limitedTrending, recentlyReleasedListener);
                            recentlyReleasedRecyclerView.setAdapter(recentlyReleasedRecyclerViewAdapter);
                            recentlyReleasedRecyclerView.setNestedScrollingEnabled(false);
                            recentlyReleasedRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, ShowAllFragment.newInstance(all)); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });

                }
                else {
                    recentlyReleasedRecyclerViewTitle.setVisibility(View.GONE);
                    recentlyReleasedRecyclerView.setVisibility(View.GONE);
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
                        List<Movie> limitedTrending = topRatedMovies.size() > 10 ? topRatedMovies.subList(0, 10) : topRatedMovies;
                        List<MyMedia> all = new ArrayList<>(topRatedMovies);
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            topRatedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);

                            topRatedMoviesRecyclerView.setLayoutManager(linearLayoutManager2);
                            topRatedMoviesRecyclerView.setHasFixedSize(true);
                            topRatedMoviesRecyclerViewAdapter = new BannerRecyclerAdapter(getContext(), limitedTrending , topRatedMoviesListener);
                            topRatedMoviesRecyclerView.setAdapter(topRatedMoviesRecyclerViewAdapter);
                            topRatedMoviesRecyclerView.setNestedScrollingEnabled(false);
                            topRatedMoviesRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, ShowAllFragment.newInstance(all)); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });
                }
                else {
                    topRatedMoviesRecyclerViewTitle.setVisibility(View.GONE);
                    topRatedMoviesRecyclerView.setVisibility(View.GONE);
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
                    List<Movie> limitedTrending = trending.size() > 10 ? trending.subList(0, 10) : trending;
                    List<MyMedia> all = new ArrayList<>(trending);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            trendingTitle.setVisibility(View.VISIBLE);

                            trendingRecyclerView.setLayoutManager(linearLayoutManager2);
                            trendingRecyclerView.setHasFixedSize(true);
                            trendingMoviesRecyclerAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) limitedTrending , trendingListener);
                            trendingRecyclerView.setAdapter(trendingMoviesRecyclerAdapter);
                            trendingRecyclerView.setNestedScrollingEnabled(false);
                            trendingTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, ShowAllFragment.newInstance(all)); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });
                }
                else {
                    trendingTitle.setVisibility(View.GONE);
                    trendingRecyclerView.setVisibility(View.GONE);
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
                        List<MyMedia> limitedTrending = someRecom.size() > 10 ? someRecom.subList(0, 10) : someRecom;

                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            lastPlayedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(someRecom);
                            Collections.shuffle(limitedTrending);
                            //lastPlayedMoviesRecyclerView.setVisibility(View.VISIBLE);
                            lastPlayedMoviesRecyclerView.setLayoutManager(linearLayoutManager3);
                            lastPlayedMoviesRecyclerView.setHasFixedSize(true);
                            lastPlayedMoviesRecyclerViewAdapter = new MediaAdapter(getContext() ,limitedTrending , lastPlayedListener);
                            lastPlayedMoviesRecyclerView.setAdapter(lastPlayedMoviesRecyclerViewAdapter);
                            lastPlayedMoviesRecyclerView.setNestedScrollingEnabled(false);
                            lastPlayedMoviesRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, ShowAllFragment.newInstance(someRecom)); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });

                }
                else {
                    lastPlayedMoviesRecyclerViewTitle.setVisibility(View.GONE);
                    lastPlayedMoviesRecyclerView.setVisibility(View.GONE);
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
                            List<Movie> limitedTrending = filmIndo.size() > 10 ? filmIndo.subList(0, 10) : filmIndo;
                            List<MyMedia> all = new ArrayList<>(filmIndo);
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            filmIndoTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(filmIndo);
                            Collections.shuffle(limitedTrending);
                            filmIndoView.setLayoutManager(linearLayoutManager2);
                            filmIndoView.setHasFixedSize(true);
                            filmIndoAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) limitedTrending , filmIndoListener);
                            filmIndoView.setAdapter(filmIndoAdapter);
                            filmIndoView.setNestedScrollingEnabled(false);
                            filmIndoTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Navigate to a new fragment or activity with all data
                                    FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.container, ShowAllFragment.newInstance(all)); // Pass data to new fragment
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        }
                    });
                }
                else {
                    filmIndoTitle.setVisibility(View.GONE);
                    filmIndoView.setVisibility(View.GONE);
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

    }


}