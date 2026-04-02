package com.urlshortener.service;

import com.urlshortener.dto.UserDto;
import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.USER)
                .plan(Plan.FREE)
                .active(1)
                .deleted(0)
                .build();
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_ReturnsListOfUserDtos() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("john@example.com", result.get(0).getEmail());
    }

    @Test
    void getAllUsers_EmptyRepository_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- getUserDtoById ---

    @Test
    void getUserDtoById_ValidId_ReturnsUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserDtoById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUserDtoById_InvalidId_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDtoById(999L));
    }

    // --- updateUserStatus ---

    @Test
    void updateUserStatus_Deactivate_UpdatesActiveFlag() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUserStatus(1L, 0);

        assertEquals(0, result.getActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUserStatus_Activate_UpdatesActiveFlag() {
        testUser.setActive(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUserStatus(1L, 1);

        assertEquals(1, result.getActive());
    }

    @Test
    void updateUserStatus_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserStatus(99L, 0));
    }

    // --- softDeleteUser ---

    @Test
    void softDeleteUser_ValidId_SetsDeletedFlag() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.softDeleteUser(1L);

        assertEquals(1, testUser.getDeleted());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void softDeleteUser_InvalidId_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.softDeleteUser(99L));
    }

    // --- updateUserProfile ---

    @Test
    void updateUserProfile_ValidData_UpdatesNameAndEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto dto = UserDto.builder().name("Jane Doe").email("jane@example.com").build();
        UserDto result = userService.updateUserProfile(1L, dto);

        assertEquals("Jane Doe", result.getName());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void updateUserProfile_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserDto dto = UserDto.builder().name("Jane").build();
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserProfile(99L, dto));
    }

    // --- updateUserPlan ---

    @Test
    void updateUserPlan_UpgradeToPro_SetsSubscriptionExpiry() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUserPlan(1L, Plan.PRO);

        assertEquals("PRO", result.getPlan());
        assertNotNull(testUser.getSubscriptionExpiry());
        verify(notificationService, times(1)).notifyAdmins(anyString(), eq("SUBSCRIPTION"));
    }

    @Test
    void updateUserPlan_DowngradeToFree_ClearsSubscriptionExpiry() {
        testUser.setPlan(Plan.PRO);
        testUser.setSubscriptionExpiry(LocalDateTime.now().plusMonths(6));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUserPlan(1L, Plan.FREE);

        assertNull(testUser.getSubscriptionExpiry());
    }

    @Test
    void updateUserPlan_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserPlan(99L, Plan.PRO));
    }

    // --- Subscription auto-expiry (mapToDto) ---

    @Test
    void getAllUsers_ExpiredProUser_IsDowngradedToFree() {
        testUser.setPlan(Plan.PRO);
        testUser.setSubscriptionExpiry(LocalDateTime.now().minusDays(1)); // expired

        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        List<UserDto> result = userService.getAllUsers();

        assertEquals("FREE", result.get(0).getPlan());
        verify(userRepository, times(1)).save(testUser);
    }
}
