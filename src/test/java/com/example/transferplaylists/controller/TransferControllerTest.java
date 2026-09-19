package com.example.transferplaylists.controller;

import com.example.transferplaylists.model.TransferResult;
import com.example.transferplaylists.service.PlaylistTransferService;
import com.example.transferplaylists.service.ProviderTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProviderTokenService providerTokenService;

	@MockBean
	private PlaylistTransferService playlistTransferService;

	@Test
	void statusIsPublic() throws Exception {
		when(providerTokenService.connectionStatus())
				.thenReturn(Map.of("spotify", false, "google", false, "ready", false));

		mockMvc.perform(get("/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ready").value(false));
	}

	@Test
	void transferRequiresBothProviders() throws Exception {
		when(providerTokenService.isReady()).thenReturn(false);
		when(providerTokenService.connectionStatus())
				.thenReturn(Map.of("spotify", true, "google", false, "ready", false));

		mockMvc.perform(post("/transfer"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void transferReturnsResultWhenReady() throws Exception {
		TransferResult result = new TransferResult();
		result.incrementPlaylistsTransferred();
		result.incrementTracksMatched();

		when(providerTokenService.isReady()).thenReturn(true);
		when(providerTokenService.getToken("spotify")).thenReturn(Optional.of("spotify-token"));
		when(providerTokenService.getToken("google")).thenReturn(Optional.of("google-token"));
		when(playlistTransferService.transfer("spotify-token", "google-token")).thenReturn(result);

		mockMvc.perform(post("/transfer"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.playlistsTransferred").value(1))
				.andExpect(jsonPath("$.tracksMatched").value(1));
	}
}
