package com.example.transferplaylists.service;

import com.example.transferplaylists.model.TransferResult;
import com.example.transferplaylists.proxies.SpotifyProxy;
import com.example.transferplaylists.proxies.YouTubeProxy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlaylistTransferService {

	private final SpotifyProxy spotifyProxy;
	private final YouTubeProxy youTubeProxy;

	public PlaylistTransferService(SpotifyProxy spotifyProxy, YouTubeProxy youTubeProxy) {
		this.spotifyProxy = spotifyProxy;
		this.youTubeProxy = youTubeProxy;
	}

	public TransferResult transfer(String spotifyToken, String youtubeToken) {
		var result = new TransferResult();
		List<Map<String, Object>> playlists = spotifyProxy.getPlaylists(spotifyToken);

		for (Map<String, Object> playlist : playlists) {
			String playlistId = (String) playlist.get("id");
			String playlistName = extractPlaylistName(playlist);

			if (playlistId == null || playlistName == null) {
				result.addError("Skipped playlist with missing id or name");
				continue;
			}

			var youtubePlaylistId = youTubeProxy.createPlaylist(playlistName, youtubeToken);
			if (youtubePlaylistId.isEmpty()) {
				result.addError("Failed to create YouTube playlist: " + playlistName);
				continue;
			}

			result.incrementPlaylistsTransferred();
			transferTracks(playlistId, youtubePlaylistId.get(), spotifyToken, youtubeToken, result);
		}

		return result;
	}

	private void transferTracks(
			String spotifyPlaylistId,
			String youtubePlaylistId,
			String spotifyToken,
			String youtubeToken,
			TransferResult result) {
		List<Map<String, String>> tracks = spotifyProxy.getPlaylistTracks(spotifyPlaylistId, spotifyToken);

		for (Map<String, String> track : tracks) {
			String query = track.get("artist") + " " + track.get("name");
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
	}

	private String extractPlaylistName(Map<String, Object> playlist) {
		Object name = playlist.get("name");
		return name instanceof String ? (String) name : null;
	}
}
