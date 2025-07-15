package com.example.tournote.Functionality.Repository

import android.util.Log
import com.example.tournote.Functionality.Segments.TrackFriends.Data.UserLocationData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationRepository {

    private val database = FirebaseDatabase.getInstance()
    private val locationsRef = database.getReference("locations")

    // A map to store active ValueEventListeners. This is crucial for stopping listeners later.
    private val activeLocationListeners = mutableMapOf<String, ValueEventListener>()

    /**
     * Starts listening for real-time location updates for a specific friend.
     * @param friendUid The UID of the friend whose location to track.
     * @param onLocationUpdate A lambda to be called when new location data is received.
     * @param onError A lambda to be called if there's a database error.
     */
    fun startListeningForFriendLocation(
        friendUid: String,
        onLocationUpdate: (UserLocationData) -> Unit,
        onError: (DatabaseError) -> Unit
    ) {
        val friendLocationRef = locationsRef.child(friendUid)

        // If a listener already exists for this UID, remove it first to avoid duplicates
        stopListeningForFriendLocation(friendUid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locationData = snapshot.getValue(UserLocationData::class.java)
                if (locationData != null) {
                    onLocationUpdate(locationData)
                    Log.d("LocationRepo", "Location update for $friendUid: Lat=${locationData.lat}, Lng=${locationData.lng}")
                } else {
                    Log.d("LocationRepo", "Location data for $friendUid is null or not found.")
                    // Optionally, you could pass null to onLocationUpdate if a user disappears
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LocationRepo", "Failed to read location for $friendUid: ${error.message}", error.toException())
                onError(error)
            }
        }
        friendLocationRef.addValueEventListener(listener)
        activeLocationListeners[friendUid] = listener // Store the listener
        Log.d("LocationRepo", "Started listening for location of $friendUid")
    }

    /**
     * Stops listening for location updates for a specific friend.
     * @param friendUid The UID of the friend whose location listener to stop.
     */
    fun stopListeningForFriendLocation(friendUid: String) {
        activeLocationListeners[friendUid]?.let { listener ->
            locationsRef.child(friendUid).removeEventListener(listener)
            activeLocationListeners.remove(friendUid)
            Log.d("LocationRepo", "Stopped listening for location of $friendUid")
        }
    }

    /**
     * Stops all active location listeners managed by this repository.
     * Call this when the ViewModel is no longer needed (e.g., in onCleared()).
     */
    fun stopAllLocationListeners() {
        activeLocationListeners.forEach { (uid, listener) ->
            locationsRef.child(uid).removeEventListener(listener)
        }
        activeLocationListeners.clear()
        Log.d("LocationRepo", "Stopped all location listeners.")
    }
}