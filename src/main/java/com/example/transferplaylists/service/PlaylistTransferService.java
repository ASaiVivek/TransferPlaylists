package com.example.transferplaylists.service;

import com.example.transferplaylists.exception.ApiClientException;
import com.example.transferplaylists.model.TrackInfo;
import com.example.transferplaylists.model.TransferResult;
import com.example.transferplaylists.model.spotify.SpotifyPlaylist;
import com.example.transferplaylists.proxies.SpotifyProxy;
import com.example.transferplaylists.proxies.YouTubeProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistTransferService {

	private static final Logger log = LoggerFactory.getLogger(PlaylistTransferService.class);

	private final SpotifyProxy spotifyProxy;
	private final YouTubeProxy youTubeProxy;

	public PlaylistTransferService(SpotifyProxy spotifyProxy, YouTubeProxy youTubeProxy) {
		this.spotifyProxy = spotifyProxy;
		this.youTubeProxy = youTubeProxy;
	}

	public TransferResult transfer(String spotifyToken, String youtubeToken) {
		var result = new TransferResult();

		try {
			List<SpotifyPlaylist> playlists = spotifyProxy.getPlaylists(spotifyToken);
			for (SpotifyPlaylist playlist : playlists) {
				transferPlaylist(playlist, spotifyToken, youtubeToken, result);
			}
		} catch (ApiClientException e) {
			log.error("Spotify transfer failed", e);
			result.addError("Failed to fetch Spotify playlists: " + e.getMessage());
		}

		return result;
	}

	private void transferPlaylist(
			SpotifyPlaylist playlist,
			String spotifyToken,
			String youtubeToken,
			TransferResult result) {
		if (playlist.id() == null || playlist.name() == null) {
			result.addError("Skipped playlist with missing id or name");
			return;
		}

		try {
			var youtubePlaylistId = youTubeProxy.createPlaylist(playlist.name(), youtubeToken);
			if (youtubePlaylistId.isEmpty()) {
				result.addError("Failed to create YouTube playlist: " + playlist.name());
				return;
			}

			result.incrementPlaylistsTransferred();
			transferTracks(playlist.id(), youtubePlaylistId.get(), spotifyToken, youtubeToken, result);
		} catch (ApiClientException e) {
			result.addError("Failed to create YouTube playlist '" + playlist.name() + "': " + e.getMessage());
		}
	}

	private void transferTracks(
			String spotifyPlaylistId,
			String youtubePlaylistId,
			String spotifyToken,
			String youtubeToken,
			TransferResult result) {
		try {
			List<TrackInfo> tracks = spotifyProxy.getPlaylistTracks(spotifyPlaylistId, spotifyToken);

			for (TrackInfo track : tracks) {
				String query = track.artist() + " " + track.name();
				var videoId = youTubeProxy.searchVideo(query, youtubeToken);

				if (videoId.isEmpty()) {
					result.incrementTracksSkipped();
					continue;
				}

				if (youTubeProxy.addVideoToPlaylist(youtubePlaylistId, videoId.get(), youtubeToken)) {
					result.incrementTracksMatched();
				} else {
					result.incrementTracksSkipped();
				}
			}
		} catch (ApiClientException e) {
			result.addError("Failed to fetch tracks for playlist " + spotifyPlaylistId + ": " + e.getMessage());
		} catch (IllegalArgumentException e) {
			result.addError("Invalid playlist id: " + spotifyPlaylistId);
		}
	}
}
