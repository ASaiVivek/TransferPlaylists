package com.example.transferplaylists.proxies;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class YouTubeProxy {

	private final WebClient webClient;

	public YouTubeProxy(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl("https://www.googleapis.com/youtube/v3").build();
	}

	public Optional<String> searchVideo(String query, String accessToken) {
		try {
			Map<String, Object> response = webClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search")
							.queryParam("part", "snippet")
							.queryParam("type", "video")
							.queryParam("maxResults", 1)
							.queryParam("q", query)
							.build())
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.bodyToMono(Map.class)
					.block();

			return extractVideoId(response);
		} catch (WebClientResponseException e) {
			System.err.println("Error searching YouTube: " + e.getMessage());
			return Optional.empty();
		}
	}

	public Optional<String> createPlaylist(String title, String accessToken) {
		try {
			Map<String, Object> body = Map.of(
					"snippet", Map.of("title", title),
					"status", Map.of("privacyStatus", "private"));

			Map<String, Object> response = webClient.post()
					.uri(uriBuilder -> uriBuilder
							.path("/playlists")
							.queryParam("part", "snippet,status")
							.build())
					.header("Authorization", "Bearer " + accessToken)
					.bodyValue(body)
					.retrieve()
					.bodyToMono(Map.class)
					.block();

			if (response == null || !response.containsKey("id")) {
				return Optional.empty();
			}

			return Optional.of((String) response.get("id"));
		} catch (WebClientResponseException e) {
			System.err.println("Error creating YouTube playlist: " + e.getMessage());
			return Optional.empty();
		}
	}

	public boolean addVideoToPlaylist(String playlistId, String videoId, String accessToken) {
		try {
			Map<String, Object> body = Map.of(
					"snippet", Map.of(
							"playlistId", playlistId,
							"resourceId", Map.of(
									"kind", "youtube#video",
									"videoId", videoId)));

			webClient.post()
					.uri(uriBuilder -> uriBuilder
							.path("/playlistItems")
							.queryParam("part", "snippet")
							.build())
					.header("Authorization", "Bearer " + accessToken)
					.bodyValue(body)
					.retrieve()
					.toBodilessEntity()
					.block();

			return true;
		} catch (WebClientResponseException e) {
			System.err.println("Error adding video to playlist: " + e.getMessage());
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<String> extractVideoId(Map<String, Object> response) {
		if (response == null || !response.containsKey("items")) {
			return Optional.empty();
		}

		List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
		if (items.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> id = (Map<String, Object>) items.get(0).get("id");
		if (id == null || !id.containsKey("videoId")) {
			return Optional.empty();
		}

		return Optional.of((String) id.get("videoId"));
	}
}
