package com.theflexproject.thunder.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Cast(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String? = null,
    @Json(name = "character") val character: String? = null,
    @Json(name = "profile_path") val profilePath: String? = null
) : MyMedia, Parcelable
