package com.example.tournote.Functionality.Segments.Memories.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tournote.Functionality.Segments.Memories.adapter.ImageAdapter
import com.example.tournote.Functionality.Segments.Memories.data.PhotoItem
import com.example.tournote.R
import com.example.tournote.databinding.ActivityImageBinding

class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val data = intent.getParcelableExtra<PhotoItem>("data")
        if (data != null) {
            binding.txtTitle.text = data.date
            binding.imageRecyclerView.layoutManager = GridLayoutManager(this, 3)
            binding.imageRecyclerView.adapter = ImageAdapter(data.allPhotos,this)
        }else{
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

    }
}