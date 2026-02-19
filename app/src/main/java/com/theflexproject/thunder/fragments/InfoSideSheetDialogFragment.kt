package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.theflexproject.thunder.ui.detail.DetailScreen
import com.theflexproject.thunder.ui.detail.DetailViewModel
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InfoSideSheetDialogFragment : DialogFragment() {

    private val viewModel: DetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set style to ensure it looks like a side sheet or at least stays on the right
        setStyle(STYLE_NO_FRAME, com.theflexproject.thunder.R.style.SideSheetDialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.gravity = Gravity.END
            params.width = (resources.displayMetrics.widthPixels * 0.4).toInt() // 40% width
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.transparent)
            // Add entry animation from right
            window.setWindowAnimations(android.R.style.Animation_InputMethod) 
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val movieId = arguments?.getInt("movie_id", -1) ?: -1
        val tvShowId = arguments?.getInt("tv_show_id", -1) ?: -1

        return ComposeView(requireContext()).apply {
            setContent {
                NfgPlusTheme {
                    DetailScreen(
                        movieId = if (movieId != -1 && movieId != 0) movieId else null,
                        tvShowId = if (tvShowId != -1 && tvShowId != 0) tvShowId else null,
                        viewModel = viewModel,
                        onBackClick = {
                            dismiss()
                        },
                        onNavigate = { videoId, isMovie, tvShow, season ->
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(movieId: Int): InfoSideSheetDialogFragment {
            return InfoSideSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt("movie_id", movieId)
                }
            }
        }

        fun newTvInstance(tvShowId: Int): InfoSideSheetDialogFragment {
            return InfoSideSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt("tv_show_id", tvShowId)
                }
            }
        }
    }
}
