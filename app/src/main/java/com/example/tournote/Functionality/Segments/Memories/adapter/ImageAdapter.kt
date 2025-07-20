package com.example.tournote.Functionality.Segments.Memories.adapter

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Memories.activity.ImageFullActivity
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.R

class ImageAdapter(val imageList:List<PhotosData>, val context: Context):RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_thumbnail_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        val image = imageList[position]
        holder.itemView.setOnClickListener {
            setOnClick(image)
        }
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val imageSize = screenWidth / 3

        // Set CardView as square
        holder.imageView.layoutParams = holder.imageView.layoutParams.apply {
            width = imageSize
            height = imageSize
        }

        // Tint and load image with Glide
        val placeholderDrawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.placeholder_photos)?.mutate()
        placeholderDrawable?.setTint(Color.WHITE)
        placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)

        Glide.with(holder.itemView.context)
            .load("https://drive.google.com/uc?export=view&id=${image.fileId}")
            .placeholder(placeholderDrawable)
            .error(R.drawable.mark)
            .into(holder.imageView)
    }

    fun setOnClick(data: PhotosData){
        val intent = Intent(context, ImageFullActivity::class.java)
        intent.putExtra("imagePath", data.fileId)
        context.startActivity(intent)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ImageViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.findViewById<ImageView>(R.id.image)
    }
}