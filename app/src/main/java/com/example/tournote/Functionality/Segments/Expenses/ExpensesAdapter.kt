package com.example.tournote.Functionality.Segments.Expenses

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.GlobalClass
import com.example.tournote.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Constants to differentiate view types
private const val ITEM_TYPE_EXPENSE = 0
private const val ITEM_TYPE_HEADER = 1

class ExpensesAdapter : ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>(ExpenseListItemDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        // Determine the type of item at this position
        return when (getItem(position)) {
            is ExpenseListItem.ExpenseItem -> ITEM_TYPE_EXPENSE
            is ExpenseListItem.MonthHeader -> ITEM_TYPE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Inflate the correct layout and return the appropriate ViewHolder based on viewType
        return when (viewType) {
            ITEM_TYPE_EXPENSE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
                ExpenseViewHolder(view)
            }
            ITEM_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Bind data based on the item type
        when (holder.itemViewType) {
            ITEM_TYPE_EXPENSE -> {
                val expenseItem = getItem(position) as ExpenseListItem.ExpenseItem
                (holder as ExpenseViewHolder).bind(expenseItem.expense)
            }
            ITEM_TYPE_HEADER -> {
                val headerItem = getItem(position) as ExpenseListItem.MonthHeader
                (holder as MonthHeaderViewHolder).bind(headerItem.monthYear)
            }
        }
    }

    // Existing ViewHolder for individual expense items
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMonth: TextView = itemView.findViewById(R.id.txtMonth)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        private val txtDescription: TextView = itemView.findViewById(R.id.txtDescription)
        private val txtWhoPaidToWhom: TextView = itemView.findViewById(R.id.txtWhoPaidToWhom)

        fun bind(expense: ExpensesDataClass) {
            val timestampLong = expense.timestamp.toLongOrNull()

            if (timestampLong != null) {
                try {
                    val date = Date(timestampLong)
                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

                    txtMonth.text = monthFormat.format(date)
                    txtDate.text = dayFormat.format(date)
                } catch (e: Exception) {
                    txtMonth.text = ""
                    txtDate.text = ""
                    Log.e("ExpensesAdapter", "Error creating Date object from timestamp $timestampLong: ${e.message}")
                }
            } else {
                txtMonth.text = ""
                txtDate.text = ""
                Log.e("ExpensesAdapter", "Timestamp '${expense.timestamp}' is not a valid Long.")
            }

            txtDescription.text = expense.details

            val paidByUid = expense.paidBy
            val currentGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }

            if (GlobalClass.Me?.uid == paidByUid) {
                txtWhoPaidToWhom.text = "You paid to yourself"
            } else {
                val payerName = currentGroup?.members?.find { it.uid == paidByUid }?.name
                txtWhoPaidToWhom.text = "${payerName ?: "Someone"} paid to oneself"
            }

        }
    }

    // NEW: ViewHolder for the Month/Year Header
    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMonthYearHeader: TextView = itemView.findViewById(R.id.txtMonthYearHeader)

        fun bind(monthYear: String) {
            txtMonthYearHeader.text = monthYear
        }
    }

    // DiffUtil for ExpenseListItem, now handling both types
    class ExpenseListItemDiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
        override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            return when {
                oldItem is ExpenseListItem.ExpenseItem && newItem is ExpenseListItem.ExpenseItem ->
                    // Assuming timestamp and details together are unique for an expense
                    oldItem.expense.timestamp == newItem.expense.timestamp && oldItem.expense.details == newItem.expense.details
                oldItem is ExpenseListItem.MonthHeader && newItem is ExpenseListItem.MonthHeader ->
                    oldItem.monthYear == newItem.monthYear
                else -> false // Different types are never the same item
            }
        }

        override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            // Data classes' 'equals' method handles content comparison for ExpenseItem and MonthHeader
            return oldItem == newItem
        }
    }
}