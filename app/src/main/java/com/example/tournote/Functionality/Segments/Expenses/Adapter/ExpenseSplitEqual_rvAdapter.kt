package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.DataClass.UserModelForSplitter
import com.example.tournote.UserModel
import com.example.tournote.R

// Define an interface for callbacks to the activity
interface OnMemberSelectionChangeListener {
    fun onMemberSelectionChanged(member: UserModelForSplitter, isSelected: Boolean)
    fun onAllMembersToggled(areAllSelected: Boolean) // Callback for when all are toggled
}

class ExpenseSplitEqual_rvAdapter(
    private var members: List<UserModelForSplitter>,
    private val context: Context,
    private val listener: OnMemberSelectionChangeListener // Add listener here
) : RecyclerView.Adapter<ExpenseSplitEqual_rvAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expensesplitter_equal, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.txtMemberName.text = member.name
        Glide.with(context)
            .load(member.profilePic)
            .placeholder(R.drawable.imageselector) // Placeholder image if URL is null or loading
            .error(R.drawable.imageselector) // Error image if loading fails
            .into(holder.imgMemberProfile)

        // Set the tick state based on the member's isSelected property
        if (member.isSelected) {
            holder.tick.setImageResource(R.drawable.tick)
        } else {
            holder.tick.setImageResource(R.drawable.untick)
        }

        holder.body.setOnClickListener {
            // Toggle the isSelected state for this member
            member.isSelected = !member.isSelected
            if (member.isSelected) {
                holder.tick.setImageResource(R.drawable.tick)
            } else {
                holder.tick.setImageResource(R.drawable.untick)
            }
            // Notify the activity about the change for this specific member
            listener.onMemberSelectionChanged(member, member.isSelected)

            // Check if all are now selected/deselected after this individual click
            val allCurrentlySelected = members.all { it.isSelected }
            val allCurrentlyDeselected = members.none { it.isSelected }

            if (allCurrentlySelected) {
                listener.onAllMembersToggled(true)
            } else if (allCurrentlyDeselected) {
                listener.onAllMembersToggled(false)
            }
        }
    }

    override fun getItemCount(): Int {
        return members.size
    }

    // Function to update the data in the adapter
    fun updateMembers(newMembers: List<UserModelForSplitter>) {
        this.members = newMembers
        notifyDataSetChanged() // Notifies the RecyclerView that the data has changed
    }

    // New function to update the selection state of all members
    fun setAllMembersSelected(isSelected: Boolean) {
        members.forEach { it.isSelected = isSelected }
        notifyDataSetChanged() // Refresh the RecyclerView to reflect changes
    }

    // New function to get currently selected members
    fun getSelectedMembers(): List<UserModelForSplitter> {
        return members.filter { it.isSelected }
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMemberProfile: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val txtMemberName: TextView = itemView.findViewById(R.id.txtName)
        val tick: ImageView = itemView.findViewById(R.id .imgtick)
        val body: View = itemView.findViewById(R.id.itemBody)
    }
}