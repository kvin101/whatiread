package com.whatiread.social.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.social.api.BlockedUserDto;
import com.whatiread.social.api.CreateFriendRequestRequest;
import com.whatiread.social.api.FriendRequestDto;
import com.whatiread.social.api.FriendSummaryDto;
import com.whatiread.social.service.FriendService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.FRIENDS)
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    FriendRequestDto sendRequest(@CurrentUserId UUID userId, @Valid @RequestBody CreateFriendRequestRequest request) {
        return friendService.sendRequest(userId, request);
    }

    @GetMapping("/requests/incoming")
    List<FriendRequestDto> listIncoming(@CurrentUserId UUID userId) {
        return friendService.listIncoming(userId);
    }

    @GetMapping("/requests/outgoing")
    List<FriendRequestDto> listOutgoing(@CurrentUserId UUID userId) {
        return friendService.listOutgoing(userId);
    }

    @PostMapping("/requests/{requestId}/accept")
    FriendRequestDto accept(@CurrentUserId UUID userId, @PathVariable UUID requestId) {
        return friendService.accept(userId, requestId);
    }

    @PostMapping("/requests/{requestId}/decline")
    FriendRequestDto decline(@CurrentUserId UUID userId, @PathVariable UUID requestId) {
        return friendService.decline(userId, requestId);
    }

    @DeleteMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelRequest(@CurrentUserId UUID userId, @PathVariable UUID requestId) {
        friendService.cancelRequest(userId, requestId);
    }

    @GetMapping
    List<FriendSummaryDto> listFriends(@CurrentUserId UUID userId) {
        return friendService.listFriends(userId);
    }

    @GetMapping("/blocked")
    List<BlockedUserDto> listBlocked(@CurrentUserId UUID userId) {
        return friendService.listBlocked(userId);
    }

    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfriend(@CurrentUserId UUID userId, @PathVariable UUID friendUserId) {
        friendService.unfriend(userId, friendUserId);
    }

    @PostMapping("/{blockedUserId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void block(@CurrentUserId UUID userId, @PathVariable UUID blockedUserId) {
        friendService.block(userId, blockedUserId);
    }

    @DeleteMapping("/{blockedUserId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unblock(@CurrentUserId UUID userId, @PathVariable UUID blockedUserId) {
        friendService.unblock(userId, blockedUserId);
    }
}
