package com.example.tournote.Functionality.Segments.Memories.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.Memories.data.PhotosData
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FullScreenAdapter(private val items: List<PhotosData>, private val lifecycle: Lifecycle): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].mimeType.startsWith("image/")) {
            TYPE_IMAGE
        } else {
            TYPE_VIDEO
        }
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val data = items[position]
        val fileId = data.fileId
        val userId = GlobalClass.Me?.uid ?: return
        val context = holder.itemView.context

        val placeholderDrawable = ContextCompat.getDrawable(context, R.drawable.placeholder_photos)?.mutate()
        placeholderDrawable?.setTint(Color.WHITE)
        placeholderDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)

        val dbRef = FirebaseDatabase.getInstance().getReference("likes").child(fileId)

        // Listener to update like count and heart icon live
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.child("count").getValue(Int::class.java) ?: 0
                val isLiked = snapshot.child("users").child(userId).getValue(Boolean::class.java) == true

                data.likeCount = count
                data.isLiked = isLiked

                if (holder is ImageViewHolder) {
                    holder.likeText.text = "$count likes"
                    holder.heartIcon.setImageResource(
                        if (isLiked) R.drawable.heart__1_ else R.drawable.heart
                    )
                } else if (holder is VideoViewHolder) {
                    holder.likeText.text = "$count likes"
                    holder.heartIcon.setImageResource(
                        if (isLiked) R.drawable.heart__1_ else R.drawable.heart
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Handle image
        if (holder is ImageViewHolder) {
            Glide.with(context)
                .load("https://drive.google.com/uc?export=view&id=${data.fileId}")
                .placeholder(placeholderDrawable)
                .error(R.drawable.mark)
                .into(holder.photoView)

            holder.photoView.setOnTouchListener(object : View.OnTouchListener {
                private var lastTapTime = 0L

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            CoroutineScope(Dispatchers.IO).launch {
                                toggleLike(userId, fileId)
                            }
                        }
                        lastTapTime = now
                    }
                    return false
                }
            })

            holder.heartIcon.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    toggleLike(userId, fileId)
                }
            }
        }

        // Handle video
        else if (holder is VideoViewHolder) {
            val player = ExoPlayer.Builder(context).build()
            val videoUrl = "https://drive.google.com/uc?export=download&id=${fileId}"
            holder.playerView.player = player
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            player.playWhenReady = true

            holder.setPlayer(player)

            holder.heartIcon.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    toggleLike(userId, fileId)
                }
            }
        }
    }


    override fun getItemCount(): Int {
        return items.size
    }

    private suspend fun toggleLike(userId: String, fileId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("likes").child(fileId)

        val snapshot = dbRef.child("users").child(userId).get().await()
        val liked = snapshot.getValue(Boolean::class.java) == true

        if (liked) {
            dbRef.child("users").child(userId).removeValue()
            dbRef.child("count").runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val current = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = (current - 1).coerceAtLeast(0)
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
            })
        } else {
            dbRef.child("users").child(userId).setValue(true)
            dbRef.child("count").runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val current = mutableData.getValue(Int::class.java) ?: 0
                    mutableData.value = current + 1
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
            })
        }
    }



    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
        val heartIcon: ImageView = view.findViewById(R.id.heartIcon)
        val likeText: TextView = view.findViewById(R.id.likeCountText)
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val heartIcon: ImageView = view.findViewById(R.id.heartIcon)
        val likeText: TextView = view.findViewById(R.id.likeCountText)
        private var player: ExoPlayer? = null
        fun setPlayer(p: ExoPlayer) { this.player = p }
        fun releasePlayer() { player?.release() }
    }

}