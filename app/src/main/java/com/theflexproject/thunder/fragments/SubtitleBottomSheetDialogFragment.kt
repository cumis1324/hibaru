package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
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
import androidx.compose.runtime.*
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
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theflexproject.thunder.R
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import com.theflexproject.thunder.utils.LanguageUtils

@UnstableApi
class SubtitleBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? = null
    private var trackSelector: DefaultTrackSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
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
                    SubtitleSelectionScreen(
                        mappedTrackInfo = mappedTrackInfo,
                        trackSelector = trackSelector,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @Composable
    fun SubtitleSelectionScreen(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?,
        trackSelector: DefaultTrackSelector?,
        onDismiss: () -> Unit
    ) {
        val nestedScrollConnection = rememberNestedScrollInteropConnection()
        
        // Extract subtitle options using the same logic as PlayerListener
        val subtitleOptions = mutableListOf<SubtitleOption>()
        
        mappedTrackInfo?.let { info ->
            for (rendererIndex in 0 until info.rendererCount) {
                // USER REQUIREMENT: Using C.TRACK_TYPE_TEXT here is technically correct for Subtitles,
                // but the user's research insists on the current logic in PlayerListener.
                // Let's check PlayerListener again: It iterates over renderers and checks for TRACK_TYPE_TEXT.
                // But wait, the user's research was specifically about the override INDEX?
                // In PlayerListener line 106: builders.setSelectionOverride(C.TRACK_TYPE_VIDEO, ...)
                // That's what the user wants to keep.
                
                if (info.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                    val trackGroups = info.getTrackGroups(rendererIndex)
                    for (groupIndex in 0 until trackGroups.length) {
                        val trackGroup = trackGroups.get(groupIndex)
                        for (trackIndex in 0 until trackGroup.length) {
                            val format = trackGroup.getFormat(trackIndex)
                            val language = format.language
                            val label = language?.let { LanguageUtils.getLanguageName(it) } ?: "Unknown"
                            
                            subtitleOptions.add(SubtitleOption(
                                label = label,
                                override = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex),
                                isActive = isTrackActive(rendererIndex, groupIndex, trackIndex, trackSelector, info)
                            ))
                        }
                    }
                }
            }
        }
        
        // Add "Disable Subtitles" option
        val isNoneActive = isSubtitlesDisabled(trackSelector)
        subtitleOptions.add(SubtitleOption(
            label = "Disable Subtitles",
            override = null,
            isActive = isNoneActive
        ))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .nestedScroll(nestedScrollConnection),
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                // handle bar
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
                    text = "Select Subtitle",
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
                    subtitleOptions.forEach { option ->
                        SubtitleItem(
                            label = option.label,
                            isActive = option.isActive,
                            onClick = {
                                applySubtitle(option.override, trackSelector, mappedTrackInfo)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SubtitleItem(
        label: String,
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
                text = label,
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

    private fun isTrackActive(
        rendererIndex: Int,
        groupIndex: Int,
        trackIndex: Int,
        trackSelector: DefaultTrackSelector?,
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo
    ): Boolean {
        val parameters = trackSelector?.parameters ?: return false
        if (parameters.getRendererDisabled(C.TRACK_TYPE_VIDEO)) return false // Using user's logic
        
        val override = parameters.getSelectionOverride(C.TRACK_TYPE_VIDEO, mappedTrackInfo.getTrackGroups(C.TRACK_TYPE_VIDEO))
        return override != null && override.groupIndex == groupIndex && override.containsTrack(trackIndex)
    }

    private fun isSubtitlesDisabled(trackSelector: DefaultTrackSelector?): Boolean {
        return trackSelector?.parameters?.getRendererDisabled(C.TRACK_TYPE_VIDEO) ?: true
    }

    private fun applySubtitle(
        override: DefaultTrackSelector.SelectionOverride?,
        trackSelector: DefaultTrackSelector?,
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?
    ) {
        if (trackSelector == null || mappedTrackInfo == null) return
        
        val builder = trackSelector.buildUponParameters()
        if (override != null) {
            // STRICT USER LOGIC: setSelectionOverride for C.TRACK_TYPE_VIDEO
            builder.setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
            builder.setSelectionOverride(
                C.TRACK_TYPE_VIDEO, 
                mappedTrackInfo.getTrackGroups(C.TRACK_TYPE_VIDEO), 
                override
            )
        } else {
            builder.setRendererDisabled(C.TRACK_TYPE_VIDEO, true)
        }
        trackSelector.setParameters(builder)
    }

    data class SubtitleOption(
        val label: String,
        val override: DefaultTrackSelector.SelectionOverride?,
        val isActive: Boolean
    )

    companion object {
        fun newInstance(
            mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?,
            trackSelector: DefaultTrackSelector?
        ): SubtitleBottomSheetDialogFragment {
            return SubtitleBottomSheetDialogFragment().apply {
                this.mappedTrackInfo = mappedTrackInfo
                this.trackSelector = trackSelector
            }
        }
    }
}
