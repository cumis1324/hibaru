package com.theflexproject.thunder.fragments.movie

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.theflexproject.thunder.R
import com.theflexproject.thunder.adapter.MediaAdapter
import com.theflexproject.thunder.database.DatabaseClient
import com.theflexproject.thunder.databinding.FragmentForYouBinding
import com.theflexproject.thunder.fragments.BaseFragment
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

@AndroidEntryPoint
class ForYouFragment : BaseFragment() {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!
    
    private var mediaAdapter: MediaAdapter? = null
    private var forYouList: MutableList<MyMedia> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLibraryMovies()
    }

    private fun showLibraryMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            val recommendations = withContext(Dispatchers.IO) {
                val db = DatabaseClient.getInstance(requireContext()).appDatabase
                val lastPlayed = db.movieDao().getrecomendation(10, 0)
                val played = db.movieDao().getMoreMovied()
                val fav = db.movieDao().recombyfav
                
                val combined = mutableListOf<MyMedia>()
                combined.addAll(played)
                combined.addAll(fav)
                combined.addAll(lastPlayed)
                combined.distinctBy { 
                    if (it is Movie) it.id else it.hashCode()
                }.shuffled()
            }

            if (recommendations.isNotEmpty()) {
                forYouList.clear()
                forYouList.addAll(recommendations)
                setupRecyclerView()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val noOfItems = (dpWidth / 120).toInt().coerceAtLeast(1)

        binding.recyclerForYou.layoutManager = GridLayoutManager(requireContext(), noOfItems)
        binding.recyclerForYou.setHasFixedSize(true)
        
        mediaAdapter = MediaAdapter(requireContext(), forYouList, parentFragmentManager)
        mediaAdapter?.setOnItemClickListener { _, position ->
            val movie = forYouList[position] as? Movie
            movie?.let {
                val bundle = Bundle().apply {
                    putInt("videoId", it.id)
                    putBoolean("isMovie", true)
                }
                findNavController().navigate(R.id.playerFragment, bundle)
            }
        }
        binding.recyclerForYou.adapter = mediaAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
