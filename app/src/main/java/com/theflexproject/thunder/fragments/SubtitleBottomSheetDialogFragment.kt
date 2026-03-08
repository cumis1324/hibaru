package com.theflexproject.thunder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theflexproject.thunder.R
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import org.videolan.libvlc.MediaPlayer

class SubtitleBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var vlcPlayer: MediaPlayer? = null

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
                        player = vlcPlayer,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @Composable
    fun SubtitleSelectionScreen(
        player: MediaPlayer?,
        onDismiss: () -> Unit
    ) {
        val nestedScrollConnection = rememberNestedScrollInteropConnection()
        
        val subtitleOptions = remember(player) {
            val options = mutableListOf<SubtitleOption>()
            player?.let { p ->
                val tracks = p.spuTracks
                val currentTrackId = p.spuTrack
                
                tracks?.forEach { track ->
                    // LibVLC track ID -1 is usually "None" / Disabled
                    options.add(SubtitleOption(
                        id = track.id,
                        label = track.name ?: "Unknown",
                        isActive = (track.id == currentTrackId)
                    ))
                }
            }
            options
        }

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
                                player?.spuTrack = option.id
                                onDismiss()
                            }
                        )
                    }
                    
                    if (subtitleOptions.isEmpty()) {
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

    data class SubtitleOption(
        val id: Int,
        val label: String,
        val isActive: Boolean
    )

    companion object {
        fun newInstance(
            player: MediaPlayer?
        ): SubtitleBottomSheetDialogFragment {
            val fragment = SubtitleBottomSheetDialogFragment()
            fragment.vlcPlayer = player
            return fragment
        }
    }
}
