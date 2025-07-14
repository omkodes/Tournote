package com.example.tournote.Groups.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.activityProfileInfo
import com.example.tournote.R
import com.example.tournote.UserModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class grpMemberAdapter(val grpList: MutableList<UserModel>, val context: Context) : RecyclerView.Adapter<grpMemberAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_members, parent, false)
        return ViewHolder(view)
    }

    val repo = MainActivityRepository()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = grpList[position]
        holder.memberName.text = user.name

        Glide.with(context)
            .load(user.profilePic)
            .placeholder(R.drawable.profile_photosample)
            .error(R.drawable.profile_photosample)
            .into(holder.memberPic)

        holder.itemView.setOnClickListener {
            Log.d("grpadapter", "Member clicked: ${user.name}")

            val intent = Intent(context, activityProfileInfo::class.java)
            intent.putExtra("user", user)
            context.startActivity(intent)

            holder.body.setOnClickListener {
                val selectedGroup = GlobalClass.GroupDetails_Everything
                    .find { it.groupID == GlobalClass.selected_groupId }

                /*if (selectedGroup != null &&
                    (GlobalClass.Me == selectedGroup.owner || selectedGroup.admins.contains(GlobalClass.Me))) {
                    showBottomSheet(user,position)
                }else{
                    val intent = Intent(context, activityProfileInfo::class.java)
                    intent.putExtra("user", user)
                    context.startActivity(intent)
                }*/
            }

        }
    }


    fun updateList(newList: List<UserModel>) {
        grpList.clear()
        grpList.addAll(newList)
        notifyDataSetChanged()
    }

    /*private fun showBottomSheet(user: UserModel, position: Int) {
        val dialog = BottomSheetDialog(context).apply {
            setContentView(R.layout.bsfragment_admin)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val txtAdminAction = dialog.findViewById<TextView>(R.id.txtbtnAdmin)
        val btnAdmin = dialog.findViewById<RelativeLayout>(R.id.btnAdmin)
        val btnInfo = dialog.findViewById<RelativeLayout>(R.id.btnInfo)
        val btnRemoveMember = dialog.findViewById<RelativeLayout>(R.id.btnRemove)

        val isAdmin = viewModel.checkForPresence_AdminList(user)
        txtAdminAction?.text = if (isAdmin) "Remove from group admin" else "Make group admin"

        btnAdmin?.setOnClickListener {
            if (isAdmin) {
                viewModel.removeUserFromAdminList(user)
            } else {
                viewModel.addUserToAdminList(user)
            }

            notifyItemChanged(position) // ⬅️ This will rebind the item and update admin tag
            dialog.dismiss()
        }

        btnInfo?.setOnClickListener {
            val intent = Intent(context, activityProfileInfo::class.java)
            intent.putExtra("user", user)
            context.startActivity(intent)
        }

        dialog.show()
    }*/

    override fun getItemCount(): Int {
        return grpList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val memberName = itemView.findViewById<TextView>(R.id.txtMemberName)
        val memberPic = itemView.findViewById<ImageView>(R.id.imgMemberProfile)
        val body = itemView.findViewById<CardView>(R.id.itemBody)
    }
}