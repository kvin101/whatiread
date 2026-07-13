package com.whatiread.social.service;

import com.whatiread.social.api.BlockedUserDto;
import java.util.List;
import java.util.UUID;

public interface BlockService {

    void block(UUID blockerId, UUID blockedUserId);

    void unblock(UUID blockerId, UUID blockedUserId);

    boolean isBlockedEitherWay(UUID userId, UUID otherUserId);

    boolean hasBlocked(UUID blockerId, UUID blockedUserId);

    List<BlockedUserDto> listBlockedUsers(UUID blockerId);
}
