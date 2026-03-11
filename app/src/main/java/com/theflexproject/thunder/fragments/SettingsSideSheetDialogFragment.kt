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
        
        val speeds = listOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "1.75x", "2.0x")
        val currentSpeedLabel = remember(player) {
            val rate = player?.rate ?: 1.0f
            when (rate) {
                0.5f -> "0.5x"
                0.75f -> "0.75x"
                1.25f -> "1.25x"
                1.5f -> "1.5x"
                1.75f -> "1.75x"
                2.0f -> "2.0x"
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
                    SettingsSection.AUDIO_CONFIG -> "Audio Configuration"
                    SettingsSection.AUDIO_SYNC -> "Audio Sync (Delay)"
                    SettingsSection.SUBTITLE_SYNC -> "Subtitle Sync (Delay)"
                    SettingsSection.SLEEP_TIMER -> "Sleep Timer"
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
                                onResizeClick = { currentSection = SettingsSection.RESIZE },
                                onAudioConfigClick = { currentSection = SettingsSection.AUDIO_CONFIG },
                                onAudioSyncClick = { currentSection = SettingsSection.AUDIO_SYNC },
                                onSubSyncClick = { currentSection = SettingsSection.SUBTITLE_SYNC },
                                onSleepClick = { currentSection = SettingsSection.SLEEP_TIMER }
                            )
                        }
                        SettingsSection.SLEEP_TIMER -> {
                            SleepTimerSection(onDismiss = { currentSection = SettingsSection.MAIN })
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
                        SettingsSection.AUDIO_CONFIG -> {
                            val prefs = remember { requireContext().getSharedPreferences("PlayerSettings", android.content.Context.MODE_PRIVATE) }
                            var passthroughEnabled by remember { mutableStateOf(prefs.getBoolean("audio_passthrough", false)) }
                            
                            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                                SettingToggleTvItem(
                                    label = "Digital Audio Passthrough",
                                    description = "Enable for Atmos/Surround support (Required compatible hardware). Turn OFF if you have NO SOUND.",
                                    isChecked = passthroughEnabled,
                                    onCheckedChange = { 
                                        passthroughEnabled = it
                                        prefs.edit().putBoolean("audio_passthrough", it).apply()
                                        (parentFragment as? PlayerFragment)?.reloadPlayback()
                                    }
                                )
                            }
                        }
                        SettingsSection.AUDIO_SYNC -> {
                            SyncControlTvItem(
                                label = "Audio Delay",
                                currentOffsetMs = player?.audioDelay ?: 0L,
                                onOffsetChange = { player?.audioDelay = it }
                            )
                        }
                        SettingsSection.SUBTITLE_SYNC -> {
                            SyncControlTvItem(
                                label = "Subtitle Delay",
                                currentOffsetMs = player?.spuDelay ?: 0L,
                                onOffsetChange = { player?.spuDelay = it }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MainSettingsMenu(
        onAudioClick: () -> Unit,
        onSpeedClick: () -> Unit,
        onResizeClick: () -> Unit,
        onAudioConfigClick: () -> Unit,
        onAudioSyncClick: () -> Unit,
        onSubSyncClick: () -> Unit,
        onSleepClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            SettingMenuTvItem(
                label = "Audio Track",
                description = "Choose between available audio languages",
                icon = Icons.Default.Settings,
                onClick = onAudioClick
            )
            SettingMenuTvItem(
                label = "Playback Speed",
                description = "Change video playback speed",
                icon = Icons.Default.PlayArrow,
                onClick = onSpeedClick
            )
            SettingMenuTvItem(
                label = "Resize Mode",
                description = "Adjust video presentation (Fit/Stretch)",
                icon = Icons.Default.Settings,
                onClick = onResizeClick
            )
            SettingMenuTvItem(
                label = "Audio Configuration",
                description = "Configure Audio Passthrough (Atmos/PCM)",
                icon = Icons.Default.Settings,
                onClick = onAudioConfigClick
            )
            SettingMenuTvItem(
                label = "Audio Sync",
                description = "Adjust audio delay (ms)",
                icon = Icons.Default.Settings,
                onClick = onAudioSyncClick
            )
            SettingMenuTvItem(
                label = "Subtitle Sync",
                description = "Adjust subtitle timing (ms)",
                icon = Icons.Default.Settings,
                onClick = onSubSyncClick
            )
            SettingMenuTvItem(
                label = "Sleep Timer",
                description = "Turn off player automatically",
                icon = Icons.Default.Settings,
                onClick = onSleepClick
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
        description: String? = null, // Added description parameter
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() } // Moved focusRequester inside

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick),
            color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
                Column(modifier = Modifier.weight(1f)) { // Use Column for label and description
                    Text(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                    )
                    description?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
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
                    fontSize = 18.sp,
                    fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.Medium,
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

    @Composable
    fun SettingToggleTvItem(
        label: String,
        description: String? = null,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        var isFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable { onCheckedChange(!isChecked) },
            color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                    )
                    description?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }

    private fun applyPlaybackSpeed(speed: String, player: MediaPlayer?) {
        if (player == null) return
        val playbackSpeed = when (speed) {
            "0.5x" -> 0.5f
            "0.75x" -> 0.75f
            "1.25x" -> 1.25f
            "1.5x" -> 1.5f
            "1.75x" -> 1.75f
            "2.0x" -> 2.0f
            else -> 1.0f
        }
        player.rate = playbackSpeed
    }

    @Composable
    fun SyncControlTvItem(
        label: String,
        currentOffsetMs: Long,
        onOffsetChange: (Long) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val labelColor = MaterialTheme.colorScheme.onSurface
            Text(
                text = label,
                color = labelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Large step buttons for TV focus
                IconButton(
                    onClick = { onOffsetChange(currentOffsetMs - 1000) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("-1s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { onOffsetChange(currentOffsetMs - 50) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("-50ms", color = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "${currentOffsetMs} ms",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                IconButton(
                    onClick = { onOffsetChange(currentOffsetMs + 50) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("+50ms", color = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onOffsetChange(currentOffsetMs + 1000) },
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("+1s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onOffsetChange(0) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("Reset to 0", color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }

    @Composable
    fun SleepTimerSection(onDismiss: () -> Unit) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val options = listOf(0, 15, 30, 60, 90, 120)
        val labels = listOf("Off", "15 Minutes", "30 Minutes", "60 Minutes", "90 Minutes", "120 Minutes")
        
        val prefs = remember { context.getSharedPreferences("PlayerSettings", android.content.Context.MODE_PRIVATE) }
        var selectedMinutes by remember { mutableStateOf(prefs.getInt("sleep_timer_minutes", 0)) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            itemsIndexed(labels) { index, label ->
                val minutes = options[index]
                SettingItemTv(
                    label = label,
                    isActive = selectedMinutes == minutes,
                    icon = Icons.Default.Settings,
                    focusRequester = remember { FocusRequester() },
                    onClick = {
                        selectedMinutes = minutes
                        prefs.edit().putInt("sleep_timer_minutes", minutes).apply()
                        setSleepTimer(context, minutes)
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }

    private fun setSleepTimer(context: android.content.Context, minutes: Int) {
        val intent = android.content.Intent("com.theflexproject.thunder.ACTION_SLEEP_TIMER")
        intent.putExtra("minutes", minutes)
        context.sendBroadcast(intent)
        
        if (minutes > 0) {
            android.widget.Toast.makeText(context, "Sleep timer set for $minutes minutes", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Sleep timer disabled", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    data class AudioOption(
        val id: Int,
        val label: String,
        val isActive: Boolean
    )

    private enum class SettingsSection {
        MAIN, AUDIO, SPEED, RESIZE, AUDIO_CONFIG, AUDIO_SYNC, SUBTITLE_SYNC, SLEEP_TIMER
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
