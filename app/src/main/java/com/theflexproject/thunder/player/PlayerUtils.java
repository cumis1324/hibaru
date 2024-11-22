package com.theflexproject.thunder.player;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayerUtils {
    static FirebaseManager manager;
    static DatabaseReference databaseReference;
    static String userId;
    static final String HISTORY_PATH = "History/", LAST_POSITION = "lastPosition", OFFLINE = "offline";
    public static ValueEventListener lastPositionListener;


    public static void resumePlayerState(Player player, String tmdbId) {
        manager = new FirebaseManager();
        userId = manager.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference(HISTORY_PATH);
        if (tmdbId != null && !OFFLINE.equals(tmdbId)) {
            DatabaseReference lastPositionRef = databaseReference.child(userId).child(tmdbId).child(LAST_POSITION);
            lastPositionListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Long lastPosition = snapshot.getValue(Long.class);
                        if (lastPosition != null && player != null) {
                            player.seekTo(lastPosition);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            lastPositionRef.addListenerForSingleValueEvent(lastPositionListener);
        }
    }

    @SuppressLint("SetTextI18n")
    public static void updateTimer(TextView timer, long currentPosition, long duration) {
        String current = formatTime(currentPosition);
        String total = formatTime(duration);
        timer.setText(current + " / " + total);
    }
    @SuppressLint("DefaultLocale")
    static String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static MediaSource.Factory createMediaSourceFactory(Context mActivity) {
        DataSource.Factory dataSourceFactory = DemoUtil.getDataSourceFactory(mActivity);
        DefaultDrmSessionManagerProvider drmProvider = new DefaultDrmSessionManagerProvider();
        drmProvider.setDrmHttpDataSourceFactory(DemoUtil.getHttpDataSourceFactory(mActivity));
        return new DefaultMediaSourceFactory(mActivity)
                .setDataSourceFactory(dataSourceFactory)
                .setDrmSessionManagerProvider(drmProvider);
    }
}
