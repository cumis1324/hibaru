
import requests
import argparse
import urllib.parse
import os
import re
import sys
import json
import base64
import time
import datetime
from thefuzz import fuzz, process

# Defaults
DEFAULT_API_URL = "https://nfgplus-backend.worker1-b8f.workers.dev/api"
DEFAULT_TMDB_KEY = "75399494372c92bd800f70079dff476b" # From wrangler.toml

class GDIndexImporter:
    def __init__(self, api_url, admin_key, tmdb_key, gdindex_url, auth=None):
        self.api_url = api_url.rstrip('/')
        self.admin_key = admin_key
        self.tmdb_key = tmdb_key
        self.gdindex_url = gdindex_url.rstrip('/')
        self.auth = auth # (user, pass) tuple or "user:pass" string
        
        self.headers = {
            'X-Admin-Key': self.admin_key,
            'Content-Type': 'application/json'
        }
        
        self.existing_gdids = set()
        self.fetch_existing_gdids()

    def fetch_existing_gdids(self):
        print("Fetching existing GDIDs from database...")
        try:
            res = requests.get(f"{self.api_url}/admin/gdids", headers=self.headers)
            if res.status_code == 200:
                data = res.json()
                count = data.get('count', 0)
                movies = data.get('movies', [])
                episodes = data.get('episodes', [])
                
                self.existing_gdids.update(movies)
                self.existing_gdids.update(episodes)
                print(f"Loaded {len(self.existing_gdids)} existing items (Movies + Episodes).")
            else:
                # If 500, maybe table is empty or API error. Just warn and proceed empty.
                print(f"Warning: Failed to fetch existing GDIDs. Status: {res.status_code}.")
                print(f"Response: {res.text[:500]}") # DEBUG
                print("Proceeding with empty cache.")
        except Exception as e:
            print(f"Error fetching existing GDIDs: {e}. Proceeding with empty cache.")

    def decode_legacy_gdindex_response(self, text):
        """
        Replicates the logic from SendPostRequest.java:
        1. Reverse string
        2. Substring (24 from start, 20 from end)
        3. Base64 Decode
        """
        # Pre-check: If it looks like JSON or HTML, don't try to decode as legacy
            # Strict Java Implementation:
        # 1. strip() (Crucial to match Java's stream reading)
        text = text.strip()
        
        # Check if it's actually a server-side error served as 200 OK
        if "GDIndex Error Handler" in text or "error" in text.lower() and "worker.js" in text.lower():
            # print("WARNING: Server returned a Worker Error/Stack Trace instead of valid data.")
            return None

        try:
            reversed_text = text[::-1]
            
            if len(reversed_text) <= 44:
                return None
            
            # Strategies
            slices = [
                (24, -20),   # Java Gold Standard
                (24, -22),   # Maybe == padding included in prefix count?
                (24, None), 
                (0, None) 
            ]
            
            for start, end in slices:
                try:
                    if end:
                        encoded_string = reversed_text[start:end]
                    else:
                        encoded_string = reversed_text[start:]
                    
                    missing_padding = len(encoded_string) % 4
                    if missing_padding:
                        encoded_string += '=' * (4 - missing_padding)

                    try:
                        decoded_bytes = base64.b64decode(encoded_string, validate=False) # validate=False for leniency
                    except Exception as b64e:
                        print(f"DEBUG: B64 Error {start}:{end}: {b64e}")
                        # Try urlsafe as fallback
                        decoded_bytes = base64.urlsafe_b64decode(encoded_string)

                    decoded_str = decoded_bytes.decode('utf-8')
                    return json.loads(decoded_str)

                except Exception:
                    continue
            
            return None 
        except Exception:
             return None

    def fetch_gdindex_folder(self, folder_url, page_token="", page_index=0):
        print(f"Scanning: {folder_url}")
        
        # Determine strict or legacy mode (heuristic)
        # Try both concurrently or sequentially.
        
        # 1. Try GoIndex/Theme approach (JSON POST)
        # Java sends: { "q":"","password": null , "page_index": pageIndex }
        payload = {"page_token": page_token, "page_index": page_index}
        payload['password'] = None
        payload['q'] = "" # Crucial for listing files!
        
        # Strategy 1: Try JSON (GoIndex/Theme)
        try:
            # print(f"DEBUG: Posting JSON to {folder_url}")
            res = requests.post(folder_url, json=payload, auth=self.auth)
            
            try:
                data = res.json()
                if 'data' in data and 'files' in data['data']:
                    return data
                if 'files' in data: 
                    return {'data': data}
            except:
                pass
                
            decoded = self.decode_legacy_gdindex_response(res.text)
            if decoded and 'data' in decoded:
                 return decoded

        except Exception as e:
            print(f"DEBUG: JSON Strategy failed: {e}")

        # Strategy 2: Try Form Encoded (Classic GDIndex)
        # Replicating SendPostRequest.java::postRequestGDIndex EXACTLY
        try:
            # Java: params.put("authorization", authHeaderValue);
            # Java: params.put("page_token", nextPageToken);
            # Java: params.put("page_index", pageIndex);
            
            # Construct Auth Header Value manually to match Java's "Basic ..."
            user, password = self.auth if self.auth else ("", "")
            auth_str = f"{user}:{password}"
            auth_b64 = base64.b64encode(auth_str.encode('utf-8')).decode('utf-8')
            auth_header_val = f"Basic {auth_b64}"
            
            form_payload = {
                "authorization": auth_header_val,
                "page_token": payload.get('page_token', ""),
                "page_index": payload.get('page_index', 0)
            }
            # Note: Java does NOT send 'password' param in this mode.
            
            # print(f"DEBUG: Posting Form-Data to {folder_url} with keys {list(form_payload.keys())}")
            
            # Note: Java sends 'authorization' header AND 'authorization' body param.
            # requests 'auth=' param handles the Header. form_payload handles the Body.
            res = requests.post(folder_url, data=form_payload, auth=self.auth)
            
            # Check for legacy encrypted response in body (most likely for Classic GDIndex)
            decoded = self.decode_legacy_gdindex_response(res.text)
            if decoded and 'data' in decoded:
                 return decoded
                 
            # Check for plain JSON (rare but possible)
            try:
                data = res.json()
                if 'data' in data and 'files' in data['data']:
                    return data
            except:
                pass
            
            # Handle 200 OK but unparsable
            if res.status_code == 200:
                print(f"DEBUG: Status 200 but could not parse.")
                print(f"DEBUG: Content-Type: {res.headers.get('Content-Type')}")
                print(f"DEBUG: Response Preview: {res.text[:500]}") # Show more to identify format
                return {'data': {'files': []}}

            print(f"DEBUG: Failed to parse (Both strategies). Status: {res.status_code}")
            if res.status_code != 200:
                print(f"DEBUG: Response Preview: {res.text[:200]}")

        except Exception as e:
            print(f"Error fetching {folder_url}: {e}")
            
        print(f"Warning: Could not parse content from {folder_url}")
        return None

    def search_tmdb(self, query, year=None, is_tv=False):
        url = f"https://api.themoviedb.org/3/search/{'tv' if is_tv else 'movie'}"
        params = {
            'api_key': self.tmdb_key,
            'query': query,
            'language': 'en-US'
        }
        if year and not is_tv:
            params['year'] = year
        if year and is_tv:
            params['first_air_date_year'] = year
            
        res = requests.get(url, params=params)
        if res.status_code == 200:
            results = res.json().get('results', [])
            if not results:
                return None
                
            # Fuzzy Logic Selection (Replicates Java FuzzySearch)
            # Java logic: if match == 100, take it. else if > 70, take it. else first?
            # We use process.extractOne
            
            choices = {i: (r.get('title') if not is_tv else r.get('name')) for i, r in enumerate(results)}
            
            # Fuzzy match query against titles
            best_match = process.extractOne(query, choices)
            # best_match is ((title), score, index)
            
            if best_match:
                score = best_match[1]
                idx = best_match[2]
                if score > 70:
                    return results[idx]
            
            # Fallback to first result if no good fuzzy match? 
            # Java code fallback behavior: 
            # if (matchedTvTitle.getScore() == 100) ... else if (score > 70) ...
            # it implies if < 70 it does NOT return? 
            # Actually Java code: `tvShowId = tvShowsResponseFromTMDB.results.get(finalIndex).getId();` 
            # where finalIndex=0 default. So it falls back to first result.
            return results[0]

        return None

    def get_tmdb_details(self, tmdb_id, is_tv=False):
        url = f"https://api.themoviedb.org/3/{'tv' if is_tv else 'movie'}/{tmdb_id}"
        params = {'api_key': self.tmdb_key, 'language': 'en-US'}
        res = requests.get(url, params=params)
        return res.json() if res.status_code == 200 else None

    def check_exists(self, gd_id, is_tv=False):
        if not gd_id:
            return False
            
        # Check cached set first
        if gd_id in self.existing_gdids:
            return True
            
        # Fallback to API check (Optional, but if cache is loaded, we can trust it)
        # For robustness, we can update cache after insert.
        return False

    def import_movie(self, file_data, parsed_info, index_url):
        gd_id = file_data.get('id')
        print(f"  [Movie] {parsed_info['title']} ({parsed_info['year'] or '?'})")
        
        # 1. Check if exists using gd_id
        if self.check_exists(gd_id, is_tv=False):
            print(f"    -> Skipped (Already Exists: {gd_id})")
            return

        tmdb_result = self.search_tmdb(parsed_info['title'], parsed_info['year'], is_tv=False)
        if not tmdb_result:
            print("    -> No TMDB match found.")
            return

        detail = self.get_tmdb_details(tmdb_result['id'], is_tv=False)
        if not detail:
            return

        # Prepare Payload
        clean_index_url = index_url.rstrip('/')
        full_url = ""
        if 'url' in file_data:
            full_url = file_data['url'] 
        else:
             full_url = f"{clean_index_url}/{urllib.parse.quote(file_data['name'])}"

        payload = {
            "tmdb_id": detail['id'],
            "title": detail['title'],
            "original_title": detail.get('original_title', ""),
            "overview": detail.get('overview', ""),
            "poster_path": detail.get('poster_path', ""),
            "backdrop_path": detail.get('backdrop_path', ""),
            "logo_path": "", 
            "release_date": detail.get('release_date', ""),
            "runtime": detail.get('runtime', 0),
            "vote_average": detail.get('vote_average', 0.0),
            "vote_count": detail.get('vote_count', 0),
            "popularity": detail.get('popularity', 0.0),
            "adult": detail.get('adult', False),
            "status": detail.get('status', ""),
            "original_language": detail.get('original_language', ""),
            "imdb_id": detail.get('imdb_id', ""),
            "homepage": detail.get('homepage', ""),
            "tagline": detail.get('tagline', ""),
            # File info
            "url_string": full_url,
            "file_name": file_data['name'],
            "mime_type": file_data.get('mimeType', ""),
            "size": str(file_data.get('size', "")),
            "modified_time": self._iso_to_ms(file_data.get('modifiedTime', "")),
            "gd_id": gd_id
        }
        # print(f"DEBUG: Raw modifiedTime for {file_data['name']}: {file_data.get('modifiedTime', 'N/A')}")
        
        res = requests.post(f"{self.api_url}/movies", json=payload, headers=self.headers)
        if res.status_code in [200, 201]:
            print(f"    -> Synced: {detail['title']}")
            if gd_id: self.existing_gdids.add(gd_id) # Update cache
        else:
            print(f"    -> Fail: {res.text}")

    def _iso_to_ms(self, iso_string):
        if not iso_string:
            return ""
        try:
            # Check if already digits (legacy safe)
            if str(iso_string).isdigit():
                return str(iso_string)
            
            # Parse ISO string (e.g., 2026-02-12T01:47:29.766Z)
            # safe replacement for Z -> +00:00
            iso_string = iso_string.replace('Z', '+00:00')
            dt = datetime.datetime.fromisoformat(iso_string)
            return str(int(dt.timestamp() * 1000))
        except Exception as e:
            return str(iso_string)



    def import_episode(self, file_data, parsed_info, index_url):
        gd_id = file_data.get('id')
        print(f"  [Episode] {parsed_info['title']} S{parsed_info['season']}E{parsed_info['episode']}")
        
        # 1. Check if exists using gd_id (Episode level)
        if self.check_exists(gd_id, is_tv=True):
             print(f"    -> Skipped (Already Exists: {gd_id})")
             return
        
        # 2. Search TV Show
        tv_search = self.search_tmdb(parsed_info['title'], year=parsed_info.get('year'), is_tv=True)
        if not tv_search:
             print("    -> TV Show Not Found")
             return

        tv_detail = self.get_tmdb_details(tv_search['id'], is_tv=True)
        # 3. Sync TV Show (Upsert)
        tv_payload = {
            "tmdb_id": tv_detail['id'],
            "name": tv_detail['name'],
            "overview": tv_detail.get('overview', ""),
            "poster_path": tv_detail.get('poster_path', ""),
            "backdrop_path": tv_detail.get('backdrop_path', ""),
            "vote_average": tv_detail.get('vote_average', 0.0),
            "first_air_date": tv_detail.get('first_air_date', ""),
            "last_air_date": tv_detail.get('last_air_date', ""),
            "number_of_seasons": tv_detail.get('number_of_seasons', 0),
            "number_of_episodes": tv_detail.get('number_of_episodes', 0),
            "status": tv_detail.get('status', ""),
            "type": tv_detail.get('type', ""),
            "logo_path": ""
        }
        res_tv = requests.post(f"{self.api_url}/tvshows", json=tv_payload, headers=self.headers)
        if res_tv.status_code not in [200, 201]:
             print(f"    -> TV Sync Failed: {res_tv.text}")
             return
        
        show_db_id = res_tv.json().get('id')

        # 4. Sync Season (Upsert)
        # Schema uses TMDB ID for Foreign Key, not Internal ID
        season_payload = {
            "show_id": tv_detail['id'],
            "season_number": parsed_info['season'],
            "name": f"Season {parsed_info['season']}",
            "overview": "",
            "poster_path": "", 
            "air_date": "",
            "episode_count": 0
        }
        res_season = requests.post(f"{self.api_url}/seasons", json=season_payload, headers=self.headers)
        if res_season.status_code not in [200, 201]:
             print(f"    -> Season Sync Failed: {res_season.text}")
        
        # 5. Get Episode Details from TMDB
        # GET /tv/{id}/season/{season}/episode/{episode}
        ep_url = f"https://api.themoviedb.org/3/tv/{tv_detail['id']}/season/{parsed_info['season']}/episode/{parsed_info['episode']}"
        ep_res = requests.get(ep_url, params={'api_key': self.tmdb_key})
        if ep_res.status_code == 404:
            print(f"    -> Skipped (TMDB 404): Episode S{parsed_info['season']}E{parsed_info['episode']} not found for Show ID {tv_detail['id']}")
            return

        ep_detail = ep_res.json() if ep_res.status_code == 200 else {}
        
        # Fix Double Slash Issue
        # index_url might end with /, and clean_name might be handled by quote
        # Best way: strip slash from base, quote name, join with single slash
        clean_base_url = index_url.rstrip('/')
        full_url = ""
        if 'url' in file_data:
            full_url = file_data['url']
        else:
             full_url = f"{clean_base_url}/{urllib.parse.quote(file_data['name'])}"
        
        ep_payload = {
            "show_id": tv_detail['id'],
            "season_number": parsed_info['season'],
            "episode_number": parsed_info['episode'],
            "tmdb_id": ep_detail.get('id'), # ADDED TMDB ID
            "name": ep_detail.get('name', file_data['name']),
            "overview": ep_detail.get('overview', ""),
            "still_path": ep_detail.get('still_path', ""),
            "air_date": ep_detail.get('air_date', ""),
            "vote_average": ep_detail.get('vote_average', 0.0),
            "url_string": full_url,
            "file_name": file_data['name'],
            "mime_type": file_data.get('mimeType', ""),
            "size": str(file_data.get('size', "")),
            "modified_time": self._iso_to_ms(file_data.get('modifiedTime', "")),
            "gd_id": gd_id
        }
        
        res_ep = requests.post(f"{self.api_url}/episodes", json=ep_payload, headers=self.headers)
        if res_ep.status_code in [200, 201]:
            print(f"    -> Synced Episode: S{parsed_info['season']}E{parsed_info['episode']}")
            if gd_id: self.existing_gdids.add(gd_id) # Update cache
        else:
            print(f"    -> Episode Sync Fail: {res_ep.text}")

    def parse_filename(self, filename, force_type=None, parent_folder_name=None):
        # Clean up "Copy of", etc.
        filename = filename.replace("Copy of ", "").strip()
        name_no_ext = os.path.splitext(filename)[0]
        
        # Regex setup
        # Standard S01E01 or 1x01
        tv_pattern = re.compile(r'(.+?)[ .]?[Ss](\d{1,2})[ .]?[Ee](\d{1,2})', re.IGNORECASE)
        # Pattern for just SxxExx without title (common in subfolders)
        tv_simple_pattern = re.compile(r'^[ .]?[Ss](\d{1,2})[ .]?[Ee](\d{1,2})', re.IGNORECASE)
        
        tv_match = tv_pattern.search(name_no_ext)
        tv_simple_match = tv_simple_pattern.search(name_no_ext)
        
        # Function to return TV result
        def return_tv(title, s, e, year=None):
            return {
                 'type': 'episode',
                 'title': title,
                 'season': int(s),
                 'episode': int(e),
                 'year': year
             }

        # Function to return Movie result
        def return_movie(title, year):
            return {
                'type': 'movie',
                'title': title,
                'year': year
            }

        if force_type == 'tv':
            # Priority: Parent Folder with Year (e.g. "Breathless (2024)") > Filename Title
            if parent_folder_name and re.search(r'[\(\.]\d{4}[\)\.]', parent_folder_name) and "Season" not in parent_folder_name:
                print(f"DEBUG: Using parent folder '{parent_folder_name}' (found year) over filename title")
                
                # Extract year
                year_match = re.search(r'[\(\.](\d{4})[\)\.]', parent_folder_name)
                year = year_match.group(1) if year_match else None
                
                # Clean title (Remove year and brackets)
                clean_title = parent_folder_name
                if year:
                    clean_title = clean_title.replace(f"({year})", "").replace(f".{year}.", "").strip()
                
                if tv_match:
                    return return_tv(clean_title, tv_match.group(2), tv_match.group(3), year)
                elif tv_simple_match:
                    return return_tv(clean_title, tv_simple_match.group(1), tv_simple_match.group(2), year)

            if tv_match:
                return return_tv(tv_match.group(1).replace('.', ' ').strip(), tv_match.group(2), tv_match.group(3))
            elif tv_simple_match and parent_folder_name:
                 # Use parent folder name as title
                 print(f"DEBUG: Using parent folder '{parent_folder_name}' for title of {filename}")
                 return return_tv(parent_folder_name, tv_simple_match.group(1), tv_simple_match.group(2))
            else:
                 pass

        if force_type == 'movie':
             movie_pattern = re.compile(r'(.+?)[ .\(](\d{4})[ .\)]', re.IGNORECASE)
             movie_match = movie_pattern.search(name_no_ext)
             if movie_match:
                 return return_movie(movie_match.group(1).replace('.', ' ').strip(), movie_match.group(2))
             else:
                 return return_movie(name_no_ext.replace('.', ' ').strip(), None)

        # AUTO MODE (Default)
        if tv_match:
             return return_tv(tv_match.group(1).replace('.', ' ').strip(), tv_match.group(2), tv_match.group(3))
        elif tv_simple_match and parent_folder_name:
             # Fallback for Auto mode: if looks like SxxExx and has parent folder, assume TV
             print(f"DEBUG: Auto-detected TV using parent '{parent_folder_name}' for {filename}")
             return return_tv(parent_folder_name, tv_simple_match.group(1), tv_simple_match.group(2))
             
        # Movie Pattern
        movie_pattern = re.compile(r'(.+?)[ .\(](\d{4})[ .\)]', re.IGNORECASE)
        movie_match = movie_pattern.search(name_no_ext)
        
        if movie_match:
            return return_movie(movie_match.group(1).replace('.', ' ').strip(), movie_match.group(2))
            
        return return_movie(name_no_ext.replace('.', ' ').strip(), None)

    def crawl_folder(self, url, recursive=True, force_type=None, current_folder_name=None):
        page_token = ""
        page_index = 0
        
        while True:
            # print(f"Fetching page {page_index} token={page_token}")
            data = self.fetch_gdindex_folder(url, page_token, page_index)
            if not data:
                break
                
            files = data.get('data', {}).get('files', [])
            next_token = data.get('nextPageToken')
            
            for f in files:
                mime = f.get('mimeType', '')
                if mime == 'application/vnd.google-apps.folder':
                    if recursive:
                        subfolder_name = f['name']
                        if url.endswith('/'):
                             sub_url = url + urllib.parse.quote(subfolder_name)
                        else:
                             sub_url = url + '/' + urllib.parse.quote(subfolder_name)
                        
                        # FORCE TRAILING SLASH for GDIndex to treat it as a folder listing
                        if not sub_url.endswith('/'):
                            sub_url += '/'
                        
                        # Clean up potential double slashes from URL construction
                        sub_url = sub_url.replace("://", "###").replace("//", "/").replace("###", "://")

                        self.crawl_folder(sub_url, recursive=True, force_type=force_type, current_folder_name=subfolder_name)
                    else:
                        print(f"Skipping subfolder: {f['name']}")
                
                elif 'video' in mime or mime == 'application/octet-stream':
                    parsed = self.parse_filename(f['name'], force_type=force_type, parent_folder_name=current_folder_name)
                    
                    if parsed['type'] == 'movie':
                        self.import_movie(f, parsed, url)
                    elif parsed['type'] == 'episode':
                        self.import_episode(f, parsed, url)
            
            if next_token:
                page_token = next_token
                page_index += 1
            else:
                break

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Import content from GDIndex to D1")
    parser.add_argument("--url", required=True, help="Root folder URL to scan")
    parser.add_argument("--admin-key", required=True, help="Worker Admin API Key")
    parser.add_argument("--user", help="Basic Auth User")
    parser.add_argument("--password", help="Basic Auth Password")
    parser.add_argument("--tmdb-key", help="TMDB API Key (Optional)")
    parser.add_argument("--no-recursive", action="store_true", help="Disable recursive scanning")
    parser.add_argument("--type", choices=['auto', 'movie', 'tv'], default='auto', help="Force folder content type (default: auto)")
    
    args = parser.parse_args()
    
    # Auth parsing
    auth = None
    if args.user and args.password:
        auth = (args.user, args.password)
        
    importer = GDIndexImporter(
        api_url=DEFAULT_API_URL,
        admin_key=args.admin_key,
        tmdb_key=args.tmdb_key or DEFAULT_TMDB_KEY,
        gdindex_url=args.url,
        auth=auth
    )
    
    root_folder_name = urllib.parse.unquote(args.url.rstrip('/').split('/')[-1])
    print(f"Starting crawl on: {args.url} [Type: {args.type}] (Root: {root_folder_name})")
    importer.crawl_folder(args.url, recursive=not args.no_recursive, force_type=None if args.type == 'auto' else args.type, current_folder_name=root_folder_name)
