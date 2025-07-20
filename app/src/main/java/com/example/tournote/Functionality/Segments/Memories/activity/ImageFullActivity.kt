package com.example.tournote.Functionality.Segments.Memories.activity

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tournote.R

class ImageFullActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_full)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val fileID = intent.getStringExtra("imagePath") ?: ""
        val imageView: ImageView = findViewById(R.id.fullImageView)
        val btnBack = findViewById<ImageView>(R.id.buttonBack)

        btnBack.setOnClickListener {
            finish()
        }

        // Tint and load image with Glide
        val placeholderDrawable = ContextCompat.getDrawable(this, R.drawable.placeholder_photos)?.mutate()
        placeholderDrawable?.setTint(Color.WHITE)
        placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)

        Glide.with(this)
            .load("https://drive.google.com/uc?export=view&id=${fileID}")
            .placeholder(placeholderDrawable)
            .error(R.drawable.mark)
            .into(imageView)

    }
}