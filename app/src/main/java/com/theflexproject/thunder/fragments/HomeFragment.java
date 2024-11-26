package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.utils.ColapsingTitle.collapseTitle;
import static com.theflexproject.thunder.utils.ColapsingTitle.expandTitle;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.theflexproject.thunder.DetailActivity;
import com.theflexproject.thunder.MainActivity;
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
import com.theflexproject.thunder.model.SharedViewModel;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.FetchMovie;
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


    List<Movie> recentlyAddedMovies,allrecentlyAddedMovies, recentlyReleasedMovies,allrecentlyReleasedMovies, topRatedMovies,
            trending, lastPlayedList, fav, played, ogMovies, topOld, filmIndo;
    List<MyMedia> ogtop, someRecom;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String itemId = intent.getStringExtra("itemId");
            openMovieDetailsFragment(itemId);
        }
    };


    private NestedScrollView nestedScrollView;
    private TextView homeTitle;
    private boolean isTitleVisible = true; // Flag untuk visibilitas title
    private Handler handler = new Handler(Looper.getMainLooper()); // Untuk debounce
    private Runnable scrollRunnable;
    private FragmentManager fragmentManager;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        loadUI(view);
        return view;

    }

    private void loadUI(View view) {
        FirebaseManager firebaseManager;
        firebaseManager = new FirebaseManager();
        FirebaseUser currentUser;
        currentUser = firebaseManager.getCurrentUser();
        verifTitle = view.findViewById(R.id.verifTitle);
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
        homeTitle = mActivity.findViewById(R.id.homeTitle);
        nestedScrollView = view.findViewById(R.id.nestedMovieHome);
        fragmentManager = mActivity.getSupportFragmentManager();
        if ("M20Oxpp64gZ480Lqus4afv6x2n63".equals(currentUser.getUid())) {
            verifTitle.setVisibility(View.VISIBLE);
            recentlyAddedRecyclerViewTitle.setVisibility(View.GONE);
            topRatedMoviesRecyclerViewTitle.setVisibility(View.GONE);
            lastPlayedMoviesRecyclerViewTitle.setVisibility(View.GONE);
            watchlistRecyclerViewTitle.setVisibility(View.GONE);
            filmIndoTitle.setVisibility(View.GONE);
            loadTrending();
            loadRecentlyReleasedMovies();
        }else{


            // Tambahkan OnScrollChangeListener ke NestedScrollView
            nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    // Batalkan scrollRunnable jika ada event scroll baru
                    if (scrollRunnable != null) {
                        handler.removeCallbacks(scrollRunnable);
                    }

                    scrollRunnable = () -> {
                        if (scrollY > oldScrollY && isTitleVisible) {
                            // Scroll ke bawah: sembunyikan title
                            isTitleVisible = false;
                            collapseTitle(homeTitle);
                        } else if (scrollY < oldScrollY && !isTitleVisible) {
                            // Scroll ke atas: tampilkan title
                            isTitleVisible = true;
                            expandTitle(homeTitle);
                        }
                    };

                    // Jalankan scrollRunnable setelah debounce (200ms)
                    handler.postDelayed(scrollRunnable, 200);
                }
            });



            loadRecentlyAddedMovies();
            loadRecentlyReleasedMovies();
            loadTopRatedMovies();
            loadLastPlayedMovies();
            loadWatchlist();

            loadTrending();

            loadFilmIndo();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("MovieDetailsFragment");
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
        }

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
                ogMovies = FetchMovie.getOldGold(mActivity);
                topOld = FetchMovie.getTopOld(mActivity);
                ogtop = new ArrayList<>();
                ogtop.addAll(topOld);
                ogtop.addAll(ogMovies);
                List<MyMedia> limitedTrending = ogtop.size() > 10 ? ogtop.subList(0, 10) : ogtop;
                if(ogtop!=null && ogtop.size()>0){

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Random random = new Random();

                            Collections.shuffle(limitedTrending);

                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            watchlistRecyclerViewTitle.setVisibility(View.VISIBLE);

                            //watchlistRecyclerView.setVisibility(View.VISIBLE);
                            watchlistRecyclerView.setLayoutManager(linearLayoutManager3);
                            watchlistRecyclerView.setHasFixedSize(true);
                            watchlistRecyclerViewAdapter = new MediaAdapter(getContext() ,limitedTrending , fragmentManager);
                            watchlistRecyclerView.setAdapter(watchlistRecyclerViewAdapter);
                            watchlistRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ShowAllFragment showAllFragment = new ShowAllFragment(ogtop);
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
                            recentlyAddedRecyclerAdapter = new BannerRecyclerAdapter(getContext(), recentlyAddedMovies , fragmentManager);
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
                recentlyReleasedMovies  = FetchMovie.getRecentRelease(mActivity);
                allrecentlyReleasedMovies  = FetchMovie.getAllRecentRelease(mActivity);
                List<MyMedia> all = new ArrayList<>(allrecentlyReleasedMovies);
                if(recentlyReleasedMovies!=null && recentlyReleasedMovies.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            ScaleCenterItemLayoutManager linearLayoutManager1 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            recentlyReleasedRecyclerViewTitle.setVisibility(View.VISIBLE);

                            recentlyReleasedRecyclerView.setLayoutManager(linearLayoutManager1);
                            recentlyReleasedRecyclerView.setHasFixedSize(true);
                            recentlyReleasedRecyclerViewAdapter = new MediaAdapter(getContext(),(List<MyMedia>)(List<?>) recentlyReleasedMovies, fragmentManager);
                            recentlyReleasedRecyclerView.setAdapter(recentlyReleasedRecyclerViewAdapter);
                            recentlyReleasedRecyclerView.setNestedScrollingEnabled(false);
                            recentlyReleasedRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
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
    private void loadTopRatedMovies()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                topRatedMovies = FetchMovie.getTopRated(mActivity);
                List<Movie> limitedTrending = topRatedMovies.size() > 10 ? topRatedMovies.subList(0, 10) : topRatedMovies;
                List<MyMedia> all = new ArrayList<>(topRatedMovies);
                if(topRatedMovies!=null && topRatedMovies.size()>0){
                    mActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            topRatedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);

                            topRatedMoviesRecyclerView.setLayoutManager(linearLayoutManager2);
                            topRatedMoviesRecyclerView.setHasFixedSize(true);
                            topRatedMoviesRecyclerViewAdapter = new BannerRecyclerAdapter(getContext(), limitedTrending , fragmentManager);
                            topRatedMoviesRecyclerView.setAdapter(topRatedMoviesRecyclerViewAdapter);
                            topRatedMoviesRecyclerView.setNestedScrollingEnabled(false);
                            topRatedMoviesRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
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
    private void loadTrending()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                trending = FetchMovie.getRecentlyAdded(mActivity);
                allrecentlyAddedMovies = FetchMovie.getAllRecentAdded(mActivity);
                List<MyMedia> all = new ArrayList<>(allrecentlyAddedMovies);
                if(trending!=null && trending.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            trendingTitle.setVisibility(View.VISIBLE);

                            trendingRecyclerView.setLayoutManager(linearLayoutManager2);
                            trendingRecyclerView.setHasFixedSize(true);
                            trendingMoviesRecyclerAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) trending, fragmentManager);
                            trendingRecyclerView.setAdapter(trendingMoviesRecyclerAdapter);
                            trendingRecyclerView.setNestedScrollingEnabled(false);
                            trendingTitle.setOnClickListener(new View.OnClickListener() {
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
    private void loadLastPlayedMovies() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lastPlayedList = FetchMovie.getRecommendation(mActivity);

                played = FetchMovie.getMore(mActivity);

                fav = FetchMovie.getRecombyFav(mActivity);

                someRecom = new ArrayList<>();
                someRecom.addAll(played);
                someRecom.addAll(fav);
                someRecom.addAll(lastPlayedList);
                List<MyMedia> limitedTrending = someRecom.size() > 10 ? someRecom.subList(0, 10) : someRecom;
                if(someRecom!=null && someRecom.size()>0){
                    mActivity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            lastPlayedMoviesRecyclerViewTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(someRecom);
                            Collections.shuffle(limitedTrending);
                            //lastPlayedMoviesRecyclerView.setVisibility(View.VISIBLE);
                            lastPlayedMoviesRecyclerView.setLayoutManager(linearLayoutManager3);
                            lastPlayedMoviesRecyclerView.setHasFixedSize(true);
                            lastPlayedMoviesRecyclerViewAdapter = new MediaAdapter(getContext() ,limitedTrending , fragmentManager);
                            lastPlayedMoviesRecyclerView.setAdapter(lastPlayedMoviesRecyclerViewAdapter);
                            lastPlayedMoviesRecyclerView.setNestedScrollingEnabled(false);
                            lastPlayedMoviesRecyclerViewTitle.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ShowAllFragment showAllFragment = new ShowAllFragment(someRecom);
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
    private void loadFilmIndo()  {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                filmIndo = FetchMovie.getFilmIndo(mActivity);
                List<Movie> limitedTrending = filmIndo.size() > 10 ? filmIndo.subList(0, 10) : filmIndo;
                List<MyMedia> all = new ArrayList<>(filmIndo);
                if(filmIndo!=null && filmIndo.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            ScaleCenterItemLayoutManager linearLayoutManager2 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            filmIndoTitle.setVisibility(View.VISIBLE);
                            Collections.shuffle(filmIndo);
                            Collections.shuffle(limitedTrending);
                            filmIndoView.setLayoutManager(linearLayoutManager2);
                            filmIndoView.setHasFixedSize(true);
                            filmIndoAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) limitedTrending , fragmentManager);
                            filmIndoView.setAdapter(filmIndoAdapter);
                            filmIndoView.setNestedScrollingEnabled(false);
                            filmIndoTitle.setOnClickListener(new View.OnClickListener() {
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


}