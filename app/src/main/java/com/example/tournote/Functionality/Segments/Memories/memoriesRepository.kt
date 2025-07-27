package com.example.tournote.Functionality.Segments.Memories

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.tournote.Functionality.Segments.Memories.data.PhotoItem
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.GlobalClass
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File as LocalFile
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class memoriesRepository {

    private lateinit var driveService: Drive
    private val firebaseDb: DatabaseReference = Firebase.database.getReference("memories")
    val db = Firebase.database
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val inputFormats = listOf(
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )
    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    fun setDriveService(service: Drive) {
        this.driveService = service
    }

    suspend fun uploadMediaToDriveAndFirebase(
        uris: List<Uri>,
        context: Context,
        groupName: String,
        folderId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (uri in uris) {
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileExt = when {
                    mimeType.startsWith("image/") -> ".jpg"
                    mimeType.startsWith("video/") -> ".mp4"
                    else -> ""
                }

                val inputStream = context.contentResolver.openInputStream(uri) ?: continue

                val date: String = if (mimeType.startsWith("image/")) {
                    try {
                        val exif = ExifInterface(inputStream)
                        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    } catch (e: Exception) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    }
                } else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }

                val fileMeta = DriveFile().apply {
                    name = "media_${System.currentTimeMillis()}$fileExt"
                    parents = listOf(folderId)
                    appProperties = mapOf("group" to groupName, "mediaDate" to date)
                }

                val mediaContent = InputStreamContent(mimeType, context.contentResolver.openInputStream(uri))
                val uploadedFile = driveService.files().create(fileMeta, mediaContent)
                    .setFields("id, webViewLink")
                    .execute()

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
                    "userId" to auth.uid,
                    "mimeType" to mimeType
                )

                val newRf = firebaseDb.push()
                val key = newRf.key
                newRf.setValue(metadata)

                val anotherPath = db.getReference("groups").child(groupName).child("Memories")
                anotherPath.child(key!!).setValue(true)
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

        val metadata = DriveFile().apply {
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
        val memoriesRef = db.getReference("groups")
            .child(GlobalClass.selected_groupId!!)
            .child("Memories")

        val groupedMap = mutableMapOf<String, MutableList<PhotosData>>()

        memoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val keys = snapshot.children.mapNotNull { it.key }
                if (keys.isEmpty()) {
                    onResult(emptyList())
                    return
                }

                var completed = 0
                val total = keys.size

                for (key in keys) {
                    firebaseDb.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snap: DataSnapshot) {
                            val fileId = snap.child("fileId").getValue(String::class.java) ?: ""
                            val date = snap.child("date").getValue(String::class.java) ?: ""
                            val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L
                            val userId = snap.child("userId").getValue(String::class.java) ?: ""
                            val group = snap.child("group").getValue(String::class.java) ?: ""
                            val mime = snap.child("mimeType").getValue(String::class.java) ?: ""

                            val parsedDate = inputFormats.firstNotNullOfOrNull {
                                try {
                                    it.parse(date)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            val prettyDate = parsedDate?.let { outputFormat.format(it) } ?: date

                            val photo = PhotosData(fileId, date, timestamp, group, mime,userId)
                            groupedMap.getOrPut(prettyDate) { mutableListOf() }.add(photo)

                            completed++
                            if (completed == total) {
                                val dateGroups = groupedMap.entries.map { entry ->
                                    val rawDate = entry.key
                                    val sortedPhotos = entry.value.sortedBy { it.timestamp }
                                    PhotoItem(
                                        rawDate,
                                        sortedPhotos.first().fileId,
                                        sortedPhotos.sortedByDescending { it.timestamp }
                                    )
                                }.sortedBy { it.date }

                                onResult(dateGroups)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FIREBASE", "Metadata fetch failed: ${error.message}")
                            onError(error.toException())
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Memories fetch failed: ${error.message}")
                onError(error.toException())
            }
        })
    }

    suspend fun deleteMediaByFileId(
        fileId: String,
        groupId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firebaseDb.get().await()
            var keyToDelete: String? = null

            for (child in snapshot.children) {
                val fileIdInDb = child.child("fileId").getValue(String::class.java)
                if (fileIdInDb == fileId) {
                    keyToDelete = child.key
                    break
                }
            }

            if (keyToDelete == null) {
                return@withContext Result.failure(Exception("Memory entry not found for fileId: $fileId"))
            }

            // 1. Delete from Google Drive
            driveService.files().delete(fileId).execute()

            // 2. Delete from /memories
            firebaseDb.child(keyToDelete).removeValue()

            // 3. Delete from /groups/{groupId}/Memories/{key}
            val groupMemoriesRef = db.getReference("groups")
                .child(groupId)
                .child("Memories")
                .child(keyToDelete)

            groupMemoriesRef.removeValue()

            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun downloadGroupMediaAsZip(
        context: Context,
        groupId: String,
        zipFileName: String = "group_memories.zip"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileList = mutableListOf<LocalFile>()

            val groupMemoriesRef = db.getReference("groups").child(groupId).child("Memories")
            val snapshot = groupMemoriesRef.get().await()

            if (!snapshot.exists()) return@withContext Result.failure(Exception("No memories found"))

            for (child in snapshot.children) {
                val memoryKey = child.key ?: continue
                val fileSnapshot = firebaseDb.child(memoryKey).get().await()
                val fileId = fileSnapshot.child("fileId").getValue(String::class.java) ?: continue
                val mimeType = fileSnapshot.child("mimeType").getValue(String::class.java)
                    ?: "application/octet-stream"
                val extension = when {
                    mimeType.startsWith("image/") -> ".jpg"
                    mimeType.startsWith("video/") -> ".mp4"
                    else -> ""
                }

                val fileName = "media_${System.currentTimeMillis()}$extension"
                val mediaFile = downloadDriveFile(context, fileId, fileName)
                fileList.add(mediaFile)
            }

            // Create ZIP file
            val zipDir = LocalFile(context.cacheDir, "media_zip")
            zipDir.mkdirs()
            val zipFile = LocalFile(zipDir, zipFileName)
            zipFiles(fileList, zipFile)

            // Save to Downloads as well 🔥
            val savedUriResult = saveZipToDownloads(context, zipFile, zipFileName)

            // Clean up temp files
            fileList.forEach { it.delete() }

            return@withContext savedUriResult

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }


    private fun downloadDriveFile(context: Context, fileId: String, fileName: String): LocalFile {
        // temp folder need for grouping the data
        val outputDir = LocalFile(context.cacheDir, "media_zip")
        outputDir.mkdirs()
        // create file with name
        val file = LocalFile(outputDir, fileName)
        FileOutputStream(file).use { output ->
            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(output)
        }
        return file
    }
    private fun zipFiles(files: List<LocalFile>, zipFile: LocalFile) {
        ZipOutputStream(BufferedOutputStream( FileOutputStream(zipFile))).use { out ->
            for (file in files) {
                FileInputStream(file).use { input ->
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    input.copyTo(out, 8192)
                    out.closeEntry()
                }
            }
        }
    }
    suspend fun saveZipToDownloads(
        context: Context,
        sourceFile: LocalFile,
        zipDisplayName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val mimeType = "application/zip"

            // Scoped storage (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, zipDisplayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val resolver = context.contentResolver
                val uri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(Exception("Failed to insert into MediaStore"))

                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().copyTo(output)
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                return@withContext Result.success(uri)

            } else {
                // Legacy storage (API < 29)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val targetFile = LocalFile(downloadsDir, zipDisplayName)
                sourceFile.copyTo(targetFile, overwrite = true)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
                return@withContext Result.success(uri)
            }

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }








}