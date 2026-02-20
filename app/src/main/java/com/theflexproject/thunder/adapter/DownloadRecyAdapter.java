package com.theflexproject.thunder.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;

import java.util.List;

public class DownloadRecyAdapter extends RecyclerView.Adapter<DownloadRecyAdapter.MediaViewHolder> {

    private List<MyMedia> mediaList;
    private Context context;
    private OnItemDeleteListener onItemDeleteListener;
    private boolean isTV;

    public interface OnItemDeleteListener {
        void onItemDelete(MyMedia mediaItem);
    }

    public DownloadRecyAdapter(List<MyMedia> mediaList, Context context, OnItemDeleteListener onItemDeleteListener,
            boolean isTV) {
        this.mediaList = mediaList;
        this.context = context;
        this.onItemDeleteListener = onItemDeleteListener;
        this.isTV = isTV;
    }

    public DownloadRecyAdapter(List<MyMedia> mediaList, Context context, OnItemDeleteListener onItemDeleteListener) {
        this(mediaList, context, onItemDeleteListener, false);
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isTV ? R.layout.download_item_tv : R.layout.media_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MediaViewHolder(view, isTV);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MyMedia mediaItem = mediaList.get(position);
        String title = "";
        String posterPath = "";
        String localPath = "";

        if (mediaItem instanceof Movie) {
            Movie movie = (Movie) mediaItem;
            title = movie.getTitle();
            posterPath = "https://image.tmdb.org/t/p/w500" + movie.getPosterPath();
            localPath = movie.getLocalPath();
        } else if (mediaItem instanceof Episode) {
            Episode episode = (Episode) mediaItem;
            title = episode.getName();
            posterPath = "https://image.tmdb.org/t/p/w500" + episode.getStillPath();
            localPath = episode.getLocalPath();
        }

        holder.fileNameTextView.setText(title);

        if (holder.ivThumbnail != null && !posterPath.isEmpty()) {
            Glide.with(context)
                    .load(posterPath)
                    .placeholder(R.drawable.ic_download)
                    .into(holder.ivThumbnail);
        } else if (isTV && holder.ivPosterTV != null && !posterPath.isEmpty()) {
            Glide.with(context)
                    .load(posterPath)
                    .placeholder(R.color.card_bg)
                    .into(holder.ivPosterTV);
        } else if (!isTV && holder.ivThumbnail != null && !posterPath.isEmpty()) {
            // Redundant check but keeps logic clear if ivThumbnail is reused
            Glide.with(context)
                    .load(posterPath)
                    .placeholder(R.drawable.ic_download)
                    .into(holder.ivThumbnail);
        }

        final String finalLocalPath = localPath;
        final int finalVideoId = (mediaItem instanceof Movie) ? ((Movie) mediaItem).getId()
                : ((Episode) mediaItem).getId();
        final boolean finalIsMovie = (mediaItem instanceof Movie);

        final int finalShowId = (mediaItem instanceof Episode) ? (int) ((Episode) mediaItem).getShowId() : -1;
        final int finalSeasonId = (mediaItem instanceof Episode) ? ((Episode) mediaItem).getSeasonId() : -1;

        if (isTV) {
            holder.itemView.setOnClickListener(
                    v -> playMedia(v, finalLocalPath, finalVideoId, finalIsMovie, finalShowId, finalSeasonId));
            holder.itemView.setOnLongClickListener(v -> {
                if (onItemDeleteListener != null) {
                    onItemDeleteListener.onItemDelete(mediaItem);
                }
                return true;
            });

            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                    if (holder.focusOverlay != null)
                        holder.focusOverlay.setVisibility(View.VISIBLE);
                    v.setZ(10f);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    if (holder.focusOverlay != null)
                        holder.focusOverlay.setVisibility(View.GONE);
                    v.setZ(0f);
                }
            });

            // Recycling fix: ensure visual state is correct if bound while focused
            if (holder.itemView.hasFocus()) {
                holder.itemView.setScaleX(1.1f);
                holder.itemView.setScaleY(1.1f);
                holder.itemView.setZ(10f);
                if (holder.focusOverlay != null)
                    holder.focusOverlay.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
                holder.itemView.setZ(0f);
                if (holder.focusOverlay != null)
                    holder.focusOverlay.setVisibility(View.GONE);
            }

        } else {
            // Mobile (Now using media_item layout which acts like a button)
            holder.itemView.setOnClickListener(
                    v -> playMedia(v, finalLocalPath, finalVideoId, finalIsMovie, finalShowId, finalSeasonId));

            holder.itemView.setOnLongClickListener(v -> {
                if (onItemDeleteListener != null) {
                    onItemDeleteListener.onItemDelete(mediaItem);
                }
                return true;
            });

            // Legacy button handling (if layout still used them, which it doesn't now)
            if (holder.hapus != null) {
                holder.hapus.setOnClickListener(view -> {
                    if (onItemDeleteListener != null) {
                        onItemDeleteListener.onItemDelete(mediaItem);
                    }
                });
            }
            if (holder.play != null) {
                holder.play.setOnClickListener(
                        v -> playMedia(v, finalLocalPath, finalVideoId, finalIsMovie, finalShowId, finalSeasonId));
            }
        }
    }

    private void playMedia(View view, String localPath, int videoId, boolean isMovie, int showId, int seasonId) {
        Bundle bundle = new Bundle();
        bundle.putString("localPath", localPath);
        bundle.putInt("videoId", videoId);
        bundle.putBoolean("isMovie", isMovie);
        if (!isMovie) {
            bundle.putInt("episodeId", videoId); // Explicitly pass as episodeId too
            bundle.putInt("showId", showId);
            bundle.putInt("seasonId", seasonId);
        }
        Navigation.findNavController(view).navigate(R.id.playerFragment, bundle);
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public void setMediaList(List<MyMedia> mediaList) {
        this.mediaList = mediaList;
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        ImageView ivThumbnail; // Mobile & TV (Now shared concept)
        ImageView ivPosterTV; // Specific for TV layout logic if needed
        View play;
        View hapus;
        View focusOverlay;

        public MediaViewHolder(@NonNull View itemView, boolean isTV) {
            super(itemView);
            // Common logic or split
            if (isTV) {
                fileNameTextView = itemView.findViewById(R.id.fileNameInDownload); // TV layout ID
                ivPosterTV = itemView.findViewById(R.id.posterInMediaItem);
                focusOverlay = itemView.findViewById(R.id.focusOverlay);
            } else {
                // Mobile - now using media_item.xml
                fileNameTextView = itemView.findViewById(R.id.nameInMediaItem); // Helper to find title
                ivThumbnail = itemView.findViewById(R.id.posterInMediaItem); // Reuse this field for the poster

                // Old IDs for safety if layout switch fails or old layout used
                if (fileNameTextView == null)
                    fileNameTextView = itemView.findViewById(R.id.fileNameInDownload);
                if (ivThumbnail == null)
                    ivThumbnail = itemView.findViewById(R.id.ivThumbnail);

                play = itemView.findViewById(R.id.playInDownload); // Will be null in media_item
                hapus = itemView.findViewById(R.id.hapusDownload); // Will be null in media_item
            }
        }
    }
}
