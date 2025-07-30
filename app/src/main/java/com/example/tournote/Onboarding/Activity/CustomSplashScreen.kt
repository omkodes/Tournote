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
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.UserModel
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.launch

class CustomSplashScreen : AppCompatActivity() {
    private val authViewModel: authViewModel by viewModels()
    private val viewModel: GroupSelectorActivityViewModel2 by viewModels()
    private val repo = FirebaseRTDBRepository()

    private val fixedSplashDurationForGettingStarted = 5000L
    private val PREF_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
    private val PREF_SMS_READER_ENAMBELD = "sms_enabled"

    private var hasRedirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_splash_screen)

        viewModel.refreshGroupData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.navigationBarColor = ContextCompat.getColor(this, R.color.customSplashScreenBackground)

        GlobalClass.isTracking = loadLocationTrackingPreference()
        GlobalClass.isSmsWatched = loadSmsWatcherPreference()

        if (authViewModel.repo.getuser() != null) {
            val email = authViewModel.repo.getuser()
            if (email != null) {
                lifecycleScope.launch {
                    val userResult = repo.getUserByMailId(email)
                    userResult.onSuccess { user ->
                        GlobalClass.Me = user
                        Log.d("CustomSplashScreen", "Current user (GlobalClass.Me) set: ${user.name}")
                    }.onFailure { e ->
                        Log.e("CustomSplashScreen", "Failed to fetch current user data: ${e.message}")
                    }

                    redirectToActivity(GroupSelectorActivity::class.java)
                }
            } else {
                Log.d("CustomSplashScreen", "User email is null, redirecting to GettingStartedActivity.")
                redirectToActivityWithDelay(GettingStartedActivity::class.java, fixedSplashDurationForGettingStarted)
            }
        } else {
            Log.d("CustomSplashScreen", "No user found, redirecting to GettingStartedActivity.")
            redirectToActivityWithDelay(GettingStartedActivity::class.java, fixedSplashDurationForGettingStarted)
        }
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        if (hasRedirected) return
        hasRedirected = true

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

    private fun loadLocationTrackingPreference(): Boolean {
        val prefs = getSharedPreferences("MY_SETTING", MODE_PRIVATE)
        return prefs.getBoolean(PREF_LOCATION_TRACKING_ENABLED, false)
    }

    private fun loadSmsWatcherPreference(): Boolean {
        val prefs = getSharedPreferences("MY_SETTING", MODE_PRIVATE)
        return prefs.getBoolean(PREF_SMS_READER_ENAMBELD, false)
    }
}
