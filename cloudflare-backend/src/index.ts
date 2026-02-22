/**
 * NFGPlus Cloudflare Workers API
 * REST API for NFGPlus Android App
 */

export interface Env {
    DB: D1Database;
    DEMO_DB: D1Database;
    ALLOWED_ORIGINS: string;
    ADMIN_API_KEY: string;
}

// Helper to select database based on header
function getDatabase(request: Request, env: Env): D1Database {
    const isDemo = request.headers.get('X-Demo-Mode') === 'true';
    return isDemo ? env.DEMO_DB : env.DB;
}

// CORS headers helper
function corsHeaders(origin: string) {
    return {
        'Access-Control-Allow-Origin': origin,
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Admin-Key',
        'Access-Control-Max-Age': '86400',
    };
}

// Handle OPTIONS preflight requests
function handleOptions(request: Request): Response {
    const origin = request.headers.get('Origin') || '*';
    return new Response(null, {
        status: 204,
        headers: corsHeaders(origin),
    });
}

// JSON response helper
function jsonResponse(data: any, status = 200, origin = '*'): Response {
    return new Response(JSON.stringify(data), {
        status,
        headers: {
            'Content-Type': 'application/json',
            ...corsHeaders(origin),
        },
    });
}

// Error response helper
function errorResponse(message: string, status = 400, origin = '*'): Response {
    return jsonResponse({ error: message }, status, origin);
}

// Authentication helper
function authenticate(request: Request, env: Env): boolean {
    const apiKey = request.headers.get('X-Admin-Key');
    // Allow if key matches OR if key is not set in env (dev mode, though risky)
    // Strictly enforcing auth if env var exists.
    if (env.ADMIN_API_KEY && apiKey === env.ADMIN_API_KEY) {
        return true;
    }
    return false;
}

export default {
    async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
        const url = new URL(request.url);
        const path = url.pathname;
        const method = request.method;
        const origin = request.headers.get('Origin') || '*';

        // Handle CORS preflight
        if (method === 'OPTIONS') {
            return handleOptions(request);
        }

        try {
            // Routes
            if (path.startsWith('/api/movies')) {
                return await handleMovies(request, env, path, method, origin);
            } else if (path.startsWith('/api/tvshows')) {
                return await handleTVShows(request, env, path, method, origin);
            } else if (path.startsWith('/api/episodes')) {
                return await handleEpisodes(request, env, path, method, origin);
            } else if (path.startsWith('/api/seasons')) {
                return await handleSeasons(request, env, path, method, origin);
            } else if (path.startsWith('/api/genres')) {
                return await handleGenres(request, env, path, method, origin);
            } else if (path === '/cumis') {
                return await handleAdminPanel(request, env, origin);
            } else if (path.startsWith('/api/admin/')) {
                return await handleAdminApi(request, env, path, method, origin);
            } else if (path === '/api/health') {
                return jsonResponse({ status: 'ok', timestamp: new Date().toISOString() }, 200, origin);
            } else {
                return errorResponse('Not Found', 404, origin);
            }
        } catch (error: any) {
            console.error('API Error:', error);
            return errorResponse(error.message || 'Internal Server Error', 500, origin);
        }
    },
};

