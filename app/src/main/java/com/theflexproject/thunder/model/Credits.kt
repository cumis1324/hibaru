package com.theflexproject.thunder.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@JsonClass(generateAdapter = true)
@Parcelize
data class Credits(
    @Json(name = "cast") val cast: List<@RawValue Cast> = emptyList(),
    @Json(name = "crew") val crew: List<@RawValue Crew> = emptyList()
) : Parcelable
