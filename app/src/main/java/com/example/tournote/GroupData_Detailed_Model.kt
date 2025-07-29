package com.example.tournote

data class GroupData_Detailed_Model(
    val groupID: String? = null,
    val name: String? = null,
    val description: String? = null,
    val profilePic: String? = null,
    val owner: UserModel?,
    val createdAt: Long? = null,
    var isGroupValid: Boolean? = false,
    val members : List<UserModel>,//no need to pass id...this will be processed info
    val admins : List<UserModel>,//no need to pass id...this will be processed info
    val trackFriends : List<UserModel>
)
