package com.example.tournote.Functionality.Segments.Expenses.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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
import com.example.tournote.Functionality.Segments.Expenses.DataClass.MemberShare
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitType
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

class ExpenseAddActivity : AppCompatActivity() {
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

    // Flag to check if the expense is auto-detected
    private var isAutoDetectedExpense: Boolean = false

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

        // Constants for intent extras
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IS_AUTO_DETECTED = "extra_is_auto_detected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.taskbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupDescriptionTextWatcher()

        // Check if launched for auto-detected expense
        isAutoDetectedExpense = intent.getBooleanExtra(EXTRA_IS_AUTO_DETECTED, false)
        if (isAutoDetectedExpense) {
            val amount = intent.getStringExtra(EXTRA_AMOUNT)
            val description = intent.getStringExtra(EXTRA_DESCRIPTION)

            if(amount!="Unknown"){
                binding.txtAmount.setText(amount)
                binding.txtAmount.isEnabled = false
            }else{
                binding.txtAmount.isEnabled = true
            }

            binding.txtDescription.setText(description)


            // Optional: Disable editing for amount and description if auto-detected
            binding.txtDescription.isEnabled = false
            Toast.makeText(this, "Expense details pre-filled from SMS.", Toast.LENGTH_LONG).show()
        }



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
            if(binding.rellayoutCalender.visibility==View.VISIBLE){
                binding.rellayoutCalender.visibility=View.GONE
            }else{
                binding.rellayoutCalender.visibility=View.VISIBLE
            }
        }

        binding.btnSplit.setOnClickListener{
            if(binding.txtAmount.text.isEmpty()){
                Toast.makeText(this, "Please enter the amount first.", Toast.LENGTH_SHORT).show()
            }else{
                val intent = Intent(this, ExpenseSplitterActivity::class.java)
                intent.putExtra("totalAmount", binding.txtAmount.text.toString())
                // Pass current split data if available for re-editing
                splitMemberShares?.let {
                    intent.putParcelableArrayListExtra(ExpenseSplitterActivity.EXTRA_MEMBER_SHARES, it)
                    intent.putExtra(ExpenseSplitterActivity.EXTRA_SPLIT_TYPE, selectedSplitType.name)
                }
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
                                imageUrl = repo.uploadImageToCloudinary(uri, this@ExpenseAddActivity)
                            } catch (e: Exception) {
                                // Log the actual exception and its stack trace
                                Log.e("AddExpenseActivity", "Image upload failed: ${e.message}", e)
                                Toast.makeText(this@ExpenseAddActivity, "Image upload failed: ${e.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                                // Continue without image if upload fails
                            }
                        }

                        // Convert SplitType enum to string, guaranteed to be non-null
                        // This logic is slightly different from pushExpenseToFirebase, which infers from splitMembers
                        // We should pass the actual selectedSplitType name
                        val splitTypeString = selectedSplitType.name

                        // Ensure we have split member shares, create default if somehow null
                        val finalSplitMemberShares = splitMemberShares ?: createDefaultSelfSplit()

                        // Create expense details, including the split information
                        val expense = ExpensesDataClass(
                            null, // Expense ID will be generated by Firebase
                            binding.txtDescription.text.toString(),
                            binding.txtAmount.text.toString(),
                            (GlobalClass.Me?.uid)!!, // Ensure GlobalClass.Me.uid is not null here
                            (if (selectedDate == null) System.currentTimeMillis().toString() else selectedDate.toString()),
                            imageUrl,
                            savedLatitude,
                            savedLongitude,
                            note,
                            splitTypeString, // Use the selectedSplitType name directly
                            finalSplitMemberShares // Pass the split shares
                        )

                        // Add logging before pushing to Firebase
                        Log.d("AddExpenseActivity", "Attempting to push expense to Firebase: $expense")
                        repo.pushExpenseToFirebase(expense)

                        // Clean up camera-captured image after saving
                        cleanupCameraImage()

                        Toast.makeText(this@ExpenseAddActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()

                        // --- START OF MODIFICATION ---
                        if (isAutoDetectedExpense) {
                            // Close the entire application if the expense was auto-detected
                            finishAffinity()
                        } else {
                            // Otherwise, just finish this activity and return to the previous one
                            setResult(RESULT_OK_EXPENSE_ADDED)
                            finish()
                        }
                        // --- END OF MODIFICATION ---

                    } catch (e: Exception) {
                        // Log the actual exception and its stack trace here
                        Log.e("AddExpenseActivity", "Failed to save expense: ${e.message}", e)
                        Toast.makeText(this@ExpenseAddActivity, "Failed to save expense: ${e.message ?: "Unknown error occurred"}", Toast.LENGTH_LONG).show()
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
                    originalInputValue = totalAmount,
                    paid = false, // Default to false
                    partialPayment = 0.0 // Default to 0.0
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
                    this@ExpenseAddActivity,
                    "Map location saved: $latitude, $longitude", // More descriptive toast
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getExpenseImageResource(description: String): Int {
        val categoryKeywords = mapOf(
            "health" to listOf("clinic", "pharmacy", "wellness", "hospital", "dentist", "doctor", "remedy", "vaccine", "therapist", "physician", "checkup", "prescription", "lab", "examination", "medication", "sanatorium", "infirmary", "rehab", "nursing", "specialist", "pediatrician", "cardiologist", "therapy", "healing", "medical", "physio", "surgeon", "consultation", "x-ray", "screening", "diagnosis", "injection", "treatment", "bandage", "first-aid", "hygiene", "mental", "physiotherapy", "nutritionist", "optician", "chiropractor", "dermatology", "oncology", "geriatrics", "pathology", "radiology", "cardiology", "neurology", "urology", "endocrinology", "gastroenterology", "immunology", "psychiatry", "psychology", "obstetrics", "gynecology", "anesthesia", "pediatrics", "surgery", "euthanasia", "quarantine", "isolation", "epidemic", "pandemic", "vaccination", "immunization", "prophylaxis", "antibiotics", "antivirus", "analgesic", "antihistamine", "antidepressant", "psychotherapy", "counseling", "rehabilitation", "hospice", "palliative", "wellness", "fitness", "nutrition", "dietitian", "exercise", "acupuncture", "herbal", "homeopathy", "naturopathy", "aromatherapy", "meditation", "mindfulness", "yoga", "pilates", "aerobics", "cardio", "strength", "endurance", "flexibility", "stamina", "physique", "anatomy"),
            "taxi" to listOf("cab", "uber", "lyft", "ola", "ride", "shuttle", "transfer", "cabs", "minivan", "limo", "chauffeur", "carpool", "fare", "dispatch", "pickup", "dropoff", "hail", "commute", "vehicle", "taxicab", "blackcab", "minicab", "autorickshaw", "rickshaw", "tuk-tuk", "carriage", "buggy", "sedan", "suv", "van", "courier", "delivery", "privatehire", "airport", "railway", "station", "terminal", "destination", "route", "gps", "navigation", "meter", "tariff", "tip", "gratuity", "booking", "reservation", "account", "corporate", "voucher", "coupon", "discount", "promo", "app", "application", "driver", "passenger", "luggage", "baggage", "on-demand", "pre-booked", "scheduled", "shared", "pool", "express", "executive", "premium", "luxury", "economy", "standard", "electric", "hybrid", "gasoline", "diesel", "petrol", "fuel", "toll", "road", "trip", "journey", "itinerary", "tour", "sightseeing", "event", "nightlife", "party", "pub", "bar", "restaurant", "hotel", "motel", "hostel", "inn", "lodge", "resort", "guesthouse", "homestay"),
            "games" to listOf("arcade", "game", "casino", "boardgame", "console", "poker", "bingo", "esports", "videogame", "puzzle", "strategy", "dice", "cardgame", "tournament", "competition", "play", "gaming", "multiplayer", "single-player", "xbox", "playstation", "nintendo", "pc", "mobile", "virtual", "vr", "augmented", "ar", "online", "offline", "lan", "party", "solo", "co-op", "multiplayer", "mmo", "rpg", "fps", "rts", "simulation", "casual", "hyper-casual", "indie", "aaa", "retro", "classic", "vintage", "arcade", "pinball", "slot", "roulette", "blackjack", "craps", "baccarat", "keno", "lottery", "scratchcard", "prize", "jackpot", "winner", "loser", "bet", "wager", "ante", "pot", "hand", "deck", "chip", "token", "coin", "score", "level", "boss", "quest", "mission", "achievement", "trophy", "high-score", "leaderboard", "stream", "streaming", "twitch", "youtube", "mixer", "streamer", "gamer", "pro-gamer", "team", "clan", "guild", "esports", "league", "championship", "cup", "trophy", "medal", "award"),
            "tickets" to listOf("pass", "stub", "entry", "voucher", "admission", "permit", "reservation", "coupon", "credential", "booking", "gate", "seat", "standby", "receipt", "booking", "advance", "willcall", "boxoffice", "serial", "serial", "code", "barcode", "qr", "qr-code", "eticket", "mobile", "paper", "physical", "digital", "online", "offline", "pre-booked", "walk-in", "late-entry", "vip", "backstage", "meet&greet", "frontrow", "balcony", "stalls", "circle", "gallery", "standing", "seating", "general", "premium", "gold", "silver", "bronze", "platinum", "diamond", "family", "student", "senior", "child", "adult", "group", "corporate", "season", "annual", "monthly", "weekly", "daily", "single", "return", "oneway", "roundtrip", "flight", "train", "bus", "ferry", "cruise", "cinema", "theatre", "concert", "gig", "festival", "sports", "match", "game", "museum", "gallery", "exhibition", "attraction", "tour", "sightseeing", "event", "conference", "workshop", "seminar"),
            "sports" to listOf("gym", "yoga", "arena", "tennis", "golf", "pool", "match", "club", "court", "track", "baseball", "soccer", "basketball", "football", "swimming", "workout", "jogging", "running", "fitness", "training", "athlete", "racquet", "field", "stadium", "hockey", "cricket", "rugby", "volleyball", "badminton", "table", "tennis", "squash", "boxing", "wrestling", "judo", "karate", "taekwondo", "mma", "ufc", "cycling", "running", "marathon", "triathlon", "decathlon", "pentathlon", "sprint", "hurdle", "relay", "shotput", "discus", "javelin", "longjump", "highjump", "polevault", "gymnastics", "aerobics", "calisthenics", "crossfit", "pilates", "zumba", "hiit", "weightlifting", "powerlifting", "bodybuilding", "personal", "trainer", "coach", "instructor", "class", "session", "bootcamp", "league", "championship", "cup", "trophy", "medal", "award", "fan", "spectator", "crowd", "cheer", "applause", "whistle", "referee", "umpire", "linesman", "player", "team", "squad", "jersey", "kit", "equipment", "gear", "accessory", "nutrition", "hydration", "diet", "supplement"),
            "food" to listOf("meal", "snack", "dine", "cafe", "buffet", "brunch", "supper", "breakfast", "lunch", "dinner", "restaurant", "eatery", "bakery", "deli", "fastfood", "cuisine", "dish", "takeout", "delivery", "patisserie", "grill", "pizzeria", "caterer", "bistro", "pub", "bar", "kebab", "tapas", "dimsum", "sushi", "ramen", "pasta", "pizza", "burger", "sandwich", "salad", "soup", "stew", "curry", "rice", "noodles", "bread", "pastry", "cake", "cookie", "icecream", "gelato", "sorbet", "dessert", "appetizer", "starter", "maincourse", "entree", "side", "drink", "beverage", "coffee", "tea", "juice", "soda", "water", "beer", "wine", "cocktail", "liquor", "spirit", "whiskey", "vodka", "gin", "rum", "tequila", "brandy", "sake", "champagne", "prosecco", "cider", "ale", "stout", "lager", "espresso", "latte", "cappuccino", "macchiato", "americano", "frappe", "smoothie", "milkshake", "shake", "protein", "organic", "vegan", "vegetarian", "gluten-free", "lactose-free", "halal", "kosher", "pescatarian"),
            "services" to listOf("laundry", "spa", "guide", "repair", "booking", "massage", "salon", "internet", "cleaning", "delivery", "plumbing", "electrician", "haircut", "manicure", "pedicure", "webdesign", "consulting", "maintenance", "installation", "subscription", "support", "tutor", "accountant", "lawyer", "notary", "translator", "interpreter", "architect", "engineer", "designer", "developer", "programmer", "marketer", "advertiser", "pr", "publicrelations", "hr", "humanresources", "recruitment", "training", "coach", "mentor", "financial", "advisor", "insurance", "realestate", "mortgage", "loan", "tax", "legal", "medical", "veterinary", "pet", "grooming", "daycare", "kennel", "boarding", "housekeeping", "concierge", "valet", "doorman", "security", "guard", "patrol", "courier", "postal", "mail", "package", "shipping", "freight", "logistics", "storage", "self-storage", "movers", "removal", "pestcontrol", "landscaping", "gardening", "poolservice", "handyman", "carpenter", "painter", "roofer", "builder", "plasterer", "bricklayer", "welder", "mechanic"),
            "clothing" to listOf("apparel", "shirt", "dress", "jacket", "shoes", "jeans", "outfit", "sweater", "suit", "skirt", "pants", "trousers", "blouse", "hoodie", "tie", "socks", "sandals", "boots", "fashion", "boutique", "accessory", "hat", "cap", "glove", "scarf", "coat", "blazer", "vest", "jumper", "cardigan", "polo", "t-shirt", "tanktop", "shorts", "leggings", "tights", "bra", "panties", "underwear", "boxers", "briefs", "pyjamas", "pajamas", "nightgown", "robe", "swimsuit", "bikini", "trunks", "goggles", "watch", "belt", "handbag", "purse", "backpack", "wallet", "jewelry", "necklace", "earrings", "bracelet", "ring", "sunglasses", "eyeglasses", "chain", "cufflinks", "brooch", "pin", "tiepin", "shoelaces", "insole", "shoeshine", "tailor", "seamstress", "dryclean", "laundry", "ironing", "alteration", "repair", "hemming", "embroidery", "printing", "dyeing", "fabric", "material", "cotton", "linen", "silk", "wool", "denim", "leather", "suede", "velvet", "lace", "sequin", "bead", "zipper", "button", "snap"),
            "bus_train" to listOf("transit", "metro", "tram", "subway", "rail", "coach", "shuttle", "ticket", "pass", "commuter", "locomotive", "carriage", "conductor", "platform", "station", "line", "route", "express", "underground", "overground", "overhead", "intercity", "interstate", "international", "local", "rapid", "busway", "streetcar", "trolley", "doubledecker", "singledecker", "minibus", "schoolbus", "tourbus", "coach", "sleeper", "cabin", "berth", "seat", "aisle", "window", "timetable", "schedule", "delay", "cancellation", "strike", "fare", "tariff", "zone", "oneway", "return", "roundtrip", "season", "annual", "monthly", "weekly", "daily", "single", "group", "family", "student", "senior", "child", "adult", "luggage", "baggage", "cargo", "freight", "terminal", "depot", "garage", "stop", "station", "junction", "intersection", "signal", "track", "gauge", "railroad", "locomotive", "engine", "driver", "operator", "guard", "inspector", "police", "security", "onboard", "offboard", "transfer", "connection", "itinerary", "journey", "trip", "tour", "sightseeing"),
            "hotel" to listOf("inn", "lodge", "hostel", "resort", "stay", "suite", "guesthouse", "motel", "homestay", "villa", "boardinghouse", "accommodations", "reservation", "frontdesk", "concierge", "bedandbreakfast", "caravan", "chalet", "apartment", "serviced", "holiday", "vacation", "rental", "airbnb", "agoda", "booking", "expedia", "tripadvisor", "host", "guest", "checkin", "checkout", "early", "late", "keycard", "roomkey", "doorman", "valet", "bellboy", "porter", "housekeeping", "maid", "cleaner", "laundry", "dryclean", "ironing", "minibar", "safe", "tv", "wifi", "internet", "phone", "roomservice", "restaurant", "bar", "pub", "cafe", "pool", "gym", "spa", "sauna", "jacuzzi", "steamroom", "massage", "facial", "pedicure", "manicure", "haircut", "salon", "business", "center", "meeting", "room", "conference", "banquet", "wedding", "event", "party", "nightclub", "lounge", "rooftop", "terrace", "balcony", "view", "ocean", "mountain", "city", "garden", "lake", "river", "forest"),
            "parking" to listOf("garage", "lot", "meter", "valet", "bay", "space", "deck", "stall", "spot", "parkade", "ramp", "curb", "driveway", "underground", "permit", "violation", "fine", "ticket", "clamp", "tow", "towaway", "no", "parking", "reserved", "disabled", "handicap", "ev", "electric", "charging", "charger", "station", "evse", "ic", "ice", "internal", "combustion", "engine", "gasoline", "diesel", "petrol", "fuel", "motorcycle", "bike", "bicycle", "car", "truck", "van", "bus", "lorry", "trailer", "campervan", "motorhome", "rv", "recreational", "vehicle", "public", "private", "on-street", "off-street", "multistorey", "surface", "airport", "station", "terminal", "hotel", "restaurant", "shopping", "mall", "supermarket", "hospital", "clinic", "school", "university", "campus", "business", "district", "cbd", "downtown", "city", "center", "suburb", "rural", "countryside", "urban", "town", "village", "street", "road", "avenue", "lane", "drive"),
            "flight" to listOf("plane", "airline", "airfare", "jet", "baggage", "boarding", "ticket", "airport", "runway", "terminal", "checkin", "gate", "takeoff", "landing", "stewardess", "pilot", "destination", "departure", "arrival", "layover", "charter", "domestic", "international", "oneway", "return", "roundtrip", "multicity", "firstclass", "business", "economy", "premium", "seat", "aisle", "window", "extra", "legroom", "overhead", "bin", "carryon", "checked", "handluggage", "excess", "weight", "fee", "compensation", "delay", "cancellation", "strike", "security", "passport", "visa", "customs", "immigration", "dutyfree", "lounge", "gate", "terminal", "concourse", "runway", "taxiway", "apron", "hangar", "control", "tower", "cockpit", "cabin", "inflight", "entertainment", "meal", "snack", "drink", "beverage", "wifi", "power", "adapter", "blanket", "pillow", "headphone", "mask", "earplug", "eyes", "shade", "book", "magazine"),
            "household" to listOf("toiletries", "soap", "detergent", "utensil", "supplies", "linen", "bedding", "cleaning", "crockery", "furniture", "appliances", "dishes", "cookware", "silverware", "hardware", "cutlery", "towel", "bathmat", "vase", "picture", "frame", "mirror", "lamp", "light", "bulb", "candle", "diffuser", "rug", "carpet", "curtain", "blinds", "cushion", "pillow", "blanket", "quilt", "duvet", "sheets", "pillowcase", "bedspread", "tablecloth", "napkin", "placemat", "coaster", "mop", "broom", "dustpan", "vacuum", "cleaner", "washer", "dryer", "dishwasher", "microwave", "oven", "fridge", "freezer", "kettle", "toaster", "blender", "mixer", "foodprocessor", "iron", "ironing", "board", "clothes", "rack", "hanger", "storage", "box", "basket", "bin", "container", "shelf", "cabinet", "drawer", "wardrobe", "closet", "door", "window", "key", "lock", "alarm", "security", "camera", "fire", "extinguisher", "smoke", "detector", "carbon", "monoxide"),
            "music" to listOf("concert", "album", "gig", "festival", "record", "band", "karaoke", "track", "cd", "vinyl", "playlist", "artist", "livemusic", "performance", "symphony", "orchestra", "auditorium", "melody", "song", "juke", "radio", "spotify", "applemusic", "deezer", "tidal", "youtube", "music", "video", "clip", "mp3", "flac", "wav", "streaming", "download", "purchase", "subscription", "podcast", "radio", "station", "dj", "mixer", "turntable", "instrument", "guitar", "piano", "keyboard", "drums", "bass", "violin", "cello", "flute", "clarinet", "saxophone", "trumpet", "trombone", "harmonica", "microphone", "speaker", "headphone", "earphone", "amplifier", "sound", "system", "studio", "recording", "mixing", "mastering", "producer", "engineer", "label", "recordcompany", "publisher", "royalties", "licensing", "copyright", "ip", "intellectual", "property", "genre", "pop", "rock", "jazz", "blues", "country", "folk", "hiphop", "r&b", "electronic", "dance", "edm", "techno", "house", "trance", "dubstep", "metal", "punk", "indie", "alternative", "classical", "opera", "salsa", "reggae"),
            "fuel" to listOf("gas", "petrol", "diesel", "refuel", "charge", "charging", "fillup", "electric", "unleaded", "octane", "pump", "tank", "station", "gasoline", "ethanol", "hydrogen", "energy", "petroleum", "gasoil", "lpg", "cng", "biofuel", "biodiesel", "battery", "ev", "electric", "vehicle", "supercharger", "fast", "slow", "home", "public", "station", "plug", "socket", "connector", "type1", "type2", "ccs", "chademo", "tesla", "supercharger", "destination", "charger", "watt", "kilowatt", "ampere", "volt", "power", "grid", "smart", "home", "solar", "panel", "wind", "turbine", "hydro", "geothermal", "nuclear", "coal", "oil", "gas", "pipeline", "refinery", "storage", "tank", "delivery", "truck", "bowser", "nozzle", "forecourt", "attendant", "selfservice", "prepay", "payatpump", "card", "cash", "loyalty", "program", "points", "coupon", "discount", "promo", "voucher", "receipt", "invoice", "vat", "tax", "duty"),
            "grocery" to listOf("market", "store", "supermart", "produce", "pantry", "provision", "mart", "basket", "supermarket", "vegetables", "fruits", "meat", "dairy", "checkout", "aisle", "cart", "shopping", "butcher", "bakery", "delicatessen", "farm", "shop", "convenience", "corner", "online", "delivery", "pickup", "click&collect", "order", "list", "receipt", "invoice", "coupon", "voucher", "discount", "promo", "loyalty", "program", "points", "card", "cash", "contactless", "apple", "pay", "google", "pay", "debit", "credit", "card", "shelf", "stock", "aisle", "section", "produce", "meat", "fish", "seafood", "dairy", "eggs", "bakery", "bread", "pastry", "cake", "cereal", "grains", "pasta", "rice", "sauce", "canned", "goods", "frozen", "food", "beverages", "snacks", "sweets", "chocolate", "chips", "crisps", "nuts", "seeds", "spices", "herbs", "condiments", "oil", "vinegar", "tea", "coffee", "juice", "soda", "water", "beer", "wine", "spirit", "cleaning", "supplies", "toiletries", "personal", "care", "pet", "food", "baby", "products", "health", "wellness"),
            "liquor" to listOf("wine", "beer", "whiskey", "spirits", "vodka", "rum", "bar", "brewery", "cocktail", "tequila", "gin", "brandy", "sake", "pub", "tavern", "distillery", "cider", "ale", "stout", "lager", "porter", "ipa", "pilsner", "weissbier", "saison", "gose", "sour", "blonde", "brown", "red", "black", "stout", "porter", "ipa", "pilsner", "weissbier", "saison", "gose", "sour", "blonde", "brown", "red", "black", "ale", "wine", "redwine", "whitewine", "rose", "sparkling", "champagne", "prosecco", "cava", "moscato", "sauvignon", "chardonnay", "pinot", "noir", "merlot", "cabernet", "syrah", "zinfandel", "malbec", "riesling", "pinotgrigio", "pinotblanc", "gewurztraminer", "viognier", "chardonnay", "whiskey", "scotch", "irish", "bourbon", "rye", "tennessee", "japanese", "canadian", "single", "malt", "blended", "grain", "barrel", "proof", "bottle", "case", "sixpack", "pint", "can", "glass", "shot", "cocktail", "shaker", "jigger", "strainer", "ice", "cube", "mixer", "soda", "tonic", "juice", "garnish", "lemon", "lime", "orange", "cherry", "olive", "salt", "sugar"),
            "gift" to listOf("present", "token", "memento", "souvenir", "package", "parcel", "hamper", "card", "gifting", "wrapping", "birthday", "anniversary", "holiday", "keepsake", "bouquet", "voucher", "donation", "charity", "tribute", "giftcard", "gift", "certificate", "egift", "online", "offline", "physical", "digital", "email", "sms", "text", "message", "voucher", "code", "barcode", "qr", "qr-code", "gift", "box", "bag", "ribbon", "bow", "tag", "paper", "wrapping", "tape", "scissors", "pen", "marker", "card", "greeting", "thankyou", "getwell", "congratulations", "sympathy", "condolence", "love", "romance", "friendship", "family", "corporate", "promotional", "merchandise", "swag", "giveaway", "freebie", "prize", "award", "trophy", "medal", "plaque", "certificate", "honor", "recognition", "appreciation", "gratitude", "tribute", "memory", "memorial", "inloving", "memory", "donation", "charity", "fundraiser", "crowdfunding", "contribution", "support", "sponsor", "patron", "benefactor", "donor", "alumni", "foundation", "nonprofit", "volunteer", "cause", "mission", "purpose", "social", "enterprise")
        )

        val lowerDesc = description.lowercase()

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerDesc.contains(it.lowercase()) }) {
                val resourceName = "expense_$category"
                val resId = getDrawableResourceByName(resourceName)
                if (resId != 0) return resId
            }
        }

        return getDrawableResourceByName("expense_other")
    }

    // Helper function to get drawable resource by name
    fun getDrawableResourceByName(name: String): Int {
        return try {
            val resId = R.drawable::class.java.getField(name).getInt(null)
            resId
        } catch (e: Exception) {
            R.drawable.expense_other
        }
    }

    private fun setupDescriptionTextWatcher() {
        binding.txtDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Update category image whenever description changes
                val description = s?.toString() ?: ""
                val imageResource = getExpenseImageResource(description)

                // Make sure imgexpenseCategory exists in your layout
                try {
                    binding.imgexpenseCategory.setImageResource(imageResource)
                    Log.d("ExpenseCategory", "Image updated for description: $description, resource: $imageResource")
                } catch (e: Exception) {
                    Log.e("ExpenseCategory", "Error setting image resource: ${e.message}", e)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


}