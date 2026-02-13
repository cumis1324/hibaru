package com.theflexproject.thunder.adapter;

import static com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.theflexproject.thunder.Constants;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;

import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MoreMoviesAdapterr extends RecyclerView.Adapter<MoreMoviesAdapterr.MoreMoviesAdapterrHolder> {

    Context context;
    List<MyMedia> moreList;
    private MoreMoviesAdapterr.OnItemClickListener listener;

    public MoreMoviesAdapterr(Context context, List<MyMedia> moreList, OnItemClickListener listener) {
        this.context = context;
        this.moreList = moreList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MoreMoviesAdapterrHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.more_item, parent, false);
        return new MoreMoviesAdapterrHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoreMoviesAdapterrHolder holder, @SuppressLint("RecyclerView") int position) {
        if (moreList.get(position) instanceof Movie) {
            Movie movie = ((Movie) moreList.get(position));
            if (movie.getBackdropPath() != null) {
                holder.moreName.setText(movie.getTitle());
                Glide.with(context)
                        .load(TMDB_IMAGE_BASE_URL + movie.getBackdropPath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.moreposter);

                if (movie.getLogoPath() != null) {
                    holder.moreLogo.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(TMDB_IMAGE_BASE_URL + movie.getLogoPath())
                            .apply(new RequestOptions()
                                    .fitCenter()
                                    .override(Target.SIZE_ORIGINAL))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .into(holder.moreLogo);
                }
                if (movie.getLogoPath() == null) {
                    holder.moreLogo.setVisibility(View.GONE);
                }
                if (movie.getOverview() != null) {
                    holder.overview.setText(movie.getOverview());
                }
                if (movie.getPlayed() != null) {
                    holder.watched.setVisibility(View.VISIBLE);
                }
            }

        }
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        // Set your desired limit (e.g., 5)
        int limit = 30;

        // Return the minimum between the size of moreList and the limit
        return Math.min(moreList.size(), limit);
    }

    public class MoreMoviesAdapterrHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView moreposter;
        ImageView moreLogo;
        TextView moreName;
        TextView overview;
        TextView watched;

        public MoreMoviesAdapterrHolder(@NonNull View itemView) {
            super(itemView);

            moreposter = itemView.findViewById(R.id.moviepostermore);
            moreLogo = itemView.findViewById(R.id.movieLogomore);
            moreName = itemView.findViewById(R.id.moreMoviesname);
            overview = itemView.findViewById(R.id.moreoverview);
            watched = itemView.findViewById(R.id.markWatchedEpisode2);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAbsoluteAdapterPosition());
        }

    }

    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

    private void setAnimation(View itemView, int position) {
        Animation popIn = AnimationUtils.loadAnimation(context, R.anim.pop_in);
        itemView.startAnimation(popIn);
    }

}
