package com.urlshortener.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.urlshortener.dto.UserDto;
import com.urlshortener.entity.User;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<UserDto> getAllUsers() {
        log.info("Fetching all users from database");
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public UserDto updateUserStatus(Long id, int active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(active);
        User savedUser = userRepository.save(user);
        
        auditLogService.log("USER_STATUS_UPDATE", "User status changed to " + (active == 1 ? "ACTIVE" : "INACTIVE") + " for email: " + savedUser.getEmail(), null, null, null);
        
        return mapToDto(savedUser);
    }

    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(1);
        userRepository.save(user);
        
        auditLogService.log("USER_SOFT_DELETE", "User account soft-deleted for email: " + user.getEmail(), null, null, null);
    }

    public UserDto updateUserProfile(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (userDto.getName() != null) user.setName(userDto.getName());
        if (userDto.getEmail() != null) user.setEmail(userDto.getEmail());
        
        User savedUser = userRepository.save(user);
        auditLogService.log("USER_PROFILE_UPDATE", "User profile updated for email: " + savedUser.getEmail(), null, null, null);
        
        return mapToDto(savedUser);
    }

    public byte[] generateUsersPdf() {
        List<User> users = userRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setSize(18);

        Paragraph p = new Paragraph("User Data Report", font);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(p);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] {1.5f, 3.5f, 3.0f, 3.0f, 1.5f});
        table.setSpacingBefore(10);

        table.addCell("ID");
        table.addCell("Name");
        table.addCell("Email");
        table.addCell("Role");
        table.addCell("Status");

        for (User user : users) {
            table.addCell(String.valueOf(user.getId()));
            table.addCell(user.getName());
            table.addCell(user.getEmail());
            table.addCell(user.getRole().name());
            table.addCell(user.getActive() == 1 ? "Active" : "Inactive");
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateStatusForAllUsers(int active) {
        userRepository.updateAllUsersStatus(active);
        auditLogService.log("USER_BULK_STATUS_UPDATE", "Status updated to " + (active == 1 ? "ACTIVE" : "INACTIVE") + " for all users", null, null, null);
    }

    public UserDto getUserDtoById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user);
    }

    public UserDto updateUserPlan(Long id, com.urlshortener.entity.Plan plan) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPlan(plan);
        if (plan == com.urlshortener.entity.Plan.PRO) {
            user.setSubscriptionExpiry(java.time.LocalDateTime.now().plusYears(1));
            notificationService.notifyAdmins("User " + user.getName() + " (" + user.getEmail() + ") upgraded to PRO plan!", "SUBSCRIPTION");
        } else {
            user.setSubscriptionExpiry(null);
        }
        
        User savedUser = userRepository.save(user);
        auditLogService.log("USER_PLAN_UPDATE", "User plan updated to " + plan.name() + " for email: " + savedUser.getEmail(), null, null, null);
        
        return mapToDto(savedUser);
    }

    private UserDto mapToDto(User user) {
        // Check for subscription expiry
        if (user.getPlan() == com.urlshortener.entity.Plan.PRO && user.getSubscriptionExpiry() != null) {
            if (user.getSubscriptionExpiry().isBefore(java.time.LocalDateTime.now())) {
                user.setPlan(com.urlshortener.entity.Plan.FREE);
                user.setSubscriptionExpiry(null);
                userRepository.save(user); // Persistence of the reversion
            }
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .plan(user.getPlan().name())
                .active(user.getActive())
                .deleted(user.getDeleted())
                .createdAt(user.getCreatedAt())
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .build();
    }
}
