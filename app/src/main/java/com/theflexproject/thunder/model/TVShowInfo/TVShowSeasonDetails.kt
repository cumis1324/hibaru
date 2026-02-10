package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.theflexproject.thunder.model.MyMedia
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class TVShowSeasonDetails(
    @PrimaryKey(autoGenerate = true)
    val idfordb: Int = 0,
    
    val _id: String = "",
    val airDate: String? = null,
    val episodes: List<Episode>? = null,
    val name: String? = null,
    val overview: String? = null,
    val id: Int = 0,
    val posterPath: String? = null,
    val seasonNumber: Int = 0,
    val showId: Long = 0
) : MyMedia, Parcelable
