package com.theflexproject.thunder.utils;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.TVShowInfo.Episode;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;
import com.theflexproject.thunder.model.TVShowInfo.TVShowSeasonDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class DetailsUtils {

    public static Episode getNextEpisode(Context mActivity, int id) {
        // FutureTask yang akan menjalankan Callable dan mengembalikan hasilnya
        FutureTask<Episode> futureTask = new FutureTask<>(new Callable<Episode>() {
            @Override
            public Episode call() {
                return DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .episodeDao()
                        .find(id);
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
    public static TVShowSeasonDetails getSeasonDetails(Context mActivity, int id) {
        // FutureTask yang akan menjalankan Callable dan mengembalikan hasilnya
        FutureTask<TVShowSeasonDetails> futureTask = new FutureTask<>(new Callable<TVShowSeasonDetails>() {
            @Override
            public TVShowSeasonDetails call() {
                return DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowSeasonDetailsDao()
                        .find(id);
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
    public static TVShow getSeriesDetails(Context mActivity, int id) {
        // FutureTask yang akan menjalankan Callable dan mengembalikan hasilnya
        FutureTask<TVShow> futureTask = new FutureTask<>(new Callable<TVShow>() {
            @Override
            public TVShow call() {
                return DatabaseClient
                        .getInstance(mActivity)
                        .getAppDatabase()
                        .tvShowDao()
                        .find(id);
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
    public static List<Movie> getSourceList(Context mActivity, int id) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getAllById(id);

        // Kirim tugas ke executor
        Future<List<Movie>> future = executor.submit(callable);

        try {
            // Tunggu hasilnya
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Kembalikan daftar kosong jika terjadi kesalahan
        } finally {
            // Pastikan executor ditutup
            executor.shutdown();
        }
    }
    public static List<Episode> getEpisodeSource(Context mActivity, int id) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Episode>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .episodeDao()
                .getAllById(id);

        // Kirim tugas ke executor
        Future<List<Episode>> future = executor.submit(callable);

        try {
            // Tunggu hasilnya
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Kembalikan daftar kosong jika terjadi kesalahan
        } finally {
            // Pastikan executor ditutup
            executor.shutdown();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static List<Movie> getSimilarMovies(Context mActivity, int id) {
        tmdbTrending movieSimilar = new tmdbTrending();
        List<String> similarId = movieSimilar.getSimilarMovie(id);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .loadAllByIds(similarId);

        // Kirim tugas ke executor
        Future<List<Movie>> future = executor.submit(callable);

        try {
            // Tunggu hasilnya
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Kembalikan daftar kosong jika terjadi kesalahan
        } finally {
            // Pastikan executor ditutup
            executor.shutdown();
        }
    }
}
