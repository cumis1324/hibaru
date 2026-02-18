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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theflexproject.thunder.R
import com.theflexproject.thunder.databinding.FragmentHomeBinding
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.HomeSectionAdapter
import com.theflexproject.thunder.ui.home.HomeSectionItem
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
        // If we want to use the TV layout, we need to manually inflate it since Binding is tied to a specific XML
        // Actually, ViewBinding for fragment_home might work if IDs are shared, but it's safer to use the binding
        // or just let binding inflation handle it if we use layout aliases. 
        // But here we'll just inflate the specific layout and use binding for common IDs.
        if (isTVDevice) {
            val tvView = inflater.inflate(R.layout.fragment_home_tv, container, false)
            _binding = FragmentHomeBinding.bind(tvView)
        } else {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d("HomeFragment", "onViewCreated: Initializing ads and UI")
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private val isTVDevice: Boolean by lazy {
        val uiModeManager = requireContext().getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTelevision = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val packageManager = requireContext().packageManager
        val hasLeanback = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        val hasNoTouch = !packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)
        isTelevision || hasLeanback || hasNoTouch
    }

    private fun setupRecyclerView() {
        adapter = HomeSectionAdapter(
            isTV = isTVDevice,
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
            },
            onFocusChange = { item ->
                if (isTVDevice) {
                    updateDynamicUI(item)
                }
            }
        )
        binding.homeRecyclerView.apply {
            this.adapter = this@HomeFragment.adapter
            
            if (this is androidx.leanback.widget.VerticalGridView) {
                // Leanback specific vertical configuration
                setItemSpacing(0)
                // Window alignment ensures the focused row stays in a consistent area
                windowAlignment = androidx.leanback.widget.BaseGridView.WINDOW_ALIGN_LOW_EDGE
                windowAlignmentOffsetPercent = 65f // Reverted from 75f to restore original start position
                
                // Add padding to allow the first item to have some clearance from the top
                setPadding(0, 0, 0, 0)
                clipToPadding = false

                // TV Focus Sync: When a new row is selected vertically, update the preview area
                setOnChildViewHolderSelectedListener(object : androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                    override fun onChildViewHolderSelected(
                        parent: RecyclerView,
                        child: RecyclerView.ViewHolder?,
                        position: Int,
                        subposition: Int
                    ) {
                        if (child is HomeSectionAdapter.SectionViewHolder) {
                            child.syncSelectedFocus()
                        }

                        // Infinite Scroll Trigger for Genres: Load next batch when user is near bottom
                        if (position >= this@HomeFragment.adapter.itemCount - 2) {
                            viewModel.loadNextGenreBatch()
                        }
                    }
                })
            } else {
                // Mobile Infinite Scroll Trigger
                binding.homeRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val totalItemCount = layoutManager.itemCount
                        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                        if (!viewModel.uiState.value.isLoadingMoreGenres && totalItemCount <= (lastVisibleItem + 2)) {
                            viewModel.loadNextGenreBatch()
                        }
                    }
                })
            }
        }
    }

    private fun updateDynamicUI(item: com.theflexproject.thunder.model.MyMedia) {
        val context = requireContext()
        val activity = requireActivity() as? com.theflexproject.thunder.MainActivity
        
        var title: String? = null
        var backdropPath: String? = null
        var posterPath: String? = null
        var overview: String? = null
        var metadata = ""
        var logoPath: String? = null

        when (item) {
            is Movie -> {
                // If the movie's original language is Indonesian ("id"), use that as the title per request;
                // otherwise use the movie title or fallback to file_name.
                title = if (item.original_language == "id") item.original_title else item.title ?: item.file_name
                backdropPath = item.backdrop_path
                posterPath = item.poster_path
                overview = item.overview
                logoPath = item.logo_path
                val year = item.release_date?.take(4) ?: ""
                val rating = if (item.vote_average > 0) " | ${java.text.DecimalFormat("0.0").format(item.vote_average)} ★" else ""
                metadata = "$year$rating"
            }
            is TVShow -> {
                title = item.name
                backdropPath = item.backdrop_path
                posterPath = item.poster_path
                overview = item.overview
                logoPath = item.logo_path
                val year = item.first_air_date?.take(4) ?: ""
                val rating = if (item.vote_average > 0) " | ${java.text.DecimalFormat("0.0").format(item.vote_average)} ★" else ""
                metadata = "$year$rating"
            }
        }

        // 1. Update Global Background in MainActivity
        val path = backdropPath ?: posterPath
        if (path != null) {
            val bgImageView = activity?.findViewById<android.widget.ImageView>(R.id.globalDynamicBackground)
            if (bgImageView != null) {
                com.bumptech.glide.Glide.with(context)
                    .load(com.theflexproject.thunder.Constants.TMDB_BACKDROP_IMAGE_BASE_URL + path)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                    .into(bgImageView)
            }
        }
        val previewLogo = binding.root.findViewById<android.widget.ImageView>(R.id.previewLogo)
        if (!logoPath.isNullOrEmpty()) {
            previewLogo?.visibility = View.VISIBLE
            binding.previewTitle.visibility = View.GONE
            com.bumptech.glide.Glide.with(context)
                .load(com.theflexproject.thunder.Constants.TMDB_IMAGE_BASE_URL + logoPath)
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                .into(previewLogo!!)
        } else {
            previewLogo?.visibility = View.GONE
            binding.previewTitle.visibility = View.VISIBLE
            binding.previewTitle.text = title
        }

        binding.previewMetadata.text = metadata
        binding.previewDescription.text = overview
        // 2. Update Preview Area in HomeFragment
        binding.dynamicPreview.visibility = View.VISIBLE

    }

    private fun setupSwipeRefresh() {
        if (isTVDevice) {
            binding.swipeRefresh.isEnabled = false
        } else {
            binding.swipeRefresh.setOnRefreshListener {
                viewModel.loadHomeData()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading

                    // Convert HomeSection list to HomeSectionItem list with ads interspersed
                    val itemsWithAds = mutableListOf<HomeSectionItem>()
                    state.sections.forEachIndexed { index, section ->
                        itemsWithAds.add(HomeSectionItem.SectionItem(section))
                        // Add banner ad after every 2 sections (or customize as needed)
                        if ((index + 1) % 2 == 0) {
                            itemsWithAds.add(HomeSectionItem.AdBannerItem)
                        }
                    }

                    adapter.submitList(itemsWithAds)

                    // Show/Hide bottom loading indicator for infinite scroll genres (Both TV and Mobile)
                    val bottomProgress = binding.root.findViewById<android.view.View>(R.id.bottomGenreProgress)
                    bottomProgress?.visibility = if (state.isLoadingMoreGenres) android.view.View.VISIBLE else android.view.View.GONE

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
