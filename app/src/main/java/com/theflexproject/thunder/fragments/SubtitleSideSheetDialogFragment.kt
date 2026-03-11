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
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import org.videolan.libvlc.MediaPlayer

class SubtitleSideSheetDialogFragment : DialogFragment() {

    var vlcPlayer: MediaPlayer? = null

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
                        player = vlcPlayer,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @Composable
    fun SubtitleSideSelectionScreen(
        player: MediaPlayer?,
        onDismiss: () -> Unit
    ) {
        var subtitleOptions by remember { mutableStateOf(emptyList<SubtitleOption>()) }
        val lazyListState = rememberLazyListState()

        // Polling for tracks because LibVLC finds them asynchronously
        LaunchedEffect(player) {
            if (player == null) return@LaunchedEffect
            
            // Poll every 1s for 5 seconds to catch asynchronously discovered tracks
            for (i in 1..5) {
                val currentTrackId = try { player.spuTrack } catch (e: Exception) { -1 }
                val tracks = try { player.spuTracks } catch (e: Exception) { null }
                
                val newOptions = mutableListOf<SubtitleOption>()
                tracks?.filterNotNull()?.forEach { track ->
                    newOptions.add(SubtitleOption(
                        id = track.id,
                        label = track.name ?: "Unknown",
                        isActive = (track.id == currentTrackId)
                    ))
                }
                
                // Only update if the list changed (to avoid unnecessary recompositions)
                if (newOptions != subtitleOptions) {
                    subtitleOptions = newOptions
                }
                
                kotlinx.coroutines.delay(1000)
            }
        }

        val focusRequesters = remember(subtitleOptions.size) { List(subtitleOptions.size) { FocusRequester() } }

        LaunchedEffect(subtitleOptions) {
            if (subtitleOptions.isNotEmpty()) {
                val activeIndex = subtitleOptions.indexOfFirst { it.isActive }.coerceAtLeast(0)
                if (activeIndex >= 0 && activeIndex < focusRequesters.size) {
                    try {
                        focusRequesters[activeIndex].requestFocus()
                        lazyListState.scrollToItem(activeIndex)
                    } catch (e: Exception) {
                        android.util.Log.w("SubtitleSideSheet", "Focus request failed", e)
                    }
                }
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
                                player?.spuTrack = option.id
                                onDismiss()
                            }
                        )
                    }
                    
                    if (subtitleOptions.isEmpty()) {
                        item {
                            Text(
                                text = "No subtitles available",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(24.dp)
                            )
                        }
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
            color = when {
                isFocused -> MaterialTheme.colorScheme.primaryContainer 
                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else -> Color.Transparent
            },
            contentColor = when {
                isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
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
                    color = when {
                        isFocused -> MaterialTheme.colorScheme.onPrimaryContainer
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 18.sp,
                    fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.Medium,
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

    data class SubtitleOption(
        val id: Int,
        val label: String,
        val isActive: Boolean
    )

    companion object {
        fun newInstance(
            player: MediaPlayer?
        ): SubtitleSideSheetDialogFragment {
            val fragment = SubtitleSideSheetDialogFragment()
            fragment.vlcPlayer = player
            return fragment
        }
    }
}
