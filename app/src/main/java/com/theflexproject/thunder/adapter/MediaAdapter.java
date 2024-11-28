
package com.theflexproject.thunder.adapter;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.Constants;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.fragments.MovieDetailsFragment;
import com.theflexproject.thunder.fragments.PlayerFragment;
import com.theflexproject.thunder.fragments.TvShowDetailsFragment;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.Season;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.DetailsUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaAdapterHolder> {

    Context context;
    List<MyMedia> mediaList;
    private final FragmentManager fragmentManager;

    public MediaAdapter(Context context, List<MyMedia> mediaList, FragmentManager fragmentManager) {
        this.context = context;
        this.mediaList = mediaList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public MediaAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item, parent, false);
        return new MediaAdapterHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MediaAdapterHolder holder, int position) {
        if(mediaList.get(position) instanceof Movie) {
            Movie movie = ((Movie)mediaList.get(position));
           if(movie.getTitle()==null){
            holder.name.setText(movie.getFileName());
            } else {
               String year = movie.getRelease_date().substring(0,4);
               holder.name.setText(movie.getTitle() + " (" + year + ")");
           }

            if(movie.getVote_average()!=0){
                // holder.star.setVisibility(View.VISIBLE);
                holder.textStar.setVisibility(View.VISIBLE);
                DecimalFormat decimalFormat = new DecimalFormat("0.0");
                String roundedVoteAverage = decimalFormat.format(movie.getVote_average());
                holder.textStar.setText(roundedVoteAverage);
            }

            if(movie.getPoster_path()!=null){
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL+movie.getPoster_path())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
            }
            holder.itemView.setOnClickListener(v -> loadMovie(movie.getId()));
        }
        
        
        if(mediaList.get(position) instanceof TVShow){
            TVShow tvShow = ((TVShow)mediaList.get(position));
            if(tvShow.getName()!=null){
                String year = tvShow.getFirst_air_date().substring(0,4);
              holder.name.setText(tvShow.getName() + " (" + year + ")");
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL+tvShow.getPoster_path())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
            }
            if(tvShow.getVote_average()!=0){
                // holder.star.setVisibility(View.VISIBLE);
                holder.textStar.setVisibility(View.VISIBLE);
                DecimalFormat decimalFormat = new DecimalFormat("0.0");
                String roundedVoteAverage = decimalFormat.format(tvShow.getVote_average());
                holder.textStar.setText(roundedVoteAverage);
            }
            holder.itemView.setOnClickListener(v -> loadSeries(tvShow.getId()));
        }
        
        
        if(mediaList.get(position) instanceof TVShowSeasonDetails){
            TVShowSeasonDetails tvShowSeason = ((TVShowSeasonDetails)mediaList.get(position));
            if(tvShowSeason.getName()!=null){
                //holder.name.setVisibility(View.VISIBLE);
                holder.name.setText("Season "+tvShowSeason.getSeason_number());

                holder.season2.setVisibility(View.VISIBLE);
                holder.season2.setText(tvShowSeason.getName());
                String poster_path = tvShowSeason.getPoster_path();
                if(poster_path!=null){
                    Glide.with(context)
                            .load(Constants.TMDB_IMAGE_BASE_URL+poster_path)
                            .placeholder(new ColorDrawable(Color.BLACK))
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                            .into(holder.poster);
                }

            }
        }


        setAnimation(holder.itemView,position);

    }

    private void loadSeries(int id) {
        TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(id);
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment oldFragment = fragmentManager.findFragmentById(R.id.container);
        if (oldFragment != null) {
            transaction.hide(oldFragment);
        }
        transaction.add(R.id.container, tvShowDetailsFragment).addToBackStack(null);
        transaction.commit();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loadMovie(int id) {
        PlayerFragment movieDetailsFragment = new PlayerFragment(id, true);
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            Fragment oldFragment = fragmentManager.findFragmentById(R.id.container);
            if (oldFragment != null) {
                transaction.hide(oldFragment);
            }
            transaction.add(R.id.container, movieDetailsFragment).addToBackStack(null);
            transaction.commit();

    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public class MediaAdapterHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView season2;
        ImageView poster;
        TextView movieYear;
        ImageView star;
        TextView textStar;
        TextView watched;



        public MediaAdapterHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.nameInMediaItem);
            poster= itemView.findViewById(R.id.posterInMediaItem);
            movieYear = itemView.findViewById(R.id.yearInMediaItem);
            star = itemView.findViewById(R.id.starRate);
            textStar = itemView.findViewById(R.id.textStar);
            watched = itemView.findViewById(R.id.markWatchedMedia);
            season2 = itemView.findViewById(R.id.season2);

        }
    }

    
    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

    private void setAnimation(View itemView , int position){
        Animation popIn = AnimationUtils.loadAnimation(context,R.anim.pop_in);
        itemView.startAnimation(popIn);
    }

}


