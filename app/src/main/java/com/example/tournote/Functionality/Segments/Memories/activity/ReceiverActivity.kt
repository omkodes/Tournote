package com.example.tournote.Functionality.Segments.Memories.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.api.services.drive.Drive
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.Functionality.Segments.Memories.memoriesViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiverActivity : AppCompatActivity() {
    val mainRepo = MainActivityRepository()
    private val RC_SIGN_IN = 1001
    private val REQUEST_AUTHORIZATION = 2001
    private lateinit var driveService: Drive
    private lateinit var credential: GoogleAccountCredential
    private lateinit var bar: ProgressBar
    private lateinit var sharedImageUris: List<Uri>
    private var selectedGroup = ""
    val viewModel: memoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reciever)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bar = findViewById(R.id.progressBar)
        bar.visibility = View.VISIBLE
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
                    viewModel.uploadImages(sharedImageUris, this, selectedGroup)
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

        viewModel.repository.setDriveService(driveService)
        observeViewModel()
        showGroupSelectionDialog()
    }

    private fun observeViewModel() {
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

    private fun showGroupSelectionDialog() {
        // same as your original implementation, then:
        // on group selected:

        lifecycleScope.launch {
            val groupsResponse = mainRepo.getAllMyDetailedGroups()

            if (groupsResponse.isSuccess) {
                bar.visibility = View.GONE

                val validGroups = groupsResponse.getOrNull()?.filter { it.isGroupValid == true }

                if (validGroups.isNullOrEmpty()) {
                    Toast.makeText(this@ReceiverActivity, "No valid groups available. Please create a valid group first.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val groupNames = validGroups.map { it.name ?: "Unnamed Group" }.toTypedArray()

                AlertDialog.Builder(this@ReceiverActivity)
                    .setTitle("Select Group")
                    .setItems(groupNames) { _, which ->
                        selectedGroup = groupNames[which]
                        bar.visibility = View.VISIBLE
                        viewModel.uploadImages(sharedImageUris, this@ReceiverActivity, selectedGroup)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this@ReceiverActivity, "Error fetching groups", Toast.LENGTH_SHORT).show()
                bar.visibility = View.GONE
            }
        }


    }



}