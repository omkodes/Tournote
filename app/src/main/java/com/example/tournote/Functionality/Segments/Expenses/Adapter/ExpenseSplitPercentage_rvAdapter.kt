// File: com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpenseSplitPercentage_rvAdapter.kt
package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.DataClass.UserModelForSplitter
import com.example.tournote.R

// Define an interface for callbacks to the activity for percentage changes
interface OnPercentageChangeListener {
    fun onPercentageChanged(member: UserModelForSplitter, newPercentage: Double)
}

class ExpenseSplitPercentage_rvAdapter(
    private var members: List<UserModelForSplitter>,
    val context: Context,
    private val listener: OnPercentageChangeListener // Add the listener
) : RecyclerView.Adapter<ExpenseSplitPercentage_rvAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expensesplitter_percentage, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.txtMemberName.text = member.name
        Glide.with(context)
            .load(member.profilePic)
            .placeholder(R.drawable.imageselector)
            .error(R.drawable.imageselector)
            .into(holder.imgMemberProfile)

        // Prevent TextWatcher from firing when setting text programmatically
        holder.percentage.removeTextChangedListener(holder.textWatcher)

        // Set the percentage value from the UserModelForSplitter
        // Only set if the value is not 0.0 to avoid showing "0.0" unnecessarily
        if (member.percentage != 0.0) {
            holder.percentage.setText(member.percentage.toString())
        } else {
            holder.percentage.setText("") // Clear the EditText if 0.0
        }

        // Add TextWatcher
        holder.textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                val newPercentage = input.toDoubleOrNull() ?: 0.0

                // Update the member's percentage directly
                member.percentage = newPercentage

                // Notify the activity about the change
                listener.onPercentageChanged(member, newPercentage)
            }
        }
        holder.percentage.addTextChangedListener(holder.textWatcher)

        // Set a hint for the EditText if it's empty
        if (holder.percentage.text.isEmpty()) {
            holder.percentage.hint = "0%"
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

    // You might also need a way to get all current members with their updated percentages
    // This is useful for `calculateAndStorePercentageShares` in the activity
    fun getAllMembersWithPercentages(): List<UserModelForSplitter> {
        return members
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMemberProfile: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val txtMemberName: TextView = itemView.findViewById(R.id.txtName)
        val percentage: EditText = itemView.findViewById(R.id.txtPercentage)

        // To manage TextWatcher efficiently during recycling
        var textWatcher: TextWatcher? = null
    }
}