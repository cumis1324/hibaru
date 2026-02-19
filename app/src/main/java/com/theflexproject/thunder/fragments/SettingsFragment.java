package com.theflexproject.thunder.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.utils.SettingsManager;

public class SettingsFragment extends BaseFragment {

    private View addIndex; // Can be Button or TextView
    private View viewIndexes; // Can be Button or TextView
    private View importExportDatabase; // Can be Button or TextView
    private View checkForUpdate; // Can be Button or TextView
    private View telegram; // Can be Button or TextView

    SwitchCompat externalPlayerToggle;
    SwitchCompat refreshPeriodicallyToggle;

    ImageView profilePicture;
    TextView userProfile;
    TextView userfullname;

    Dialog d;
    SettingsManager settingsManager;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    FirebaseManager manager;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private boolean isTVDevice = false;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        manager = new FirebaseManager();
        currentUser = manager.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        isTVDevice = isTVDevice();
        if (isTVDevice) {
            return inflater.inflate(R.layout.fragment_settings_tv, container, false);
        } else {
            return inflater.inflate(R.layout.fragment_settings, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWidgets(view);
        updateUI();
        setStatesOfToggleSwitches();
        setMyOnClickListeners();
    }

    private void initWidgets(View view) {
        addIndex = view.findViewById(R.id.addIndexButton);
        // viewIndexes = view.findViewById(R.id.viewIndexes);
        importExportDatabase = view.findViewById(R.id.importExportDatabase);
        importExportDatabase.setVisibility(View.GONE);
        checkForUpdate = view.findViewById(R.id.checkforUpdates);
        telegram = view.findViewById(R.id.telegroup);

        externalPlayerToggle = view.findViewById(R.id.externalPlayerToggle);
        externalPlayerToggle.setVisibility(View.GONE); // Hide the external player toggle
        // refreshPeriodicallyToggle =
        // view.findViewById(R.id.refreshPeriodicallyToggle);

        settingsManager = new SettingsManager(mActivity);
        d = new Dialog(mActivity);

        profilePicture = view.findViewById(R.id.profileImageView);
        userProfile = view.findViewById(R.id.profile);
        userfullname = view.findViewById(R.id.fullname);
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            uploadImageToStorage();
        }
    }

    private void uploadImageToStorage() {
        if (imageUri != null) {
            String filename = currentUser.getUid() + "_profile_image";
            StorageReference imageRef = storageReference.child(filename);

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            databaseReference.child(currentUser.getUid()).child("profileImage").setValue(imageUrl);
                            loadProfileImage(imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Handle failed image upload
                    });
        }
    }

    private void setMyOnClickListeners() {
        if (addIndex != null) {
            addIndex.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS)));
        }

        if (viewIndexes != null) {
            viewIndexes.setOnClickListener(v -> {
                // ManageIndexesFragment manageIndexesFragment = new ManageIndexesFragment();
                // mActivity.getSupportFragmentManager().beginTransaction()
                // .setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left,
                // R.anim.to_right)
                // .replace(R.id.container,
                // manageIndexesFragment).addToBackStack(null).commit();
            });
        }

        if (importExportDatabase != null) {
            importExportDatabase.setOnClickListener(v -> {
                // ManageDatabaseFragment manageDatabaseFragment = new ManageDatabaseFragment();
                // mActivity.getSupportFragmentManager().beginTransaction()
                // .setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left,
                // R.anim.to_right)
                // .replace(R.id.container,
                // manageDatabaseFragment).addToBackStack(null).commit();
            });
        }

        if (externalPlayerToggle != null) {
            externalPlayerToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsManager.saveExternal(isChecked);
            });
        }

        if (refreshPeriodicallyToggle != null) {
            refreshPeriodicallyToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    showTimeDialog();
                } else {
                    settingsManager.saveRefresh(true, 0);
                }
            });
        }

        if (checkForUpdate != null) {
            checkForUpdate.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.gelaskaca.nfgplus")));
            });
        }

        if (telegram != null) {
            telegram.setOnClickListener(
                    v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/nfgplus1"))));
        }

        if (profilePicture != null) {
            profilePicture.setOnClickListener(v -> openImageChooser());
        }
    }

    private void setStatesOfToggleSwitches() {
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean savedREF = sharedPreferences.getBoolean("REFRESH_SETTING", true);
        String s = "Refresh at : " + sharedPreferences.getInt("REFRESH_TIME", 0) + ":00";

        if (refreshPeriodicallyToggle != null) {
            mActivity.runOnUiThread(() -> refreshPeriodicallyToggle.setText(s));
            refreshPeriodicallyToggle.setChecked(savedREF);
        }

        boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING", false);
        if (externalPlayerToggle != null) {
            externalPlayerToggle.setChecked(savedEXT);
        }
    }

    Integer timeToRefresh = 0;

    public void showTimeDialog() {
        d.setTitle("SelectTime");
        d.setContentView(R.layout.refresh_time_dialog);
        Button b1 = d.findViewById(R.id.button1);
        final NumberPicker np = d.findViewById(R.id.numberPicker1);
        np.setMaxValue(23);
        np.setMinValue(0);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener((picker, oldVal, newVal) -> timeToRefresh = newVal);
        b1.setOnClickListener(v -> {
            settingsManager.saveRefresh(true, timeToRefresh);
            d.dismiss();
        });
        d.show();
    }

    private void loadProfileImage(String imageUrl) {
        if (isAdded() && mActivity != null && profilePicture != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(profilePicture);
        }
    }

    private void updateUI() {
        if (currentUser != null) {
            databaseReference.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!isAdded() || mActivity == null) return;
                    if (dataSnapshot.hasChild("profileImage")) {
                        String imageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                        loadProfileImage(imageUrl);
                    }
                    updateUserData(dataSnapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    private void updateUserData(DataSnapshot dataSnapshot) {
        String username = dataSnapshot.child("username").getValue(String.class);
        String email = dataSnapshot.child("email").getValue(String.class);

        if (username != null && userProfile != null) {
            userProfile.setText("Hallo, " + username);
            userProfile.setVisibility(View.VISIBLE);
        }

        if (email != null && userfullname != null) {
            userfullname.setText(email);
            userfullname.setVisibility(View.VISIBLE);
        }
    }

    private boolean isTVDevice() {
        UiModeManager uiModeManager = (UiModeManager) requireContext().getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}