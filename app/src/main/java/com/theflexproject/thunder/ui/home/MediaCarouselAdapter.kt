package com.theflexproject.thunder.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.theflexproject.thunder.Constants
import com.theflexproject.thunder.R
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import java.text.DecimalFormat

class MediaCarouselAdapter(
    private val isHero: Boolean = false,
    private val onItemClick: (MyMedia) -> Unit,
    private val onLoadMore: () -> Unit
) : ListAdapter<MyMedia, MediaCarouselAdapter.ViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (isHero) R.layout.movie_item_banner else R.layout.media_item
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        
        if (position >= itemCount - 5 && itemCount >= 10) {
            onLoadMore()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView? = if (isHero) itemView.findViewById(R.id.textView4) else itemView.findViewById(R.id.nameInMediaItem)
        private val poster: ImageView? = if (isHero) itemView.findViewById(R.id.moviePoster) else itemView.findViewById(R.id.posterInMediaItem)
        private val textStar: TextView? = itemView.findViewById(R.id.textStar)

        fun bind(item: MyMedia) {
            val context = itemView.context
            var title: String? = null
            var posterPath: String? = null
            var backdropPath: String? = null
            var voteAverage: Double = 0.0
            var year: String? = null

            when (item) {
                is Movie -> {
                    title = item.title ?: item.file_name
                    posterPath = item.poster_path
                    backdropPath = item.backdrop_path
                    voteAverage = item.vote_average
                    val date = item.release_date
                    if (!date.isNullOrEmpty() && date.length >= 4) {
                        year = date.substring(0, 4)
                    }
                }
                is TVShow -> {
                    title = item.name
                    posterPath = item.poster_path
                    backdropPath = item.backdrop_path
                    voteAverage = item.vote_average
                    val date = item.first_air_date
                    if (!date.isNullOrEmpty() && date.length >= 4) {
                        year = date.substring(0, 4)
                    }
                }
            }

            if (year != null) {
                name?.text = "$title ($year)"
            } else {
                name?.text = title
            }

            if (voteAverage != 0.0) {
                textStar?.visibility = View.VISIBLE
                textStar?.text = DecimalFormat("0.0").format(voteAverage)
            } else {
                textStar?.visibility = View.GONE
            }

            val pathToShow = if (isHero) (backdropPath ?: posterPath) else posterPath

            if (pathToShow != null && poster != null) {
                Glide.with(context)
                    .load(Constants.TMDB_IMAGE_BASE_URL + pathToShow)
                    .placeholder(android.R.color.black)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(14)))
                    .into(poster)
            } else if (poster != null) {
                poster.setImageResource(android.R.color.black)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<MyMedia>() {
        override fun areItemsTheSame(oldItem: MyMedia, newItem: MyMedia): Boolean {
            // MyMedia interface doesn't enforce ID, so checking instance type and ID
            return if (oldItem is Movie && newItem is Movie) {
                oldItem.id == newItem.id
            } else if (oldItem is TVShow && newItem is TVShow) {
                oldItem.id == newItem.id
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItem: MyMedia, newItem: MyMedia): Boolean {
            return oldItem == newItem // Data classes implement equals
        }
    }
}
