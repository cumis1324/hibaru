package com.theflexproject.thunder.model.TVShowInfo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.theflexproject.thunder.model.MyMedia
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class TVShowSeasonDetails(
    @PrimaryKey(autoGenerate = true)
    var idfordb: Int = 0,
    
    var _id: String = "",
    @get:JvmName("getAirDate") @set:JvmName("setAirDate")
    @ColumnInfo(name = "air_date")
    var air_date: String? = null,
    var episodes: ArrayList<Episode>? = null,
    var name: String? = null,
    var overview: String? = null,
    var id: Int = 0,
    @get:JvmName("getPosterPath") @set:JvmName("setPosterPath")
    @ColumnInfo(name = "poster_path")
    var poster_path: String? = null,
    @get:JvmName("getSeasonNumber") @set:JvmName("setSeasonNumber")
    @ColumnInfo(name = "season_number")
    var season_number: Int = 0,
    @get:JvmName("getShowId") @set:JvmName("setShowId")
    @ColumnInfo(name = "show_id")
    var show_id: Long = 0
) : MyMedia, Parcelable
