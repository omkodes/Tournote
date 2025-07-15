package com.example.tournote.Functionality.Segments.Expenses

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tournote.GlobalClass
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ExpensesRepository {

    val db = Firebase.database

    suspend fun pushExpenseToFirebase(expense : ExpensesDataClass){
        val reff = db.getReference("expenses")
        val reffGroup = db.getReference("groups").child(GlobalClass.selected_groupId!!).child("Expenses")

        /*utility of "!!"
        :

        "I, the developer, am absolutely certain that this value will not be null at this point in the execution."

        "Therefore, treat this expression as its non-nullable type."*/

        val iD = reff.push().key

        val reffMain = reff.child(iD!!).child("PrimaryDetails")
        val reffLocation = reff.child(iD).child("Location")

        // Create expense data without location for PrimaryDetails
        val expenseWithoutLocation = ExpensesDataClass(
            details = expense.details,
            amount = expense.amount,
            paidBy = expense.paidBy,
            timestamp = expense.timestamp,
            billImageUrl = expense.billImageUrl,
            latitude = null,
            longitude = null
        )

        // Save primary details without location
        reffMain.setValue(expenseWithoutLocation).await()

        // Save location data separately if available
        if (expense.latitude != null && expense.longitude != null) {
            val locationData = mapOf(
                "latitude" to expense.latitude,
                "longitude" to expense.longitude
            )
            reffLocation.setValue(locationData).await()
        }

        // Update group reference
        reffGroup.child(iD).setValue(true).await()
    }

    /**
     * Uploads an image to Cloudinary and returns the secure URL
     * @param uri The URI of the image to upload
     * @param context The application context
     * @return The secure URL of the uploaded image, or null if upload fails
     */
    suspend fun uploadImageToCloudinary(uri: Uri, context: Context): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val file = getFileFromUri(uri, context)

                if (file == null) {
                    continuation.resumeWithException(Exception("Failed to create file from URI"))
                    return@suspendCancellableCoroutine
                }

                val uploadOptions = hashMapOf<String, Any>(
                    "public_id" to "expense_${System.currentTimeMillis()}",
                    "folder" to "expense_bills",
                    "resource_type" to "image",
                    "quality" to "auto",     // Move quality directly here
                    "fetch_format" to "auto" // Move fetch_format directly here
                )

                val uploadRequest = MediaManager.get()
                    .upload(file.absolutePath)
                    .options(uploadOptions)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {
                            // Upload started
                        }

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                            // Progress updates (optional)
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            val url = resultData?.get("secure_url") as? String
                            if (url != null) {
                                continuation.resume(url)
                            } else {
                                continuation.resumeWithException(Exception("Failed to get secure URL from upload result"))
                            }
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            continuation.resumeWithException(
                                Exception("Upload failed: ${error?.description ?: "Unknown error"}")
                            )
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            continuation.resumeWithException(
                                Exception("Upload rescheduled: ${error?.description ?: "Unknown error"}")
                            )
                        }
                    })

                uploadRequest.dispatch()

                // Handle cancellation
                continuation.invokeOnCancellation {

                }

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun getAllExpenseAsAList(){

    }

    /**
     * Converts a URI to a File object
     * @param uri The URI to convert
     * @param context The application context
     * @return File object or null if conversion fails
     */
    private fun getFileFromUri(uri: Uri, context: Context): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.let {
                val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(tempFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                tempFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}