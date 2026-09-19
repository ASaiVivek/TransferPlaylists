package com.example.transferplaylists.service;

import com.example.transferplaylists.exception.ApiClientException;
import com.example.transferplaylists.model.TrackInfo;
import com.example.transferplaylists.model.TransferResult;
import com.example.transferplaylists.model.spotify.SpotifyPlaylist;
import com.example.transferplaylists.proxies.SpotifyProxy;
import com.example.transferplaylists.proxies.YouTubeProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistTransferServiceTest {

	@Mock
	private SpotifyProxy spotifyProxy;

	@Mock
	private YouTubeProxy youTubeProxy;

	@InjectMocks
	private PlaylistTransferService playlistTransferService;

	@Test
	void transferCreatesPlaylistAndAddsMatchedTracks() {
		when(spotifyProxy.getPlaylists("spotify-token"))
				.thenReturn(List.of(new SpotifyPlaylist("playlist1", "My Playlist")));
		when(youTubeProxy.createPlaylist("My Playlist", "youtube-token"))
				.thenReturn(Optional.of("yt-playlist"));
		when(spotifyProxy.getPlaylistTracks("playlist1", "spotify-token"))
				.thenReturn(List.of(new TrackInfo("Song", "Artist")));
		when(youTubeProxy.searchVideo("Artist Song", "youtube-token"))
				.thenReturn(Optional.of("video-id"));
		when(youTubeProxy.addVideoToPlaylist("yt-playlist", "video-id", "youtube-token"))
				.thenReturn(true);

		TransferResult result = playlistTransferService.transfer("spotify-token", "youtube-token");

		assertEquals(1, result.getPlaylistsTransferred());
		assertEquals(1, result.getTracksMatched());
		assertEquals(0, result.getTracksSkipped());
		assertEquals(0, result.getErrors().size());
		verify(youTubeProxy).addVideoToPlaylist("yt-playlist", "video-id", "youtube-token");
	}

	@Test
	void transferRecordsSpotifyFailure() {
		when(spotifyProxy.getPlaylists("spotify-token"))
				.thenThrow(new ApiClientException("spotify", "Unauthorized", 401));

		TransferResult result = playlistTransferService.transfer("spotify-token", "youtube-token");

		assertEquals(0, result.getPlaylistsTransferred());
		assertFalse(result.getErrors().isEmpty());
	}
}
