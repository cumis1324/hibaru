package com.theflexproject.thunder.adapter;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.player.PlayerActivity;
import com.theflexproject.thunder.utils.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class HistoryEpisodeAdapter extends RecyclerView.Adapter<HistoryEpisodeAdapter.HistoryEpisodeAdapterHolder> {

    Context context;
    List<Episode> episodeList;
    TVShow tvShow;
    private HistoryEpisodeAdapter.OnItemClickListener listener;
    FirebaseManager manager;
    private DatabaseReference databaseReference;

    public HistoryEpisodeAdapter(TVShow tvShow, Context context, List<Episode> episodeList,
            HistoryEpisodeAdapter.OnItemClickListener listener) {
        this.context = context;
        this.episodeList = episodeList;
        this.listener = listener;
        this.tvShow = tvShow;
        this.manager = new FirebaseManager();
        databaseReference = FirebaseDatabase.getInstance().getReference("History");
        setHasStableIds(true); // Enable stable IDs
    }

    @Override
    public int getItemViewType(int position) {
        // Return a unique view type for each item
        return position;
    }

    @NonNull
    @Override
    public HistoryEpisodeAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_episode_item, parent, false);
        return new HistoryEpisodeAdapterHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryEpisodeAdapterHolder holder,
            @SuppressLint("RecyclerView") int position) {
        if (episodeList != null) {
            Episode episode = episodeList.get(position);
            tvShow = DatabaseClient.getInstance(context.getApplicationContext()).getAppDatabase().tvShowDao()
                    .find(episode.getShowId());
            String seriesName = tvShow.getName();
            if (seriesName != null) {
                holder.judulSeries.setText(seriesName);
            }
            if (episode.getEpisodeNumber() > 9 && episode.getEpisodeNumber() > 999) {
                holder.episodeNumber.setText("E" + episode.getEpisodeNumber());
            } else {
                holder.episodeNumber.setText("E0" + episode.getEpisodeNumber());
            }
            if (episode.getSeasonNumber() > 9) {
                holder.seasonNumber.setText("S" + episode.getSeasonNumber());
            } else {
                holder.seasonNumber.setText("S0" + episode.getSeasonNumber());
            }
            if (episode.getName() != null) {
                holder.episodeName.setText(episode.getName());
            }
            if (episode.getStillPath() != null) {
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL + episode.getStillPath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.episodeStill);
            }
            if (episode.getOverview() != null) {
                holder.overview.setText(episode.getOverview());
            }
            if (episode.getRuntime() != 0) {
                String result = StringUtils.runtimeIntegerToString(episode.getRuntime());
                holder.runtime.setVisibility(View.VISIBLE);
                holder.runtime.setText(result);
            }

            holder.play.setOnClickListener(view -> holder.playEpisode(episode));

            String tmdbId = String.valueOf(episode.getId());
            String userId = manager.getCurrentUser().getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("History");
            DatabaseReference userReference = databaseReference.child(tmdbId + "/" + userId);
            DatabaseReference p = userReference.child("lastPosition");

            p.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Long lastPosition = dataSnapshot.getValue(Long.class);
                        if (lastPosition != null) {
                            long runtime = (long) episode.getRuntime() * 60 * 1000;
                            double progress = (double) lastPosition / runtime;

                            holder.episodeStill.getViewTreeObserver()
                                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            holder.episodeStill.getViewTreeObserver()
                                                    .removeOnGlobalLayoutListener(this);

                                            int progressWidth = (int) (holder.episodeStill.getWidth() * progress);
                                            holder.progressOverlay.getLayoutParams().width = progressWidth;
                                            holder.progressOverlay.requestLayout();
                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle onCancelled event
                }
            });
        }

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    public class HistoryEpisodeAdapterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        BlurView blurView;
        ViewGroup rootView;
        View decorView;

        TextView episodeName;
        ImageView episodeStill;
        TextView seasonNumber;
        TextView episodeNumber;
        TextView runtime;
        TextView overview;
        TextView judulSeries;
        Button play;
        TextView watched;
        View progressOverlay;

        public HistoryEpisodeAdapterHolder(@NonNull View itemView) {
            super(itemView);
            progressOverlay = itemView.findViewById(R.id.progress_overlay4);
            blurView = itemView.findViewById(R.id.blurView4);
            decorView = ((Activity) context).getWindow().getDecorView();
            rootView = decorView.findViewById(android.R.id.content);

            episodeName = itemView.findViewById(R.id.episodeNameInItem2);
            episodeStill = itemView.findViewById(R.id.episodeStill2);
            seasonNumber = itemView.findViewById(R.id.seasonNumberInItem2);
            episodeNumber = itemView.findViewById(R.id.episodeNumberInItem2);
            runtime = itemView.findViewById(R.id.RuntimeInItem2);
            overview = itemView.findViewById(R.id.overviewDescInItem2);
            watched = itemView.findViewById(R.id.markWatchedEpisode2);
            play = itemView.findViewById(R.id.playInEpisodeItem2);
            judulSeries = itemView.findViewById(R.id.judulSeries);

            blurBottom();

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAbsoluteAdapterPosition());
        }

        private void playEpisode(Episode episode) {
            SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("Settings",
                    Context.MODE_PRIVATE);
            boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING", false);

            if (savedEXT) {
                addToLastPlayed(episode.getId());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(episode.getUrlString()));
                intent.setDataAndType(Uri.parse(episode.getUrlString()), "video/*");
                itemView.getContext().startActivity(intent);
            } else {
                addToLastPlayed(episode.getId());
                Intent in = new Intent(itemView.getContext(), PlayerActivity.class);
                in.putExtra("url", episode.getUrlString());
                in.putExtra("season", String.valueOf(episode.getSeasonNumber()));
                in.putExtra("number", String.valueOf(episode.getEpisodeNumber()));
                in.putExtra("episode", episode.getName());
                in.putExtra("title", tvShow.getName());
                in.putExtra("tmdbId", String.valueOf(episode.getId()));
                itemView.getContext().startActivity(in);
                Toast.makeText(itemView.getContext(), "Play", Toast.LENGTH_LONG).show();
            }
        }

        private void addToLastPlayed(int id) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            String currentDateTime = ZonedDateTime.now(java.time.ZoneId.of("GMT+07:00")).format(formatter);
            Thread thread = new Thread(() -> DatabaseClient.getInstance(itemView.getContext()).getAppDatabase()
                    .episodeDao().updatePlayed(id, currentDateTime + " added"));
            thread.start();
        }

        void blurBottom() {
            ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            ((Activity) context).getWindow().setStatusBarColor(Color.TRANSPARENT);
            final float radius = 5f;
            final Drawable windowBackground = ((Activity) context).getWindow().getDecorView().getBackground();

            blurView.setupWith(rootView, new RenderScriptBlur(context))
                    .setFrameClearDrawable(windowBackground)
                    .setBlurRadius(radius);
            blurView.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            blurView.setClipToOutline(true);
        }
    }

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    private void setAnimation(View itemView, int position) {
        Animation popIn = AnimationUtils.loadAnimation(context, R.anim.pop_in);
        itemView.startAnimation(popIn);
    }
}
