package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.language;

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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.BannerRecyclerAdapter;
import com.theflexproject.thunder.adapter.CreditsAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Cast;
import com.theflexproject.thunder.model.Credits;
import com.theflexproject.thunder.model.Crew;
import com.theflexproject.thunder.model.Genre;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.Translate;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.util.ArrayList;
import java.util.List;

public class VideoDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TYPE = "type";
    private Movie movieDetails;
    private TVShow tvShowDetails;
    private TVShow tvShow;
    private int movieId;
    private RecyclerView castView, crewView;
    private CreditsAdapter castAdapter, crewAdapter;
    private Credits credits;
    private FragmentManager fragmentManager;
    private List<MyMedia> castList, crewList;

    private boolean isMovie;
    private TVShowSeasonDetails season; private int episodeId;

    public VideoDetailsBottomSheet (int id, boolean isMovie) {
        this.isMovie = true;
        this.movieId = id;
    }
    public  VideoDetailsBottomSheet(){

    }
    public  VideoDetailsBottomSheet(TVShow tvShowDetails){
        this.isMovie = false;
        this.tvShow = tvShowDetails;
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_layout, container, false);

        TextView titleView = view.findViewById(R.id.bottom_sheet_title);
        TextView originalTitle = view.findViewById(R.id.originalTitle);
        TextView descriptionView = view.findViewById(R.id.bottom_sheet_description);
        MaterialButton rating = view.findViewById(R.id.ratingsSheet);
        TextView genreSheet = view.findViewById(R.id.genreSheet);
        castView = view.findViewById(R.id.castRecycler);
        crewView = view.findViewById(R.id.crewRecycler);
        fragmentManager = getActivity().getSupportFragmentManager();


            // Lakukan query database di thread terpisah
            new Thread(() -> {
                if (isMovie) {
                    movieDetails = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .movieDao()
                            .byId(movieId);
                    tmdbTrending credit = new tmdbTrending();
                    credits = credit.getMovieCredits(movieId);
                    List<Cast> cast = credits.getCastList();
                    List<Crew> crew = credits.getCrewList();
                    castList = new ArrayList<>();
                    castList.addAll(cast);
                    crewList = new ArrayList<>();
                    crewList.addAll(crew);
                    if (movieDetails != null) {
                        getActivity().runOnUiThread(() -> {
                            String year = movieDetails.getReleaseDate();
                            String yearCrop = year.substring(0, year.indexOf('-'));
                            titleView.setText(movieDetails.getTitle() + " (" + yearCrop + ") ");
                            String description = movieDetails.getOverview();
                            descriptionView.setText(description);
                            String ratings = String.valueOf((int) (movieDetails.getVoteAverage() * 10));
                            String result = StringUtils.runtimeIntegerToString(movieDetails.getVoteCount());
                            rating.setText(ratings + " from " + result + " Votes");
                            originalTitle.setText(movieDetails.getOriginalTitle());
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
                            castView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            castView.setHasFixedSize(true);
                            crewView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            crewView.setHasFixedSize(true);
                            castAdapter = new CreditsAdapter(getContext(), castList , fragmentManager);
                            crewAdapter = new CreditsAdapter(getContext(), crewList , fragmentManager);
                            castView.setAdapter(castAdapter);
                            castView.setNestedScrollingEnabled(false);
                            crewView.setAdapter(crewAdapter);
                            crewView.setNestedScrollingEnabled(false);

                        });
                    }
                }
                else  {
                    tvShowDetails = DatabaseClient
                            .getInstance(getContext())
                            .getAppDatabase()
                            .tvShowDao()
                            .find(tvShow.getId());
                    tmdbTrending credit = new tmdbTrending();
                    credits = credit.getTvCredits(tvShowDetails.getId());
                    List<Cast> cast = credits.getCastList();
                    List<Crew> crew = credits.getCrewList();
                    castList = new ArrayList<>();
                    castList.addAll(cast);
                    crewList = new ArrayList<>();
                    crewList.addAll(crew);

                    if (tvShowDetails != null) {
                        getActivity().runOnUiThread(() -> {
                            String year = tvShowDetails.getFirstAirDate();
                            String yearCrop = year.substring(0, year.indexOf('-'));
                            titleView.setText(tvShowDetails.getName() + " (" + yearCrop + ") ");
                            descriptionView.setText(tvShowDetails.getOverview());
                            originalTitle.setText(tvShowDetails.getOriginalName());
                            String ratings = String.valueOf((int) (tvShowDetails.getVoteAverage() * 10));
                            String result = StringUtils.runtimeIntegerToString(tvShowDetails.getVoteCount());
                            rating.setText(ratings + " from " + result + " Votes");
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
                            castView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            castView.setHasFixedSize(true);
                            crewView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                            crewView.setHasFixedSize(true);
                            castAdapter = new CreditsAdapter(getContext(), castList , fragmentManager);
                            crewAdapter = new CreditsAdapter(getContext(), crewList , fragmentManager);
                            castView.setAdapter(castAdapter);
                            castView.setNestedScrollingEnabled(false);
                            crewView.setAdapter(crewAdapter);
                            crewView.setNestedScrollingEnabled(false);
                        });
                    }
                }
            }).start();

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
            if (PlayerUtils.isTVDevice(requireActivity())){
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }else {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            // Optional: Mengatur tinggi Bottom Sheet ke full-screen
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

        }
    }
}


