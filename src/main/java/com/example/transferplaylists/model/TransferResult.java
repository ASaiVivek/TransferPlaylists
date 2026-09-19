package com.example.transferplaylists.model;

import java.util.ArrayList;
import java.util.List;

public class TransferResult {

	private int playlistsTransferred;
	private int tracksMatched;
	private int tracksSkipped;
	private final List<String> errors = new ArrayList<>();

	public void incrementPlaylistsTransferred() {
		playlistsTransferred++;
	}

	public void incrementTracksMatched() {
		tracksMatched++;
	}

	public void incrementTracksSkipped() {
		tracksSkipped++;
	}

	public void addError(String error) {
		errors.add(error);
	}

	public int getPlaylistsTransferred() {
		return playlistsTransferred;
	}

	public int getTracksMatched() {
		return tracksMatched;
	}

	public int getTracksSkipped() {
		return tracksSkipped;
	}

	public List<String> getErrors() {
		return errors;
	}
}
