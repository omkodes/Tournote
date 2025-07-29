package com.example.tournote.Groups.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.Groups.Adapter.FetchIncludedGroupDetailsRecyclerViewAdapter
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel2
import com.example.tournote.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: GroupSelectorActivityViewModel2 by activityViewModels()
    private lateinit var adapter: FetchIncludedGroupDetailsRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        // The ViewModel's init block already handles the data fetching
    }

    private fun setupRecyclerView() {
        adapter = FetchIncludedGroupDetailsRecyclerViewAdapter(requireContext())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            // Use submitList() to update the adapter with DiffUtil
            // This is the key change!
            adapter.submitList(groups)

            if (groups.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                // Show empty state view
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                // Hide empty state view
            }
        }

        // ... (rest of the code is unchanged)
    }
}