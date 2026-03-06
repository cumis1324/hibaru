package com.theflexproject.thunder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.utils.MovieQualityExtractor;

import java.util.List;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder> {

    private final List<MyMedia> sources;
    private final int activeIndex;
    private final OnSourceClickListener listener;

    public interface OnSourceClickListener {
        void onSourceClick(MyMedia source);
    }

    public SourceAdapter(List<MyMedia> sources, int activeIndex, OnSourceClickListener listener) {
        this.sources = sources;
        this.activeIndex = activeIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_source_professional, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyMedia source = sources.get(position);
        String fileName = "";
        if (source instanceof Movie) {
            fileName = ((Movie) source).getFileName();
        } else if (source instanceof Episode) {
            fileName = ((Episode) source).getFileName();
        }

        String quality = MovieQualityExtractor.extractQualtiy(fileName);
        holder.tvQuality.setText(quality);

        if (position == activeIndex) {
            holder.ivActive.setVisibility(View.VISIBLE);
            holder.tvQuality.setTextColor(
                    holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_light));
        } else {
            holder.ivActive.setVisibility(View.GONE);
            holder.tvQuality.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }

        holder.itemView.setOnClickListener(v -> listener.onSourceClick(source));
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuality;
        ImageView ivActive;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuality = itemView.findViewById(R.id.tvQuality);
            ivActive = itemView.findViewById(R.id.ivActive);
        }
    }
}
