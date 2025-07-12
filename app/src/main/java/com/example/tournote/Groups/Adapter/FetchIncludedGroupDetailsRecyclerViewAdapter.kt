package com.example.tournote.Groups.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Activity.MainActivity
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.DataClass.GroupInfoModel
import com.example.tournote.R
// Removed CoroutineScope and MainActivityRepository as they are no longer needed for direct data fetching here

class FetchIncludedGroupDetailsRecyclerViewAdapter(
    private val context: Context
    // Removed viewModel and coroutineScope as they are not used for direct data fetching anymore
) : RecyclerView.Adapter<FetchIncludedGroupDetailsRecyclerViewAdapter.ViewHolder>() {

    private var groupList: List<GroupInfoModel> = emptyList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val clickable: ConstraintLayout = itemView.findViewById(R.id.itemBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.inclusivegroup_rvitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groupList[position]
        holder.name.text = group.name ?: "Unknown Group"

        // Handle profile picture loading
        if (group.profilePic == "null" || group.profilePic.isNullOrBlank()) {
            holder.profilePhoto.setImageResource(R.drawable.defaultgroupimage)
        } else {
            Glide.with(context)
                .load(group.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(holder.profilePhoto)
        }

        holder.clickable.setOnClickListener {
            // 🔥 MODIFICATION: Just set selected_groupId and start MainActivity
            // The actual detailed group data is already pre-loaded in GlobalClass.GroupDetails_Everything
            GlobalClass.selected_groupId = group.groupid ?: "" // Set the selected group ID
            val intent = Intent(context, MainActivity::class.java)
            // No need to put "GROUP_ID" extra as MainActivity will now get it from GlobalClass.selected_groupId
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = groupList.size

    // Function to update the adapter data
    fun updateGroupList(newGroupList: List<GroupInfoModel>) {
        groupList = newGroupList
        notifyDataSetChanged()
    }
}