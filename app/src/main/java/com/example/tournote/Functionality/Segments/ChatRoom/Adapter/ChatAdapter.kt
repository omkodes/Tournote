package com.example.tournote.Functionality.Segments.ChatRoom.Adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatItem
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatMessage
import com.example.tournote.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(val context: Context): ListAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback()) {
    private var selectedMessageId: String? = null
    var selectionListener: ((selectedCount: Boolean) -> Unit)? = null


    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_OTHER = 2
        private const val VIEW_TYPE_DATE = 3
    }


    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE
            is ChatItem.MessageItem -> {
                if (item.message.isUser) VIEW_TYPE_USER else VIEW_TYPE_OTHER
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }

            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }

            VIEW_TYPE_OTHER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_other, parent, false)
                OtherMessageViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.label)
            is ChatItem.MessageItem -> {
                if (holder is UserMessageViewHolder) holder.bind(item.message)
                else if (holder is OtherMessageViewHolder) holder.bind(item.message,position)
            }
        }
    }


    // Moved formatTime function inside the class
    private fun formatTime(timestamp: Long): String {
        try {
            // Check if timestamp looks like it's in seconds (e.g., 1699778399), convert to millis
            val correctedTimestamp = if (timestamp < 1_000_000_000_000L) {
                timestamp * 1000
            } else timestamp

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(correctedTimestamp))
        } catch (e: Exception) {
            e.printStackTrace()
            return "" // fallback to empty
        }
    }


    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView = itemView.findViewById<TextView>(R.id.messageText)
        private val timestampTextView = itemView.findViewById<TextView>(R.id.timeText)
        private val editTV = itemView.findViewById<TextView>(R.id.edited)
        private val body = itemView.findViewById<LinearLayout>(R.id.halloutBody)

        fun bind(message: ChatMessage) {
            messageTextView.text = message.message_content
            timestampTextView.text = formatTime(message.timestamp)
            if (message.edited == true){
                editTV.visibility = View.VISIBLE
            }
            itemView.setBackgroundColor(
                if (message.isSelected)
                    ContextCompat.getColor(itemView.context, R.color.selected_bg)
                else
                    Color.TRANSPARENT
            )

            val isLastOfStreak = isLastMessageOfStreak(position, message.user_id)

            if (isLastOfStreak){
                body.background= ContextCompat.getDrawable(context,R.drawable.bg_message_user)
            }else{
                body.background= ContextCompat.getDrawable(context,R.drawable.bg_message_streak_user)
            }

            itemView.setOnLongClickListener {
                toggleSelection(message)
                true
            }

            itemView.setOnClickListener {
                if (selectedMessageId == message.message_id) clearSelection()
            }


        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateHeaderTextView = itemView.findViewById<TextView>(R.id.dateHeaderText)
        fun bind(label: String) {
            dateHeaderTextView.text = label
        }
    }


    inner class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView = itemView.findViewById<TextView>(R.id.messageText)
        private val timestampTextView = itemView.findViewById<TextView>(R.id.timeText)
        private val userNameTextView = itemView.findViewById<TextView>(R.id.userNameText)
        private val profilePicImageView = itemView.findViewById<ImageView>(R.id.user_avatar)
        private val editTV = itemView.findViewById<TextView>(R.id.edited)
        private val body = itemView.findViewById<LinearLayout>(R.id.halloutBody)

        fun bind(message: ChatMessage, position:Int) {
            messageTextView.text = message.message_content
            timestampTextView.text = formatTime(message.timestamp)
            userNameTextView.text = message.user_name
            if (message.edited == true){
                editTV.visibility = View.VISIBLE
            }

            // Show profile image logic
            val isLastOfUserStreak = isLastMessageOfStreak(position, message.user_id)
            if (isLastOfUserStreak){
                body?.background= ContextCompat.getDrawable(context,R.drawable.bg_message_bot)
                profilePicImageView.visibility = View.VISIBLE
            }else{
                body?.background= ContextCompat.getDrawable(context,R.drawable.bg_message_streak_bot)
                profilePicImageView.visibility = View.INVISIBLE
            }

            // Show/hide username based on start of streak
            val isFirstOfStreak = isFirstMessageOfStreak(position, message.user_id)
            userNameTextView.visibility = if (isFirstOfStreak) View.VISIBLE else View.GONE


            // Simplified Glide usage - placeholder and error handle null/empty URLs
            Glide.with(context)
                .load(message.profile_pic)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .into(profilePicImageView)
        }
    }


    private fun isFirstMessageOfStreak(position: Int, userId: String?): Boolean {
        // If it's the first message in the list, it's definitely first of a streak
        val previousIndex = position - 1
        if (previousIndex < 0) return true

        val prevItem = getItem(previousIndex)
        if (prevItem is ChatItem.MessageItem) {
            return prevItem.message.user_id != userId
        }
        return true
    }

    private fun isLastMessageOfStreak(position: Int, userId: String?): Boolean {
        // Check if next message exists
        val nextIndex = position + 1
        if (nextIndex >= itemCount) return true

        val nextItem = getItem(nextIndex)
        if (nextItem is ChatItem.MessageItem) {
            return nextItem.message.user_id != userId
        }
        return true
    }

    fun toggleSelection(message: ChatMessage) {
        val previousId = selectedMessageId
        val newId = message.message_id

        if (previousId == newId) {
            clearSelection()
        } else {
            val oldIndex = currentList.indexOfFirst {
                it is ChatItem.MessageItem && it.message.message_id == previousId
            }
            val newIndex = currentList.indexOfFirst {
                it is ChatItem.MessageItem && it.message.message_id == newId
            }

            (currentList.filterIsInstance<ChatItem.MessageItem>()).forEach {
                it.message.isSelected = false
            }

            message.isSelected = true
            selectedMessageId = newId

            if (oldIndex >= 0) notifyItemChanged(oldIndex)
            if (newIndex >= 0) notifyItemChanged(newIndex)

            selectionListener?.invoke(true)
        }
    }


    fun clearSelection() {
        val previousIndex = currentList.indexOfFirst {
            it is ChatItem.MessageItem && it.message.message_id == selectedMessageId
        }

        (currentList.filterIsInstance<ChatItem.MessageItem>()).forEach {
            it.message.isSelected = false
        }

        selectedMessageId = null
        if (previousIndex >= 0) notifyItemChanged(previousIndex)

        selectionListener?.invoke(false)
    }

    fun getSelectedMessage(): ChatMessage? {
        return (currentList.filterIsInstance<ChatItem.MessageItem>())
            .firstOrNull { it.message.message_id == selectedMessageId }
            ?.message
    }


}

class ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return when {
            oldItem is ChatItem.DateHeader && newItem is ChatItem.DateHeader -> oldItem.label == newItem.label
            oldItem is ChatItem.MessageItem && newItem is ChatItem.MessageItem ->
                oldItem.message.message_id == newItem.message.message_id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem == newItem
    }
}
