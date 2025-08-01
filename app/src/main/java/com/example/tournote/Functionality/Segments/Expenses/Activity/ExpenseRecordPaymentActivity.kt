// File: com.example.tournote.Functionality.Segments.Expenses.Activity.RecordPaymentActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SettleUpDisplayItem
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityRecordPaymentBinding
import kotlinx.coroutines.launch

class ExpenseRecordPaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordPaymentBinding
    private val repo = ExpensesRepository()
    private var settlingData: SettleUpDisplayItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRecordPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settlingData = intent.getParcelableExtra("settlingInfo")

        setupUI()
        setupClickListeners()
        setupWindowInsets()
    }

    private fun setupUI() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)

        settlingData?.let { data ->
            // Load profile images
            Glide.with(this)
                .load(data.memberProfilePicUrl)
                .placeholder(R.drawable.imageselector)
                .into(binding.imgSender)

            Glide.with(this)
                .load(GlobalClass.Me?.profilePic)
                .placeholder(R.drawable.imageselector)
                .into(binding.imgRecever)

            // Set member name
            binding.txtSender.text = data.memberName

            // Set remaining amount to pay
            val remainingAmount = data.shareAmount - (data.partialPayment ?: 0.0)
            binding.txtAmount.setText("%.2f".format(remainingAmount))

            // Show partial payment info if exists
            if ((data.partialPayment ?: 0.0) > 0) {
                // You can add a TextView to show partial payment info
                // binding.txtPartialInfo.text = "Already paid: ₹${"%.2f".format(data.partialPayment)}"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            settlingData?.let { data ->
                val enteredAmount = binding.txtAmount.text.toString().toDoubleOrNull()

                if (enteredAmount == null || enteredAmount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val remainingAmount = data.shareAmount - (data.partialPayment ?: 0.0)

                // Use a small epsilon for floating-point comparison to handle precision issues
                val epsilon = 0.01 // Allow 1 cent tolerance
                if (enteredAmount > remainingAmount + epsilon) {
                    Toast.makeText(this, "Amount cannot exceed remaining balance", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Disable button to prevent multiple clicks
                binding.btnSave.isEnabled = false

                lifecycleScope.launch {
                    try {
                        processPayment(data, enteredAmount)
                        // Set result to OK to trigger refresh in parent activity
                        setResult(RESULT_OK)
                        Toast.makeText(this@ExpenseRecordPaymentActivity, "Payment recorded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ExpenseRecordPaymentActivity, "Failed to record payment: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        // Re-enable button and finish activity regardless of success/failure
                        binding.btnSave.isEnabled = true
                        finish()
                    }
                }
            }
        }

        binding.btnCloseActivity.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private suspend fun processPayment(data: SettleUpDisplayItem, enteredAmount: Double) {
        val currentAmountPaid = data.partialPayment ?: 0.0
        val newAmountPaid = currentAmountPaid + enteredAmount

        // Determine if the share is now fully paid
        val isFullyPaid = newAmountPaid >= data.shareAmount

        // Update the repository
        repo.updateSettling(data.expenseId, data.memberUid, isFullyPaid, newAmountPaid)

        // Update GlobalClass data for immediate UI refresh
        updateGlobalClassData(data.expenseId, data.memberUid, isFullyPaid, newAmountPaid)
    }

    private fun updateGlobalClassData(expenseId: String, memberUid: String, isFullyPaid: Boolean, newAmountPaid: Double) {
        // Find and update the expense in GlobalClass
        GlobalClass.expenses.find { it.expenseId == expenseId }?.let { expense ->
            expense.splitMembers?.find { it.memberUid == memberUid }?.let { memberShare ->
                memberShare.paid = isFullyPaid
                memberShare.partialPayment = newAmountPaid
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}