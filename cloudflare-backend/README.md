# NFGPlus Cloudflare Backend

This directory contains the Cloudflare Workers backend for NFGPlus app.

## Architecture

- **Cloudflare D1**: SQLite-based edge database (10GB storage, 10M reads/day free)
- **Cloudflare Workers**: Serverless API endpoints at the edge
- **REST API**: JSON API for Android app communication

## Setup

1. Install Wrangler CLI: `npm install -g wrangler`
2. Login to Cloudflare: `wrangler login`
3. Create D1 database: `wrangler d1 create nfgplus-db`
4. Deploy: `npm run deploy`

## API Endpoints

- `GET /api/movies` - Get all movies
- `GET /api/movies/:id` - Get movie by ID
- `POST /api/movies` - Create new movie
- `PUT /api/movies/:id` - Update movie
- `DELETE /api/movies/:id` - Delete movie

Similar endpoints for TVShows, Episodes, etc.

## Database Schema

See `schema.sql` for the complete D1 database schema.
