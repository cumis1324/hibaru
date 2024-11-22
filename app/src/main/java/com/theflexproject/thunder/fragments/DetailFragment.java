package com.theflexproject.thunder.fragments;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.StringUtils;

public class DetailFragment extends BaseFragment{
    private Movie movieDetails;
    int id;
    String type;
    private MaterialTextView deskripsi, judul;
    private ShapeableImageView poster;
    private MaterialButton rating, donasi, watchlist, download, share;
    private RecyclerView moreItem;
    RelativeLayout frameDeskripsi;
    public DetailFragment(){
    }
    public DetailFragment (int id, String type){
        this.id = id;
        this.type = type;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout untuk fragment
        View view = inflater.inflate(R.layout.detail_item, container, false);
        initWidget(view);
        if ("movie".equals(type)) {
            loadMovieDetails(id);
        }
        return view;
    }

    @SuppressLint("SetTextI18n")
    private void loadMovieDetails(int id) {
        movieDetails = DetailsUtils.getMovieDetails(mActivity, id);
        if (movieDetails!=null) {
            String titleText = movieDetails.getTitle();
            String year = movieDetails.getRelease_date();
            String yearCrop = year.substring(0,year.indexOf('-'));
            String ratings = (int)(movieDetails.getVote_average()*10)+"%";
            String result = StringUtils.runtimeIntegerToString(movieDetails.getRuntime());
            rating.setText(ratings + " - " + result);
            judul.setText(titleText + " ("+yearCrop+")");
            deskripsi.setText(movieDetails.getOverview());
            Glide.with(mActivity)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + movieDetails.getPoster_path())
                    .apply(new RequestOptions()
                            .fitCenter()
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);
        }

    }

    private void initWidget(View view) {
        deskripsi = view.findViewById(R.id.deskJudul);
        judul = view.findViewById(R.id.judulNama);
        poster = view.findViewById(R.id.gambarPoster);
        donasi = view.findViewById(R.id.donasi);
        watchlist = view.findViewById(R.id.watchlist);
        download = view.findViewById(R.id.download);
        share = view.findViewById(R.id.share);
        moreItem = view.findViewById(R.id.moreItem);
        frameDeskripsi = view.findViewById(R.id.framedeskripsi);
        rating = view.findViewById(R.id.ratingsIkon);

    }
}
