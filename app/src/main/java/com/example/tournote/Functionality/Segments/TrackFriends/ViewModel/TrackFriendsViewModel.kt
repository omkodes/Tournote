package com.example.tournote.Functionality.Segments.TrackFriends.ViewModel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Repository.LocationRepository
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.Functionality.Segments.TrackFriends.Repository.AlertRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.Repository.authRepository
import kotlinx.coroutines.launch

class TrackFriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val mainRepo = MainActivityRepository()
    private val authRepo = authRepository()
    private val repo = AlertRepository()
    private val locationRepo = LocationRepository() // Initialize LocationRepository

    // LiveData for UI states
    private val _showPermissionRequest = MutableLiveData<Boolean>()
    val showPermissionRequest: LiveData<Boolean> = _showPermissionRequest

    private val _showMapView = MutableLiveData<Boolean>()
    val showMapView: LiveData<Boolean> = _showMapView

    private val _isWebViewReady = MutableLiveData<Boolean>()
    val isWebViewReady: LiveData<Boolean> = _isWebViewReady

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _locationUpdatePending = MutableLiveData<Boolean>()
    val locationUpdatePending: LiveData<Boolean> = _locationUpdatePending

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _trackingEnabled = MutableLiveData<Boolean>()
    val trackingEnabled: LiveData<Boolean> = _trackingEnabled

    // Friend tracking data
    // This list will hold the most recent location and profile data for each tracked friend
    private val _friendsOnMap = MutableLiveData<List<FriendMapData>>()
    val friendsOnMap: LiveData<List<FriendMapData>> = _friendsOnMap

    // WebView interaction commands
    private val _webViewCommand = MutableLiveData<WebViewCommand?>()
    val webViewCommand: LiveData<WebViewCommand?> = _webViewCommand

    // Location retry management for current user's location
    private var currentLocationRetryCount = 0

    // Map to keep track of the last known FriendMapData for each UID for efficient updates
    private val friendMapDataCache = mutableMapOf<String, FriendMapData>()

    // Location retry management
    private val _maxLocationRetries = 3 // Changed to private for internal use

    // Public getter for max retries
    val maxLocationRetries: Int
        get() = _maxLocationRetries // Expose it via a public getter

    // New public function to set error messages, to be called from Fragment if needed
    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    init {
        _isWebViewReady.value = false
        _showPermissionRequest.value = false
        _showMapView.value = false
        _trackingEnabled.value = false
        _friendsOnMap.value = emptyList() // Initialize empty list
        handleUserTrackingPermissions()
    }

    private fun handleUserTrackingPermissions() {
        val currentUser = GlobalClass.Me
        val selectedGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId } // Find the selected group

        if (selectedGroup == null) {
            Log.e("TrackFriendsVM", "No selected group found in GlobalClass.GroupDetails_Everything for tracking.")
            _errorMessage.value = "Error: Group not found."
            _showPermissionRequest.value = false // Or decide how to handle
            _showMapView.value = false // Or decide how to handle
            return // Exit if no valid group is selected
        }

        // Check if current user's email exists and is already in the trackFriends list of the SELECTED group
        val isCurrentUserTracked = currentUser?.email != null &&
                selectedGroup.trackFriends.any { it.email == currentUser.email } // 🔥 MODIFIED LINE

        if (isCurrentUserTracked) {
            Log.d("TrackFriendsVM", "Current user is already tracked. Enabling map.")
            _trackingEnabled.value = true
            showMapAndRequestLocation()
            startTrackingFriendsInGroup() // Start tracking friends immediately
        } else {
            // User is not tracked in the group
            viewModelScope.launch {
                // If current user is the owner of the SELECTED group AND not yet tracked, enable tracking
                if (GlobalClass.Me?.uid == selectedGroup.owner?.uid && !isCurrentUserTracked) { // 🔥 MODIFIED LINE
                    Log.d("TrackFriendsVM", "Current user is owner and not tracked. Enabling tracking.")
                    enableTrackingForCurrentUser() // This will also update GlobalClass
                    showMapAndRequestLocation()
                    startTrackingFriendsInGroup() // Start tracking friends after enabling self
                } else {
                    // User is not tracked and is not the owner, or is tracked (handled above)
                    // In this case, if not tracked, show permission request
                    if (!isCurrentUserTracked) {
                        Log.d("TrackFriendsVM", "Current user not tracked and not owner. Showing permission request.")
                        _showPermissionRequest.value = true
                        _showMapView.value = false
                    }
                    // If isCurrentUserTracked is true, it's handled by the first `if`
                }
            }
        }
    }


    fun onPermissionGranted() {
        viewModelScope.launch {
            enableTrackingForCurrentUser()
            showMapAndRequestLocation()
            startTrackingFriendsInGroup() // Start tracking friends when permission is granted
        }
    }

    private suspend fun enableTrackingForCurrentUser() {
        try {
            mainRepo.EnableMyTrackingOnCurrentGroup() // This function should add currentUser to GroupDetails_Everything.trackFriends in Firebase
            val currentUser = GlobalClass.Me
            // Find the selected group in GlobalClass.GroupDetails_Everything
            val currentGroupList = GlobalClass.GroupDetails_Everything.toMutableList()
            val selectedGroupIndex = currentGroupList.indexOfFirst { it.groupID == GlobalClass.selected_groupId }

            if (currentUser != null && selectedGroupIndex != -1) {
                val selectedGroup = currentGroupList[selectedGroupIndex]
                val currentTrackFriends = selectedGroup.trackFriends.toMutableList() // 🔥 MODIFIED LINE (access trackFriends from selectedGroup)

                if (!currentTrackFriends.any { it.email == currentUser.email }) {
                    currentTrackFriends.add(currentUser) // Add the current user's UserModel object
                    // Create an updated version of the selected group
                    val updatedSelectedGroup = selectedGroup.copy(
                        trackFriends = currentTrackFriends
                    )
                    // Replace the old selected group with the updated one in the global list
                    currentGroupList[selectedGroupIndex] = updatedSelectedGroup
                    GlobalClass.GroupDetails_Everything = currentGroupList // 🔥 MODIFIED LINE (update the entire list)
                }
            }
            _trackingEnabled.value = true
            Log.d("TrackFriendsVM", "Tracking enabled for current user.")
        } catch (e: Exception) {
            Log.e("TrackFriendsViewModel", "Error enabling tracking", e)
            _errorMessage.value = "Error enabling tracking: ${e.message}"
        }
    }

    private fun showMapAndRequestLocation() {
        _showPermissionRequest.value = false
        _showMapView.value = true
    }

    fun onWebViewPageFinished() {
        Log.d("TrackFriendsViewModel", "WebView page finished loading")
        _isWebViewReady.value = true

        // If there's a pending user location update, trigger it
        _currentLocation.value?.let { location ->
            updateLocationOnMap(location)
        }

        // Add all currently tracked friends to the map if WebView is now ready
        _friendsOnMap.value?.forEach { friendData ->
            addFriendOnMap(
                friendData.id,
                friendData.name,
                friendData.lat,
                friendData.lng,
                friendData.status,
                friendData.profilePicUrl
            )
        }
    }

    fun onWebViewError(description: String?) {
        Log.e("TrackFriendsViewModel", "WebView error: $description")
        _errorMessage.value = "WebView error: $description"
    }

    fun onLocationPermissionGranted() {
        Log.d("TrackFriendsViewModel", "Location permission granted")
        // This will be handled by the fragment when it observes location permission changes
    }

    fun onLocationPermissionDenied() {
        Log.w("TrackFriendsViewModel", "Location permission denied")
        _showPermissionRequest.value = true
        _showMapView.value = false
    }

    fun updateCurrentLocation(location: Location) {
        Log.d("TrackFriendsViewModel", "User location update: ${location.latitude}, ${location.longitude}")
        _currentLocation.value = location
        currentLocationRetryCount = 0 // Reset retry count on success

        if (_isWebViewReady.value == true) {
            updateLocationOnMap(location)
        } else {
            _locationUpdatePending.value = true
        }
    }

    fun onLocationUpdateFailed(exception: Exception) {
        currentLocationRetryCount++
        Log.e("TrackFriendsViewModel", "Failed to get current location (attempt $currentLocationRetryCount)", exception)

        if (currentLocationRetryCount >= maxLocationRetries) {
            Log.e("TrackFriendsViewModel", "Max location retries reached")
            _errorMessage.value = "Unable to get current location after $maxLocationRetries attempts"
        }
    }

    fun shouldRetryLocation(): Boolean {
        return currentLocationRetryCount < maxLocationRetries
    }

    fun getCurrentRetryCount(): Int = currentLocationRetryCount

    fun isLocationRecent(location: Location): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < 30000 // 30 seconds
    }

    private fun updateLocationOnMap(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val userProfilePhotoUrl = GlobalClass.Me?.profilePic ?: ""

        if (_isWebViewReady.value == true) {
            _webViewCommand.value = WebViewCommand.UpdateUserLocation(
                latitude, longitude, userProfilePhotoUrl
            )
            _locationUpdatePending.value = false
        } else {
            _locationUpdatePending.value = true
        }
    }

    /**
     * Starts listening to Firebase for location updates for all friends in the current group's TrackFriends list.
     */
    fun startTrackingFriendsInGroup() {
        // Stop any previous listeners to prevent duplicates if called multiple times
        stopTrackingAllFriends()
        // Clear cached friend data for a fresh start
        friendMapDataCache.clear()
        _friendsOnMap.value = emptyList() // Clear friends from map UI initially

        Log.d("TrackFriendsVM", "Starting to track friends in group...")
        val selectedGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId } // Find the selected group

        if (selectedGroup == null) {
            Log.e("TrackFriendsVM", "No selected group found to start tracking friends.")
            return // Cannot proceed without a selected group
        }

        val groupFriends = selectedGroup.trackFriends // 🔥 MODIFIED LINE (access trackFriends from selectedGroup)

        groupFriends.forEach { friend ->
            // Don't track yourself as a "friend" if your own location is handled separately
            if (friend.uid != GlobalClass.Me?.uid) {
                friend.uid?.let { uid ->
                    locationRepo.startListeningForFriendLocation(
                        friendUid = uid,
                        onLocationUpdate = { locationData ->
                            // Convert Firebase UserLocationData to FriendMapData
                            val friendModel = friend // 'friend' is the UserModel object from the list
                            val friendMapData = FriendMapData(
                                id = uid.hashCode(), // Using hashCode for unique int ID for JS.
                                name = friendModel.name ?: friendModel.email ?: "Unknown Friend",
                                lat = locationData.lat,
                                lng = locationData.lng,
                                status = if (isLocationRecent(locationData.timestamp)) "online" else "last seen", // Simple status based on timestamp
                                profilePicUrl = friendModel.profilePic ?: ""
                            )
                            updateFriendMarkerOnMap(friendMapData)
                        },
                        onError = { databaseError ->
                            Log.e("TrackFriendsVM", "Error tracking friend ${friend.email}: ${databaseError.message}")
                            // Optionally, remove the friend from the map if there's a persistent error
                            removeFriendFromMap(uid.hashCode())
                        }
                    )
                }
            } else {
                Log.d("TrackFriendsVM", "Skipping current user ${friend.email} for friend tracking.")
            }
        }
    }

    // Helper function to determine if a timestamp is recent
    private fun isLocationRecent(timestamp: Long): Boolean {
        val locationAge = System.currentTimeMillis() - timestamp
        return locationAge < 60000 // Consider location recent if within last 60 seconds
    }

    /**
     * Updates the _friendsOnMap LiveData and triggers WebView commands based on friend location updates.
     */
    private fun updateFriendMarkerOnMap(newFriendData: FriendMapData) {
        val currentList = _friendsOnMap.value.orEmpty().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == newFriendData.id }

        if (existingIndex != -1) {
            // Friend exists, update their data in the list
            currentList[existingIndex] = newFriendData
            // Trigger WebView update command
            if (_isWebViewReady.value == true) {
                updateFriendOnMap(
                    newFriendData.id,
                    newFriendData.lat,
                    newFriendData.lng,
                    newFriendData.status,
                    newFriendData.profilePicUrl
                )
            }
        } else {
            // New friend, add to the list
            currentList.add(newFriendData)
            // Trigger WebView add command
            if (_isWebViewReady.value == true) {
                addFriendOnMap(
                    newFriendData.id,
                    newFriendData.name,
                    newFriendData.lat,
                    newFriendData.lng,
                    newFriendData.status,
                    newFriendData.profilePicUrl
                )
            }
        }
        _friendsOnMap.value = currentList // Update LiveData
        friendMapDataCache[newFriendData.id.toString()] = newFriendData // Cache for quick lookup
        Log.d("TrackFriendsVM", "Updated friend marker on map for ${newFriendData.name}")
    }

    /**
     * Stops all active Firebase location listeners. Call this when the ViewModel is destroyed.
     */
    fun stopTrackingAllFriends() {
        locationRepo.stopAllLocationListeners()
        friendMapDataCache.clear() // Clear the cache
        _friendsOnMap.value = emptyList() // Clear the LiveData (and thus the map UI)
        Log.d("TrackFriendsVM", "Stopped tracking all friends and cleared map data.")
    }

    // Map interaction functions (already present, just keeping them here for completeness)
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for addFriendOnMap (ID: $id)")
            return
        }
        _webViewCommand.value = WebViewCommand.AddFriendMarker(
            id, name, lat, lng, status, profilePicUrl
        )
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for updateFriendOnMap (ID: $id)")
            return
        }
        _webViewCommand.value = WebViewCommand.UpdateFriendLocation(
            id, lat, lng, status, profilePicUrl
        )
    }

    fun showAlertAPI() {
        val currentUser = GlobalClass.Me
        val selectedGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }
        viewModelScope.launch {
            val result =repo.showAlertAPI(
                currentUser?.name!!,
                currentUser.uid!!,
                selectedGroup?.groupID!!,
                selectedGroup.name!!
            )

            result.fold(
                onSuccess = { Log.d("TrackFriendsVM", "Alert API success")
                            _errorMessage.value = "Alert sent successfully"
                            },
                onFailure = { Log.e("TrackFriendsVM", "Alert API failure", it)
                    _errorMessage.value = it.message
                }
            )

        }
    }

    fun removeFriendFromMap(id: Int) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for removeFriendFromMap (ID: $id)")
            return
        }
        _webViewCommand.value = WebViewCommand.RemoveFriendMarker(id)
        // Also remove from cache and LiveData
        val currentList = _friendsOnMap.value.orEmpty().toMutableList()
        currentList.removeAll { it.id == id }
        _friendsOnMap.value = currentList
        friendMapDataCache.remove(id.toString()) // Assuming ID is hashcode of UID
    }


    fun clearWebViewCommand() {
        _webViewCommand.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Data classes for structured data (already present)
    data class FriendMapData(
        val id: Int, // Using Int for JS bridge; will be UID.hashCode()
        val name: String,
        val lat: Double,
        val lng: Double,
        val status: String,
        val profilePicUrl: String
    )

    // Sealed class for WebView commands (already present)
    sealed class WebViewCommand {
        data class UpdateUserLocation(
            val latitude: Double,
            val longitude: Double,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class AddFriendMarker(
            val id: Int,
            val name: String,
            val lat: Double,
            val lng: Double,
            val status: String,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class UpdateFriendLocation(
            val id: Int,
            val lat: Double,
            val lng: Double,
            val status: String,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class RemoveFriendMarker(val id: Int) : WebViewCommand()
    }

    override fun onCleared() {
        super.onCleared()
        stopTrackingAllFriends() // Stop all Firebase listeners
        Log.d("TrackFriendsVM", "ViewModel cleared. All listeners stopped.")
    }
}