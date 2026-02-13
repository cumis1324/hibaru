
package com.theflexproject.thunder.adapter;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
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
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.Season;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimilarAdapter extends RecyclerView.Adapter<SimilarAdapter.SimilarAdapterHolder> {

    Context context;
    List<MyMedia> mediaList;
    TVShow seasonDetails;

    private SimilarAdapter.OnItemClickListener listener;

    public SimilarAdapter(Context context, List<MyMedia> mediaList, SimilarAdapter.OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
        this.listener= listener;
    }
    public SimilarAdapter(Context context, List<MyMedia> mediaList, TVShow seasonDetails, SimilarAdapter.OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
        this.seasonDetails = seasonDetails;
        this.listener= listener;
    }

    @NonNull
    @Override
    public SimilarAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_similar_episode, parent, false);
        return new SimilarAdapterHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SimilarAdapterHolder holder, int position) {
        if (isTVDevice(context)) {
            // Tetapkan lebar dalam dp
            int widthInDp = 360;
            int heightInDp = 246;
            float density = context.getResources().getDisplayMetrics().density;
            int width = (int) (widthInDp * density); // Lebar dalam px
            int height = (int) (heightInDp * density);
            int heightB = (int) (width / 16.0 * 9.0);
            ViewGroup.LayoutParams params = holder.cardView.getLayoutParams();
            params.width = width;
            holder.cardView.setLayoutParams(params);
            ViewGroup.LayoutParams paramsB = holder.backdrop.getLayoutParams();
            paramsB.height = heightB; // Tinggi dalam piksel
            holder.backdrop.setLayoutParams(paramsB);
        }

        holder.backdrop.post(() -> {
            int width = holder.backdrop.getWidth(); // Lebar FrameLayout
            int height = (int) (width / 16.0 * 9.0); // Hitung tinggi sesuai rasio 16:9
            ViewGroup.LayoutParams params = holder.backdrop.getLayoutParams();
            params.height = height;
            holder.backdrop.setLayoutParams(params);
        });
        if(mediaList.get(position) instanceof Movie) {
            Movie movie = ((Movie) mediaList.get(position));

            // Ensure movie title is not null before using it
            if (movie.getTitle() != null) {
                holder.name.setText(movie.getTitle());
            } else if (movie.getFileName() != null) {
                holder.name.setText(movie.getFileName());
            } else {
                holder.name.setText("Unknown Title");
            }

            // Ensure other fields are also checked for null
            if (movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4) {
                String year = movie.getReleaseDate().substring(0, 4);
                holder.name.append(" (" + year + ")");
            }
            if (movie.getOverview() != null) {
                holder.desc.setText(movie.getOverview());
            } else {
                holder.desc.setText("No description available");
            }

            if (movie.getPosterPath() != null) {
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL + movie.getPosterPath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL + movie.getBackdropPath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.backdrop);
            }
        }

        if (mediaList.get(position) instanceof Episode) {
            Episode episode = ((Episode) mediaList.get(position));

            // Check if episode name is null before using it
            if (episode.getName() != null) {
                holder.name.setText("Episode " + episode.getEpisodeNumber() + ": " + episode.getName());
            } else {
                holder.name.setText("Episode " + episode.getEpisodeNumber() + ": Unknown");
            }

            // Handle description in a similar way
            if (episode.getOverview() != null) {
                holder.desc.setText(episode.getOverview());
            } else {
                holder.desc.setText("No description available");
            }

            // Handle still image path
            String stillPath = episode.getStillPath();
            if (stillPath == null || stillPath.isEmpty()) {
                String backdropUrl = Constants.TMDB_BACKDROP_IMAGE_BASE_URL + seasonDetails.getBackdropPath();
                if (seasonDetails.getBackdropPath() != null && !seasonDetails.getBackdropPath().isEmpty()) {
                    Glide.with(context)
                            .load(backdropUrl)
                            .placeholder(new ColorDrawable(Color.BLACK))
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                            .into(holder.backdrop);
                } else {
                    holder.backdrop.setVisibility(View.GONE);  // Hide if no backdrop available
                }
            } else {
                String imageUrl = Constants.TMDB_IMAGE_BASE_URL + stillPath;
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.backdrop);
            }
        }



        setAnimation(holder.itemView,position);

    }



    @Override
    public int getItemCount() {
        return mediaList.size();
    }



    public class SimilarAdapterHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView name, desc;
        ImageView backdrop, poster;
        CardView cardView;



        public SimilarAdapterHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.season_name);
            desc = itemView.findViewById(R.id.season_details);
            poster= itemView.findViewById(R.id.sPoster);
            backdrop = itemView.findViewById(R.id.moviepostersimilar);
            cardView = itemView.findViewById(R.id.tvEpisodeCard);

            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            listener.onClick(v,getAbsoluteAdapterPosition());
        }
    }


    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

    private void setAnimation(View itemView , int position){
        Animation popIn = AnimationUtils.loadAnimation(context,R.anim.pop_in);
        itemView.startAnimation(popIn);
    }
    private boolean isTVDevice(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null) {
            int modeType = uiModeManager.getCurrentModeType();
            return modeType == Configuration.UI_MODE_TYPE_TELEVISION;
        }
        return false;
    }
}



