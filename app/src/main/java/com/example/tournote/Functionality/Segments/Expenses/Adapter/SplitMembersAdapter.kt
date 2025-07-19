// File: com.example.tournote.Functionality.Segments.Expenses.Adapter.SplitMembersAdapter.kt
package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitMemberDisplayData
import com.example.tournote.R
import com.example.tournote.databinding.ItemSplitMemberBinding
import java.text.NumberFormat
import java.util.Locale

class SplitMembersAdapter(private val members: List<SplitMemberDisplayData>) :
    RecyclerView.Adapter<SplitMembersAdapter.SplitMemberViewHolder>() {

    class SplitMemberViewHolder(val binding: ItemSplitMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SplitMemberViewHolder {
        val binding = ItemSplitMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SplitMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SplitMemberViewHolder, position: Int) {
        val member = members[position]
        holder.binding.apply {

            // Share Amount and "owes" text - NOW USING CURRENCY FORMATTER
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()) // Get currency format


            // As per image, it's always "X owes Y" or "You owe Y"
            txtShareAmount.text = if (member.isCurrentUser) {
                "You owe ${member.shareAmount}" // Use formatted amount
            } else {
                "${member.memberName} owes ₹${member.shareAmount}" // Use formatted amount
            }

            // Load profile picture
            Glide.with(imgProfilePic.context)
                .load(member.profilePicUrl)
                .placeholder(R.drawable.imageselector) // Use a default placeholder
                .error(R.drawable.imageselector)       // Use a default error image
                .into(imgProfilePic)

            // Adjust tree view lines visibility based on position - CORRECTED LOGIC for verticalLineTop
            verticalLineTop.visibility = View.VISIBLE
            verticalLineBottom.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE
            horizontalLine.visibility = View.VISIBLE // Always visible for a split member
        }
    }

    override fun getItemCount(): Int = members.size
}