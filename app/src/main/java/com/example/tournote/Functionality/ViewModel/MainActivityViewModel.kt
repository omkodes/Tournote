package com.example.tournote.Functionality.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Segments.ChatRoom.Repository.ChatRepository
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.ChatViewModel
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import kotlinx.coroutines.launch
import android.util.Log // Added for logging potential issues

class MainActivityViewModel : ViewModel() {


    private val repo = FirebaseRTDBRepository()
    val chatRepo = ChatRepository()
    val chatView = ChatViewModel() // Consider if ChatViewModel should be initialized here or passed from UI

    private val _groupInfo = MutableLiveData<Result<GroupData_Detailed_Model>>()
    val groupInfo: LiveData<Result<GroupData_Detailed_Model>> = _groupInfo

    private val _groupId = MutableLiveData<String?>(null)
    val groupId: LiveData<String?> = _groupId

    private val _isGroupValid = MutableLiveData<Boolean?>(true)
    val isGroupValid: LiveData<Boolean?> = _isGroupValid

    fun loadGroup() {
        viewModelScope.launch {
            // 🔥 MODIFICATION: Find the currently selected group from the list
            val selectedGroup = GlobalClass.GroupDetails_Everything

            if (selectedGroup != null) {
                _groupId.value = selectedGroup.groupID // Use properties of the found group
                _groupInfo.value = Result.success(selectedGroup)

                // 🔐 Now safe to call after data is ready, using the found group's ID
                selectedGroup.groupID?.let {
                    chatRepo.connectSocket(it, GlobalClass.Me?.uid.toString())
                } ?: run {
                    Log.e("MainActivityViewModel", "Group ID is null for selected group. Cannot connect chat socket.")
                    _groupInfo.value = Result.failure(Exception("Selected group has no ID."))
                }
            } else {
                // Handle case where selected group is not found (e.g., GlobalClass.selected_groupId is null or invalid)
                Log.e("MainActivityViewModel", "Selected group not found in GlobalClass.GroupDetails_Everything or selected_groupId is null: ${GlobalClass.selected_groupId}")
                _groupId.value = null
                _groupInfo.value = Result.failure(Exception("Selected group data not found."))
            }
        }
    }

    fun loadGroupValidity(valid : Boolean){
        // This function sets the _isGroupValid LiveData based on an external 'valid' parameter.
        // It does not fetch validity from the GlobalClass list, but rather updates the UI state.
        _isGroupValid.value=valid
    }
    fun turnOffGroupValidity(){
        _isGroupValid.value=false
    }

    fun loadChatRoom() {
        viewModelScope.launch {
            // 🔥 MODIFICATION: Find the currently selected group to get its ID for the chat room
            val selectedGroup = GlobalClass.GroupDetails_Everything

            selectedGroup?.groupID?.let {
                chatView.joinROOM(it)
            } ?: run {
                Log.e("MainActivityViewModel", "Group ID is null for selected group. Cannot join chat room.")
                // Optionally, inform the UI about this error
            }
        }
    }
}