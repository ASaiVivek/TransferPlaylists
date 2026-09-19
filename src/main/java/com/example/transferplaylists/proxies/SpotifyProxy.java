package com.example.transferplaylists.proxies;

import com.example.transferplaylists.exception.ApiClientException;
import com.example.transferplaylists.model.TrackInfo;
import com.example.transferplaylists.model.spotify.SpotifyPage;
import com.example.transferplaylists.model.spotify.SpotifyPlaylist;
import com.example.transferplaylists.model.spotify.SpotifyPlaylistTrack;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SpotifyProxy {

	private static final Logger log = LoggerFactory.getLogger(SpotifyProxy.class);
	private static final Pattern PLAYLIST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{10,}$");

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	public SpotifyProxy(WebClient spotifyWebClient, ObjectMapper objectMapper) {
		this.webClient = spotifyWebClient;
		this.objectMapper = objectMapper;
	}

	public List<SpotifyPlaylist> getPlaylists(String accessToken) {
		var playlists = new ArrayList<SpotifyPlaylist>();
		String nextPath = "/me/playlists?limit=50";

		while (nextPath != null) {
			SpotifyPage<SpotifyPlaylist> page = fetchPage(accessToken, nextPath, SpotifyPlaylist.class);
			if (page.items() != null) {
				playlists.addAll(page.items());
			}
			nextPath = extractNextPath(page.next());
		}

		return playlists;
	}

	public List<TrackInfo> getPlaylistTracks(String playlistId, String accessToken) {
		validatePlaylistId(playlistId);

		var tracks = new ArrayList<TrackInfo>();
		String nextPath = "/playlists/" + playlistId + "/tracks?limit=100";

		while (nextPath != null) {
			SpotifyPage<SpotifyPlaylistTrack> page = fetchPage(accessToken, nextPath, SpotifyPlaylistTrack.class);
			if (page.items() != null) {
				page.items().stream()
						.map(SpotifyPlaylistTrack::track)
						.filter(track -> track != null && track.name() != null)
						.map(track -> new TrackInfo(track.name(), extractArtistNames(track.artists())))
						.forEach(tracks::add);
			}
			nextPath = extractNextPath(page.next());
		}

		return tracks;
	}

	private <T> SpotifyPage<T> fetchPage(String accessToken, String uri, Class<T> itemType) {
		try {
			String body = webClient.get()
					.uri(uri)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.bodyToMono(String.class)
					.block();

			JavaType pageType = objectMapper.getTypeFactory()
					.constructParametricType(SpotifyPage.class, itemType);
			return objectMapper.readValue(body, pageType);
		} catch (WebClientResponseException e) {
			log.error("Spotify API request failed: {} {}", e.getStatusCode(), e.getMessage());
			throw new ApiClientException("spotify", "Failed to fetch data from Spotify", e.getStatusCode().value());
		} catch (Exception e) {
			log.error("Failed to parse Spotify response", e);
			throw new ApiClientException("spotify", "Failed to parse Spotify response");
		}
	}

	private String extractNextPath(String next) {
		if (next == null || next.isBlank()) {
			return null;
		}
		return next.replace("https://api.spotify.com/v1", "");
	}

	private String extractArtistNames(List<com.example.transferplaylists.model.spotify.SpotifyArtist> artists) {
		if (artists == null || artists.isEmpty()) {
			return "Unknown Artist";
		}

		return artists.stream()
				.map(com.example.transferplaylists.model.spotify.SpotifyArtist::name)
				.filter(name -> name != null && !name.isBlank())
				.reduce((left, right) -> left + ", " + right)
				.orElse("Unknown Artist");
	}

	private void validatePlaylistId(String playlistId) {
		if (playlistId == null || !PLAYLIST_ID_PATTERN.matcher(playlistId).matches()) {
			throw new IllegalArgumentException("Invalid Spotify playlist id");
		}
	}
}
