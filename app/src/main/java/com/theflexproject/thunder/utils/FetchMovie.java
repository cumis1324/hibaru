package com.theflexproject.thunder.utils;

import android.content.Context;

import com.theflexproject.thunder.database.DatabaseClient;
import com.theflexproject.thunder.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FetchMovie {
    public static List<Movie> getRecentlyAdded(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getrecentlyadded(10, 0);

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
    public static List<Movie> getRecentRelease(Context mActivity) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getrecentreleases();

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
    public static List<Movie> getSearch(Context mActivity, String query) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getSearchQuery(query);

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
    public static List<Movie> getAllRecentRelease(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getallrecentreleases();

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
    public static List<Movie> getAllRecentAdded(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getallrecentlyadded();

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
    public static List<Movie> getFilmIndo(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getFilmIndo();

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
    public static List<Movie> getOldGold(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getOgMovies();

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
    public static List<Movie> getTopOld(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getTopOld();

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
    public static List<Movie> getTopRated(Context mActivity) {
        tmdbTrending tmdb = new tmdbTrending();
        List<String> topRated = tmdb.getTopRatedMovie();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .loadAllByIds(topRated);

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
    public static List<Movie> getRecommendation(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getrecomendation();

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
    public static List<Movie> getMore(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getMoreMovied();

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
    public static List<Movie> getRecombyFav(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getRecombyfav();

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
