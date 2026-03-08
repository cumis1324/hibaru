package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theflexproject.thunder.model.Movie
import com.theflexproject.thunder.model.MyMedia
import com.theflexproject.thunder.model.TVShowInfo.Episode
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import com.theflexproject.thunder.utils.MovieQualityExtractor

class SourceBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var sourceList: List<MyMedia>? = null
    var currentUrl: String? = null
    var onSourceSelected: ((MyMedia) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.theflexproject.thunder.R.style.BottomSheetDialogTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                val behavior = BottomSheetBehavior.from(sheet)
                // Langsung TERBUKA PENUH
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true 
                
                sheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NfgPlusTheme {
                    SourceSelectionScreen(
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
    fun SourceSelectionScreen(
        sources: List<MyMedia>,
        currentUrl: String,
        onSelected: (MyMedia) -> Unit
    ) {
        // Penting: Interop connection membuat scroll list dan geser window jadi satu kesatuan
        val nestedScrollConnection = rememberNestedScrollInteropConnection()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .nestedScroll(nestedScrollConnection), // Bridges Compose scroll to BottomSheet gestures
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                // handle bar dekoratif
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            MaterialTheme.shapes.extraSmall
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select Source / Quality",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    sources.forEach { source ->
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

                        SourceItem(
                            quality = quality,
                            isActive = isActive,
                            onClick = { onSelected(source) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SourceItem(
        quality: String,
        isActive: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
        val textColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        val iconColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .background(backgroundColor, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = quality,
                color = textColor,
                fontSize = 17.sp,
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

    companion object {
        fun newInstance(
            sources: List<MyMedia>,
            currentUrl: String?,
            onSourceSelected: (MyMedia) -> Unit
        ): SourceBottomSheetDialogFragment {
            val fragment = SourceBottomSheetDialogFragment()
            fragment.sourceList = sources
            fragment.currentUrl = currentUrl
            fragment.onSourceSelected = onSourceSelected
            return fragment
        }
    }
}
