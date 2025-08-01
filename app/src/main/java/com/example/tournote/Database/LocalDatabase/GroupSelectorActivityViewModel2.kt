package com.example.tournote.Groups.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.tournote.Database.LocalDatabase.NetworkResult
import com.example.tournote.Database.LocalDatabase.RoomDBRepository
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.database.TourNoteDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class GroupSelectorActivityViewModel2(application: Application) : AndroidViewModel(application) {

    private val repository: RoomDBRepository

    private val _groupsFlow = MutableLiveData<Flow<List<GroupData_Detailed_Model>>>()
    val groups: LiveData<List<GroupData_Detailed_Model>> = _groupsFlow.switchMap { flow ->
        flow.asLiveData()
    }

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _toastmsg = MutableLiveData<String?>()
    val toastmsg: LiveData<String?> = _toastmsg

    // Track if initial sync is complete
    private var initialSyncCompleted = false

    init {
        val database = TourNoteDatabase.getDatabase(application)
        val dao = database.tourNoteDao()
        val firebaseDatabase = FirebaseDatabase.getInstance()
        // Pass viewModelScope to the repository
        repository = RoomDBRepository(firebaseDatabase, dao, this, viewModelScope)

        initializeGroups()
        // Start listening for Firebase changes as soon as the ViewModel is created
        repository.startListeningForGroupChanges()

    }

    private fun initializeGroups() {
        viewModelScope.launch {
            try {
                val groupsFlow = repository.getGroups()
                _groupsFlow.value = groupsFlow

                // Start observing the groups flow
                observeGroupsFlow(groupsFlow)

                // Trigger initial sync if needed (this will also be triggered by the listener now)
                triggerInitialOrBackgroundSync(groupsFlow)

            } catch (e: Exception) {
                Log.e("GroupSelectorVM2", "Error initializing groups", e)
                _error.value = "Failed to initialize groups: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun observeGroupsFlow(groupsFlow: Flow<List<GroupData_Detailed_Model>>) {
        // Use a separate coroutine for observation to avoid blocking initialization
        viewModelScope.launch {
            groupsFlow.collectLatest { detailedGroups ->
                Log.d("GroupSelectorVM2", "Groups flow emitted: ${detailedGroups.size} groups")
                Log.d("GroupSelectorVM2", "Group list: $detailedGroups")


                // Only update GlobalClass after initial sync is attempted
                // This prevents clearing valid data due to initial empty state
                if (initialSyncCompleted || detailedGroups.isNotEmpty()) {
                    //GlobalClass.GroupDetails_Everything = detailedGroups
                    Log.d("GroupSelectorVM2", "GlobalClass.GroupDetails_Everything updated with ${detailedGroups.size} groups")
                }

                // If we have data and were loading, stop loading
                if (detailedGroups.isNotEmpty() && _isLoading.value == true) {
                    _isLoading.value = false
                    Log.d("GroupSelectorVM2", "Loading stopped - data available")
                }
            }
        }
    }

    private fun triggerInitialOrBackgroundSync(groupsFlow: Flow<List<GroupData_Detailed_Model>>) {
        viewModelScope.launch {
            try {
                val hasData = groupsFlow.firstOrNull()?.isNotEmpty() ?: false
                Log.d("GroupSelectorVM2", "Initial check - has local data: $hasData")

                if (hasData) {
                    // We have local data, show it immediately and sync in background
                    _isLoading.value = false
                    Log.d("GroupSelectorVM2", "Performing background sync...")
                    // The Firebase listener will trigger refreshGroupsFromNetwork when needed.
                    // This initial call ensures we have the latest data even if no Firebase change occurs.
                    val result = repository.refreshGroupsFromNetwork()
                    initialSyncCompleted = true

                    when (result) {
                        is NetworkResult.Success -> {
                            Log.d("GroupSelectorVM2", "Background sync completed successfully")
                        }
                        is NetworkResult.Error -> {
                            Log.e("GroupSelectorVM2", "Background sync failed: ${result.message}")
                        }
                    }
                } else {
                    // No local data, show loading and fetch from network
                    _isLoading.value = true
                    Log.d("GroupSelectorVM2", "No local data, fetching from network...")

                    val result = repository.refreshGroupsFromNetwork()
                    initialSyncCompleted = true

                    when (result) {
                        is NetworkResult.Success -> {
                            Log.d("GroupSelectorVM2", "Initial network fetch completed successfully")
                            // Check if we got data after the sync
                            delay(100) // Small delay to let Room update propagate
                            val dataAfterSync = groupsFlow.firstOrNull()?.isNotEmpty() ?: false
                            if (!dataAfterSync) {
                                Log.w("GroupSelectorVM2", "No data available after successful sync")
                                _isLoading.value = false
                            }
                        }
                        is NetworkResult.Error -> {
                            Log.e("GroupSelectorVM2", "Initial network fetch failed: ${result.message}")
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupSelectorVM2", "Error in initial sync", e)
                _error.value = "Sync failed: ${e.message}"
                _isLoading.value = false
                initialSyncCompleted = true
            }
        }
    }

    fun refreshGroupData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("GroupSelectorVM2", "Manual refresh triggered")

                val result = repository.refreshGroupsFromNetwork()

                when (result) {
                    is NetworkResult.Success -> {
                        Log.d("GroupSelectorVM2", "Manual refresh completed successfully")
                        // Wait a bit for Room to update, then stop loading
                        delay(100)
                        val hasData = _groupsFlow.value?.let { flow ->
                            flow.firstOrNull()?.isNotEmpty()
                        } ?: false

                        _isLoading.value = false

                        if (!hasData) {
                            Log.w("GroupSelectorVM2", "No data available after manual refresh")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e("GroupSelectorVM2", "Manual refresh failed: ${result.message}")
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupSelectorVM2", "Error in manual refresh", e)
                _error.value = "Refresh failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop listening for Firebase changes when the ViewModel is cleared
        repository.stopListeningForGroupChanges()
        Log.d("GroupSelectorVM2", "ViewModel onCleared: Firebase listener stopped.")
    }

    fun showToast(message: String) {
        _toastmsg.value = message
        // Clear the message after a short delay to prevent re-showing
        viewModelScope.launch {
            delay(100)
            _toastmsg.value = null
        }
    }

    fun showError(message: String) {
        _error.value = message
        // Clear the error after a short delay
        viewModelScope.launch {
            delay(100)
            _error.value = null
        }
    }
}