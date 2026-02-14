package com.theflexproject.thunder.ui.home

import com.theflexproject.thunder.model.MyMedia

data class HomeSection(
    val id: String,
    val title: String,
    val items: List<MyMedia>,
    val type: SectionType = SectionType.CAROUSEL,
    val isLoadingMore: Boolean = false
)

enum class SectionType {
    HERO, BANNER, CAROUSEL
}
