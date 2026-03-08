package com.theflexproject.thunder.player;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.utils.LanguageUtils;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener {
    public static void fastForward(Object player, ImageButton ffBtn) {
        if (player != null) {
            ffBtn.setEnabled(true);
            if (player instanceof org.videolan.libvlc.MediaPlayer) {
                org.videolan.libvlc.MediaPlayer p = (org.videolan.libvlc.MediaPlayer) player;
                long currentPosition = p.getTime();
                long duration = p.getLength();
                long forwardPosition = currentPosition + 10000;
                if (forwardPosition > duration)
                    forwardPosition = duration;
                p.setTime(forwardPosition);
            }
        } else {
            ffBtn.setEnabled(false);
        }
    }

    public static void rewind(Object player, ImageButton bwBtn) {
        if (player != null) {
            bwBtn.setEnabled(true);
            if (player instanceof org.videolan.libvlc.MediaPlayer) {
                org.videolan.libvlc.MediaPlayer p = (org.videolan.libvlc.MediaPlayer) player;
                long currentPosition = p.getTime();
                long rewindPosition = currentPosition - 10000;
                if (rewindPosition < 0)
                    rewindPosition = 0;
                p.setTime(rewindPosition);
            }
        }
    }

    public static void togglePlayback(Object player, ImageButton playPauseButton) {
        if (player != null) {
            if (player instanceof org.videolan.libvlc.MediaPlayer) {
                org.videolan.libvlc.MediaPlayer p = (org.videolan.libvlc.MediaPlayer) player;
                if (p.isPlaying()) {
                    p.pause();
                    playPauseButton.setImageResource(R.drawable.ic_play);
                } else {
                    p.play();
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                }
            }
        }
    }

    public static void showSubtitleSelectionDialog(Context mActivity, Object player) {
        // Stub for now, can be implemented with LibVLC later
        Toast.makeText(mActivity, "Subtitle selection not implemented", Toast.LENGTH_SHORT).show();
    }

    public static void showSettingsDialog(Context mActivity, Spinner spinnerAudioTrack,
            Spinner spinnerPlaybackSpeed, View dialogView,
            Object player) {
        // Stub for now
        Toast.makeText(mActivity, "Settings not implemented", Toast.LENGTH_SHORT).show();
    }
}
