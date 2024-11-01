package com.theflexproject.thunder.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.DownloadRecyAdapter;
import com.theflexproject.thunder.model.MediaItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends BaseFragment implements DownloadRecyAdapter.OnItemDeleteListener{
    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final String TAG = "DownloadFragment";
    private static final int FILE_SELECT_CODE = 0;
    private RecyclerView recyclerView;
    private DownloadRecyAdapter mediaAdapter;
    private List<MediaItem> mediaList = new ArrayList<>();
    private TextView teksDownload;
    private FloatingActionButton fabAddFile;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerDownload);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        teksDownload = view.findViewById(R.id.teksDownload);
        fabAddFile = view.findViewById(R.id.fabAddFile);
        fabAddFile.setOnClickListener(v -> openFileChooser());

        if (mediaList.isEmpty()) {
            checkPermisi();
            Log.d(TAG, "Checking storage permissions...");
        }else{
            mediaAdapter = new DownloadRecyAdapter(mediaList, getContext());
            recyclerView.setAdapter(mediaAdapter);
        }

    }

    private void checkPermisi() {
        if (Build.VERSION.SDK_INT < 32) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                Log.d(TAG, "Permission not granted. Requesting storage permissions...");
            } else {
                Log.i(TAG, "Izin READ_EXTERNAL_STORAGE sudah diberikan.");
                loadMediaFiles();
            }
        }else{
            Log.i(TAG, "Memuat file media tanpa memerlukan izin READ_EXTERNAL_STORAGE (Android 13+).");
            loadMediaFiles();
        }
    }

    private void loadMediaFiles() {
        File movieDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "nfgplus/movies");
        File seriesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "nfgplus/series");

        Log.d(TAG, "Memuat file media dari direktori: " + movieDir.getAbsolutePath());
        addFilesFromDirectory(movieDir);

        Log.d(TAG, "Memuat file media dari direktori: " + seriesDir.getAbsolutePath());
        addFilesFromDirectory(seriesDir);



        if (mediaList.isEmpty()) {
            teksDownload.setText("You Are Not Download Anything");
            Log.i(TAG, "Tidak ada media yang ditemukan di direktori.");;
        }else {
            mediaAdapter = new DownloadRecyAdapter(mediaList, getContext());
            recyclerView.setAdapter(mediaAdapter);
        }
    }

    private void addFilesFromDirectory(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                Log.d(TAG, "Menambahkan file dari direktori: " + directory.getName() + ", jumlah file: " + files.length);
                for (File file : files) {
                    if (file.isFile()) {
                        Log.d(TAG, "Menambahkan file: " + file.getName());
                        scanWithMediaStore(file);
                        mediaList.add(new MediaItem(file.getName(), file.getAbsolutePath()));
                    }
                }
            }else {
                Log.w(TAG, "Tidak ada file dalam direktori: " + directory.getName());
            }
        }else {
            Log.w(TAG, "Direktori tidak ditemukan atau bukan direktori: " + directory.getAbsolutePath());
        }
    }

    private void scanWithMediaStore(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            mActivity.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        }else {
            mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMediaFiles();
            }
        }
    }
   @Override
    public void onItemDelete(MediaItem mediaItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle("Delete File")
                .setMessage("Are you sure want to delete this "+mediaItem.getFName()+"?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(mediaItem.getFilePath());
                        if (file.exists() && file.delete()){
                            mediaList.remove(mediaItem);
                            mediaAdapter.notifyDataSetChanged();
                            if (mediaList.isEmpty()){
                                teksDownload.setText("You Are Not Download Anything");
                            }
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Menutup dialog
                    }
                });

       AlertDialog dialog = builder.create();
       dialog.show();

    }

    @Override
    public void onDestroyView() {
        mediaList.clear();
        super.onDestroyView();
    }
    private void openFileChooser() {
        if (Build.VERSION.SDK_INT < 32) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                Log.d(TAG, "Permission not granted. Requesting storage permissions...");
            } else {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*"); // Untuk memilih semua jenis file
                startActivityForResult(Intent.createChooser(intent, "Pilih file"), FILE_SELECT_CODE);
            }
        }else{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*"); // Untuk memilih semua jenis file
            startActivityForResult(Intent.createChooser(intent, "Pilih file"), FILE_SELECT_CODE);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String path = uri.getPath();
                // Tambahkan file ke mediaList
                if (path != null) {
                    mediaList.add(new MediaItem(new File(path).getName(), path));
                    mediaAdapter.notifyItemInserted(mediaList.size() - 1);
                }
            }
        }
    }
}
