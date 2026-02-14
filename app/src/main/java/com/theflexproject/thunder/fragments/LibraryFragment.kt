package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.theflexproject.thunder.R
import com.theflexproject.thunder.adapter.GenreAdapter
import com.theflexproject.thunder.databinding.FragmentLibraryBinding
import com.theflexproject.thunder.model.Genres
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.MediaCarouselAdapter
import com.theflexproject.thunder.ui.library.LibraryViewModel
import com.theflexproject.thunder.ui.library.MediaType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels()
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaAdapter: MediaCarouselAdapter
    private lateinit var genreAdapter: GenreAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupSpinners()
        setupRecyclerViews()
        observeState()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setMediaType(MediaType.MOVIE)
                    1 -> viewModel.setMediaType(MediaType.TV_SHOW)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSpinners() {
        val orderByAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.order_by_options,
            android.R.layout.simple_spinner_item
        )
        orderByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.orderBy.setAdapter(orderByAdapter)

        val sortByAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_by_options,
            android.R.layout.simple_spinner_item
        )
        sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortBy.setAdapter(sortByAdapter)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                 viewModel.setSortOrder(
                     binding.sortBy.selectedItem.toString(),
                     binding.orderBy.selectedItem.toString()
                 )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.orderBy.onItemSelectedListener = listener
        binding.sortBy.onItemSelectedListener = listener
    }

    private fun setupRecyclerViews() {
        // Content RecyclerView
        mediaAdapter = MediaCarouselAdapter(
            onItemClick = { media -> onMediaClicked(media) },
            onLoadMore = { /* Infinite scroll not implemented for library yet */ }
        )
        
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (dpWidth / 120).toInt()

        binding.recyclerLibrary.apply {
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = mediaAdapter
        }

        // Genre RecyclerView
        // Note: GenreAdapter requires a custom layout. I'll reuse existing GenreAdapter.
        // But GenreAdapter in Java takes Context, List, OnGenreClickListener.
        // I need to check if I can use it directly or if I need to wrap/convert it.
        // It's a standard Adapter, so should be fine to use from Kotlin.
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    
                    // Update Content
                    if (state.selectedMediaType == MediaType.MOVIE) {
                        mediaAdapter.submitList(state.movies)
                    } else {
                        mediaAdapter.submitList(state.tvShows)
                    }

                    // Update Genre Adapter
                    // We need to re-create or update GenreAdapter when genres change
                    // Ideally check if list changed. 
                    // GenreAdapter.java seems to take list in constructor.
                    // I'll re-instantiate for now as list is small.
                    genreAdapter = GenreAdapter(requireActivity(), state.genres) { id ->
                         viewModel.setGenre(if (id == state.selectedGenreId) null else id)
                    }
                    binding.recyclerGenre.setAdapter(genreAdapter)
                    
                    if (state.isLoading) {
                        binding.progressBar.visibility = View.VISIBLE
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }

                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onMediaClicked(media: MyMedia) {
        if (media is Movie) {
             val bundle = bundleOf("videoId" to media.id, "isMovie" to true)
             findNavController().navigate(R.id.playerFragment, bundle)
        } else if (media is TVShow) {
            val bundle = bundleOf("tmdbId" to media.id)
            findNavController().navigate(R.id.tvShowDetailsFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
