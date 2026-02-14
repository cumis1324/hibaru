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
    private val onItemClick: (MyMedia) -> Unit,
    private val onSeeAllClick: (HomeSection) -> Unit,
    private val onLoadMore: (String) -> Unit
) : ListAdapter<HomeSectionItem, RecyclerView.ViewHolder>(ItemDiffCallback()) {

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
                val view = LayoutInflater.from(parent.context).inflate(R.layout.home_item, parent, false)
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

        fun bind(section: HomeSection) {
            title.text = section.title
            title.setOnClickListener { onSeeAllClick(section) }

            val adapter = MediaCarouselAdapter(
                isHero = section.type == SectionType.HERO,
                onItemClick = onItemClick,
                onLoadMore = { onLoadMore(section.id) }
            )
            
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                this.adapter = adapter
                setRecycledViewPool(viewPool)
                // Disable nested scrolling to let the parent handle vertical scroll
                isNestedScrollingEnabled = false
            }
            
            adapter.submitList(section.items)
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
