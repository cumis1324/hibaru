package com.theflexproject.thunder.fragments;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.DownloadAdapter;
import com.theflexproject.thunder.adapter.DownloadRecyAdapter;
import com.theflexproject.thunder.model.DownloadItem;
import com.theflexproject.thunder.model.MediaItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends BaseFragment implements DownloadRecyAdapter.OnItemDeleteListener {
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final String TAG = "DownloadFragment";
    private static final int REQUEST_MEDIA_PERMISSION = 100;
    private RecyclerView recyclerView, downloadingRecy;
    private DownloadRecyAdapter mediaAdapter;
    private List<MediaItem> mediaList = new ArrayList<>();
    private TextView teksDownload;
    private DownloadManager downloadManager;
    private DownloadAdapter downloadAdapter;
    private List<DownloadItem> downloadItems;
    private Handler handler;
    private boolean isTVDevice = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        isTVDevice = isTVDevice();
        if (isTVDevice) {
            return inflater.inflate(R.layout.fragment_download_tv, container, false);
        }
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        downloadItems = new ArrayList<>();
        downloadManager = (DownloadManager) requireActivity().getSystemService(DOWNLOAD_SERVICE);

        // Downloading recycler (Active downloads)
        // Note: For TV, we might want to hide active downloads or show them differently
        // if they don't fit well.
        // For now, only find it if it exists in the layout.
        downloadingRecy = view.findViewById(R.id.recyclerDownloading);
        if (downloadingRecy != null) {
            downloadingRecy.setLayoutManager(new LinearLayoutManager(getContext()));
            initializeDownloadItems();
            downloadAdapter = new DownloadAdapter(downloadItems, getContext());
            downloadingRecy.setAdapter(downloadAdapter);
            checkDownloadProgress();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestMediaPermissions();
        }

        recyclerView = view.findViewById(R.id.recyclerDownload);
        // For TV (VerticalGridView) we don't strictly set LayoutManager vertically as
        // it handles it,
        // but for safety if it's RecyclerView:
        if (recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        teksDownload = view.findViewById(R.id.teksDownload);

        if (mediaList.isEmpty()) {
            checkPermisi();
        } else {
            mediaAdapter = new DownloadRecyAdapter(mediaList, getContext(), this, isTVDevice);
            recyclerView.setAdapter(mediaAdapter);
        }
    }

    private boolean isTVDevice() {
        UiModeManager uiModeManager = (UiModeManager) requireContext().getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void initializeDownloadItems() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(
                DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PAUSED | DownloadManager.STATUS_PENDING);

        Cursor cursor = downloadManager.query(query);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int downloadIdIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
                    int filenameIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME);
                    int titleIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
                    int statusIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);

                    long downloadId = cursor.getLong(downloadIdIndex);
                    String title = cursor.getString(titleIndex);
                    String filename = cursor.getString(filenameIndex);
                    int status = cursor.getInt(statusIndex);

                    downloadItems.add(new DownloadItem(filename, downloadId, title, status, 0));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing download items: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
    }

    private void checkDownloadProgress() {
        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByStatus(
                        DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_PAUSED | DownloadManager.STATUS_PENDING
                                | DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED);
                Cursor cursor = downloadManager.query(query);

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            do {
                                int downloadIdIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                int bytesDownloadedIndex = cursor
                                        .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                                int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                                long downloadId = cursor.getLong(downloadIdIndex);
                                int status = cursor.getInt(statusIndex);
                                int bytesDownloaded = cursor.getInt(bytesDownloadedIndex);
                                int bytesTotal = cursor.getInt(bytesTotalIndex);

                                if (status == DownloadManager.STATUS_RUNNING && bytesTotal > 0) {
                                    final int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                    mActivity.runOnUiThread(() -> {
                                        if (downloadAdapter != null)
                                            downloadAdapter.updateProgress(downloadId, progress);
                                    });
                                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    mActivity.runOnUiThread(() -> {
                                        checkPermisi();
                                        removeDownloadItem(downloadId);
                                    });
                                }
                            } while (cursor.moveToNext());
                            cursor.close();
                        }
                        handler.postDelayed(this, 1000);
                    } catch (Exception e) {
                        Log.e(TAG, "Error while querying downloads: " + e.getMessage());
                    } finally {
                        cursor.close();
                    }
                }
            }
        };
        handler.post(runnable);
    }

    private void removeDownloadItem(long downloadId) {
        for (int i = 0; i < downloadItems.size(); i++) {
            if (downloadItems.get(i).getDownloadId() == downloadId) {
                downloadItems.remove(i);
                if (downloadAdapter != null) {
                    downloadAdapter.notifyItemRemoved(i);
                    downloadAdapter.notifyItemRangeChanged(i, downloadItems.size());
                }
                break;
            }
        }
    }

    private void checkPermisi() {
        if (Build.VERSION.SDK_INT < 32) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        STORAGE_PERMISSION_CODE);
            } else {
                loadMediaFiles();
            }
        } else {
            loadMediaFiles();
        }
    }

    private void loadMediaFiles() {
        mediaList.clear(); // Clear existing list to avoid duplicates on refresh
        File movieDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "nfgplus/movies");
        File seriesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "nfgplus/series");

        if (!movieDir.exists())
            movieDir.mkdirs();
        if (!seriesDir.exists())
            seriesDir.mkdirs();

        addFilesFromDirectory(movieDir, "movies");
        addFilesFromDirectory(seriesDir, "series");

        if (mediaList.isEmpty()) {
            if (teksDownload != null)
                teksDownload.setText("You Are Not Download Anything");
        } else {
            if (teksDownload != null)
                teksDownload.setText(isTVDevice ? "" : "Downloaded Files");
            mediaAdapter = new DownloadRecyAdapter(mediaList, getContext(), this, isTVDevice);
            recyclerView.setAdapter(mediaAdapter);
            mediaAdapter.notifyDataSetChanged();
        }
    }

    private void addFilesFromDirectory(File directory, String dirType) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        mediaList.add(new MediaItem(file.getName(), file.getAbsolutePath()));
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMediaFiles();
            }
        }
    }

    @Override
    public void onItemDelete(MediaItem mediaItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle("Delete File")
                .setMessage("Are you sure want to delete this " + mediaItem.getFName() + " permanently?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    File file = new File(mediaItem.getFilePath());
                    if (file.exists() && file.delete()) {
                        mediaList.remove(mediaItem);
                        mediaAdapter.notifyDataSetChanged();
                        if (mediaList.isEmpty()) {
                            if (teksDownload != null)
                                teksDownload.setText("You Are Not Download Anything");
                        }
                    }
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mediaList.clear();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void checkAndRequestMediaPermissions() {
        String[] permissions = {
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
        };
        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(mActivity,
                        Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_MEDIA_PERMISSION);
        }
    }
}
