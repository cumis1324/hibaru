package com.theflexproject.thunder.adapter;

import static com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;

import java.util.List;

public class BannerRecyclerAdapter extends RecyclerView.Adapter<BannerRecyclerAdapter.MovieViewHolder> {

    Context context;
    List<Movie> mediaList;
    private OnItemClickListener listener;
    FirebaseManager manager;
    private DatabaseReference databaseReference;

    public BannerRecyclerAdapter(Context context, List<Movie> mediaList, OnItemClickListener listener) {
        this.context = context;
        this.mediaList = mediaList;
        this.listener = listener;
        this.manager = new FirebaseManager();
        databaseReference = FirebaseDatabase.getInstance().getReference("History");
        setHasStableIds(true); // Enable stable IDs
    }

    @Override
    public int getItemViewType(int position) {
        // Return a unique view type for each item position
        return position;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item_banner, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Movie movie = mediaList.get(position);

        holder.name.setText(movie.getTitle());

        if (movie.getBackdrop_path() != null) {
            Glide.with(context)
                    .load(TMDB_IMAGE_BASE_URL + movie.getBackdrop_path())
                    .placeholder(new ColorDrawable(Color.BLACK))
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                    .into(holder.poster);

            if (movie.getLogo_path() != null) {
                holder.logo.setVisibility(View.VISIBLE);
                holder.name.setVisibility(View.GONE);
                Glide.with(context)
                        .load(TMDB_IMAGE_BASE_URL + movie.getLogo_path())
                        .apply(new RequestOptions()
                                .fitCenter()
                                .override(Target.SIZE_ORIGINAL))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .into(holder.logo);
            } else {
                holder.name.setVisibility(View.VISIBLE);
            }
        }

        String tmdbId = String.valueOf(mediaList.get(position).getId());
        String userId = manager.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("History/");
        DatabaseReference p = databaseReference.child(userId).child(tmdbId).child("lastPosition");
        DatabaseReference lastP = databaseReference.child(userId).child(tmdbId).child("lastPlayed");

        lastP.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String lastPlayed = dataSnapshot.getValue(String.class);
                    if (lastPlayed != null) {
                        // Update the played field in your local database asynchronously
                        AsyncTask.execute(() -> {
                            DatabaseClient.getInstance(context).getAppDatabase().movieDao().updatePlayed(Integer.parseInt(tmdbId), lastPlayed+" added");
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
            }
        });

        p.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Long lastPosition = dataSnapshot.getValue(Long.class);
                    if (lastPosition != null) {
                        long runtime = (long) mediaList.get(position).getRuntime() * 60 * 1000;
                        double progress = (double) lastPosition / runtime;
                        int progressWidth = (int) (holder.poster.getWidth() * progress);
                        holder.progressOverlay.getLayoutParams().width = progressWidth;
                        holder.progressOverlay.requestLayout();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled event
            }
        });
    }

    @Override
    public int getItemCount() {
        return (mediaList == null) ? 0 : mediaList.size();
    }

    @Override
    public long getItemId(int position) {
        return position; // Return a unique ID for each item
    }

    public class MovieViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView name;
        ImageView poster;
        ImageView logo;
        View progressOverlay;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            progressOverlay = itemView.findViewById(R.id.progress_overlay2);
            logo = itemView.findViewById(R.id.movieLogo1);
            name = itemView.findViewById(R.id.textView4);
            poster = itemView.findViewById(R.id.moviePoster);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAbsoluteAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }
}
