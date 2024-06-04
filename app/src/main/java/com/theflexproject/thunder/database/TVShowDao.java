package com.theflexproject.thunder.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.TVShowInfo.TVShow;

import java.util.List;

@Dao
public interface TVShowDao {
    @Query("SELECT  * FROM TVShow")
    List<TVShow> getAll();

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL GROUP BY name ORDER BY name ASC")
    List<TVShow> getAllByTitles();

    @Query("SELECT * FROM TVShow WHERE TVShow.name LIKE '%' || :string || '%' OR TVShow.overview like '%' || :string || '%' GROUP BY name")
    List<TVShow> getSearchQuery(String string);

    @Query("SELECT * FROM TVShow WHERE id LIKE :id")
    TVShow find(long id);

    @Query("SELECT * FROM TVShow WHERE name LIKE :name")
    TVShow getByShowName(String name);

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL GROUP BY name order by last_air_date desc")
    List<TVShow> getNewShows();
    @Query("SELECT * FROM TVShow WHERE backdrop_path IS NOT NULL AND original_language = 'ko' GROUP BY name ORDER BY last_air_date DESC")
    List<TVShow> getDrakor();

    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL and original_language != 'ko' GROUP BY name order by vote_average desc")
    List<TVShow> getTopRated();
    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL AND last_air_date>= '2023-01-01' GROUP BY name ORDER BY (popularity + last_air_date) DESC LIMIT 10")
    List<TVShow> getTrending();
    @Query("SELECT * FROM TVShow WHERE poster_path IS NOT NULL AND ( genres IN (SELECT genres FROM TVShow WHERE vote_count > 5000 AND original_language != 'ko')) AND genres IS NOT NULL GROUP BY name ORDER BY vote_count DESC")
    List<TVShow> getrecomendation();


    @Query("Delete FROM TVShow WHERE id = :show_id")
    void deleteById(int show_id);

    @Insert
    void insert(TVShow... tvShows);

    @Delete
    void delete(TVShow tvShow);


    @Query("select * from TVShow where name like :finalShowName ")
    TVShow findByName(String finalShowName);

    @Query("UPDATE TVShow SET addToList=1 WHERE id=:tvId")
    void updateAddToList(int tvId);

    @Query("select * from TVShow where addToList=1 GROUP BY name")
    List<TVShow> getWatchlisted();

    @Query("UPDATE TVShow SET addToList=0 WHERE id=:tvShowId")
    void updateRemoveFromList(int tvShowId);
}
