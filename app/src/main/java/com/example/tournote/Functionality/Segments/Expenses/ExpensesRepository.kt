package com.example.tournote.Functionality.Segments.Expenses

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tournote.GlobalClass
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
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

        // Update global class expenses list after successfully pushing to Firebase
        getAllExpenseAsAList()
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

    /**
     * Fetches all expenses for the selected group and updates the global expenses list
     */
    suspend fun getAllExpenseAsAList() {
        return suspendCancellableCoroutine { continuation ->
            try {
                val groupId = GlobalClass.selected_groupId
                if (groupId == null) {
                    continuation.resumeWithException(Exception("No group selected"))
                    return@suspendCancellableCoroutine
                }

                val groupExpensesRef = db.getReference("groups").child(groupId).child("Expenses")

                groupExpensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(groupSnapshot: DataSnapshot) {
                        val expenseIds = mutableListOf<String>()

                        // Get all expense IDs from the group
                        for (expenseSnapshot in groupSnapshot.children) {
                            val expenseId = expenseSnapshot.key
                            if (expenseId != null && expenseSnapshot.getValue(Boolean::class.java) == true) {
                                expenseIds.add(expenseId)
                            }
                        }

                        if (expenseIds.isEmpty()) {
                            // No expenses found, update global class with empty list
                            GlobalClass.expenses = emptyList()
                            continuation.resume(Unit)
                            return
                        }

                        // Counter to track completed requests
                        var completedRequests = 0
                        val totalRequests = expenseIds.size
                        val expensesList = mutableListOf<ExpensesDataClass>()
                        var hasError = false

                        // Fetch each expense details
                        for (expenseId in expenseIds) {
                            val expenseRef = db.getReference("expenses").child(expenseId)

                            expenseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(expenseSnapshot: DataSnapshot) {
                                    if (hasError) return

                                    try {
                                        // Get primary details
                                        val primaryDetails = expenseSnapshot.child("PrimaryDetails")
                                        val locationDetails = expenseSnapshot.child("Location")

                                        val details = primaryDetails.child("details").getValue(String::class.java) ?: ""
                                        val amount = primaryDetails.child("amount").getValue(String::class.java) ?: ""
                                        val paidBy = primaryDetails.child("paidBy").getValue(String::class.java) ?: ""
                                        val timestamp = primaryDetails.child("timestamp").getValue(String::class.java) ?: ""
                                        val billImageUrl = primaryDetails.child("billImageUrl").getValue(String::class.java)

                                        // Get location data
                                        val latitude = locationDetails.child("latitude").getValue(Double::class.java)
                                        val longitude = locationDetails.child("longitude").getValue(Double::class.java)

                                        val expense = ExpensesDataClass(
                                            details = details,
                                            amount = amount,
                                            paidBy = paidBy,
                                            timestamp = timestamp,
                                            billImageUrl = billImageUrl,
                                            latitude = latitude,
                                            longitude = longitude
                                        )

                                        expensesList.add(expense)
                                        completedRequests++

                                        // Check if all requests are completed
                                        if (completedRequests == totalRequests) {
                                            // Sort expenses by timestamp (newest first)
                                            val sortedExpenses = expensesList.sortedByDescending { it.timestamp }
                                            GlobalClass.expenses = sortedExpenses
                                            continuation.resume(Unit)
                                        }
                                    } catch (e: Exception) {
                                        if (!hasError) {
                                            hasError = true
                                            continuation.resumeWithException(e)
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    if (!hasError) {
                                        hasError = true
                                        continuation.resumeWithException(Exception("Database error: ${error.message}"))
                                    }
                                }
                            })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(Exception("Database error: ${error.message}"))
                    }
                })

                // Handle cancellation
                continuation.invokeOnCancellation {
                    // Clean up if needed
                }

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
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