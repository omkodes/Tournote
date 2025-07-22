package com.example.tournote.Functionality.Segments.Memories.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotosData(
    val fileId: String,
    val date: String,
    val timestamp: Long,
    val group: String,
    val mimeType: String,
    val userId: String
):Parcelable
