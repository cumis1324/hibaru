/**
 * NFGPlus Cloudflare Workers API
 * REST API for NFGPlus Android App
 */

export interface Env {
    DB: D1Database;
    ALLOWED_ORIGINS: string;
}

// CORS headers helper
function corsHeaders(origin: string) {
    return {
        'Access-Control-Allow-Origin': origin,
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
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
                return handleMovies(request, env, path, method, origin);
            } else if (path.startsWith('/api/tvshows')) {
                return handleTVShows(request, env, path, method, origin);
            } else if (path.startsWith('/api/episodes')) {
                return handleEpisodes(request, env, path, method, origin);
            } else if (path.startsWith('/api/genres')) {
                return handleGenres(request, env, path, method, origin);
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

        let query = 'SELECT * FROM movies WHERE 1=1';
        const params: any[] = [];

        if (search) {
            query += ' AND title LIKE ?';
            params.push(`%${search}%`);
        }

        query += ' ORDER BY updated_at DESC LIMIT ? OFFSET ?';
        params.push(limit, offset);

        const result = await env.DB.prepare(query).bind(...params).all();
        return jsonResponse({ movies: result.results, count: result.results.length }, 200, origin);
    } else if (method === 'GET' && movieId) {
        // GET /api/movies/:id - Get single movie with genres
        const movie = await env.DB.prepare('SELECT * FROM movies WHERE id = ?').bind(movieId).first();

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
    } else if (method === 'POST' && !movieId) {
        // POST /api/movies - Create new movie
        const body = await request.json() as any;

        const result = await env.DB.prepare(`
      INSERT INTO movies (tmdb_id, title, original_title, overview, poster_path, backdrop_path, 
        logo_path, release_date, runtime, vote_average, vote_count, popularity, adult, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).bind(
            body.tmdb_id, body.title, body.original_title, body.overview, body.poster_path,
            body.backdrop_path, body.logo_path, body.release_date, body.runtime,
            body.vote_average, body.vote_count, body.popularity, body.adult ? 1 : 0, body.status
        ).run();

        return jsonResponse({ id: result.meta.last_row_id, message: 'Movie created' }, 201, origin);
    } else if (method === 'PUT' && movieId) {
        // PUT /api/movies/:id - Update movie
        const body = await request.json() as any;

        await env.DB.prepare(`
      UPDATE movies SET title = ?, overview = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).bind(body.title, body.overview, movieId).run();

        return jsonResponse({ message: 'Movie updated' }, 200, origin);
    } else if (method === 'DELETE' && movieId) {
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
        const result = await env.DB.prepare('SELECT * FROM tv_shows ORDER BY updated_at DESC LIMIT 50').all();
        return jsonResponse({ tvshows: result.results }, 200, origin);
    } else if (method === 'GET' && showId && subResource === 'seasons') {
        // GET /api/tvshows/:id/seasons - Get all seasons for a show
        const seasons = await env.DB.prepare('SELECT * FROM seasons WHERE show_id = ? ORDER BY season_number').bind(showId).all();
        return jsonResponse({ seasons: seasons.results }, 200, origin);
    } else if (method === 'GET' && showId && subResource === 'episodes') {
        // GET /api/tvshows/:id/episodes - Get all episodes for a show
        const url = new URL(request.url);
        const seasonNumber = url.searchParams.get('season');

        let query = 'SELECT * FROM episodes WHERE show_id = ?';
        const params: any[] = [showId];

        if (seasonNumber) {
            query += ' AND season_number = ?';
            params.push(parseInt(seasonNumber));
        }

        query += ' ORDER BY season_number, episode_number';

        const episodes = await env.DB.prepare(query).bind(...params).all();
        return jsonResponse({ episodes: episodes.results }, 200, origin);
    } else if (method === 'GET' && showId) {
        // GET /api/tvshows/:id - Get single TV show
        const show = await env.DB.prepare('SELECT * FROM tv_shows WHERE id = ?').bind(showId).first();

        if (!show) {
            return errorResponse('TV Show not found', 404, origin);
        }

        return jsonResponse(show, 200, origin);
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

    if (method === 'GET' && episodeId) {
        const episode = await env.DB.prepare('SELECT * FROM episodes WHERE id = ?').bind(episodeId).first();
        if (!episode) {
            return errorResponse('Episode not found', 404, origin);
        }
        return jsonResponse(episode, 200, origin);
    } else if (method === 'PUT' && episodeId) {
        // Update episode (mark as played)
        const body = await request.json() as any;
        await env.DB.prepare('UPDATE episodes SET played = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?')
            .bind(body.played ? 1 : 0, episodeId).run();
        return jsonResponse({ message: 'Episode updated' }, 200, origin);
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
        const genres = await env.DB.prepare('SELECT * FROM genres ORDER BY name').all();
        return jsonResponse({ genres: genres.results }, 200, origin);
    }

    return errorResponse('Method not allowed', 405, origin);
}
