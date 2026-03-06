package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.theflexproject.thunder.R
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import com.theflexproject.thunder.utils.MovieQualityExtractor

class SourceSideSheetDialogFragment : DialogFragment() {

    private var sourceList: List<MyMedia>? = null
    private var currentUrl: String? = null
    private var onSourceSelected: ((MyMedia) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.SideSheetDialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.gravity = android.view.Gravity.END
            params.width = (resources.displayMetrics.widthPixels * 0.35).toInt()
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.dimAmount = 0f
            window.attributes = params
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        (parentFragment as? PlayerFragment)?.toggleSideSheetResize(true)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? PlayerFragment)?.toggleSideSheetResize(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NfgPlusTheme {
                    SourceSideSelectionScreen(
                        sources = sourceList ?: emptyList(),
                        currentUrl = currentUrl ?: "",
                        onSelected = {
                            onSourceSelected?.invoke(it)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun SourceSideSelectionScreen(
        sources: List<MyMedia>,
        currentUrl: String,
        onSelected: (MyMedia) -> Unit
    ) {
        val focusRequesters = remember { List(sources.size) { FocusRequester() } }
        var activeIndex = -1

        Surface(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface, // Dynamic Background
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "Sources",
                    color = MaterialTheme.colorScheme.onSurface, // Dynamic Text
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sources) { index, source ->
                        val url = when (source) {
                            is Movie -> source.url_string
                            is Episode -> source.url_string
                            else -> ""
                        }
                        val fileName = when (source) {
                            is Movie -> source.file_name
                            is Episode -> source.file_name
                            else -> "Unknown"
                        }
                        val quality = MovieQualityExtractor.extractQualtiy(fileName)
                        val isActive = url == currentUrl
                        if (isActive) activeIndex = index

                        SourceItemTv(
                            quality = quality,
                            isActive = isActive,
                            focusRequester = focusRequesters[index],
                            onClick = { onSelected(source) }
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            val focusTarget = if (activeIndex != -1) activeIndex else 0
            if (focusRequesters.isNotEmpty()) {
                focusRequesters[focusTarget].requestFocus()
            }
        }
    }

    @Composable
    fun SourceItemTv(
        quality: String,
        isActive: Boolean,
        focusRequester: FocusRequester,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = quality,
                    fontSize = 18.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(
            sources: List<MyMedia>,
            currentUrl: String?,
            onSourceSelected: (MyMedia) -> Unit
        ): SourceSideSheetDialogFragment {
            return SourceSideSheetDialogFragment().apply {
                this.sourceList = sources
                this.currentUrl = currentUrl
                this.onSourceSelected = onSourceSelected
            }
        }
    }
}
