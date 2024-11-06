package com.theflexproject.thunder.fragments;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.GenreAdapter;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Genres;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.GenreUtils;

import java.util.List;

public class TvShowsLibraryFragment extends BaseFragment {

    RecyclerView recyclerViewTVShows, recyclerGenre;
    MediaAdapter mediaAdapter;
    GenreAdapter genreAdapter;
    MediaAdapter.OnItemClickListener listenerTVShow;
    TextView textView;
    List<TVShow> tvShowList, genreTvShows;

    public TvShowsLibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tv_shows_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerGenre = view.findViewById(R.id.recyclerTVGenre);
        recyclerGenre.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // Set up the genre adapter and click listener
        genreAdapter = new GenreAdapter(mActivity, GenreUtils.getTvSeriesGenresList(), genreId -> loadTvShowsByGenre(genreId));
        recyclerGenre.setAdapter(genreAdapter);


        showLibraryTVShows();
    }

    void showLibraryTVShows() {
        setOnClickListener();
        Thread thread = new Thread(() -> {
            Log.i(" ", "in thread");

            if (tvShowList == null) {
                tvShowList = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao().getAllByTitles();
            }
            showRecyclerTVShows(tvShowList);
        });
        thread.start();
    }

    private void showRecyclerTVShows(List<TVShow> tvShowList) {
        mActivity.runOnUiThread(() -> {
            DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
            int noOfItems = (int) (dpWidth / 120);

            Log.i(" ", tvShowList.toString());
            recyclerViewTVShows = mActivity.findViewById(R.id.recyclerLibraryTVShows);
            if (recyclerViewTVShows != null) {
                recyclerViewTVShows.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
                recyclerViewTVShows.setHasFixedSize(true);
                mediaAdapter = new MediaAdapter(mActivity, (List<MyMedia>) (List<?>) tvShowList, listenerTVShow);
                recyclerViewTVShows.setAdapter(mediaAdapter);
                mediaAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setOnClickListener() {
        listenerTVShow = (view, position) -> {
            TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(tvShowList.get(position).getId());
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .add(R.id.container, tvShowDetailsFragment)
                    .addToBackStack(null)
                    .commit();
        };
    }

    private void loadTvShowsByGenre(int genreId) {
        Thread thread = new Thread(() -> {
            String genreIdStr = String.valueOf(genreId);

             genreTvShows = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .tvShowDao()
                    .getTvSeriesByGenreId(genreIdStr);

            tvShowList = genreTvShows;

            showRecyclerTVShows(tvShowList);
        });
        thread.start();
    }
}
