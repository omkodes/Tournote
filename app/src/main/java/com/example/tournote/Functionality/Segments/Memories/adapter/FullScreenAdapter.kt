package com.example.tournote.Functionality.Segments.Memories.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.R
import com.github.chrisbanes.photoview.PhotoView

class FullScreenAdapter(private val items: List<PhotosData>, private val lifecycle: Lifecycle): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_image, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_video, parent, false)
            VideoViewHolder(view)
        }
 }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val data = items[position]
        if (holder is ImageViewHolder) {
            val placeholderDrawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.placeholder_photos)?.mutate()
            placeholderDrawable?.setTint(Color.WHITE)
            placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)
            Glide.with(holder.itemView.context)
                .load("https://drive.google.com/uc?export=view&id=${data.fileId}")
                .placeholder(placeholderDrawable)
                .error(R.drawable.mark)
                .into(holder.photoView)
        } else if (holder is VideoViewHolder) {
            val context = holder.itemView.context
            val player = ExoPlayer.Builder(context).build()
            val videoUrl = "https://drive.google.com/uc?export=download&id=${data.fileId}"
            holder.playerView.player = player
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            player.playWhenReady = true

            holder.setPlayer(player)
        }
    }


    override fun getItemCount(): Int {
        return items.size
    }


    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        private var player: ExoPlayer? = null
        fun setPlayer(p: ExoPlayer) { this.player = p }
        fun releasePlayer() { player?.release() }
    }

}