package com.theflexproject.thunder.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.MediaAdapter;
import com.theflexproject.thunder.adapter.ScaleCenterItemLayoutManager;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.FirebaseManager;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.MyMedia;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.utils.DetailsUtils;
import com.theflexproject.thunder.utils.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends BaseFragment {

    AppCompatButton addIndex;
    AppCompatButton viewIndexes;
    AppCompatButton importExportDatabase;
    Button checkForUpdate;

    ImageButton discord;
    ImageButton github;
    Button telegram;
    ImageButton profilePicture;
    TextView userProfile;
    TextView userfullname;

    SwitchCompat externalPlayerToggle;
    SwitchMaterial castButtonToggle;
    SwitchCompat refreshPeriodicallyToggle;
    //recyler
    MediaAdapter lastPlayedMoviesRecyclerViewAdapter;
    MediaAdapter watchlistRecyclerViewAdapter;
    TextView lastPlayedMoviesRecyclerViewTitle;
    RecyclerView lastPlayedMoviesRecyclerView;

    List<Movie> lastPlayedList;
    List<Episode> episodeList;
    List<MyMedia> watchlist;

    TextView watchlistRecyclerViewTitle;
    RecyclerView watchlistRecyclerView;
    MediaAdapter.OnItemClickListener lastPlayedListener;
    MediaAdapter.OnItemClickListener watchlistListener;

    Dialog d;

    SettingsManager settingsManager;

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    FirebaseManager manager;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private SwipeRefreshLayout swipeRefreshLayout;
    List<String> itemIds = new ArrayList<>();
    String userId;
    DatabaseReference userRef;
    public SettingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        manager = new FirebaseManager();
        currentUser = manager.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");
        userId = currentUser.getUid();
        userRef = databaseReference.child(userId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater , ViewGroup container ,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings , container , false);

    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view , savedInstanceState);

        // Initialize Firebase components

        watchlistRecyclerView = view.findViewById(R.id.watchListMediaRecycler2);
        lastPlayedMoviesRecyclerView = view.findViewById(R.id.lastPlayedMoviesRecycler2);
        initWidgets();
        updateUI();

        setStatesOfToggleSwitches();

        setMyOnClickListeners();
        setOnClickListner();
        getHistoryFb();
        getFavorit();




    }

    private void initWidgets() {
        addIndex = mActivity.findViewById(R.id.addIndexButton);
        viewIndexes = mActivity.findViewById(R.id.viewIndexes);
        importExportDatabase = mActivity.findViewById(R.id.importExportDatabase);

        checkForUpdate = mActivity.findViewById(R.id.checkforUpdates);

        discord = mActivity.findViewById(R.id.discordImageButton);
        github = mActivity.findViewById(R.id.githubImageButton);
        telegram = mActivity.findViewById(R.id.telegroup);


        externalPlayerToggle = mActivity.findViewById(R.id.externalPlayerToggle);
//        castButtonToggle = mActivity.findViewById(R.id.castButtonVisibility);
        refreshPeriodicallyToggle =mActivity.findViewById(R.id.refreshPeriodicallyToggle);

        settingsManager = new SettingsManager(mActivity);

        d = new Dialog(mActivity);

        profilePicture = mActivity.findViewById(R.id.profileImageView);
        userProfile = mActivity.findViewById(R.id.profile);
        userfullname = mActivity.findViewById(R.id.fullname);
    }

    //Image Chooser
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

            // Upload the image to Firebase Storage and update the user's profile
            uploadImageToStorage();
        }
    }

    private void uploadImageToStorage() {
        if (imageUri != null) {
            // Create a unique filename for the image
            String filename = currentUser.getUid() + "_profile_image";
            StorageReference imageRef = storageReference.child(filename);

            // Upload image to Firebase Storage
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL and update the user's profile
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            // Update the user's profile with the new image URL
                            databaseReference.child(currentUser.getUid()).child("profileImage").setValue(imageUrl);
                            loadProfileImage(imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Handle failed image upload
                    });
        }
    }
    // on click button
    private void setMyOnClickListeners() {



        addIndex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
            }
        });

        viewIndexes.setOnClickListener(v -> {
            ManageIndexesFragment manageIndexesFragment = new ManageIndexesFragment();
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                    .replace(R.id.container, manageIndexesFragment).addToBackStack(null).commit();
        });

        importExportDatabase.setOnClickListener( v -> {
            ManageDatabaseFragment manageDatabaseFragment = new ManageDatabaseFragment();
            mActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.from_right,R.anim.to_left,R.anim.from_left,R.anim.to_right)
                    .replace(R.id.container, manageDatabaseFragment).addToBackStack(null).commit();
        });

        externalPlayerToggle.setOnCheckedChangeListener((buttonView , isChecked) -> {
            settingsManager.saveExternal(isChecked);
        });

        refreshPeriodicallyToggle.setOnCheckedChangeListener((buttonView , isChecked) -> {
//            settingsManager.saveRefresh(isChecked);
            if(isChecked){
                showTimeDialog();
            }else {
                settingsManager.saveRefresh(true,0);
            }
        });

        checkForUpdate.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.gelaskaca.nfgplus")));

        });

        //        castButtonToggle.setOnCheckedChangeListener(
//                (buttonView , isChecked) -> {
//                    if(isChecked){settingsManager.saveCast(true);}else{settingsManager.saveCast(false);}
//                });


        discord.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/NWrz5euMJs"))));
        github.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anujd64/Thunder"))));
        telegram.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/nfgplus1"))));

    }

    private void setStatesOfToggleSwitches() {
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean savedREF = sharedPreferences.getBoolean("REFRESH_SETTING", true);
        String s = "Refresh at : "+ sharedPreferences.getInt("REFRESH_TIME",0)+":00";

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshPeriodicallyToggle.setText(s);
            }
        });

        boolean savedEXT = sharedPreferences.getBoolean("EXTERNAL_SETTING", false);
        boolean savedCAST = sharedPreferences.getBoolean("CAST_SETTING", false);
        externalPlayerToggle.setChecked(savedEXT);
        refreshPeriodicallyToggle.setChecked(savedREF);

