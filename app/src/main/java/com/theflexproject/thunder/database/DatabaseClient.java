package com.theflexproject.thunder.database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {

    private Context mCtx;
    private static DatabaseClient mInstance;

    // our app database object
    private AppDatabase appDatabase;

    private DatabaseClient(Context mCtx) {
        this.mCtx = mCtx;

        // creating the app database with Room database builder
        // Using nfgplus_v4.db to ensure fresh copy from assets with fixed schema
        // (TVShow/Episode patch)
        appDatabase = Room.databaseBuilder(mCtx, AppDatabase.class, "nfgplus_v4.db")
                .createFromAsset("nfgplus.db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized DatabaseClient getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new DatabaseClient(mCtx);
        }
        return mInstance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public void closeDatabase() {
        if (appDatabase.isOpen()) {
            appDatabase.close();
        }
    }
}
