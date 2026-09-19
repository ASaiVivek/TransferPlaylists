package com.example.transferplaylists.config;

import com.example.transferplaylists.service.ProviderTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final ProviderTokenService providerTokenService;
	private final OAuth2AuthorizedClientService authorizedClientService;

	public SecurityConfig(
			ProviderTokenService providerTokenService,
			OAuth2AuthorizedClientService authorizedClientService) {
		this.providerTokenService = providerTokenService;
		this.authorizedClientService = authorizedClientService;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/status", "/error").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2.successHandler(saveTokenAndRedirect()))
				.logout(logout -> logout.logoutSuccessUrl("/status"));

		return http.build();
	}

	private AuthenticationSuccessHandler saveTokenAndRedirect() {
		return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
			if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
				OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
						oauthToken.getAuthorizedClientRegistrationId(),
						oauthToken.getName());

				if (client != null) {
					providerTokenService.saveToken(
							oauthToken.getAuthorizedClientRegistrationId(),
							client.getAccessToken().getTokenValue());
				}
			}

			response.sendRedirect("/status");
		};
	}
}
