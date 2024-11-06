package com.theflexproject.thunder.fragments;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.utils.GenreUtils;

import java.util.List;


public class MovieLibraryFragment extends BaseFragment {

    RecyclerView recyclerViewMovies, recyclerGenre;
    GenreAdapter genreAdapter;
    List<Genres> genreList;
    MediaAdapter mediaAdapter;
    MediaAdapter.OnItemClickListener listenerMovie;
    List<Movie> movieList, genreMovies;

    public MovieLibraryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerGenre = view.findViewById(R.id.recyclerMovieGenre);
        recyclerGenre.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // Setup GenreAdapter with OnGenreClickListener
        genreAdapter = new GenreAdapter(mActivity, GenreUtils.getGenresList(), genreId -> loadMoviesByGenre(genreId));
        recyclerGenre.setAdapter(genreAdapter);

        showLibraryMovies(); // Load all movies initially
    }

    // Function to load movies based on genre
    private void loadMoviesByGenre(int genreId) {
        Thread thread = new Thread(() -> {
            String genreIdString = String.valueOf(genreId);
           genreMovies = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .movieDao()
                    .getMoviesByGenre(genreIdString); // Assuming getMoviesByGenre is defined in MovieDao
            movieList = genreMovies;
            showRecyclerMovies(movieList);
        });
        thread.start();
    }

    void showLibraryMovies() {
        setOnClickListner();
        Thread thread = new Thread(() -> {
            if (movieList == null) {
                movieList = DatabaseClient.getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .getAll();
            }
            showRecyclerMovies(movieList);
        });
        thread.start();
    }

    private void showRecyclerMovies(List<Movie> newMediaList) {
        mActivity.runOnUiThread(() -> {
            DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
            int noOfItems = (int) (dpWidth / 120);
            recyclerViewMovies = mActivity.findViewById(R.id.recyclerLibraryMovies);

            if (recyclerViewMovies != null) {
                recyclerViewMovies.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
                recyclerViewMovies.setHasFixedSize(true);
                mediaAdapter = new MediaAdapter(mActivity, (List<MyMedia>) (List<?>) newMediaList, listenerMovie);
                recyclerViewMovies.setAdapter(mediaAdapter);
                mediaAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setOnClickListner() {
        listenerMovie = (view, position) -> {
            MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(movieList.get(position).getId());
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .add(R.id.container, movieDetailsFragment).addToBackStack(null).commit();
        };
    }
}
