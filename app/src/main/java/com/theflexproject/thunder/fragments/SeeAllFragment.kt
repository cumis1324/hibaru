package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.theflexproject.thunder.R
import com.theflexproject.thunder.ui.home.HomeViewModel
import com.theflexproject.thunder.ui.seeall.SeeAllScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeeAllFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

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
                                val bundle = Bundle().apply {
                                    putInt("videoId", item.id)
                                    putBoolean("isMovie", true)
                                }
                                findNavController().navigate(R.id.action_seeAllFragment_to_playerFragment, bundle)
                            }
                            is com.theflexproject.thunder.model.TVShowInfo.TVShow -> {
                                val bundle = Bundle().apply {
                                    putInt("tvShowId", item.id)
                                }
                                findNavController().navigate(R.id.action_seeAllFragment_to_tvShowDetailsFragment, bundle)
                            }
                        }
                    }
                )
            }
        }
    }
}
