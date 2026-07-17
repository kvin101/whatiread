package com.whatiread.shelf.service;

import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.shelf.api.AddShelfBookRequest;
import com.whatiread.shelf.api.AddShelfMemberRequest;
import com.whatiread.shelf.api.CloneShelfRequest;
import com.whatiread.shelf.api.CreateShelfRequest;
import com.whatiread.shelf.api.CreateShelfShareLinkRequest;
import com.whatiread.shelf.api.ExploreShelfDto;
import com.whatiread.shelf.api.SharedShelfDto;
import com.whatiread.shelf.api.ShelfBookDto;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.api.ShelfEventDto;
import com.whatiread.shelf.api.ShelfMemberDto;
import com.whatiread.shelf.api.ShelfReadingOverlapDto;
import com.whatiread.shelf.api.ShelfShareLinkDto;
import com.whatiread.shelf.api.SystemShelfDto;
import com.whatiread.shelf.api.UpdateShelfBookRequest;
import com.whatiread.shelf.api.UpdateShelfMemberRequest;
import com.whatiread.shelf.api.UpdateShelfRequest;
import com.whatiread.shelf.api.UnlockShelfRequest;
import com.whatiread.shelf.api.UnlockShelfResponse;
import com.whatiread.shelf.domain.Shelf;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShelfService {

    ShelfDto create(UUID userId, CreateShelfRequest request);

    List<ShelfDto> listMine(UUID userId);

    List<ShelfDto> listPublicByOwner(UUID ownerId);

    List<ShelfDto> listVisibleOnProfile(UUID ownerId, UUID viewerId);

    /**
     * Public, friends', and shared-with-me shelves from other users.
     */
    Page<ExploreShelfDto> exploreFeed(UUID viewerId, Pageable pageable);

    ShelfDto get(UUID userId, UUID shelfId);

    ShelfDto getPublic(UUID ownerId, String slug);

    ShelfDto update(UUID userId, UUID shelfId, UpdateShelfRequest request);

    UnlockShelfResponse unlock(UUID userId, UUID shelfId, UnlockShelfRequest request);

    void delete(UUID userId, UUID shelfId);

    List<ShelfMemberDto> listMembers(UUID userId, UUID shelfId);

    ShelfMemberDto addMember(UUID userId, UUID shelfId, AddShelfMemberRequest request);

    ShelfMemberDto updateMember(UUID userId, UUID shelfId, UUID memberUserId, UpdateShelfMemberRequest request);

    void removeMember(UUID userId, UUID shelfId, UUID memberUserId);

    Page<ShelfEventDto> listEvents(UUID userId, UUID shelfId, Pageable pageable);

    ShelfDto cloneShelf(UUID userId, UUID shelfId, CloneShelfRequest request);

    ShelfDto cloneForRecommendationRecipient(UUID recipientId, UUID recommenderId, UUID shelfId);

    ShelfDto getRecommendationPreview(UUID shelfId);

    Map<UUID, Integer> countBooksByShelfIds(Collection<UUID> shelfIds);

    ShelfDto cloneFromShare(UUID userId, UUID token, CloneShelfRequest request);

    List<ShelfShareLinkDto> listShareLinks(UUID userId, UUID shelfId);

    ShelfShareLinkDto createShareLink(UUID userId, UUID shelfId, CreateShelfShareLinkRequest request);

    void revokeShareLink(UUID userId, UUID shelfId, UUID linkId);

    SharedShelfDto getSharedShelf(UUID token);

    List<ShelfBookDto> listBooks(UUID userId, UUID shelfId, String unlockToken);

    List<ShelfBookDto> listPublicBooks(UUID ownerId, String slug);

    ShelfBookDto addBook(UUID userId, UUID shelfId, AddShelfBookRequest request, String unlockToken);

    ShelfBookDto updateBook(
            UUID userId,
            UUID shelfId,
            UUID userBookId,
            UpdateShelfBookRequest request,
            String unlockToken
    );

    void removeBook(UUID userId, UUID shelfId, UUID userBookId, String unlockToken);

    List<SystemShelfDto> listSystemShelves();

    Page<UserBookDto> listSystemShelfBooks(UUID userId, ReadingStatus status, Pageable pageable);

    boolean canViewShelf(UUID shelfId, UUID viewerId);

    UUID getShelfOwnerId(UUID shelfId);

    Shelf getPersistenceReference(UUID shelfId);

    boolean canViewUserBookViaShelf(UUID userBookId, UUID viewerId);

    boolean canViewBookViaShelf(UUID bookId, UUID viewerId);

    boolean hasUserBookOnShelf(UUID shelfId, UUID userBookId);

    java.util.Optional<UUID> findShelfIdByOwnerAndName(UUID ownerId, String name);

    Map<UUID, List<String>> getShelfNamesForUserBooks(UUID userId, List<UUID> userBookIds);

    List<ShelfReadingOverlapDto> listReadingOverlap(UUID userId, UUID shelfId);
}
