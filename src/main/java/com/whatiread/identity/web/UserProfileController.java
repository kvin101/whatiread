package com.whatiread.identity.web;

import com.whatiread.identity.api.UserProfileDto;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.identity.service.UserIdentityResolver;
import com.whatiread.identity.service.UserProfileService;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.service.ShelfService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USERS)
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final ShelfService shelfService;
    private final UserIdentityResolver userIdentityResolver;

    public UserProfileController(
            UserProfileService userProfileService,
            ShelfService shelfService,
            UserIdentityResolver userIdentityResolver
    ) {
        this.userProfileService = userProfileService;
        this.shelfService = shelfService;
        this.userIdentityResolver = userIdentityResolver;
    }

    @GetMapping("/{userRef}/profile")
    UserProfileDto profile(@PathVariable String userRef, @CurrentUserId UUID viewerId) {
        UUID userId = userIdentityResolver.resolveUserId(userRef);
        return userProfileService.getProfile(userId, viewerId);
    }

    @GetMapping("/{userRef}/shelves")
    List<ShelfDto> listShelves(@PathVariable String userRef, @CurrentUserId UUID viewerId) {
        UUID userId = userIdentityResolver.resolveUserId(userRef);
        return shelfService.listVisibleOnProfile(userId, viewerId);
    }
}
