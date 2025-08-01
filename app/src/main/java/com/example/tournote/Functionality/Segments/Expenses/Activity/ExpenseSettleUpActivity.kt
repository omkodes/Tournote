// File: com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseSettleUpActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseSettleUpBinding
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpensesSettleUpAdapter
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SettleUpDisplayItem

class ExpenseSettleUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseSettleUpBinding
    private lateinit var settleUpAdapter: ExpensesSettleUpAdapter

    // Register for activity result to handle RecordPaymentActivity result
    private val recordPaymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh regardless of result to ensure UI consistency
        loadSettleUpData()

        // Optional: Show different messages based on result
        // if (result.resultCode == RESULT_OK) {
        //     // Payment was recorded successfully
        // } else {
        //     // Payment was canceled
        // }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExpenseSettleUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindow()
        setupClickListeners()
        setupRecyclerView()
        loadSettleUpData()
        setupWindowInsets()
    }

    private fun setupWindow() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.taskbar)
    }

    private fun setupClickListeners() {
        binding.btnCloseActivity.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        settleUpAdapter = ExpensesSettleUpAdapter(this, emptyList(), recordPaymentLauncher)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExpenseSettleUpActivity)
            adapter = settleUpAdapter
        }
    }

    private fun loadSettleUpData() {
        val currentUserId = GlobalClass.Me?.uid
        val groupMembers = GlobalClass.GroupDetails_Everything?.members ?: emptyList()

        if (currentUserId == null) {
            // Handle case where current user is not logged in
            settleUpAdapter.updateData(emptyList())
            return
        }

        val settleUpItems = mutableListOf<SettleUpDisplayItem>()

        // Filter expenses paid by the current user
        GlobalClass.expenses.filter { it.paidBy == currentUserId }
            .forEach { expense ->
                expense.splitMembers?.forEach { memberShare ->
                    // Only include members who have an outstanding balance (not fully paid)
                    val remainingAmount = memberShare.shareAmount - (memberShare.partialPayment ?: 0.0)

                    if (!(memberShare.paid)!! && remainingAmount > 0) {
                        val memberUser = groupMembers.find { it.uid == memberShare.memberUid }

                        settleUpItems.add(
                            SettleUpDisplayItem(
                                memberUid = memberShare.memberUid,
                                memberName = memberUser?.name ?: "Unknown Member",
                                memberProfilePicUrl = memberUser?.profilePic,
                                expenseDetails = expense.details,
                                shareAmount = memberShare.shareAmount,
                                expenseId = expense.expenseId ?: "",
                                partialPayment = memberShare.partialPayment
                            )
                        )
                    }
                }
            }

        // Sort by remaining amount (highest first) for better UX
        settleUpItems.sortByDescending {
            it.shareAmount - (it.partialPayment ?: 0.0)
        }

        settleUpAdapter.updateData(settleUpItems)

        // Optional: Show empty state message
        if (settleUpItems.isEmpty()) {
            // binding.txtEmptyState.visibility = View.VISIBLE
            // binding.recyclerView.visibility = View.GONE
        } else {
            // binding.txtEmptyState.visibility = View.GONE
            // binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when activity resumes (backup mechanism)
        loadSettleUpData()
    }
}