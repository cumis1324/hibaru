package com.theflexproject.thunder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Data(
    val files: List<File>? = null
) : Parcelable
