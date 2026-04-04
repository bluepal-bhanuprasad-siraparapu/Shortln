package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.LinkRequest;
import com.urlshortener.dto.LinkResponse;
import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.LinkService;
import com.urlshortener.security.JwtTokenProvider;
import com.urlshortener.security.UserDetailsServiceImpl;
import com.urlshortener.security.AuthEntryPointJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LinkService linkService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDetailsImpl userDetails;
    private LinkRequest linkRequest;
    private LinkResponse linkResponse;

    @BeforeEach
    void setUp() {
        userDetails = new UserDetailsImpl(
                1L,
                "Test User",
                "test@example.com",
                "password",
                1,
                0,
                "FREE",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        linkRequest = new LinkRequest();
        linkRequest.setOriginalUrl("https://google.com");
        linkRequest.setTitle("Google");

        linkResponse = new LinkResponse();
        linkResponse.setId(10L);
        linkResponse.setOriginalUrl("https://google.com");
        linkResponse.setShortCode("abc123");
        linkResponse.setTitle("Google");
    }

    @Test
    void createLink_ReturnsOk() throws Exception {
        when(linkService.createLink(any(LinkRequest.class), anyLong())).thenReturn(linkResponse);

        mockMvc.perform(post("/api/links")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc123"));
    }

    @Test
    void getLinkById_ReturnsOk() throws Exception {
        when(linkService.getLinkById(anyLong(), anyLong(), anyBoolean())).thenReturn(linkResponse);

        mockMvc.perform(get("/api/links/10")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deleteLink_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/links/10")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void toggleLinkStatus_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/links/10/status")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
