package com.example.tournote.Functionality.Segments.Memories.adapter

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColor
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Memories.activity.ImageActivity
import com.example.tournote.Functionality.Segments.Memories.data.PhotoItem
import com.example.tournote.R

class FolderAdapter(val folderList: List<PhotoItem>,val context: android.content.Context): RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.folder_photos_item, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: FolderViewHolder,
        position: Int
    ) {
        val folder = folderList[position]
        holder.title.text = folder.date
        holder.itemView.setOnClickListener {
            setOnClick(folder)
        }

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val imageSize = screenWidth / 2

        // Set CardView as square
        holder.card.layoutParams = holder.card.layoutParams.apply {
            width = imageSize
            height = imageSize
        }

        // Set ImageView to match parent (full size of CardView)
        holder.image.layoutParams = holder.image.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        // Tint and load image with Glide
        val placeholderDrawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.placeholder_photos)?.mutate()
        placeholderDrawable?.setTint(Color.WHITE)
        placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)

        Glide.with(holder.itemView.context)
            .load("https://drive.google.com/uc?export=view&id=${folder.thumbnail}")
            .placeholder(placeholderDrawable)
            .error(R.drawable.mark)
            .into(holder.image)
    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    fun setOnClick(data: PhotoItem){
        val intent = Intent(context, ImageActivity::class.java)
        intent.putExtra("data", data)
        context.startActivity(intent)
    }


    class FolderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.folderThumbnail)
        val title: TextView = itemView.findViewById(R.id.folderName)
        val card: ViewGroup = itemView.findViewById(R.id.cardView)
    }

}