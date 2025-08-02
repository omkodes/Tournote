package com.example.tournote.Functionality.Segments.Memories.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ProgressBar
import android.widget.Toast
import com.google.api.services.drive.Drive
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer // Import Observer
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.Functionality.Segments.Memories.UploadWorker
import com.example.tournote.Functionality.Segments.Memories.memoriesRepository
import com.example.tournote.Functionality.Segments.Memories.memoriesViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes

import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2 // Import your GroupSelectorActivityViewModel2
import com.example.tournote.GroupData_Detailed_Model // Import your GroupData_Detailed_Model
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


class ReceiverActivity : AppCompatActivity() {
    // val mainRepo = MainActivityRepository() // REMOVE: No longer directly fetching groups here
    private val RC_SIGN_IN = 1001
    private val REQUEST_AUTHORIZATION = 2001
    private lateinit var driveService: Drive
    private lateinit var credential: GoogleAccountCredential // Unused property (local variable in setupDriveService)
    private lateinit var bar: ProgressBar
    private lateinit var sharedImageUris: List<Uri>
    private var selectedGroup = ""
    val viewModel: memoriesViewModel by viewModels() // Your existing memoriesViewModel
    val repository = memoriesRepository()
    // NEW: ViewModel to get group data
    private val groupSelectorViewModel: GroupSelectorActivityViewModel2 by viewModels()

