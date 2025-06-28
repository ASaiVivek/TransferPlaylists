package com.example.transferplaylists.proxies;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyProxy {
	private final WebClient webClient;
	
	public SpotifyProxy(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl("https://api.spotify.com/v1").build();
	}
	
	/**
     * Fetches the user's playlists from Spotify.
     *
     * @param accessToken Spotify access token.
     * @return List of playlists with their details.
     * 
     * 
     * for access token: 
     * 
     *  curl -X POST "https://accounts.spotify.com/api/token" \
		>     -H "Authorization: Basic $(echo -n 'secret' | base64)" \
		>     -d "grant_type=client_credentials"
		
		response:
		{"access_token":"secret","token_type":"Bearer","expires_in":3600}%
     * 
     * 
     * handled using Spring OAuth
     * 
     * 
     * sample req/resp
     * 
     * my user-id: secret_id
     * 
     * curl --request GET \
  			--url https://api.spotify.com/v1/users/<USER_ID>/playlists \
  			--header 'Authorization: Bearer <ACCESS_TOKEN>'
     * 
     */
    @SuppressWarnings("unchecked")
	public List<Map<String, String>> getPlaylists(String accessToken) {
        try {
			Map<String, Object> response = webClient.get()
                    .uri("/me/playlists")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (List<Map<String, String>>) response.get("items");
        } catch (WebClientResponseException e) {
            // Log error and handle it gracefully
            System.err.println("Error fetching playlists: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetches the tracks for a specific playlist.
     *
     * @param playlistId  Spotify playlist ID.
     * @param accessToken Spotify access token.
     * @return List of tracks with their details.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getPlaylistTracks(String playlistId, String accessToken) {
        try {
			Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/playlists/{playlistId}/tracks")
                            .build(playlistId))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseTracks(response);
        } catch (WebClientResponseException e) {
            // Log error and handle it gracefully
            System.err.println("Error fetching playlist tracks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parses tracks from the Spotify API response.
     *
     * @param response Spotify API response containing track details.
     * @return List of parsed tracks.
     */
    @SuppressWarnings("unchecked")
	private List<Map<String, String>> parseTracks(Map<String, Object> response) {
        var tracks = new ArrayList<Map<String, String>>();
        if (response == null || !response.containsKey("items")) {
            return tracks;
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        for (Map<String, Object> item : items) {
            Map<String, Object> track = (Map<String, Object>) item.get("track");
            if (track != null) {
                tracks.add(Map.of(
                        "name", (String) track.get("name"),
                        "artist", extractArtistNames((List<Map<String, Object>>) track.get("artists"))
                ));
            }
        }
        return tracks;
    }

    /**
     * Extracts artist names from the track's artist list.
     *
     * @param artists List of artist details.
     * @return Comma-separated artist names.
     */
    private String extractArtistNames(List<Map<String, Object>> artists) {
        if (artists == null || artists.isEmpty()) {
            return "Unknown Artist";
        }
        var artistNames = new ArrayList<String>();
        for (Map<String, Object> artist : artists) {
            artistNames.add((String) artist.get("name"));
        }
        return String.join(", ", artistNames);
    }
}
