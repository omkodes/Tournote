package com.example.tournote.Functionality.Segments.Memories.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.Functionality.Segments.Memories.memoriesRepository
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

class ImageFullActivity : AppCompatActivity() {
    private var exoPlayer: ExoPlayer? = null
    private lateinit var driveService: Drive
    private val repository = memoriesRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_full)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val data = intent.getParcelableExtra<PhotosData>("imagePath") ?: PhotosData("", "", 0, "","", "")
        val imgPos = intent.getIntExtra("itemPosition",-1)
        val imageView: ImageView = findViewById(R.id.fullImageView)
        val playerView: PlayerView = findViewById(R.id.playerView)
        val delete_btn = findViewById<ImageView>(R.id.btn_delete)
        val btnBack = findViewById<ImageView>(R.id.buttonBack)
        val bar = findViewById<ProgressBar>(R.id.progressBar)

        btnBack.setOnClickListener {
            finish()
        }

        delete_btn.setOnClickListener {
            setupDriveService()
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete Item")
            builder.setMessage("Are you sure you want to delete this item?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                bar.visibility = android.view.View.VISIBLE
                lifecycleScope.launch {
                    val result = repository.deleteMediaByFileId(data.fileId, GlobalClass.selected_groupId!!)
                    if (result.isSuccess) {
                        bar.visibility = android.view.View.GONE
                        Toast.makeText(this@ImageFullActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent().apply {
                            putExtra("deleted_position", imgPos)
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()


                    } else {
                        bar.visibility = android.view.View.GONE
                        Log.d("grpadapter", "Delete failed: ${result.exceptionOrNull()?.message}")
                        Toast.makeText(this@ImageFullActivity, "Delete failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()

        }

        // Tint and load image with Glide
        val placeholderDrawable = ContextCompat.getDrawable(this, R.drawable.placeholder_photos)?.mutate()
        placeholderDrawable?.setTint(Color.WHITE)
        placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)

        if (data.mimeType == "video/mp4"){
            playerView.visibility = android.view.View.VISIBLE
            imageView.visibility = android.view.View.GONE

            val videoUrl = "https://drive.google.com/uc?export=download&id=${data.fileId}"

            // Setup ExoPlayer
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                playerView.player = player
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
            }
        }else{
            imageView.visibility = android.view.View.VISIBLE
            playerView.visibility = android.view.View.GONE
            Glide.with(this)
                .load("https://drive.google.com/uc?export=view&id=${data.fileId}")
                .placeholder(placeholderDrawable)
                .error(R.drawable.mark)
                .into(imageView)
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

        repository.setDriveService(driveService)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.release()
        exoPlayer = null
    }

}