package com.theflexproject.thunder.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.theflexproject.thunder.R
import com.theflexproject.thunder.model.MyMedia

class HomeSectionAdapter(
    private val onItemClick: (MyMedia) -> Unit,
    private val onSeeAllClick: (HomeSection) -> Unit,
    private val onLoadMore: (String) -> Unit
) : ListAdapter<HomeSection, HomeSectionAdapter.ViewHolder>(SectionDiffCallback()) {

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

    class SectionDiffCallback : DiffUtil.ItemCallback<HomeSection>() {
        override fun areItemsTheSame(oldItem: HomeSection, newItem: HomeSection): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HomeSection, newItem: HomeSection): Boolean = oldItem == newItem
    }
}
