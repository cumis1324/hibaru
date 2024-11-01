package com.theflexproject.thunder.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.MediaItem;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.player.PlayerActivity;


import java.util.List;

public class DownloadRecyAdapter extends RecyclerView.Adapter<DownloadRecyAdapter.MediaViewHolder> {

    private List<MediaItem> mediaList;
    private Context context;
    private OnItemDeleteListener onItemDeleteListener;
    public interface OnItemDeleteListener{
        void onItemDelete(MediaItem mediaItem);
    }


    public DownloadRecyAdapter(List<MediaItem> mediaList, Context context, OnItemDeleteListener onItemDeleteListener) {
        this.mediaList = mediaList;
        this.context = context;
        this.onItemDeleteListener = onItemDeleteListener;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);
        return new MediaViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem mediaItem = mediaList.get(position);
        holder.fileNameTextView.setText(mediaItem.getFName());
        holder.hapus.setOnClickListener(view -> {
            if (onItemDeleteListener!=null){
                onItemDeleteListener.onItemDelete(mediaItem);
            }
        });
        holder.play.setOnClickListener(view -> {
            String offline = "offline";
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("url", mediaItem.getFilePath());
            intent.putExtra("tmdbId", offline);
            intent.putExtra("year", offline);
            intent.putExtra("title", mediaItem.getFName());
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(view -> {

        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView, path;
        Button play;
        Button hapus;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameInDownload);
            play = itemView.findViewById(R.id.playInDownload);
            hapus = itemView.findViewById(R.id.hapusDownload);
        }
    }
}
