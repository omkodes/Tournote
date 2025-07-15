package com.example.tournote.Functionality.Segments.ChatRoom.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatItem
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatMessage
import com.example.tournote.Functionality.Segments.ChatRoom.Repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ChatViewModel: ViewModel() {

    private val repo: ChatRepository = ChatRepository()

    var hasStartedListening = false
    private val _incomeMessage = MutableLiveData<ChatMessage>()
    val incomeMessage : LiveData<ChatMessage> get() = _incomeMessage

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> get()  = _messages


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val messageList = mutableListOf<ChatMessage>()

    private val _resendMsg = MutableLiveData<Boolean>(false)
    val resendMsg: LiveData<Boolean> get() = _resendMsg

    var currentRoomJoined: String? = null

    init {

    }


    fun sendMsg(userMessage: ChatMessage){
        Log.d("ChatDebug", "Sending message: $userMessage")
        if (userMessage.message_content?.trim().isNullOrEmpty()) return

        messageList.add(userMessage)
        _messages.value = messageList.toList()

        repo.sendMessage(userMessage, ack = {
            val res = it[0] as JSONObject
            if (res.optString("status") == "error"){
                _resendMsg.value = true
                Log.d("send_msg_db",res.optString("debug"))
            }else{
            }
        })
    }

    fun joinROOM(groupId: String){

    }

    fun listenMsg() {
        if (hasStartedListening) return
        hasStartedListening = true

        repo.listenMessage { args ->
            val data = args[0] as JSONObject
            val msg = ChatMessage(
                message_id = data.optString("message_id"),
                message_content = data.optString("message_content"),
                user_name = data.optString("user_name"),
                group_id = data.optString("group_id"),
                user_id = data.optString("user_id"),
                timestamp = data.optLong("timestamp"),
                edited = data.optBoolean("edited"),
                isUser = false,
                profile_pic = data.optString("profile_pic")
            )

            // 👇 Switch to main thread for LiveData update
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    messageList.add(msg)
                    _messages.value = messageList.toList()
                }
            }
        }
    }

    fun sendUpdate(userMessage: ChatMessage){
        Log.d("ChatDebug", "Sending message: $userMessage")
        if (userMessage.message_content?.trim().isNullOrEmpty()) return
        userMessage.edited = true
        val index = messageList.indexOfFirst { it.message_id == userMessage.message_id }
        if (index != -1) {
            messageList[index] = userMessage  // Replace the element
        }
        _messages.value = messageList.toList()

        repo.updateMessage(userMessage, ack = {
            val res = it[0] as JSONObject
            if (res.optString("status") == "error"){
                _resendMsg.value = true
                Log.d("send_msg_db",res.optString("debug"))
            }
        })
    }

    fun sendDelete(userMessage: ChatMessage){
        Log.d("ChatDebug", "Sending message: $userMessage")
        if (userMessage.message_content?.trim().isNullOrEmpty()) return

        val index = messageList.indexOfFirst { it.message_id == userMessage.message_id }
        if (index != -1) {
            messageList[index].message_content = "⊘ Message Deleted."
            messageList[index].edited = false
        }
        _messages.value = messageList.toList()

        val data = JSONObject().put("message_id", userMessage.message_id).put("group_id", userMessage.group_id).put("user_id", userMessage.user_id)

        repo.deleteMessage(data, ack = {
            val res = it[0] as JSONObject
            if (res.optString("status") == "error"){
                _resendMsg.value = true
                Log.d("send_msg_db",res.optString("debug"))
            }
        })
    }

    fun listenMsgDelete() {
        repo.listenDelete { args ->
            Log.d("ChatDebug", "Deleting message: $args")
            val data = args[0] as JSONObject
            val msg_id = data.optString("message_id")

            // 👇 Switch to main thread for LiveData update
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    val index = messageList.indexOfFirst { it.message_id == msg_id }
                    val msg_data = ChatMessage(
                        message_id = msg_id,
                        message_content = "⊘ Message Deleted.",
                        user_name = messageList[index].user_name,
                        group_id = messageList[index].group_id,
                        user_id = messageList[index].user_id,
                        timestamp = messageList[index].timestamp,
                        edited = false,
                        isUser = false,
                        profile_pic = messageList[index].profile_pic
                    )
                    if (index != -1) {
                        messageList[index] = msg_data  // Replace the element
                    }
                    _messages.value = messageList.toList()
                }
            }
        }
    }



    fun listenMsgUpdate() {
        repo.listenUpdate { args ->
            val data = args[0] as JSONObject
            val msg_id = data.optString("message_id")
            val msg = ChatMessage(
                message_id = data.optString("message_id"),
                message_content = data.optString("message_content"),
                user_name = data.optString("user_name"),
                group_id = data.optString("group_id"),
                user_id = data.optString("user_id"),
                timestamp = data.optLong("timestamp"),
                edited = true,
                isUser = false,
                profile_pic = data.optString("profile_pic")
            )

            // 👇 Switch to main thread for LiveData update
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    val index = messageList.indexOfFirst { it.message_id == msg_id }
                    if (index != -1) {
                        messageList[index] = msg  // Replace the element
                    }
                    _messages.value = messageList.toList()
                }
            }
        }
    }


    fun getAllMsgFromDB(groupId: String, context: Context) {
        Log.d("ChatDebug", "Calling getAllMsgFromDB() for groupId: $groupId")

        viewModelScope.launch {
            try {
                val response = repo.getAllMsgByApi(groupId)
                if (response.isSuccessful) {
                    Log.d("ChatDebug", "✅ API call successful")

                    response.body()?.let { newMessages ->
                        Log.d("ChatDebug", "Received ${newMessages.size} messages from API")

                        withContext(Dispatchers.Main) {
                            // Clear and update the list
                            messageList.clear()
                            messageList.addAll(newMessages)

                            // Create a new list to trigger observer
                            _messages.value = messageList.toList()
                        }

                        Log.d("ChatDebug", "Updated _messages with ${messageList.size} messages")
                    } ?: run {
                        Log.d("ChatDebug", "Response body is null")
                        _error.value = "No data received"
                    }
                } else {
                    Log.d("ChatDebug", "❌ API call failed with code: ${response.code()}")
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.d("ChatDebug", "❗ Exception caught: ${e.message}")
                _error.value = e.message ?: "Failed to get messages"
            }
        }
    }

    fun groupMessagesByDate(messages: List<ChatMessage>): List<ChatItem> {
        val result = mutableListOf<ChatItem>()
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val labelFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        var lastDateKey: String? = null

        for (msg in messages.sortedBy { it.timestamp }) {
            val msgDate = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
            val msgKey = dateFormatter.format(msgDate.time)

            if (msgKey != lastDateKey) {
                val label = when (msgKey) {
                    dateFormatter.format(today.time) -> "Today"
                    dateFormatter.format(yesterday.time) -> "Yesterday"
                    else -> labelFormatter.format(msgDate.time)
                }
                result.add(ChatItem.DateHeader(label))
                lastDateKey = msgKey
            }

            result.add(ChatItem.MessageItem(msg))
        }

        return result
    }



    fun generateShortMessageId(): String {
        return UUID.randomUUID().toString().take(8)
    }

    fun clearError() {
        _error.value = null
    }

    fun clearResend() {
        _resendMsg.value = false
    }


}