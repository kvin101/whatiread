package com.whatiread.shelf.service;

import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.domain.ShelfMemberRole;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ShelfMemberRepository;
import com.whatiread.social.service.FriendshipService;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShelfAccessService {

    private static final Set<ShelfMemberRole> EDIT_ROLES = EnumSet.of(
            ShelfMemberRole.OWNER, ShelfMemberRole.ADMIN, ShelfMemberRole.EDITOR
    );
    private static final Set<ShelfMemberRole> MANAGE_MEMBER_ROLES = EnumSet.of(
            ShelfMemberRole.OWNER, ShelfMemberRole.ADMIN
    );
    private static final Set<ShelfMemberRole> MANAGE_SHELF_ROLES = EnumSet.of(
            ShelfMemberRole.OWNER, ShelfMemberRole.ADMIN
    );

    private final ShelfMemberRepository shelfMemberRepository;
    private final FriendshipService friendshipService;

    public ShelfAccessService(
            ShelfMemberRepository shelfMemberRepository,
            FriendshipService friendshipService
    ) {
        this.shelfMemberRepository = shelfMemberRepository;
        this.friendshipService = friendshipService;
    }

    public Optional<ShelfMemberRole> roleFor(Shelf shelf, UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        if (shelf.getOwner().getId().equals(userId)) {
            return Optional.of(ShelfMemberRole.OWNER);
        }
        return shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), userId)
                .map(member -> member.getRole());
    }

    public boolean canView(Shelf shelf, UUID viewerId) {
        ShelfVisibility visibility = shelf.getVisibility();
        if (visibility == ShelfVisibility.SECRET) {
            return viewerId != null && shelf.getOwner().getId().equals(viewerId);
        }
        if (visibility == ShelfVisibility.PUBLIC) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        Optional<ShelfMemberRole> role = roleFor(shelf, viewerId);
        if (role.isPresent()) {
            return true;
        }
        if (visibility == ShelfVisibility.FRIENDS) {
            return friendshipService.areFriends(viewerId, shelf.getOwner().getId());
        }
        return false;
    }

    /**
     * Whether a shelf appears when browsing another user's profile.
     */
    public boolean appearsOnProfile(Shelf shelf, UUID viewerId) {
        if (shelf.getOwner().getId().equals(viewerId)) {
            return true;
        }
        ShelfVisibility visibility = shelf.getVisibility();
        if (visibility == ShelfVisibility.SECRET || visibility == ShelfVisibility.PRIVATE) {
            return false;
        }
        if (visibility == ShelfVisibility.PUBLIC) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        return friendshipService.areFriends(viewerId, shelf.getOwner().getId());
    }

    public void requireView(Shelf shelf, UUID viewerId) {
        if (!canView(shelf, viewerId)) {
            throw new com.whatiread.shared.exception.ForbiddenException("Shelf is not accessible");
        }
    }

    /**
     * Share-link viewers see all books on the shelf (viewer-equivalent access).
     */
    public boolean canViewShelfBookViaShareLink(ShelfBook shelfBook) {
        return true;
    }

    public boolean canViewShelfBook(ShelfBook shelfBook, UUID viewerId) {
        Shelf shelf = shelfBook.getShelf();
        ShelfVisibility effective = shelfBook.effectiveVisibility();
        if (effective == ShelfVisibility.PUBLIC) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        if (roleFor(shelf, viewerId).isPresent()) {
            return true;
        }
        if (effective == ShelfVisibility.FRIENDS) {
            return friendshipService.areFriends(viewerId, shelf.getOwner().getId());
        }
        return false;
    }

    public void requireEdit(Shelf shelf, UUID userId) {
        requireRole(shelf, userId, EDIT_ROLES, "Insufficient permission to modify shelf books");
    }

    public void requireManageShelf(Shelf shelf, UUID userId) {
        requireRole(shelf, userId, MANAGE_SHELF_ROLES, "Insufficient permission to modify shelf");
    }

    public void requireManageMembers(Shelf shelf, UUID userId) {
        requireRole(shelf, userId, MANAGE_MEMBER_ROLES, "Insufficient permission to manage members");
    }

    public void requireOwner(Shelf shelf, UUID userId) {
        requireRole(shelf, userId, EnumSet.of(ShelfMemberRole.OWNER), "Only the shelf owner can perform this action");
    }

    /**
     * Which shelf activity events the requester may see. Viewers and non-members see nothing; editors see book changes only; owners/admins see all.
     */
    public ShelfActivityScope activityScopeFor(Shelf shelf, UUID userId) {
        return roleFor(shelf, userId)
                .map(role -> switch (role) {
                    case OWNER, ADMIN -> ShelfActivityScope.ALL;
                    case EDITOR -> ShelfActivityScope.BOOK_CHANGES;
                    case VIEWER -> ShelfActivityScope.NONE;
                })
                .orElse(ShelfActivityScope.NONE);
    }

    private void requireRole(Shelf shelf, UUID userId, Set<ShelfMemberRole> allowed, String message) {
        ShelfMemberRole role = roleFor(shelf, userId)
                .orElseThrow(() -> new com.whatiread.shared.exception.ForbiddenException(message));
        if (!allowed.contains(role)) {
            throw new com.whatiread.shared.exception.ForbiddenException(message);
        }
    }
}
