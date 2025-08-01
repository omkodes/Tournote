package com.example.tournote.Groups.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log // Added for logging
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.Groups.Adapter.grpAdminsAdapter
import com.example.tournote.Groups.Adapter.grpMemberAdapter
import com.example.tournote.Groups.Adapter.grpOwnerAdapter
import com.example.tournote.Functionality.AddUsersToGroup_GroupInfoRecyclerViewAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.groupViewModel
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.R
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityGroupInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.getValue // Keep if used for something else, not directly for this change

class activityGroupInfo : AppCompatActivity() {
    private lateinit var binding: ActivityGroupInfoBinding
    private var MemberList = listOf<String>() // Consider if this is still needed or can be derived directly
    private val viewModel: groupViewModel by viewModels()

    private val mainRepo = FirebaseRTDBRepository()
    private lateinit var adapter: AddUsersToGroup_GroupInfoRecyclerViewAdapter

    private var usersIn: List<UserModel> = listOf()

    private var initialAddMembersClick = false

    private val viewModel1: GroupSelectorActivityViewModel by viewModels() // For user list and adding members
    private val viewModel2: MainActivityViewModel by viewModels() // For setting group validity (turnOffGroupValidity)

    // 🔥 NEW: Store the currently active group's detailed data
    private var currentDetailedGroup: GroupData_Detailed_Model? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGroupInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)

        // 🔥 MODIFICATION: Retrieve the selected group's detailed data at the start
        val selectedGroupId = GlobalClass.selected_groupId
        if (selectedGroupId == null) {
            Log.e("ActivityGroupInfo", "GlobalClass.selected_groupId is null. Cannot load group info.")
            finish() // Or redirect to GroupSelectorActivity
            return
        }

        currentDetailedGroup = GlobalClass.GroupDetails_Everything

        if (currentDetailedGroup == null) {
            Log.e("ActivityGroupInfo", "No detailed group found for ID: $selectedGroupId. Navigating back.")
            // This case indicates an inconsistency, perhaps group was deleted or not loaded.
            redirectToActivity(GroupSelectorActivity::class.java) // Redirect to avoid errors
            return
        }

        // Use 'currentDetailedGroup' for all subsequent group data accesses
        val grpData = currentDetailedGroup!! // Non-null after checks
        val currentUser = GlobalClass.Me // Local immutable copy for smart cast

        // 🔥 MODIFIED: Added a null check for grpData.owner
        if ((grpData.owner?.uid == currentUser?.uid) || (grpData.admins.contains(currentUser))) {
            binding.btnAddMembers.visibility = View.VISIBLE
        } else {
            binding.btnAddMembers.visibility = View.GONE
        }

        val userId = viewModel.authrepo.getUid() // Ensure this is still needed or remove if unused

        // 🔥 MODIFIED: Added a null check for grpData.owner
        if (grpData.owner?.uid == currentUser?.uid) {
            binding.btnDeleteGroup.visibility = View.VISIBLE
            binding.btnLeaveGroup.visibility = View.GONE
            if (grpData.isGroupValid == true) { // Use grpData
                binding.btnEndTrip.visibility = View.VISIBLE
            } else {
                binding.btnEndTrip.visibility = View.GONE
            }

            binding.btnDeleteGroup.setOnClickListener {
                showDeleteGrpConfirmationBsFragment { res ->
                    if (res) {
                        lifecycleScope.launch {
                            binding.progressBar.visibility = View.VISIBLE
                            mainRepo.DeleteCurrentGroupFromRoot() // This repo function already uses GlobalClass.selected_groupId
                            redirectToActivity(GroupSelectorActivity::class.java)
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        } else {
            binding.btnDeleteGroup.visibility = View.GONE
            binding.btnLeaveGroup.visibility = View.VISIBLE
            binding.btnEndTrip.visibility = View.GONE
        }


        val currentUserEmail = currentUser?.email // Ensure currentUserEmail is derived from the local copy

        // 🔥 MODIFICATION: Check if current user is tracked in the specific 'grpData'
        val isTracked = (currentUserEmail != null &&
                grpData.trackFriends.any { it.email == currentUser.email })


        binding.btnAddMembers.setOnClickListener {
            if (!initialAddMembersClick) {
                binding.relLayoutMemberSelector.visibility = View.VISIBLE
                initialAddMembersClick = true
            } else {
                //final click before adding members
                binding.relLayoutMemberSelector.visibility = View.GONE
                if (usersIn.isNotEmpty()) {
                    lifecycleScope.launch { // Launch a single coroutine for the entire process
                        binding.progressBar.visibility = View.VISIBLE // Show progress bar

                        val addMemberJobs = usersIn.map { user ->
                            async { // Use async to run additions concurrently and get a Deferred object
                                mainRepo.AddMemberToGroup(user) // This repo function already uses GlobalClass.selected_groupId
                            }
                        }
                        addMemberJobs.awaitAll() // Wait for all member additions to complete

                        // Re-fetch the current group's details to reflect added members
                        // This ensures 'grpData' reflects the latest state after adding members
                        val updatedGroup = GlobalClass.GroupDetails_Everything
                        if (updatedGroup != null) {
                            setupMembersList(updatedGroup) // Use the updated group data
                        } else {
                            Log.e("ActivityGroupInfo", "Error: Updated group data not found after adding members.")
                        }

                        binding.progressBar.visibility = View.GONE // Hide progress bar

                        usersIn = listOf() // Clear the list

                        initialAddMembersClick = false

                        viewModel1.fetchAllActiveUsers() // Refresh list of active users if needed
                    }
                }
            }
        }


        binding.btnLeaveGroup.setOnClickListener {
            lifecycleScope.launch {
                mainRepo.LeaveCurrentGroup() // This repo function already uses GlobalClass.selected_groupId
                redirectToActivity(GroupSelectorActivity::class.java)
            }
        }

        // 🔥 MODIFICATION: Check for tracking based on 'grpData'
        // 🔥 Also added a null check for owner
        if (isTracked && currentUser?.uid != grpData.owner?.uid) {
            binding.btnDisableTracking.visibility = View.VISIBLE

            binding.btnDisableTracking.setOnClickListener {
                lifecycleScope.launch {
                    mainRepo.DisableMyTrackingOnCurrentGroup() // This updates Firebase
                    binding.btnDisableTracking.visibility = View.GONE

                    // 🔥 MODIFICATION: Update GlobalClass.GroupDetails_Everything locally
                    val currentUserEmail = currentUser?.email // Ensure currentUserEmail is accessible here

                    // Directly access the single GroupData_Detailed_Model from GlobalClass
                    GlobalClass.GroupDetails_Everything?.let { currentGroup ->
                        // Check if this is the correct group we're trying to update
                        if (currentGroup.groupID == grpData.groupID && currentUserEmail != null) {
                            val currentTrackFriends = currentGroup.trackFriends.toMutableList()
                            val userToRemove = currentTrackFriends.find { it.email == currentUserEmail }

                            if (userToRemove != null) {
                                if (currentTrackFriends.remove(userToRemove)) {
                                    val updatedGroup = currentGroup.copy(trackFriends = currentTrackFriends)
                                    GlobalClass.GroupDetails_Everything = updatedGroup // Update the single global object
                                    Log.d("DisableTracking", "Successfully removed ${currentUserEmail} from trackFriends in GlobalClass for group ${currentGroup.groupID}.")
                                }
                            } else {
                                Log.d("DisableTracking", "User ${currentUserEmail} not found in trackFriends for group ${currentGroup.groupID} locally.")
                            }
                        } else {
                            Log.w("DisableTracking", "GlobalClass.GroupDetails_Everything is null or holds a different group (${currentGroup.groupID}) than the one being updated (${grpData.groupID}). Local state might be inconsistent.")
                        }
                    } ?: run {
                        Log.w("DisableTracking", "GlobalClass.GroupDetails_Everything is null. Cannot update local tracking state.")
                    }
                }
            }
        } else {
            binding.btnDisableTracking.visibility = View.GONE
        }


        // 🔥 MODIFICATION: Use 'grpData' for UI population
        binding.txtGroupName.text = grpData.name
        binding.txtGroupDescription.text = grpData.description
        binding.createdAtDate.text = viewModel.formatTimestamp(grpData.createdAt!!)
        Glide.with(this)
            .load(grpData.profilePic)
            .placeholder(R.drawable.defaultgroupimage)
            .error(R.drawable.defaultgroupimage)
            .into(binding.grpProfileImage)

        setupMembersList(grpData) // Use grpData

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEndTrip.setOnClickListener {
            showEndTripConfirmationBsFragment { res ->
                if (res) {
                    lifecycleScope.launch {
                        binding.btnEndTrip.visibility = View.GONE
                        mainRepo.EndTour() // This repo function already uses GlobalClass.selected_groupId
                        viewModel2.turnOffGroupValidity() // This affects the ViewModel's LiveData

                        // 🔥 Update the local GlobalClass.GroupDetails_Everything's
                        // isGroupValid and trackFriends after EndTour to reflect it immediately

                        // Directly access the single GroupData_Detailed_Model from GlobalClass
                        GlobalClass.GroupDetails_Everything?.let { currentGroup ->
                            // Ensure the currently loaded group in GlobalClass matches the one being ended
                            if (currentGroup.groupID == GlobalClass.selected_groupId) {
                                // Create an updated copy of the group with the new validity and empty trackFriends
                                val updatedGroup = currentGroup.copy(
                                    isGroupValid = false,
                                    trackFriends = emptyList() // Assuming EndTour clears this
                                )
                                // Assign the updated group back to the global object
                                GlobalClass.GroupDetails_Everything = updatedGroup
                                Log.d("EndTrip", "Successfully updated GlobalClass.GroupDetails_Everything for group ${currentGroup.groupID}: isGroupValid=false, trackFriends cleared.")
                            } else {
                                Log.w("EndTrip", "GlobalClass.GroupDetails_Everything holds a different group (${currentGroup.groupID}) than the selected one (${GlobalClass.selected_groupId}). Local state might be inconsistent.")
                            }
                        } ?: run {
                            Log.w("EndTrip", "GlobalClass.GroupDetails_Everything is null. Cannot update local state after trip end.")
                        }
                    }
                }
            }
        }


        // Setup user lists for adding members
        binding.rvActiveUserList.layoutManager = LinearLayoutManager(this)

        viewModel1.fetchAllActiveUsers()

        viewModel1.users.observe(this) { allUsers ->
            // 🔥 MODIFICATION: Filter based on members of the 'currentDetailedGroup'
            val currentMemberIds = grpData.members.map { it.uid }.toSet()

            // Filter 'allUsers' by checking if their ID is NOT in the 'currentMemberIds' Set.
            val nonMemberUsers = allUsers.filter { user ->
                !currentMemberIds.contains(user.uid)
            }

            // Pass the filtered list to the adapter
            adapter = AddUsersToGroup_GroupInfoRecyclerViewAdapter(this, nonMemberUsers, viewModel1)
            binding.rvActiveUserList.adapter = adapter

            binding.txtSearch.addTextChangedListener { editable ->
                val query = editable?.toString() ?: ""
                adapter.filter(query)
            }
        }

        viewModel1.usersIn.observe(this) { users ->
            usersIn = users
        }
    }

    private fun showEndTripConfirmationBsFragment(onResult: (Boolean) -> Unit) {
        val dialog = BottomSheetDialog(this).apply {
            setContentView(R.layout.bsfragment_endtrack_confirmation)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnConfirm)

        btnConfirm?.setOnClickListener {
            dialog.dismiss()
            onResult(true) // ✅ Button was clicked
        }

        dialog.setOnDismissListener {
            onResult(false) // ❌ Dialog dismissed without button click
        }

        dialog.show()
    }

    private fun showDeleteGrpConfirmationBsFragment(onResult: (Boolean) -> Unit) {
        val dialog = BottomSheetDialog(this).apply {
            setContentView(R.layout.bsfragment_deletegrp_confirmation)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnConfirm)

        btnConfirm?.setOnClickListener {
            dialog.dismiss()
            onResult(true) // ✅ Button was clicked
        }

        dialog.setOnDismissListener {
            onResult(false) // ❌ Dialog dismissed without button click
        }

        dialog.show()
    }


    private fun setupMembersList(grpData: GroupData_Detailed_Model){ // Parameter is now a single GroupData_Detailed_Model
        val memberList = grpData.members
        val adminList = grpData.admins
        // 🔥 MODIFIED: owner is now nullable, so handle it
        val owner = grpData.owner
        val ownerList = if (owner != null) listOf(owner) else emptyList() // Safe null check
        val allExcludedIds = (ownerList + adminList).map { it.uid }.toSet()
        val filteredMemberList = memberList.filterNot { it.uid in allExcludedIds }

        val ownerAdapter = grpOwnerAdapter(ownerList.toMutableList(), this)
        val adminAdapter = grpAdminsAdapter(adminList.toMutableList(), this)
        val memberAdapter = grpMemberAdapter(filteredMemberList.toMutableList(), this)

        val concatAdapter = ConcatAdapter(ownerAdapter, adminAdapter, memberAdapter)
        binding.rvMembersList.adapter = concatAdapter
        binding.rvMembersList.layoutManager = LinearLayoutManager(this)

        binding.rvMembersList.post {
            binding.txtMemberCount.text = concatAdapter.itemCount.toString()
        }
    }


    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        // Clear back stack to prevent user from returning to previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}