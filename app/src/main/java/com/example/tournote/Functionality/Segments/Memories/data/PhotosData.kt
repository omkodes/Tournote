package com.example.tournote.Functionality.Segments.Memories.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotosData(
    val fileId: String,
    val date: String,
    val timestamp: Long,
    val userId: String
):Parcelable
