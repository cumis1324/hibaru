-- NFGPlus D1 Database Schema
-- Based on Room Database entities

-- Movies Table
CREATE TABLE IF NOT EXISTS movies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tmdb_id INTEGER UNIQUE,
    title TEXT NOT NULL,
    original_title TEXT,
    overview TEXT,
    poster_path TEXT,
    backdrop_path TEXT,
    logo_path TEXT,
    release_date TEXT,
    runtime INTEGER,
    vote_average REAL,
    vote_count INTEGER,
    popularity REAL,
    adult INTEGER DEFAULT 0,
    video INTEGER DEFAULT 0,
    budget INTEGER DEFAULT 0,
    revenue INTEGER DEFAULT 0,
    status TEXT,
    tagline TEXT,
    homepage TEXT,
    imdb_id TEXT,
    original_language TEXT,
    disabled INTEGER DEFAULT 0,
    add_to_list INTEGER DEFAULT 0,
    played INTEGER DEFAULT 0,
    index_id INTEGER DEFAULT 0,
    gd_id TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- TVShows Table
CREATE TABLE IF NOT EXISTS tv_shows (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tmdb_id INTEGER UNIQUE,
    name TEXT NOT NULL,
    original_name TEXT,
    overview TEXT,
    poster_path TEXT,
    backdrop_path TEXT,
    logo_path TEXT,
    first_air_date TEXT,
    last_air_date TEXT,
    number_of_seasons INTEGER DEFAULT 0,
    number_of_episodes INTEGER DEFAULT 0,
    vote_average REAL,
    vote_count INTEGER,
    popularity REAL,
    adult INTEGER DEFAULT 0,
    in_production INTEGER DEFAULT 0,
    status TEXT,
    type TEXT,
    tagline TEXT,
    homepage TEXT,
    original_language TEXT,
    add_to_list INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Episodes Table
CREATE TABLE IF NOT EXISTS episodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tmdb_id INTEGER,
    show_id INTEGER NOT NULL,
    season_number INTEGER NOT NULL,
    episode_number INTEGER NOT NULL,
    name TEXT,
    overview TEXT,
    still_path TEXT,
    air_date TEXT,
    runtime INTEGER,
    vote_average REAL,
    vote_count INTEGER,
    production_code TEXT,
    played INTEGER DEFAULT 0,
    disabled INTEGER DEFAULT 0,
    index_id INTEGER DEFAULT 0,
    gd_id TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES tv_shows(id) ON DELETE CASCADE,
    UNIQUE(show_id, season_number, episode_number)
);

-- Seasons Table
CREATE TABLE IF NOT EXISTS seasons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    show_id INTEGER NOT NULL,
    season_number INTEGER NOT NULL,
    name TEXT,
    overview TEXT,
    poster_path TEXT,
    air_date TEXT,
    episode_count INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES tv_shows(id) ON DELETE CASCADE,
    UNIQUE(show_id, season_number)
);

-- Genres Table (shared by movies and TV shows)
CREATE TABLE IF NOT EXISTS genres (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

-- Movie-Genre relationship
CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- TVShow-Genre relationship
CREATE TABLE IF NOT EXISTS tvshow_genres (
    tvshow_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (tvshow_id, genre_id),
    FOREIGN KEY (tvshow_id) REFERENCES tv_shows(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- Index Links (for external indices)
CREATE TABLE IF NOT EXISTS index_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    link TEXT NOT NULL,
    username TEXT,
    password TEXT,
    index_type TEXT,
    folder_type TEXT,
    disabled INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_movies_tmdb_id ON movies(tmdb_id);
CREATE INDEX IF NOT EXISTS idx_movies_title ON movies(title);
CREATE INDEX IF NOT EXISTS idx_movies_release_date ON movies(release_date);
CREATE INDEX IF NOT EXISTS idx_tvshows_tmdb_id ON tv_shows(tmdb_id);
CREATE INDEX IF NOT EXISTS idx_tvshows_name ON tv_shows(name);
CREATE INDEX IF NOT EXISTS idx_episodes_show_id ON episodes(show_id);
CREATE INDEX IF NOT EXISTS idx_episodes_season ON episodes(show_id, season_number);
CREATE INDEX IF NOT EXISTS idx_seasons_show_id ON seasons(show_id);

-- Insert default genres
INSERT OR IGNORE INTO genres (id, name) VALUES
(28, 'Action'),
(12, 'Adventure'),
(16, 'Animation'),
(35, 'Comedy'),
(80, 'Crime'),
(99, 'Documentary'),
(18, 'Drama'),
(10751, 'Family'),
(14, 'Fantasy'),
(36, 'History'),
(27, 'Horror'),
(10402, 'Music'),
(9648, 'Mystery'),
(10749, 'Romance'),
(878, 'Science Fiction'),
(10770, 'TV Movie'),
(53, 'Thriller'),
(10752, 'War'),
(37, 'Western'),
-- TV Genres
(10759, 'Action & Adventure'),
(10762, 'Kids'),
(10763, 'News'),
(10764, 'Reality'),
(10765, 'Sci-Fi & Fantasy'),
(10766, 'Soap'),
(10767, 'Talk'),
(10768, 'War & Politics');
