// File: com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseSplitterActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpenseSplitEqual_rvAdapter
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpenseSplitExactAmt_rvAdapter
import com.example.tournote.Functionality.Segments.Expenses.Adapter.ExpenseSplitPercentage_rvAdapter
import com.example.tournote.Functionality.Segments.Expenses.Adapter.OnMemberSelectionChangeListener
import com.example.tournote.Functionality.Segments.Expenses.Adapter.OnPercentageChangeListener
import com.example.tournote.Functionality.Segments.Expenses.Adapter.OnExactAmountChangeListener
import com.example.tournote.Functionality.Segments.Expenses.DataClass.MemberShare
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitType // Make sure SplitType is imported
import com.example.tournote.Functionality.Segments.Expenses.DataClass.UserModelForSplitter
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseSplitterBinding
import java.text.NumberFormat
import java.util.Locale

class ExpenseSplitterActivity : AppCompatActivity(), OnMemberSelectionChangeListener, OnPercentageChangeListener, OnExactAmountChangeListener {

    private lateinit var binding: ActivityExpenseSplitterBinding

    private lateinit var equalAdapter: ExpenseSplitEqual_rvAdapter
    private lateinit var exactamtAdapter: ExpenseSplitExactAmt_rvAdapter
    private lateinit var percentageAdapter: ExpenseSplitPercentage_rvAdapter

    private var currentSplitIndex: Int = 0 // 0: Equal, 1: Exact Amt, 2: Percentage
    private var isAllTicked: Boolean = false

    private var totalAmount: Double = 0.0

    private var finalMemberShares: MutableList<MemberShare> = mutableListOf()

    private lateinit var slideInRight: Animation
    private lateinit var slideOutLeft: Animation
    private lateinit var slideInLeft: Animation
    private lateinit var slideOutRight: Animation

    // Companion object for Intent keys
    companion object {
        const val EXTRA_MEMBER_SHARES = "extra_member_shares"
        const val EXTRA_SPLIT_TYPE = "extra_split_type"
        const val RESULT_OK_SPLIT = AppCompatActivity.RESULT_OK + 2 // A unique result code
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExpenseSplitterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val totalAmountString = intent.getStringExtra("totalAmount")
        totalAmount = totalAmountString?.toDoubleOrNull() ?: 0.0

        val currentGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }
        val groupMembers: MutableList<UserModelForSplitter> =
            currentGroup?.members?.map {
                UserModelForSplitter(
                    uid = it.uid,
                    email = it.email,
                    name = it.name,
                    profilePic = it.profilePic,
                    phoneNumber = it.phoneNumber,
                    isSelected = false,
                    exactAmount = 0.0,
                    percentage = 0.0
                )
            }?.toMutableList() ?: mutableListOf()

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        slideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        binding.viewFlipperRecyclerViews.inAnimation = slideInRight
        binding.viewFlipperRecyclerViews.outAnimation = slideOutLeft
        binding.viewFlipperBottomNav.inAnimation = slideInRight
        binding.viewFlipperBottomNav.outAnimation = slideOutLeft

        equalAdapter = ExpenseSplitEqual_rvAdapter(groupMembers, this, this)
        exactamtAdapter = ExpenseSplitExactAmt_rvAdapter(groupMembers, this, this)
        percentageAdapter = ExpenseSplitPercentage_rvAdapter(groupMembers, this, this)

        binding.recyclerViewEqual.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewExactAmt.layoutManager = LinearLayoutManager(this)
        binding.recyclerViePercentage.layoutManager = LinearLayoutManager(this)

