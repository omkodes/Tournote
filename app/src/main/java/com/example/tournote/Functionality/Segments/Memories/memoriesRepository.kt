package com.example.tournote.Functionality.Segments.Memories

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.tournote.Functionality.Segments.Memories.data.PhotoItem
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class memoriesRepository {

    private lateinit var driveService: Drive
    private val firebaseDb: DatabaseReference = Firebase.database.getReference("photos")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val inputFormats = listOf(
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )
    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    fun setDriveService(service: Drive) {
        this.driveService = service
    }

    suspend fun uploadImagesToDriveAndFirebase(
        uris: List<Uri>,
        context: Context,
        groupName: String,
        folderId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (uri in uris) {
                val stream = context.contentResolver.openInputStream(uri)
                val exif = ExifInterface(stream!!)
                val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val fileMeta = File().apply {
                    name = "photo_${System.currentTimeMillis()}.jpg"
                    parents = listOf(folderId)
                    appProperties = mapOf("group" to groupName, "photoDate" to date)
                }

                val mediaContent = InputStreamContent("image/jpeg", context.contentResolver.openInputStream(uri))
                val uploadedFile = driveService.files().create(fileMeta, mediaContent)
                    .setFields("id, webViewLink")
                    .execute()

                // ➕ Make the file publicly viewable
                val permission = com.google.api.services.drive.model.Permission().apply {
                    type = "anyone"
                    role = "reader"
                }
                driveService.permissions().create(uploadedFile.id, permission).execute()

                val metadata = mapOf(
                    "fileId" to uploadedFile.id,
                    "group" to groupName,
                    "date" to date,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "userId" to auth.uid
                )

                firebaseDb.push().setValue(metadata)
            }

            return@withContext Result.success(Unit)

        } catch (e: UserRecoverableAuthIOException) {
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun findOrCreateFolder(name: String, parentId: String): String = withContext(Dispatchers.IO) {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$name' and '$parentId' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) return@withContext result.files[0].id

        val metadata = File().apply {
            this.name = name
            this.mimeType = "application/vnd.google-apps.folder"
            this.parents = listOf(parentId)
        }

        val folder = driveService.files().create(metadata).setFields("id").execute()
        return@withContext folder.id
    }

    fun fetchPhotosGroupedByDateForGroup(
        groupName: String,
        onResult: (List<PhotoItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firebaseDb.orderByChild("group").equalTo(groupName).get()
            .addOnSuccessListener { snapshot ->

                val groupedMap = mutableMapOf<String, MutableList<PhotosData>>()

                snapshot.children.forEach { snap ->
                    val fileId = snap.child("fileId").getValue(String::class.java) ?: return@forEach
                    val date = snap.child("date").getValue(String::class.java) ?: return@forEach
                    val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L
                    val userId = snap.child("userId").getValue(String::class.java) ?: ""
                    // date formatting
                    val parsedDate = inputFormats.firstNotNullOfOrNull {
                        try {
                            it.parse(date)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val prettyDate = parsedDate?.let { outputFormat.format(it) } ?: date

                    val photo = PhotosData(fileId, date, timestamp, userId)
                    groupedMap.getOrPut(prettyDate) { mutableListOf() }.add(photo)
                }

                val dateGroups = groupedMap.entries.map { entry ->
                    val rawDate = entry.key
                    val sortedPhotos = entry.value.sortedBy { it.timestamp }
                    PhotoItem(
                        rawDate, // now using formatted title
                        sortedPhotos.first().fileId,
                        sortedPhotos.sortedByDescending { it.timestamp }
                    )
                }.sortedBy { it.date }

                onResult(dateGroups)
            }
            .addOnFailureListener { ex ->
                onError(ex)
            }
    }


}