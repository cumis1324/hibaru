package com.theflexproject.thunder.fragments;


import static com.theflexproject.thunder.utils.ColapsingTitle.collapseTitle;
import static com.theflexproject.thunder.utils.ColapsingTitle.expandTitle;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.FetchMovie;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class SearchFragment extends BaseFragment {
    RecyclerView recyclerView;
    RecyclerView recyclerViewGenres;
    List<Integer> genreList;
    MediaAdapter mediaAdapter;

    List<Movie> movieList;
    List<TVShow> tvShowsList;
    List<MyMedia> matchesFound;
    TextInputEditText searchBox;
    private TextView homeTitle;
    private boolean isTitleVisible = true; // Flag untuk visibilitas title
    private Handler handler = new Handler(Looper.getMainLooper()); // Untuk debounce
    private Runnable scrollRunnable;

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchBox = view.findViewById(R.id.search_input);
        searchBox.requestFocus();
        recyclerView = mActivity.findViewById(R.id.recyclersearch);
        homeTitle = mActivity.findViewById(R.id.searchTitle);
        setOnClickListner();
        showSearchResults();


    }

    void showSearchResults() {

        try {
            searchBox.setOnEditorActionListener((v, actionId, event) -> {
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (!searchBox.getText().toString().isEmpty()) {
                        Thread thread = new Thread(() -> {
                            Log.i(" ", "in thread");
                            movieList = FetchMovie.getSearch(mActivity, searchBox.getText().toString());
                            tvShowsList = DatabaseClient
                                    .getInstance(mActivity)
                                    .getAppDatabase()
                                    .tvShowDao()
                                    .getSearchQuery(searchBox.getText().toString());

                            matchesFound = new ArrayList<>();
                            matchesFound.addAll(movieList);
                            matchesFound.addAll(tvShowsList);

                            mActivity.runOnUiThread(() -> {
                                DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                                float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
                                int noOfItems;
                                if (dpWidth < 600f) {
                                    noOfItems = 3;
                                } else if (dpWidth < 840f) {
                                    noOfItems = 6;
                                } else {
                                    noOfItems = 8;
                                }
                                Log.i(" ", matchesFound.toString());
                                recyclerView.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
                                recyclerView.setHasFixedSize(true);
                                FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
                                mediaAdapter = new MediaAdapter(mActivity, matchesFound, fragmentManager);
                                recyclerView.setAdapter(mediaAdapter);
                                mediaAdapter.notifyDataSetChanged();
                            });
                        });
                        thread.start();
                    }
                    return true; // Return true to indicate the event is handled
                }
                return false;
            });
        } catch (Exception e) {
            Log.i(e.toString(), "Exception");
        }
    }


    @OptIn(markerClass = UnstableApi.class)
    private void setOnClickListner() {
        recyclerView.setOnScrollChangeListener(new RecyclerView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
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

    }
}