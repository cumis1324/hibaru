package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.theflexproject.thunder.R
import com.theflexproject.thunder.ui.home.HomeViewModel
import com.theflexproject.thunder.ui.seeall.SeeAllScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeeAllFragment : Fragment() {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sectionId = arguments?.getString("sectionId") ?: ""
        val sectionTitle = arguments?.getString("sectionTitle") ?: ""

        return ComposeView(requireContext()).apply {
            setContent {
                SeeAllScreen(
                    sectionId = sectionId,
                    sectionTitle = sectionTitle,
                    viewModel = viewModel,
                    onBackClick = { findNavController().popBackStack() },
                    onItemClick = { item ->
                        when (item) {
                            is com.theflexproject.thunder.model.Movie -> {
                                android.util.Log.d("NavigationClick", "SeeAll -> Player: Movie ID=${item.id}, Title=${item.title}")
                                val bundle = Bundle().apply {
                                    putInt("videoId", item.id)
                                    putBoolean("isMovie", true)
                                }
                                try {
                                    findNavController().navigate(R.id.action_seeAllFragment_to_playerFragment, bundle)
                                } catch (e: Exception) {
                                    android.util.Log.e("NavigationClick", "SeeAll Navigation Failed: ${e.message}")
                                }
                            }
                            is com.theflexproject.thunder.model.TVShowInfo.Episode -> {
                                android.util.Log.d("NavigationClick", "SeeAll -> Player: Episode ID=${item.id}, Title=${item.name}")
                                val bundle = Bundle().apply {
                                    putInt("videoId", item.id)
                                    putBoolean("isMovie", false)
                                    putInt("episodeId", item.id)
                                }
                                try {
                                    findNavController().navigate(R.id.action_seeAllFragment_to_playerFragment, bundle)
                                } catch (e: Exception) {
                                    android.util.Log.e("NavigationClick", "SeeAll Navigation Failed: ${e.message}")
                                }
                            }
                            is com.theflexproject.thunder.model.TVShowInfo.TVShow -> {
                                android.util.Log.d("NavigationClick", "SeeAll -> Detail: TVShow ID=${item.id}, Title=${item.name}")
                                val bundle = Bundle().apply {
                                    putInt("tvShowId", item.id)
                                }
                                try {
                                    findNavController().navigate(R.id.action_seeAllFragment_to_tvShowDetailsFragment, bundle)
                                } catch (e: Exception) {
                                    android.util.Log.e("NavigationClick", "SeeAll Navigation Failed: ${e.message}")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
