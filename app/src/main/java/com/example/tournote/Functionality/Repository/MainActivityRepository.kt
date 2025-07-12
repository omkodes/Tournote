package com.example.tournote.Functionality.Repository

import android.util.Log
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.UserModel
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

class MainActivityRepository {

    val db = Firebase.database

    // Fetches detailed data for a single group
    suspend fun groupData(groupId: String): Result<GroupData_Detailed_Model> {
        return try {
            val groupRef = db.getReference("groups").child(groupId)

            // 1. Fetch GroupDetails
            val groupDetailsSnap = groupRef.child("GroupDetails").get().await()
            val groupDetails = groupDetailsSnap.value as? Map<*, *> ?: return Result.failure(Exception("Invalid GroupDetails"))

            val name = groupDetails["name"] as? String
            val isGroupValid = groupDetails["isGroupValid"] as? Boolean
            val description = groupDetails["description"] as? String
            val profilePic = groupDetails["profilePic"] as? String
            val ownerID = groupDetails["owner"] as? String
            val createdAt = groupDetails["createdAt"] as? Long
            val groupID = groupDetails["groupId"] as? String ?: groupId

            // 2. Get raw member/admin/trackFriends email keys (unsanitized, with `,`)
            val rawMemberKeys = groupRef.child("Members").get().await().children.mapNotNull { it.key }
            val rawAdminKeys = groupRef.child("Admins").get().await().children.mapNotNull { it.key }
            val rawTrackFriendKeys = groupRef.child("TrackFriends").get().await().children.mapNotNull { it.key }


            // 3. Sanitize keys
            val memberEmails = rawMemberKeys.map { it.replace(",", ".") }
            val adminEmails = rawAdminKeys.map { it.replace(",", ".") }
            val ownerEmail = ownerID?.replace(",",".")
            val trackFriendEmails = rawTrackFriendKeys.map { it.replace(",", ".") }


            // 4. Fetch all users and match by email for members, admins, owner, and trackFriends
            val usersSnap = db.getReference("users").get().await()
            val membersList = mutableListOf<UserModel>()
            val adminsList = mutableListOf<UserModel>()
            val trackFriendsList = mutableListOf<UserModel>() // List for track friends
            var owner : UserModel?=null

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email != null) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )

