// File: com.example.tournote.Functionality.Segments.Expenses.Repository.kt
package com.example.tournote.Functionality.Segments.Expenses.Repository

import android.R
import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass
import com.example.tournote.Functionality.Segments.Expenses.DataClass.MemberShare
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitType
import com.example.tournote.GlobalClass
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.collections.ArrayList // Explicitly import ArrayList

class ExpensesRepository {

    val db = Firebase.database

    suspend fun pushExpenseToFirebase(expense : ExpensesDataClass){
        val reff = db.getReference("expenses")
        val reffGroup = db.getReference("groups").child(GlobalClass.selected_groupId!!).child("Expenses")

        val iD = reff.push().key

        if (iD == null) {
            throw Exception("Failed to generate unique expense ID.")
        }

        val reffMain = reff.child(iD).child("PrimaryDetails")
        val reffLocation = reff.child(iD).child("Location")
        val reffDistribution = reff.child(iD).child("Distribution") // Reference for Distribution node

        // Filter out members with 0 share amount and determine overall split type
        val filteredSplitMembers = expense.splitMembers?.filter { it.shareAmount != 0.0 }

        val overallSplitTypeString = if (!filteredSplitMembers.isNullOrEmpty()) {
            filteredSplitMembers[0].shareType.name // Take the type of the first (non-zero) member's share
        } else {
            "SELF" // Default to "null" if no split members or all shares are zero
        }

        // Create expense data for PrimaryDetails
        val expenseForPrimaryDetails = ExpensesDataClass(
            expenseId = null, // expenseId will be the key
            details = expense.details,
            amount = expense.amount,
            paidBy = expense.paidBy,
            timestamp = expense.timestamp,
            billImageUrl = expense.billImageUrl,
            latitude = null, // Latitude/Longitude are stored separately
            longitude = null, // Latitude/Longitude are stored separately
            note = expense.note,
            splitType = overallSplitTypeString, // Set the overall split type
            splitMembers = null // Do not store splitMembers list directly in PrimaryDetails
        )

        // Save primary details
        reffMain.setValue(expenseForPrimaryDetails).await()

        // Save location data separately if available
        if (expense.latitude != null && expense.longitude != null) {
            val locationData = mapOf(
                "latitude" to expense.latitude,
                "longitude" to expense.longitude
            )
            reffLocation.setValue(locationData).await()
        }

        // Save split distribution details if available (only for filtered members)
        if (!filteredSplitMembers.isNullOrEmpty()) {
            for (memberShare in filteredSplitMembers) {
                val memberDistributionData = hashMapOf<String, Any>(
                    "shareAmount" to memberShare.shareAmount,
                    "paid" to false, // Initially set to false
                    "partialPayment" to (memberShare.partialPayment)!!
                    // memberName, shareType, originalInputValue are no longer stored here
                )
                reffDistribution.child(memberShare.memberUid).setValue(memberDistributionData).await()
            }
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
                    "quality" to "auto",
                    "fetch_format" to "auto"
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

                continuation.invokeOnCancellation {
                    // Consider cancelling the upload if necessary
                }

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Fetches all expenses for the selected group and updates the global expenses list
     */
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
                                        val primaryDetailsSnapshot = expenseSnapshot.child("PrimaryDetails")
                                        val locationDetailsSnapshot = expenseSnapshot.child("Location")
                                        val distributionDetailsSnapshot = expenseSnapshot.child("Distribution")

                                        val details = primaryDetailsSnapshot.child("details")
                                            .getValue(String::class.java) ?: ""
                                        val amount = primaryDetailsSnapshot.child("amount")
                                            .getValue(String::class.java) ?: ""
                                        val paidBy = primaryDetailsSnapshot.child("paidBy")
                                            .getValue(String::class.java) ?: ""
                                        val timestamp = primaryDetailsSnapshot.child("timestamp")
                                            .getValue(String::class.java) ?: ""
                                        val billImageUrl = primaryDetailsSnapshot.child("billImageUrl")
                                            .getValue(String::class.java)
                                        val note = primaryDetailsSnapshot.child("note")
                                            .getValue(String::class.java)
                                        val splitTypeString = primaryDetailsSnapshot.child("splitType")
                                            .getValue(String::class.java)?: "SELF" // Overall split type as string

                                        // Get location data
                                        val latitude = locationDetailsSnapshot.child("latitude")
                                            .getValue(Double::class.java)
                                        val longitude = locationDetailsSnapshot.child("longitude")
                                            .getValue(Double::class.java)

                                        // Reconstruct SplitType enum from the global splitTypeString
                                        val overallSplitType = try {
                                            SplitType.valueOf(splitTypeString)
                                        } catch (e: IllegalArgumentException) {
                                            SplitType.SELF // Fallback if string is invalid
                                        }

                                        // Fetch split distribution details
                                        val fetchedSplitMembers = ArrayList<MemberShare>()
                                        for (memberDistributionSnapshot in distributionDetailsSnapshot.children) {
                                            val memberUid = memberDistributionSnapshot.key
                                            if (memberUid != null) {
                                                val fetchedShareAmount = memberDistributionSnapshot.child("shareAmount").getValue(Double::class.java) ?: 0.0
                                                val paid = memberDistributionSnapshot.child("paid").getValue(Boolean::class.java)?:false
                                                val partialPayment = memberDistributionSnapshot.child("partialPayment").getValue(Double::class.java) ?: 0.0

                                                fetchedSplitMembers.add(
                                                    MemberShare(
                                                        memberUid = memberUid,
                                                        memberName = "", // Placeholder: You need to fetch member names independently using memberUid
                                                        shareAmount = fetchedShareAmount,
                                                        shareType = overallSplitType, // Use the overall expense split type
                                                        originalInputValue = null, // Not stored, so null
                                                        paid = paid,
                                                        partialPayment = partialPayment
                                                    )
                                                )
                                            }
                                        }

                                        val expense = ExpensesDataClass(
                                            expenseId = expenseId,
                                            details = details,
                                            amount = amount,
                                            paidBy = paidBy,
                                            timestamp = timestamp,
                                            billImageUrl = billImageUrl,
                                            latitude = latitude,
                                            longitude = longitude,
                                            note = note,
                                            splitType = splitTypeString, // Store the string as per ExpensesDataClass
                                            splitMembers = if (fetchedSplitMembers.isNotEmpty()) fetchedSplitMembers else null
                                        )

                                        expensesList.add(expense)
                                        completedRequests++

                                        // Check if all requests are completed
                                        if (completedRequests == totalRequests) {
                                            val sortedExpenses =
                                                expensesList.sortedByDescending { it.timestamp }
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

                continuation.invokeOnCancellation {
                    // Any cleanup if the coroutine is cancelled
                }

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun updateSettling(expenseId: String, uid: String, paidFinally: Boolean, partialPay: Double) {
        return suspendCancellableCoroutine { continuation ->
            val expenseDistributionRef = db.getReference("expenses")
                .child(expenseId)
                .child("Distribution")
                .child(uid)

            // Create a map for atomic updates
            val updates = hashMapOf<String, Any>(
                "paid" to paidFinally,
                "partialPayment" to partialPay
            )

            expenseDistributionRef.updateChildren(updates)
                .addOnSuccessListener {
                    // Update GlobalClass data
                    val expense = GlobalClass.expenses.find { it.expenseId == expenseId }
                    expense?.splitMembers?.let { memberList ->
                        val memberIndex = memberList.indexOfFirst { it.memberUid == uid }
                        if (memberIndex != -1) {
                            memberList[memberIndex].paid = paidFinally
                            memberList[memberIndex].partialPayment = partialPay
                        }
                    }

                    // IMPORTANT: Resume the continuation to complete the coroutine
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    // IMPORTANT: Resume with exception to complete the coroutine
                    continuation.resumeWithException(exception)
                }

            continuation.invokeOnCancellation {
                // No specific cleanup needed for Firebase operations on cancellation
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
                val tempFile =
                    File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
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

    /**
     * Deletes an expense from Firebase Realtime Database.
     * Deletes from the main 'expenses' branch and the 'groups/{groupId}/Expenses' branch.
     * After deletion, it refreshes the global expenses list.
     *
     * @param expenseId The ID of the expense to delete.
     * @throws Exception if the group ID is not selected or deletion fails.
     */
    suspend fun deleteExpense(expenseId: String) {
        return suspendCancellableCoroutine { continuation ->
            val groupId = GlobalClass.selected_groupId
            if (groupId == null) {
                continuation.resumeWithException(Exception("No group selected. Cannot delete expense."))
                return@suspendCancellableCoroutine
            }

            val expenseRef = db.getReference("expenses").child(expenseId)
            val groupExpenseRef = db.getReference("groups").child(groupId).child("Expenses").child(expenseId)

            // Build a map of paths to null for atomic multi-location deletion
            val updates = hashMapOf<String, Any?>(
                "/expenses/$expenseId" to null,
                "/groups/$groupId/Expenses/$expenseId" to null
            )

            db.reference.updateChildren(updates).addOnSuccessListener {
                // Remove from GlobalClass.expenses
                GlobalClass.expenses = GlobalClass.expenses.filterNot { it.expenseId == expenseId }
                continuation.resume(Unit)
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }

            continuation.invokeOnCancellation {
                // No cleanup needed, Firebase handles this
            }
        }
    }


}