//        castButtonToggle.setChecked(savedCAST);
    }


    Integer timeToRefresh =0;
    public void showTimeDialog()
    {

        d.setTitle("SelectTime");
        d.setContentView(R.layout.refresh_time_dialog);
        Button b1 = d.findViewById(R.id.button1);
//        Button b2 = (Button) d.findViewById(R.id.button2);
        final NumberPicker np = d.findViewById(R.id.numberPicker1);
        np.setMaxValue(23);
        np.setMinValue(0);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker , int oldVal , int newVal) {
                timeToRefresh =newVal;
            }
        });
        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                settingsManager.saveRefresh(true,timeToRefresh);
                d.dismiss();
            }
        });
//        b2.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v) {
//                d.dismiss();
//            }
//        });


        d.show();

    }


    // profile picture adapter


    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .into(profilePicture);
    }

    private void updateUI() {
        if (currentUser != null) {

            // Update the user's profile with the new image URL


            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Ensure the "profileImage" field exists in the database
                    if (dataSnapshot.hasChild("profileImage")) {
                        String imageUrl = dataSnapshot.child("profileImage").getValue(String.class);

                        // Load the profile image
                        loadProfileImage(imageUrl);
                    }
                    updateUserData(dataSnapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle the error
                }
            });
        }
    }
    private void updateUserData(DataSnapshot dataSnapshot) {
        // Retrieve user data
        String username = dataSnapshot.child("username").getValue(String.class);
        String firstName = dataSnapshot.child("firstName").getValue(String.class);
        String lastName = dataSnapshot.child("lastName").getValue(String.class);
        String email = dataSnapshot.child("email").getValue(String.class);

        // Display user data in the UI
        if (username != null) {
            userProfile.setText("Hallo, " + username);
            userProfile.setVisibility(View.VISIBLE);
        }

        if (email!=null) {

            userfullname.setText(email);
            userfullname.setVisibility(View.VISIBLE);
        }
    }

    private void loadLastPlayedMovies(List<String> itemIds) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                lastPlayedList = DetailsUtils.getHistoryMovies(mActivity, itemIds);

                if(lastPlayedList!=null && lastPlayedList.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            lastPlayedMoviesRecyclerView.setVisibility(View.VISIBLE);
                            lastPlayedMoviesRecyclerView.setLayoutManager(linearLayoutManager3);
                            lastPlayedMoviesRecyclerView.setHasFixedSize(true);
                            lastPlayedMoviesRecyclerViewAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) lastPlayedList , lastPlayedListener);
                            lastPlayedMoviesRecyclerView.setAdapter(lastPlayedMoviesRecyclerViewAdapter);
                        }
                    });

                }
            }});
        thread.start();
    }
    private void loadWatchlist(List<String> itemIds) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Movie> watchlistMovies = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .loadAllByIds(itemIds);

                List<TVShow> watchlistShows = DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .loadAllByIds(itemIds);

                watchlist = new ArrayList<>();
                watchlist.addAll(watchlistMovies);
                watchlist.addAll(watchlistShows);
                if(watchlist!=null && watchlist.size()>0){
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScaleCenterItemLayoutManager linearLayoutManager3 = new ScaleCenterItemLayoutManager(getContext() , LinearLayoutManager.HORIZONTAL , false);

                            watchlistRecyclerView.setVisibility(View.VISIBLE);
                            watchlistRecyclerView.setLayoutManager(linearLayoutManager3);
                            watchlistRecyclerView.setHasFixedSize(true);
                            watchlistRecyclerViewAdapter = new MediaAdapter(getContext() ,(List<MyMedia>)(List<?>) watchlist , watchlistListener);
                            watchlistRecyclerView.setAdapter(watchlistRecyclerViewAdapter);
                        }
                    });

                }
            }});
        thread.start();

    }
    private void setOnClickListner() {
        lastPlayedListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(lastPlayedList.get(position).getId());
                mActivity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .add(R.id.container, movieDetailsFragment).addToBackStack(null).commit();
            }
        };

        watchlistListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                int id = 0;
                if (watchlist.get(position) instanceof Movie) {
                    id = ((Movie) watchlist.get(position)).getId();
                    MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(id);
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                            .add(R.id.container, movieDetailsFragment).addToBackStack(null).commit();
                } else {
                    id = ((TVShow) (watchlist.get(position))).getId();
                    TvShowDetailsFragment tvShowDetailsFragment = new TvShowDetailsFragment(id);
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                            .add(R.id.container, tvShowDetailsFragment).addToBackStack(null).commit();
                }

            }
        };
    }
    private void getFavorit() {

        String userId = currentUser.getUid();
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("Favorit").child(userId);
        List<String> itemIds = new ArrayList<>();

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot item : dataSnapshot.getChildren()){
                        String itemId = item.getKey();
                        itemIds.add(itemId);
                        Log.d("item", itemId);
                        loadWatchlist(itemIds);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }


    private void getHistoryFb() {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("History").child(userId);
        List<String> itemIds = new ArrayList<>();

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot item : dataSnapshot.getChildren()){
                    String itemId = item.getKey();
                    itemIds.add(itemId);
                    Log.d("item", itemId);
                    loadLastPlayedMovies(itemIds);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
            }
        });
    }



}