package com.example.tournote.Functionality.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tournote.Functionality.Segments.ChatRoom.Fragment.ChatsFragment
import com.example.tournote.Functionality.Segments.Expenses.Fragment.ExpensesFragment
import com.example.tournote.Functionality.Segments.Memories.MemoriesFragment
import com.example.tournote.Functionality.Segments.SmartRoutePlanner.Fragment.SmartRoutePlannerFragment
import com.example.tournote.Functionality.Segments.TrackFriends.Fragment.TrackFriendsFragment

class FunctionalityPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0-> SmartRoutePlannerFragment()
            1-> MemoriesFragment()
            2-> ExpensesFragment()
            3-> ChatsFragment()
            4-> TrackFriendsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
