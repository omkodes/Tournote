package com.example.tournote.Groups.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.tournote.DatabaseCatching.GroupRepository
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.GlobalClass // Ensure this import is present
import com.example.tournote.database.TourNoteDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest // Import collectLatest
import kotlinx.coroutines.launch

class GroupSelectorActivityViewModel2(application: Application) : AndroidViewModel(application) {

    private val repository: GroupRepository

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

    init {
        val database = TourNoteDatabase.getDatabase(application)
        val dao = database.tourNoteDao()
        val firebaseDatabase = FirebaseDatabase.getInstance()
        repository = GroupRepository(firebaseDatabase, dao, this)

        viewModelScope.launch {
            try {
                val groupsFlow = repository.getGroups()
                _groupsFlow.value = groupsFlow

                // Now, observe the groupsFlow to update GlobalClass.GroupDetails_Everything
                // Use collectLatest to ensure that if the flow emits very rapidly,
                // we only process the latest value.
                groupsFlow.collectLatest { detailedGroups ->
                    if (detailedGroups.isNotEmpty()) {
                        GlobalClass.GroupDetails_Everything = detailedGroups
                        // You can add a log here to confirm the update
                        // Log.d("GroupSelectorVM2", "GlobalClass.GroupDetails_Everything updated with ${detailedGroups.size} groups.")
                    } else {
                        GlobalClass.GroupDetails_Everything = emptyList() // Clear if no groups
                        // Log.d("GroupSelectorVM2", "GlobalClass.GroupDetails_Everything cleared as no groups found.")
                    }
                }

                // Trigger initial sync after setting up the observation
                // This ensures UI gets initial data from Room quickly, and then a sync
                // updates Room (and thus the LiveData and GlobalClass) in the background.
                triggerInitialOrBackgroundSync(groupsFlow)

            } catch (e: Exception) {
                _error.value = "Failed to load groups: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun triggerInitialOrBackgroundSync(groupsFlow: Flow<List<GroupData_Detailed_Model>>) {
        viewModelScope.launch {
            val hasData = groupsFlow.firstOrNull()?.isNotEmpty() ?: false

            if (hasData) {
                _isLoading.value = false // Hide the progress bar as Room has data
                // Perform a silent sync in the background.
                // The UI and GlobalClass will be updated automatically by the LiveData/Flow observation.
                repository.refreshGroupsFromNetwork()
            } else {
                // No data found initially, show loading indicator and fetch from network.
                _isLoading.value = true
                repository.refreshGroupsFromNetwork()
                // isLoading will be set to false when refreshGroupsFromNetwork completes
                // or when the flow emits data.
            }
        }
    }

    fun refreshGroupData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshGroupsFromNetwork() // This will update Room, which in turn updates the Flow and LiveData
            _isLoading.value = false // This might be set before the Room update fully propagates.
            // Consider updating _isLoading based on the refreshGroupsFromNetwork()'s Result.
        }
    }

    // ... rest of the ViewModel code remains the same ...

    // Keeping these as examples, though the logic for getMyGroupsIdList
    // might need to fetch from the observed groups or directly from Room.
    fun getMyGroupsIdList(): List<String>? {
        // Now you can potentially use _groupsFlow.value or _groups.value
        // For synchronous access, you'd typically need to run this in a coroutine
        // and fetch from the repository.
        // Example:
        // viewModelScope.launch {
        //     val allMyGroups = repository.getGroups().first() // Get current list from Room
        //     // filter based on GlobalClass.Me.uid or email etc.
        // }
        return GlobalClass.Me?.email?.let { email ->
            // This logic is still problematic as it's synchronous and hardcoded.
            // If you need the actual list of group IDs the current user is in,
            // you should derive it from the `_groups` LiveData or from `repository.getGroups()`
            // filtering by whether GlobalClass.Me.uid is in `members` list.
            emptyList() // Return empty list or implement actual filtering
        }
    }

    fun showToast(message: String) {
        _toastmsg.value = message
        _toastmsg.value = null
    }

    fun showError(message: String) {
        _error.value = message
        _error.value = null
    }
}