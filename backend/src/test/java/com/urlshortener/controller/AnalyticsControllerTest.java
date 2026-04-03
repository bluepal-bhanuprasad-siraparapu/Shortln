package com.urlshortener.controller;

import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.AnalyticsService;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new UserDetailsImpl(
                1L,
                "Test User",
                "test@example.com",
                "password",
                1,
                0,
                "PRO",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void getOverallAnalytics_ReturnsOk() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClicks", 100L);
        when(analyticsService.getOverallAnalytics(anyLong(), anyLong(), anyBoolean())).thenReturn(stats);

        mockMvc.perform(get("/api/analytics/link/10/overall")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(100));
    }

    @Test
    void getGeoAnalytics_ReturnsOk() throws Exception {
        Map<String, Long> geo = new HashMap<>();
        geo.put("India", 50L);
        when(analyticsService.getGeoAnalytics(anyLong(), anyLong(), anyBoolean())).thenReturn(geo);

        mockMvc.perform(get("/api/analytics/link/10/geo")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.India").value(50));
    }

    @Test
    void getDashboardStats_ReturnsOk() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLinks", 5);
        when(analyticsService.getDashboardStats(anyLong(), anyBoolean())).thenReturn(stats);

        mockMvc.perform(get("/api/analytics/stats")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinks").value(5));
    }

    @Test
    void exportLinkAnalytics_ReturnsPdf() throws Exception {
        byte[] pdfContent = new byte[]{1, 2, 3};
        when(analyticsService.exportLinkAnalytics(anyLong(), anyLong(), anyBoolean())).thenReturn(pdfContent);

        mockMvc.perform(get("/api/analytics/link/10/export")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
