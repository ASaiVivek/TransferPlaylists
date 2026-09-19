package com.example.transferplaylists.controller;

import com.example.transferplaylists.model.TransferResult;
import com.example.transferplaylists.service.PlaylistTransferService;
import com.example.transferplaylists.service.ProviderTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class TransferController {

	private final ProviderTokenService providerTokenService;
	private final PlaylistTransferService playlistTransferService;

	public TransferController(
			ProviderTokenService providerTokenService,
			PlaylistTransferService playlistTransferService) {
		this.providerTokenService = providerTokenService;
		this.playlistTransferService = playlistTransferService;
	}

	@GetMapping("/status")
	public Map<String, Object> status() {
		return new LinkedHashMap<>(providerTokenService.connectionStatus());
	}

	@PostMapping("/transfer")
	public ResponseEntity<?> transfer() {
		if (!providerTokenService.isReady()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
					"error", "Connect both Spotify and Google before transferring",
					"status", providerTokenService.connectionStatus()));
		}

		String spotifyToken = providerTokenService.getToken("spotify").orElseThrow();
		String youtubeToken = providerTokenService.getToken("google").orElseThrow();
		TransferResult result = playlistTransferService.transfer(spotifyToken, youtubeToken);
		return ResponseEntity.ok(result);
	}
}
