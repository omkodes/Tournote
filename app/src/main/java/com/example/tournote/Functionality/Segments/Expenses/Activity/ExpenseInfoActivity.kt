// File: com.example.tournote.Functionality.Segments.Expenses.Activity.ExpenseInfoActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Expenses.Adapter.SplitMembersAdapter // Import new adapter
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitMemberDisplayData // Import new data class
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseInfoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseInfoActivity : AppCompatActivity() {

    private val repo = ExpensesRepository()

    private lateinit var binding: ActivityExpenseInfoBinding
    private lateinit var webView: WebView
    private lateinit var scrollView: ScrollView
    private lateinit var splitMembersAdapter: SplitMembersAdapter // Declare the adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        binding = ActivityExpenseInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scrollView = binding.scrollView // Reference to ScrollView from binding

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val expenseId = intent.getStringExtra("expenseId")
        val currentExpense = GlobalClass.expenses.find { it.expenseId == expenseId }
        val currentGroup = GlobalClass.GroupDetails_Everything

        binding.txtDescription.text = currentExpense?.details
        binding.txtAmount.text = currentExpense?.amount

        if(!currentExpense?.note.isNullOrEmpty()){
            binding.txtNote.visibility = View.VISIBLE
            binding.txtLabelNote.visibility = View.VISIBLE
            binding.txtNote.text = currentExpense?.note
        } else {
            binding.txtNote.visibility = View.GONE
            binding.txtLabelNote.visibility = View.GONE
        }

        if(GlobalClass.Me?.uid == currentExpense?.paidBy){
            binding.btnDeleteRecord.visibility = View.VISIBLE
            //binding.btnEdit.visibility = View.VISIBLE
            binding.txtPaidBy.text = "You paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by you on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((GlobalClass.Me?.profilePic)!!)
        } else {
            binding.btnDeleteRecord.visibility = View.GONE
            //binding.btnEdit.visibility = View.GONE
            val whoPaid = currentGroup?.members?.find { it.uid == currentExpense?.paidBy }
            binding.txtPaidBy.text = "${whoPaid?.name} paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by ${whoPaid?.name} on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((whoPaid?.profilePic)!!)
        }

        binding.btnCloseActivity.setOnClickListener {
            finish()
        }

        binding.btnDeleteRecord.setOnClickListener {
            lifecycleScope.launch {
                repo.deleteExpense((currentExpense.expenseId)!!)
                finish()
            }
        }

        // --- Split Members List Handling ---
        val splitMembers = currentExpense?.splitMembers
        if (!splitMembers.isNullOrEmpty()) {
            binding.recyclerViewSplitMembers.visibility = View.VISIBLE

            val displayDataList = mutableListOf<SplitMemberDisplayData>()
            // Ensure you have GroupMember data class defined and accessible, e.g., in GlobalClass or its own package.
            // Assuming GroupMember looks something like: data class GroupMember(val uid: String, val name: String, val profilePic: String?)
            val groupMembersMap = currentGroup?.members?.associateBy { it.uid } ?: emptyMap()

            splitMembers.forEach { memberShare ->
                val groupMember = groupMembersMap[memberShare.memberUid]
                val memberName = if (memberShare.memberUid == GlobalClass.Me?.uid) {
                    "You"
                } else {
                    groupMember?.name ?: "Unknown Member"
                }
                val profilePicUrl = groupMember?.profilePic

                displayDataList.add(
                    SplitMemberDisplayData(
                        memberUid = memberShare.memberUid,
                        memberName = memberName,
                        profilePicUrl = profilePicUrl,
                        shareAmount = memberShare.shareAmount,
                        isCurrentUser = (memberShare.memberUid == GlobalClass.Me?.uid)
                    )
                )
            }

            splitMembersAdapter = SplitMembersAdapter(displayDataList)
            binding.recyclerViewSplitMembers.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewSplitMembers.adapter = splitMembersAdapter

        } else {
            binding.recyclerViewSplitMembers.visibility = View.GONE
        }
        // --- End Split Members List Handling ---


        // Bill image handling
        if(!currentExpense?.billImageUrl.isNullOrEmpty()){
            binding.cardBillPreview.visibility = View.VISIBLE

            Glide.with(this)
                .load(currentExpense.billImageUrl)
                .placeholder(R.drawable.imageselector)
                .error(R.drawable.imageselector)
                .into(binding.imgBillPreview)
        } else {
            binding.cardBillPreview.visibility = View.GONE
        }

        // Location map handling
        if(currentExpense?.latitude != null && currentExpense.longitude != null){ // Use direct null check for Double
            binding.cardWebView.visibility = View.VISIBLE // Ensure map card is visible
            setupLocationMap(currentExpense.latitude.toString(), currentExpense.longitude.toString())
        } else {
            binding.cardWebView.visibility = View.GONE
        }
    }

    private fun setupLocationMap(latitude: String, longitude: String) {
        webView = binding.webView

        // Configure WebView settings for zoom and interaction
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // Enable built-in zoom controls (pinch to zoom)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false // Hide zoom buttons, keep pinch-to-zoom
        webSettings.setSupportZoom(true)

        // Set up touch handling to distinguish between WebView and ScrollView
        setupWebViewTouchHandling()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val jsCode = "showExpenseLocation($latitude, $longitude);"
                webView.evaluateJavascript(jsCode, null)
            }
        }

        webView.loadUrl("file:///android_asset/expensesinfo_map.html")
    }

    private fun setupWebViewTouchHandling() {
        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Request parent ScrollView to not intercept touch events
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Allow parent ScrollView to intercept touch events again
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Check if this is a multi-touch (pinch) gesture
                    if (event.pointerCount > 1) {
                        // Multi-touch detected, keep blocking parent scroll
                        scrollView.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // Single touch - allow parent to handle if needed
                        // You can add additional logic here if needed
                    }
                }
            }
            // Let WebView handle the touch event
            false
        }

        // Alternative approach: Create a custom WebView wrapper
        val webViewContainer = binding.cardWebView
        webViewContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    fun formatTimestampToDate(timestampString: String): String {
        return try {
            val millis = timestampString.toLong()
            val date = Date(millis)
            val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            formatter.format(date)
        } catch (e: NumberFormatException) {
            "Invalid timestamp"
        }
    }

    fun imgPaidBy_Populator(url: String){
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.imageselector)
            .error(R.drawable.imageselector)
            .into(binding.imgPaidBy)
    }
}