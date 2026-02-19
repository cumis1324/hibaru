package com.theflexproject.thunder.fragments;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.DownloadItem;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadFragment extends BaseFragment
        implements DownloadRecyAdapter.OnItemDeleteListener, DownloadAdapter.OnCancelListener {
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final String TAG = "DownloadFragment";
    private static final int REQUEST_MEDIA_PERMISSION = 100;
    private RecyclerView recyclerView, downloadingRecy;
    private DownloadRecyAdapter mediaAdapter;
    private List<MyMedia> mediaList = new ArrayList<>();
    private TextView teksDownload;
    private DownloadManager downloadManager;
    private DownloadAdapter downloadAdapter;
    private List<DownloadItem> downloadItems;
    private Handler handler;
    private boolean isTVDevice = false;

    // TV UI Components
    private android.widget.ImageView backgroundImageView;
    private android.widget.LinearLayout dynamicPreview;
    private TextView previewTitle, previewMetadata, previewDescription;
    private TextView titleActiveDownloads, titleLibraryDownloads;
    private View emptyStateContainer;
    private TextView emptyStateText;

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

        // TV Components Init
        if (isTVDevice) {
            backgroundImageView = view.findViewById(R.id.backgroundImageView);
            dynamicPreview = view.findViewById(R.id.dynamicPreview);
            previewTitle = view.findViewById(R.id.previewTitle);
            previewMetadata = view.findViewById(R.id.previewMetadata);
            previewDescription = view.findViewById(R.id.previewDescription);
            titleActiveDownloads = view.findViewById(R.id.titleActiveDownloads);
            titleLibraryDownloads = view.findViewById(R.id.titleLibraryDownloads);
        }

        // Downloading recycler (Active downloads)
        downloadingRecy = view.findViewById(R.id.recyclerDownloading);
        if (downloadingRecy != null) {
            if (!isTVDevice) {
                downloadingRecy.setLayoutManager(new LinearLayoutManager(getContext()));
            } else {
                // TV: HorizontalGridView handles layout
                androidx.leanback.widget.HorizontalGridView hGridView = (androidx.leanback.widget.HorizontalGridView) downloadingRecy;
                hGridView.addOnChildViewHolderSelectedListener(
                        new androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                            @Override
                            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child,
                                    int position, int subposition) {
                                if (child != null) {
                                    // Update Preview for Active Download
                                    if (position != RecyclerView.NO_POSITION && position < downloadItems.size()) {
                                        DownloadItem dItem = downloadItems.get(position);
                                        updatePreview(dItem.getTitle(), "Downloading...",
                                                "Please wait while your media is being downloaded.");
                                    }
                                }
                            }
                        });
            }
            initializeDownloadItems();
            downloadAdapter = new DownloadAdapter(downloadItems, getContext(), isTVDevice, this);
            downloadingRecy.setAdapter(downloadAdapter);
            checkDownloadProgress();

            // TV: Toggle visibility of Active Downloads section
            if (isTVDevice) {
                toggleActiveDownloadsVisibility(!downloadItems.isEmpty());
            }
        }

        recyclerView = view.findViewById(R.id.recyclerDownload);

        if (!isTVDevice && recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        } else if (isTVDevice && recyclerView instanceof androidx.leanback.widget.VerticalGridView) {
            androidx.leanback.widget.VerticalGridView vGridView = (androidx.leanback.widget.VerticalGridView) recyclerView;
            vGridView.addOnChildViewHolderSelectedListener(
                    new androidx.leanback.widget.OnChildViewHolderSelectedListener() {

                        @Override
                        public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child,
                                int position, int subposition) {
                            if (child != null) {
                                if (position != RecyclerView.NO_POSITION && position < mediaList.size()) {
                                    MyMedia mItem = mediaList.get(position);
                                    String title = "";
                                    String path = "";
                                    if (mItem instanceof Movie) {
                                        title = ((Movie) mItem).getTitle();
                                        path = ((Movie) mItem).getLocalPath();
                                    } else if (mItem instanceof Episode) {
                                        title = ((Episode) mItem).getName();
                                        path = ((Episode) mItem).getLocalPath();
                                    }
                                    updatePreview(title, "Downloaded", path);
                                    // Ideally load a backdrop if available, for now just gradient
                                }
                            }
                        }

                    });
        }

        teksDownload = view.findViewById(R.id.teksDownload);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        if (mediaList.isEmpty()) {
            checkPermisi();
        } else {
            mediaAdapter = new DownloadRecyAdapter(mediaList, getContext(), this, isTVDevice);
            recyclerView.setAdapter(mediaAdapter);
        }

        updateGlobalEmptyState();
    }

    private void updatePreview(String title, String metadata, String description) {
        if (previewTitle != null)
            previewTitle.setText(title);
        if (previewMetadata != null)
            previewMetadata.setText(metadata);
        if (previewDescription != null)
            previewDescription.setText(description);
    }

    private void toggleActiveDownloadsVisibility(boolean visible) {
        if (titleActiveDownloads != null)
            titleActiveDownloads.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (downloadingRecy != null)
            downloadingRecy.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void toggleLibraryVisibility(boolean visible) {
        if (isTVDevice) {
            if (titleLibraryDownloads != null)
                titleLibraryDownloads.setVisibility(visible ? View.VISIBLE : View.GONE);
        } else {
            if (teksDownload != null)
                teksDownload.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null)
            recyclerView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateGlobalEmptyState() {
        boolean hasActive = !downloadItems.isEmpty();
        boolean hasLibrary = !mediaList.isEmpty();
        boolean allEmpty = !hasActive && !hasLibrary;

        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
        }

        if (isTVDevice) {
            if (titleLibraryDownloads != null) {
                titleLibraryDownloads.setVisibility(hasLibrary ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(hasLibrary ? View.VISIBLE : View.GONE);
            }
            toggleActiveDownloadsVisibility(hasActive);
        } else {
            // Mobile: Standard logic
            if (teksDownload != null) {
                teksDownload.setVisibility(hasLibrary ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(hasLibrary ? View.VISIBLE : View.GONE);
            }
            if (downloadingRecy != null) {
                downloadingRecy.setVisibility(hasActive ? View.VISIBLE : View.GONE);
            }
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
                int downloadIdIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                while (cursor.moveToNext()) {
                    long downloadId = (downloadIdIndex != -1) ? cursor.getLong(downloadIdIndex) : -1L;
                    String title = (titleIndex != -1) ? cursor.getString(titleIndex) : "Unknown";
                    int status = (statusIndex != -1) ? cursor.getInt(statusIndex) : -1;

                    // Skip if invalid
                    if (downloadId == -1L)
                        continue;

                    // Check if already in list
                    boolean exists = false;
                    for (DownloadItem item : downloadItems) {
                        if (item.getDownloadId() == downloadId) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        // Use title as filename for display since filename column is restricted
                        downloadItems.add(new DownloadItem(title, downloadId, title, status, 0));
                    }
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
                        int downloadIdIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                        int bytesDownloadedIndex = cursor
                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                        while (cursor.moveToNext()) {
                            if (downloadIdIndex == -1)
                                break;

                            long downloadId = cursor.getLong(downloadIdIndex);
                            int status = (statusIndex != -1) ? cursor.getInt(statusIndex) : -1;
                            int bytesDownloaded = (bytesDownloadedIndex != -1) ? cursor.getInt(bytesDownloadedIndex)
                                    : 0;
                            int bytesTotal = (bytesTotalIndex != -1) ? cursor.getInt(bytesTotalIndex) : 0;
                            String title = (titleIndex != -1) ? cursor.getString(titleIndex) : "Unknown";

                            // Dynamic task detection: check if this ID is in our list
                            boolean inList = false;
                            for (DownloadItem item : downloadItems) {
                                if (item.getDownloadId() == downloadId) {
                                    inList = true;
                                    break;
                                }
                            }

                            // If running/pending and NOT in list, add it
                            if (!inList && (status == DownloadManager.STATUS_RUNNING
                                    || status == DownloadManager.STATUS_PENDING
                                    || status == DownloadManager.STATUS_PAUSED)) {
                                final DownloadItem newItem = new DownloadItem(title, downloadId, title, status, 0);
                                mActivity.runOnUiThread(() -> {
                                    downloadItems.add(newItem);
                                    if (downloadAdapter != null) {
                                        downloadAdapter.notifyItemInserted(downloadItems.size() - 1);
                                    }
                                    toggleActiveDownloadsVisibility(true);
                                    updateGlobalEmptyState();
                                });
                            }

                            if (status == DownloadManager.STATUS_RUNNING && bytesTotal > 0) {
                                final int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                mActivity.runOnUiThread(() -> {
                                    if (downloadAdapter != null)
                                        downloadAdapter.updateProgress(downloadId, progress);
                                });
                            } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                mActivity.runOnUiThread(() -> {
                                    removeDownloadItem(downloadId);
                                    // Small delay to let system finalize file move/renaming
                                    @SuppressLint("Range")
                                    String localUri = cursor
                                            .getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                    // No log needed here
                                    new Handler(Looper.getMainLooper())
                                            .postDelayed(() -> DownloadFragment.this.loadMediaFiles(), 500);
                                });
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                mActivity.runOnUiThread(() -> removeDownloadItem(downloadId));
                            }
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

                if (isTVDevice && downloadItems.isEmpty()) {
                    toggleActiveDownloadsVisibility(false);
                }
                updateGlobalEmptyState();
                break;
            }
        }
    }

    private void checkPermisi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            boolean hasVideo = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            if (!hasVideo) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[] { Manifest.permission.READ_MEDIA_VIDEO }, STORAGE_PERMISSION_CODE);
            } else {
                loadMediaFiles();
            }
        } else {
            // Android 12 and below
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
        }
    }

    private void loadMediaFiles() {
        if (!isAdded())
            return;
        mediaList.clear();
        AsyncTask.execute(() -> {
            try {
                // 1. Determine primary download directories
                String subPath = "nfgplus/downloads";
                java.util.Map<String, String> filePathsMap = new java.util.HashMap<>();

                // Public Dir (New Standard)
                File publicFolder = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), subPath);
                if (!publicFolder.exists())
                    publicFolder.mkdirs();
                File[] publicFiles = publicFolder.listFiles();
                if (publicFiles != null) {
                    for (File f : publicFiles) {
                        if (f.isFile() && f.length() > 0) {
                            filePathsMap.put(f.getName().toLowerCase(), f.getAbsolutePath());
                        }
                    }
                }

                // Private Dir (Old Standard / Fallback)
                File privateFolder = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        subPath);
                if (!privateFolder.exists())
                    privateFolder.mkdirs();
                File[] privateFiles = privateFolder.listFiles();
                if (privateFiles != null) {
                    for (File f : privateFiles) {
                        if (f.isFile() && f.length() > 0) {
                            if (!filePathsMap.containsKey(f.getName().toLowerCase())) {
                                filePathsMap.put(f.getName().toLowerCase(), f.getAbsolutePath());
                            }
                        }
                    }
                }

                // 3. Fallback: Query DownloadManager
                DownloadManager.Query q = new DownloadManager.Query();
                Cursor c = downloadManager.query(q);
                if (c != null) {
                    try {
                        int uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        while (c.moveToNext()) {
                            String uriStr = (uriIdx != -1) ? c.getString(uriIdx) : null;
                            int status = (statusIdx != -1) ? c.getInt(statusIdx) : -1;

                            if (uriStr != null && status == DownloadManager.STATUS_SUCCESSFUL) {
                                String path = uriStr;
                                if (uriStr.startsWith("file://")) {
                                    path = Uri.parse(uriStr).getPath();
                                }

                                String fileName = null;
                                if (path != null) {
                                    fileName = new File(path).getName();
                                }

                                if (fileName != null && !fileName.isEmpty()) {
                                    if (!filePathsMap.containsKey(fileName.toLowerCase())) {
                                        filePathsMap.put(fileName.toLowerCase(), path);
                                    }
                                }
                            }
                        }
                    } finally {
                        c.close();
                    }
                }

                if (filePathsMap.isEmpty()) {
                    updateUIWithValidMedia(new ArrayList<>());
                    return;
                }

                List<String> foundNames = new ArrayList<>(filePathsMap.keySet());

                // 4. Query DB for metadata (Resilient approach)
                List<Movie> movies = DatabaseClient.getInstance(requireContext()).getAppDatabase().movieDao()
                        .getMoviesByFileNames(foundNames);
                List<Episode> episodes = DatabaseClient.getInstance(requireContext()).getAppDatabase().episodeDao()
                        .getEpisodesByFileNames(foundNames);

                // Fallback for legacy items (NULL file_name in DB)
                // If we found files but query returned fewer movies/episodes, we might need a
                // broader check.
                // For now, let's at least ensure we find all movies if file_name is missing but
                // we've seen them before.
                if (movies.size() + episodes.size() < foundNames.size()) {
                    List<Movie> allStoredMovies = DatabaseClient.getInstance(requireContext()).getAppDatabase()
                            .movieDao()
                            .getAllMoviesSync();
                    for (Movie m : allStoredMovies) {
                        if (m.getFileName() == null && m.getLocalPath() != null) {
                            String nameOnDisk = new File(m.getLocalPath()).getName().toLowerCase();
                            if (filePathsMap.containsKey(nameOnDisk)) {
                                boolean alreadyIn = false;
                                for (Movie existing : movies) {
                                    if (existing.getId() == m.getId()) {
                                        alreadyIn = true;
                                        break;
                                    }
                                }
                                if (!alreadyIn)
                                    movies.add(m);
                            }
                        }
                    }

                    List<Episode> allStoredEpisodes = DatabaseClient.getInstance(requireContext()).getAppDatabase()
                            .episodeDao().getAllEpisodesSync();
                    for (Episode e : allStoredEpisodes) {
                        if (e.getFileName() == null && e.getLocalPath() != null) {
                            String nameOnDisk = new File(e.getLocalPath()).getName().toLowerCase();
                            if (filePathsMap.containsKey(nameOnDisk)) {
                                boolean alreadyIn = false;
                                for (Episode existing : episodes) {
                                    if (existing.getId() == e.getId()) {
                                        alreadyIn = true;
                                        break;
                                    }
                                }
                                if (!alreadyIn)
                                    episodes.add(e);
                            }
                        }
                    }
                }

                List<MyMedia> validMedia = new ArrayList<>();
                for (Movie movie : movies) {
                    String fn = movie.getFileName();
                    if (fn == null)
                        continue;
                    String path = filePathsMap.get(fn.toLowerCase());
                    if (path != null) {
                        movie.setLocalPath(path);
                        validMedia.add(movie);
                    }
                }
                for (Episode episode : episodes) {
                    String fn = episode.getFileName();
                    if (fn == null)
                        continue;
                    String path = filePathsMap.get(fn.toLowerCase());
                    if (path != null) {
                        episode.setLocalPath(path);
                        validMedia.add(episode);
                    }
                }

                updateUIWithValidMedia(validMedia);

            } catch (Exception e) {
                Log.e("DownloadFragment", "Scan Error: " + e.getMessage());
            }
        });
    }

    private void updateUIWithValidMedia(List<MyMedia> validMedia) {
        if (!isAdded() || mActivity == null)
            return;
        mActivity.runOnUiThread(() -> {
            mediaList.clear();
            mediaList.addAll(validMedia);

            if (mediaList.isEmpty()) {
                if (!isTVDevice && teksDownload != null) {
                    teksDownload.setText("");
                }
                toggleLibraryVisibility(false);
            } else {
                if (teksDownload != null) {
                    teksDownload.setText(isTVDevice ? "" : "Downloaded Files");
                }
                toggleLibraryVisibility(true);

                if (mediaAdapter == null) {
                    mediaAdapter = new DownloadRecyAdapter(mediaList, requireContext(), this, isTVDevice);
                    if (recyclerView != null) {
                        recyclerView.setAdapter(mediaAdapter);
                    }
                } else {
                    mediaAdapter.setMediaList(mediaList);
                    mediaAdapter.notifyDataSetChanged();
                }

                // TV-only preview update for the first item
                if (isTVDevice && !mediaList.isEmpty()) {
                    MyMedia first = mediaList.get(0);
                    String title = (first instanceof Movie) ? ((Movie) first).getTitle() : ((Episode) first).getName();
                    String path = (first instanceof Movie) ? ((Movie) first).getLocalPath()
                            : ((Episode) first).getLocalPath();
                    updatePreview(title, "Downloaded", "Ready to play.");
                }
            }
            updateGlobalEmptyState();
        });
    }

    @Override
    public void onItemDelete(MyMedia mediaItem) {
        String title = mediaItem instanceof Movie ? ((Movie) mediaItem).getTitle() : ((Episode) mediaItem).getName();
        String localPath = mediaItem instanceof Movie ? ((Movie) mediaItem).getLocalPath()
                : ((Episode) mediaItem).getLocalPath();

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle("Delete File")
                .setMessage("Are you sure want to delete this " + title + " permanently?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (localPath != null) {
                        File file = new File(localPath);
                        if (file.exists() && file.delete()) {
                            mediaList.remove(mediaItem);
                            mediaAdapter.notifyDataSetChanged();

                            // Delete from DB
                            AsyncTask.execute(() -> {
                                if (mediaItem instanceof Movie) {
                                    DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao()
                                            .delete((Movie) mediaItem);
                                } else {
                                    DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao()
                                            .delete((Episode) mediaItem);
                                }
                            });

                            updateGlobalEmptyState();
                        } else {
                            // If file doesn't exist but still in DB, delete from DB anyway
                            AsyncTask.execute(() -> {
                                if (mediaItem instanceof Movie) {
                                    DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao()
                                            .delete((Movie) mediaItem);
                                } else {
                                    DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao()
                                            .delete((Episode) mediaItem);
                                }
                            });
                            mediaList.remove(mediaItem);
                            mediaAdapter.notifyDataSetChanged();
                            updateGlobalEmptyState();
                        }
                    }
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onCancel(DownloadItem item) {
        new AlertDialog.Builder(mActivity)
                .setTitle("Cancel Download")
                .setMessage("Are you sure you want to cancel the download for " + item.getTitle() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    downloadManager.remove(item.getDownloadId());
                    removeDownloadItem(item.getDownloadId());

                    // Cleanup DB and partial files
                    AsyncTask.execute(() -> {
                        // We need to find which Movie/Episode this belongs to.
                        // Since DownloadItem only has ID and title, we query by downloadId.
                        Movie movie = DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao()
                                .getMovieByDownloadId(item.getDownloadId());
                        if (movie != null) {
                            if (movie.getLocalPath() != null)
                                new File(movie.getLocalPath()).delete();
                            DatabaseClient.getInstance(getContext()).getAppDatabase().movieDao().delete(movie);
                        } else {
                            Episode episode = DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao()
                                    .getEpisodeByDownloadId(item.getDownloadId());
                            if (episode != null) {
                                if (episode.getLocalPath() != null)
                                    new File(episode.getLocalPath()).delete();
                                DatabaseClient.getInstance(getContext()).getAppDatabase().episodeDao().delete(episode);
                            }
                        }
                    });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
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
    public void onDestroyView() {
        super.onDestroyView();
        mediaList.clear();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

}
