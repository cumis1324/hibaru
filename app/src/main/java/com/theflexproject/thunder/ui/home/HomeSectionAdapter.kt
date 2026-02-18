package com.theflexproject.thunder.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.theflexproject.thunder.R
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.utils.UnityAdHelper

// Sealed class untuk menggabungkan HomeSection dan AdBanner item
sealed class HomeSectionItem {
    data class SectionItem(val section: HomeSection) : HomeSectionItem()
    object AdBannerItem : HomeSectionItem()
}

class HomeSectionAdapter(
    private val isTV: Boolean = false,
    private val onItemClick: (MyMedia) -> Unit,
    private val onSeeAllClick: (HomeSection) -> Unit,
    private val onLoadMore: (String) -> Unit,
    private val onFocusChange: ((MyMedia) -> Unit)? = null
) : ListAdapter<HomeSectionItem, RecyclerView.ViewHolder>(ItemDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is HomeSectionItem.SectionItem -> item.section.id.hashCode().toLong()
            is HomeSectionItem.AdBannerItem -> -1L
        }
    }

    private val viewPool = RecyclerView.RecycledViewPool()
    private val AD_VIEW_TYPE = 1
    private val SECTION_VIEW_TYPE = 0

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeSectionItem.SectionItem -> SECTION_VIEW_TYPE
            is HomeSectionItem.AdBannerItem -> AD_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AD_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.ad_banner_item, parent, false)
                AdBannerViewHolder(view)
            }
            else -> {
                val layoutId = if (isTV) R.layout.home_item_tv else R.layout.home_item
                val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
                SectionViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeSectionItem.SectionItem -> {
                (holder as SectionViewHolder).bind(item.section)
            }
            is HomeSectionItem.AdBannerItem -> {
                (holder as AdBannerViewHolder).bind()
            }
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.recyclerTitleInHomeItem)
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerInHomeItem)
        private var innerAdapter: MediaCarouselAdapter? = null

        init {
            recyclerView.apply {
                if (this !is androidx.leanback.widget.HorizontalGridView) {
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
                setRecycledViewPool(viewPool)
                isNestedScrollingEnabled = false
                
                if (this is androidx.leanback.widget.HorizontalGridView) {
                    setItemSpacing(20)
                    windowAlignment = androidx.leanback.widget.BaseGridView.WINDOW_ALIGN_LOW_EDGE
                    windowAlignmentOffsetPercent = 15f

                    // TV Focus Sync: Report selection changes to update dynamic UI
                    setOnChildViewHolderSelectedListener(object : androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                        override fun onChildViewHolderSelected(
                            parent: RecyclerView,
                            child: RecyclerView.ViewHolder?,
                            position: Int,
                            subposition: Int
                        ) {
                            // Only report if this row itself has focus or is becoming selected
                            if (parent.hasFocus() || parent.isFocused) {
                                (parent.adapter as? MediaCarouselAdapter)?.let { adapter ->
                                    if (position >= 0 && position < adapter.itemCount) {
                                        val item = adapter.currentList[position]
                                        if (item !is LoadingMedia) {
                                            onFocusChange?.invoke(item)
                                        }
                                    }
                                }
                            }
                        }
                    })
                }
            }
        }

        /**
         * Manually triggers a focus update for the currently selected item in this row.
         * Used by HomeFragment when a new row is selected vertically.
         */
        fun syncSelectedFocus() {
            if (isTV) {
                val grid = recyclerView as? androidx.leanback.widget.HorizontalGridView
                val selectedPos = grid?.selectedPosition ?: 0
                innerAdapter?.let { adapter ->
                    if (selectedPos >= 0 && selectedPos < adapter.itemCount) {
                        val item = adapter.currentList[selectedPos]
                        if (item !is LoadingMedia) {
                            onFocusChange?.invoke(item)
                        }
                    }
                }
            }
        }

        fun bind(section: HomeSection) {
            title.text = section.title
            title.setOnClickListener { onSeeAllClick(section) }

            if (innerAdapter == null) {
                innerAdapter = MediaCarouselAdapter(
                    isHero = if (isTV) false else section.type == SectionType.HERO,
                    isTV = isTV,
                    onItemClick = onItemClick,
                    onLoadMore = { onLoadMore(section.id) },
                    onFocusChange = onFocusChange
                )
                recyclerView.adapter = innerAdapter
            }

            val itemsToShow = if (section.isLoadingMore && isTV) {
                section.items.toMutableList<MyMedia>().apply {
                    add(LoadingMedia())
                }
            } else {
                section.items
            }
            
            innerAdapter?.submitList(itemsToShow)
        }
    }

    inner class AdBannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bannerContainer: FrameLayout = itemView.findViewById(R.id.adBannerContainer)

        fun bind() {
            // Load Unity Ads banner
            itemView.context.let { context ->
                try {
                    UnityAdHelper.loadBanner(
                        itemView.context as android.app.Activity,
                        bannerContainer
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AdBannerViewHolder", "Error loading banner: ${e.message}")
                }
            }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<HomeSectionItem>() {
        override fun areItemsTheSame(oldItem: HomeSectionItem, newItem: HomeSectionItem): Boolean {
            return when {
                oldItem is HomeSectionItem.SectionItem && newItem is HomeSectionItem.SectionItem ->
                    oldItem.section.id == newItem.section.id
                oldItem is HomeSectionItem.AdBannerItem && newItem is HomeSectionItem.AdBannerItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeSectionItem, newItem: HomeSectionItem): Boolean {
            return when {
                oldItem is HomeSectionItem.SectionItem && newItem is HomeSectionItem.SectionItem ->
                    oldItem.section == newItem.section
                oldItem is HomeSectionItem.AdBannerItem && newItem is HomeSectionItem.AdBannerItem -> true
                else -> false
            }
        }
    }
}
