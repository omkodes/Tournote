package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.content.Intent // Import Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseAddActivity // Import ExpenseAddActivity
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.Activity.GettingStartedActivity
import com.example.tournote.Onboarding.Repository.authRepository
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import kotlinx.coroutines.launch
import kotlin.getValue

class SmsGroupSelectionActivity : AppCompatActivity() {

    private val authViewModel: authViewModel by viewModels()
    val repo = MainActivityRepository()

    private val mainRepo = MainActivityRepository()
    private lateinit var bar: ProgressBar
    private var selectedGroup: String = "" // Keep this if still used for other purposes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sms_group_selection)

        // Retrieve amount and description passed from the launching intent (now from confirmation notification tap)
        val amount = intent.getStringExtra("amount")
        val description = intent.getStringExtra("description")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bar = findViewById(R.id.progressBar)

        // Show a temporary toast to confirm details received
        Toast.makeText(this, "Expense Details: Amount = $amount, Description = $description", Toast.LENGTH_LONG).show()


        if (authViewModel.repo.getuser() != null) {
            val email = authViewModel.repo.getuser()
            if (email != null) {
                // Logged-in user: Start fetching user and group data
                // Wait indefinitely until all data is loaded
                lifecycleScope.launch {
                    val userResult = repo.getUserByMailId(email)
                    userResult.onSuccess { user ->
                        GlobalClass.Me = user
                        Log.d("SmsGroupSelectionActivity", "Current user (GlobalClass.Me) set: ${user.name}")
                        showGroupSelectionDialog(amount, description) // Pass amount and description to the dialog
                    }.onFailure { e ->
                        Log.e("SmsGroupSelectionActivity", "Failed to fetch current user data: ${e.message}")
                        showGroupSelectionDialog(amount, description) // Pass amount and description to the dialog
                    }
                }

                // No fallback timer - wait until data loading is complete
                Log.d("SmsGroupSelectionActivity", "Waiting for all group data to load completely...")

            } else {
                Log.d("SmsGroupSelectionActivity", "User email is null, redirecting to GettingStartedActivity.")
            }
        } else {
            Log.d("SmsGroupSelectionActivity", "No user found, redirecting to GettingStartedActivity.")
        }

    }

    private fun showGroupSelectionDialog(amount: String?, description: String?) {
        lifecycleScope.launch {
            bar.visibility = View.VISIBLE
            val response = mainRepo.getAllMyDetailedGroups()
            bar.visibility = View.GONE

            val groups = response.getOrNull()

            if (response.isSuccess && !groups.isNullOrEmpty()) {
                // ✅ Filter groups where isGroupValid == true
                val validGroups = groups.filter { it.isGroupValid == true }

                if (validGroups.isEmpty()) {
                    Toast.makeText(this@SmsGroupSelectionActivity, "No valid groups available. Please create a valid group first.", Toast.LENGTH_LONG).show()
                    finish()
                }

                val groupNames = validGroups.map { it.name ?: "Unnamed Group" }.toTypedArray()
                val groupIds = validGroups.map { it.groupID ?: "Unknown ID" }.toTypedArray()

                AlertDialog.Builder(this@SmsGroupSelectionActivity)
                    .setTitle("Select Group")
                    .setItems(groupNames) { _, which ->
                        selectedGroup = groupNames[which]
                        val selectedGroupId = groupIds[which]

                        Toast.makeText(this@SmsGroupSelectionActivity, "Selected Group: ${groupNames[which]}, Group ID: $selectedGroupId", Toast.LENGTH_SHORT).show()

                        GlobalClass.selected_groupId = selectedGroupId

                        val intent = Intent(this@SmsGroupSelectionActivity, ExpenseAddActivity::class.java).apply {
                            putExtra(ExpenseAddActivity.EXTRA_AMOUNT, amount)
                            putExtra(ExpenseAddActivity.EXTRA_DESCRIPTION, description)
                            putExtra(ExpenseAddActivity.EXTRA_IS_AUTO_DETECTED, true)
                        }
                        startActivity(intent)
                        finish()
                    }
                    .setCancelable(false)
                    .show()

            } else {
                Toast.makeText(this@SmsGroupSelectionActivity, "No groups available or error fetching. Please create a group first.", Toast.LENGTH_LONG).show()
                finish()
            }

        }
    }
}