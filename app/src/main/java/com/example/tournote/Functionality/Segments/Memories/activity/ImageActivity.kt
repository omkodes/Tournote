package com.example.tournote.Functionality.Segments.Memories.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tournote.Functionality.Segments.Memories.adapter.ImageAdapter
import com.example.tournote.Functionality.Segments.Memories.data.PhotoItem
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.R
import com.example.tournote.databinding.ActivityImageBinding

class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    private lateinit var adapter: ImageAdapter
    private var imageList: MutableList<PhotosData> = mutableListOf()

    private val fullImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedPosition = result.data?.getIntExtra("deleted_position", -1) ?: -1
            if (deletedPosition != -1 && deletedPosition < imageList.size) {
                imageList.removeAt(deletedPosition)
                adapter.notifyItemRemoved(deletedPosition)
            }
        }
    }

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
            imageList = data.allPhotos.toMutableList()
            adapter = ImageAdapter(imageList, this, fullImageLauncher)
            binding.imageRecyclerView.layoutManager = GridLayoutManager(this, 3)
            binding.imageRecyclerView.adapter = adapter
        } else {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
            finish()
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.taskbar)

        binding.btnBack.setOnClickListener {
            finish()
        }

    }
}