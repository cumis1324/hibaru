package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.theflexproject.thunder.R
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.model.TVShowInfo.TVShow
import com.theflexproject.thunder.ui.search.SearchScreen
import com.theflexproject.thunder.ui.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SearchScreen(
                        viewModel = viewModel,
                        onItemClick = { media -> onMediaClicked(media) }
                    )
                }
            }
        }
    }

    private fun onMediaClicked(media: MyMedia) {
        when (media) {
            is Movie -> {
                android.util.Log.d("NavigationClick", "Search -> Player: Movie ID=${media.id}, Title=${media.title}")
                val bundle = Bundle().apply {
                    putInt("videoId", media.id)
                    putBoolean("isMovie", true)
                }
                findNavController().navigate(R.id.playerFragment, bundle)
            }
            is Episode -> {
                android.util.Log.d("NavigationClick", "Search -> Player: Episode ID=${media.id}, Title=${media.name}")
                val bundle = Bundle().apply {
                    putInt("videoId", media.id)
                    putBoolean("isMovie", false)
                    putInt("episodeId", media.id)
                }
                findNavController().navigate(R.id.playerFragment, bundle)
            }
            is TVShow -> {
                android.util.Log.d("NavigationClick", "Search -> Detail: TVShow ID=${media.id}, Title=${media.name}")
                val bundle = Bundle().apply {
                    putInt("tvShowId", media.id)
                }
                findNavController().navigate(R.id.tvShowDetailsFragment, bundle)
            }
            else -> {}
        }
    }
}
