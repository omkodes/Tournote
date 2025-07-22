package com.example.tournote.Functionality.Segments.Memories

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tournote.Functionality.Segments.Memories.activity.ImageFullActivity
import com.example.tournote.Functionality.Segments.Memories.adapter.FolderAdapter
import com.example.tournote.Groups.Activity.activityGroupInfo
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import kotlin.getValue

class MemoriesFragment : Fragment() {

    private val viewModel: memoriesViewModel by viewModels()
    private lateinit var driveService: Drive
    private lateinit var folderAdapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_memories, container, false)

        //toolbar
        val group_logo = view.findViewById<ImageView>(R.id.grp_logo)
        val group_name = view.findViewById<TextView>(R.id.grp_name)
        val bar = view.findViewById<ProgressBar>(R.id.progressBar)
        val download = view.findViewById<ImageButton>(R.id.down_all)
        view.findViewById<LinearLayout>(R.id.toolbar).setOnClickListener {
            val intent = Intent(requireContext(), activityGroupInfo::class.java)
            //intent.putExtra("GROUP_ID", GlobalClass.GroupDetails_Everything.groupID)
            startActivity(intent)
        }

        download.setOnClickListener {
            setupDriveService()
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Download Memories")
            builder.setMessage("Are you sure you want to download all this items?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                bar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val result = viewModel.downloadAndShareZip(requireContext())
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        viewModel.isZipLoading.observe(viewLifecycleOwner) { isLoading ->
            bar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
        val recyclerView = view.findViewById<RecyclerView>(R.id.folder_recycler_view)
        val selectedGroup = GlobalClass.GroupDetails_Everything.find { it.groupID == GlobalClass.selected_groupId }
        // RecyclerView setup
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        bar.visibility = View.VISIBLE

        viewModel.repository.fetchPhotosGroupedByDateForGroup(
            groupName = selectedGroup!!.groupID ?: "",
            onResult = { folderList ->
                folderAdapter = FolderAdapter(folderList, requireContext())
                Log.d("MemoriesFragment", "folderList: $folderList")
                recyclerView.adapter = folderAdapter
                bar.visibility = View.GONE
            },
            onError = { exception ->
                folderAdapter = FolderAdapter(emptyList(),requireContext())
                recyclerView.adapter = folderAdapter
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.d("MemoriesFragment", "Error: ${exception.message}")
                bar.visibility = View.GONE
            }
        )

        // Inflate the layout for this fragment
        return view
    }

    private fun setupDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        val credential = GoogleAccountCredential.usingOAuth2(requireContext(), listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account?.account

        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Tournote").build()

            viewModel.repository.setDriveService(driveService)
    }

}