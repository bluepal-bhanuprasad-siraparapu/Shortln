package com.urlshortener.controller;

import com.urlshortener.entity.ShortLink;
import com.urlshortener.repository.ShortLinkRepository;
import com.urlshortener.security.JwtAuthenticationFilter;
import com.urlshortener.security.TestSecurityUtils;
import com.urlshortener.service.QrService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrController.class)
@AutoConfigureMockMvc(addFilters = false)
class QrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrService qrService;

    @MockBean
    private ShortLinkRepository shortLinkRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clear();
    }

    @Test
    void getQrCode_Success() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        ShortLink link = ShortLink.builder()
                .id(1L)
                .shortCode("abc12")
                .originalUrl("https://example.com")
                .userId(1L)
                .build();
        
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));
        
        byte[] qrBytes = new byte[]{1, 2, 3};
        when(qrService.generateQrCode(anyString(), anyInt(), anyInt())).thenReturn(qrBytes);

        mockMvc.perform(get("/api/links/1/qr")
                        .header("Host", "localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-abc12.png\""))
                .andExpect(content().bytes(qrBytes));
    }

    @Test
    void getQrCode_LinkNotFound() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/links/1/qr"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQrCode_AccessDenied() throws Exception {
        TestSecurityUtils.setupMockUser(2L, "User", "user@example.com", "USER");
        ShortLink link = ShortLink.builder()
                .id(1L)
                .userId(1L) // Owned by different user
                .build();
        
        when(shortLinkRepository.findById(1L)).thenReturn(Optional.of(link));

        mockMvc.perform(get("/api/links/1/qr"))
                .andExpect(status().isForbidden());
    }
}
