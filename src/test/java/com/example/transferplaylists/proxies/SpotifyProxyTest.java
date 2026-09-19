package com.example.transferplaylists.proxies;

import com.example.transferplaylists.exception.ApiClientException;
import com.example.transferplaylists.model.TrackInfo;
import com.example.transferplaylists.model.spotify.SpotifyPlaylist;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpotifyProxyTest {

	private MockWebServer server;
	private SpotifyProxy spotifyProxy;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();

		WebClient webClient = WebClient.builder()
				.baseUrl(server.url("/").toString())
				.build();
		spotifyProxy = new SpotifyProxy(webClient, new ObjectMapper());
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void getPlaylistsPaginatesAndParsesPlaylists() {
		server.enqueue(new MockResponse()
				.setBody("{\"items\":[{\"id\":\"p1\",\"name\":\"Road Trip\"}],\"next\":\"https://api.spotify.com/v1/me/playlists?offset=50\"}"));
		server.enqueue(new MockResponse()
				.setBody("{\"items\":[{\"id\":\"p2\",\"name\":\"Chill\"}],\"next\":null}"));

		List<SpotifyPlaylist> playlists = spotifyProxy.getPlaylists("token");

		assertEquals(2, playlists.size());
		assertEquals("Road Trip", playlists.get(0).name());
		assertEquals("Chill", playlists.get(1).name());
	}

	@Test
	void getPlaylistTracksParsesArtistsAndTracks() {
		server.enqueue(new MockResponse().setBody(
				"{\"items\":[{\"track\":{\"name\":\"Song One\",\"artists\":[{\"name\":\"Artist A\"},{\"name\":\"Artist B\"}]}}],\"next\":null}"));

		List<TrackInfo> tracks = spotifyProxy.getPlaylistTracks("playlist123456", "token");

		assertEquals(1, tracks.size());
		assertEquals("Song One", tracks.get(0).name());
		assertEquals("Artist A, Artist B", tracks.get(0).artist());
	}

	@Test
	void getPlaylistTracksRejectsInvalidPlaylistId() {
		assertThrows(IllegalArgumentException.class, () -> spotifyProxy.getPlaylistTracks("../bad", "token"));
	}

	@Test
	void getPlaylistsThrowsOnApiFailure() {
		server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_token\"}"));

		assertThrows(ApiClientException.class, () -> spotifyProxy.getPlaylists("bad-token"));
	}
}
