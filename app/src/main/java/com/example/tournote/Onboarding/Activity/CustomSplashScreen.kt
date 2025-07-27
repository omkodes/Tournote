package com.example.tournote.Onboarding.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.example.tournote.Onboarding.ViewModel.authViewModel
import kotlinx.coroutines.launch

class CustomSplashScreen : AppCompatActivity() {
    private val authViewModel: authViewModel by viewModels()
    val repo = MainActivityRepository()

    private val fixedSplashDurationForGettingStarted = 5000L
    // Removed maxSplashDurationFallback - no time limit for logged-in users

    // Add this constant for SharedPreferences key
    private val PREF_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
    private val PREF_SMS_READER_ENAMBELD = "sms_enabled"

    private var splashStartTime = 0L
    private var dataLoadingComplete = false
    private var hasRedirected = false // Flag to prevent multiple redirections

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.navigationBarColor = ContextCompat.getColor(this, R.color.customSplashScreenBackground)

        GlobalClass.isTracking=loadLocationTrackingPreference()
        GlobalClass.isSmsWatched=loadSmsWatcherPreference()

        splashStartTime = System.currentTimeMillis()

        if (authViewModel.repo.getuser() != null) {
            val email = authViewModel.repo.getuser()
            if (email != null) {
                // Logged-in user: Start fetching user and group data
                // Wait indefinitely until all data is loaded
                lifecycleScope.launch {
                    val userResult = repo.getUserByMailId(email)
                    userResult.onSuccess { user ->
                        GlobalClass.Me = user
                        Log.d("CustomSplashScreen", "Current user (GlobalClass.Me) set: ${user.name}")
                        fetchAllUserGroups()
                    }.onFailure { e ->
                        Log.e("CustomSplashScreen", "Failed to fetch current user data: ${e.message}")
                        fetchAllUserGroups()
                    }
                }

                // No fallback timer - wait until data loading is complete
                Log.d("CustomSplashScreen", "Waiting for all group data to load completely...")

            } else {
                Log.d("CustomSplashScreen", "User email is null, redirecting to GettingStartedActivity.")
                redirectToActivityWithDelay(GettingStartedActivity::class.java, fixedSplashDurationForGettingStarted)
            }
        } else {
            Log.d("CustomSplashScreen", "No user found, redirecting to GettingStartedActivity.")
            redirectToActivityWithDelay(GettingStartedActivity::class.java, fixedSplashDurationForGettingStarted)
        }
    }

    private fun fetchAllUserGroups() {
        lifecycleScope.launch {
            Log.d("CustomSplashScreen", "Attempting to fetch all user's detailed groups...")
            val groupsResult = repo.getAllMyDetailedGroups()
            groupsResult.onSuccess { groups ->
                Log.d("CustomSplashScreen", "Successfully loaded ${groups.size} detailed groups.")
            }.onFailure { e ->
                Log.e("CustomSplashScreen", "Failed to load all user's detailed groups: ${e.message}")
            }

            dataLoadingComplete = true
            Log.d("CustomSplashScreen", "All data loading processes completed, preparing to redirect...")

            // Only redirect if we haven't already redirected
            if (!hasRedirected) {
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAndRedirect()
                }, 100)
            }
        }
    }

    private fun checkAndRedirect() {
        if (!dataLoadingComplete || hasRedirected) return // CHECK hasRedirected flag

        redirectToActivity(GroupSelectorActivity::class.java)
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        if (hasRedirected) return // PREVENT multiple redirections

        hasRedirected = true // SET flag before redirecting
        Log.d("CustomSplashScreen", "Redirecting to ${activityClass.simpleName}")

        val intent = Intent(this, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun redirectToActivityWithDelay(activityClass: Class<*>, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            redirectToActivity(activityClass)
        }, delay)
    }

    /**
     * Load the location tracking preference from SharedPreferences
     */
    private fun loadLocationTrackingPreference(): Boolean {
        val editor = getSharedPreferences("MY_SETTING", MODE_PRIVATE)
        return editor.getBoolean(PREF_LOCATION_TRACKING_ENABLED, false)
    }

    private fun loadSmsWatcherPreference(): Boolean{
        val editor = getSharedPreferences("MY_SETTING", MODE_PRIVATE)
        return editor.getBoolean(PREF_SMS_READER_ENAMBELD, false)
    }
}