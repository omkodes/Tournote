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
import com.example.tournote.Functionality.Segments.Expenses.Activity.AddExpenseActivity
import com.example.tournote.Functionality.Segments.Expenses.SealedClass.ExpenseListItem
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpensesAdapter
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.activityGroupInfo
import com.example.tournote.R
import com.example.tournote.databinding.FragmentExpensesBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts // Import this

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private val expensesRepository = ExpensesRepository()
    private lateinit var expensesAdapter: ExpensesAdapter

    // 1. Declare the ActivityResultLauncher
    private val addExpenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AddExpenseActivity.RESULT_OK_EXPENSE_ADDED) { // Use a custom result code
            // An expense was successfully added, refresh the list
            refreshExpensesList()
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

        val selectedGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }
        groupName.text = selectedGroup?.name

        if (selectedGroup?.profilePic.isNullOrBlank() || selectedGroup?.profilePic == "null") {
            Glide.with(this)
                .load(selectedGroup?.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(groupLogo)
        }

        // Initialize RecyclerView and Adapter
        expensesAdapter = ExpensesAdapter()
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expensesAdapter
        }

        // Fetch and display expenses initially
        fetchAndDisplayExpenses()

        binding.btnAddExpense.setOnClickListener {
            // 2. Launch AddExpenseActivity using the launcher
            val intent = Intent(requireContext(), AddExpenseActivity::class.java)
            addExpenseLauncher.launch(intent)
        }

        return binding.root
    }

    // Function to handle refreshing the list
    private fun refreshExpensesList() {
        // Clear the existing GlobalClass.expenses to force a fresh fetch
        // Or, implement a smarter update if you only need to add the new item
        //GlobalClass.expenses.clear() // This ensures we refetch everything
        fetchAndDisplayExpenses()
    }

    private fun fetchAndDisplayExpenses() {
        lifecycleScope.launch {
            if (GlobalClass.expenses.isEmpty()) {
                // List is empty, fetch from Firebase
                try {
                    expensesRepository.getAllExpenseAsAList()
                    // After fetching, process the list to add headers and submit
                    val itemsWithHeaders = createExpenseListWithHeaders(GlobalClass.expenses)
                    expensesAdapter.submitList(itemsWithHeaders)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle error, e.g., show a Toast message
                }
            } else {
                // List is not empty, directly use the existing data, but process it to add headers
                val itemsWithHeaders = createExpenseListWithHeaders(GlobalClass.expenses)
                expensesAdapter.submitList(itemsWithHeaders)
            }
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

    // Removed redirectToActivity method since we're using the launcher directly for AddExpenseActivity

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

    }
}