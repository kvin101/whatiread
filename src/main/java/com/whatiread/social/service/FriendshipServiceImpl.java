package com.whatiread.social.service;

import com.whatiread.social.repository.FriendshipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public FriendshipServiceImpl(FriendshipRepository friendshipRepository) {
        this.friendshipRepository = friendshipRepository;
    }

    @Override
    public boolean areFriends(UUID userId, UUID otherUserId) {
        if (userId == null || otherUserId == null || userId.equals(otherUserId)) {
            return false;
        }
        return friendshipRepository.existsByUser_IdAndFriend_Id(userId, otherUserId);
    }

    @Override
    public List<UUID> listFriendIds(UUID userId) {
        return friendshipRepository.findFriendIdsByUserId(userId);
    }
}
