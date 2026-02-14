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
                .getrecentreleases(10, 0);

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
                .getFilmIndo(10, 0);

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
                .getOgMovies(10, 0);

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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getTopRated(10, 0);

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

    public static List<Movie> getTrending(Context mActivity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Tugas untuk mendapatkan data dari database
        Callable<List<Movie>> callable = () -> DatabaseClient
                .getInstance(mActivity)
                .getAppDatabase()
                .movieDao()
                .getTrending(10, 0);

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
                .getrecomendation(10, 0);

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

    public static List<Movie> getMore(Context mActivity, List<String> historyId) {
        tmdbTrending movieSimilar = new tmdbTrending();
        List<String> similarId = movieSimilar.getRecommendation(historyId);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        if (similarId == null || similarId.isEmpty()) {
            return new ArrayList<>();
        }

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

    public static List<Movie> getRecombyFav(Context mActivity, List<String> favId) {
        tmdbTrending movieSimilar = new tmdbTrending();

        // Mengambil ID rekomendasi berdasarkan favorit
        List<String> similarId = movieSimilar.getRecommendation(favId);

        // Jika tidak ada ID rekomendasi, kembalikan daftar kosong
        if (similarId == null || similarId.isEmpty()) {
            return new ArrayList<>();
        }

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Set status interrupted
            e.printStackTrace();
            return new ArrayList<>(); // Kembalikan daftar kosong jika terjadi kesalahan
        } catch (ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Kembalikan daftar kosong jika terjadi kesalahan
        } finally {
            // Pastikan executor ditutup
            executor.shutdown();
        }
    }

}
