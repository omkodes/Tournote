package com.example.tournote.Functionality.Segments.ChatRoom.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.groupData
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.userData
import com.example.tournote.Functionality.Segments.ChatRoom.Repository.firebaseChatRepository
import com.example.tournote.Onboarding.Repository.authRepository
import kotlinx.coroutines.launch

class groupViewModel:ViewModel() {

    val repo = firebaseChatRepository()
    val authrepo = authRepository()

    private val _groupInfo = MutableLiveData<Result<groupData>>()
    val groupInfo: LiveData<Result<groupData>> = _groupInfo

    private val _userInfo = MutableLiveData<Result<userData>>()
    val userInfo: LiveData<Result<userData>> = _userInfo

    private val _emailList = MutableLiveData<MutableList<String>>()
    val emailList: LiveData<MutableList<String>> = _emailList

    private val _userGroupList = MutableLiveData<List<String>>()
    val userGroupList: LiveData<List<String>> = _userGroupList

    private val _thiSuserGroupList = MutableLiveData<List<String>>()
    val thiSuserGroupList: LiveData<List<String>> = _thiSuserGroupList

    fun loadMemberEmails(groupId: String) {
        viewModelScope.launch {
            try {
                val emails = repo.groupMemebersData(groupId )
                emails.onSuccess {
                    _emailList.value = it
                }
                emails.onFailure {
                    _emailList.value = mutableListOf<String>()
                }
            } catch (e: Exception) {
                // Handle error gracefully
                _emailList.value = mutableListOf<String>()
            }
        }
    }

    fun loadUserGroupList(groupId: String) {
        viewModelScope.launch {
            try {
                Log.d("grpadapter", "loadUserGroupList: $groupId")
                val emails = repo.userGroupData(groupId )
                emails.onSuccess {
                    Log.d("grpadapter", "loadUserGroupList: $it")
                    _userGroupList.value = it
                }
                emails.onFailure {
                    Log.d("grpadapter", "loadUserGroupList: $it")
                    _userGroupList.value = emptyList()
                }
            } catch (e: Exception) {
                // Handle error gracefully
                Log.d("grpadapter", "loadUserGroupList: $e")
                _userGroupList.value = emptyList()
            }
        }
    }

    fun loadThisUserGroupList() {
        viewModelScope.launch {
            try {
                Log.d("grpadapter", "thisloadUserGroupList: this")
                val userId = authrepo.getUid()
                val emails = repo.userGroupData(userId!!)
                emails.onSuccess {
                    Log.d("grpadapter", "thisloadUserGroupList: $it")
                    _thiSuserGroupList.value = it
                }
                emails.onFailure {
                    Log.d("grpadapter", "thisloadUserGroupList: $it")
                    _thiSuserGroupList.value = emptyList()
                }
            } catch (e: Exception) {
                // Handle error gracefully
                Log.d("grpadapter", "thisloadUserGroupList: $e")
                _thiSuserGroupList.value = emptyList()
            }
        }
    }



    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            val result = repo.groupData(groupId)
            _groupInfo.value = result
        }
    }

    fun loadUser(groupId: String) {
        viewModelScope.launch {
            val result = repo.userData(groupId)
            _userInfo.value = result
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val date = java.util.Date(timestamp)
        return sdf.format(date)
    }



}