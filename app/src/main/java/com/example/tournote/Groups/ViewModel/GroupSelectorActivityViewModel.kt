package com.example.tournote.Groups.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.DataClass.GroupInfoModel
import com.example.tournote.Groups.Repository.CreateGroupRepository
import com.example.tournote.UserModel
import kotlinx.coroutines.launch
import java.io.File

class GroupSelectorActivityViewModel : ViewModel() {

    val repo = CreateGroupRepository()
    val isLoading = MutableLiveData<Boolean>()

    private val _resetUI =  MutableLiveData(false)
    val resetUi: LiveData<Boolean> = _resetUI

    private val _imageUrl = MutableLiveData<String?>()
    val imageUrl: LiveData<String?> get() = _imageUrl

    private val _toastmsg = MutableLiveData<String?>(null)
    val toastmsg : LiveData<String?> get() = _toastmsg

    private val _navigateToHome =  MutableLiveData(true)
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    private val _users = MutableLiveData<List<UserModel>>()
    val users: LiveData<List<UserModel>> = _users

    private val _usersIn = MutableLiveData<List<UserModel>>()
    val usersIn: LiveData<List<UserModel>> = _usersIn

    private val _admins = MutableLiveData<List<UserModel>>()
    val admins: LiveData<List<UserModel>> = _admins

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _groups = MutableLiveData<List<GroupData_Detailed_Model>>()
    val groups: LiveData<List<GroupData_Detailed_Model>> = _groups

    fun navToHomeSwitch() {
        _resetUI.value=true
        _navigateToHome.value = !(_navigateToHome.value ?: false)
    }


    fun resetUiToggleSwitch(){
        _resetUI.value = !(_resetUI.value ?: false)
    }


    fun addUserToGrp(user: UserModel) {
        val current = _usersIn.value?.toMutableList() ?: mutableListOf()
        if (current.isEmpty()) {
            val defaultUser = UserModel(
                uid = null,
                email = GlobalClass.Me?.email.toString(),
                name = "(You)",
                phoneNumber = null,
                profilePic = null
            )
            current.add(defaultUser)
        }
        if (!current.contains(user)) {
            current.add(user)
            _usersIn.value = current
        }
    }

    fun removeUserFromGrp(user: UserModel) {
        val current = _usersIn.value?.toMutableList() ?: mutableListOf()
        if (current.contains(user)) {
            current.remove(user)
            _usersIn.value = current
        }
    }


    fun checkForPresence_AdminList(user: UserModel): Boolean{
        if(_admins.value?.any { it.uid == user.uid } == true){
            return true
        }else{
            return false
        }
    }

    fun addUserToAdminList(user : UserModel){
        val current = _admins.value?.toMutableList() ?: mutableListOf()
        current.add(user)
        _admins.value=current
    }
    fun removeUserFromAdminList(user : UserModel){
        val current = _admins.value?.toMutableList() ?: mutableListOf()
        if (current.contains(user)) {
            current.remove(user)
            _admins.value=current
        }
    }

    fun removeAllUsersFromGrp(){
        _users.value=emptyList()
        _usersIn.value=emptyList()
        _imageUrl.value=null
    }


    fun createGroup(name: String, description: String, members: List<UserModel>, admins: List<UserModel>, groupProfileUrl: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = repo.registerGroup(name, description, members,admins, GlobalClass.Me?.email.toString(),groupProfileUrl)

                if (result.isSuccess) {
                    val groupId = result.getOrNull()
                    //_toastmsg.postValue("Group created with ID: $groupId")
                    _resetUI.value=true
                    _navigateToHome.postValue(true)
                } else {
                    _toastmsg.postValue("Failed to create group: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _toastmsg.postValue("Exception occurred: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }


    fun fetchAllActiveUsers() {
        viewModelScope.launch {
            val result = repo.fetchDetailsOfAllUsers()
            result.onSuccess { userArray ->
                _users.value = userArray
            }.onFailure { e ->
                _error.value = e.message
            }
        }
    }

    fun fetchGroupDetails(){
        viewModelScope.launch {
            _groups.value= GlobalClass.GroupDetails_Everything

        }
    }

    fun uploadImageToCloudinary(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val file = getFileFromUri(uri, context)

                val uploadOptions = hashMapOf<String, Any>(
                    "public_id" to "user_${System.currentTimeMillis()}",
                    "folder" to "android_uploads",
                    "resource_type" to "image"
                )

                MediaManager.get()
                    .upload(file.absolutePath)
                    .options(uploadOptions)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {
                            isLoading.postValue(true)
                        }

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                            // optional progress updates
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            val url = resultData?.get("secure_url") as? String
                            _imageUrl.postValue(url)
                            isLoading.postValue(false)
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            _toastmsg.postValue("Upload Failed: ${error?.description}")
                            isLoading.postValue(false)
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            // not needed here
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                _toastmsg.postValue("Exception: ${e.message}")
                isLoading.postValue(false)
            }
        }
    }

    private fun getFileFromUri(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload_", ".jpg", context.cacheDir)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
