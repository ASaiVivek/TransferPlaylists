package com.example.transferplaylists.proxies;

import com.example.transferplaylists.exception.ApiClientException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class YouTubeProxy {

	private static final Logger log = LoggerFactory.getLogger(YouTubeProxy.class);

	private final WebClient webClient;

	public YouTubeProxy(WebClient youtubeWebClient) {
		this.webClient = youtubeWebClient;
	}

	public Optional<String> searchVideo(String query, String accessToken) {
		try {
			YouTubeSearchResponse response = webClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search")
							.queryParam("part", "snippet")
							.queryParam("type", "video")
							.queryParam("maxResults", 1)
							.queryParam("q", query)
							.build())
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.bodyToMono(YouTubeSearchResponse.class)
					.block();

			return extractVideoId(response);
		} catch (WebClientResponseException e) {
			log.warn("YouTube search failed for query '{}': {} {}", query, e.getStatusCode(), e.getMessage());
			return Optional.empty();
		}
	}

	public Optional<String> createPlaylist(String title, String accessToken) {
		try {
			Map<String, Object> body = Map.of(
					"snippet", Map.of("title", title),
					"status", Map.of("privacyStatus", "private"));

			YouTubePlaylistResponse response = webClient.post()
					.uri(uriBuilder -> uriBuilder
							.path("/playlists")
							.queryParam("part", "snippet,status")
							.build())
					.header("Authorization", "Bearer " + accessToken)
					.bodyValue(body)
					.retrieve()
					.bodyToMono(YouTubePlaylistResponse.class)
					.block();

			if (response == null || response.id() == null) {
				return Optional.empty();
			}

			return Optional.of(response.id());
		} catch (WebClientResponseException e) {
			log.error("YouTube playlist creation failed for '{}': {} {}", title, e.getStatusCode(), e.getMessage());
			throw new ApiClientException("google", "Failed to create YouTube playlist", e.getStatusCode().value());
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
			log.warn("Failed to add video {} to playlist {}: {} {}", videoId, playlistId, e.getStatusCode(), e.getMessage());
			return false;
		}
	}

	private Optional<String> extractVideoId(YouTubeSearchResponse response) {
		if (response == null || response.items() == null || response.items().isEmpty()) {
			return Optional.empty();
		}

		YouTubeSearchItem item = response.items().get(0);
		if (item.id() == null || item.id().videoId() == null) {
			return Optional.empty();
		}

		return Optional.of(item.id().videoId());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record YouTubeSearchResponse(List<YouTubeSearchItem> items) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record YouTubeSearchItem(YouTubeVideoId id) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record YouTubeVideoId(String videoId) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record YouTubePlaylistResponse(String id) {
	}
}
