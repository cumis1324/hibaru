package com.theflexproject.thunder.fragments;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

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
    List<Movie> movieList, genreMovies;
    private int genreId = -1;
    private Spinner spinnerOrderBy, spinnerSortBy;

    public MovieLibraryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinnerSortBy = view.findViewById(R.id.sortBy);
        spinnerOrderBy = view.findViewById(R.id.orderBy);

        ArrayAdapter<CharSequence> orderByAdapter = ArrayAdapter.createFromResource(mActivity,
                R.array.order_by_options, android.R.layout.simple_spinner_item);
        orderByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderBy.setAdapter(orderByAdapter);

        ArrayAdapter<CharSequence> sortByAdapter = ArrayAdapter.createFromResource(mActivity,
                R.array.sort_by_options, android.R.layout.simple_spinner_item);
        sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortBy.setAdapter(sortByAdapter);
        spinnerOrderBy.setSelection(0); spinnerSortBy.setSelection(0);
        recyclerGenre = view.findViewById(R.id.recyclerMovieGenre);
        recyclerGenre.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // Setup GenreAdapter with OnGenreClickListener
        genreAdapter = new GenreAdapter(mActivity, GenreUtils.getGenresList(), selectedGenreId -> {
            genreId = selectedGenreId;
            loadMoviesByGenreAndSort(selectedGenreId, spinnerOrderBy.getSelectedItem().toString(), spinnerSortBy.getSelectedItem().toString());// Simpan genreId yang dipilih

        });
        recyclerGenre.setAdapter(genreAdapter);

        listenerSpinner();
    }

    private void listenerSpinner() {
        spinnerOrderBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String orderBy = parent.getItemAtPosition(position).toString();
                if (genreId == -1) {
                    loadSortedAll(orderBy, spinnerSortBy.getSelectedItem().toString());

                }

                // Muat ulang data berdasarkan genreId, orderBy, dan nilai sortBy dari spinner kedua
                loadMoviesByGenreAndSort(genreId, orderBy, spinnerSortBy.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Tidak ada aksi
            }
        });

        spinnerSortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortBy = parent.getItemAtPosition(position).toString();
                if (genreId == -1) {
                    loadSortedAll(spinnerOrderBy.getSelectedItem().toString(), sortBy);

                }

                // Ambil nilai SortBy dari spinner


                // Muat ulang data berdasarkan genreId, sortBy, dan nilai orderBy dari spinner pertama
                loadMoviesByGenreAndSort(genreId, spinnerOrderBy.getSelectedItem().toString(), sortBy);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Tidak ada aksi
            }
        });

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
            FragmentManager fragmentManager = mActivity.getSupportFragmentManager();

            if (recyclerViewMovies != null) {
                recyclerViewMovies.setLayoutManager(new GridLayoutManager(mActivity, noOfItems));
                recyclerViewMovies.setHasFixedSize(true);
                mediaAdapter = new MediaAdapter(mActivity, (List<MyMedia>) (List<?>) newMediaList, fragmentManager);
                recyclerViewMovies.setAdapter(mediaAdapter);
                mediaAdapter.notifyDataSetChanged();
            }
        });
    }
    private void loadMoviesByGenreAndSort(int genreId, String orderBy, String sortBy) {
        Thread thread = new Thread(() -> {
            String genreIdString = String.valueOf(genreId);

            // Tentukan urutan (ASC/DESC)
            String orderByClause = "ASC"; // Default
            if ("Descending".equals(orderBy)) {
                orderByClause = "DESC";
            }

            // Tentukan kolom pengurutan (title/release_date)
            String sortByColumn = "title"; // Default
            if ("Year".equals(sortBy)) {
                sortByColumn = "release_date";
            }
            if ("Rating".equals(sortBy)) {
                sortByColumn = "vote_count";
            }
            if ("Popularity".equals(sortBy)) {
                sortByColumn = "popularity";
            }

            // Eksekusi query dinamis
            String queryStr = "SELECT * FROM Movie WHERE genres LIKE '%' || ? || '%' AND disabled=0 " +
                    "GROUP BY id ORDER BY " + sortByColumn + " " + orderByClause;

            SupportSQLiteQuery query = new SimpleSQLiteQuery(queryStr, new Object[]{genreIdString});

            genreMovies = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .movieDao()
                    .getMoviesByGenreAndSort(query);

            // Tampilkan data di RecyclerView
            movieList = genreMovies;
            showRecyclerMovies(movieList);
        });
        thread.start();
    }
    private void loadSortedAll(String orderBy, String sortBy) {
        Thread thread = new Thread(() -> {

            // Tentukan urutan (ASC/DESC)
            String orderByClause = "ASC"; // Default
            if ("Descending".equals(orderBy)) {
                orderByClause = "DESC";
            }

            // Tentukan kolom pengurutan (title/release_date)
            String sortByColumn = "title"; // Default
            if ("Year".equals(sortBy)) {
                sortByColumn = "release_date";
            }
            if ("Rating".equals(sortBy)) {
                sortByColumn = "vote_count";
            }
            if ("Popularity".equals(sortBy)) {
                sortByColumn = "popularity";
            }

            // Eksekusi query dinamis
            String queryStr = "SELECT * FROM Movie WHERE title is not null AND disabled=0 " +
                    "GROUP BY id ORDER BY " + sortByColumn + " " + orderByClause;

            SupportSQLiteQuery query = new SimpleSQLiteQuery(queryStr);

            genreMovies = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .movieDao()
                    .getMoviesByGenreAndSort(query);

            // Tampilkan data di RecyclerView
            movieList = genreMovies;
            showRecyclerMovies(movieList);
        });
        thread.start();
    }

}
