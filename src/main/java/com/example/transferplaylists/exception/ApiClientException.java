package com.example.transferplaylists.exception;

public class ApiClientException extends RuntimeException {

	private final String provider;
	private final int statusCode;

	public ApiClientException(String provider, String message) {
		this(provider, message, 0);
	}

	public ApiClientException(String provider, String message, int statusCode) {
		super(message);
		this.provider = provider;
		this.statusCode = statusCode;
	}

	public String getProvider() {
		return provider;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
