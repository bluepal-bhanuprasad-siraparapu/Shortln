package com.urlshortener.controller;

import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.entity.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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

    private UserDetailsImpl proUserDetails;
    private UserDetailsImpl freeUserDetails;
    private UserDetailsImpl adminUserDetails;

    @BeforeEach
    void setUp() {
        proUserDetails = new UserDetailsImpl(
                1L, "pro", "pro@example.com", "pass", 1, 0, Plan.PRO.name(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        freeUserDetails = new UserDetailsImpl(
                2L, "free", "free@example.com", "pass", 1, 0, Plan.FREE.name(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        adminUserDetails = new UserDetailsImpl(
                3L, "admin", "admin@example.com", "pass", 1, 0, Plan.PRO.name(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void getOverallAnalytics_ReturnsOk() throws Exception {
        when(analyticsService.getOverallAnalytics(anyLong(), anyLong(), anyBoolean()))
                .thenReturn(Map.of("totalClicks", 10L));

        mockMvc.perform(get("/api/analytics/link/1/overall")
                .with(user(proUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(10));
    }

    @Test
    void getDashboardStats_ReturnsOk() throws Exception {
        when(analyticsService.getDashboardStats(anyLong(), anyBoolean()))
                .thenReturn(Map.of("totalLinks", 5));

        mockMvc.perform(get("/api/analytics/stats")
                .with(user(proUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinks").value(5));
    }

    @Test
    void exportLinkAnalytics_ProUser_ReturnsPdf() throws Exception {
        byte[] pdf = "PDF CONTENT".getBytes();
        when(analyticsService.exportLinkAnalytics(anyLong(), anyLong(), anyBoolean())).thenReturn(pdf);

        mockMvc.perform(get("/api/analytics/link/1/export")
                .with(user(proUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=link_analytics_1.pdf"));
    }

    @Test
    void exportLinkAnalytics_FreeUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/link/1/export")
                .with(user(freeUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportAllAnalytics_Admin_ReturnsPdf() throws Exception {
        byte[] pdf = "ALL PDF".getBytes();
        when(analyticsService.exportAllAnalytics()).thenReturn(pdf);

        mockMvc.perform(get("/api/analytics/admin/export/all")
                .with(user(adminUserDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void getLatestEvents_Admin_ReturnsOk() throws Exception {
        when(analyticsService.getLatestClickEvents(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/analytics/admin/latest")
                .param("query", "test")
                .with(user(adminUserDetails)))
                .andExpect(status().isOk());
    }

    @Test
    void getLinkEvents_ReturnsOk() throws Exception {
        when(analyticsService.getLinkEvents(anyLong(), anyLong(), anyBoolean(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/analytics/link/1/events")
                .with(user(proUserDetails)))
                .andExpect(status().isOk());
    }
}
