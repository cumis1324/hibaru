package com.theflexproject.thunder.fragments;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;

import java.util.ArrayList;
import java.util.List;

public class ShowAllFragment extends BaseFragment {

    private static final String ARG_MOVIE_LIST = "movie_list";
    private List<MyMedia> movieList;
    private RecyclerView allMoviesRecyclerView;
    private MediaAdapter allMoviesAdapter;
    MediaAdapter.OnItemClickListener listener;
    BottomNavigationView botnav;

    public ShowAllFragment() {
        // Required empty public constructor
    }

    // Membuat instance baru dari fragment dan mengirimkan daftar film melalui argument Bundle
    public ShowAllFragment(List<MyMedia> movies) {
        this.movieList = movies;
    }


    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout untuk fragment
        View view = inflater.inflate(R.layout.fragment_show_all, container, false);
        botnav = mActivity.findViewById(R.id.bottom_navigation);
        botnav.setVisibility(View.GONE);

        // Memanggil UI setup
        loadUI(view);

        return view;
    }
    private boolean isTVDevice() {
        UiModeManager uiModeManager = (UiModeManager) mActivity.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isTVDevice()) {
            botnav.setVisibility(View.GONE);
        }else {
            botnav.setVisibility(View.VISIBLE);
        }
    }

    private void loadUI(View view) {
        allMoviesRecyclerView = view.findViewById(R.id.allMoviesRecyclerView);


        mActivity.runOnUiThread(() -> {
            DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
            int noOfItems;

            // Menentukan jumlah kolom berdasarkan lebar layar
            if (dpWidth < 600f) {
                noOfItems = 3;
            } else if (dpWidth < 840f) {
                noOfItems = 6;
            } else {
                noOfItems = 8;
            }

            allMoviesRecyclerView.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
            allMoviesRecyclerView.setHasFixedSize(true);
            allMoviesAdapter = new MediaAdapter(getContext(), movieList, mActivity.getSupportFragmentManager());
            allMoviesRecyclerView.setAdapter(allMoviesAdapter);
            allMoviesAdapter.notifyDataSetChanged();
        });
    }

}