// Movie handlers
async function handleMovies(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    const pathParts = path.split('/').filter(Boolean);
    const movieId = pathParts[2] ? parseInt(pathParts[2]) : null;

    if (method === 'GET' && !movieId) {
        // GET /api/movies - List all movies
        const url = new URL(request.url);
        const limit = parseInt(url.searchParams.get('limit') || '50');
        const offset = parseInt(url.searchParams.get('offset') || '0');
        const search = url.searchParams.get('search');
        let updatedAfter = url.searchParams.get('updated_after'); // Timestamp in seconds or ISO string

        let query = 'SELECT * FROM movies WHERE 1=1';
        const params: any[] = [];

        if (search) {
            query += ' AND title LIKE ?';
            params.push(`%${search}%`);
        }

        if (updatedAfter) {
            // Standardize to milliseconds
            if (updatedAfter.includes('-')) {
                // ISO format
                let isoDate = updatedAfter.replace(' ', 'T');
                if (!isoDate.endsWith('Z') && !isoDate.includes('+')) isoDate += 'Z';
                const ts = Date.parse(isoDate);
                if (!isNaN(ts)) updatedAfter = ts.toString();
            } else if (/^\d+$/.test(updatedAfter)) {
                // Numeric: if seconds (short), convert to ms
                if (updatedAfter.length < 12) {
                    updatedAfter = (parseInt(updatedAfter) * 1000).toString();
                }
            }

            query += ' AND updated_at > ?';
            params.push(parseInt(updatedAfter));
        }

        query += ' ORDER BY updated_at DESC LIMIT ? OFFSET ?';
        params.push(limit, offset);

        const db = getDatabase(request, env);
        const result = await db.prepare(query).bind(...params).all();
        return jsonResponse({ movies: result.results, count: result.results.length }, 200, origin);
    } else if (method === 'GET' && movieId) {
        // GET /api/movies/:id - Get single movie with genres
        const db = getDatabase(request, env);
        const movie = await db.prepare('SELECT * FROM movies WHERE id = ?').bind(movieId).first();

        if (!movie) {
            return errorResponse('Movie not found', 404, origin);
        }

        // Get genres
        const genres = await env.DB.prepare(`
      SELECT g.* FROM genres g
      JOIN movie_genres mg ON g.id = mg.genre_id
      WHERE mg.movie_id = ?
    `).bind(movieId).all();

        return jsonResponse({ ...movie, genres: genres.results }, 200, origin);
    } else if (method === 'POST') { // Create or Update
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);

        // POST /api/movies - Create new movie (Upsert logic often preferred for import)
        const body = await request.json() as any;

        // Check if exists by TMDB ID
        // Check if exists by GDID (Unique File ID)
        let existing = null;
        if (body.gd_id) {
            existing = await env.DB.prepare('SELECT id FROM movies WHERE gd_id = ?').bind(body.gd_id).first();

            // Fallback: If not found by gd_id, check if there's an "orphan" entry 
            // (same TMDB ID but NULL gd_id) that we should claim/fix.
            if (!existing) {
                existing = await env.DB.prepare('SELECT id FROM movies WHERE tmdb_id = ? AND gd_id IS NULL').bind(body.tmdb_id).first();
            }
        } else {
            // Legacy fallback (shouldn't happen with new importer): Check by TMDB ID
            existing = await env.DB.prepare('SELECT id FROM movies WHERE tmdb_id = ?').bind(body.tmdb_id).first();
        }

        if (existing) {
            // Update existing
            const now = Date.now();
            await env.DB.prepare(`
                UPDATE movies SET 
                    title = ?, original_title = ?, overview = ?, poster_path = ?, backdrop_path = ?, 
                    logo_path = ?, release_date = ?, runtime = ?, vote_average = ?, vote_count = ?, 
                    popularity = ?, adult = ?, status = ?, 
                    original_language = ?, imdb_id = ?, homepage = ?, tagline = ?,
                    url_string = ?, file_name = ?, mime_type = ?, size = ?, modified_time = ?, gd_id = ?,
                    updated_at = ?
                WHERE id = ?
             `).bind(
                body.title, body.original_title, body.overview, body.poster_path, body.backdrop_path,
                body.logo_path, body.release_date, body.runtime, body.vote_average, body.vote_count,
                body.popularity, body.adult ? 1 : 0, body.status,
                body.original_language, body.imdb_id, body.homepage, body.tagline,
                body.url_string, body.file_name, body.mime_type, body.size, body.modified_time, body.gd_id,
                now, existing.id
            ).run();
            return jsonResponse({ id: existing.id, message: 'Movie updated' }, 200, origin);
        } else {
            // Insert new
            const now = Date.now();
            const result = await env.DB.prepare(`
              INSERT INTO movies (tmdb_id, title, original_title, overview, poster_path, backdrop_path, 
                logo_path, release_date, runtime, vote_average, vote_count, popularity, adult, status,
                original_language, imdb_id, homepage, tagline,
                url_string, file_name, mime_type, size, modified_time, gd_id, updated_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `).bind(
                body.tmdb_id, body.title, body.original_title, body.overview, body.poster_path,
                body.backdrop_path, body.logo_path, body.release_date, body.runtime,
                body.vote_average, body.vote_count, body.popularity, body.adult ? 1 : 0, body.status,
                body.original_language, body.imdb_id, body.homepage, body.tagline,
                body.url_string, body.file_name, body.mime_type, body.size, body.modified_time, body.gd_id,
                now
            ).run();
            return jsonResponse({ id: result.meta.last_row_id, message: 'Movie created' }, 201, origin);
        }

    } else if (method === 'DELETE' && movieId) {
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);
        // DELETE /api/movies/:id
        await env.DB.prepare('DELETE FROM movies WHERE id = ?').bind(movieId).run();
        return jsonResponse({ message: 'Movie deleted' }, 200, origin);
    }

    return errorResponse('Method not allowed', 405, origin);
}

