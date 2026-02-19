package com.theflexproject.thunder.fragments

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theflexproject.thunder.R
import com.theflexproject.thunder.databinding.FragmentSearchBinding
import com.theflexproject.thunder.databinding.FragmentSearchTvBinding
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.home.MediaCarouselAdapter
import com.theflexproject.thunder.ui.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()
    
    // Bindings for Mobile and TV
    private var mobileBinding: FragmentSearchBinding? = null
    private var tvBinding: FragmentSearchTvBinding? = null
    
    private lateinit var mediaAdapter: MediaCarouselAdapter
    private var isTVDevice = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isTVDevice = isTVDevice()
        
        return if (isTVDevice) {
            tvBinding = FragmentSearchTvBinding.inflate(inflater, container, false)
            tvBinding!!.root
        } else {
            mobileBinding = FragmentSearchBinding.inflate(inflater, container, false)
            mobileBinding!!.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchInput()
        observeState()
    }
    
    private fun setupRecyclerView() {
        mediaAdapter = MediaCarouselAdapter(
            onItemClick = { media -> onMediaClicked(media) },
            onLoadMore = { /* Infinite scroll for search can be added later */ }
        )
        
        if (isTVDevice) {
            tvBinding?.homeRecyclerView?.apply {
                setNumColumns(5) // Adjust columns for TV
                adapter = mediaAdapter
            }
        } else {
            val displayMetrics = resources.displayMetrics
            val dpWidth = displayMetrics.widthPixels / displayMetrics.density
            val spanCount = when {
                dpWidth < 600f -> 3
                dpWidth < 840f -> 6
                else -> 8
            }
            
            mobileBinding?.recyclersearch?.apply {
                layoutManager = GridLayoutManager(requireContext(), spanCount)
                adapter = mediaAdapter
            }
        }
    }
    
    private fun setupSearchInput() {
        val searchInput: EditText? = if (isTVDevice) {
            tvBinding?.searchInput
        } else {
            mobileBinding?.searchInput
        }
        searchInput?.requestFocus()
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        searchInput?.setOnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                // Hide keyboard logic if needed
                true
            } else {
                false
            }
        }
    }
    
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    mediaAdapter.submitList(state.searchResults)
                    
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
            val bundle = bundleOf("tvShowId" to media.id)
            findNavController().navigate(R.id.tvShowDetailsFragment, bundle)
        }
    }

    private fun isTVDevice(): Boolean {
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mobileBinding = null
        tvBinding = null
    }
}
