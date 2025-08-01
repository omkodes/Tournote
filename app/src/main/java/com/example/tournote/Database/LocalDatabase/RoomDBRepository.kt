package com.example.tournote.Database.LocalDatabase

import android.util.Log
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.UserModel
import com.google.firebase.auth.FirebaseAuth
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
        myUid = FirebaseAuth.getInstance().currentUser?.uid
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

    // Helper function to sanitize email for Firebase keys
    private fun sanitizeEmail(email: String): String {
        return email.replace(".", ",")
    }

    // Helper function to unsanitize email from Firebase keys
    private fun unsanitizeEmail(sanitizedEmail: String): String {
        return sanitizedEmail.replace(",", ".")
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
                val uid = userSnap.key ?: return@mapNotNull null
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)
                val name = personalDetails.child("name").getValue(String::class.java) ?: ""
                val phoneNumber = personalDetails.child("phone").getValue(String::class.java) ?: ""
                val profilePic = personalDetails.child("profilePic").getValue(String::class.java)

                if (email != null) {
                    UserModel(
                        uid = uid,
                        email = email, // Store unsanitized email in UserModel
                        name = name,
                        phoneNumber = phoneNumber,
                        profilePic = profilePic
                    ).toUserEntity()
                } else {
                    Log.w("RoomDBRepository", "User $uid has no email, skipping")
                    null
                }
            }

            Log.d("RoomDBRepository", "Upserting ${allUsers.size} users")
            tourNoteDao.upsertUsers(allUsers)

            // ... (existing code remains the same up to here)

// Step 3: Create a mapping from sanitized email to uid for efficient lookups
            val sanitizedEmailToUidMap = allUsers.associate { userEntity ->
                sanitizeEmail(userEntity.email ?: "") to userEntity.uid
            }

// Step 4: Fetch and upsert group details and cross-references for the user's groups
            var successCount = 0
            myGroupsIdList.forEach { groupId ->
                try {
                    val groupRef = db.getReference("groups").child(groupId)
                    val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
                    val groupDetails = groupDetailsSnap.value as? Map<*, *>
                        ?: throw Exception("Invalid GroupDetails for group: $groupId")

                    val name = groupDetails["name"] as? String
                    val isGroupValid = groupDetails["isGroupValid"] as? Boolean ?: true
                    val description = groupDetails["description"] as? String
                    val profilePic = groupDetails["profilePic"] as? String
                    val ownerSanitizedEmail = groupDetails["owner"] as? String // This is the sanitized email
                    val createdAt = groupDetails["createdAt"] as? Long

                    // **Modification 1: Look up the owner's UID using the sanitized email**
                    val ownerUid = ownerSanitizedEmail?.let { sanitizedEmailToUidMap[it] }

                    val groupEntity = GroupEntity(
                        groupID = groupId,
                        name = name,
                        description = description,
                        profilePic = profilePic,
                        isGroupValid = isGroupValid,
                        ownerId = ownerUid, // Assign the looked-up UID here
                        createdAt = createdAt
                    )

                    tourNoteDao.upsertGroup(groupEntity)

                    // Get cross-reference data from Firebase (these are sanitized emails as keys)
                    val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
                    val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }
                    val rawTrackFriendKeys = groupRef.child("TrackFriends").get().await().children.mapNotNull { it.key }

                    Log.d("RoomDBRepository", "Group $groupId - Members: ${rawMemberKeys.size}, Admins: ${rawAdminKeys.size}, TrackFriends: ${rawTrackFriendKeys.size}")

                    // Map sanitized email keys to UIDs
                    val memberCrossRefs = rawMemberKeys.mapNotNull { sanitizedEmail ->
                        sanitizedEmailToUidMap[sanitizedEmail]?.let { uid ->
                            GroupMemberCrossRef(groupId, uid)
                        } ?: run {
                            Log.w("RoomDBRepository", "Could not find UID for member email: $sanitizedEmail in group $groupId")
                            null
                        }
                    }

                    val adminCrossRefs = rawAdminKeys.mapNotNull { sanitizedEmail ->
                        sanitizedEmailToUidMap[sanitizedEmail]?.let { uid ->
                            GroupAdminCrossRef(groupId, uid)
                        } ?: run {
                            Log.w("RoomDBRepository", "Could not find UID for admin email: $sanitizedEmail in group $groupId")
                            null
                        }
                    }

                    val trackFriendCrossRefs = rawTrackFriendKeys.mapNotNull { sanitizedEmail ->
                        sanitizedEmailToUidMap[sanitizedEmail]?.let { uid ->
                            GroupTrackFriendCrossRef(groupId, uid)
                        } ?: run {
                            Log.w("RoomDBRepository", "Could not find UID for trackfriend email: $sanitizedEmail in group $groupId")
                            null
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
                    Log.d("RoomDBRepository", "Successfully synced group: $groupId with ${memberCrossRefs.size} members, ${adminCrossRefs.size} admins, ${trackFriendCrossRefs.size} trackfriends, Owner: $ownerUid")

                } catch (e: Exception) {
                    Log.e("RoomDBRepository", "Error syncing group $groupId: ${e.message}", e)
                    // Continue with other groups instead of failing completely
                }
            }
// ... (rest of the code)

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