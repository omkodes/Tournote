package com.example.tournote.Database.LocalDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tournote.UserModel

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String?,
    var name: String?,
    val phoneNumber: String?,
    var profilePic: String?
)

fun UserEntity.toUserModel() = UserModel(
    uid = this.uid,
    email = this.email,
    name = this.name,
    phoneNumber = this.phoneNumber,
    profilePic = this.profilePic
)

fun UserModel.toUserEntity() = this.uid?.let {
    UserEntity(
        uid = it,
        email = this.email,
        name = this.name,
        phoneNumber = this.phoneNumber,
        profilePic = this.profilePic
    )
}

