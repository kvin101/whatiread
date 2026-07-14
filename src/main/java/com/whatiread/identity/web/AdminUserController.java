package com.whatiread.identity.web;

import com.whatiread.identity.api.AdminCreateUserRequest;
import com.whatiread.identity.api.AdminResetPasswordRequest;
import com.whatiread.identity.api.AdminSetUserEnabledRequest;
import com.whatiread.identity.api.AdminUserDto;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.identity.service.AdminUserService;
import com.whatiread.identity.suggest.AdminUserSuggestDto;
import com.whatiread.identity.suggest.UserSuggestService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_USERS)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserSuggestService userSuggestService;

    public AdminUserController(AdminUserService adminUserService, UserSuggestService userSuggestService) {
        this.adminUserService = adminUserService;
        this.userSuggestService = userSuggestService;
    }

    @GetMapping("/suggest")
    List<AdminUserSuggestDto> suggest(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return userSuggestService.suggestAdmin(query, limit);
    }

    @GetMapping
    Page<AdminUserDto> listUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminUserService.listUsers(q, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminUserDto createUser(
            @CurrentUserId UUID actingAdminId,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        return adminUserService.createUser(request, actingAdminId);
    }

    @PatchMapping("/{userId}/password")
    AdminUserDto resetPassword(
            @CurrentUserId UUID actingAdminId,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request
    ) {
        return adminUserService.resetPassword(userId, request.password(), actingAdminId);
    }

    @PatchMapping("/{userId}/enabled")
    AdminUserDto setEnabled(
            @CurrentUserId UUID actingAdminId,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminSetUserEnabledRequest request
    ) {
        return adminUserService.setEnabled(userId, request.enabled(), actingAdminId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteUser(@CurrentUserId UUID actingAdminId, @PathVariable UUID userId) {
        adminUserService.deleteUser(userId, actingAdminId);
    }
}
