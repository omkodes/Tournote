package com.example.tournote.Functionality.Segments.Expenses.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseAddActivity
import com.example.tournote.Functionality.Segments.Expenses.SealedClass.ExpenseListItem
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpensesAdapter
import com.example.tournote.Functionality.Segments.Expenses.Adapter.FinalDistributionAdapter // Import new adapter
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.activityGroupInfo
import com.example.tournote.R
import com.example.tournote.UserModel // Import UserModel
import com.example.tournote.databinding.FragmentExpensesBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseSettleUpActivity
import com.example.tournote.Functionality.Segments.Expenses.SealedClass.DistributionItem

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private val expensesRepository = ExpensesRepository()
    private lateinit var expensesAdapter: ExpensesAdapter
    private lateinit var finalDistributionAdapter: FinalDistributionAdapter // Declare new adapter

    // Views for final distribution
    private lateinit var txtMyFinalResult: TextView
    private lateinit var recvwFinalDistributionRelaventtoMe: RecyclerView

    private val addExpenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == ExpenseAddActivity.RESULT_OK_EXPENSE_ADDED) {
            refreshExpensesList() // Refresh all data when expense is added
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        val view = binding.root

        // Toolbar setup (existing code)
        val groupLogo = view.findViewById<ImageView>(R.id.grp_logo)
        val groupName = view.findViewById<TextView>(R.id.grp_name)

        view.findViewById<LinearLayout>(R.id.toolbar).setOnClickListener {
            val intent = Intent(requireContext(), activityGroupInfo::class.java)
            startActivity(intent)
        }

        val selectedGroup = GlobalClass.GroupDetails_Everything
        groupName.text = selectedGroup?.name

        // Using Glide correctly: check for null or "null" string before loading
        if (!selectedGroup?.profilePic.isNullOrBlank() && selectedGroup?.profilePic != "null") {
            Glide.with(this)
                .load(selectedGroup?.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(groupLogo)
        } else {
            Glide.with(this)
                .load(R.drawable.defaultgroupimage) // Load default if path is null, blank, or "null"
                .into(groupLogo)
        }


        // Initialize main Expenses RecyclerView and Adapter
        expensesAdapter = ExpensesAdapter()
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expensesAdapter
        }

        // Initialize Final Distribution views and RecyclerView
        txtMyFinalResult = view.findViewById(R.id.txtMyFinalResult)
        recvwFinalDistributionRelaventtoMe = view.findViewById(R.id.recvwFinalDistributionRelaventtoMe)
        setupFinalDistributionRecyclerView() // Setup the new RecyclerView

        // Fetch and display all data initially (main expenses and final distribution)
        fetchAndDisplayExpenses()

        binding.btnAddExpense.setOnClickListener {
            val intent = Intent(requireContext(), ExpenseAddActivity::class.java)
            addExpenseLauncher.launch(intent)
        }

        binding.btnSettleUp.setOnClickListener {
            val intent = Intent(requireContext(), ExpenseSettleUpActivity::class.java)
            startActivity(intent)
        }


        // Handle other buttons (Settle Up, Charts, Balances, Totals)
        //binding.btnCharts.setOnClickListener { /* Implement Charts logic */ }
        //binding.btnTotals.setOnClickListener { /* Implement Totals logic */ }

        return binding.root
    }

    private fun setupFinalDistributionRecyclerView() {
        // Use an empty list initially, it will be updated after calculations
        finalDistributionAdapter = FinalDistributionAdapter(emptyList())
        recvwFinalDistributionRelaventtoMe.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = finalDistributionAdapter
            isNestedScrollingEnabled = false // Important for RecyclerViews inside ScrollView
        }
    }

    private fun refreshExpensesList() {
        // This will refetch all expenses and recalculate distribution
        fetchAndDisplayExpenses()
    }

    private fun fetchAndDisplayExpenses() {
        if (_binding == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (GlobalClass.expenses.isEmpty() || GlobalClass.selected_groupId == null) {
                try {
                    // Fetch expenses from repository only if empty or group not selected
                    expensesRepository.getAllExpenseAsAList() // This updates GlobalClass.expenses
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle error, e.g., show a Toast message
                }
            }
            //total group expenditure calculator
            val total = GlobalClass.expenses.sumOf { it.amount.toDouble() }
            binding.txtTotalGroupExpenditure.text="Total group expenditure ₹$total"

            // After fetching (or if already populated), process and display both lists
            updateMainExpensesList()
            calculateAndDisplayFinalDistribution()
        }
    }

    /**
     * Updates the main expenses RecyclerView with headers.
     */
    private fun updateMainExpensesList() {
        val itemsWithHeaders = createExpenseListWithHeaders(GlobalClass.expenses)
        expensesAdapter.submitList(itemsWithHeaders)
    }

    /**
     * Calculates the final expense distribution and updates the UI.
     */
    /**
     * Calculates the final expense distribution and updates the UI.
     * First calculates individual distributions, then derives the total from those entries.
     */
    private fun calculateAndDisplayFinalDistribution() {
        val myUid = GlobalClass.Me?.uid ?: return // Get current user's UID
        val groupMembers = GlobalClass.GroupDetails_Everything
            ?.members ?: emptyList()

        if (groupMembers.isEmpty() || GlobalClass.expenses.isEmpty()) {
            // No members or no expenses, show default or clear distribution
            txtMyFinalResult.text = "No expenses yet."
            txtMyFinalResult.setTextColor(resources.getColor(R.color.black, null))
            finalDistributionAdapter.submitList(emptyList())
            return
        }

        // Step 1: Calculate individual distributions first
        val distributionList = calculateIndividualDistributions(myUid, groupMembers)

        // Step 2: Sort distribution list for consistent display
        val sortedDistributionList = distributionList.sortedWith(compareBy<DistributionItem> {
            when (it) {
                is DistributionItem.Owes -> 0 // Owes items come first
                is DistributionItem.Borrowed -> 1 // Borrowed items come second
            }
        }.thenBy {
            when (it) {
                is DistributionItem.Owes -> it.name
                is DistributionItem.Borrowed -> it.name
            }
        })

        // Step 3: Update the RecyclerView with distribution data
        finalDistributionAdapter.submitList(sortedDistributionList)

        // Step 4: Calculate total from the distribution entries (this ensures consistency)
        val totalOwedToMe = sortedDistributionList
            .filterIsInstance<DistributionItem.Owes>()
            .sumOf { it.amount }

        val totalIOweThem = sortedDistributionList
            .filterIsInstance<DistributionItem.Borrowed>()
            .sumOf { it.amount }

        val netBalance = totalOwedToMe - totalIOweThem

        // Step 5: Update overall result text based on net balance from distribution entries
        updateOverallResultFromDistribution(netBalance, totalOwedToMe, totalIOweThem)
    }

    /**
     * Calculate individual distributions between me and each group member
     */
    private fun calculateIndividualDistributions(myUid: String, groupMembers: List<UserModel>): List<DistributionItem> {
        val distributionList = mutableListOf<DistributionItem>()

        groupMembers.forEach { otherMember ->
            if (otherMember.uid != myUid) {
                val otherMemberUid = otherMember.uid!!

                // Calculate direct net amount between me and this specific member
                var directNetAmount = 0.0

                // Sum of amounts I paid for which 'otherMember' was liable (they owe me)
                GlobalClass.expenses.forEach { expense ->
                    if (expense.paidBy == myUid) {
                        expense.splitMembers?.forEach { share ->
                            if (share.memberUid == otherMemberUid) {
                                if(share.paid==false){
                                    directNetAmount += (share.shareAmount-(share.partialPayment?:0.00))
                                }
                            }
                        }
                    }
                }

                // Sum of amounts 'otherMember' paid for which I was liable (I owe them)
                GlobalClass.expenses.forEach { expense ->
                    if (expense.paidBy == otherMemberUid) {
                        expense.splitMembers?.forEach { share ->
                            if (share.memberUid == myUid) {
                                if(share.paid==false){
                                    directNetAmount -= (share.shareAmount-(share.partialPayment?:0.00))
                                }
                            }
                        }
                    }
                }

                // Add to distribution list based on net amount
                if (directNetAmount > 0.01) { // Use small epsilon for double comparison
                    // otherMember owes me
                    distributionList.add(DistributionItem.Owes(otherMember.name ?: "Unknown", directNetAmount))
                } else if (directNetAmount < -0.01) {
                    // I owe otherMember
                    distributionList.add(DistributionItem.Borrowed(otherMember.name ?: "Unknown", -directNetAmount))
                }
                // If directNetAmount is ~0, no entry is added (settled between us)
            }
        }

        return distributionList
    }

    /**
     * Update the overall result text based on the calculated distribution entries
     */
    private fun updateOverallResultFromDistribution(netBalance: Double, totalOwedToMe: Double, totalIOweThem: Double) {
        when {
            netBalance > 0.01 -> {
                // I am owed more than I owe
                val formattedAmount = "₹%.2f".format(netBalance)
                txtMyFinalResult.text = "You are owed $formattedAmount overall"
                txtMyFinalResult.setTextColor(resources.getColor(R.color.textGreen, null))
            }
            netBalance < -0.01 -> {
                // I owe more than I am owed
                val formattedAmount = "₹%.2f".format(-netBalance)
                txtMyFinalResult.text = "You owe $formattedAmount overall"
                txtMyFinalResult.setTextColor(resources.getColor(R.color.textRed, null))
            }
            else -> {
                // Settled (net balance is ~0)
                txtMyFinalResult.text = "You are settled up"
                txtMyFinalResult.setTextColor(resources.getColor(R.color.black, null))
            }
        }
    }

    /**
     * Optional: Validation method to verify consistency between individual distributions and overall total
     * Call this method in debug builds to ensure calculations are correct
     */
    private fun validateDistributionConsistency(distributionList: List<DistributionItem>) {
        val totalOwedToMe = distributionList
            .filterIsInstance<DistributionItem.Owes>()
            .sumOf { it.amount }

        val totalIOweThem = distributionList
            .filterIsInstance<DistributionItem.Borrowed>()
            .sumOf { it.amount }

        val netFromDistribution = totalOwedToMe - totalIOweThem

        // Also calculate using the old method for comparison
        val myUid = GlobalClass.Me?.uid ?: return
        val balances = mutableMapOf<String, Double>()
        val groupMembers = GlobalClass.GroupDetails_Everything
            ?.members ?: return

        // Initialize all group members with 0 balance
        groupMembers.forEach { member ->
            balances[member.uid!!] = 0.0
        }

        // Calculate balances using old method
        GlobalClass.expenses.forEach { expense ->
            val paidByUid = expense.paidBy
            val totalAmount = expense.amount.toDoubleOrNull() ?: 0.0

            balances[paidByUid] = (balances[paidByUid] ?: 0.0) + totalAmount

            expense.splitMembers?.forEach { memberShare ->
                val memberUid = memberShare.memberUid
                val shareAmount = memberShare.shareAmount
                balances[memberUid] = (balances[memberUid] ?: 0.0) - shareAmount
            }
        }

        val myNetBalanceOldMethod = balances[myUid] ?: 0.0

        // Compare the two methods (should be very close)
        val difference = kotlin.math.abs(netFromDistribution - myNetBalanceOldMethod)
        if (difference > 0.02) { // Allow small floating point differences
            println("Warning: Distribution calculation inconsistency detected!")
            println("Net from distribution: $netFromDistribution")
            println("Net from old method: $myNetBalanceOldMethod")
            println("Difference: $difference")
        } else {
            println("Distribution calculation is consistent ✓")
        }
    }


    // Function to update the overall result text
    private fun updateOverallResult(isOwed: Boolean, amount: Double) {
        val formattedAmount = "₹%.2f".format(amount)
        if (isOwed) {
            txtMyFinalResult.text = "You are owed $formattedAmount overall"
            txtMyFinalResult.setTextColor(resources.getColor(R.color.textGreen, null))
        } else {
            txtMyFinalResult.text = "You owe $formattedAmount overall"
            txtMyFinalResult.setTextColor(resources.getColor(R.color.textRed, null))
        }
    }


    /**
     * Processes a list of ExpensesDataClass to insert MonthHeader items for display in RecyclerView.
     * Expenses are sorted by timestamp in descending order (newest first).
     */
    private fun createExpenseListWithHeaders(expenses: List<ExpensesDataClass>): List<ExpenseListItem> {
        val listWithHeaders = mutableListOf<ExpenseListItem>()

        // 1. Sort expenses by timestamp in descending order (newest first)
        val sortedExpenses = expenses.sortedByDescending { it.timestamp.toLongOrNull() ?: 0L }

        var currentMonthYear: String? = null
        val monthYearFormat =
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // e.g., "June 2025"

        for (expense in sortedExpenses) {
            val timestampLong = expense.timestamp.toLongOrNull()
            if (timestampLong != null) {
                val expenseDate = Date(timestampLong)
                val monthYear = monthYearFormat.format(expenseDate)

                // If the month/year changes, add a new header
                if (monthYear != currentMonthYear) {
                    listWithHeaders.add(ExpenseListItem.MonthHeader(monthYear))
                    currentMonthYear = monthYear
                }
            }
            // Always add the expense item itself
            listWithHeaders.add(ExpenseListItem.ExpenseItem(expense))
        }
        return listWithHeaders
    }

    // Helper functions to calculate individual contributions and shares (for more complex calculations if needed)
    private fun calculateIndividualContributions(userUid: String, expenses: List<ExpensesDataClass>): Double {
        return expenses.filter { it.paidBy == userUid }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }

    private fun calculateIndividualShares(userUid: String, expenses: List<ExpensesDataClass>): Double {
        return expenses.sumOf { expense ->
            expense.splitMembers?.find { it.memberUid == userUid }?.shareAmount ?: 0.0
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            refreshExpensesList()
        }
    }
}
