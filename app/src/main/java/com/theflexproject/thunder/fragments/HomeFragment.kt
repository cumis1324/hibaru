package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.theflexproject.thunder.R
import com.theflexproject.thunder.databinding.FragmentHomeBinding
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.HomeSectionAdapter
import com.theflexproject.thunder.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: HomeSectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d("HomeFragment", "onViewCreated: Initializing ads and UI")
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        
        // Initialize and load Ads
        activity?.let {
            com.theflexproject.thunder.utils.UnityAdHelper.init(it)
            com.theflexproject.thunder.utils.UnityAdHelper.loadBanner(it, binding.bannerContainer)
        }
    }

    private fun setupRecyclerView() {
        adapter = HomeSectionAdapter(
            onItemClick = { item ->
                when (item) {
                    is Movie -> {
                        val bundle = Bundle().apply {
                            putInt("videoId", item.id)
                            putBoolean("isMovie", true)
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_playerFragment, bundle)
                    }
                    is TVShow -> {
                        val bundle = Bundle().apply {
                            putInt("tvShowId", item.id)
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_tvShowDetailsFragment, bundle)
                    }
                }
            },
            onSeeAllClick = { section ->
                // TODO: Implement "See All" navigation if needed
            },
            onLoadMore = { sectionId ->
                viewModel.loadMore(sectionId)
            }
        )
        binding.homeRecyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHomeData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    adapter.submitList(state.sections)
                    
                    if (state.error != null) {
                        // Handle error (e.g., show Toast or Snackbar)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
