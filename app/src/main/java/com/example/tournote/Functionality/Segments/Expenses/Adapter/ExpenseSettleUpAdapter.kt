// File: com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpensesSettleUpAdapter.kt
package com.example.tournote.Functionality.Segments.Expenses.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseRecordPaymentActivity
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SettleUpDisplayItem
import com.example.tournote.R

class ExpensesSettleUpAdapter(
    private val context: Context,
    private var dataList: List<SettleUpDisplayItem>,
    private val recordPaymentLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<ExpensesSettleUpAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfilePic: ImageView = itemView.findViewById(R.id.imgProfilePic)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtDescription: TextView = itemView.findViewById(R.id.txtDescription)
        val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_settleupactivity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]

        // Load profile picture using Glide
        if (!item.memberProfilePicUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(item.memberProfilePicUrl)
                .placeholder(R.drawable.imageselector)
                .into(holder.imgProfilePic)
        } else {
            holder.imgProfilePic.setImageResource(R.drawable.imageselector)
        }

        holder.txtName.text = item.memberName
        holder.txtDescription.text = item.expenseDetails

        // Calculate and display remaining amount
        val remainingAmount = item.shareAmount - (item.partialPayment ?: 0.0)
        holder.txtAmount.text = "₹${"%.2f".format(remainingAmount)}"


        // Use the launcher instead of direct startActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ExpenseRecordPaymentActivity::class.java)
            intent.putExtra("settlingInfo", item)
            recordPaymentLauncher.launch(intent) // ✅ Use launcher for proper callback
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newList: List<SettleUpDisplayItem>) {
        dataList = newList
        notifyDataSetChanged()
    }
}