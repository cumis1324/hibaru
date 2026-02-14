package com.theflexproject.thunder.fragments.movie;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.fragments.BaseFragment;
import com.theflexproject.thunder.fragments.MovieDetailsFragment;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForYou extends BaseFragment {

    RecyclerView recyclerViewMovies;
    MediaAdapter mediaAdapter;
    MediaAdapter.OnItemClickListener listenerMovie;
    List<Movie> lastPlayedList, fav, played;
    List<MyMedia> forYou;

    public ForYou() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_for_you, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLibraryMovies();
    }

    void showLibraryMovies() {
        setOnClickListner();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(" ", "in thread");
                lastPlayedList = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getrecomendation(10, 0);

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

                forYou = new ArrayList<>();
                forYou.addAll(played);
                forYou.addAll(fav);
                forYou.addAll(lastPlayedList);

                if (forYou != null && forYou.size() > 0) {
                    mActivity.runOnUiThread(() -> {
                        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
                        int noOfItems;

                        noOfItems = (int) (dpWidth / 120);
                        Log.i(" ", forYou.toString());
                        recyclerViewMovies = mActivity.findViewById(R.id.recyclerForYou);
                        if (recyclerViewMovies != null) {
                            Collections.shuffle(forYou);
                            recyclerViewMovies.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
                            recyclerViewMovies.setHasFixedSize(true);
                            mediaAdapter = new MediaAdapter(getContext(), forYou,
                                    mActivity.getSupportFragmentManager());
                            recyclerViewMovies.setAdapter(mediaAdapter);
                            mediaAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showRecyclerMovies(List<MyMedia> forYou) {

    }

    private void setOnClickListner() {
        listenerMovie = (view, position) -> {
            Movie forYouId = ((Movie) forYou.get(position));
            MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(forYouId.getId());
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .add(R.id.container, movieDetailsFragment).addToBackStack(null).commit();
        };
    }
}