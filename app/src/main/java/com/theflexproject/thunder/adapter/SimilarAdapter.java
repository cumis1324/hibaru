
package com.theflexproject.thunder.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SimilarAdapter extends RecyclerView.Adapter<SimilarAdapter.SimilarAdapterHolder> {

    Context context;
    List<MyMedia> mediaList;

    private SimilarAdapter.OnItemClickListener listener;

    public SimilarAdapter(Context context, List<MyMedia> mediaList, SimilarAdapter.OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
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
        holder.backdrop.post(() -> {
            int width = holder.backdrop.getWidth(); // Lebar FrameLayout
            int height = (int) (width / 16.0 * 9.0); // Hitung tinggi sesuai rasio 16:9
            ViewGroup.LayoutParams params = holder.backdrop.getLayoutParams();
            params.height = height;
            holder.backdrop.setLayoutParams(params);
        });
        if(mediaList.get(position) instanceof Movie) {
            Movie movie = ((Movie)mediaList.get(position));
            if(movie.getTitle()==null){
                holder.name.setText(movie.getFileName());
            } else {
                String year = movie.getRelease_date().substring(0,4);
                holder.name.setText(movie.getTitle() + " (" + year + ")");
                holder.desc.setText(movie.getOverview());
            }


            if(movie.getPoster_path()!=null){
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL+movie.getPoster_path())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL+movie.getBackdrop_path())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.backdrop);
            }
        }


        if(mediaList.get(position) instanceof Episode){
            Episode episode = ((Episode)mediaList.get(position));
            if(episode.getName()!=null){
                holder.name.setText( "Episode " + episode.getEpisode_number() + ": "+episode.getName());
                holder.desc.setText(episode.getOverview());
                holder.poster.setVisibility(View.GONE);
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL+episode.getStill_path())
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



        public SimilarAdapterHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.season_name);
            desc = itemView.findViewById(R.id.season_details);
            poster= itemView.findViewById(R.id.sPoster);
            backdrop = itemView.findViewById(R.id.moviepostersimilar);

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
}


