package com.example.tournote.Functionality.Segments.ChatRoom.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.Groups.Activity.activityGroupInfo
import com.example.tournote.Functionality.Segments.ChatRoom.Adapter.ChatAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.Interface.MenuActionHandler
import com.example.tournote.Functionality.Segments.ChatRoom.ViewModel.ChatViewModel
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.ChatMessage
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatsFragment : androidx.fragment.app.Fragment(), MenuActionHandler {

    private val mainViewModel: MainActivityViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    lateinit var recyclerViewChat : RecyclerView
    lateinit var chatAdapter : ChatAdapter
    private val authViewmodel: authViewModel by viewModels()
    lateinit var editTextMessage: EditText
    lateinit var buttonSend : FloatingActionButton
    lateinit var buttonEdit : FloatingActionButton
    private var grp_id: String? = null

    lateinit var moreOptions: ImageButton

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        recyclerViewChat = view.findViewById<RecyclerView>(R.id.recyclerViewChat)
        editTextMessage = view.findViewById<EditText>(R.id.editTextMessage)
        buttonSend = view.findViewById<FloatingActionButton>(R.id.buttonSend)
        buttonEdit = view.findViewById<FloatingActionButton>(R.id.buttonEdit)

        //toolbar
        val group_logo = view.findViewById<ImageView>(R.id.grp_logo)
        val group_name = view.findViewById<TextView>(R.id.grp_name)
        view.findViewById<LinearLayout>(R.id.toolbar).setOnClickListener {
            val intent = Intent(requireContext(), activityGroupInfo::class.java)
            //intent.putExtra("GROUP_ID", GlobalClass.GroupDetails_Everything.groupID)
            startActivity(intent)
        }

        group_name.text = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.name
        if (GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.profilePic == "null" || GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.profilePic.isNullOrBlank()) {
            group_logo.setImageResource(R.drawable.defaultgroupimage)
        } else {
            // Load the image using Glide or any other image loading library
            com.bumptech.glide.Glide.with(this)
                .load(GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.profilePic)
                .placeholder(R.drawable.defaultgroupimage)
                .error(R.drawable.defaultgroupimage)
                .into(group_logo)
        }

        moreOptions = view.findViewById(R.id.more_options)

        moreOptions.setOnClickListener {
            showPopupMenu(it)
        }


        chatAdapter = ChatAdapter(requireContext())
        mainViewModel.loadChatRoom()


        recyclerViewChat.adapter =chatAdapter
        recyclerViewChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        chatViewModel.getAllMsgFromDB(GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.groupID?:"",requireContext())
        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            val updated_msg = processMessages(messages)
            val itemList = chatViewModel.groupMessagesByDate(updated_msg)
            Log.d("ChatDebug","lets see:${messages}")
            chatAdapter.submitList(itemList) {
                recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        chatViewModel.listenMsg()
        chatViewModel.listenMsgDelete()
        chatViewModel.listenMsgUpdate()

        return view
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val toolbar = view.findViewById<View>(R.id.toolbar)
//        val rootLayout = view.findViewById<View>(R.id.rootLayout)
//
//        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
//            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//            toolbar.setPadding(
//                toolbar.paddingLeft,
//                12,
//                toolbar.paddingRight,
//                toolbar.paddingBottom
//            )
//            insets
//        }
//    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            onMenuActionSelected(item.itemId)
            true
        }

        popup.show()
    }


    override fun onResume() {
        super.onResume()


        chatAdapter.selectionListener={

         if (it == true){
             moreOptions.visibility = View.VISIBLE
             buttonSend.visibility = View.GONE
             buttonEdit.visibility = View.VISIBLE
         }else{
             moreOptions.visibility = View.GONE
             buttonSend.visibility = View.VISIBLE
             buttonEdit.visibility = View.GONE
         }

        }

    }

    override fun onMenuActionSelected(actionId: Int) {
        when (actionId) {
            R.id.action_edit -> {
                val msg_data = chatAdapter.getSelectedMessage()
                editTextMessage.setText(msg_data?.message_content)

                buttonEdit.setOnClickListener {
                    val content = editTextMessage.text.toString()
                    msg_data?.message_content = content
                    if (msg_data != null){
                        chatViewModel.sendUpdate(msg_data)
                        Toast.makeText(requireContext(), "Message edited successfully", Toast.LENGTH_SHORT).show()
                        chatAdapter.clearSelection()
                        editTextMessage.text.clear()
                    }else{
                        Toast.makeText(requireContext(), "Message can't be edit", Toast.LENGTH_SHORT).show()
                        editTextMessage.text.clear()
                        chatAdapter.clearSelection()
                    }
                }

            }
            R.id.action_delete -> {
                val msg_data = chatAdapter.getSelectedMessage()
                if (msg_data != null){
                    chatViewModel.sendDelete(msg_data)
                    chatAdapter.clearSelection()
                    Toast.makeText(requireContext(), "Message deleted successfully", Toast.LENGTH_SHORT).show()
                }else{
                    chatAdapter.clearSelection()
                    Toast.makeText(requireContext(), "Message can't be delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setupRecyclerView() {
//        recyclerViewChat.apply {
//            chatAdapter = ChatAdapter()
//            adapter = chatAdapter
//            layoutManager = LinearLayoutManager(requireContext()).apply {
//                stackFromEnd = true
//            }
//            clipToPadding = false
//        }


    }


    private fun setupClickListeners() {
        buttonSend.setOnClickListener {
            sendMsgTOView()
        }
            editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            // use when user presses "Send" on keyboard
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMsgTOView()
                true
            } else false
        }

        // Handle focus changes to scroll to bottom
        editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && ::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
                recyclerViewChat.post {
                    recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun observeViewModel() {

        chatViewModel.resendMsg.observe(viewLifecycleOwner) { it ->
            if (it == true){
                Toast.makeText(requireContext(), "Error: Message is not saved", Toast.LENGTH_SHORT).show()
                chatViewModel.clearResend()
            }
        }

        mainViewModel.groupId.observe(viewLifecycleOwner) {
            grp_id = it
        }

        chatViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            buttonSend.isEnabled = !isLoading
        }

        chatViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                chatViewModel.clearError()
            }
        }
    }

    private fun processMessages(messages: List<ChatMessage>): List<ChatMessage> {
        val userID = authViewmodel.repo.getUid()
        for (message in messages) {
            if (message.user_id == userID) {
                message.isUser = true  // update field
            }else{
                message.isUser = false
            }
        }
        return messages
    }


    fun sendMsgTOView(){

        val msg_content = editTextMessage.text.toString()

        if (msg_content.trim().isNotEmpty()) {

            CoroutineScope(Dispatchers.Main).launch {
                val userID = authViewmodel.repo.getUid()
                if (userID != null) {
                    val user_data = authViewmodel.repo.userDetailGetLogin(userID)
                    if (user_data != null && GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.groupID != null) {
                        val user_name = user_data.child("name").value.toString()
                        val profile_url: String? = user_data.child("profilePic").value.toString()
                        val group_id = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }?.groupID
                        val msg_id = chatViewModel.generateShortMessageId()
                        val msg = ChatMessage(
                            msg_content,
                            user_name,
                            group_id,
                            userID,
                            profile_pic = profile_url,
                            message_id = msg_id,
                            isUser = true
                        )
                        chatViewModel.sendMsg(msg)
                    }else{
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(requireContext(), "UserID not found", Toast.LENGTH_SHORT).show()
                }
            }

            editTextMessage.text?.clear()

        }

    }




}