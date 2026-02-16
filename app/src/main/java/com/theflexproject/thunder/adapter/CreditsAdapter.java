
package com.theflexproject.thunder.adapter;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
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
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
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
import com.theflexproject.thunder.fragments.BottomPerson;
import com.theflexproject.thunder.fragments.PlayerFragment;
import com.theflexproject.thunder.model.Cast;
import com.theflexproject.thunder.model.Crew;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.Season;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.player.PlayerUtils;
import com.theflexproject.thunder.utils.DetailsUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CreditsAdapter extends RecyclerView.Adapter<CreditsAdapter.CreditsAdapterHolder> {

    Context context;
    List<MyMedia> mediaList;
    private final FragmentManager fragmentManager;

    public CreditsAdapter(Context context, List<MyMedia> mediaList, FragmentManager fragmentManager) {
        this.context = context;
        this.mediaList = mediaList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public CreditsAdapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_credits, parent, false);
        return new CreditsAdapterHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CreditsAdapterHolder holder, int position) {
        if (mediaList.get(position) instanceof Cast) {
            Cast cast = ((Cast) mediaList.get(position));
            if (cast.getName() != null) {
                holder.name.setText(cast.getName() + " as " + cast.getCharacter());
            }

            if (cast.getProfilePath() != null) {
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL + cast.getProfilePath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
            }
            holder.itemView.setOnClickListener(v -> loadMovie(cast.getId()));
        }

        if (mediaList.get(position) instanceof Crew) {
            Crew crew = ((Crew) mediaList.get(position));
            if (crew.getName() != null) {
                holder.name.setText(crew.getName() + " as "
                        + (crew.getJob() != null && !crew.getJob().isEmpty() ? crew.getJob() : crew.getDepartment()));
                Glide.with(context)
                        .load(Constants.TMDB_IMAGE_BASE_URL + crew.getProfilePath())
                        .placeholder(new ColorDrawable(Color.BLACK))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(14)))
                        .into(holder.poster);
            }
            holder.itemView.setOnClickListener(v -> loadSeries(crew.getId()));
        }

        setAnimation(holder.itemView, position);

    }

    private void loadSeries(int id) {
        BottomPerson movieDetailsFragment = new BottomPerson(id);
        movieDetailsFragment.show(fragmentManager, "bottomSheet");
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loadMovie(int id) {
        BottomPerson movieDetailsFragment = new BottomPerson(id);
        movieDetailsFragment.show(fragmentManager, "bottomSheet");
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public class CreditsAdapterHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView season2;
        ImageView poster;
        TextView movieYear;
        ImageView star;
        TextView textStar;
        TextView watched;

        public CreditsAdapterHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.nameInMediaItem);
            poster = itemView.findViewById(R.id.posterInMediaItem);
            movieYear = itemView.findViewById(R.id.yearInMediaItem);
            star = itemView.findViewById(R.id.starRate);
            textStar = itemView.findViewById(R.id.textStar);
            watched = itemView.findViewById(R.id.markWatchedMedia);
            season2 = itemView.findViewById(R.id.season2);

        }
    }

    public interface OnItemClickListener {
        public void onClick(View view, int position);
    }

    private void setAnimation(View itemView, int position) {
        Animation popIn = AnimationUtils.loadAnimation(context, R.anim.pop_in);
        itemView.startAnimation(popIn);
    }

}
