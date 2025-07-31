package com.example.tournote.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert // Import the new Upsert annotation
import com.example.tournote.Database.LocalDatabase.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TourNoteDao {
    // --- Group Operations ---
    // Change insert to upsert for groups and users for non-destructive updates.
    @Upsert // Use Upsert to either insert or update
    suspend fun upsertGroup(group: GroupEntity)

    @Upsert // Use Upsert for users as well
    suspend fun upsertUsers(users: List<UserEntity>)

    // For cross-reference tables, since they often don't have a simple primary key for upsert,
    // we'll keep the insert methods but add new delete methods to manage them manually.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(crossRefs: List<GroupMemberCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupAdmins(crossRefs: List<GroupAdminCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupTrackFriends(crossRefs: List<GroupTrackFriendCrossRef>)

    @Transaction
    @Query("SELECT * FROM 'groups' WHERE groupID = :groupId")
    fun getGroupWithDetails(groupId: String): Flow<GroupWithMembersAndAdmins?>

    @Transaction
    @Query("SELECT * FROM 'groups'")
    fun getAllGroupsWithDetails(): Flow<List<GroupWithMembersAndAdmins>>

    // --- New Methods for Differential Sync ---

    // 1. Get all group IDs for comparison
    @Query("SELECT groupID FROM 'groups'")
    suspend fun getAllGroupIds(): List<String>

    // 2. Delete groups that a user has left
    @Query("DELETE FROM 'groups' WHERE groupID IN (:groupIds)")
    suspend fun deleteGroupsById(groupIds: List<String>)

    // 3. Delete cross-reference entries for a specific group before re-inserting
    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteGroupMembers(groupId: String)

    @Query("DELETE FROM group_admins WHERE groupId = :groupId")
    suspend fun deleteGroupAdmins(groupId: String)

    @Query("DELETE FROM group_track_friends WHERE groupId = :groupId")
    suspend fun deleteGroupTrackFriends(groupId: String)

    // 4. Get all users for mapping in the repository
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>


    // Helper function to get a Map for efficient lookups
    suspend fun getAllUsersAsMap(): Map<String, UserEntity> {
        return getAllUsers().associateBy { it.email?.replace(".", ",") ?: "" }
    }

    // --- Existing clear methods (can be kept for specific use cases like logout) ---
    @Query("DELETE FROM 'groups'")
    suspend fun clearGroups()

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembers()

    @Query("DELETE FROM group_admins")
    suspend fun clearGroupAdmins()

    @Query("DELETE FROM group_track_friends")
    suspend fun clearGroupTrackFriends()

    @Transaction
    suspend fun clearAllTables() {
        clearGroups()
        clearUsers()
        clearGroupMembers()
        clearGroupAdmins()
        clearGroupTrackFriends()
    }
}