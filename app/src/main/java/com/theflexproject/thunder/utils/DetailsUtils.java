package com.theflexproject.thunder.utils;

import android.content.Context;
import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DetailsUtils {

    public static Movie getMovieDetails(Context mActivity, int id) {
        // FutureTask yang akan menjalankan Callable dan mengembalikan hasilnya
        FutureTask<Movie> futureTask = new FutureTask<>(new Callable<Movie>() {
            @Override
            public Movie call() {
                return DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .byId(id);
            }
        });

        // Menjalankan FutureTask di dalam thread baru
        Thread thread = new Thread(futureTask);
        thread.start();

        try {
            // Mendapatkan hasil dari FutureTask setelah thread selesai
            return futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    public static Movie getMovieSmallest(Context mActivity, int id) {
        // FutureTask yang akan menjalankan Callable dan mengembalikan hasilnya
        FutureTask<Movie> futureTask = new FutureTask<>(new Callable<Movie>() {
            @Override
            public Movie call() {
                return DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .movieDao()
                        .byIdSmallest(id);
            }
        });

        // Menjalankan FutureTask di dalam thread baru
        Thread thread = new Thread(futureTask);
        thread.start();

        try {
            // Mendapatkan hasil dari FutureTask setelah thread selesai
            return futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
