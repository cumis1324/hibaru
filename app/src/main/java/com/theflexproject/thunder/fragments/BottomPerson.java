package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.CreditsAdapter;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Cast;
import com.theflexproject.thunder.model.CombinedCredits;
import com.theflexproject.thunder.model.Credits;
import com.theflexproject.thunder.model.Crew;
import com.theflexproject.thunder.model.Genre;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MovieCredit;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.PersonDetails;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TvCredit;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.Person;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BottomPerson extends BottomSheetDialogFragment {
    private int personId;
    private RecyclerView castView, crewView;
    private MediaAdapter mediaAdapter, crewAdapter;
    private Credits credits;
    private FragmentManager fragmentManager;
    private List<MyMedia> castList, crewList;
    private PersonDetails personDetails;
    private CombinedCredits combinedCredits;
    private List<Movie> movieList;
    private List<TVShow> tvShowList;
    private String movieId;
    private String tvId;
    private List<String> movieIds = new ArrayList<>();
    private List<String> tvIds = new ArrayList<>();
    public BottomPerson(){}
    public BottomPerson(int personId){
        this.personId=personId;
    }
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_person, container, false);


        TextView originalTitle = view.findViewById(R.id.originalTitle);
        TextView descriptionView = view.findViewById(R.id.bottom_sheet_description);
        TextView expandDescription = view.findViewById(R.id.expand_description);
        ImageView posterView = view.findViewById(R.id.posterInMediaItem);
        TextView genreSheet = view.findViewById(R.id.genreSheet);
        castView = view.findViewById(R.id.castRecycler);
        fragmentManager = getActivity().getSupportFragmentManager();
        NestedScrollView nestedScrollView = view.findViewById(R.id.nested_scroll_view);  // Ganti dengan ID yang sesuai
        if (nestedScrollView != null) {
            nestedScrollView.setNestedScrollingEnabled(true);
        }



        // Lakukan query database di thread terpisah
        new Thread(() -> {
            Person person = new Person();
            personDetails = person.getPersonDetails(personId);
            combinedCredits = person.getCombinedCredits(personId);
            for (MovieCredit movieCredit : combinedCredits.getMovieCredits()) {
                movieId = String.valueOf(movieCredit.getId());
                movieIds.addAll(Collections.singleton(movieId));
            }
            for (TvCredit tvCredit : combinedCredits.getTvCredits()) {
                tvId = String.valueOf(tvCredit.getId());
                tvIds.addAll(Collections.singleton(tvId));
            }
            movieList = DatabaseClient
                    .getInstance(getContext())
                    .getAppDatabase()
                    .movieDao()
                    .loadAllByIds(movieIds);
            tvShowList = DatabaseClient
                    .getInstance(getContext())
                    .getAppDatabase()
                    .tvShowDao()
                    .loadAllByIds(tvIds);
            castList = new ArrayList<>();
            castList.addAll(movieList);
            castList.addAll(tvShowList);
            getActivity().runOnUiThread(() -> {
                originalTitle.setText(personDetails.getName());
                genreSheet.setText(personDetails.getBirthday());
                descriptionView.setText(personDetails.getBiography());
                descriptionView.setMaxLines(2);
                descriptionView.setEllipsize(TextUtils.TruncateAt.END);
                expandDescription.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Cek apakah TextView sudah expanded atau tidak
                        if (descriptionView.getMaxLines() == 2) {
                            // Expand TextView
                            descriptionView.setMaxLines(Integer.MAX_VALUE);
                            expandDescription.setText("Collapse");
                        } else {
                            // Collapse TextView
                            descriptionView.setMaxLines(2);
                            expandDescription.setText("Expand");
                        }
                    }
                });
                Glide.with(requireActivity())
                        .load(TMDB_BACKDROP_IMAGE_BASE_URL + personDetails.getProfilePath())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(posterView);
                castView.setLayoutManager(new ScaleCenterItemLayoutManager(getContext() , RecyclerView.HORIZONTAL , false));
                castView.setHasFixedSize(true);
                mediaAdapter = new MediaAdapter(getContext(), castList , fragmentManager);
                castView.setAdapter(mediaAdapter);
                castView.setNestedScrollingEnabled(false);


            });


        }).start();

        return view;
    }
    @SuppressLint("ClickableViewAccessibility")
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
            // Mengatur agar hanya dapat scrolling dan tidak menjadi expanded
            // Mengatur agar BottomSheet dapat melakukan scroll tanpa menjadi expand
            bottomSheet.setOnTouchListener((v, event) -> {
                // Membiarkan NestedScrollView untuk melakukan scrolling

                return false;
            });

            // Optional: Mengatur tinggi Bottom Sheet ke full-screen
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            NestedScrollView nestedScrollView = bottomSheet.findViewById(R.id.nested_scroll_view);  // Ganti dengan ID yang sesuai
            if (nestedScrollView != null) {
                nestedScrollView.setNestedScrollingEnabled(true);
            }

        }
    }

}
