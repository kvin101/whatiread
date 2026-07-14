package com.whatiread.messaging.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.messaging.api.AddGroupMemberRequest;
import com.whatiread.messaging.api.ConversationDto;
import com.whatiread.messaging.api.CreateGroupConversationRequest;
import com.whatiread.messaging.api.MessageDto;
import com.whatiread.messaging.api.UpdateGroupConversationRequest;
import com.whatiread.messaging.service.MessagingService;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CONVERSATIONS)
public class ConversationController {

    private final MessagingService messagingService;

    public ConversationController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    List<ConversationDto> listConversations(@CurrentUserId UUID userId) {
        return messagingService.listConversations(userId);
    }

    @GetMapping("/groups")
    List<ConversationDto> listGroups(@CurrentUserId UUID userId) {
        return messagingService.listGroupConversations(userId);
    }

    @PostMapping("/with/{friendUserId}")
    @ResponseStatus(HttpStatus.OK)
    ConversationDto getOrCreate(@CurrentUserId UUID userId, @PathVariable UUID friendUserId) {
        return messagingService.getOrCreateConversation(userId, friendUserId);
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationDto createGroup(
            @CurrentUserId UUID userId,
            @Valid @RequestBody CreateGroupConversationRequest request
    ) {
        return messagingService.createGroupConversation(userId, request.name(), request.memberUserIds());
    }

    @PutMapping("/{conversationId}")
    ConversationDto renameGroup(
            @CurrentUserId UUID userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateGroupConversationRequest request
    ) {
        return messagingService.renameGroupConversation(userId, conversationId, request.name());
    }

    @PostMapping("/{conversationId}/members")
    ConversationDto addMember(
            @CurrentUserId UUID userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return messagingService.addGroupMember(userId, conversationId, request.userId());
    }

    @DeleteMapping("/{conversationId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(
            @CurrentUserId UUID userId,
            @PathVariable UUID conversationId,
            @PathVariable UUID memberUserId
    ) {
        messagingService.removeGroupMember(userId, conversationId, memberUserId);
    }

    @PostMapping("/{conversationId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leaveGroup(@CurrentUserId UUID userId, @PathVariable UUID conversationId) {
        messagingService.leaveGroupConversation(userId, conversationId);
    }

    @GetMapping("/unread-count")
    long unreadCount(@CurrentUserId UUID userId) {
        return messagingService.countUnreadMessages(userId);
    }

    @GetMapping("/{conversationId}/messages")
    CursorPage<MessageDto> listMessages(
            @CurrentUserId UUID userId,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return messagingService.listMessages(userId, conversationId, cursor, limit);
    }

    @PostMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@CurrentUserId UUID userId, @PathVariable UUID conversationId) {
        messagingService.markConversationRead(userId, conversationId);
    }
}
