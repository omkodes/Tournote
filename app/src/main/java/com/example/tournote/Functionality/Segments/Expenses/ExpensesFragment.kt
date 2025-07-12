package com.example.tournote.Functionality.Segments.Expenses

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.example.tournote.Groups.Activity.activityGroupInfo
import com.example.tournote.Functionality.ViewModel.MainActivityViewModel
import com.example.tournote.GlobalClass
import com.example.tournote.R
import kotlin.getValue

class ExpensesFragment : Fragment() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

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

        // Inflate the layout for this fragment
        return view
    }

}