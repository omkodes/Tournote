package com.example.tournote.DatabaseCatching

import android.util.Log
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.UserModel
import com.example.tournote.database.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val db: FirebaseDatabase,
    private val tourNoteDao: TourNoteDao,
    private val sharedViewModel: GroupSelectorActivityViewModel2
) {
    suspend fun getGroups(): Flow<List<GroupData_Detailed_Model>> {
        return tourNoteDao.getAllGroupsWithDetails().map { groupWithDetailsList ->
            groupWithDetailsList.map { it.toDetailedModel() }
        }
    }

    suspend fun refreshGroupsFromNetwork() {
        try {
            // No Toast for silent updates unless it's an error.
            // sharedViewModel.showToast("Syncing with cloud...")

            val myUid = GlobalClass.Me?.uid ?: run {
                sharedViewModel.showError("User not logged in.")
                return
            }

            val myGroupsIdList = db.getReference("users")
                .child(myUid)
                .child("Groups")
                .get()
                .await()
                .children
                .mapNotNull { it.key }

            // Step 1: Delete groups that the user has left
            val localGroupIds = tourNoteDao.getAllGroupIds()
            val groupsToDelete = localGroupIds.filter { it !in myGroupsIdList }
            if (groupsToDelete.isNotEmpty()) {
                tourNoteDao.deleteGroupsById(groupsToDelete)
            }

            if (myGroupsIdList.isEmpty()) {
                // If there are no groups, and we've already deleted any old ones, we're done.
                return
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
            tourNoteDao.upsertUsers(allUsers)

            // Step 3: Fetch and upsert group details and cross-references for the user's groups
            myGroupsIdList.forEach { groupId ->
                val groupRef = db.getReference("groups").child(groupId)
                val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
                val groupDetails = groupDetailsSnap.value as? Map<*, *> ?: throw Exception("Invalid GroupDetails for group: $groupId")

                val name = groupDetails["name"] as? String
                val isGroupValid = groupDetails["isGroupValid"] as? Boolean
                val description = groupDetails["isGroupValid"] as? String
                val profilePic = groupDetails["profilePic"] as? String
                val ownerID = groupDetails["owner"] as? String
                val createdAt = groupDetails["createdAt"] as? Long

                val groupEntity = GroupEntity(
                    groupID = groupId,
                    name = name,
                    description = description,
                    profilePic = profilePic,
                    isGroupValid = isGroupValid,
                    ownerId = ownerID, // Owner ID is nullable, as per previous fix
                    createdAt = createdAt
                )

                // 🔥 Use upsert for the group entity.
                tourNoteDao.upsertGroup(groupEntity)

                // Get cross-reference data from Firebase
                val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
                val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }
                val rawTrackFriendKeys = groupRef.child("TrackFriends").get().await().children.mapNotNull { it.key }

                // Get all users from the database for mapping
                val allUsersMap = tourNoteDao.getAllUsersAsMap() // New DAO function to get a Map for efficiency

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

                // 🔥 Instead of clearing and re-inserting, we'll delete and re-insert the cross-refs.
                // It's safer for `N:M` relationships to manage them this way.
                tourNoteDao.deleteGroupMembers(groupId)
                tourNoteDao.insertGroupMembers(memberCrossRefs)
                tourNoteDao.deleteGroupAdmins(groupId)
                tourNoteDao.insertGroupAdmins(adminCrossRefs)
                tourNoteDao.deleteGroupTrackFriends(groupId)
                tourNoteDao.insertGroupTrackFriends(trackFriendCrossRefs)
            }
            sharedViewModel.showToast("Group data synced successfully!")

        } catch (e: Exception) {
            Log.e("GroupRepository", "Error fetching group data: ${e.message}", e)
            sharedViewModel.showError("Failed to fetch group data. ${e.message}")
        }
    }
}