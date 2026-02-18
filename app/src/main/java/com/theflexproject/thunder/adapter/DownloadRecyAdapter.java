package com.theflexproject.thunder.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.MediaItem;
import com.theflexproject.thunder.player.PlayerActivity;

import java.util.List;

public class DownloadRecyAdapter extends RecyclerView.Adapter<DownloadRecyAdapter.MediaViewHolder> {

    private List<MediaItem> mediaList;
    private Context context;
    private OnItemDeleteListener onItemDeleteListener;
    private boolean isTV;

    public interface OnItemDeleteListener {
        void onItemDelete(MediaItem mediaItem);
    }

    public DownloadRecyAdapter(List<MediaItem> mediaList, Context context, OnItemDeleteListener onItemDeleteListener,
            boolean isTV) {
        this.mediaList = mediaList;
        this.context = context;
        this.onItemDeleteListener = onItemDeleteListener;
        this.isTV = isTV;
    }

    // Constructor for backward compatibility if needed, defaults to false
    public DownloadRecyAdapter(List<MediaItem> mediaList, Context context, OnItemDeleteListener onItemDeleteListener) {
        this(mediaList, context, onItemDeleteListener, false);
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isTV ? R.layout.download_item_tv : R.layout.download_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MediaViewHolder(view, isTV);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem mediaItem = mediaList.get(position);
        holder.fileNameTextView.setText(mediaItem.getFName());

        if (isTV) {
            // TV: Click whole item to play, long click to delete
            holder.itemView.setOnClickListener(v -> playMedia(mediaItem));
            holder.itemView.setOnLongClickListener(v -> {
                if (onItemDeleteListener != null) {
                    onItemDeleteListener.onItemDelete(mediaItem);
                }
                return true;
            });
        } else {
            // Mobile: Click specific buttons
            if (holder.hapus != null) {
                holder.hapus.setOnClickListener(view -> {
                    if (onItemDeleteListener != null) {
                        onItemDeleteListener.onItemDelete(mediaItem);
                    }
                });
            }
            if (holder.play != null) {
                holder.play.setOnClickListener(view -> playMedia(mediaItem));
            }
        }
    }

    private void playMedia(MediaItem mediaItem) {
        String offline = "offline";
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("url", mediaItem.getFilePath());
        intent.putExtra("tmdbId", offline);
        intent.putExtra("year", offline);
        intent.putExtra("title", mediaItem.getFName());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        Button play; // Mobile only
        Button hapus; // Mobile only

        public MediaViewHolder(@NonNull View itemView, boolean isTV) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameInDownload);
            if (!isTV) {
                play = itemView.findViewById(R.id.playInDownload);
                hapus = itemView.findViewById(R.id.hapusDownload);
            }
        }
    }
}
