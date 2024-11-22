package com.theflexproject.thunder.fragments;


import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
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
    MediaAdapter.OnItemClickListener listener;

    TextInputEditText searchBox;
    Button search;
    ScrollView scrollview;

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
//        search = view.findViewById(R.id.search_button);
//        scrollview= view.findViewById(R.id.scrollview);
        recyclerView = mActivity.findViewById(R.id.recyclersearch);
//        scrollview.setVisibility(View.VISIBLE);
        showSearchResults();
//        search.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showSearchResults();
//            }
//        });



    }

    void showSearchResults() {
        setOnClickListner();
        try {
            searchBox.setOnEditorActionListener((v, actionId, event) -> {
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (!searchBox.getText().toString().isEmpty()) {
                        Thread thread = new Thread(() -> {
                            Log.i(" ", "in thread");
                            movieList = DatabaseClient
                                    .getInstance(mActivity)
                                    .getAppDatabase()
                                    .movieDao()
                                    .getSearchQuery(searchBox.getText().toString());
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
                                mediaAdapter = new MediaAdapter(mActivity, matchesFound, listener);
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
        listener = (view , position) -> {
            if(matchesFound.get(position) instanceof Movie) {
                MovieDetailsFragment movieDetailsFragment;
                PlayerFragment playerFragment = new PlayerFragment(((Movie) matchesFound.get(position)).getId(), "movie");
                if (((Movie) matchesFound.get(position)).getId() == 0) {
                    movieDetailsFragment = new MovieDetailsFragment(((Movie) matchesFound.get(position)).getFileName());

                } else {
                    playerFragment = new PlayerFragment(((Movie) matchesFound.get(position)).getId(), "movie");
                }

                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .add(R.id.container, playerFragment).addToBackStack(null).commit();
            }
            if(matchesFound.get(position) instanceof TVShow){
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(((TVShow)matchesFound.get(position)).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .add(R.id.container,tvShowDetailsFragment).addToBackStack(null).commit();
            }

        };
    }
}