// TV Show handlers
async function handleTVShows(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    const pathParts = path.split('/').filter(Boolean);
    const showId = pathParts[2] ? parseInt(pathParts[2]) : null;
    const subResource = pathParts[3];

    if (method === 'GET' && !showId) {
        // GET /api/tvshows - List all TV shows
        const url = new URL(request.url);
        let updatedAfter = url.searchParams.get('updated_after');
        const limit = parseInt(url.searchParams.get('limit') || '50');
        const offset = parseInt(url.searchParams.get('offset') || '0');

        let query = 'SELECT * FROM tv_shows WHERE 1=1';
        const params: any[] = [];

        if (updatedAfter) {
            // Standardize to milliseconds
            if (updatedAfter.includes('-')) {
                let isoDate = updatedAfter.replace(' ', 'T');
                if (!isoDate.endsWith('Z') && !isoDate.includes('+')) isoDate += 'Z';
                const ts = Date.parse(isoDate);
                if (!isNaN(ts)) updatedAfter = ts.toString();
            } else if (/^\d+$/.test(updatedAfter)) {
                if (updatedAfter.length < 12) {
                    updatedAfter = (parseInt(updatedAfter) * 1000).toString();
                }
            }
            query += ' AND updated_at > ?';
            params.push(parseInt(updatedAfter));
        }

        query += ' ORDER BY updated_at DESC LIMIT ? OFFSET ?';
        params.push(limit, offset);

        const db = getDatabase(request, env);
        const result = await db.prepare(query).bind(...params).all();
        return jsonResponse({ tvshows: result.results }, 200, origin);
    } else if (method === 'GET' && showId && subResource === 'seasons') {
        // GET /api/tvshows/:id/seasons - Get all seasons for a show
        const db = getDatabase(request, env);
        const seasons = await db.prepare('SELECT * FROM seasons WHERE show_id = ? ORDER BY season_number').bind(showId).all();
        return jsonResponse({ seasons: seasons.results }, 200, origin);
    } else if (method === 'GET' && showId && subResource === 'episodes') {
        const url = new URL(request.url);
        const seasonNumber = url.searchParams.get('season');

        let query = 'SELECT * FROM episodes WHERE show_id = ?';
        const params: any[] = [showId];

        if (seasonNumber) {
            query += ' AND season_number = ?';
            params.push(parseInt(seasonNumber));
        }

        query += ' ORDER BY season_number, episode_number';

        const db = getDatabase(request, env);
        const episodes = await db.prepare(query).bind(...params).all();
        return jsonResponse({ episodes: episodes.results }, 200, origin);
    } else if (method === 'GET' && showId) {
        // GET /api/tvshows/:id
        const db = getDatabase(request, env);
        const show = await db.prepare('SELECT * FROM tv_shows WHERE id = ?').bind(showId).first();
        if (!show) return errorResponse('TV Show not found', 404, origin);
        return jsonResponse(show, 200, origin);
    } else if (method === 'POST') {
        // POST /api/tvshows - Create/Update TV Show
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);
        const body = await request.json() as any;

        const existing = await env.DB.prepare('SELECT id FROM tv_shows WHERE tmdb_id = ?').bind(body.tmdb_id).first();
        if (existing) {
            const now = Date.now();
            await env.DB.prepare(`
                UPDATE tv_shows SET 
                    name = ?, overview = ?, poster_path = ?, backdrop_path = ?, 
                    vote_average = ?, first_air_date = ?, last_air_date = ?, number_of_seasons = ?, number_of_episodes = ?,
                    status = ?, type = ?, logo_path = ?, updated_at = ?
                WHERE id = ?
             `).bind(
                body.name, body.overview, body.poster_path, body.backdrop_path,
                body.vote_average, body.first_air_date, body.last_air_date, body.number_of_seasons, body.number_of_episodes,
                body.status, body.type, body.logo_path, now, existing.id
            ).run();
            return jsonResponse({ id: existing.id, message: 'TV Show updated' }, 200, origin);
        } else {
            const now = Date.now();
            const result = await env.DB.prepare(`
                INSERT INTO tv_shows (tmdb_id, name, overview, poster_path, backdrop_path, 
                    vote_average, first_air_date, last_air_date, number_of_seasons, number_of_episodes,
                    status, type, logo_path, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             `).bind(
                body.tmdb_id, body.name, body.overview, body.poster_path, body.backdrop_path,
                body.vote_average, body.first_air_date, body.last_air_date, body.number_of_seasons, body.number_of_episodes,
                body.status, body.type, body.logo_path, now
            ).run();
            return jsonResponse({ id: result.meta.last_row_id, message: 'TV Show created' }, 201, origin);
        }
    }

    return errorResponse('Method not allowed', 405, origin);
}

