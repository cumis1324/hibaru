package com.theflexproject.thunder.di

import android.content.Context
import androidx.room.Room
import com.theflexproject.thunder.database.AppDatabase
import com.theflexproject.thunder.database.EpisodeDao
import com.theflexproject.thunder.database.IndexLinksDao
import com.theflexproject.thunder.database.MovieDao
import com.theflexproject.thunder.database.TVShowDao
import com.theflexproject.thunder.database.TVShowSeasonDetailsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nfgplus.db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }
    
    @Provides
    @Singleton
    fun provideTVShowDao(database: AppDatabase): TVShowDao {
        return database.tvShowDao()
    }
    
    @Provides
    @Singleton
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }
    
    @Provides
    @Singleton
    fun provideTVShowSeasonDetailsDao(database: AppDatabase): TVShowSeasonDetailsDao {
        return database.tvShowSeasonDetailsDao()
    }
    
    @Provides
    @Singleton
    fun provideIndexLinksDao(database: AppDatabase): IndexLinksDao {
        return database.indexLinksDao()
    }
}
