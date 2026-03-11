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
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.theflexproject.thunder.data.sync.SyncPrefs;
import com.theflexproject.thunder.model.HistoryEntry;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.fragments.PlayerFragment;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;

import java.util.List;
import java.util.Objects;

public class BannerRecyclerAdapter extends RecyclerView.Adapter<BannerRecyclerAdapter.MovieViewHolder> {

    Context context;
    List<Movie> mediaList;
    FragmentManager fragmentManager;
    FirebaseManager manager;
    private DatabaseReference databaseReference;

    private SyncPrefs syncPrefs;
    private java.util.Map<String, HistoryEntry> historyCache = new java.util.HashMap<>();

    public BannerRecyclerAdapter(Context context, List<Movie> mediaList, FragmentManager fragmentManager,
            SyncPrefs syncPrefs) {
        this.context = context;
        this.mediaList = mediaList;
        this.fragmentManager = fragmentManager;
        this.manager = new FirebaseManager();
        this.syncPrefs = syncPrefs;
        databaseReference = FirebaseDatabase.getInstance().getReference("History");
        setHasStableIds(true); // Enable stable IDs
        updateHistoryCache();
    }

    private void updateHistoryCache() {
        String json = syncPrefs.getPlaybackHistoryJson();
        if (json != null && !json.isEmpty()) {
            try {
                Gson gson = new Gson();
                java.lang.reflect.Type type = new TypeToken<java.util.Map<String, HistoryEntry>>() {
                }.getType();
                historyCache = gson.fromJson(json, type);
            } catch (Exception e) {
                android.util.Log.e("BannerRecyclerAdapter", "Error parsing history JSON", e);
            }
        }
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Movie movie = mediaList.get(position);
        String year = movie.getReleaseDate().substring(0, 4);
        if (Objects.equals(movie.getOriginalLanguage(), "id")) {
            holder.name.setText(movie.getOriginalTitle() + " (" + year + ")");
        } else {
            holder.name.setText(movie.getTitle() + " (" + year + ")");
        }

        if (movie.getBackdropPath() != null) {
            Glide.with(context)
                    .load(TMDB_IMAGE_BASE_URL + movie.getBackdropPath())
                    .placeholder(new ColorDrawable(Color.BLACK))
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                    .into(holder.poster);

            if (movie.getLogoPath() != null) {
                holder.logo.setVisibility(View.VISIBLE);
                holder.name.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(TMDB_IMAGE_BASE_URL + movie.getLogoPath())
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
        holder.itemView.setOnClickListener(v -> {
            try {
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putInt("videoId", movie.getId());
                bundle.putBoolean("isMovie", true);
                Navigation.findNavController(v).navigate(R.id.playerFragment, bundle);
            } catch (Exception e) {
                android.util.Log.e("BannerRecyclerAdapter", "Navigation failed: " + e.getMessage());
            }
        });

        String tmdbId = String.valueOf(movie.getId());
        // Optimized Progress from Local Cache
        HistoryEntry entry = historyCache.get(tmdbId);
        if (entry != null && entry.lastPosition > 0) {
            long runtime = (long) movie.getRuntime() * 60 * 1000;
            if (runtime > 0) {
                double progress = (double) entry.lastPosition / runtime;

                // Use ViewTreeObserver to get width if not yet laid out
                if (holder.poster.getWidth() > 0) {
                    applyProgress(holder.progressOverlay, progress, holder.poster.getWidth());
                } else {
                    holder.poster.getViewTreeObserver()
                            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    holder.poster.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    applyProgress(holder.progressOverlay, progress, holder.poster.getWidth());
                                }
                            });
                }
            } else {
                holder.progressOverlay.setVisibility(View.GONE);
            }
        } else {
            holder.progressOverlay.setVisibility(View.GONE);
        }
    }

    private void applyProgress(View view, double progress, int fullWidth) {
        view.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = (int) Math.min(fullWidth, fullWidth * progress);
        view.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return (mediaList == null) ? 0 : mediaList.size();
    }

    @Override
    public long getItemId(int position) {
        return position; // Return a unique ID for each item
    }

    public class MovieViewHolder extends RecyclerView.ViewHolder {

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

        }

    }

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }
}
