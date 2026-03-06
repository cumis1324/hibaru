package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import com.theflexproject.thunder.utils.LanguageUtils

@UnstableApi
class SubtitleSideSheetDialogFragment : DialogFragment() {

    private var mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? = null
    private var trackSelector: DefaultTrackSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, 0)
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
            window.setWindowAnimations(android.R.style.Animation_InputMethod)
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
                    SubtitleSideSelectionScreen(
                        mappedTrackInfo = mappedTrackInfo,
                        trackSelector = trackSelector,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @Composable
    fun SubtitleSideSelectionScreen(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo?,
        trackSelector: DefaultTrackSelector?,
        onDismiss: () -> Unit
    ) {
        val subtitleOptions = remember(mappedTrackInfo, trackSelector) {
            val options = mutableListOf<SubtitleOption>()
            mappedTrackInfo?.let { info ->
                for (rendererIndex in 0 until info.rendererCount) {
                    if (info.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                        val trackGroups = info.getTrackGroups(rendererIndex)
                        for (groupIndex in 0 until trackGroups.length) {
                            val trackGroup = trackGroups.get(groupIndex)
                            for (trackIndex in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(trackIndex)
                                val language = format.language
                                val label = language?.let { LanguageUtils.getLanguageName(it) } ?: "Unknown"
                                
                                options.add(SubtitleOption(
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
            options.add(SubtitleOption(
                label = "Disable Subtitles",
                override = null,
                isActive = isSubtitlesDisabled(trackSelector)
            ))
            options
        }

        val focusRequesters = remember { List(subtitleOptions.size) { FocusRequester() } }
        val lazyListState = rememberLazyListState()

        LaunchedEffect(Unit) {
            val activeIndex = subtitleOptions.indexOfFirst { it.isActive }.coerceAtLeast(0)
            if (activeIndex < focusRequesters.size) {
                focusRequesters[activeIndex].requestFocus()
                lazyListState.scrollToItem(activeIndex)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Select Subtitle",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(subtitleOptions) { index, option ->
                        SubtitleItemTv(
                            label = option.label,
                            isActive = option.isActive,
                            focusRequester = focusRequesters[index],
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
    fun SubtitleItemTv(
        label: String,
        isActive: Boolean,
        focusRequester: FocusRequester,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick),
            color = if (isFocused) MaterialTheme.colorScheme.primaryContainer 
                    else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Transparent,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer 
                           else if (isActive) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = label,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer 
                            else if (isActive) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer 
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
        if (parameters.getRendererDisabled(C.TRACK_TYPE_VIDEO)) return false
        
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
        ): SubtitleSideSheetDialogFragment {
            return SubtitleSideSheetDialogFragment().apply {
                this.mappedTrackInfo = mappedTrackInfo
                this.trackSelector = trackSelector
            }
        }
    }
}
