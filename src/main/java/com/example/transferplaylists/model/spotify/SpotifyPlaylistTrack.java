package com.example.transferplaylists.model.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyPlaylistTrack(SpotifyTrack track) {
}
