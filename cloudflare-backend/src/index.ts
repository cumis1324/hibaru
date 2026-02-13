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
            } else if (path === '/api/admin/gdids') {
                return await handleAdminGdIds(request, env, origin);
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

// Admin handlers
async function handleAdminGdIds(request: Request, env: Env, origin: string): Promise<Response> {
    if (!authenticate(request, env)) return errorResponse('Unauthorized', 401, origin);

    // Fetch all GDIDs from Movies and Episodes
    // This optimization prevents N+1 requests during sync
    const movies = await env.DB.prepare('SELECT gd_id FROM movies WHERE gd_id IS NOT NULL').all();
    const episodes = await env.DB.prepare('SELECT gd_id FROM episodes WHERE gd_id IS NOT NULL').all();

    // Flatten results
    const movieIds = movies.results.map((r: any) => r.gd_id);
    const episodeIds = episodes.results.map((r: any) => r.gd_id);

    return jsonResponse({
        movies: movieIds,
        episodes: episodeIds,
        count: movieIds.length + episodeIds.length
    }, 200, origin);
}
