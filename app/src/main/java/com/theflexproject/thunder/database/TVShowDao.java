package com.theflexproject.thunder.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.RawQuery;
import androidx.room.OnConflictStrategy;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;

import java.util.List;

@Dao
public interface TVShowDao {
    @Query("SELECT * FROM TVShow WHERE genres LIKE '%' || :genreId || '%' GROUP BY id")
    List<TVShow> getTvSeriesByGenreId(String genreId);

    @Query("SELECT  * FROM TVShow")
    List<TVShow> getAll();

    @Query("SELECT * FROM TVShow WHERE id IN (:itemIds) GROUP BY id")
    List<TVShow> loadAllByIds(List<String> itemIds);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL GROUP BY id ORDER BY name ASC")
    List<TVShow> getAllByTitles();

    @Query("SELECT * FROM TVShow WHERE TVShow.name LIKE '%' || :string || '%' OR TVShow.overview like '%' || :string || '%' or original_name like '%' || :string || '%' GROUP BY id")
    List<TVShow> getSearchQuery(String string);

    @Query("SELECT * FROM TVShow WHERE id LIKE :id")
    TVShow find(long id);

    @Query("SELECT * FROM TVShow WHERE name LIKE :name")
    TVShow getByShowName(String name);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL GROUP BY id order by last_air_date desc LIMIT :limit OFFSET :offset")
    List<TVShow> getNewShows(int limit, int offset);

    @Query("SELECT * FROM TVShow WHERE backdrop_path IS NOT NULL AND original_language = 'ko' GROUP BY id ORDER BY last_air_date DESC LIMIT :limit OFFSET :offset")
    List<TVShow> getDrakor(int limit, int offset);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL and original_language != 'ko' GROUP BY id order by vote_average desc LIMIT :limit OFFSET :offset")
    List<TVShow> getTopRated(int limit, int offset);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL AND last_air_date>= '2023-01-01' GROUP BY id ORDER BY (popularity + last_air_date) DESC LIMIT :limit OFFSET :offset")
    List<TVShow> getTrending(int limit, int offset);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL AND ( genres IN (SELECT genres FROM TVShow WHERE vote_count > 5000 AND original_language != 'ko')) AND genres IS NOT NULL GROUP BY id ORDER BY vote_count DESC LIMIT :limit OFFSET :offset")
    List<TVShow> getrecomendation(int limit, int offset);

    @Query("Delete FROM TVShow WHERE id = :show_id")
    void deleteById(int show_id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TVShow... tvShows);

    @Delete
    void delete(TVShow tvShow);

    @Query("select * from TVShow where name like :finalShowName ")
    TVShow findByName(String finalShowName);

    @Query("UPDATE TVShow SET add_to_list=1 WHERE id=:tvId")
    void updateAddToList(int tvId);

    @Query("select * from TVShow where add_to_list=1 GROUP BY id")
    List<TVShow> getWatchlisted();

    @Query("UPDATE TVShow SET add_to_list=0 WHERE id=:tvShowId")
    void updateRemoveFromList(int tvShowId);

    @RawQuery
    List<TVShow> getTvSeriesByGenreAndSort(SupportSQLiteQuery query);
}
