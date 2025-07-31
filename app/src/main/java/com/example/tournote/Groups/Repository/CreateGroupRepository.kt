package com.example.tournote.Groups.Repository

import android.content.Context
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.DataClass.GroupInfoModel
import com.example.tournote.Onboarding.Repository.authRepository
import com.example.tournote.UserModel
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class CreateGroupRepository {
    private val repo = authRepository()
    val realDB = Firebase.database

    suspend fun fetchDetailsOfAllUsers(): Result<List<UserModel>> {
        return try {
            val snapshot = realDB
                .getReference("users")
                .get()
                .await()
            val userList = mutableListOf<UserModel>()

            snapshot.children.forEach { userSnapshot ->
                val uid = userSnapshot.key  // uid is the node key
                val personalDetails = userSnapshot.child("PersonalDetails")
                val email = personalDetails.child("email").getValue(String::class.java)
                val name = personalDetails.child("name").getValue(String::class.java)
                val phoneNumber = personalDetails.child("phoneNumber").getValue(String::class.java)
                val profilePic = personalDetails.child("profilePic").getValue(String::class.java)

                if (uid != null && email != null) {
                    userList.add(
                        UserModel(
                            uid = uid,
                            email = email,
                            name = name!!,
                            phoneNumber = phoneNumber!!,
                            profilePic = profilePic
                        )
                    )
                }
            }

            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDetailsOfGroups(): Result<List<GroupInfoModel>> {
        val uid = repo.getUid()

        return uid?.let {
            try {
                val userSnapshot = realDB
                    .getReference("users")
                    .child(it)
                    .child("Groups")
                    .get()
                    .await()

                val groupIds = userSnapshot.children.mapNotNull { group -> group.key }

                val groupList = mutableListOf<GroupInfoModel>()

                for (groupId in groupIds) {
                    val groupSnapshot = realDB
                        .getReference("groups")
                        .child(groupId)
                        .child("GroupDetails")
                        .get()
                        .await()

                    if (groupSnapshot.exists()) {
                        val name = groupSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Group"
                        val profilePic = groupSnapshot.child("profilePic").getValue(String::class.java) ?: "null"

                        groupList.add(
                            GroupInfoModel(
                                groupid = groupId,
                                name = name,
                                profilePic = profilePic
                            )
                        )
                    }
                }

                Result.success(groupList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } ?: Result.failure(IllegalStateException("User UID is null"))
    }

    suspend fun registerGroup(
        name: String,
        description: String,
        members: List<UserModel>,
        admins: List<UserModel>,
        owner : String,
        groupProfileUrl: String
    ): Result<String> {
        return try {
            val memberEmails = if (members.isNotEmpty()) {
                members.mapNotNull { it.email }
            } else {
                listOf(GlobalClass.Me?.email ?: "unknown@email.com")
            }


            // Generate a unique group ID (Realtime DB)
            val groupId = realDB.getReference("groups").push().key
                ?: return Result.failure(Exception("Failed to generate group ID"))

            // Create group details
            val groupDetails = mapOf(
                "groupId" to groupId,
                "name" to name,
                "description" to description,
                "profilePic" to groupProfileUrl,
                "owner" to sanitizer(owner),
                "createdAt" to System.currentTimeMillis(),
                "isGroupValid" to true
            )

            // Save group details under /groups/{groupId}/GroupDetails
            realDB.getReference("groups")
                .child(groupId)
                .child("GroupDetails")
                .setValue(groupDetails)
                .await()

            // Add members under /groups/{groupId}/Members
            val membersMap = mutableMapOf<String, Boolean>()
            memberEmails.forEach { email ->
                membersMap[sanitizer(email)] = true
            }


            realDB.getReference("groups")
                .child(groupId)
                .child("Members")
                .setValue(membersMap)
                .await()

            if (admins.isNotEmpty()) {
                val adminsMap = admins.mapNotNull { it.email }
                    .associate { sanitizer(it) to true }

                realDB.getReference("groups")
                    .child(groupId)
                    .child("Admins")
                    .setValue(adminsMap)
                    .await()
            }


            // Add groupId to each user's /users/{uid}/Groups/{groupId}: true
            addGroupIdToUsersByEmail(memberEmails, groupId)

            // Create GroupData_Detailed_Model and append to GlobalClass.GroupDetails_Everything
            val newGroupData = GroupData_Detailed_Model(
                groupID = groupId,
                name = name,
                description = description,
                profilePic = groupProfileUrl,
                owner = GlobalClass.Me, // Assuming owner is current user
                createdAt = System.currentTimeMillis(),
                isGroupValid = true,
                members = members,
                admins = admins,
                trackFriends = emptyList()
            )


            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun addGroupIdToUsersByEmail(emails: List<String>, groupId: String) {
        try {
            val usersSnapshot = realDB.getReference("users").get().await()

            for (userSnapshot in usersSnapshot.children) {
                val uid = userSnapshot.key ?: continue
                val userEmail = userSnapshot.child("PersonalDetails").child("email").getValue(String::class.java)

                if (userEmail != null && userEmail in emails) {
                    realDB.getReference("users")
                        .child(uid)
                        .child("Groups")
                        .child(groupId)
                        .setValue(true)
                        .await()
                }
            }
        } catch (e: Exception) {
            println("Error while adding group to users: ${e.message}")
        }
    }

    fun sanitizer(input : String): String{
        return input.replace(".", ",")  // Firebase keys cannot contain '.'
    }

}
