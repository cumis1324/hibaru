package com.theflexproject.thunder.player;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class PlayerListener {
    public static void fastForward(Player player, ImageButton ffBtn) {
        if (player!=null) {
            ffBtn.setEnabled(true);
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            long forwardPosition = currentPosition + 10000; // Tambahkan 10 detik
            if (forwardPosition > duration) forwardPosition = duration; // Hindari melebihi durasi
            player.seekTo(forwardPosition);
        }else {
            ffBtn.setEnabled(false);
        }
    }
    public static void rewind (Player player, ImageButton bwBtn) {
        if (player!=null){
            bwBtn.setEnabled(true);
            long currentPosition = player.getCurrentPosition();
            long rewindPosition = currentPosition - 10000; // Kurangi 10 detik
            if (rewindPosition < 0) rewindPosition = 0; // Hindari nilai negatif
            player.seekTo(rewindPosition);
        }
    }

    public static void togglePlayback(Player player, ImageButton playPauseButton) {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                playPauseButton.setImageResource(R.drawable.ic_pause);
            }
        }
    }
    @OptIn(markerClass = UnstableApi.class)
    public static void showSubtitleSelectionDialog(Context mActivity,
                                                   MappingTrackSelector.MappedTrackInfo mappedTrackInfo,
                                                   DefaultTrackSelector trackSelector) {

        if (mappedTrackInfo == null) {
            Toast.makeText(mActivity, "No subtitles available", Toast.LENGTH_SHORT).show();
            return;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Select Subtitle");
        List<String> subtitleOptions = new ArrayList<>();
        List<DefaultTrackSelector.SelectionOverride> overrides = new ArrayList<>();

        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);

                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup trackGroup = trackGroups.get(groupIndex);

                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        Format format = trackGroup.getFormat(trackIndex);
                        String language = format.language; // Contoh mendapatkan kode bahasa
                        subtitleOptions.add(language != null ? LanguageUtils.getLanguageName(language) : "Unknown");
                        overrides.add(new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                    }
                }
            }
        }

        // Tambahkan opsi untuk mematikan subtitle
        subtitleOptions.add("Disable Subtitles");
        overrides.add(null);

        builder.setItems(subtitleOptions.toArray(new String[0]), (dialog, which) -> {
            if (overrides.get(which) != null) {
                DefaultTrackSelector.Parameters.Builder builders = trackSelector.buildUponParameters();
                builders.setRendererDisabled(C.TRACK_TYPE_VIDEO, false); // Aktifkan renderer subtitle
                builders.setSelectionOverride(C.TRACK_TYPE_VIDEO, mappedTrackInfo.getTrackGroups(C.TRACK_TYPE_VIDEO), overrides.get(which));
                trackSelector.setParameters(builders);
            } else {
                DefaultTrackSelector.Parameters.Builder builders = trackSelector.buildUponParameters();
                builders.setRendererDisabled(C.TRACK_TYPE_VIDEO, true); // Nonaktifkan renderer subtitle
                trackSelector.setParameters(builders);
            }
        });

        builder.create().show();
    }

    public static void showSettingsDialog(Context mActivity, Spinner spinnerAudioTrack,
                                          Spinner spinnerPlaybackSpeed, View dialogView,
                                          DefaultTrackSelector trackSelector,
                                          MappingTrackSelector.MappedTrackInfo mappedTrackInfo,
                                          Player player) {

        // Populate Audio Track Spinner dynamically
        List<String> audioTracks = getAudioTracks(mappedTrackInfo, trackSelector);
        ArrayAdapter<String> audioAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, audioTracks);
        audioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAudioTrack.setAdapter(audioAdapter);

        // Populate Playback Speed Spinner
        List<String> playbackSpeeds = new ArrayList<>();
        playbackSpeeds.add("0.5x");
        playbackSpeeds.add("1.0x (Normal)");
        playbackSpeeds.add("1.5x");
        playbackSpeeds.add("2.0x");

        ArrayAdapter<String> speedAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, playbackSpeeds);
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlaybackSpeed.setAdapter(speedAdapter);
        spinnerPlaybackSpeed.setSelection(1);

        // Remove dialogView parent if exists
        if (dialogView.getParent() != null) {
            ((ViewGroup) dialogView.getParent()).removeView(dialogView);
        }

        // Create and show the AlertDialog
        new AlertDialog.Builder(mActivity)
                .setTitle("Settings")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Get selected values from spinners
                    String selectedAudioTrack = spinnerAudioTrack.getSelectedItem().toString();
                    String selectedPlaybackSpeed = spinnerPlaybackSpeed.getSelectedItem().toString();

                    // Apply selected audio track
                    applyAudioTrack(selectedAudioTrack, mappedTrackInfo, trackSelector);

                    // Apply playback speed
                    applyPlaybackSpeed(selectedPlaybackSpeed, player);

                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }


    public static List<String> getAudioTracks(MappingTrackSelector.MappedTrackInfo trackInfo, DefaultTrackSelector trackSelector) {
        List<String> audioTrackList = new ArrayList<>();

        if (trackInfo != null) {
            for (int rendererIndex = 0; rendererIndex < trackInfo.getRendererCount(); rendererIndex++) {
                if (trackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                    TrackGroupArray trackGroups = trackInfo.getTrackGroups(rendererIndex);

                    for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                        TrackGroup trackGroup = trackGroups.get(groupIndex);

                        for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                            Format format = trackGroup.getFormat(trackIndex);
                            String language = format.language != null ? format.language : "Unknown Language";
                            String label = format.label != null ? format.label : "Audio Track " + (groupIndex + 1);
                            audioTrackList.add(label + " (" + language + ")");
                        }
                    }
                }
            }
        }

        // Tambahkan fallback jika tidak ada audio track ditemukan
        if (audioTrackList.isEmpty()) {
            audioTrackList.add("Default Audio");
        }

        return audioTrackList;
    }

    public static void applyAudioTrack(String selectedTrack, MappingTrackSelector.MappedTrackInfo trackInfo, DefaultTrackSelector trackSelector) {
        if (trackInfo == null || selectedTrack == null || selectedTrack.isEmpty()) {
            return;
        }

        for (int rendererIndex = 0; rendererIndex < trackInfo.getRendererCount(); rendererIndex++) {
            if (trackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                TrackGroupArray trackGroups = trackInfo.getTrackGroups(rendererIndex);
                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup trackGroup = trackGroups.get(groupIndex);

                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        String trackLabel = "Audio Track " + (groupIndex + 1);
                        if (selectedTrack.contains(trackLabel)) {
                            DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
                            DefaultTrackSelector.SelectionOverride override =
                                    new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex);
                            builder.setSelectionOverride(rendererIndex, trackGroups, override);
                            trackSelector.setParameters(builder);
                            return;
                        }
                    }
                }
            }
        }

        // Jika tidak ditemukan, nonaktifkan audio track
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters();
        builder.setRendererDisabled(C.TRACK_TYPE_AUDIO, true);
        trackSelector.setParameters(builder);
    }
    private static void applyPlaybackSpeed(String speed, Player player) {
        float playbackSpeed;

        switch (speed) {
            case "0.5x":
                playbackSpeed = 0.5f;
                break;
            case "1.0x (Normal)":
                playbackSpeed = 1.0f;
                break;
            case "1.5x":
                playbackSpeed = 1.5f;
                break;
            case "2.0x":
                playbackSpeed = 2.0f;
                break;
            default:
                playbackSpeed = 1.0f; // Default to normal speed
        }

        player.setPlaybackParameters(player.getPlaybackParameters().withSpeed(playbackSpeed));
    }

}
