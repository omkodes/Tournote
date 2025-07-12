package com.example.tournote.Groups.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.Groups.Adapter.FetchIncludedGroupDetailsRecyclerViewAdapter
import com.example.tournote.Groups.ViewModel.GroupSelectorActivityViewModel
import com.example.tournote.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: GroupSelectorActivityViewModel by activityViewModels()
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

        // Fetch groups when fragment is created
        viewModel.fetchGroupDetails()
    }

    private fun setupRecyclerView() {
        // Initialize adapter once
        adapter = FetchIncludedGroupDetailsRecyclerViewAdapter(requireContext())

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun observeViewModel() {
        // Observe groups data
        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            adapter.updateGroupList(groups)

            // Handle empty state
            if (groups.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                // Show empty state view if you have one
                // binding.emptyStateView.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                // binding.emptyStateView.visibility = View.GONE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Show loading indicator
                // binding.progressBar.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                // Hide loading indicator
                // binding.progressBar.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe toast messages
        viewModel.toastmsg.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up if needed
    }
}