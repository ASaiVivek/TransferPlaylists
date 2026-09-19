# TransferPlaylists

Transfer playlists between music platforms. Currently supports **Spotify → YouTube Music** (via YouTube Data API v3).

## Status

| Area | Status |
|------|--------|
| Spotify playlist + track fetch | Done (with pagination) |
| Spotify OAuth | Done |
| Google / YouTube OAuth | Done |
| YouTube playlist create + track search | Done |
| Transfer orchestration | Done |

## Setup

1. Create a [Spotify app](https://developer.spotify.com/dashboard) with redirect URI `http://localhost:8080/login/oauth2/code/spotify`.
2. Create a [Google Cloud project](https://console.cloud.google.com/) with YouTube Data API v3 enabled and OAuth redirect URI `http://localhost:8080/login/oauth2/code/google`.
3. Set environment variables (or edit `application.properties`):

```bash
export SPOTIFY_CLIENT_ID=your_spotify_client_id
export SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
```

4. Run the app:

```bash
mvn spring-boot:run
```

## Usage

1. Open `http://localhost:8080` in your browser.
2. Click **Connect Spotify**, then **Connect Google**.
3. Click **Transfer playlists** when both show as connected.

You can also use the API directly: `GET /status` and `POST /transfer`.

The transfer creates private YouTube playlists matching your Spotify playlist names and adds the best-matching YouTube video for each track (`artist + track name` search). Unmatched tracks are skipped.

## Design decisions

- **YouTube Data API v3** is used because YouTube Music has no public write API. Playlists created in YouTube appear in YouTube Music.
- **Track matching** uses the first YouTube search result for `artist track` (simple, good enough for a first version).
- **All playlists** are transferred; there is no per-playlist selection yet.
- **Unmatched tracks** are skipped rather than failing the whole transfer.
