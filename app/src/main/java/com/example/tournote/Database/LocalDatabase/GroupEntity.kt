package com.example.tournote.Database.LocalDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tournote.Database.LocalDatabase.UserEntity

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val groupID: String,
    val name: String?,
    val description: String?,
    val profilePic: String?,
    val ownerId: String?, // Store owner's UID
    val createdAt: Long?,
    var isGroupValid: Boolean?
)

// Junction tables for many-to-many relationships
@Entity(tableName = "group_members", primaryKeys = ["groupId", "memberId"])
data class GroupMemberCrossRef(
    val groupId: String,
    val memberId: String
)

@Entity(tableName = "group_admins", primaryKeys = ["groupId", "adminId"])
data class GroupAdminCrossRef(
    val groupId: String,
    val adminId: String
)

@Entity(tableName = "group_track_friends", primaryKeys = ["groupId", "trackFriendId"])
data class GroupTrackFriendCrossRef(
    val groupId: String,
    val trackFriendId: String
)


@Entity(tableName = "group_expenses", primaryKeys = ["groupId", "expenseId"])
data class GroupExpensesCrossRef(
    val groupId: String,
    val expenseId: String
)