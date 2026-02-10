package com.theflexproject.thunder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Crew(
    val id: Int = 0,
    val name: String? = null,
    val job: String? = null,
    val department: String? = null,
    val profilePath: String? = null
) : MyMedia, Parcelable
