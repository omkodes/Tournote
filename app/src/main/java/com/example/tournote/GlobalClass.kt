package com.example.tournote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass

object GlobalClass {
    var Me: UserModel? = null
    var selected_groupId : String?=null
    var GroupDetails_Everything: List<GroupData_Detailed_Model> = emptyList() // Initialize with empty list
    var isTracking : Boolean = false
    var expenses : List<ExpensesDataClass> = emptyList()

    //var group_id: String ? = null
}
