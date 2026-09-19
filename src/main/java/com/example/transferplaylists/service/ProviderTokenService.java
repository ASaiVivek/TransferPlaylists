package com.example.transferplaylists.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderTokenService {

	private static final String SESSION_PREFIX = "token_";

	private final HttpSession session;

	public ProviderTokenService(HttpSession session) {
		this.session = session;
	}

	public void saveToken(String provider, String token) {
		session.setAttribute(SESSION_PREFIX + provider, token);
	}

	public Optional<String> getToken(String provider) {
		return Optional.ofNullable((String) session.getAttribute(SESSION_PREFIX + provider));
	}

	public boolean isReady() {
		return getToken("spotify").isPresent() && getToken("google").isPresent();
	}

	public Map<String, Boolean> connectionStatus() {
		Map<String, Boolean> status = new LinkedHashMap<>();
		status.put("spotify", getToken("spotify").isPresent());
		status.put("google", getToken("google").isPresent());
		status.put("ready", isReady());
		return status;
	}
}