// Season handlers
async function handleSeasons(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    if (method === 'POST') {
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);
        const body = await request.json() as any;

        // Ensure show_id exists (we assume caller passes internal DB ID for show_id)
        // OR we can lookup by tmdb_id if passed. But usually hierarchical insert should know relation.
        // Let's assume body has show_id (internal D1 ID).

        const existing = await env.DB.prepare('SELECT id FROM seasons WHERE show_id = ? AND season_number = ?').bind(body.show_id, body.season_number).first();

        if (existing) {
            const now = Date.now();
            await env.DB.prepare(`
                UPDATE seasons SET 
                    name = ?, overview = ?, poster_path = ?, air_date = ?, episode_count = ?,
                    updated_at = ?
                WHERE id = ?
             `).bind(body.name, body.overview, body.poster_path, body.air_date, body.episode_count, now, existing.id).run();
            return jsonResponse({ id: existing.id, message: 'Season updated' }, 200, origin);
        } else {
            const now = Date.now();
            const result = await env.DB.prepare(`
                INSERT INTO seasons (show_id, season_number, name, overview, poster_path, air_date, episode_count, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
             `).bind(body.show_id, body.season_number, body.name, body.overview, body.poster_path, body.air_date, body.episode_count, now).run();
            return jsonResponse({ id: result.meta.last_row_id, message: 'Season created' }, 201, origin);
        }
    }
    return errorResponse('Method not allowed', 405, origin);
}

