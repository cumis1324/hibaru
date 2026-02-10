package com.theflexproject.thunder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cast(
    val id: Int = 0,
    val name: String? = null,
    val character: String? = null,
    val profilePath: String? = null
) : MyMedia, Parcelable
