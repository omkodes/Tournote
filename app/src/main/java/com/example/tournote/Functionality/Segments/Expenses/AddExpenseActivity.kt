package com.example.tournote.Functionality.Segments.Expenses

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.TextView
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
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.activityProfileInfo
import com.example.tournote.R
import com.example.tournote.UserModel
import com.example.tournote.databinding.ActivityAddExpenseBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private val repo = ExpensesRepository()
    private var savedLatitude: Double? = null
    private var savedLongitude: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null
    private var isImageFromCamera: Boolean = false // Track image source
    private var capturedImageFile: File? = null // Store reference to captured image file

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.green_theme_Light_taskbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupWebView()
        checkLocationPermission()

        binding.btnCloseActivity.setOnClickListener {
            finish()
        }

        binding.btnCalender.setOnClickListener {

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
                                // Continue without image
                            }
                        }

                        // Create expense details
                        val expense = ExpensesDataClass(
                            binding.txtDescription.text.toString(),
                            binding.txtAmount.text.toString(),
                            (GlobalClass.Me?.uid)!!,
                            System.currentTimeMillis().toString(),
                            imageUrl,// Add image URL to expense data
                            savedLatitude,
                            savedLongitude
                        )

                        repo.pushExpenseToFirebase(expense)

                        // Clean up camera-captured image after saving
                        cleanupCameraImage()

                        Toast.makeText(this@AddExpenseActivity, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                        finish()

                    } catch (e: Exception) {
                        Toast.makeText(this@AddExpenseActivity, "Failed to save expense: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        // Reset button state
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
     * Deletes the camera-captured image file if it exists
     * Only deletes images captured by camera, not gallery selections
     */
    private fun cleanupCameraImage() {
        if (isImageFromCamera && capturedImageFile != null) {
            try {
                if (capturedImageFile!!.exists()) {
                    val deleted = capturedImageFile!!.delete()
                    if (deleted) {
                        // Optional: Log success or show debug message
                        // Toast.makeText(this, "Camera image cleaned up", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Handle deletion error silently or log it
                e.printStackTrace()
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
            val storageDir = File(filesDir, "images")

            // Create the directory if it doesn't exist
            if (!storageDir.exists()) {
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
                "${packageName}.fileprovider",
                imageFile
            )
        } catch (ex: Exception) {
            Toast.makeText(this, "Error creating image file: ${ex.message}", Toast.LENGTH_SHORT).show()
            capturedImageFile = null
            null
        }
    }

    private fun setupWebView() {
        val webView = binding.webView // Make sure this matches your layout
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
            // Permission already granted, but wait for WebView to load
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

                    // Show location on map
                    binding.webView.loadUrl("javascript:showLocationOnMap($latitude, $longitude)")

                    //Toast.makeText(this, "Current location loaded", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error getting location: ${it.message}", Toast.LENGTH_SHORT).show()
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
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun IsEverythingNonEmpty(): Boolean {
        return binding.txtDescription.text.isNotEmpty() && binding.txtAmount.text.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up camera image if activity is destroyed without saving
        // This handles cases where user exits without saving
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
                    "Location saved: $latitude, $longitude",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}