package com.urlshortener.controller;

import com.urlshortener.service.RedirectService;
import com.urlshortener.security.JwtTokenProvider;
import com.urlshortener.security.UserDetailsServiceImpl;
import com.urlshortener.security.AuthEntryPointJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;
import com.urlshortener.config.SecurityConfig;

@WebMvcTest(RedirectController.class)
@Import(SecurityConfig.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedirectService redirectService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @Test
    void redirect_PointsToOriginalUrl() throws Exception {
        String shortCode = "abc123";
        String originalUrl = "https://google.com";

        when(redirectService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        doNothing().when(redirectService).trackClick(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(get("/" + shortCode))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(originalUrl));
    }
}
