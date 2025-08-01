package com.example.tournote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass

object GlobalClass {
    var Me: UserModel? = null
    var selected_groupId : String?=null
    var GroupDetails_Everything: GroupData_Detailed_Model?=null
    // Initialize with empty list
    var isTracking : Boolean = false
    var isSmsWatched : Boolean = false
    var expenses : List<ExpensesDataClass> = emptyList()

    //var group_id: String ? = null

    // ✅ New: Drive API service for global access (used in UploadWorker)
    var driveService: com.google.api.services.drive.Drive? = null
}