                    if (email in memberEmails) membersList.add(user)
                    if (email in adminEmails) adminsList.add(user)
                    if (email == ownerEmail) owner = user
                    if (email in trackFriendEmails) trackFriendsList.add(user) // Add to trackFriendsList
                }
            }

            val group = GroupData_Detailed_Model(
                groupID = groupID,
                name = name,
                description = description,
                profilePic = profilePic,
                isGroupValid = isGroupValid,
                owner = owner!!,
                createdAt = createdAt,
                members = membersList,
                admins = adminsList,
                trackFriends = trackFriendsList // Assign the list of UserModel directly
            )

            Result.success(group)

        } catch (e: Exception) {
            Log.e("MainActivityRepository", "Error fetching group data for ID $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    // NEW: Fetches all groups the current user is a member of
    suspend fun getAllMyDetailedGroups(): Result<List<GroupData_Detailed_Model>> {
        val myUid = GlobalClass.Me?.uid
        if (myUid == null) {
            Log.e("MainActivityRepository", "GlobalClass.Me.uid is null. Cannot fetch user's groups.")
            return Result.failure(Exception("User not logged in or UID not set."))
        }

        return try {
            val userGroupsRef = db.getReference("users").child(myUid).child("Groups")
            val groupIdsSnap = userGroupsRef.get().await()

            val myGroupIds = groupIdsSnap.children.mapNotNull { it.key }
            Log.d("MainActivityRepository", "Found ${myGroupIds.size} groups for user $myUid")

            val detailedGroups = mutableListOf<GroupData_Detailed_Model>()
            for (groupId in myGroupIds) {
                // Use the existing groupData function to fetch detailed info for each group
                val result = groupData(groupId)
                result.onSuccess { group ->
                    detailedGroups.add(group)
                }.onFailure { e ->
                    Log.w("MainActivityRepository", "Failed to fetch detailed data for group $groupId: ${e.message}")
                    // Optionally, you could decide to fail the entire operation here
                    // return Result.failure(e)
                }
            }
            Log.d("MainActivityRepository", "Successfully fetched ${detailedGroups.size} detailed groups.")
            Result.success(detailedGroups)
        } catch (e: Exception) {
            Log.e("MainActivityRepository", "Error fetching all detailed groups for user $myUid: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun getUserByMailId(emailId: String): Result<UserModel> {
        return try {
            val usersSnap = db.getReference("users").get().await()

            usersSnap.children.forEach { userSnap ->
                val personalDetails = userSnap.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)

                if (email == emailId) {
                    val user = UserModel(
                        uid = userSnap.key,
                        email = email,
                        name = personalDetails.child("name").getValue(String::class.java),
                        phoneNumber = personalDetails.child("phone").getValue(String::class.java),
                        profilePic = personalDetails.child("profilePic").getValue(String::class.java)
                    )
                    return Result.success(user)
                }
            }

            Result.failure(Exception("User with email $emailId not found"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun EnableMyTrackingOnCurrentGroup() {
        try {
            val groupID = GlobalClass.selected_groupId ?: return
            val uID = GlobalClass.Me?.uid ?: return
            val email = GlobalClass.Me?.email?.replace(".",",") ?: return  // prevent null call

            val groupRef = db.getReference("groups").child(groupID).child("TrackFriends")
            val userRef = db.getReference("users").child(uID).child("GroupsTrackingMe")
            groupRef.child(email).setValue(true).await()
            userRef.child(groupID).setValue(true).await()

        } catch (e: Exception) {
            Log.e("EnableTracking", "Error enabling tracking: ${e.message}")
        }
    }

    suspend fun DisableMyTrackingOnCurrentGroup() {
        try {
            val groupID = GlobalClass.selected_groupId ?: return
            val uID = GlobalClass.Me?.uid ?: return
            val email = GlobalClass.Me?.email?.replace(".",",") ?: return

            val groupRef = db.getReference("groups").child(groupID).child("TrackFriends")
            val userRef = db.getReference("users").child(uID).child("GroupsTrackingMe")
            groupRef.child(email).removeValue().await()
            userRef.child(groupID).removeValue().await()

        } catch (e: Exception) {
            Log.e("DisableTracking", "Error disabling tracking: ${e.message}")
        }
    }

    suspend fun LeaveCurrentGroup() {
        val currentUser = GlobalClass.Me
        val currentUserEmail = currentUser?.email
        val currentUserId = currentUser?.uid

        if (currentUserEmail == null || currentUserId == null) {
            println("Error: Current user email or UID is null. Cannot leave group.")
            return
        }

        val grpId = GlobalClass.selected_groupId // Use the globally saved selected group ID

        if (grpId == null) {
            Log.d("LeaveCurrentGroup", "Error: Group ID in GlobalClass.GroupDetails_Everything is null. Cannot leave group.")
            return
        }

        val sanitizedEmail = currentUserEmail.replace(".", ",")

        try {
            val groupRef = db.getReference("groups").child(grpId)
            val userRef = db.getReference("users").child(currentUserId)

            val memberRef = groupRef.child("Members").child(sanitizedEmail)
            memberRef.removeValue().await()

            val adminRef = groupRef.child("Admins").child(sanitizedEmail)
            adminRef.removeValue().await()

            val trackFriendsRef = groupRef.child("TrackFriends").child(sanitizedEmail)
            trackFriendsRef.removeValue().await()

            val userGroupsRef = userRef.child("Groups").child(grpId)
            userGroupsRef.removeValue().await()

            // Update GlobalClass.GroupDetails_Everything locally
            // Find the group that was left and remove it from the list
            val updatedGroupList = GlobalClass.GroupDetails_Everything.filter { it.groupID != grpId }
            GlobalClass.GroupDetails_Everything = updatedGroupList
            Log.d("LeaveCurrentGroup", "Updated GlobalClass.GroupDetails_Everything after leaving group $grpId.")


        } catch (e: Exception) {
            Log.d("LeaveCurrentGroup", "Error leaving group $grpId: ${e.message}")
            throw e
        }
    }

    suspend fun DeleteCurrentGroupFromRoot(){
        val grpId = GlobalClass.selected_groupId // Use the globally saved selected group ID
        // Need to iterate through GlobalClass.GroupDetails_Everything if it's a list
        // Let's assume for this function, GlobalClass.selected_groupId is set for the group to be deleted
        val groupIdToDelete = GlobalClass.selected_groupId

        if (groupIdToDelete == null) {
            Log.d("DeleteCurrentGroupFromRoot", "Error: No group selected for deletion (GlobalClass.selected_groupId is null).")
            return
        }

        try {
            val groupRef = db.getReference("groups").child(groupIdToDelete)

            // Re-fetch group details for accurate member UIDs before deletion
            val groupDetailsResult = groupData(groupIdToDelete)
            if (groupDetailsResult.isFailure) {
                Log.e("DeleteCurrentGroupFromRoot", "Failed to fetch group details for deletion: ${groupDetailsResult.exceptionOrNull()?.message}")
                return
            }
            val groupData = groupDetailsResult.getOrThrow()

            val allGroupMembers = mutableSetOf<String>()
            groupData.owner.uid?.let { allGroupMembers.add(it) }
            groupData.members.forEach { it.uid?.let { uid -> allGroupMembers.add(uid) } }
            groupData.admins.forEach { it.uid?.let { uid -> allGroupMembers.add(uid) } }

            val userGroupsUpdates = mutableMapOf<String, Any?>()
            for (memberUid in allGroupMembers) {
                userGroupsUpdates["users/$memberUid/Groups/$groupIdToDelete"] = null
            }

            if (userGroupsUpdates.isNotEmpty()) {
                db.reference.updateChildren(userGroupsUpdates).await()
                Log.d("DeleteCurrentGroupFromRoot", "Removed group $groupIdToDelete from ${allGroupMembers.size} user's Groups branches.")
            }

            groupRef.removeValue().await()
            Log.d("DeleteCurrentGroupFromRoot", "Deleted group $groupIdToDelete from /groups root.")

            // Update GlobalClass.GroupDetails_Everything by removing the deleted group
            val updatedGroupList = GlobalClass.GroupDetails_Everything.filter { it.groupID != groupIdToDelete }
            GlobalClass.GroupDetails_Everything = updatedGroupList
            Log.d("DeleteCurrentGroupFromRoot", "GlobalClass.GroupDetails_Everything updated after deleting group $groupIdToDelete.")

            // Clear selected_groupId as the group no longer exists
            GlobalClass.selected_groupId = null

        }catch (e: Exception){
            Log.d("DeleteCurrentGroupFromRoot", "Error deleting group $groupIdToDelete: ${e.message}")
            throw e
        }
    }

    suspend fun AddMemberToGroup(UserInfo: UserModel) {
        try {
            // Get group ID from GlobalClass.selected_groupId (assuming it's set for the current context)
            val grpId = GlobalClass.selected_groupId

            if (grpId == null) {
                Log.d("AddMemberToGroup", "Error: GlobalClass.selected_groupId is null. Cannot add member.")
                return
            }

            val userEmail = UserInfo.email
            val userUid = UserInfo.uid

            if (userEmail == null || userUid == null) {
                Log.d("AddMemberToGroup", "Error: User email or UID is null. Cannot add member.")
                return
            }

            val sanitizedEmail = userEmail.replace(".", ",")

            val groupRef = db.getReference("groups").child(grpId)
            val userRef = db.getReference("users").child(userUid)

            val memberRef = groupRef.child("Members").child(sanitizedEmail)
            memberRef.setValue(true).await()

            val userGroupsRef = userRef.child("Groups").child(grpId)
            userGroupsRef.setValue(true).await()

            // --- Update GlobalClass.GroupDetails_Everything locally ---
            // Find the specific group in the list and update its members
            val currentGroups = GlobalClass.GroupDetails_Everything.toMutableList()
            val groupToUpdateIndex = currentGroups.indexOfFirst { it.groupID == grpId }

            if (groupToUpdateIndex != -1) {
                val groupToUpdate = currentGroups[groupToUpdateIndex]
                val currentMembers = groupToUpdate.members.toMutableList()
                val isAlreadyMember = currentMembers.any { it.email == userEmail }

                if (!isAlreadyMember) {
                    currentMembers.add(UserInfo)
                    val updatedGroup = groupToUpdate.copy(members = currentMembers)
                    currentGroups[groupToUpdateIndex] = updatedGroup
                    GlobalClass.GroupDetails_Everything = currentGroups
                    Log.d("AddMemberToGroup", "Successfully added ${UserInfo.name} to group $grpId in GlobalClass.")
                } else {
                    Log.d("AddMemberToGroup", "User ${UserInfo.email} is already a member of group $grpId locally.")
                }
            } else {
                Log.w("AddMemberToGroup", "Group $grpId not found in GlobalClass.GroupDetails_Everything. Local state might be inconsistent.")
            }


        } catch (e: Exception) {
            Log.e("AddMemberToGroup", "Error adding member to group: ${e.message}")
            throw e
        }
    }

    suspend fun EndTour(){
        // Using GlobalClass.selected_groupId if it's meant to be the "current" group
        val grpId = GlobalClass.selected_groupId

        if (grpId == null) {
            Log.d("EndTour", "Error: GlobalClass.selected_groupId is null. Cannot EndTour for the group.")
            return
        }

        try {
            val groupRef = db.getReference("groups").child(grpId)

            groupRef.child("GroupDetails").child("isGroupValid").setValue(false).await()
            groupRef.child("TrackFriends").removeValue().await()

            // --- Update GlobalClass.GroupDetails_Everything locally ---
            val currentGroups = GlobalClass.GroupDetails_Everything.toMutableList()
            val groupToUpdateIndex = currentGroups.indexOfFirst { it.groupID == grpId }

            if (groupToUpdateIndex != -1) {
                val groupToUpdate = currentGroups[groupToUpdateIndex]
                val updatedGroup = groupToUpdate.copy(
                    isGroupValid = false,
                    trackFriends = emptyList() // Clear track friends locally
                )
                currentGroups[groupToUpdateIndex] = updatedGroup
                GlobalClass.GroupDetails_Everything = currentGroups
                Log.d("EndTour", "Group $grpId marked as invalid and track friends cleared in GlobalClass.")
            } else {
                Log.w("EndTour", "Group $grpId not found in GlobalClass.GroupDetails_Everything. Local state might be inconsistent.")
            }


            Log.d("EndTour", "Successfully ended tour for group $grpId.")

        }
        catch (e: Exception){
            Log.d("EndTour", "Error ending tour for group $grpId: ${e.message}")
            throw e
        }

    }

}