        binding.recyclerViewEqual.adapter = equalAdapter
        binding.recyclerViewExactAmt.adapter = exactamtAdapter
        binding.recyclerViePercentage.adapter = percentageAdapter

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED) // Indicate that the user cancelled
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSplitSelectorControls()
        setupEqualBottomNavControls()
        setupPercentageBottomNavControls()
        setupExactAmountBottomNavControls()

        updateSplitUI(0)
        updateEqualSplitUI()
        calculateAndStoreEqualShares() // Initial calculation for default tab

        binding.btnDone.setOnClickListener {
            var selectedSplitType: SplitType? = null
            var canProceed = true

            when (currentSplitIndex) {
                0 -> {
                    calculateAndStoreEqualShares()
                    selectedSplitType = SplitType.EQUAL
                }
                1 -> {
                    calculateAndStoreExactShares()
                    selectedSplitType = SplitType.EXACT_AMOUNT
                    val currentAllocatedAmount = exactamtAdapter.getAllMembersWithExactAmounts().sumOf { it.exactAmount }
                    val epsilon = 0.01 // Adjusted epsilon for currency
                    if (Math.abs(currentAllocatedAmount - totalAmount) > epsilon) {
                        canProceed = false
                        Toast.makeText(this, "Exact amounts do not sum to total amount. Please adjust.", Toast.LENGTH_LONG).show()
                    }
                }
                2 -> {
                    calculateAndStorePercentageShares()
                    selectedSplitType = SplitType.PERCENTAGE
                    val currentTotalPercentage = percentageAdapter.getAllMembersWithPercentages().sumOf { it.percentage }
                    val epsilon = 0.001
                    if (Math.abs(currentTotalPercentage - 100.0) > epsilon) {
                        canProceed = false
                        Toast.makeText(this, "Percentages do not sum to 100%. Please adjust.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (canProceed && finalMemberShares.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    // Use ArrayList for putExtra to pass List<Parcelable>
                    putExtra(EXTRA_MEMBER_SHARES, ArrayList(finalMemberShares))
                    putExtra(EXTRA_SPLIT_TYPE, selectedSplitType?.name) // Pass enum name as string
                }
                setResult(RESULT_OK_SPLIT, resultIntent)
                finish()
            } else if (canProceed && finalMemberShares.isEmpty()) {
                Toast.makeText(this, "No members selected for split or amounts are zero.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSplitSelectorControls() {
        binding.btnEqual.setOnClickListener {
            if (currentSplitIndex != 0) {
                setFlipperAnimations(currentSplitIndex, 0)
                binding.viewFlipperRecyclerViews.displayedChild = 0
                binding.viewFlipperBottomNav.displayedChild = 0
                currentSplitIndex = 0
                updateSplitUI(0)
                updateEqualSplitUI()
                calculateAndStoreEqualShares()
            }
        }

        binding.btnExact.setOnClickListener {
            if (currentSplitIndex != 1) {
                setFlipperAnimations(currentSplitIndex, 1)
                binding.viewFlipperRecyclerViews.displayedChild = 1
                binding.viewFlipperBottomNav.displayedChild = 1
                currentSplitIndex = 1
                updateSplitUI(1)
                updateExactSplitUI()
                calculateAndStoreExactShares()
            }
        }

        binding.btnPercentage.setOnClickListener {
            if (currentSplitIndex != 2) {
                setFlipperAnimations(currentSplitIndex, 2)
                binding.viewFlipperRecyclerViews.displayedChild = 2
                binding.viewFlipperBottomNav.displayedChild = 2
                currentSplitIndex = 2
                updateSplitUI(2)
                updatePercentageSplitUI()
                calculateAndStorePercentageShares()
            }
        }
    }

    // --- Equal Split Specific Controls ---
    private fun setupEqualBottomNavControls() {
        binding.imgtick.setOnClickListener {
            isAllTicked = !isAllTicked
            updateAllTickUI()
            equalAdapter.setAllMembersSelected(isAllTicked)
            updateEqualSplitUI()
            calculateAndStoreEqualShares()
        }
    }

    private fun updateEqualSplitUI() {
        val selectedCount = equalAdapter.getSelectedMembers().size
        val amountPerPerson: Double

        if (selectedCount > 0) {
            amountPerPerson = totalAmount / selectedCount
            val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amountPerPerson)
                .replace(NumberFormat.getCurrencyInstance(Locale("en", "IN")).currency?.symbol ?: "₹", "₹")

            binding.constraintEqualBottomNav.findViewById<TextView>(R.id.textView_amount_per_person).text = "$formattedAmount/person"
        } else {
            binding.constraintEqualBottomNav.findViewById<TextView>(R.id.textView_amount_per_person).text = "₹0.00/person"
        }
        binding.constraintEqualBottomNav.findViewById<TextView>(R.id.textView_people_count).text = "($selectedCount people)"
        updateAllTickUI()
        //binding.btnDone.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
    }

    // --- Percentage Split Specific Controls ---
    private fun setupPercentageBottomNavControls() {
        // No specific controls for percentage here, updates from listener
    }

    private fun updatePercentageSplitUI() {
        val currentTotalPercentage = percentageAdapter.getAllMembersWithPercentages().sumOf { it.percentage }
        val remainingPercentage = 100.0 - currentTotalPercentage

        val formattedTotalPercentage = String.format(Locale.getDefault(), "%.2f%%", currentTotalPercentage)
        val formattedRemainingPercentage = String.format(Locale.getDefault(), "%.2f%%", remainingPercentage)

        val tvTotalPercentage = binding.constraintPercentBottomNav.findViewById<TextView>(R.id.textView_total_percentage)
        val tvRemainingPercentage = binding.constraintPercentBottomNav.findViewById<TextView>(R.id.textView_remaining_percentage)

        tvTotalPercentage.text = "$formattedTotalPercentage of 100%"
        tvRemainingPercentage.text = "$formattedRemainingPercentage left"

        val epsilon = 0.001
        if (Math.abs(remainingPercentage) > epsilon) {
            tvRemainingPercentage.setTextColor(ContextCompat.getColor(this, R.color.mapEndPoint))
            tvTotalPercentage.setTextColor(ContextCompat.getColor(this, R.color.mapEndPoint))
            //binding.btnDone.visibility = View.GONE
        } else {
            tvRemainingPercentage.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvTotalPercentage.setTextColor(ContextCompat.getColor(this, R.color.white))
            //binding.btnDone.visibility = View.VISIBLE
        }
    }

    // --- NEW: Exact Amount Split Specific Controls ---
    private fun setupExactAmountBottomNavControls() {
        // You could add buttons like "Clear All" or "Fill Remaining" here for exact amounts if desired.
    }

    private fun updateExactSplitUI() {
        val currentAllocatedAmount = exactamtAdapter.getAllMembersWithExactAmounts().sumOf { it.exactAmount }
        val remainingAmount = totalAmount - currentAllocatedAmount

        val formattedAllocatedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(currentAllocatedAmount)
            .replace(NumberFormat.getCurrencyInstance(Locale("en", "IN")).currency?.symbol ?: "₹", "₹")

        val formattedTotalAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(totalAmount)
            .replace(NumberFormat.getCurrencyInstance(Locale("en", "IN")).currency?.symbol ?: "₹", "₹")

        val formattedRemainingAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(remainingAmount)
            .replace(NumberFormat.getCurrencyInstance(Locale("en", "IN")).currency?.symbol ?: "₹", "₹")


        val tvAllocatedAmount = binding.constraintExactAmtBottomNav.findViewById<TextView>(R.id.textView_allocated_amount)
        val tvRemainingAmount = binding.constraintExactAmtBottomNav.findViewById<TextView>(R.id.textView_remaining_amount)

        tvAllocatedAmount.text = "$formattedAllocatedAmount of $formattedTotalAmount"
        tvRemainingAmount.text = "$formattedRemainingAmount left"

        val epsilon = 0.01
        if (Math.abs(remainingAmount) > epsilon) {
            tvRemainingAmount.setTextColor(ContextCompat.getColor(this, R.color.mapEndPoint))
            tvAllocatedAmount.setTextColor(ContextCompat.getColor(this, R.color.mapEndPoint))
            //binding.btnDone.visibility = View.GONE
        } else {
            tvRemainingAmount.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvAllocatedAmount.setTextColor(ContextCompat.getColor(this, R.color.white))
            //binding.btnDone.visibility = View.VISIBLE
        }
    }


    private fun setFlipperAnimations(oldIndex: Int, newIndex: Int) {
        if (newIndex > oldIndex) {
            binding.viewFlipperRecyclerViews.inAnimation = slideInRight
            binding.viewFlipperRecyclerViews.outAnimation = slideOutLeft
            binding.viewFlipperBottomNav.inAnimation = slideInRight
            binding.viewFlipperBottomNav.outAnimation = slideOutLeft
        } else {
            binding.viewFlipperRecyclerViews.inAnimation = slideInLeft
            binding.viewFlipperRecyclerViews.outAnimation = slideOutRight
            binding.viewFlipperBottomNav.inAnimation = slideInLeft
            binding.viewFlipperBottomNav.outAnimation = slideOutRight
        }
    }

    private fun updateSplitUI(selectedIndex: Int) {
        binding.btnEqual.background = ContextCompat.getDrawable(this, R.drawable.whitebutton_sharpedge)
        binding.btnExact.background = ContextCompat.getDrawable(this, R.drawable.whitebutton_sharpedge)
        binding.btnPercentage.background = ContextCompat.getDrawable(this, R.drawable.whitebutton_sharpedge)

        when (selectedIndex) {
            0 -> binding.btnEqual.background = ContextCompat.getDrawable(this, R.drawable.greenbutton_sharpedge)
            1 -> binding.btnExact.background = ContextCompat.getDrawable(this, R.drawable.greenbutton_sharpedge)
            2 -> binding.btnPercentage.background = ContextCompat.getDrawable(this, R.drawable.greenbutton_sharpedge)
        }
    }

    private fun updateAllTickUI() {
        if (isAllTicked) {
            binding.imgtick.setImageResource(R.drawable.tick)
        } else {
            binding.imgtick.setImageResource(R.drawable.untick)
        }
    }

    // --- Share Calculation Functions ---

    private fun calculateAndStoreEqualShares() {
        finalMemberShares.clear()

        val selectedMembers = equalAdapter.getSelectedMembers()
        val selectedCount = selectedMembers.size

        if (selectedCount > 0) {
            val amountPerPerson = totalAmount / selectedCount
            selectedMembers.forEach { member ->
                finalMemberShares.add(
                    MemberShare(
                        memberUid = (member.uid)!!,
                        memberName = (member.name)!!,
                        shareAmount = amountPerPerson,
                        shareType = SplitType.EQUAL
                    )
                )
            }
        }
        Log.d("ExpenseSplitter", "Equal Shares: $finalMemberShares")
    }

    private fun calculateAndStoreExactShares() {
        finalMemberShares.clear()
        val allMembersWithExactAmounts = exactamtAdapter.getAllMembersWithExactAmounts()
        val currentAllocatedAmount = allMembersWithExactAmounts.sumOf { it.exactAmount }

        val epsilon = 0.01

        if (Math.abs(currentAllocatedAmount - totalAmount) < epsilon) {
            allMembersWithExactAmounts.forEach { member ->
                finalMemberShares.add(
                    MemberShare(
                        memberUid = (member.uid)!!,
                        memberName = (member.name)!!,
                        shareAmount = member.exactAmount,
                        shareType = SplitType.EXACT_AMOUNT,
                        originalInputValue = member.exactAmount
                    )
                )
            }
        } else {
            Log.w("ExpenseSplitter", "Exact amount sum does not match total amount! Allocated: $currentAllocatedAmount, Total: $totalAmount")
        }
        Log.d("ExpenseSplitter", "Exact Shares: $finalMemberShares")
    }

    private fun calculateAndStorePercentageShares() {
        finalMemberShares.clear()
        val allMembersWithPercentages = percentageAdapter.getAllMembersWithPercentages()
        val currentTotalPercentage = allMembersWithPercentages.sumOf { it.percentage }

        val epsilon = 0.001
        if (Math.abs(currentTotalPercentage - 100.0) < epsilon) {
            allMembersWithPercentages.forEach { member ->
                val shareAmount = totalAmount * (member.percentage / 100.0)
                finalMemberShares.add(
                    MemberShare(
                        memberUid = (member.uid)!!,
                        memberName = (member.name)!!,
                        shareAmount = shareAmount,
                        shareType = SplitType.PERCENTAGE,
                        originalInputValue = member.percentage
                    )
                )
            }
        } else {
            Log.w("ExpenseSplitter", "Percentage sum is not 100%! Current: $currentTotalPercentage%")
        }
        Log.d("ExpenseSplitter", "Percentage Shares: $finalMemberShares")
    }

    // --- OnMemberSelectionChangeListener (for Equal split) ---
    override fun onMemberSelectionChanged(member: UserModelForSplitter, isSelected: Boolean) {
        val allSelected = equalAdapter.getSelectedMembers().size == equalAdapter.itemCount
        val noneSelected = equalAdapter.getSelectedMembers().isEmpty()

        if (allSelected) {
            if (!isAllTicked) {
                isAllTicked = true
                updateAllTickUI()
            }
        } else if (noneSelected) {
            if (isAllTicked) {
                isAllTicked = false
                updateAllTickUI()
            }
        } else {
            if (isAllTicked) {
                isAllTicked = false
                updateAllTickUI()
            }
        }
        updateEqualSplitUI()
        calculateAndStoreEqualShares()
    }

    override fun onAllMembersToggled(areAllSelected: Boolean) {
        // Left minimal as handled elsewhere.
    }

    // --- OnPercentageChangeListener (for Percentage split) ---
    override fun onPercentageChanged(member: UserModelForSplitter, newPercentage: Double) {
        updatePercentageSplitUI()
        calculateAndStorePercentageShares()
    }

    // --- NEW: OnExactAmountChangeListener (for Exact Amount split) ---
    override fun onExactAmountChanged(member: UserModelForSplitter, newAmount: Double) {
        updateExactSplitUI()
        calculateAndStoreExactShares()
    }
}