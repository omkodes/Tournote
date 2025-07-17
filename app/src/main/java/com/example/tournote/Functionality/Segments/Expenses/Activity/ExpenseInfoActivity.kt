package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityExpenseInfoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.ScrollView

class ExpenseInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseInfoBinding
    private lateinit var webView: WebView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_info)

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        binding = ActivityExpenseInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get reference to ScrollView
        scrollView = binding.root.findViewById<ScrollView>(R.id.scrollView) // Make sure you have this ID in your layout

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val expenseId = intent.getStringExtra("expenseId")
        val currentExpense = GlobalClass.expenses.find { it.expenseId == expenseId }
        val currentGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }

        binding.txtDescription.text = currentExpense?.details
        binding.txtAmount.text = currentExpense?.amount

        if(!currentExpense?.note.isNullOrEmpty()){
            binding.txtNote.visibility = View.VISIBLE
            binding.txtLabelNote.visibility = View.VISIBLE
            binding.txtNote.text = currentExpense?.note
        }

        if(GlobalClass.Me?.uid == currentExpense?.paidBy){
            binding.btnDeleteRecord.visibility = View.VISIBLE
            binding.btnEdit.visibility = View.VISIBLE
            binding.txtPaidBy.text = "You paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by you on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((GlobalClass.Me?.profilePic)!!)
        } else {
            binding.btnDeleteRecord.visibility = View.GONE
            binding.btnEdit.visibility = View.GONE
            val whoPaid = currentGroup?.members?.find { it.uid == currentExpense?.paidBy }
            binding.txtPaidBy.text = "${whoPaid?.name} paid ${binding.txtCurrency.text.toString()} ${currentExpense?.amount}"
            binding.txtDate.text = "Added by ${whoPaid?.name} on ${formatTimestampToDate((currentExpense?.timestamp)!!)}"
            imgPaidBy_Populator((whoPaid?.profilePic)!!)
        }

        binding.btnCloseActivity.setOnClickListener {
            finish()
        }

        // Bill image handling
        if(!currentExpense?.billImageUrl.isNullOrEmpty()){
            binding.cardBillPreview.visibility = View.VISIBLE

            Glide.with(this)
                .load(currentExpense.billImageUrl)
                .placeholder(R.drawable.imageselector)
                .error(R.drawable.imageselector)
                .into(binding.imgBillPreview)
        }

        // Location map handling
        if(!currentExpense?.latitude.toString().isNullOrEmpty() && !currentExpense?.longitude.toString().isNullOrEmpty()){
            setupLocationMap(currentExpense.latitude.toString(), currentExpense.longitude.toString())
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