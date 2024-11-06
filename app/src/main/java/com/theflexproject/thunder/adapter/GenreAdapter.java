package com.theflexproject.thunder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.Genres;

import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    private List<Genres> genreList;
    private OnGenreClickListener onGenreClickListener;
    private Context context;

    public GenreAdapter(Context context, List<Genres> genreList, OnGenreClickListener listener) {
        this.context = context;
        this.genreList = genreList;
        this.onGenreClickListener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genres genre = genreList.get(position);
        holder.genreName.setText(genre.getName());
        holder.itemView.setOnClickListener(v -> {
            if (onGenreClickListener != null) {
                onGenreClickListener.onGenreClick(genre.getId()); // Pass genre ID
            }
        });
    }

    @Override
    public int getItemCount() {
        return genreList.size();
    }

    public static class GenreViewHolder extends RecyclerView.ViewHolder {
        TextView genreName;

        public GenreViewHolder(View itemView) {
            super(itemView);
            genreName = itemView.findViewById(R.id.genre_name);
        }
    }
    public interface OnGenreClickListener {
        void onGenreClick(int genreId);
    }
}
