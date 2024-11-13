package com.theflexproject.thunder.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Genre;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.StringUtils;

import java.util.ArrayList;

public class VideoDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TYPE = "type";
    private Movie movieDetails;
    private TVShow tvShowDetails;

    // Enum atau Constant untuk menentukan tipe
    public static final String TYPE_MOVIE = "movie";
    public static final String TYPE_TVSHOW = "tvshow";

    public static VideoDetailsBottomSheet newInstance(int id, String type) {
        VideoDetailsBottomSheet fragment = new VideoDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, id);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        TextView titleView = view.findViewById(R.id.bottom_sheet_title);
        TextView descriptionView = view.findViewById(R.id.bottom_sheet_description);
        MaterialButton rating = view.findViewById(R.id.ratingsSheet);
        TextView genreSheet = view.findViewById(R.id.genreSheet);

        if (getArguments() != null) {
            int id = getArguments().getInt(ARG_ID);
            String type = getArguments().getString(ARG_TYPE);

            // Lakukan query database di thread terpisah
            new Thread(() -> {
                if (TYPE_MOVIE.equals(type)) {
                    movieDetails = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .movieDao()
                            .byId(id);

                    if (movieDetails != null) {
                        getActivity().runOnUiThread(() -> {
                            String year = movieDetails.getRelease_date();
                            String yearCrop = year.substring(0, year.indexOf('-'));
                            titleView.setText(movieDetails.getTitle() + " (" + yearCrop + ") ");
                            descriptionView.setText(movieDetails.getOverview());
                            String result = StringUtils.runtimeIntegerToString(movieDetails.getRuntime());
                            String ratings = String.valueOf((int) (movieDetails.getVote_average() * 10));
                            rating.setText(ratings + " - " + result);
                            ArrayList<Genre> genres = movieDetails.getGenres();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < genres.size(); i++) {
                                Genre genre = genres.get(i);
                                if (i == genres.size() - 1 && genre != null) {
                                    sb.append(genre.getName());
                                } else if (genre != null) {
                                    sb.append(genre.getName()).append(", ");
                                }
                            }
                            genreSheet.setText(sb.toString());

                        });
                    }
                }
                else if (TYPE_TVSHOW.equals(type)) {
                    tvShowDetails = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .tvShowDao()
                            .find(id);

                    if (tvShowDetails != null) {
                        getActivity().runOnUiThread(() -> {
                            String year = tvShowDetails.getFirst_air_date();
                            String yearCrop = year.substring(0, year.indexOf('-'));
                            titleView.setText(tvShowDetails.getName() + " (" + yearCrop + ") ");
                            descriptionView.setText(tvShowDetails.getOverview());
                            String result = String.valueOf(tvShowDetails.getNumber_of_seasons());
                            String ratings = String.valueOf((int) (tvShowDetails.getVote_average() * 10));
                            rating.setText(ratings + " - " + result + " Season");
                            ArrayList<Genre> genres = tvShowDetails.getGenres();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < genres.size(); i++) {
                                Genre genre = genres.get(i);
                                if (i == genres.size() - 1 && genre != null) {
                                    sb.append(genre.getName());
                                } else if (genre != null) {
                                    sb.append(genre.getName()).append(", ");
                                }
                            }
                            genreSheet.setText(sb.toString());
                        });
                    }
                }
            }).start();

        }

        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            // Mengambil BottomSheet untuk mengatur state-nya
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            // Mengatur BottomSheet ke state expanded secara otomatis

            // Optional: Mengatur tinggi Bottom Sheet ke full-screen
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

        }
    }
}