// Episode handlers
async function handleEpisodes(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    const pathParts = path.split('/').filter(Boolean);
    const episodeId = pathParts[2] ? parseInt(pathParts[2]) : null;
    const db = getDatabase(request, env); // Initialize db once at the top

    if (method === 'GET' && episodeId) {
        const episode = await db.prepare('SELECT * FROM episodes WHERE id = ?').bind(episodeId).first();
        if (!episode) {
            return errorResponse('Episode not found', 404, origin);
        }
        return jsonResponse(episode, 200, origin);
    } else if (method === 'PUT' && episodeId) {
        // Update episode (mark as played) - Publicly allowed? Maybe restricted for now.
        // User state usually syncs separately. Admin update?
        // Let's allow simple updates if auth or maybe specific fields.
        // For now, implementing Admin Update logic.
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);

        const body = await request.json() as any;
        const now = Date.now();
        await env.DB.prepare("UPDATE episodes SET played = ?, updated_at = ? WHERE id = ?")
            .bind(body.played ? 1 : 0, now, episodeId).run();
        return jsonResponse({ message: 'Episode updated' }, 200, origin);
    } else if (method === 'POST') {
        if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);
        const body = await request.json() as any;

        let existing = null;
        if (body.gd_id) {
            existing = await env.DB.prepare('SELECT id FROM episodes WHERE gd_id = ?').bind(body.gd_id).first();

            if (!existing) {
                // Fallback: Claim orphan episode
                existing = await env.DB.prepare('SELECT id FROM episodes WHERE show_id = ? AND season_number = ? AND episode_number = ? AND gd_id IS NULL')
                    .bind(body.show_id, body.season_number, body.episode_number).first();
            }
        } else {
            existing = await env.DB.prepare('SELECT id FROM episodes WHERE show_id = ? AND season_number = ? AND episode_number = ?')
                .bind(body.show_id, body.season_number, body.episode_number).first();
        }

        if (existing) {
            const now = Date.now();
            await env.DB.prepare(`
                UPDATE episodes SET 
                    tmdb_id = ?, name = ?, overview = ?, still_path = ?, air_date = ?, vote_average = ?,
                    url_string = ?, file_name = ?, mime_type = ?, size = ?, modified_time = ?, gd_id = ?,
                    updated_at = ?
                WHERE id = ?
             `).bind(
                body.tmdb_id, body.name, body.overview, body.still_path, body.air_date, body.vote_average,
                body.url_string, body.file_name, body.mime_type, body.size, body.modified_time, body.gd_id,
                now, existing.id
            ).run();
            return jsonResponse({ id: existing.id, message: 'Episode updated' }, 200, origin);
        } else {
            const now = Date.now();
            const result = await env.DB.prepare(`
                INSERT INTO episodes (show_id, season_number, episode_number, tmdb_id, name, overview, 
                    still_path, air_date, vote_average, url_string, file_name, mime_type, size, gd_id, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
             `).bind(
                body.show_id, body.season_number, body.episode_number, body.tmdb_id, body.name, body.overview,
                body.still_path, body.air_date, body.vote_average,
                body.url_string, body.file_name, body.mime_type, body.size,
                body.gd_id, now
            ).run();
            return jsonResponse({ id: result.meta.last_row_id, message: 'Episode created' }, 201, origin);
        }
    }

    return errorResponse('Method not allowed', 405, origin);
}

// Genre handlers
async function handleGenres(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    if (method === 'GET') {
        const db = getDatabase(request, env);
        const genres = await db.prepare('SELECT * FROM genres ORDER BY name').all();
        return jsonResponse({ genres: genres.results }, 200, origin);
    }

    return errorResponse('Method not allowed', 405, origin);
}

// Admin Panel UI Handler
async function handleAdminPanel(request: Request, env: Env, origin: string): Promise<Response> {
    const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NFGPlus Admin | Premium D1 Manager</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-deep: #0f172a;
            --bg-card: rgba(30, 41, 59, 0.7);
            --accent: #10b981;
            --accent-hover: #059669;
            --text-main: #f8fafc;
            --text-dim: #94a3b8;
            --glass-border: rgba(255, 255, 255, 0.1);
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Outfit', sans-serif;
            background: var(--bg-deep);
            color: var(--text-main);
            overflow: hidden;
            background-image: radial-gradient(circle at 0% 0%, rgba(16, 185, 129, 0.15) 0%, transparent 40%),
                              radial-gradient(circle at 100% 100%, rgba(16, 185, 129, 0.1) 0%, transparent 40%);
            min-height: 100vh;
        }

        .auth-container {
            height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .glass-card {
            background: var(--bg-card);
            backdrop-filter: blur(12px);
            border: 1px solid var(--glass-border);
            border-radius: 24px;
            padding: 40px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        }

        .auth-card {
            width: 100%;
            max-width: 400px;
            text-align: center;
        }

        h1 { font-size: 2.5rem; margin-bottom: 8px; font-weight: 700; letter-spacing: -1px; }
        .subtitle { color: var(--text-dim); margin-bottom: 32px; font-size: 0.95rem; }

        .input-group { margin-bottom: 24px; text-align: left; }
        label { display: block; margin-bottom: 8px; color: var(--text-dim); font-size: 0.85rem; font-weight: 600; text-transform: uppercase; }
        input {
            width: 100%;
            padding: 14px 18px;
            background: rgba(15, 23, 42, 0.6);
            border: 1px solid var(--glass-border);
            border-radius: 12px;
            color: white;
            font-family: inherit;
            transition: all 0.3s;
        }
        input:focus { outline: none; border-color: var(--accent); box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1); }

        .btn {
            width: 100%;
            padding: 14px;
            background: var(--accent);
            color: white;
            border: none;
            border-radius: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }
        .btn:hover { background: var(--accent-hover); transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.3); }

        /* Main Dashboard */
        #main-app { display: none; height: 100vh; }
        .sidebar {
            width: 280px;
            height: 100%;
            border-right: 1px solid var(--glass-border);
            padding: 32px;
            display: flex;
            flex-direction: column;
        }

        .content { flex: 1; padding: 40px; overflow-y: auto; }
        
        .nav-item {
            padding: 12px 18px;
            margin-bottom: 8px;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.2s;
            color: var(--text-dim);
            font-weight: 500;
        }
        .nav-item:hover { background: rgba(255,255,255,0.05); color: white; }
        .nav-item.active { background: rgba(16, 185, 129, 0.1); color: var(--accent); }

        table { width: 100%; border-collapse: collapse; margin-top: 24px; font-size: 0.9rem; }
        th { text-align: left; padding: 16px; color: var(--text-dim); font-weight: 600; border-bottom: 1px solid var(--glass-border); }
        td { padding: 16px; border-bottom: 1px solid var(--glass-border); color: var(--text-main); }
        tr:hover { background: rgba(255,255,255,0.02); }

        .tag {
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 0.75rem;
            font-weight: 600;
            background: rgba(16, 185, 129, 0.1);
            color: var(--accent);
        }

        .action-btn {
            background: none; border: none; color: var(--text-dim); cursor: pointer; padding: 4px; border-radius: 4px; transition: 0.2s;
        }
        .action-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); }
        .edit-btn:hover { color: var(--accent); background: rgba(16, 185, 129, 0.1); }

        /* Modal styling */
        .modal {
            display: none;
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.8);
            backdrop-filter: blur(8px);
            z-index: 1000;
            align-items: center; justify-content: center;
        }
        .modal-content {
            width: 90%; max-width: 600px;
            max-height: 80vh; overflow-y: auto;
        }
        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 20px; }
        .modal-footer { margin-top: 32px; display: flex; gap: 12px; justify-content: flex-end; }

        ::-webkit-scrollbar { width: 8px; }
        ::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; }
    </style>
