package com.theflexproject.thunder.adapter;

import static com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.fragments.PlayerFragment;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;
import com.theflexproject.thunder.utils.DetailsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SEASON = 0;
    private static final int VIEW_TYPE_EPISODE = 1;
    private final Context context;
    private final List<Object> dataList;
    private final TVShow tvShow;
    private final FragmentManager fragmentManager;
    private int expandedPosition = -1; // -1 berarti tidak ada yang diperluas
    private final Map<Integer, List<Episode>> episodeMap = new HashMap<>();



    public ExpandableAdapter(Context context, TVShow tvShow, List<TVShowSeasonDetails> seasons, FragmentManager fragmentManager) {
        this.context = context;
        this.dataList = new ArrayList<>(seasons);
        this.tvShow = tvShow;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public int getItemViewType(int position) {
        return (dataList.get(position) instanceof TVShowSeasonDetails) ? VIEW_TYPE_SEASON : VIEW_TYPE_EPISODE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SEASON) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seasons, parent, false);
            return new SeasonViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
            return new EpisodeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SeasonViewHolder) {
            TVShowSeasonDetails season = (TVShowSeasonDetails) dataList.get(position);
            ((SeasonViewHolder) holder).bind(season, position);
        } else if (holder instanceof EpisodeViewHolder) {
            Episode episode = (Episode) dataList.get(position);
            TVShowSeasonDetails relatedSeason = findRelatedSeason(position);
            ((EpisodeViewHolder) holder).bind(relatedSeason, episode);
        }
    }

    private TVShowSeasonDetails findRelatedSeason(int position) {
        for (int i = position; i >= 0; i--) {
            if (dataList.get(i) instanceof TVShowSeasonDetails) {
                return (TVShowSeasonDetails) dataList.get(i);
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class SeasonViewHolder extends RecyclerView.ViewHolder {
        private final TextView seasonName;
        private final TextView seasonDetails;
        private final ImageView poster;
        private final CardView cardView;
        private boolean isExpanded = false;

        public SeasonViewHolder(View itemView) {
            super(itemView);
            seasonName = itemView.findViewById(R.id.season_name);
            poster = itemView.findViewById(R.id.sPoster);
            seasonDetails = itemView.findViewById(R.id.season_details);
            cardView = itemView.findViewById(R.id.season_card);
        }

        @SuppressLint("SetTextI18n")
        void bind(TVShowSeasonDetails season, int position) {
            seasonName.setText("Season " + season.getSeason_number() + ": " + season.getName());
            seasonDetails.setText(season.getOverview());
            Glide.with(context)
                    .load(TMDB_BACKDROP_IMAGE_BASE_URL + season.getPoster_path())
                    .apply(new RequestOptions().fitCenter().override(Target.SIZE_ORIGINAL))
                    .placeholder(new ColorDrawable(Color.TRANSPARENT))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(poster);

            cardView.setOnClickListener(v -> {
                if (expandedPosition == position) {
                    // Jika item yang sama diklik, maka collapse
                    collapseEpisodes(position);
                    expandedPosition = -1;
                } else {
                    // Collapse item yang sebelumnya diperluas (jika ada)
                    if (expandedPosition >= 0) {
                        collapseEpisodes(expandedPosition);
                    }

                    // Expand item yang baru diklik
                    expandEpisodes(position, season);
                    expandedPosition = position;
                }
            });
        }

        private void collapseEpisodes(int position) {
            List<Episode> episodes = episodeMap.get(position);
            if (episodes != null) {
                dataList.removeAll(episodes);
                notifyItemRangeRemoved(position + 1, episodes.size());
                episodeMap.remove(position); // Hapus dari map setelah collapse
            }
            isExpanded = false;
        }


        private void expandEpisodes(int position, TVShowSeasonDetails season) {
            if (!episodeMap.containsKey(position)) {
                new FetchEpisodesTask(season, position).execute();
            } else {
                List<Episode> episodes = episodeMap.get(position);
                dataList.addAll(position + 1, episodes);
                notifyItemRangeInserted(position + 1, episodes.size());
            }
            isExpanded = true;
        }

        private class FetchEpisodesTask extends AsyncTask<Void, Void, List<Episode>> {
            private final int position;
            private final TVShowSeasonDetails season;

            FetchEpisodesTask(TVShowSeasonDetails season, int position) {
                this.season = season;
                this.position = position;
            }

            @Override
            protected List<Episode> doInBackground(Void... voids) {
                // Panggil API untuk mendapatkan daftar episode
                return DetailsUtils.getListEpisode(context, tvShow.getId(), season.getId());
            }

            @Override
            protected void onPostExecute(List<Episode> episodes) {
                if (episodes != null) {
                    // Simpan hasil episode di episodeMap
                    episodeMap.put(position, episodes);
                    dataList.addAll(position + 1, episodes);
                    notifyItemRangeInserted(position + 1, episodes.size());
                }
            }
        }

    }

    class EpisodeViewHolder extends RecyclerView.ViewHolder {
        private final TextView episodeName;

        public EpisodeViewHolder(View itemView) {
            super(itemView);
            episodeName = itemView.findViewById(R.id.episode_name);
        }

        @OptIn(markerClass = UnstableApi.class)
        @SuppressLint("SetTextI18n")
        void bind(TVShowSeasonDetails season, Episode episode) {
            if (episode.getEpisode_number()!=0) {
                episodeName.setText("Episode " + episode.getEpisode_number() + ": " + episode.getName());
                episodeName.setOnClickListener(v -> {
                    PlayerFragment playerFragment = new PlayerFragment(tvShow, season, episode.id);
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .add(R.id.container, playerFragment)
                            .addToBackStack(null)
                            .commit();
                });
            }else {
                Toast.makeText(context, "No Episodes Found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
