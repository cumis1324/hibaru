package com.theflexproject.thunder.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.theflexproject.thunder.model.Movie;

import java.util.List;

@Dao
public interface MovieDao {
    @Query("SELECT * FROM Movie WHERE genres LIKE '%' || :genreId || '%' and disabled=0 GROUP BY id")
    List<Movie> getMoviesByGenre(String genreId);



    @Query("SELECT * FROM Movie WHERE title is not null and disabled=0 GROUP BY id ")
    List<Movie> getAll();

    @Query("SELECT * FROM Movie WHERE id=:id and disabled=0")
    Movie byId(int id);

    @Query("SELECT * FROM Movie WHERE id IN (:itemIds) and disabled=0 GROUP BY id")
    List<Movie> loadAllByIds(List<String> itemIds);
    @Query("SELECT * FROM Movie WHERE id=:id and disabled=0")
    List<Movie> getAllById(int id);
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND (played IS NOT NULL AND genres IN (SELECT genres FROM Movie WHERE id=:id)) AND genres IS NOT NULL GROUP BY id ORDER BY popularity DESC")
    List<Movie> getmorebyid(int id);

    @Query("SELECT * FROM Movie WHERE id=:id and disabled=0 ORDER BY size DESC limit 1 ")
    Movie byIdLargest(int id);

    @Query("SELECT * FROM Movie WHERE (fileName LIKE '%' || :string || '%' OR title like '%' || :string || '%' OR urlString like '%' || :string || '%' or overview like '%' || :string || '%' or original_title like '%' || :string || '%') and disabled=0 GROUP BY id")
    List<Movie> getSearchQuery(String string);

    @Query("SELECT * FROM Movie WHERE fileName LIKE :fileName and disabled=0")
    Movie getByFileName(String fileName);

    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND vote_count > 5000 GROUP BY id ORDER BY release_date")
    List<Movie> getTopRated();

    @Query("SELECT * from Movie  WHERE backdrop_path IS NOT NULL and disabled=0 GROUP BY id ORDER BY modifiedTime DESC")
    List<Movie> getrecentlyadded();

    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL and disabled=0 GROUP BY id ORDER BY release_date DESC")
    List<Movie> getrecentreleases();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND release_date >= '2023-01-01' GROUP BY id ORDER BY (popularity + release_date) DESC LIMIT 10")
    List<Movie> getTrending();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL and disabled=0 GROUP BY id ORDER BY  genres Limit 10")
    List<Movie> getgenres();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND (played IS NOT NULL AND genres IN (SELECT genres FROM Movie WHERE vote_count > 5000)) AND genres IS NOT NULL GROUP BY id ORDER BY vote_count DESC")
    List<Movie> getrecomendation();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND (played IS NOT NULL AND genres IN (SELECT genres FROM Movie WHERE vote_count > 5000)) AND release_date < '2011-01-01' AND release_date IS NOT NULL GROUP BY id ORDER BY release_date ASC")
    List<Movie> getOgMovies();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND (played IS NOT NULL AND genres IN (SELECT genres FROM Movie WHERE played = 1)) AND genres IS NOT NULL GROUP BY id ORDER BY popularity DESC")
    List<Movie> getMoreMovied();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND vote_count > 5000 GROUP BY id ORDER BY release_date ASC LIMIT 10")
    List<Movie> getTopOld();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND (played IS NOT NULL AND genres IN (SELECT genres FROM Movie WHERE addToList = 1)) AND genres IS NOT NULL GROUP BY id ORDER BY popularity DESC")
    List<Movie> getRecombyfav();
    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND disabled = 0 AND original_language = 'id' GROUP BY id ORDER BY release_date DESC")
    List<Movie> getFilmIndo();





    @Insert
    void insert(Movie... movies);

    @Delete
    void delete(Movie movie);

    @Query("Delete from Movie where index_id = :index_id")
    void deleteAllFromthisIndex(int index_id);

    @Query("SELECT * FROM Movie WHERE poster_path IS NOT NULL AND played LIKE '% added' AND disabled = 0 GROUP BY id ORDER BY played DESC")
    List<Movie> getPlayed();


    @Query("Update Movie set played =:dateTime where id =:id and disabled=0")
    void updatePlayed(int id, String dateTime);

    @Query("SELECT * FROM Movie WHERE title is NULL and disabled=0")
    List<Movie> getFilesWithNoTitle();

    @Query("SELECT * FROM Movie WHERE fileName LIKE :fileName and disabled=0")
    List<Movie> getAllByFileName(String fileName);


    @Query("SELECT COUNT (title) FROM Movie WHERE index_id =:index_id")
    int getNoOfMovies(int index_id);


    @Query("UPDATE Movie set disabled=1 WHERE index_id=:index_id ")
    void disableFromThisIndex(int index_id);

    @Query("Update movie set disabled = 0 where index_id=:index_id")
    void enableFromThisIndex(int index_id);

    @Query("SELECT * FROM Movie WHERE gd_id =:gd_id")
    Movie getByGdId(String gd_id);

    @Query("DELETE FROM Movie WHERE gd_id =:id")
    void deleteByGdId(String id);

    @Query("UPDATE Movie SET addToList=1 WHERE id=:movieId")
    void updateAddToList(int movieId);

    @Query("SELECT * FROM Movie WHERE addToList=1 and disabled=0 GROUP BY id")
    List<Movie> getWatchlisted();

    @Query("UPDATE Movie SET addToList=0 WHERE id=:movieId")
    void updateRemoveFromList(int movieId);
}
