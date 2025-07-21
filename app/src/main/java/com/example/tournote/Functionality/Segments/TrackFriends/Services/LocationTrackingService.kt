package com.example.tournote.Functionality.Segments.TrackFriends.Services

import android.Manifest
import android.R
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.tournote.ApplicationClass
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

/**
 * LocationTrackingService - A foreground service that tracks user location
 * and uploads it to Firebase Realtime Database every 5 seconds with notifications
 */
class LocationTrackingService : Service() {

    // Location client for requesting location updates
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Firebase database reference
    private lateinit var databaseRef: DatabaseReference

    // Firebase authentication instance
    private lateinit var firebaseAuth: FirebaseAuth

    // Handler for scheduling periodic location updates
    private val handler = Handler(Looper.getMainLooper())

    // Location request configuration
    private lateinit var locationRequest: LocationRequest

    // Location callback to handle location updates
    private lateinit var locationCallback: LocationCallback

    // Runnable for periodic location fetching
    private lateinit var locationRunnable: Runnable

    // Flag to track if location updates are active
    private var isLocationUpdateActive = false

    // Counter for successful uploads
    private var uploadCount = 0

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds

        // Service actions
        const val ACTION_START_LOCATION_TRACKING = "START_LOCATION_TRACKING"
        const val ACTION_STOP_LOCATION_TRACKING = "STOP_LOCATION_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("locations")

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configure location request parameters
        setupLocationRequest()

        // Setup location callback
        setupLocationCallback()

        // Setup periodic location fetching
        setupLocationRunnable()

        Log.d(TAG, "LocationTrackingService created")
    }

    /**
     * Configure location request with desired accuracy and intervals
     */
    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    /**
     * Setup callback to handle location updates from FusedLocationProviderClient
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    uploadLocationToFirebase(location)
                }
            }

            /*override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    Log.w(TAG, "Location is not available")
                    // Show error notification
                    showErrorNotification("Location is not available")
                }
            }*/
        }
    }

    /**
     * Setup runnable for periodic location requests
     */
    private fun setupLocationRunnable() {
        locationRunnable = object : Runnable {
            override fun run() {
                requestCurrentLocation()
                // Schedule next location request after 5 seconds
                handler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Handle different actions
        when (intent?.action) {
            ACTION_STOP_LOCATION_TRACKING -> {
                stopLocationTracking()
                return START_NOT_STICKY
            }
            else -> {
                // Check if user is authenticated
                if (firebaseAuth.currentUser == null) {
                    Log.e(TAG, "User not authenticated. Stopping service.")
                    showErrorNotification("User not authenticated")
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Start foreground service with notification
                startForegroundService()

                // Start location tracking
                startLocationTracking()
            }
        }

        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    /**
     * Start the service as a foreground service with notification
     */
    private fun startForegroundService() {
        // Create notification with stop action
        val notification = createLocationTrackingNotification()

        // Start foreground service
        startForeground(ApplicationClass.Companion.LOCATION_NOTIFICATION_ID, notification.build())

        Log.d(TAG, "Foreground service started")
    }

    /**
     * Create notification for location tracking
     */
    private fun createLocationTrackingNotification(): NotificationCompat.Builder {
        // Intent to open the app when notification is clicked
        val openAppIntent = Intent(this, ApplicationClass::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_LOCATION_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get base notification from ApplicationClass
        val baseNotification = ApplicationClass.Companion.instance.createLocationTrackingNotification()

        return baseNotification
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel, // Using system icon
                "Stop Tracking",
                stopPendingIntent
            )
    }

    /**
     * Update notification with current status
     */
    private fun updateNotification() {
        val notification = createLocationTrackingNotification()
            .setContentText("Uploaded $uploadCount locations • Last: ${getCurrentTime()}")

        ApplicationClass.Companion.instance.getNotificationManager()
            .notify(ApplicationClass.Companion.LOCATION_NOTIFICATION_ID, notification.build())
    }

    /**
     * Show error notification
     */
    private fun showErrorNotification(errorMessage: String) {
        val errorNotification = ApplicationClass.Companion.instance.createLocationErrorNotification(errorMessage)

        ApplicationClass.Companion.instance.getNotificationManager()
            .notify(ApplicationClass.Companion.LOCATION_NOTIFICATION_ID + 1, errorNotification.build())
    }

    /**
     * Get current time as formatted string
     */
    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }

    /**
     * Start location tracking by requesting location updates
     */
    private fun startLocationTracking() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permissions not granted")
            showErrorNotification("Location permissions not granted")
            stopSelf()
            return
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Start periodic location requests
        handler.post(locationRunnable)
        isLocationUpdateActive = true

        Log.d(TAG, "Location tracking started")
    }

    /**
     * Stop location tracking
     */
    private fun stopLocationTracking() {
        if (isLocationUpdateActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            handler.removeCallbacks(locationRunnable)
            isLocationUpdateActive = false
        }

        stopForeground(true)
        stopSelf()

        Log.d(TAG, "Location tracking stopped")
    }

    /**
     * Request current location and upload to Firebase
     */
    private fun requestCurrentLocation() {
        // Check permissions again
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permissions not granted")
            showErrorNotification("Location permissions not granted")
            return
        }

        // Get last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                uploadLocationToFirebase(it)
            } ?: Log.w(TAG, "Last known location is null")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get last location: ${exception.message}")
            showErrorNotification("Failed to get location: ${exception.message}")
        }
    }

    /**
     * Upload location data to Firebase Realtime Database
     * Format: location/uid/lat, lng, timestamp
     */
    private fun uploadLocationToFirebase(location: Location) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated")
            showErrorNotification("User not authenticated")
            return
        }

        val uid = currentUser.uid
        val timestamp = System.currentTimeMillis()

        // Create location data map according to specified format
        val locationData = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to timestamp,
            "itIsRefreshing" to true
        )

        // Upload to Firebase: location/uid/
        databaseRef.child(uid).setValue(locationData)
            .addOnSuccessListener {
                uploadCount++
                Log.d(TAG, "Location uploaded successfully: lat=${location.latitude}, lng=${location.longitude}")

                // Update notification with success count
                updateNotification()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to upload location: ${exception.message}")
                showErrorNotification("Upload failed: ${exception.message}")
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop location updates
        stopLocationTracking()

        Log.d(TAG, "LocationTrackingService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }
}