</head>
<body>
    <div id="auth-screen" class="auth-container">
        <div class="glass-card auth-card">
            <h1>NFGPlus</h1>
            <p class="subtitle">Secure Database Access Portal</p>
            <div class="input-group">
                <label>Admin Access Key</label>
                <input type="password" id="admin-key" placeholder="Enter key...">
            </div>
            <button class="btn" onclick="login()">Authenticate</button>
        </div>
    </div>

    <div id="main-app" style="display: none;">
        <div class="sidebar">
            <h2 style="margin-bottom: 40px; letter-spacing: -1px;">D1 Manager</h2>
            <div id="table-list">
                <!-- Tables will be loaded here -->
            </div>
            <div style="margin-top: auto;">
                <button class="btn" style="background: rgba(239, 68, 68, 0.1); color: #ef4444;" onclick="logout()">Logout</button>
            </div>
        </div>
        <div class="content">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                <h2 id="current-table">Select a Table</h2>
                <div style="display: flex; gap: 12px; align-items: center;">
                    <input type="text" id="search-input" placeholder="Search title or ID..." style="width: 250px; padding: 10px 14px; font-size: 0.85rem;" onkeyup="if(event.key==='Enter') doSearch()">
                    <button class="btn" style="width: auto; padding: 10px 20px; font-size: 0.85rem;" onclick="doSearch()">Search</button>
                </div>
            </div>
            <div id="data-container">
                <p style="color: var(--text-dim);">Choose a table from the sidebar to manage records.</p>
            </div>
        </div>
    </div>

    <!-- Edit Modal -->
    <div id="edit-modal" class="modal">
        <div class="glass-card modal-content" style="padding: 32px;">
            <h3 id="modal-title">Edit Record</h3>
            <div id="edit-form" class="form-grid"></div>
            <div class="modal-footer">
                <button class="btn" style="background: rgba(255,255,255,0.1); width: auto; padding: 10px 24px;" onclick="closeModal()">Cancel</button>
                <button class="btn" style="width: auto; padding: 10px 24px;" onclick="saveEdit()">Save Changes</button>
            </div>
        </div>
    </div>

    <script>
        let currentKey = localStorage.getItem('admin_key');
        let editingData = { table: null, id: null };
        let currentRows = [];

        function login() {
            const key = document.getElementById('admin-key').value;
            if (!key) { alert('Key required'); return; }
            currentKey = key;
            loadDashboard();
        }

        function logout() {
            localStorage.removeItem('admin_key');
            location.reload();
        }

        async function api(path, method = 'GET', body = null) {
            const options = {
                method,
                headers: { 'X-Admin-Key': currentKey, 'Content-Type': 'application/json' }
            };
            if (body) options.body = JSON.stringify(body);
            
            try {
                const res = await fetch('/api/admin/' + path, options);
                if (res.status === 401) {
                    alert('Session expired or invalid key');
                    logout();
                    return;
                }
                return await res.json();
            } catch (e) {
                console.error('API Error:', e);
                return null;
            }
        }

        async function loadDashboard() {
            const data = await api('tables');
            if (data && data.tables) {
                localStorage.setItem('admin_key', currentKey);
                document.getElementById('auth-screen').style.display = 'none';
                document.getElementById('main-app').style.display = 'flex';
                
                const list = document.getElementById('table-list');
                list.innerHTML = '';
                data.tables.forEach(t => {
                    const div = document.createElement('div');
                    div.className = 'nav-item';
                    div.innerHTML = '<span>' + t.name + '</span> <sub style="float:right; opacity: 0.5">' + t.count + '</sub>';
                    div.onclick = () => loadTable(t.name);
                    list.appendChild(div);
                });
            } else {
                alert('Connection failed or invalid Admin Key');
            }
        }

        async function loadTable(name) {
            editingData.table = name;
            document.getElementById('search-input').value = '';
            document.getElementById('current-table').innerText = name;
            document.querySelectorAll('.nav-item').forEach(i => i.classList.toggle('active', i.innerText.includes(name)));
            renderData(await api('query?table=' + name));
        }

        async function doSearch() {
            if (!editingData.table) return;
            const q = document.getElementById('search-input').value;
            renderData(await api('query?table=' + editingData.table + '&search=' + encodeURIComponent(q)));
        }

        function renderData(data) {
            if (!data || !data.rows) return;
            currentRows = data.rows;
            const container = document.getElementById('data-container');
            if (currentRows.length === 0) {
                container.innerHTML = '<p>No records found.</p>';
                return;
            }
            
            const cols = Object.keys(currentRows[0]);
            let html = '<table><thead><tr>';
            cols.forEach(c => html += '<th>' + c + '</th>');
            html += '<th>Actions</th></tr></thead><tbody>';
            
            currentRows.forEach((row, idx) => {
                html += '<tr>';
                cols.forEach(c => {
                    let val = row[c];
                    if (val && typeof val === 'string' && val.length > 50) val = val.substring(0, 47) + '...';
                    html += '<td>' + (val === null ? '' : val) + '</td>';
                });
                html += '<td>';
                html += '<button class="action-btn edit-btn" onclick="openEdit(' + idx + ')">Edit</button>';
                html += '<button class="action-btn" onclick="deleteEntry(' + idx + ')">Delete</button>';
                html += '</td></tr>';
            });
            html += '</tbody></table>';
            container.innerHTML = html;
        }

        function openEdit(idx) {
            const row = currentRows[idx];
            editingData.id = row.id;
            const form = document.getElementById('edit-form');
            form.innerHTML = '';
            Object.keys(row).forEach(key => {
                if (key === 'id' || key === 'created_at' || key === 'updated_at') return;
                const group = document.createElement('div');
                group.className = 'input-group';
                group.innerHTML = '<label>' + key + '</label><input type="text" data-key="' + key + '" id="edit-' + key + '">';
                form.appendChild(group);
                document.getElementById('edit-' + key).value = row[key] || '';
            });
            document.getElementById('edit-modal').style.display = 'flex';
        }

        function closeModal() {
            document.getElementById('edit-modal').style.display = 'none';
        }

        async function saveEdit() {
            const data = {};
            document.querySelectorAll('#edit-form input').forEach(input => {
                data[input.getAttribute('data-key')] = input.value;
            });

            const res = await api('entry', 'PUT', { table: editingData.table, id: editingData.id, data });
            if (res && res.success) {
                closeModal();
                loadTable(editingData.table);
            }
        }

        async function deleteEntry(idx) {
            const row = currentRows[idx];
            if (!confirm('Are you sure you want to delete ID ' + row.id + '?')) return;
            const res = await api('entry', 'DELETE', { table: editingData.table, id: row.id });
            if (res && res.success) {
                loadDashboard();
                loadTable(editingData.table);
            }
        }

        if (currentKey) loadDashboard();
    </script>
