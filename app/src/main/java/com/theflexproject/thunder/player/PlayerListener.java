package com.theflexproject.thunder.player;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener {
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
    public static void showSubtitleSelectionDialog(Context mActivity, MappingTrackSelector.MappedTrackInfo mappedTrackInfo, DefaultTrackSelector trackSelector) {

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
}
