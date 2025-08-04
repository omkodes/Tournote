package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.content.Intent
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
import androidx.lifecycle.Observer // Import Observer
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseAddActivity
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseSettleUpActivity
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2 // Import your ViewModel
import com.example.tournote.Onboarding.Activity.GettingStartedActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.GroupData_Detailed_Model // Import your data model if not already
import kotlinx.coroutines.launch
// import kotlin.getValue // This import is usually not needed for `by viewModels()`

class SmsGroupSelectionActivity : AppCompatActivity() {

    private val authViewModel: authViewModel by viewModels()
    // You have two MainActivityRepository instances here. It's generally better
    // to have them injected or managed by ViewModels. For minimalistic changes,
    // we'll primarily stop using `mainRepo` for group fetching.
    val repo = FirebaseRTDBRepository() // Used for getUserByMailId
    // private val mainRepo = MainActivityRepository() // This instance won't be used for group fetching anymore.

    // Declare your GroupSelectorActivityViewModel2
    private val groupSelectionViewModel: GroupSelectorActivityViewModel2 by viewModels() // Renamed for clarity from `viewModel`

    private lateinit var bar: ProgressBar
    private var selectedGroup: String = ""

    private val expensesRepository = ExpensesRepository()

    // Flags to manage dialog display to ensure it shows only once when data is ready
    private var isUserDataLoaded = false
    private var isGroupsDataReady = false
    private var isDialogShowing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sms_group_selection)

        val amount = intent.getStringExtra("amount")
        val description = intent.getStringExtra("description")
        val functionality = intent.getStringExtra("functionality")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bar = findViewById(R.id.progressBar)
        Toast.makeText(this, "Expense Details: Amount = $amount, Description = $description", Toast.LENGTH_LONG).show()

        // --- Start of changes to observe ViewModel data ---

        // Observe ViewModel's loading state
        groupSelectionViewModel.isLoading.observe(this, Observer { isLoading ->
            bar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        // Observe ViewModel's error messages
        groupSelectionViewModel.error.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (!isDialogShowing) finish() // Optionally finish if there's a critical error preventing group selection
            }
        })

        // Observe ViewModel's toast messages (if you want to use this mechanism)
        groupSelectionViewModel.toastmsg.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        })


        // Observe the groups LiveData from the ViewModel
        groupSelectionViewModel.groups.observe(this, Observer { groups ->
            Log.d("SmsGroupSelectionActivity", "Groups LiveData updated: ${groups?.size} groups. User data loaded: $isUserDataLoaded. Dialog showing: $isDialogShowing")
            isGroupsDataReady = !groups.isNullOrEmpty() // Set flag if groups are available

            // Attempt to show dialog only if user data is loaded, groups are ready, and dialog is not already showing
            tryShowGroupSelectionDialog(amount, description,functionality)
        })

        // --- End of changes to observe ViewModel data ---


        if (authViewModel.repo.getuser() != null) {
            val email = authViewModel.repo.getuser()
            if (email != null) {
                lifecycleScope.launch {
                    // Assuming 'repo' (MainActivityRepository) can be instantiated or is already set up correctly
                    // for fetching user data. If its constructor changed, you might need to pass `application`.
                    // val userResult = MainActivityRepository(application).getUserByMailId(email) // Example if constructor changed
                    val userResult = repo.getUserByMailId(email) // Using existing `repo` instance

                    userResult.onSuccess { user ->
                        GlobalClass.Me = user
                        isUserDataLoaded = true // Set flag that user data is loaded
                        Log.d("SmsGroupSelectionActivity", "Current user (GlobalClass.Me) set: ${user.name}")
                        // Attempt to show dialog now that user data is ready
                        tryShowGroupSelectionDialog(amount, description,functionality)

                    }.onFailure { e ->
                        Log.e("SmsGroupSelectionActivity", "Failed to fetch current user data: ${e.message}")
                        Toast.makeText(this@SmsGroupSelectionActivity, "Failed to load user data. Please try again.", Toast.LENGTH_LONG).show()
                        finish() // Critical failure, finish activity
                    }
                }
                Log.d("SmsGroupSelectionActivity", "Waiting for all group data to load completely...")

            } else {
                Log.d("SmsGroupSelectionActivity", "User email is null, redirecting to GettingStartedActivity.")
                startActivity(Intent(this, GettingStartedActivity::class.java))
                finish()
            }
        } else {
            Log.d("SmsGroupSelectionActivity", "No user found, redirecting to GettingStartedActivity.")
            startActivity(Intent(this, GettingStartedActivity::class.java))
            finish()
        }
    }

    // Helper function to centralize dialog showing logic based on flags
    private fun tryShowGroupSelectionDialog(amount: String?, description: String?, functionality: String?) {
        if (!isDialogShowing && isUserDataLoaded && isGroupsDataReady) {
            val groups = groupSelectionViewModel.groups.value // Get the current value from LiveData
            val validGroups = groups?.filter { it.isGroupValid == true } // Filter for valid groups

            if (!validGroups.isNullOrEmpty()) {
                showGroupSelectionDialogInternal(validGroups, amount, description, functionality)
                isDialogShowing = true // Set flag to prevent re-showing
            } else {
                Toast.makeText(this@SmsGroupSelectionActivity, "No valid groups available. Please create a valid group first.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    // Renamed this method to avoid conflict with the original structure if you call it directly elsewhere
    private fun showGroupSelectionDialogInternal(groups: List<GroupData_Detailed_Model>, amount: String?, description: String?, functionality: String?) {
        // No need for lifecycleScope.launch here, as data is already provided by observer
        // No need to set bar visibility here, as ViewModel handles it via `isLoading`

        val groupNames = groups.map { it.name ?: "Unnamed Group" }.toTypedArray()
        val groupIds = groups.map { it.groupID ?: "Unknown ID" }.toTypedArray()

        AlertDialog.Builder(this@SmsGroupSelectionActivity)
            .setTitle("Select Group")
            .setItems(groupNames) { dialog, which ->
                selectedGroup = groupNames[which]
                val selectedGroupId = groupIds[which]

                Toast.makeText(this@SmsGroupSelectionActivity, "Selected Group: ${groupNames[which]}, Group ID: $selectedGroupId", Toast.LENGTH_SHORT).show()

                GlobalClass.selected_groupId = selectedGroupId

                if(functionality=="SettlingExpense"){
                    bar.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        if (GlobalClass.expenses.isEmpty() || selectedGroup == null) {
                            try {
                                // Fetch expenses from repository only if empty or group not selected
                                expensesRepository.getAllExpenseAsAList() // This updates GlobalClass.expenses
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Handle error, e.g., show a Toast message
                            }
                        }
                    }
                    val intent = Intent(this@SmsGroupSelectionActivity, ExpenseSettleUpActivity::class.java).apply {
                        /*putExtra(ExpenseAddActivity.EXTRA_AMOUNT, amount)
                        putExtra(ExpenseAddActivity.EXTRA_DESCRIPTION, description)*/
                        putExtra(ExpenseAddActivity.EXTRA_IS_AUTO_DETECTED, true)
                    }
                    bar.visibility=View.GONE
                    startActivity(intent)
                }else{
                    val intent = Intent(this@SmsGroupSelectionActivity, ExpenseAddActivity::class.java).apply {
                        putExtra(ExpenseAddActivity.EXTRA_AMOUNT, amount)
                        putExtra(ExpenseAddActivity.EXTRA_DESCRIPTION, description)
                        putExtra(ExpenseAddActivity.EXTRA_IS_AUTO_DETECTED, true)
                    }

                    startActivity(intent)
                }

                finish() // Finish this activity after launching ExpenseAddActivity
                dialog.dismiss() // Dismiss the dialog
            }
            .setCancelable(false) // User must select a group or exit the activity
            .setOnDismissListener {
                // If the dialog is dismissed (e.g., by back button if cancelable was true, or after selection)
                isDialogShowing = false // Reset the flag
            }
            .show()
    }
}