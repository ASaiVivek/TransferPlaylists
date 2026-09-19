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

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getPlaylists(String accessToken) {
		var playlists = new ArrayList<Map<String, Object>>();
		String nextUrl = "/me/playlists?limit=50";

		while (nextUrl != null) {
			Map<String, Object> response = fetch(accessToken, nextUrl);
			if (response == null) {
				break;
			}

			playlists.addAll((List<Map<String, Object>>) response.get("items"));
			nextUrl = extractNextPath(response.get("next"));
		}

		return playlists;
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getPlaylistTracks(String playlistId, String accessToken) {
		var tracks = new ArrayList<Map<String, String>>();
		String nextUrl = "/playlists/" + playlistId + "/tracks?limit=100";

		while (nextUrl != null) {
			Map<String, Object> response = fetch(accessToken, nextUrl);
			if (response == null) {
				break;
			}

			tracks.addAll(parseTracks(response));
			nextUrl = extractNextPath(response.get("next"));
		}

		return tracks;
	}

	private Map<String, Object> fetch(String accessToken, String uri) {
		try {
			return webClient.get()
					.uri(uri)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.bodyToMono(Map.class)
					.block();
		} catch (WebClientResponseException e) {
			System.err.println("Error fetching from Spotify: " + e.getMessage());
			return null;
		}
	}

	private String extractNextPath(Object next) {
		if (next == null) {
			return null;
		}

		String nextUrl = (String) next;
		return nextUrl.replace("https://api.spotify.com/v1", "");
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, String>> parseTracks(Map<String, Object> response) {
		var tracks = new ArrayList<Map<String, String>>();
		if (response == null || !response.containsKey("items")) {
			return tracks;
		}

		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
		for (Map<String, Object> item : items) {
			Map<String, Object> track = (Map<String, Object>) item.get("track");
			if (track != null && track.get("name") != null) {
				tracks.add(Map.of(
						"name", (String) track.get("name"),
						"artist", extractArtistNames((List<Map<String, Object>>) track.get("artists"))));
			}
		}

		return tracks;
	}

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
