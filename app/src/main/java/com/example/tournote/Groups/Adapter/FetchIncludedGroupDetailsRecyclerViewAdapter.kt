package com.example.tournote.Groups.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Activity.MainActivity
import com.example.tournote.GlobalClass
import com.example.tournote.GroupData_Detailed_Model
import com.example.tournote.R

class FetchIncludedGroupDetailsRecyclerViewAdapter(
    private val context: Context
) : ListAdapter<GroupData_Detailed_Model, FetchIncludedGroupDetailsRecyclerViewAdapter.ViewHolder>(GroupDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val name: TextView = itemView.findViewById(R.id.txtName)
        val clickable: ConstraintLayout = itemView.findViewById(R.id.itemBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_inclusivegroup, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = getItem(position) // Use getItem() from ListAdapter
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
            // Set the selected group ID
            GlobalClass.selected_groupId = group.groupID ?: ""
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    // We don't need getItemCount() or updateGroupList() with this approach.
    // ListAdapter handles getItemCount() automatically.
    // The submitList() method from ListAdapter will replace updateGroupList().
}

// DiffUtil.ItemCallback to calculate differences between two lists
class GroupDiffCallback : DiffUtil.ItemCallback<GroupData_Detailed_Model>() {
    override fun areItemsTheSame(oldItem: GroupData_Detailed_Model, newItem: GroupData_Detailed_Model): Boolean {
        return oldItem.groupID == newItem.groupID
    }

    override fun areContentsTheSame(oldItem: GroupData_Detailed_Model, newItem: GroupData_Detailed_Model): Boolean {
        // Data classes automatically generate a content-based equals() method, which is perfect here.
        return oldItem == newItem
    }
}