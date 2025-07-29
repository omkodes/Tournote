package com.example.tournote.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.tournote.DatabaseCatching.UserEntity
import com.example.tournote.DatabaseCatching.toUserModel
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.UserModel

data class GroupWithMembersAndAdmins(
    @Embedded
    val group: GroupEntity,
    @Relation(
        parentColumn = "groupID",
        entityColumn = "uid",
        associateBy = Junction(GroupMemberCrossRef::class, parentColumn = "groupId", entityColumn = "memberId")
    )
    val members: List<UserEntity>,
    @Relation(
        parentColumn = "groupID",
        entityColumn = "uid",
        associateBy = Junction(GroupAdminCrossRef::class, parentColumn = "groupId", entityColumn = "adminId")
    )
    val admins: List<UserEntity>,
    @Relation(
        parentColumn = "groupID",
        entityColumn = "uid",
        associateBy = Junction(GroupTrackFriendCrossRef::class, parentColumn = "groupId", entityColumn = "trackFriendId")
    )
    val trackFriends: List<UserEntity>,
    @Relation(
        parentColumn = "ownerId",
        entityColumn = "uid"
    )
    val owner: UserEntity?
)

fun GroupWithMembersAndAdmins.toDetailedModel(): GroupData_Detailed_Model {
    return GroupData_Detailed_Model(
        groupID = this.group.groupID,
        name = this.group.name,
        description = this.group.description,
        profilePic = this.group.profilePic,
        isGroupValid = this.group.isGroupValid,
        createdAt = this.group.createdAt,
        owner = this.owner?.toUserModel(),
        members = this.members.map { it.toUserModel() },
        admins = this.admins.map { it.toUserModel() },
        trackFriends = this.trackFriends.map { it.toUserModel() }
    )
}