    // Flags to control when the group selection dialog should be shown
    private var isDriveServiceSetup = false
    private var hasGroupsDataBeenObserved = false
    private var isGroupDialogShowing = false
    val repo01 = FirebaseRTDBRepository()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reciever)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        CoroutineScope(Dispatchers.IO).launch {
            GlobalClass.Me = repo01.getUserByMailId(FirebaseAuth.getInstance().currentUser?.email!!).getOrNull()
        }
        bar = findViewById(R.id.progressBar)
        bar.visibility = View.VISIBLE // Show loading bar initially

        sharedImageUris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: listOf()
            else -> listOf()
        }

        if (sharedImageUris.isEmpty()) {
            Toast.makeText(this, "No images received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- NEW: Observe ViewModel's loading and error states for group data ---
        groupSelectorViewModel.isLoading.observe(this, Observer { isLoading ->
            // Control ProgressBar based on combined loading states if needed
            // For simplicity, we'll let memoriesViewModel control it during upload
            // and groupSelectorViewModel control it during group fetch.
            // bar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        groupSelectorViewModel.error.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, "Group Load Error: $it", Toast.LENGTH_LONG).show()
                if (!isGroupDialogShowing) finish() // If no groups, finish activity
            }
        })

        // --- NEW: Observe ViewModel's group data ---
        groupSelectorViewModel.groups.observe(this, Observer { groups ->
            Log.d("ReceiverActivity", "Groups LiveData updated: ${groups?.size} groups. Drive service setup: $isDriveServiceSetup. Dialog showing: $isGroupDialogShowing")
            hasGroupsDataBeenObserved = true // Mark that groups data has been received

            // Attempt to show dialog only if drive service is ready and groups data is also ready
            tryShowGroupSelectionDialog()
        })


        // Existing logic for Google Sign-In and Drive setup
        if (GoogleSignIn.getLastSignedInAccount(this) == null) {
            requestDriveSignIn()
        } else {
            setupDriveService()
        }
    }

    private fun requestDriveSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGN_IN -> {
                if (resultCode == RESULT_OK) {
                    setupDriveService()
                } else {
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            REQUEST_AUTHORIZATION -> {
                if (resultCode == RESULT_OK) {
                    // Only proceed with upload if a group was already selected (which happens in the dialog)
                    if (selectedGroup.isNotEmpty()) {
                        bar.visibility = View.VISIBLE // Show progress for upload
                        viewModel.uploadMedia(sharedImageUris, this@ReceiverActivity, selectedGroup)
                    } else {
                        Toast.makeText(this, "No group selected for upload. Please try again.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Authorization required to upload images", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }


    private fun setupDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val credential = GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account?.account

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Tournote").build()

        // ✅ Make it globally available
        GlobalClass.driveService = driveService

        viewModel.repository.setDriveService(driveService) // Already here
        observeViewModel() // Already here

        isDriveServiceSetup = true
        tryShowGroupSelectionDialog()
    }


    private fun observeViewModel() {
        // Observes upload state from memoriesViewModel
        viewModel.uploadState.observe(this) { result ->
            when {
                result.isSuccess -> {
                    Toast.makeText(this, "Upload complete", Toast.LENGTH_SHORT).show()
                    bar.visibility = View.GONE
                    finish()
                }
                result.exceptionOrNull() is UserRecoverableAuthIOException -> {
                    val intent = (result.exceptionOrNull() as UserRecoverableAuthIOException).intent
                    startActivityForResult(intent, REQUEST_AUTHORIZATION)
                }
                result.isFailure -> {
                    bar.visibility = View.GONE
                    Toast.makeText(this, "Upload failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // NEW: Helper function to centralize the logic for showing the group dialog
    private fun tryShowGroupSelectionDialog() {
        // Only show if both Drive service is ready AND group data has been observed AND dialog isn't already showing
        if (isDriveServiceSetup && hasGroupsDataBeenObserved && !isGroupDialogShowing) {
            val groups = groupSelectorViewModel.groups.value // Get the current list of groups from LiveData

            val validGroups = groups?.filter { it.isGroupValid == true }

            if (!validGroups.isNullOrEmpty()) {
                showGroupSelectionDialogInternal(validGroups)
                isGroupDialogShowing = true // Set flag to prevent re-showing
            } else {
                Toast.makeText(this@ReceiverActivity, "No valid groups available. Please create a valid group first.", Toast.LENGTH_SHORT).show()
                // If no valid groups, we might need to finish the activity or guide the user.
                finish()
            }
        }
    }

    // Renamed and modified: This now receives the groups list directly
    private fun showGroupSelectionDialogInternal(validGroups: List<GroupData_Detailed_Model>) {
        // Bar visibility handled by groupSelectorViewModel's isLoading observer

        val groupNames = validGroups.map { it.name ?: "Unnamed Group" }.toTypedArray()

        AlertDialog.Builder(this@ReceiverActivity)
            .setTitle("Select Group")
            .setItems(groupNames) { dialog, which -> // Use `dialog` parameter
                selectedGroup = validGroups.getOrNull(which)?.groupID ?: ""
                if (selectedGroup.isNotEmpty()) {
                    bar.visibility = View.VISIBLE // Show progress bar as upload begins
                    CoroutineScope(Dispatchers.IO).launch {

                        val safeUris = copyUrisToInternalStorage(sharedImageUris)

                        val inputData = Data.Builder()
                            .putStringArray(UploadWorker.KEY_URIS, safeUris.map { it.toString() }.toTypedArray())
                            .putString(UploadWorker.KEY_GROUP, selectedGroup)
                            .putString(UploadWorker.GRP_NAME, validGroups[which].name)
                            .build()


                        val work = OneTimeWorkRequestBuilder<UploadWorker>()
                            .setInputData(inputData)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()

                        WorkManager.getInstance(this@ReceiverActivity).enqueue(work)

                        finish() // Activity is done, upload continues 💪
                    }

                } else {
                    Toast.makeText(this, "Failed to get selected group ID.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                dialog.dismiss() // Dismiss the dialog after selection
            }
            .setCancelable(false) // User must select a group or explicitly close (which means finishing)
            .setOnDismissListener {
                isGroupDialogShowing = false // Reset the flag when dialog is dismissed
            }
            .show()
    }

    private fun copyUrisToInternalStorage(uris: List<Uri>): List<Uri> {
        val tempDir = File(cacheDir, "upload_temp")
        tempDir.mkdirs()

        return uris.mapNotNull { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@mapNotNull null

                // 👇 Fix: Detect MIME type and extension
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType) ?: "bin"

                val fileName = "shared_${System.currentTimeMillis()}.$extension"
                val tempFile = File(tempDir, fileName)

                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    tempFile
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }



}