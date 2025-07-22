package com.example.tournote.Functionality.Segments.Memories

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.GlobalClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class memoriesViewModel:ViewModel() {

    val repository = memoriesRepository()

    private val _uploadState = MutableLiveData<Result<Unit>>()
    val uploadState: LiveData<Result<Unit>> get() = _uploadState

    private val _isZipLoading = MutableLiveData<Boolean>()
    val isZipLoading: LiveData<Boolean> get() = _isZipLoading


    fun uploadMedia(
        uris: List<Uri>,
        context: Context,
        groupName: String,
        parentId: String = "root"
    ) {
        viewModelScope.launch {
            try {
                val folderId = repository.findOrCreateFolder("Tournote", parentId)
                val groupFolderId = repository.findOrCreateFolder(groupName, folderId)

                val result = repository.uploadMediaToDriveAndFirebase(uris, context, groupName, groupFolderId)
                _uploadState.postValue(result)
            } catch (e: Exception) {
                _uploadState.postValue(Result.failure(e))
            }
        }
    }

    fun downloadAndShareZip(context: Context) {
        viewModelScope.launch {
            _isZipLoading.postValue(true) // 🚀 Show loading

            val result = repository.downloadGroupMediaAsZip(
                context = context,
                groupId = GlobalClass.selected_groupId!!,
                zipFileName = "group_memories_${System.currentTimeMillis()}.zip"
            )

            _isZipLoading.postValue(false) // ✅ Done loading

            if (result.isSuccess) {
                val uri = result.getOrNull()
                Toast.makeText(context, "ZIP saved to Downloads", Toast.LENGTH_SHORT).show()

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(intent, "Share ZIP"))

            } else {
                Toast.makeText(
                    context,
                    "Failed to export: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("memoriesViewModel", "Download error: ${result.exceptionOrNull()?.message}")
            }
        }
    }









}