package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.UserDto;
import com.urlshortener.security.JwtAuthenticationFilter;
import com.urlshortener.security.TestSecurityUtils;
import com.urlshortener.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .id(1L)
                .name("testuser")
                .email("testuser@example.com")
                .active(1)
                .build();
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clear();
    }

    @Test
    void getProfile_AdminCanViewAnyProfile() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(userService.getUserDtoById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("testuser"));
    }

    @Test
    void getProfile_UserCannotViewOthersProfile() throws Exception {
        TestSecurityUtils.setupMockUser(2L, "Other User", "other@example.com", "USER");
        
        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportUsersPdf_ReturnsPdf() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        byte[] pdfContent = new byte[]{1, 2, 3};
        when(userService.generateUsersPdf()).thenReturn(pdfContent);

        mockMvc.perform(get("/api/user/admin/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=users_report.pdf"));
    }

    @Test
    void getAllProfiles_ReturnsList() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(userService.getAllUsers()).thenReturn(Arrays.asList(userDto));

        mockMvc.perform(get("/api/user/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("testuser"));
    }

    @Test
    void updateAllStatus_Success() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        mockMvc.perform(put("/api/user/admin/status-all")
                        .param("active", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All users status updated to active"));

        verify(userService).updateStatusForAllUsers(1);
    }

    @Test
    void updateSingleStatus_Success() throws Exception {
        TestSecurityUtils.setupMockAdmin();
        when(userService.updateUserStatus(1L, 1)).thenReturn(userDto);

        mockMvc.perform(put("/api/user/admin/1/status")
                        .param("active", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testuser"));
    }
}
