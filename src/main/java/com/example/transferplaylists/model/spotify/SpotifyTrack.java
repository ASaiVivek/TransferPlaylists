package com.example.transferplaylists.model.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyTrack(String name, List<SpotifyArtist> artists) {
}
