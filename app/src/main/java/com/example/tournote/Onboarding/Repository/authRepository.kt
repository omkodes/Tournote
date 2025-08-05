package com.example.tournote.Onboarding.Repository

import android.content.SharedPreferences
import android.util.Log
import com.example.tournote.Database.RemoteDatabase.FirebaseRTDBRepository
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.FcmResponse
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.fcmSave
import com.example.tournote.Functionality.Segments.ChatRoom.Object.APIClient
import com.example.tournote.GlobalClass
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class authRepository {
    val firebaseAuth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    val realDB = Firebase.database
    val repo1 = FirebaseRTDBRepository()


    suspend fun custom_login(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email,pass).await()

            if (email != null) {
                val result = repo1.getUserByMailId(email ?: "")
                result.onSuccess { user ->
                    GlobalClass.Me = user
                }.onFailure {
                    Log.e("authViewModel", "Failed to fetch user: ${it.message}")
                }
            }

            Log.d("authRepository", "Signup success: ${result.user?.email}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Signup failed", e)
            Result.failure(e)
        }
    }

    suspend fun custom_signUp(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, pass)
                .await()
            //GlobalClass.Email=email
            Log.d("authRepository", "Signup success: ${result.user?.email}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Signup failed", e)
            Result.failure(e)
        }
    }

    suspend fun forgot_pass(email: String): Result<Unit> {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun signOut(): Result<String> {
        return try {
            firebaseAuth.signOut()
            Result.success("Signed out successfully")
        } catch (e: Exception) {
            Result.failure(e)

        }
    }

    fun getuser(): String? {
        return firebaseAuth.currentUser?.email
    }

    fun getUid():String?{
        return firebaseAuth.currentUser?.uid
    }


    suspend fun firebaseLoginWithGoogle(account: GoogleSignInAccount): FirebaseUser? {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = withContext(Dispatchers.IO) {
                firebaseAuth.signInWithCredential(credential).await()
            }

            result.user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun userDetailsToFirebaseRealtimeDatabase(
        userId: String,
        userMap: Map<String, Any>
    ): Result<Any> {
        return try {
            val result = realDB.getReference("users")
                .child(userId)
                .child("PersonalDetails")
                .updateChildren(userMap)
                .await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("authRepository", "Error saving user details: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun userDetailGetLogin(userId: String): DataSnapshot? {
        return try {
            val snapshot = realDB
                .getReference("users")
                .child(userId)
                .child("PersonalDetails")
                .get()
                .await()
            snapshot
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    suspend fun changePassword(oldPass: String, newPass: String): Result<Any> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential).await()
                user.updatePassword(newPass).await()
                Result.success("Password changed successfully")
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    suspend fun userFcmSave(userId: String): Result<FcmResponse> {
        return try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val request = fcmSave(token = fcmToken, userId = userId)

            val res = APIClient.api_fcm.saveToken(request)
            if (res.isSuccessful) {
                val body = res.body()
                if (body?.success == true) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("FCM save failed: ${body?.error ?: body?.message}"))
                }
            } else {
                val errorBody = res.errorBody()?.string()
                Result.failure(Exception("HTTP ${res.code()} — ${errorBody ?: "Unknown error"}"))
            }

        } catch (e: Exception) {
            Log.e("FCM_SAVE", "Exception during FCM save", e)
            Result.failure(e)
        }
    }



}