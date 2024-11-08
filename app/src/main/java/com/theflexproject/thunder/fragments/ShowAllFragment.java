package com.theflexproject.thunder.fragments;

import android.annotation.SuppressLint;
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

    public ShowAllFragment() {
        // Required empty public constructor
    }

    // Membuat instance baru dari fragment dan mengirimkan daftar film melalui argument Bundle
    public static ShowAllFragment newInstance(List<MyMedia> movies) {
        ShowAllFragment fragment = new ShowAllFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOVIE_LIST, new ArrayList<>(movies)); // mengonversi List ke ArrayList untuk serialisasi
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout untuk fragment
        View view = inflater.inflate(R.layout.fragment_show_all, container, false);

        // Set listener sebelum memuat UI
        setOnClickListener();

        // Memanggil UI setup
        loadUI(view);

        return view;
    }

    private void loadUI(View view) {
        allMoviesRecyclerView = view.findViewById(R.id.allMoviesRecyclerView);

        if (getArguments() != null) {
            // Mengambil daftar film yang dikirim melalui Bundle
            movieList = (List<MyMedia>) getArguments().getSerializable(ARG_MOVIE_LIST);
        }

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
            allMoviesAdapter = new MediaAdapter(getContext(), movieList, listener);
            allMoviesRecyclerView.setAdapter(allMoviesAdapter);
            allMoviesAdapter.notifyDataSetChanged();
        });
    }

    private void setOnClickListener() {
        listener = (view, position) -> {
            MyMedia mediaItem = movieList.get(position);

            if (mediaItem instanceof Movie) {
                Movie movie = (Movie) mediaItem;
                MovieDetailsFragment movieDetailsFragment;

                // Memilih fragment berdasarkan ID film
                if (movie.getId() == 0) {
                    movieDetailsFragment = new MovieDetailsFragment(movie.getFileName());
                } else {
                    movieDetailsFragment = new MovieDetailsFragment(movie.getId());
                }

                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .add(R.id.container, movieDetailsFragment).addToBackStack(null).commit();
            } else if (mediaItem instanceof TVShow) {
                TVShow tvShow = (TVShow) mediaItem;
                TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(tvShow.getId());

                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .add(R.id.container, tvShowDetailsFragment).addToBackStack(null).commit();
            }
        };
    }
}
