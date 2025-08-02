package com.example.tournote.Functionality.Segments.Memories

import android.Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.tournote.GlobalClass
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadWorker(private val context: Context,workerParams: WorkerParameters): CoroutineWorker(context,workerParams) {
    val repo = memoriesRepository()

    companion object {
        const val KEY_URIS = "key_uris"
        const val KEY_GROUP = "key_group"
        const val KEY_FOLDER_ID = "key_folder_id"
        const val NOTIFICATION_ID = 12345
        const val GRP_NAME = "grp_name"
        const val NOTIFICATION_CHANNEL_ID = "upload_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriStrings = inputData.getStringArray(KEY_URIS)?.toList() ?: return@withContext Result.failure()
        val groupId = inputData.getString(KEY_GROUP) ?: return@withContext Result.failure()
        val groupName = inputData.getString(GRP_NAME) ?: "Default Group"


        val uris = uriStrings.map { Uri.parse(it) }

        // Show foreground notification
        setForeground(createForegroundInfo("Uploading $groupName..."))


        Log.d("UploadWorker", "Starting upload for group: $groupId with ${uris.size} files")
        val repo = memoriesRepository()
        GlobalClass.driveService?.let { repo.setDriveService(it) }

        val folderId = repo.findOrCreateFolder("Tournote", "root")
        val groupFolderId = repo.findOrCreateFolder(groupId, folderId)


        return@withContext repo.uploadMediaToDriveAndFirebase(
            uris = uris,
            context = context,
            groupName = groupId,
            folderId = groupFolderId
        ).fold(
            onSuccess = {
                Log.d("UploadWorker", "Upload successful for group: $groupId")
                NotificationUtils.showUploadCompleteNotification(context, groupName)
                Result.success()
            },
            onFailure = {
                Log.d("UploadWorker", "Upload failed for group: $groupId with error: ${it.message}")
                NotificationUtils.showUploadFailedNotification(context, groupName, it.message ?: "Unknown error")
                Result.failure()
            }
        )
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification: Notification = NotificationUtils.createUploadNotification(context, title)
        return ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun setupDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account?.account

        val driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Tournote").build()

        repo.setDriveService(driveService)
    }
}