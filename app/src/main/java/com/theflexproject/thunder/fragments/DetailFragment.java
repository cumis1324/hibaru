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
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MoreMoviesAdapterr;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.StringUtils;
import com.theflexproject.thunder.utils.tmdbTrending;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailFragment extends BaseFragment{
    private Movie movieDetails;
    private int id;
    private boolean isMovie;
    private MaterialTextView deskripsi, judul;
    private ShapeableImageView poster;
    private MaterialButton rating, donasi, watchlist, download, share;
    private RecyclerView moreItem;
    RelativeLayout frameDeskripsi;
    private List<Movie> similarMovie;
    MoreMoviesAdapterr.OnItemClickListener moreMoviesListener;
    public DetailFragment(){
    }
    public DetailFragment (int id, boolean isMovie){
        this.id = id;
        this.isMovie = isMovie;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout untuk fragment
        View view = inflater.inflate(R.layout.detail_item, container, false);
        initWidget(view);
        if (isMovie) {
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

    private void similarListener() {
        moreMoviesListener = new MoreMoviesAdapterr.OnItemClickListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onClick(View view, int position) {
                Movie more = (similarMovie.get(position));
                PlayerFragment playerFragment = new PlayerFragment(more.getId(), true);
                mActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.container,playerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        };
    }

    private void loadSimilar() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                similarMovie = DetailsUtils.getSimilarMovies(mActivity, id);
                if (similarMovie!=null){
                    mActivity.runOnUiThread(new Runnable() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void run() {
                            moreItem.setVisibility(View.VISIBLE);
                            ScaleCenterItemLayoutManager linearLayoutManager = new ScaleCenterItemLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                            moreItem.setLayoutManager(linearLayoutManager);
                            moreItem.setHasFixedSize(true);
                            MoreMoviesAdapterr moreMovieRecycler = new MoreMoviesAdapterr(mActivity, (List<MyMedia>) (List<?>) similarMovie, moreMoviesListener);
                            moreItem.setAdapter(moreMovieRecycler);
                            moreMovieRecycler.notifyDataSetChanged();

                        }
                    });
                }
            }});
        thread.start();
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
