package com.theflexproject.thunder.ui.home

import android.annotation.SuppressLint
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
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.theflexproject.thunder.model.FirebaseManager
import com.theflexproject.thunder.data.sync.SyncPrefs
import com.theflexproject.thunder.model.HistoryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.DecimalFormat

class LoadingMedia : MyMedia

class MediaCarouselAdapter(
    private val isHero: Boolean = false,
    private val isTV: Boolean = false,
    private val syncPrefs: SyncPrefs,
    private val onItemClick: (MyMedia) -> Unit,
    private var onLoadMore: () -> Unit,
    private val onFocusChange: ((MyMedia) -> Unit)? = null
) : ListAdapter<MyMedia, MediaCarouselAdapter.ViewHolder>(MediaDiffCallback()) {

    fun updateOnLoadMore(newOnLoadMore: () -> Unit) {
        this.onLoadMore = newOnLoadMore
    }

    private val VIEW_TYPE_ITEM = 0
    private val VIEW_TYPE_LOADING = 1

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is LoadingMedia) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == VIEW_TYPE_LOADING) {
            if (isTV) R.layout.media_item_loading_tv else R.layout.media_item_loading
        } else if (isHero) {
            if (isTV) R.layout.movie_item_hero_tv else R.layout.movie_item_banner
        } else {
            if (isTV) R.layout.media_item_tv else R.layout.media_item
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    private var historyCache: Map<String, HistoryEntry> = emptyMap()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            updateHistoryCache()
        }
        val item = getItem(position)
        if (item is LoadingMedia) {
            onLoadMore()
        } else {
            holder.bind(item)
            
            // Fallback trigger if bumper isn't reached or not yet shown
            if (position >= itemCount - 3 && itemCount >= 15) {
                onLoadMore()
            }
        }
    }

    private fun updateHistoryCache() {
        val json = syncPrefs.playbackHistoryJson
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, HistoryEntry>>() {}.type
                historyCache = Gson().fromJson(json, type)
            } catch (e: Exception) {
                android.util.Log.e("MediaCarouselAdapter", "Error parsing history JSON", e)
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView? = if (isHero) itemView.findViewById(R.id.textView4) else itemView.findViewById(R.id.nameInMediaItem)
        private val poster: ImageView? = if (isHero) itemView.findViewById(R.id.moviePoster) else itemView.findViewById(R.id.posterInMediaItem)
        private val textStar: TextView? = itemView.findViewById(R.id.textStar)
        private val subtitle: TextView? = itemView.findViewById(R.id.episodeSubtitleInMediaItem)

        // TV Hero specific views
        private val description: TextView? = itemView.findViewById(R.id.movieDescription)
        private val watchNowBtn: View? = itemView.findViewById(R.id.watchNowBtn)
        
        // TV item specifics
        private val focusOverlay: View? = itemView.findViewById(R.id.focusOverlay)
        private val progressOverlay: View? = itemView.findViewById(R.id.progress_overlay2)

        init {
            if (isTV) {
                itemView.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                        focusOverlay?.visibility = View.VISIBLE
                        // Bring focused item to front for scale effect
                        view.z = 10f
                        
                        // Pagination Trigger for TV (Focus-based)
                        if (bindingAdapterPosition >= itemCount - 5 && itemCount >= 15) {
                            android.util.Log.d("MediaCarouselAdapter", "Focus-based trigger for position: $bindingAdapterPosition")
                            onLoadMore()
                        }
                    } else {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                        focusOverlay?.visibility = View.GONE
                        view.z = 0f
                    }
                }
            }
        }

        fun bind(item: MyMedia) {
            val context = itemView.context
            var title: String? = null
            var posterPath: String? = null
            var backdropPath: String? = null
            var voteAverage: Double = 0.0
            var year: String? = null
            var overview: String? = null

            when (item) {
                is Movie -> {
                    title = if (item.original_language == "id") item.original_title ?: item.title ?: item.file_name else (item.title ?: item.file_name)
                    posterPath = item.poster_path
                    backdropPath = item.backdrop_path
                    voteAverage = item.vote_average
                    overview = item.overview
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
                    overview = item.overview
                    val date = item.first_air_date
                    if (!date.isNullOrEmpty() && date.length >= 4) {
                        year = date.substring(0, 4)
                    }
                }
                is Episode -> {
                    val seasonLabel = "S${item.season_number}E${item.episode_number}"
                    // Main title: episode name only (maxLines=1, ellipsize)
                    title = item.name
                    // Subtitle: ShowName | S*E*
                    subtitle?.visibility = View.VISIBLE
                    subtitle?.text = "${item.show_name ?: ""} | $seasonLabel"
                    posterPath = item.show_poster_path  // Always use show poster for consistent portrait cards
                    voteAverage = item.vote_average
                    overview = item.overview
                    val date = item.air_date
                    if (!date.isNullOrEmpty() && date.length >= 4) {
                        year = null // don't show year for episodes — subtitle has more useful info
                    }
                }
            }

            if (year != null && title != null) {
                name?.text = "$title ($year)"
            } else {
                name?.text = title ?: ""
            }

            if (voteAverage != 0.0) {
                textStar?.visibility = View.VISIBLE
                textStar?.text = DecimalFormat("0.0").format(voteAverage)
            } else {
                textStar?.visibility = View.GONE
            }
            
            // TV Hero metadata
            description?.text = overview ?: ""
            watchNowBtn?.setOnClickListener { onItemClick(item) }

            val pathToShow = if (isHero) (backdropPath ?: posterPath) else posterPath

            if (pathToShow != null && poster != null) {
                val baseUrl = if (isTV && isHero) Constants.TMDB_BACKDROP_IMAGE_BASE_URL else Constants.TMDB_IMAGE_BASE_URL
                Glide.with(context)
                    .load(baseUrl + pathToShow)
                    .placeholder(android.R.color.black)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(if (isTV && isHero) 24 else 14)))
                    .into(poster)
            } else if (poster != null) {
                poster.setImageResource(android.R.color.black)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Sync Progress from Local Cache (Optimized)
            val tmdbId = when(item) {
                is Movie -> item.id.toString()
                is Episode -> item.id.toString()
                else -> null
            }
            if (tmdbId != null && progressOverlay != null) {
                val entry = historyCache[tmdbId]
                val lastPos = entry?.lastPosition ?: 0L
                if (lastPos > 0) {
                    val runtime = when(item) {
                        is Movie -> item.runtime.toLong() * 60 * 1000
                        is Episode -> item.runtime.toLong() * 60 * 1000
                        else -> 0L
                    }
                    if (runtime > 0) {
                        val progress = lastPos.toDouble() / runtime
                        
                        // Use ViewTreeObserver to get width if not yet laid out
                        if ((poster?.width ?: 0) > 0) {
                            applyProgress(progressOverlay, progress, poster!!.width)
                        } else {
                            poster?.viewTreeObserver?.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    poster.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                    applyProgress(progressOverlay, progress, poster.width)
                                }
                            })
                        }
                    } else {
                        progressOverlay.visibility = View.GONE
                    }
                } else {
                    progressOverlay.visibility = View.GONE
                }
            } else {
                progressOverlay?.visibility = View.GONE
            }

            // Sync visual state manually if this item is re-bound while focused (recycling fix)
            if (isTV) {
                if (itemView.hasFocus()) {
                    itemView.scaleX = 1.1f
                    itemView.scaleY = 1.1f
                    itemView.z = 10f
                    focusOverlay?.visibility = View.VISIBLE
                } else {
                    itemView.scaleX = 1.0f
                    itemView.scaleY = 1.0f
                    itemView.z = 0f
                }
            }
        }

        private fun applyProgress(view: View, progress: Double, fullWidth: Int) {
            view.visibility = View.VISIBLE
            val params = view.layoutParams
            params.width = (fullWidth * progress).toInt().coerceAtMost(fullWidth)
            view.layoutParams = params
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<MyMedia>() {
        override fun areItemsTheSame(oldItem: MyMedia, newItem: MyMedia): Boolean {
            if (oldItem is LoadingMedia && newItem is LoadingMedia) return true
            
            return if (oldItem is Movie && newItem is Movie) {
                oldItem.id == newItem.id
            } else if (oldItem is TVShow && newItem is TVShow) {
                oldItem.id == newItem.id
            } else if (oldItem is Episode && newItem is Episode) {
                oldItem.id == newItem.id
            } else {
                false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: MyMedia, newItem: MyMedia): Boolean {
            return oldItem == newItem // Data classes implement equals
        }
    }
}
