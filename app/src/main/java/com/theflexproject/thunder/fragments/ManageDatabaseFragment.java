package com.theflexproject.thunder.fragments;

import com.theflexproject.thunder.utils.StorageUtils;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.database.AppDatabase;
import com.theflexproject.thunder.model.FirebaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class ManageDatabaseFragment extends BaseFragment {

    Button importDatabase;
    Button exportDatabase;
    FirebaseManager firebaseManager;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    public ManageDatabaseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manage_database, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWidgets();

        setMyOnClickListeners();
        firebaseManager = new FirebaseManager();
        currentUser = firebaseManager.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("database");

    }

    private void setMyOnClickListeners() {
        exportDatabase.setOnClickListener(v -> {

            // Get the user's UID
            String uid = currentUser.getUid();

            // Reference to the user's folder in Firebase Storage
            StorageReference storageRef = storageReference.child(uid);

            // Create a reference to the backup file
            StorageReference backupRef = storageRef.child("Backup.db");

            // Get the Uri of the backup file
            Uri backupUri = Uri.fromFile(new File(mActivity.getDatabasePath("MyToDos").toString()));

            // Upload the file to Firebase Storage
            UploadTask uploadTask = backupRef.putFile(backupUri);

            // Monitor the upload task
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(mActivity, "Backup Successful", Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(mActivity, "Backup Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
        importDatabase.setOnClickListener(v -> {
            // Get the user's UID
            String uid = currentUser.getUid();

            // Reference to the user's folder in Firebase Storage
            StorageReference storageRef = storageReference.child(uid);

            // Create a reference to the backup file
            StorageReference backupRef = storageRef.child("Backup.db");

            // Create a local file for storing the downloaded data
            File databaseFile = mActivity.getDatabasePath("MyToDos");

            // Download the file from Firebase Storage
            backupRef.getFile(databaseFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Update your local database with the downloaded file
                        try {
                            // Manually copy the database file to the application's database directory
                            File appDatabaseFile = mActivity.getDatabasePath("MyToDos");
                            copyFile(databaseFile, appDatabaseFile);

                            // Now, build the Room database as usual
                            AppDatabase appDatabase = Room.databaseBuilder(mActivity, AppDatabase.class, "MyToDos")
                                    .fallbackToDestructiveMigration()
                                    .build();

                            Toast.makeText(mActivity, "Restore Successful", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(mActivity, "Restore Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle download failure
                        String errorMessage;
                        if (e instanceof StorageException) {
                            int errorCode = ((StorageException) e).getErrorCode();
                            errorMessage = "Download Failed. StorageException: " + errorCode;
                        } else {
                            errorMessage = "Download Failed: " + e.getMessage();
                        }
                        Toast.makeText(mActivity, errorMessage, Toast.LENGTH_LONG).show();
                    });
        });
    }

    // Helper method to copy a file
    private void copyFile(File sourceFile, File destFile) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
                OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private void initWidgets() {
        importDatabase = mActivity.findViewById(R.id.importDatabase);
        exportDatabase = mActivity.findViewById(R.id.exportDatabase);

    }

}

// private void setMyOnClickListeners() {
// importDatabase.setOnClickListener(v -> {
// mActivity.deleteDatabase("MyToDos");

// File folder = new
// File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
// + "/Thunder");
// File[] listOfFiles = folder.listFiles();
//
// File database = mActivity.getDatabasePath("MyToDos");
// System.out.println("paths" + backup + "\npath2 " + database);
//
// if(listOfFiles!=null)
// for (File file : listOfFiles) {
// if (file.exists() && file.isFile()) {
// try {
// System.out.println("backup exists true");
// FileChannel src = new FileInputStream(file).getChannel();
// FileChannel dst = new FileOutputStream(database).getChannel();
// dst.transferFrom(src , 0 , src.size());
//// fco.transferFrom(fc2, fc1.size() - 1, fc2.size());
// } catch (Exception e) {
// e.printStackTrace();
// }
// System.out.println(file.getName());
// }
// }

// File backup = new
// File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
// + "/Thunder" , "ThunderBackup.db");
// File database = mActivity.getDatabasePath("MyToDos");
// System.out.println("paths" + backup + "\npath2 " + database);

// try {
// if (backup.exists()) {
// System.out.println("backup exists true");
//
// FileChannel src = new FileInputStream(backup).getChannel();
// FileChannel dst = new FileOutputStream(database).getChannel();
// dst.transferFrom(src , 0 , src.size());
// src.close();
// dst.close();
// Room.databaseBuilder(mActivity ,
// AppDatabase.class , "MyToDos")
// .fallbackToDestructiveMigration()
// .createFromFile(database)
// .build();
// }
// } catch (Exception e) {
// e.printStackTrace();
// }

// Toast.makeText(mActivity,"Import Successful",Toast.LENGTH_LONG).show();

// });

// exportDatabase.setOnClickListener(v -> {

// verifyStoragePermissions(mActivity);

// int EXTERNAL_STORAGE_PERMISSION_CODE = 23;
// Requesting Permission to access External Storage
// ActivityCompat.requestPermissions(mActivity, new
// String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
// EXTERNAL_STORAGE_PERMISSION_CODE);

// File folder =
// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

// File backupDB = new File(folder +"/Thunder", "ThunderBackup.db");
// File currentDB = new File(mActivity.getDatabasePath("MyToDos").toString());

// try {

// File backupDir = new File(folder+"/Thunder");
// if(!backupDir.exists()) backupDir.mkdir();
// if (currentDB.exists()) {
// FileChannel src = new FileInputStream(currentDB).getChannel();
// FileChannel dst = new FileOutputStream(backupDB).getChannel();
// dst.transferFrom(src , 0 , src.size());
// src.close();
// dst.close();
// }
// } catch (Exception e) {
// e.printStackTrace();
// }

// Toast.makeText(mActivity,"Export Successful",Toast.LENGTH_LONG).show();

// });