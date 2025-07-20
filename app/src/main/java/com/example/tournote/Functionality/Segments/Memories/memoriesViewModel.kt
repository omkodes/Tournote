package com.example.tournote.Functionality.Segments.Memories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class memoriesViewModel:ViewModel() {

    val repository = memoriesRepository()

    private val _uploadState = MutableLiveData<Result<Unit>>()
    val uploadState: LiveData<Result<Unit>> get() = _uploadState

    fun uploadImages(
        uris: List<Uri>,
        context: Context,
        groupName: String,
        parentId: String = "root"
    ) {
        viewModelScope.launch {
            try {
                val folderId = repository.findOrCreateFolder("Tournote", parentId)
                val groupFolderId = repository.findOrCreateFolder(groupName, folderId)

                val result = repository.uploadImagesToDriveAndFirebase(uris, context, groupName, groupFolderId)
                _uploadState.postValue(result)
            } catch (e: Exception) {
                _uploadState.postValue(Result.failure(e))
            }
        }
    }


}