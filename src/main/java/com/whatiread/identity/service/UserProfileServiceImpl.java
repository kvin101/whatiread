package com.whatiread.identity.service;

import com.whatiread.identity.api.UserProfileDto;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.social.service.BlockService;
import com.whatiread.social.service.FriendshipService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final FriendshipService friendshipService;
    private final BlockService blockService;

    public UserProfileServiceImpl(
            UserRepository userRepository,
            FriendshipService friendshipService,
            BlockService blockService
    ) {
        this.userRepository = userRepository;
        this.friendshipService = friendshipService;
        this.blockService = blockService;
    }

    @Override
    public UserProfileDto getProfile(UUID profileUserId, UUID viewerId) {
        User user = userRepository.findById(profileUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean self = profileUserId.equals(viewerId);
        boolean blocked = !self && blockService.isBlockedEitherWay(viewerId, profileUserId);
        boolean friend = !self && !blocked && friendshipService.areFriends(viewerId, profileUserId);
        boolean blockedByViewer = !self && blockService.hasBlocked(viewerId, profileUserId);
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.isWriter(),
                user.getWriterBio(),
                friend,
                self,
                blocked,
                blockedByViewer
        );
    }
}
