import sqlite3
import os
import re

# Paths
SRC_DB = "../nfg2.db"
DST_DB = "../../app/src/main/assets/nfgplus.db"

def fix_doubleslash(text):
    if not text:
        return text
    # Replace any // in the path part, but keep protocol://
    # A simple robust way: split by protocol, fix path, rejoin
    if "://" in text:
        parts = text.split("://", 1)
        protocol = parts[0]
        rest = parts[1]
        # remove double slashes in 'rest'
        rest = rest.replace("//", "/")
        return f"{protocol}://{rest}"
    else:
        return text.replace("//", "/")

def migrate():
    print(f"Connecting to Source: {SRC_DB}")
    src_conn = sqlite3.connect(SRC_DB)
    src_conn.row_factory = sqlite3.Row
    src_cur = src_conn.cursor()

    print(f"Connecting to Dest: {DST_DB}")
    dst_conn = sqlite3.connect(DST_DB)
    dst_cur = dst_conn.cursor()

    # Clear destination tables
    print("Clearing destination tables...")
    dst_cur.execute("DELETE FROM Movie")
    dst_cur.execute("DELETE FROM Episode")
    dst_conn.commit()

    # --- Migrate Movies ---
    print("Migrating Movies...")
    src_cur.execute("SELECT * FROM Movie")
    movies = src_cur.fetchall()
    
    movie_count = 0
    for row in movies:
        # Map columns
        fileidForDB = row['fileidForDB'] # map to fileidForDB
        file_name = row['fileName']
        mime_type = row['mimeType']
        modified_time = row['modifiedTime']
        size = row['size']
        url_string = fix_doubleslash(row['urlString']) # FIX
        gd_id = fix_doubleslash(row['gd_id']) # FIX (just in case)
        logo_path = row['logo_path']
        index_id = row['index_id']
        add_to_list = row['addToList']
        disabled = row['disabled']
        played = str(row['Played']) if row['Played'] else None # Convert int to string/null
        adult = row['adult']
        backdrop_path = row['backdrop_path']
        budget = row['budget']
        genres = row['genres']
        homepage = row['homepage']
        id = row['id']
        imdb_id = row['imdb_id']
        original_language = row['original_language']
        original_title = row['original_title']
        overview = row['overview']
        popularity = row['popularity']
        poster_path = row['poster_path']
        release_date = row['release_date']
        revenue = row['revenue']
        runtime = row['runtime']
        status = row['status']
        tagline = row['tagline']
        title = row['title']
        video = row['video']
        vote_average = row['vote_average']
        vote_count = row['vote_count']

        dst_cur.execute("""
            INSERT INTO Movie (
                fileidForDB, file_name, mime_type, modified_time, size, url_string, gd_id,
                logo_path, index_id, add_to_list, disabled, played, adult, backdrop_path,
                budget, genres, homepage, id, imdb_id, original_language, original_title,
                overview, popularity, poster_path, release_date, revenue, runtime, status,
                tagline, title, video, vote_average, vote_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            fileidForDB, file_name, mime_type, modified_time, size, url_string, gd_id,
            logo_path, index_id, add_to_list, disabled, played, adult, backdrop_path,
            budget, genres, homepage, id, imdb_id, original_language, original_title,
            overview, popularity, poster_path, release_date, revenue, runtime, status,
            tagline, title, video, vote_average, vote_count
        ))
        movie_count += 1
    
    print(f"Migrated {movie_count} Movies.")

    # --- Migrate Episodes ---
    print("Migrating Episodes...")
    src_cur.execute("SELECT * FROM Episode")
    episodes = src_cur.fetchall()

    episode_count = 0
    seen_gd_ids = set()
    skipped_count = 0

    # Pre-populate seen_gd_ids from Movie table (if they share ID space? Unlikely but safe)
    # Actually, gd_ids are unique per table usually.
    
    for row in episodes:
        gd_id_raw = row['gd_id']
        gd_id = fix_doubleslash(gd_id_raw)
        
        if gd_id in seen_gd_ids:
            skipped_count += 1
            continue
        
        seen_gd_ids.add(gd_id)

        # Map columns
        idForDB = row['idForDB']
        file_name = row['fileName']
        mime_type = row['mimeType']
        modified_time = row['modifiedTime']
        size = row['size']
        url_string = fix_doubleslash(row['urlString']) # FIX
        index_id = row['index_id']
        disabled = row['disabled']
        # gd_id already fixed
        played = str(row['Played']) if row['Played'] else None
        season_id = row['season_id']
        air_date = row['air_date']
        episode_number = row['episode_number']
        id = row['id']
        name = row['name']
        overview = row['overview']
        production_code = row['production_code']
        runtime = row['runtime']
        season_number = row['season_number']
        show_id = row['show_id']
        still_path = row['still_path']
        vote_average = row['vote_average']
        vote_count = row['vote_count']

        dst_cur.execute("""
            INSERT INTO Episode (
                idForDB, file_name, mime_type, modified_time, size, url_string, index_id,
                disabled, gd_id, played, season_id, air_date, episode_number, id, name,
                overview, production_code, runtime, season_number, show_id, still_path,
                vote_average, vote_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            idForDB, file_name, mime_type, modified_time, size, url_string, index_id,
            disabled, gd_id, played, season_id, air_date, episode_number, id, name,
            overview, production_code, runtime, season_number, show_id, still_path,
            vote_average, vote_count
        ))
        episode_count += 1

    print(f"Migrated {episode_count} Episodes. Skipped {skipped_count} duplicates.")

    # --- Finalize ---
    print("Setting version to 32 and creating indices...")
    dst_cur.execute("PRAGMA user_version = 32")
    # Drop first to ensure check
    dst_cur.execute("DROP INDEX IF EXISTS index_Movie_gd_id")
    dst_cur.execute("CREATE UNIQUE INDEX index_Movie_gd_id ON Movie(gd_id)")
    dst_cur.execute("DROP INDEX IF EXISTS index_Episode_gd_id")
    dst_cur.execute("CREATE UNIQUE INDEX index_Episode_gd_id ON Episode(gd_id)")
    
    dst_conn.commit()
    src_conn.close()
    dst_conn.close()
    print("Done!")

if __name__ == "__main__":
    migrate()
