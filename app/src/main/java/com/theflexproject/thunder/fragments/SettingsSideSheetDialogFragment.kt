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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.theflexproject.thunder.ui.theme.NfgPlusTheme
import org.videolan.libvlc.MediaPlayer

class SettingsSideSheetDialogFragment : DialogFragment() {

    private var vlcPlayer: MediaPlayer? = null

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
                    SettingsSideSelectionScreen(
                        player = vlcPlayer,
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsSideSelectionScreen(
        player: MediaPlayer?,
        onDismiss: () -> Unit
    ) {
        var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }
        
        val audioTracks = remember(player) {
            val options = mutableListOf<AudioOption>()
            player?.let { p ->
                val tracks = p.audioTracks
                val currentTrackId = p.audioTrack
                tracks?.forEach { track ->
                    options.add(AudioOption(
                        id = track.id,
                        label = track.name ?: "Unknown",
                        isActive = (track.id == currentTrackId)
                    ))
                }
            }
            options
        }
        
        val speeds = listOf("0.5x", "1.0x (Normal)", "1.5x", "2.0x")
        val currentSpeedLabel = remember(player) {
            val rate = player?.rate ?: 1.0f
            when {
                rate == 0.5f -> "0.5x"
                rate == 1.5f -> "1.5x"
                rate == 2.0f -> "2.0x"
                else -> "1.0x (Normal)"
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
                val title = when(currentSection) {
                    SettingsSection.MAIN -> "Settings"
                    SettingsSection.AUDIO -> "Audio Track"
                    SettingsSection.SPEED -> "Playback Speed"
                    SettingsSection.RESIZE -> "Resize Mode"
                }

                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentSection) {
                        SettingsSection.MAIN -> {
                            MainSettingsMenu(
                                onAudioClick = { currentSection = SettingsSection.AUDIO },
                                onSpeedClick = { currentSection = SettingsSection.SPEED },
                                onResizeClick = { currentSection = SettingsSection.RESIZE }
                            )
                        }
                        SettingsSection.AUDIO -> {
                            SubSettingsList(
                                items = audioTracks.map { it.label },
                                activeItem = audioTracks.find { it.isActive }?.label,
                                icon = Icons.Default.Settings,
                                onItemSelected = { label ->
                                    val id = audioTracks.find { it.label == label }?.id ?: -1
                                    player?.audioTrack = id
                                    onDismiss()
                                }
                            )
                        }
                        SettingsSection.SPEED -> {
                            SubSettingsList(
                                items = speeds,
                                activeItem = currentSpeedLabel,
                                icon = Icons.Default.PlayArrow,
                                onItemSelected = {
                                    applyPlaybackSpeed(it, player)
                                    onDismiss()
                                }
                            )
                        }
                        SettingsSection.RESIZE -> {
                            val playerFragment = parentFragment as? PlayerFragment
                            val currentResizeMode = playerFragment?.getCurrentResizeMode() ?: 0
                            val resizeModes = listOf(
                                "Original (Fit)" to 0,
                                "Stretch" to 1
                            )
                            SubSettingsList(
                                items = resizeModes.map { it.first },
                                activeItem = when(currentResizeMode) {
                                    1 -> "Stretch"
                                    else -> "Original (Fit)"
                                },
                                icon = Icons.Default.Settings,
                                onItemSelected = { label ->
                                    val mode = resizeModes.find { it.first == label }?.second ?: 0
                                    playerFragment?.setResizeMode(mode)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MainSettingsMenu(onAudioClick: () -> Unit, onSpeedClick: () -> Unit, onResizeClick: () -> Unit) {
        val focusRequesters = remember { List(3) { FocusRequester() } }
        
        LaunchedEffect(Unit) {
            focusRequesters[0].requestFocus()
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SettingMenuTvItem(
                label = "Audio Track",
                icon = Icons.Default.Settings,
                focusRequester = focusRequesters[0],
                onClick = onAudioClick
            )
            SettingMenuTvItem(
                label = "Playback Speed",
                icon = Icons.Default.PlayArrow,
                focusRequester = focusRequesters[1],
                onClick = onSpeedClick
            )
            SettingMenuTvItem(
                label = "Resize Mode",
                icon = Icons.Default.Settings,
                focusRequester = focusRequesters[2],
                onClick = onResizeClick
            )
        }
    }

    @Composable
    fun SubSettingsList(
        items: List<String>,
        activeItem: String?,
        icon: ImageVector,
        onItemSelected: (String) -> Unit
    ) {
        val focusRequesters = remember(items) { List(items.size) { FocusRequester() } }
        val lazyListState = rememberLazyListState()

        LaunchedEffect(items) {
            val activeIndex = if (activeItem != null) items.indexOf(activeItem).coerceAtLeast(0) else 0
            if (focusRequesters.isNotEmpty()) {
                focusRequesters[activeIndex].requestFocus()
                lazyListState.scrollToItem(activeIndex)
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            itemsIndexed(items) { index, item ->
                SettingItemTv(
                    label = item,
                    isActive = item == activeItem,
                    icon = icon,
                    focusRequester = focusRequesters[index],
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }

    @Composable
    fun SettingMenuTvItem(
        label: String,
        icon: ImageVector,
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
            color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    @Composable
    fun SettingItemTv(
        label: String,
        isActive: Boolean,
        icon: ImageVector,
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
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
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
                        tint = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    private fun applyPlaybackSpeed(speed: String, player: MediaPlayer?) {
        if (player == null) return
        val playbackSpeed = when (speed) {
            "0.5x" -> 0.5f
            "1.5x" -> 1.5f
            "2.0x" -> 2.0f
            else -> 1.0f
        }
        player.rate = playbackSpeed
    }

    data class AudioOption(
        val id: Int,
        val label: String,
        val isActive: Boolean
    )

    enum class SettingsSection {
        MAIN, AUDIO, SPEED, RESIZE
    }

    companion object {
        fun newInstance(
            player: MediaPlayer?
        ): SettingsSideSheetDialogFragment {
            return SettingsSideSheetDialogFragment().apply {
                this.vlcPlayer = player
            }
        }
    }
}
