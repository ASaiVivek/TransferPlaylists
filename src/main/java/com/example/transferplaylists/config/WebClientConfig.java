package com.example.transferplaylists.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient.Builder webClientBuilder() {
		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(30))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient));
	}

	@Bean
	WebClient spotifyWebClient(
			WebClient.Builder webClientBuilder,
			@Value("${spotify.api.base-url:https://api.spotify.com/v1}") String baseUrl) {
		return webClientBuilder.clone().baseUrl(baseUrl).build();
	}

	@Bean
	WebClient youtubeWebClient(
			WebClient.Builder webClientBuilder,
			@Value("${youtube.api.base-url:https://www.googleapis.com/youtube/v3}") String baseUrl) {
		return webClientBuilder.clone().baseUrl(baseUrl).build();
	}
}
