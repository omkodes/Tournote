// File: app/src/main/java/com/example/tournote/Functionality/Segments/Expenses/Adapter/FinalDistributionAdapter.kt
package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.R // Ensure this points to your R file
import com.yourpackage.app.models.DistributionItem

class FinalDistributionAdapter(private var items: List<DistributionItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_OWES = 1
        private const val VIEW_TYPE_BORROWED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DistributionItem.Owes -> VIEW_TYPE_OWES
            is DistributionItem.Borrowed -> VIEW_TYPE_BORROWED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_OWES -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_expensefragment_finalresult_owes, parent, false)
                OwesViewHolder(view)
            }
            VIEW_TYPE_BORROWED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_expensefragment_finalresult_borrowed, parent, false)
                BorrowedViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DistributionItem.Owes -> (holder as OwesViewHolder).bind(item)
            is DistributionItem.Borrowed -> (holder as BorrowedViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<DistributionItem>) {
        items = newList
        notifyDataSetChanged() // For simplicity, using notifyDataSetChanged. DiffUtil is better for larger lists.
    }

    // ViewHolder for "Owes" items
    class OwesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)

        fun bind(owesItem: DistributionItem.Owes) {
            txtName.text = "${owesItem.name} owes you "
            txtAmount.text = "₹${"%.2f".format(owesItem.amount)}"
        }
    }

    // ViewHolder for "Borrowed" items
    class BorrowedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)

        fun bind(borrowedItem: DistributionItem.Borrowed) {
            txtName.text = "You owe ${borrowedItem.name} "
            txtAmount.text = "₹${"%.2f".format(borrowedItem.amount)}"
        }
    }
}