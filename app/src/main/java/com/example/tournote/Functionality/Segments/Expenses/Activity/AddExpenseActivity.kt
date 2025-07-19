// File: com.example.tournote.Functionality.Segments.Expenses.Activity.AddExpenseActivity.kt
package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log // Added for logging
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass
import com.example.tournote.Functionality.Segments.Expenses.DataClass.MemberShare // Import MemberShare
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitType // Import SplitType
import com.example.tournote.Functionality.Segments.Expenses.Repository.ExpensesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.example.tournote.databinding.ActivityAddExpenseBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayList // Explicitly import ArrayList for Parcelable list

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private val repo = ExpensesRepository()
    private var savedLatitude: Double? = null
    private var savedLongitude: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null
    private var isImageFromCamera: Boolean = false
    private var capturedImageFile: File? = null
    private var note: String? = null

    private var selectedDate : Long? = null

    // New properties to store split information received from ExpenseSplitterActivity
    private var splitMemberShares: ArrayList<MemberShare>? = null
    private var selectedSplitType: SplitType = SplitType.SELF // Initialize with SELF as default

    // ActivityResultLauncher for ExpenseSplitterActivity
    private val expenseSplitterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ExpenseSplitterActivity.RESULT_OK_SPLIT) {
            result.data?.let { intent ->
                // Retrieve the member shares list
                splitMemberShares = intent.getParcelableArrayListExtra(ExpenseSplitterActivity.EXTRA_MEMBER_SHARES)
                // Retrieve the split type name (String) and convert it back to SplitType enum
                val splitTypeName = intent.getStringExtra(ExpenseSplitterActivity.EXTRA_SPLIT_TYPE)
                selectedSplitType = splitTypeName?.let { SplitType.valueOf(it) } ?: SplitType.SELF

                // Update the txtBtnSplit text based on the selected split type
                updateSplitButtonText()

                Log.d("AddExpenseActivity", "Received Split Data: Shares=$splitMemberShares, Type=$selectedSplitType")
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            // User cancelled the split operation in ExpenseSplitterActivity
            // Reset to default SELF split instead of null
            initializeDefaultSelfSplit()
            updateSplitButtonText()
            Log.d("AddExpenseActivity", "Split activity cancelled. Reset to SELF split.")
        }
    }

    private val noteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val note = result.data?.getStringExtra("note_data")
            if (!note.isNullOrBlank()) {
                this.note = note
                // Optional: update UI with this note, e.g., binding.txtNotePreview.text = note
            }
        }
    }

    private val ImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                uri?.let {
                    selectedImageUri = it
                    isImageFromCamera = false // Gallery selection
                    capturedImageFile = null // Clear camera file reference
                    if(selectedImageUri!=null){
                        binding.cardBillPreview.visibility= View.VISIBLE
                        binding.imgBillPreview.setImageURI(it)
                    }else{
                        binding.cardBillPreview.visibility= View.GONE
                    }
                }
            }
        }

    private val CameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                photoUri?.let { uri ->
                    selectedImageUri = uri
                    isImageFromCamera = true // Camera capture
                    binding.cardBillPreview.visibility = View.VISIBLE
                    binding.imgBillPreview.setImageURI(uri)
                }
            }
        }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION_REQUEST_CODE = 2
        const val RESULT_OK_EXPENSE_ADDED = AppCompatActivity.RESULT_OK + 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupWebView()
        checkLocationPermission()

        // Initialize default self-split
        initializeDefaultSelfSplit()
        updateSplitButtonText()

        binding.btnCloseActivity.setOnClickListener {
            setResult(RESULT_OK_EXPENSE_ADDED)
            finish()
        }

        binding.btnNote.setOnClickListener {
            val intent = Intent(this, ExpenseNoteActivity::class.java)
            noteLauncher.launch(intent)
        }

        binding.btnCalender.setOnClickListener {
            binding.rellayoutCalender.visibility=View.VISIBLE
        }

        binding.btnSplit.setOnClickListener{
            if(binding.txtAmount.text.isEmpty()){
                Toast.makeText(this, "Please enter the amount first.", Toast.LENGTH_SHORT).show()
            }else{
                val intent = Intent(this, ExpenseSplitterActivity::class.java)
                intent.putExtra("totalAmount", binding.txtAmount.text.toString())
                // Use the new launcher to start ExpenseSplitterActivity
                expenseSplitterLauncher.launch(intent)
            }
        }

        binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Set time to 12:00 PM for consistency if storing as a timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Get the formatted date for display
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            val formattedDate = sdf.format(calendar.time)

            // Set formatted date in TextView
            binding.txtSelectedDate.text = formattedDate

            // Save the timestamp at 12:00 PM
            selectedDate = calendar.timeInMillis

            // Hide calendar
            binding.rellayoutCalender.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            if (IsEverythingNonEmpty()) {
                // Show loading state
                binding.btnSave.isEnabled = false
                binding.progressBar.visibility=View.VISIBLE

                lifecycleScope.launch {
                    try {
                        var imageUrl: String? = null

                        // Upload image if one is selected
                        selectedImageUri?.let { uri ->
                            try {
                                imageUrl = repo.uploadImageToCloudinary(uri, this@AddExpenseActivity)
                            } catch (e: Exception) {
                                Toast.makeText(this@AddExpenseActivity, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Continue without image if upload fails
                            }
                        }

                        // Convert SplitType enum to string, guaranteed to be non-null
                        val splitType = when (selectedSplitType) {
                            SplitType.EQUAL -> "Equally"
                            SplitType.EXACT_AMOUNT -> "ExactAmounts"
                            SplitType.PERCENTAGE -> "Percentages"
                            SplitType.SELF -> "Self"
                        }

                        // Ensure we have split member shares, create default if somehow null
                        val finalSplitMemberShares = splitMemberShares ?: createDefaultSelfSplit()

                        // Create expense details, including the split information
                        val expense = ExpensesDataClass(
                            "null", // Expense ID will be generated by Firebase
                            binding.txtDescription.text.toString(),
                            binding.txtAmount.text.toString(),
                            (GlobalClass.Me?.uid)!!,
                            (if (selectedDate == null) System.currentTimeMillis().toString() else selectedDate.toString()),
                            imageUrl,
                            savedLatitude,
                            savedLongitude,
                            note,
                            splitType, // This will always be "Self" if btnSplit was never clicked
                            finalSplitMemberShares // Pass the split shares
                        )

                        repo.pushExpenseToFirebase(expense)

                        // Clean up camera-captured image after saving
                        cleanupCameraImage()

                        Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                        // Set result and finish to go back to previous activity (e.g., Home or Segment Details)
                        setResult(RESULT_OK_EXPENSE_ADDED)
                        finish()

                    } catch (e: Exception) {
                        Toast.makeText(this@AddExpenseActivity, "Failed to save expense: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        // Reset button state regardless of success or failure
                        binding.btnSave.isEnabled = true
                        binding.progressBar.visibility=View.GONE
                    }
                }
            }
        }

        binding.btnCamera.setOnClickListener {
            showImageSourceSelectorBsFragment()
        }
    }

    /**
     * Initializes default self-split with current total amount or 0.0 if no amount entered yet
     */
    private fun initializeDefaultSelfSplit() {
        val totalAmountString = binding.txtAmount.text.toString()
        val totalAmount = totalAmountString.toDoubleOrNull() ?: 0.0

        val myUid = GlobalClass.Me?.uid ?: ""
        val myName = GlobalClass.Me?.name ?: "You"

        selectedSplitType = SplitType.SELF
        splitMemberShares = createDefaultSelfSplit(totalAmount)
    }

    /**
     * Creates default self split with given amount
     */
    private fun createDefaultSelfSplit(totalAmount: Double = 0.0): ArrayList<MemberShare> {
        val myUid = GlobalClass.Me?.uid ?: ""
        val myName = GlobalClass.Me?.name ?: "You"

        return ArrayList<MemberShare>().apply {
            add(
                MemberShare(
                    memberUid = myUid,
                    memberName = myName,
                    shareAmount = totalAmount,
                    shareType = SplitType.SELF,
                    originalInputValue = totalAmount
                )
            )
        }
    }

    /**
     * Updates the text of the split button based on the selected split type.
     * Also changes its color for visual feedback.
     */
    private fun updateSplitButtonText() {
        binding.txtbtnSplit.text = when (selectedSplitType) {
            SplitType.EQUAL -> "Equally"
            SplitType.EXACT_AMOUNT -> "Exact Amounts"
            SplitType.PERCENTAGE -> "Percentages"
            SplitType.SELF -> "Self"
        }
        // You might want to change the color here too for visual feedback
        // e.g., binding.txtbtnSplit.setTextColor(ContextCompat.getColor(this, R.color.green_accent))
    }

    /**
     * Deletes the camera-captured image file if it exists.
     * Only deletes images captured by camera, not gallery selections.
     */
    private fun cleanupCameraImage() {
        if (isImageFromCamera && capturedImageFile != null) {
            try {
                if (capturedImageFile!!.exists()) {
                    val deleted = capturedImageFile!!.delete()
                    if (deleted) {
                        Log.d("AddExpenseActivity", "Camera image file deleted successfully: ${capturedImageFile!!.absolutePath}")
                    } else {
                        Log.w("AddExpenseActivity", "Failed to delete camera image file: ${capturedImageFile!!.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error during camera image cleanup: ${e.message}", e)
            }
        }
    }

    private fun showImageSourceSelectorBsFragment() {
        val dialog = BottomSheetDialog(this).apply {
            setContentView(R.layout.bsfragment_picimportsource)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val camera = dialog.findViewById<RelativeLayout>(R.id.btnCamera)
        val gallery = dialog.findViewById<RelativeLayout>(R.id.btnGallery)

        gallery?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            ImagePickerLauncher.launch(intent)
            dialog.dismiss()
        }

        camera?.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Create a file to save the image
        photoUri = createImageFile()

        photoUri?.let { uri ->
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            CameraLauncher.launch(intent)
        }
    }

    private fun createImageFile(): Uri? {
        return try {
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            // Use getExternalFilesDir for app-specific, private storage that gets cleared on uninstall
            val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

            // Create the directory if it doesn't exist
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            // Store reference to the created file for later cleanup
            capturedImageFile = imageFile

            // Get the URI using FileProvider
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider", // Make sure this matches your manifest's <provider> authority
                imageFile
            )
        } catch (ex: Exception) {
            Toast.makeText(this, "Error creating image file: ${ex.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddExpenseActivity", "Error creating image file", ex)
            capturedImageFile = null
            null
        }
    }

    private fun setupWebView() {
        val webView = binding.webView // Make sure this matches your layout ID
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Handle touch events to prevent scroll view from intercepting map interactions
        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    // Prevent parent scroll view from intercepting touch events
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    // Allow parent scroll view to handle touch events again
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Let WebView handle the touch event
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Get current location after page loads
                getCurrentLocation()
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Load the HTML file from assets
        webView.loadUrl("file:///android_asset/addexpense_map.html")
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, but we call getCurrentLocation() after WebView loads
            // so no need to call it here directly.
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude

                    // Save location
                    savedLatitude = latitude
                    savedLongitude = longitude

                    // Show location on map using JavaScript
                    binding.webView.loadUrl("javascript:showLocationOnMap($latitude, $longitude)")

                    //Toast.makeText(this, "Current location loaded", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error getting location: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddExpenseActivity", "Error getting last location", it)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                    getCurrentLocation() // Try to get location after permission is granted
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera() // Open camera after permission is granted
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun IsEverythingNonEmpty(): Boolean {
        if(binding.txtDescription.text.isNullOrEmpty()){
            Toast.makeText(this, "Description field cannot be empty.", Toast.LENGTH_SHORT).show()
            return false
        }
        if(binding.txtAmount.text.isNullOrEmpty()){
            Toast.makeText(this, "Amount field cannot be empty.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up camera image if activity is destroyed without saving
        if (isImageFromCamera && capturedImageFile != null) {
            cleanupCameraImage()
        }
    }

    // JavaScript Interface for communication between WebView and Android
    inner class WebAppInterface {
        @JavascriptInterface
        fun saveLocation(latitude: Double, longitude: Double) {
            savedLatitude = latitude
            savedLongitude = longitude

            runOnUiThread {
                Toast.makeText(
                    this@AddExpenseActivity,
                    "Map location saved: $latitude, $longitude", // More descriptive toast
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}