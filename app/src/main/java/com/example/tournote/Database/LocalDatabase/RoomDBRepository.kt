package com.example.tournote.Database.LocalDatabase

import android.util.Log
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.UserModel
import com.example.tournote.database.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Add a sealed class for network operation results
sealed class NetworkResult {
    object Success : NetworkResult()
    data class Error(val message: String) : NetworkResult()
}

class RoomDBRepository(
    private val db: FirebaseDatabase,
    private val tourNoteDao: TourNoteDao,
    private val sharedViewModel: GroupSelectorActivityViewModel2, // Keep this for showing toasts/errors
    private val externalScope: CoroutineScope // Inject a CoroutineScope
) {

    private var groupListListener: ValueEventListener? = null
    private var myUid: String? = null

    // Call this from ViewModel to start listening
    fun startListeningForGroupChanges() {
        myUid = GlobalClass.Me?.uid
        if (myUid == null) {
            val errorMsg = "User not logged in. Cannot start group listener."
            sharedViewModel.showError(errorMsg)
            Log.e("RoomDBRepository", errorMsg)
            return
        }

        val userGroupsRef = db.getReference("users").child(myUid!!).child("Groups")

        groupListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // This block will be called initially and every time there's a change
                // in the 'Groups' child of the user.
                Log.d("RoomDBRepository", "Firebase 'Groups' node changed for user: $myUid. Initiating refresh.")
                externalScope.launch {
                    refreshGroupsFromNetwork()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RoomDBRepository", "Firebase listener cancelled for user groups: ${error.message}", error.toException())
                sharedViewModel.showError("Failed to listen for group changes: ${error.message}")
            }
        }
        userGroupsRef.addValueEventListener(groupListListener!!)
        Log.d("RoomDBRepository", "Firebase listener added for user groups: $myUid")
    }

    // Call this from ViewModel to stop listening (e.g., in onCleared)
    fun stopListeningForGroupChanges() {
        myUid?.let { uid ->
            groupListListener?.let { listener ->
                db.getReference("users").child(uid).child("Groups").removeEventListener(listener)
                Log.d("RoomDBRepository", "Firebase listener removed for user groups: $uid")
            }
        }
        groupListListener = null
    }

    suspend fun getGroups(): Flow<List<GroupData_Detailed_Model>> {
        return tourNoteDao.getAllGroupsWithDetails().map { groupWithDetailsList ->
            groupWithDetailsList.map { it.toDetailedModel() }
        }
    }

    // Return NetworkResult to indicate completion status
    suspend fun refreshGroupsFromNetwork(): NetworkResult {
        return try {
            val currentUid = GlobalClass.Me?.uid ?: run {
                val errorMsg = "User not logged in."
                sharedViewModel.showError(errorMsg)
                return NetworkResult.Error(errorMsg)
            }

            Log.d("RoomDBRepository", "Starting network sync for user: $currentUid")

            val myGroupsIdList = db.getReference("users")
                .child(currentUid)
                .child("Groups")
                .get()
                .await()
                .children
                .mapNotNull { it.key }

            Log.d("RoomDBRepository", "User groups from Firebase: ${myGroupsIdList.size}")

            // Step 1: Delete groups that the user has left
            val localGroupIds = tourNoteDao.getAllGroupIds()
            val groupsToDelete = localGroupIds.filter { it !in myGroupsIdList }
            if (groupsToDelete.isNotEmpty()) {
                Log.d("RoomDBRepository", "Deleting ${groupsToDelete.size} groups user left")
                tourNoteDao.deleteGroupsById(groupsToDelete)
            }

            if (myGroupsIdList.isEmpty()) {
                Log.d("RoomDBRepository", "No groups found for user")
                // No need to show toast here, as the UI will update to show empty state
                return NetworkResult.Success
            }

            // Step 2: Fetch all users and upsert them
            val usersSnap = db.getReference("users").get().await()
            val allUsers = usersSnap.children.mapNotNull { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)
                if (email != null) {
                    UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    ).toUserEntity()
                } else null
            }

            Log.d("RoomDBRepository", "Upserting ${allUsers.size} users")
            tourNoteDao.upsertUsers(allUsers)

            // Step 3: Fetch and upsert group details and cross-references for the user's groups
            var successCount = 0
            myGroupsIdList.forEach { groupId ->
                try {
                    val groupRef = db.getReference("groups").child(groupId)
                    val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
                    val groupDetails = groupDetailsSnap.value as? Map<*, *>
                        ?: throw Exception("Invalid GroupDetails for group: $groupId")

                    val name = groupDetails["name"] as? String
                    val isGroupValid = groupDetails["isGroupValid"] as? Boolean
                    val description = groupDetails["description"] as? String
                    val profilePic = groupDetails["profilePic"] as? String
                    val ownerID = groupDetails["owner"] as? String
                    val createdAt = groupDetails["createdAt"] as? Long

                    val groupEntity = GroupEntity(
                        groupID = groupId,
                        name = name,
                        description = description,
                        profilePic = profilePic,
                        isGroupValid = isGroupValid,
                        ownerId = ownerID,
                        createdAt = createdAt
                    )

                    tourNoteDao.upsertGroup(groupEntity)

                    // Get cross-reference data from Firebase
                    val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
                    val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }
                    val rawTrackFriendKeys = groupRef.child("TrackFriends").get().await().children.mapNotNull { it.key }

                    // Get all users from the database for mapping
                    val allUsersMap = tourNoteDao.getAllUsersAsMap()

                    val memberCrossRefs = rawMemberKeys.mapNotNull { key ->
                        val email = key.replace(",", ".")
                        allUsersMap[email]?.let { userEntity ->
                            GroupMemberCrossRef(groupId, userEntity.uid)
                        }
                    }
                    val adminCrossRefs = rawAdminKeys.mapNotNull { key ->
                        val email = key.replace(",", ".")
                        allUsersMap[email]?.let { userEntity ->
                            GroupAdminCrossRef(groupId, userEntity.uid)
                        }
                    }
                    val trackFriendCrossRefs = rawTrackFriendKeys.mapNotNull { key ->
                        val email = key.replace(",", ".")
                        allUsersMap[email]?.let { userEntity ->
                            GroupTrackFriendCrossRef(groupId, userEntity.uid)
                        }
                    }

                    // Delete and re-insert cross-refs
                    tourNoteDao.deleteGroupMembers(groupId)
                    tourNoteDao.insertGroupMembers(memberCrossRefs)
                    tourNoteDao.deleteGroupAdmins(groupId)
                    tourNoteDao.insertGroupAdmins(adminCrossRefs)
                    tourNoteDao.deleteGroupTrackFriends(groupId)
                    tourNoteDao.insertGroupTrackFriends(trackFriendCrossRefs)

                    successCount++
                    Log.d("RoomDBRepository", "Successfully synced group: $groupId")

                } catch (e: Exception) {
                    Log.e("RoomDBRepository", "Error syncing group $groupId: ${e.message}", e)
                    // Continue with other groups instead of failing completely
                }
            }

            val message = if (successCount == myGroupsIdList.size) {
                "All $successCount groups synced successfully!"
            } else {
                "$successCount of ${myGroupsIdList.size} groups synced successfully"
            }

            Log.d("RoomDBRepository", message)
            sharedViewModel.showToast(message) // Keep toast for user feedback on manual sync
            NetworkResult.Success

        } catch (e: Exception) {
            val errorMsg = "Failed to fetch group data: ${e.message}"
            Log.e("RoomDBRepository", errorMsg, e)
            sharedViewModel.showError(errorMsg)
            NetworkResult.Error(errorMsg)
        }
    }
}