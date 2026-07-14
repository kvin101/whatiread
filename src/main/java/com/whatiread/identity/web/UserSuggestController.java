package com.whatiread.identity.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.identity.suggest.UserSuggestDto;
import com.whatiread.identity.suggest.UserSuggestScope;
import com.whatiread.identity.suggest.UserSuggestService;
import com.whatiread.shared.web.ApiPaths;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USERS)
public class UserSuggestController {

    private final UserSuggestService userSuggestService;

    public UserSuggestController(UserSuggestService userSuggestService) {
        this.userSuggestService = userSuggestService;
    }

    @GetMapping("/suggest")
    List<UserSuggestDto> suggest(
            @CurrentUserId UUID viewerId,
            @RequestParam("q") String query,
            @RequestParam(value = "scope", defaultValue = "invite") String scope,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return userSuggestService.suggest(viewerId, query, parseScope(scope), limit);
    }

    private static UserSuggestScope parseScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return UserSuggestScope.INVITE;
        }
        return switch (scope.trim().toLowerCase()) {
            case "friends" -> UserSuggestScope.FRIENDS;
            case "admin" -> UserSuggestScope.ADMIN;
            default -> UserSuggestScope.INVITE;
        };
    }
}
