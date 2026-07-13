package com.whatiread.social.service;

import java.util.List;
import java.util.UUID;

public interface FriendshipService {

    boolean areFriends(UUID userId, UUID otherUserId);

    List<UUID> listFriendIds(UUID userId);
}
