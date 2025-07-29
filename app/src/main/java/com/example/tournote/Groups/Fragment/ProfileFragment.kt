package com.example.tournote.Groups.Fragment

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.ReplyReceiver
import com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.SmsReceiver
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Onboarding.Activity.LogInActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.Functionality.Segments.TrackFriends.Services.LocationTrackingService
import com.example.tournote.Profile.UpdateProfileActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    // ViewModels and Database
    private val viewModel: authViewModel by viewModels()
    private lateinit var databaseRef: DatabaseReference
    private lateinit var sharedPrefs: SharedPreferences

    // UI Elements
    private var locationSwitch: Switch? = null
    private var smsWatcherSwitch: Switch? = null

    // Constants
    private companion object {
        const val PREF_NAME = "MY_SETTING"
        const val PREF_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
        const val PREF_SMS_READER_ENABLED = "sms_enabled"
        const val SMS_PERMISSION_CODE = 101

        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
    }

    // State
    private var isUserToggled = false

    // Permission Launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handleLocationPermissionResult(permissions)
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handleSmsPermissionResult(permissions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initializeComponents(view)
        setupClickListeners(view)
        setupSwitches(view)
        observeViewModel()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStates()
        ensureServiceConsistency()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationSwitch = null
        smsWatcherSwitch = null
    }

    // MARK: - Initialization

    private fun initializeComponents(view: View) {
        databaseRef = FirebaseDatabase.getInstance().getReference("locations")
        sharedPrefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        locationSwitch = view.findViewById(R.id.switch1)
        smsWatcherSwitch = view.findViewById(R.id.switch2)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<RelativeLayout>(R.id.sign_out_button).setOnClickListener {
            viewModel.signOut()
        }

        view.findViewById<RelativeLayout>(R.id.btnEnhanceProfile).setOnClickListener {
            startActivity(Intent(requireContext(), UpdateProfileActivity::class.java))
        }
    }

    private fun setupSwitches(view: View) {
        setupLocationTrackingSwitch()
        setupSmsWatcherSwitch()
    }

    // MARK: - Location Tracking

    private fun setupLocationTrackingSwitch() {
        locationSwitch?.apply {
            isChecked = GlobalClass.isTracking
            setOnCheckedChangeListener { _, isChecked ->
                isUserToggled = true
                handleLocationSwitchToggle(isChecked)
            }
        }
    }

    private fun handleLocationSwitchToggle(isEnabled: Boolean) {
        if (isEnabled) {
            if (hasLocationPermissions()) {
                startLocationTracking()
            } else {
                requestLocationPermissions()
            }
        } else {
            stopLocationTracking()
        }
    }

    private fun handleLocationPermissionResult(permissions: Map<String, Boolean>) {
        val hasPermission = permissions.values.any { it }

        locationSwitch?.isChecked = hasPermission

        if (hasPermission) {
            startLocationTracking()
            showToast("Location permissions granted")
        } else {
            saveLocationTrackingPreference(false)
            showToast("Location permissions required for tracking")
        }
    }

    private fun startLocationTracking() {
        lifecycleScope.launch {
            try {
                startLocationService()
                saveLocationTrackingPreference(true)
                updateFirebaseRefreshState(true)

                if (isUserToggled) {
                    showToast("Location tracking started")
                }
            } catch (e: Exception) {
                handleLocationServiceError(e, false)
            }
        }
    }

    private fun stopLocationTracking() {
        lifecycleScope.launch {
            try {
                stopLocationService()
                saveLocationTrackingPreference(false)
                updateFirebaseRefreshState(false)

                if (isUserToggled) {
                    showToast("Location tracking stopped")
                }
            } catch (e: Exception) {
                handleLocationServiceError(e, true)
            }
        }
    }

    private fun handleLocationServiceError(exception: Exception, wasStarting: Boolean) {
        val action = if (wasStarting) "start" else "stop"
        showToast("Failed to $action location tracking: ${exception.message}")

        if (wasStarting) {
            locationSwitch?.isChecked = false
            saveLocationTrackingPreference(false)
        }
    }

    // MARK: - SMS Watcher

    private fun setupSmsWatcherSwitch() {
        smsWatcherSwitch?.apply {
            isChecked = GlobalClass.isSmsWatched
            setOnCheckedChangeListener { _, isChecked ->
                handleSmsWatcherToggle(isChecked)
            }
        }
    }

    private fun handleSmsWatcherToggle(isEnabled: Boolean) {
        if (isEnabled) {
            if (hasSmsPermissions()) {
                enableSmsWatcher()
            } else {
                requestSmsPermissions()
            }
        } else {
            disableSmsWatcher()
        }
    }

    private fun handleSmsPermissionResult(permissions: Map<String, Boolean>) {
        val hasAllPermissions = permissions.values.all { it }

        smsWatcherSwitch?.isChecked = hasAllPermissions

        if (hasAllPermissions) {
            enableSmsWatcher()
        } else {
            showToast("SMS permissions required for auto expense detection")
        }
    }

    private fun enableSmsWatcher() {
        GlobalClass.isSmsWatched = true
        saveSmsWatcherPreferences(true)
        setReceiversEnabled(true)
        showToast("Expense detection enabled")
    }

    private fun disableSmsWatcher() {
        GlobalClass.isSmsWatched = false
        saveSmsWatcherPreferences(false)
        setReceiversEnabled(false)
        showToast("Expense detection disabled")
    }

    private fun setReceiversEnabled(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        val receivers = arrayOf(SmsReceiver::class.java, ReplyReceiver::class.java)

        receivers.forEach { receiverClass ->
            val componentName = ComponentName(requireContext(), receiverClass)
            requireContext().packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    // MARK: - Permission Helpers

    private fun hasLocationPermissions(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasSmsPermissions(): Boolean {
        return SMS_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    private fun requestSmsPermissions() {
        smsPermissionLauncher.launch(SMS_PERMISSIONS)
    }

    // MARK: - Service Management

    private fun startLocationService() {
        val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_LOCATION_TRACKING
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_LOCATION_TRACKING
        }
        requireContext().startService(serviceIntent)
    }

    // MARK: - Preferences

    private fun saveLocationTrackingPreference(isEnabled: Boolean) {
        GlobalClass.isTracking = isEnabled
        sharedPrefs.edit()
            .putBoolean(PREF_LOCATION_TRACKING_ENABLED, isEnabled)
            .apply()
    }

    private fun saveSmsWatcherPreferences(isEnabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean(PREF_SMS_READER_ENABLED, isEnabled)
            .apply()
    }

    // MARK: - Firebase Updates

    private fun updateFirebaseRefreshState(isRefreshing: Boolean) {
        val currentUser = GlobalClass.Me?.uid ?: return
        databaseRef.child(currentUser)
            .child("itIsRefreshing")
            .setValue(isRefreshing)
    }

    // MARK: - State Updates

    private fun updateSwitchStates() {
        locationSwitch?.isChecked = GlobalClass.isTracking
        smsWatcherSwitch?.isChecked = GlobalClass.isSmsWatched
    }

    private fun ensureServiceConsistency() {
        if (GlobalClass.isTracking && hasLocationPermissions()) {
            startLocationService()
        } else if (!GlobalClass.isTracking) {
            stopLocationService()
        }
    }

    // MARK: - ViewModel Observation

    private fun observeViewModel() {
        viewModel.loginError.observe(viewLifecycleOwner) { error ->
            error?.let { showToast(it) }
        }

        viewModel.toastmsg.observe(viewLifecycleOwner) { message ->
            message?.let {
                showToast(it)
                viewModel.clearToast()
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                handleLogout()
            }
        }

        viewModel.navigateToMain.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                navigateToGroupSelector()
            }
        }
    }

    private fun handleLogout() {
        stopLocationService()
        saveLocationTrackingPreference(false)

        val intent = Intent(requireContext(), LogInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        requireActivity().finish()
        viewModel.clearNavigationLogin()
    }

    private fun navigateToGroupSelector() {
        val intent = Intent(requireContext(), GroupSelectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        requireActivity().finish()
        viewModel.clearRoleLoadingMain()
    }

    // MARK: - Utilities

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // MARK: - Legacy Permission Handling (Deprecated)

    @Deprecated("Use ActivityResultContracts instead")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE) {
            val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            smsWatcherSwitch?.isChecked = allGranted

            if (allGranted) {
                enableSmsWatcher()
            } else {
                showToast("SMS permissions required for auto expense detection")
            }
        }
    }
}