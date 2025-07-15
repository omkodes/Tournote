package com.example.tournote.Functionality.Segments.TrackFriends

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.tournote.R
import android.util.Log
import android.webkit.WebChromeClient
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.tournote.GlobalClass
import com.example.tournote.databinding.FragmentTrackFriendsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TrackFriendsFragment : Fragment() {

    private var _binding: FragmentTrackFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrackFriendsViewModel by activityViewModels()
    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var cancellationTokenSource: CancellationTokenSource? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Add flags to prevent multiple reloads
    private var isWebViewInitialized = false
    private var webViewLoadAttempts = 0
    private val maxWebViewLoadAttempts = 3

    private val locationPermissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackFriendsBinding.inflate(inflater, container, false)

        if(GlobalClass.isTracking){
            binding.relTrackingReqManualOverride.visibility=View.GONE
            if (GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.isGroupValid == false) {
                binding.relGroupInvalid.visibility = View.VISIBLE
            } else {
                binding.relGroupInvalid.visibility = View.GONE

                webView = binding.WebView
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

                setupLocationRequest()
                setupWebView()
                setupObservers()
                setupClickListeners()

                // Start tracking friends when the fragment's view is created and group is valid
                viewModel.startTrackingFriendsInGroup()
            }
        }else{
            binding.relTrackingReqManualOverride.visibility=View.VISIBLE
            binding.relPermissions.visibility=View.GONE
            binding.relWebView.visibility=View.GONE
            binding.relGroupInvalid.visibility=View.GONE
        }


        return binding.root
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()
    }

    private fun setupObservers() {
        viewModel.showPermissionRequest.observe(viewLifecycleOwner) { showPermission ->
            binding.relPermissions.visibility = if (showPermission) View.VISIBLE else View.GONE
        }

        viewModel.showMapView.observe(viewLifecycleOwner) { showMap ->
            binding.relWebView.visibility = if (showMap) View.VISIBLE else View.GONE
            if (showMap && !isWebViewInitialized) {
                requestLocationPermission()
            }
        }

        viewModel.isWebViewReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady) {
                Log.d("TrackFriendsFragment", "WebView is ready. Pushing pending location updates.")
                viewModel.currentLocation.value?.let { location ->
                    viewModel.updateCurrentLocation(location)
                }
            }
        }

        viewModel.webViewCommand.observe(viewLifecycleOwner) { command ->
            command?.let {
                executeWebViewCommand(it)
                viewModel.clearWebViewCommand()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.friendsOnMap.observe(viewLifecycleOwner) { friendsList ->
            Log.d("TrackFriendsFragment", "Friends on map updated: ${friendsList.size} friends")
        }
    }

    private fun setupClickListeners() {
        binding.btnAlert.setOnClickListener {
            viewModel.showAlertAPI()
        }

        binding.btnPermission.setOnClickListener {
            viewModel.onPermissionGranted()
        }
    }

    private fun setupWebView() {
        if (isWebViewInitialized) {
            return // Prevent multiple initializations
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            cacheMode = WebSettings.LOAD_NO_CACHE // Prevent caching issues
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // Enable remote debugging for WebView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("TrackFriendsFragment", "WebView started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("TrackFriendsFragment", "WebView finished loading: $url")
                isWebViewInitialized = true
                webViewLoadAttempts = 0 // Reset attempts on successful load
                viewModel.onWebViewPageFinished()
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                val errorDescription = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    error?.description?.toString() ?: "Unknown error"
                } else {
                    "WebView error occurred"
                }

                Log.e("TrackFriendsFragment", "WebView error: $errorDescription for ${request?.url}")

                // Only handle main frame errors, not resource errors
                if (request?.isForMainFrame == true) {
                    handleWebViewError(errorDescription)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e("TrackFriendsFragment", "WebView error (legacy): $description for $failingUrl")
                handleWebViewError(description ?: "Unknown error")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.apply {
                    Log.d("WebViewConsole", "${message()} -- From ${sourceId()}:${lineNumber()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Load the HTML file
        loadWebViewContent()
    }

    private fun loadWebViewContent() {
        try {
            // Check if the file exists in assets
            val assetManager = requireContext().assets
            val inputStream = assetManager.open("track_friends_map.html")
            inputStream.close() // File exists, we can load it

            val url = "file:///android_asset/track_friends_map.html"
            Log.d("TrackFriendsFragment", "Loading WebView URL: $url")
            webView.loadUrl(url)

        } catch (e: Exception) {
            Log.e("TrackFriendsFragment", "HTML file not found in assets: ${e.message}")
            viewModel.setErrorMessage("Map file not found. Please check the installation.")
        }
    }

    private fun handleWebViewError(description: String) {
        webViewLoadAttempts++

        if (webViewLoadAttempts < maxWebViewLoadAttempts) {
            Log.w("TrackFriendsFragment", "WebView load attempt $webViewLoadAttempts failed. Retrying...")

            // Wait before retrying
            mainHandler.postDelayed({
                if (isAdded && !isDetached && _binding != null) {
                    loadWebViewContent()
                }
            }, 2000) // 2 second delay
        } else {
            Log.e("TrackFriendsFragment", "WebView failed to load after $maxWebViewLoadAttempts attempts")
            viewModel.onWebViewError("Error loading map: $description")
        }
    }

    private fun executeWebViewCommand(command: TrackFriendsViewModel.WebViewCommand) {
        if (!isWebViewInitialized) {
            Log.w("TrackFriendsFragment", "WebView not initialized, skipping command")
            return
        }

        val javascript = when (command) {
            is TrackFriendsViewModel.WebViewCommand.UpdateUserLocation -> {
                """
                if (typeof setUserLocation === 'function') {
                    setUserLocation(${command.latitude}, ${command.longitude}, '${command.profilePicUrl}');
                } else {
                    console.log('setUserLocation function not available yet');
                }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.AddFriendMarker -> {
                """
                if (typeof addFriendMarker === 'function') {
                    addFriendMarker(${command.id}, '${command.name}', ${command.lat}, ${command.lng}, '${command.status}', '${command.profilePicUrl}');
                } else {
                    console.log('addFriendMarker function not available yet');
                }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.UpdateFriendLocation -> {
                """
                if (typeof updateFriendLocation === 'function') {
                    updateFriendLocation(${command.id}, ${command.lat}, ${command.lng}, '${command.status}', '${command.profilePicUrl}');
                } else {
                    console.log('updateFriendLocation function not available yet');
                }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.RemoveFriendMarker -> {
                """
                if (typeof removeFriendMarker === 'function') {
                    removeFriendMarker(${command.id});
                } else {
                    console.log('removeFriendMarker function not available yet');
                }
                """.trimIndent()
            }
        }

        webView.post {
            webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                override fun onReceiveValue(result: String?) {
                    Log.d("WebViewJS", "Command executed: ${command.javaClass.simpleName}, Result: $result")
                }
            })
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("TrackFriendsFragment", "Location permission already granted.")
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d("TrackFriendsFragment", "Location permission rationale needed.")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermissionRequestCode
                )
            }
            else -> {
                Log.d("TrackFriendsFragment", "Requesting location permission.")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermissionRequestCode
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (!checkLocationPermission()) {
            Log.w("TrackFriendsFragment", "Location permission not granted.")
            return
        }

        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null && viewModel.isLocationRecent(lastLocation)) {
                Log.d("TrackFriendsFragment", "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                viewModel.updateCurrentLocation(lastLocation)
            } else {
                Log.d("TrackFriendsFragment", "Requesting fresh current location.")
                requestCurrentLocationWithRetry()
            }
        }.addOnFailureListener { e ->
            Log.w("TrackFriendsFragment", "Failed to get last known location: ${e.message}")
            requestCurrentLocationWithRetry()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestCurrentLocationWithRetry() {
        if (viewModel.getCurrentRetryCount() >= viewModel.maxLocationRetries) {
            Log.e("TrackFriendsFragment", "Max location retries reached. Starting continuous updates.")
            startLocationUpdates()
            return
        }

        Log.d("TrackFriendsFragment", "Attempting getCurrentLocation (retry ${viewModel.getCurrentRetryCount() + 1})")

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("TrackFriendsFragment", "Current location obtained: ${it.latitude}, ${it.longitude}")
                viewModel.updateCurrentLocation(it)
                stopLocationUpdates()
            } ?: run {
                Log.w("TrackFriendsFragment", "Current location is null, retrying...")
                lifecycleScope.launch {
                    delay(2000)
                    if (isAdded && _binding != null) {
                        requestCurrentLocationWithRetry()
                    }
                }
            }
        }.addOnFailureListener { exception ->
            viewModel.onLocationUpdateFailed(exception)
            if (viewModel.shouldRetryLocation()) {
                Log.w("TrackFriendsFragment", "getCurrentLocation failed, retrying...")
                lifecycleScope.launch {
                    delay(2000)
                    if (isAdded && _binding != null) {
                        requestCurrentLocationWithRetry()
                    }
                }
            } else {
                Log.e("TrackFriendsFragment", "getCurrentLocation failed after retries. Starting continuous updates.")
                startLocationUpdates()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!checkLocationPermission()) {
            Log.w("TrackFriendsFragment", "Location permission not granted.")
            return
        }

        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let { location ->
                        Log.d("TrackFriendsFragment", "Continuous location update: ${location.latitude}, ${location.longitude}")
                        viewModel.updateCurrentLocation(location)
                    }
                }
            }
        }

        locationRequest?.let { request ->
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                Log.d("TrackFriendsFragment", "Location updates started successfully.")
            }.addOnFailureListener { e ->
                Log.e("TrackFriendsFragment", "Failed to request location updates: ${e.message}")
                viewModel.setErrorMessage("Failed to start location updates: ${e.message}")
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
                .addOnSuccessListener {
                    Log.d("TrackFriendsFragment", "Location updates stopped.")
                }
                .addOnFailureListener { e ->
                    Log.e("TrackFriendsFragment", "Failed to stop location updates: ${e.message}")
                }
        }
        locationCallback = null
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TrackFriendsFragment", "Location permission granted by user.")
                viewModel.onLocationPermissionGranted()
                getCurrentLocation()
            } else {
                Log.w("TrackFriendsFragment", "Location permission denied by user.")
                viewModel.onLocationPermissionDenied()
                Toast.makeText(requireContext(), "Location permission is required to track friends.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if fusedLocationClient is initialized before using it
        if (::fusedLocationClient.isInitialized &&
            viewModel.showMapView.value == true &&
            checkLocationPermission()) {
            getCurrentLocation()
        }

        // Only start tracking if we're in tracking mode and group is valid
        if (GlobalClass.isTracking &&
            GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.isGroupValid != false) {
            viewModel.startTrackingFriendsInGroup()
        }
    }

    override fun onPause() {
        super.onPause()

        // Only stop location updates if fusedLocationClient is initialized
        if (::fusedLocationClient.isInitialized) {
            stopLocationUpdates()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Cancel any pending location requests
        cancellationTokenSource?.cancel()

        // Clean up WebView
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }

        // Clean up binding
        _binding = null

        // Reset WebView state
        isWebViewInitialized = false
        webViewLoadAttempts = 0
    }

    // Public methods for external access
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        viewModel.addFriendOnMap(id, name, lat, lng, status, profilePicUrl)
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        viewModel.updateFriendOnMap(id, lat, lng, status, profilePicUrl)
    }

    fun removeFriendFromMap(id: Int) {
        viewModel.removeFriendFromMap(id)
    }

    private fun showAuthByEmailPassBottomSheet() {
        val dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bsfragment_emailpass)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
        val txtEnteredPass = dialog.findViewById<TextView>(R.id.txtEnteredPass)
        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnCnfrm)
    }
}