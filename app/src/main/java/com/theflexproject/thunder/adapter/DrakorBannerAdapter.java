package com.theflexproject.thunder.adapter;

import static com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;

import java.util.List;

public class DrakorBannerAdapter extends RecyclerView.Adapter<DrakorBannerAdapter.MovieViewHolder> {

    Context context;
    List<TVShow> mediaList;
    private DrakorBannerAdapter.OnItemClickListener listener;
    public DrakorBannerAdapter(Context context, List<TVShow> mediaList, DrakorBannerAdapter.OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item_banner, parent, false);
        return new MovieViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        if(mediaList.get(position).getBackdropPath()!=null){
            String year = mediaList.get(position).getFirstAirDate().substring(0,4);
            holder.name.setText(mediaList.get(position).getName() + " (" + year + ")");
            Glide.with(context)
                    .load(TMDB_IMAGE_BASE_URL +mediaList.get(position).getBackdropPath())
                    .placeholder(new ColorDrawable(Color.BLACK))
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                    .into(holder.poster);

            if(mediaList.get(position).getLogoPath()!=null){
                holder.logo.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(TMDB_IMAGE_BASE_URL +mediaList.get(position).getLogoPath())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .into(holder.logo);
            }
        }

    }

    @Override
    public int getItemCount() {
        return (mediaList==null)?0:mediaList.size();
    }

    public class MovieViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView name;
        ImageView poster;
        ImageView logo;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.movieLogo1);
            name = itemView.findViewById(R.id.textView4);
            poster= itemView.findViewById(R.id.moviePoster);
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

}

