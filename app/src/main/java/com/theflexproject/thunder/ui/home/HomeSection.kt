package com.theflexproject.thunder.ui.home

import com.theflexproject.thunder.model.MyMedia

data class HomeSection(
    val id: String,
    val title: String,
    val items: List<MyMedia>,
    val type: SectionType = SectionType.CAROUSEL,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

enum class SectionType {
    HERO, BANNER, CAROUSEL
}