</body>
</html>`;
    return new Response(html, { headers: { 'Content-Type': 'text/html' } });
}

// Global Admin API Handler
async function handleAdminApi(
    request: Request,
    env: Env,
    path: string,
    method: string,
    origin: string
): Promise<Response> {
    if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);

    const url = new URL(request.url);

    if (path === '/api/admin/gdids') {
        return await handleAdminGdIds(request, env, origin);
    }

    if (path === '/api/admin/tables') {
        const tableNames = ['movies', 'tv_shows', 'episodes', 'genres', 'seasons'];
        const tables = [];
        for (const name of tableNames) {
            const countRes = await env.DB.prepare('SELECT count(*) as total FROM ' + name).first();
            tables.push({ name, count: countRes ? (countRes as any).total : 0 });
        }
        return jsonResponse({ tables }, 200, origin);
    }

    if (path === '/api/admin/query') {
        const table = url.searchParams.get('table');
        const search = url.searchParams.get('search');
        if (!table) return errorResponse('Table required');

        let query = 'SELECT * FROM ' + table;
        const params: any[] = [];

        if (search) {
            // Find common searchable columns
            const columns: string[] = ['title', 'name', 'gd_id', 'username', 'email'];
            const conditions = columns.map(c => c + ' LIKE ?').join(' OR ');
            query += ' WHERE ' + conditions;
            columns.forEach(() => params.push(`%${search}%`));
        }

        query += ' ORDER BY id DESC LIMIT 100';
        const rows = await env.DB.prepare(query).bind(...params).all();
        return jsonResponse({ rows: rows.results }, 200, origin);
    }

    if (path === '/api/admin/entry' && method === 'DELETE') {
        const body = await request.json() as any;
        if (!body.table || !body.id) return errorResponse('Invalid parameters');
        await env.DB.prepare('DELETE FROM ' + body.table + ' WHERE id = ?').bind(body.id).run();
        return jsonResponse({ success: true, message: 'Deleted successfully' }, 200, origin);
    }

    if (path === '/api/admin/entry' && method === 'PUT') {
        const body = await request.json() as any;
        if (!body.table || !body.id || !body.data) return errorResponse('Invalid parameters');
        const keys = Object.keys(body.data);
        const updates = keys.map(k => k + ' = ?').join(', ');
        const values = Object.values(body.data);
        await env.DB.prepare('UPDATE ' + body.table + ' SET ' + updates + ', updated_at = ? WHERE id = ?')
            .bind(...values, Date.now(), body.id).run();
        return jsonResponse({ success: true, message: 'Updated successfully' }, 200, origin);
    }

    return errorResponse('Not Found', 404, origin);
}

// Helper for GDID Delta Sync
async function handleAdminGdIds(request: Request, env: Env, origin: string): Promise<Response> {
    const url = new URL(request.url);
    const since = url.searchParams.get('since');
    let sinceMs = 0;

    if (since) {
        sinceMs = parseInt(since);
        if (isNaN(sinceMs)) sinceMs = 0;
    }

    let movieQuery = 'SELECT gd_id FROM movies WHERE gd_id IS NOT NULL';
    let epQuery = 'SELECT gd_id FROM episodes WHERE gd_id IS NOT NULL';
    const params: any[] = [];

    if (sinceMs > 0) {
        movieQuery += ' AND updated_at > ?';
        epQuery += ' AND updated_at > ?';
        params.push(sinceMs);
    }

    const movies = await env.DB.prepare(movieQuery).bind(...params).all();
    const episodes = await env.DB.prepare(epQuery).bind(...params).all();

    return jsonResponse({
        movies: movies.results.map((r: any) => r.gd_id),
        episodes: episodes.results.map((r: any) => r.gd_id),
        count: movies.results.length + episodes.results.length,
        timestamp: Date.now()
    }, 200, origin);
}
