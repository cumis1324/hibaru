package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Season(
    val airDate: String? = null,
    val episodeCount: Int = 0,
    val id: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val seasonNumber: Int = 0
) : Parcelable
