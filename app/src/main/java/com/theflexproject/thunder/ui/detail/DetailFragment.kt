package com.theflexproject.thunder.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val movieId = arguments?.getInt("movie_id", -1) ?: -1
        // Support both "tvShowId" (from nav_graph) and "tv_show_id" (from newTvInstance)
        val tvShowIdFromNav = arguments?.getInt("tvShowId", -1) ?: -1
        val tvShowIdFromBundle = arguments?.getInt("tv_show_id", -1) ?: -1
        val tvShowId = if (tvShowIdFromNav != -1) tvShowIdFromNav else tvShowIdFromBundle
        val tvShow = arguments?.getParcelable<com.theflexproject.thunder.model.TVShowInfo.TVShow>("tv_show")
        val season = arguments?.getParcelable<com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails>("season")
        val episode = arguments?.getParcelable<com.theflexproject.thunder.model.TVShowInfo.Episode>("episode")

        android.util.Log.d("DetailFragment", "Arguments: movieId=$movieId, tvShowId=$tvShowId, tvShow=$tvShow")

        return ComposeView(requireContext()).apply {
            setContent {
                NfgPlusTheme {
                    DetailScreen(
                        movieId = if (movieId != -1 && movieId != 0) movieId else null,
                        tvShowId = if (tvShowId != -1 && tvShowId != 0) tvShowId else null,
                        tvShow = tvShow,
                        season = season,
                        episode = episode,
                        viewModel = viewModel,
                        onBackClick = {
                            androidx.navigation.fragment.NavHostFragment.findNavController(this@DetailFragment).popBackStack()
                        },
                        onNavigate = { videoId, isMovie, tvShow, season ->
                            val bundle = android.os.Bundle().apply {
                                putInt("videoId", videoId)
                                putBoolean("isMovie", isMovie)
                                if (!isMovie) {
                                    putInt("episodeId", videoId)
                                    putParcelable("tvShow", tvShow)
                                    putParcelable("season", season)
                                }
                            }
                            androidx.navigation.fragment.NavHostFragment.findNavController(this@DetailFragment).navigate(
                                com.theflexproject.thunder.R.id.action_tvShowDetailsFragment_to_playerFragment,
                                bundle
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(movieId: Int): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("movie_id", movieId)
                }
            }
        }

        fun newTvInstance(tvShowId: Int): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("tv_show_id", tvShowId)
                }
            }
        }

        fun newInstance(
            tvShow: com.theflexproject.thunder.model.TVShowInfo.TVShow,
            season: com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails,
            episode: com.theflexproject.thunder.model.TVShowInfo.`Episode`
        ): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("tv_show", tvShow)
                    putParcelable("season", season)
                    putParcelable("episode", episode)
                }
            }
        }
    }
}
