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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.GenreUtils;

import java.util.List;

public class TvShowsLibraryFragment extends BaseFragment {

    RecyclerView recyclerViewTVShows, recyclerGenre;
    MediaAdapter mediaAdapter;
    GenreAdapter genreAdapter;
    TextView textView;
    List<TVShow> tvShowList, genreTvShows;
    private String selectedOrderBy = "ASC"; // Default order
    private String selectedSortBy = "title"; // Default column
    private int genreId = -1;
    private Spinner spinnerOrderBy, spinnerSortBy;


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
        spinnerOrderBy = view.findViewById(R.id.orderBy);
        ArrayAdapter<CharSequence> orderByAdapter = ArrayAdapter.createFromResource(mActivity,
                R.array.order_by_options, android.R.layout.simple_spinner_item);
        orderByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderBy.setAdapter(orderByAdapter);
        spinnerOrderBy.setSelection(0);
        spinnerSortBy = view.findViewById(R.id.sortBy);
        ArrayAdapter<CharSequence> sortByAdapter = ArrayAdapter.createFromResource(mActivity,
                R.array.sort_by_options, android.R.layout.simple_spinner_item);
        sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortBy.setAdapter(sortByAdapter);
        spinnerSortBy.setSelection(0);
        recyclerGenre = view.findViewById(R.id.recyclerTVGenre);
        recyclerGenre.setLayoutManager(new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false));

        // Set up the genre adapter and click listener
        genreAdapter = new GenreAdapter(mActivity, GenreUtils.getTvSeriesGenresList(), selectedGenreId -> {
            genreId = selectedGenreId; // Simpan genreId yang dipilih
            loadTvShowsByGenreAndSort(selectedGenreId, spinnerOrderBy.getSelectedItem().toString(), spinnerSortBy.getSelectedItem().toString());
        });
        recyclerGenre.setAdapter(genreAdapter);
        // Indeks 0 berarti item pertama yang dipilih

        // Setup Spinner Sort By
         // Indeks 0 berarti item pertama yang dipilih
        // Load all movies initially
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
                loadTvShowsByGenreAndSort(genreId, orderBy, spinnerSortBy.getSelectedItem().toString());
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
                loadTvShowsByGenreAndSort(genreId, spinnerOrderBy.getSelectedItem().toString(), sortBy);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Tidak ada aksi
            }
        });

    }

    private void loadSortedAll(String orderBy, String sortBy) {
        Thread thread = new Thread(() -> {
            String orderByClause = "ASC"; // Default
            if ("Descending".equals(orderBy)) {
                orderByClause = "DESC";
            }

            // Tentukan kolom pengurutan (title/release_date)
            String sortByColumn = "name"; // Default
            if ("Year".equals(sortBy)) {
                sortByColumn = "last_air_date";
            }
            if ("Rating".equals(sortBy)) {
                sortByColumn = "vote_count";
            }
            if ("Popularity".equals(sortBy)) {
                sortByColumn = "popularity";
            }

            String queryStr = "SELECT * FROM TVShow WHERE poster_path IS NOT NULL " +
                    "GROUP BY id ORDER BY " + sortByColumn + " " + orderByClause;

            SupportSQLiteQuery query = new SimpleSQLiteQuery(queryStr);

            genreTvShows = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .tvShowDao()
                    .getTvSeriesByGenreAndSort(query);

            tvShowList = genreTvShows;
            showRecyclerTVShows(tvShowList);
        });
        thread.start();
    }

    private void loadTvShowsByGenreAndSort(int genreId, String orderBy, String sortBy) {
        Thread thread = new Thread(() -> {
            String genreIdStr = String.valueOf(genreId);
            String orderByClause = "ASC"; // Default
            if ("Descending".equals(orderBy)) {
                orderByClause = "DESC";
            }

            // Tentukan kolom pengurutan (title/release_date)
            String sortByColumn = "name"; // Default
            if ("Year".equals(sortBy)) {
                sortByColumn = "last_air_date";
            }
            if ("Rating".equals(sortBy)) {
                sortByColumn = "vote_count";
            }
            if ("Popularity".equals(sortBy)) {
                sortByColumn = "popularity";
            }

            String queryStr = "SELECT * FROM TVShow WHERE genres LIKE '%' || ? || '%' " +
                    "GROUP BY id ORDER BY " + sortByColumn + " " + orderByClause;

            SupportSQLiteQuery query = new SimpleSQLiteQuery(queryStr, new Object[]{genreIdStr});

            genreTvShows = DatabaseClient.getInstance(mActivity)
                    .getAppDatabase()
                    .tvShowDao()
                    .getTvSeriesByGenreAndSort(query);

            tvShowList = genreTvShows;
            showRecyclerTVShows(tvShowList);
        });
        thread.start();
    }


    void showLibraryTVShows() {

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
                mediaAdapter = new MediaAdapter(mActivity, (List<MyMedia>) (List<?>) tvShowList, mActivity.getSupportFragmentManager());
                recyclerViewTVShows.setAdapter(mediaAdapter);
                mediaAdapter.notifyDataSetChanged();
            }
        });
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
