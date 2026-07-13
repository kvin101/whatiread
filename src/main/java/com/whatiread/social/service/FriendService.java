package com.whatiread.social.service;

import com.whatiread.social.api.BlockedUserDto;
import com.whatiread.social.api.CreateFriendRequestRequest;
import com.whatiread.social.api.FriendRequestDto;
import com.whatiread.social.api.FriendSummaryDto;
import java.util.List;
import java.util.UUID;

public interface FriendService {

    FriendRequestDto sendRequest(UUID requesterId, CreateFriendRequestRequest request);

    List<FriendRequestDto> listIncoming(UUID userId);

    List<FriendRequestDto> listOutgoing(UUID userId);

    FriendRequestDto accept(UUID userId, UUID requestId);

    FriendRequestDto decline(UUID userId, UUID requestId);

    void cancelRequest(UUID userId, UUID requestId);

    List<FriendSummaryDto> listFriends(UUID userId);

    void unfriend(UUID userId, UUID friendUserId);

    void block(UUID userId, UUID blockedUserId);

    void unblock(UUID userId, UUID blockedUserId);

    List<BlockedUserDto> listBlocked(UUID userId);
}
