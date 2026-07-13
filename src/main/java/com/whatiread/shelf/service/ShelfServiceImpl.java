package com.whatiread.shelf.service;

import com.whatiread.catalog.domain.BookWorkMatcher;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.config.CacheConfig;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.domain.UserBook;
import com.whatiread.library.port.UserBookPersistencePort;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shared.util.SlugUtils;
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
import com.whatiread.shelf.api.ShelfShareLinkDto;
import com.whatiread.shelf.api.SystemShelfDto;
import com.whatiread.shelf.api.UpdateShelfBookRequest;
import com.whatiread.shelf.api.UpdateShelfMemberRequest;
import com.whatiread.shelf.api.UpdateShelfRequest;
import com.whatiread.shelf.domain.ExploreShelfSource;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.domain.ShelfEventType;
import com.whatiread.shelf.domain.ShelfMember;
import com.whatiread.shelf.domain.ShelfMemberRole;
import com.whatiread.shelf.domain.ShelfShareLink;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ShelfBookRepository;
import com.whatiread.shelf.repository.ShelfMemberRepository;
import com.whatiread.shelf.repository.ShelfRepository;
import com.whatiread.shelf.repository.ShelfShareLinkRepository;
import com.whatiread.social.service.FriendshipService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ShelfServiceImpl implements ShelfService {

    private final ShelfRepository shelfRepository;
    private final ShelfMemberRepository shelfMemberRepository;
    private final ShelfBookRepository shelfBookRepository;
    private final ShelfShareLinkRepository shelfShareLinkRepository;
    private final UserLookupService userLookupService;
    private final LibraryService libraryService;
    private final UserBookPersistencePort userBookPersistencePort;
    private final ShelfAccessService shelfAccessService;
    private final FriendshipService friendshipService;
    private final ShelfEventService shelfEventService;
    private final ExploreShelfReadModelService exploreShelfReadModelService;
    private final BusinessMetrics businessMetrics;
    private final CacheManager cacheManager;

    public ShelfServiceImpl(
            ShelfRepository shelfRepository,
            ShelfMemberRepository shelfMemberRepository,
            ShelfBookRepository shelfBookRepository,
            ShelfShareLinkRepository shelfShareLinkRepository,
            UserLookupService userLookupService,
            LibraryService libraryService,
            UserBookPersistencePort userBookPersistencePort,
            ShelfAccessService shelfAccessService,
            FriendshipService friendshipService,
            ShelfEventService shelfEventService,
            ExploreShelfReadModelService exploreShelfReadModelService,
            BusinessMetrics businessMetrics,
            CacheManager cacheManager
    ) {
        this.shelfRepository = shelfRepository;
        this.shelfMemberRepository = shelfMemberRepository;
        this.shelfBookRepository = shelfBookRepository;
        this.shelfShareLinkRepository = shelfShareLinkRepository;
        this.userLookupService = userLookupService;
        this.libraryService = libraryService;
        this.userBookPersistencePort = userBookPersistencePort;
        this.shelfAccessService = shelfAccessService;
        this.friendshipService = friendshipService;
        this.shelfEventService = shelfEventService;
        this.exploreShelfReadModelService = exploreShelfReadModelService;
        this.businessMetrics = businessMetrics;
        this.cacheManager = cacheManager;
    }

    private static String formatDisplayName(User user) {
        String name = user.getDisplayName();
        if (name == null || name.isBlank()) {
            name = user.getFirstName();
            if (user.getLastName() != null && !user.getLastName().isBlank()) {
                name = name != null && !name.isBlank() ? name + " " + user.getLastName() : user.getLastName();
            }
        }
        if (name == null || name.isBlank()) {
            return "Reader";
        }
        return name;
    }

    private static String formatStatusLabel(ReadingStatus status) {
        return switch (status) {
            case TO_READ -> "To Read";
            case READING -> "Reading";
            case READ -> "Read";
            case DNF -> "Did Not Finish";
        };
    }

    private void syncExploreModel(Shelf shelf) {
        exploreShelfReadModelService.sync(shelf);
    }

    @Override
    public ShelfDto create(UUID userId, CreateShelfRequest request) {
        User owner = userLookupService.getPersistenceReference(userId);
        String slug = uniqueSlug(owner.getId(), request.name());
        int sortOrder = nextSortOrder(owner.getId());

        Shelf shelf = new Shelf(owner, request.name().trim(), slug);
        shelf.setVisibility(request.visibilityOrDefault());
        if (StringUtils.hasText(request.description())) {
            shelf.setDescription(request.description().trim());
        }
        if (StringUtils.hasText(request.icon())) {
            shelf.setIcon(request.icon().trim());
        }
        shelf.setSortOrder(sortOrder);
        shelfRepository.save(shelf);

        shelfMemberRepository.save(new ShelfMember(shelf, owner, ShelfMemberRole.OWNER, null));
        shelfEventService.record(shelf, userId, ShelfEventType.SHELF_CREATED, Map.of("name", shelf.getName()));
        syncExploreModel(shelf);
        businessMetrics.recordShelfCreated();
        return toDto(shelf, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfDto> listMine(UUID userId) {
        return shelfMemberRepository.findShelvesForUser(userId).stream()
                .map(shelf -> toDto(shelf, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfDto> listPublicByOwner(UUID ownerId) {
        return shelfRepository.findByOwner_IdAndVisibilityOrderBySortOrderAsc(ownerId, ShelfVisibility.PUBLIC).stream()
                .map(shelf -> toDto(shelf, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfDto> listVisibleOnProfile(UUID ownerId, UUID viewerId) {
        if (ownerId.equals(viewerId)) {
            return listMine(viewerId);
        }
        return shelfRepository.findByOwner_IdOrderBySortOrderAsc(ownerId).stream()
                .filter(shelf -> shelfAccessService.appearsOnProfile(shelf, viewerId))
                .map(shelf -> toDto(shelf, viewerId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExploreShelfDto> exploreFeed(UUID viewerId, Pageable pageable) {
        List<UUID> friendIds = friendshipService.listFriendIds(viewerId);
        return shelfRepository
                .findExploreFeed(viewerId, friendIds.size(), friendIds, pageable)
                .map(shelf -> toExploreDto(shelf, resolveExploreSource(shelf, viewerId)));
    }

    private ExploreShelfSource resolveExploreSource(Shelf shelf, UUID viewerId) {
        boolean isMember = shelfMemberRepository
                .findByShelf_IdAndUser_Id(shelf.getId(), viewerId)
                .isPresent();
        if (isMember) {
            return ExploreShelfSource.SHARED;
        }
        if (shelf.getVisibility() == ShelfVisibility.FRIENDS) {
            return ExploreShelfSource.FRIEND;
        }
        return ExploreShelfSource.PUBLIC;
    }

    @Override
    @Transactional(readOnly = true)
    public ShelfDto get(UUID userId, UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireView(shelf, userId);
        return toDto(shelf, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PUBLIC_SHELF, key = "#ownerId + ':' + #slug")
    public ShelfDto getPublic(UUID ownerId, String slug) {
        Shelf shelf = shelfRepository.findByOwner_IdAndSlug(ownerId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found"));
        if (shelf.getVisibility() != ShelfVisibility.PUBLIC) {
            throw new ForbiddenException("Shelf is not public");
        }
        return toDto(shelf, null);
    }

    @Override
    public ShelfDto update(UUID userId, UUID shelfId, UpdateShelfRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageShelf(shelf, userId);

        if (request.name() != null && !request.name().isBlank()) {
            shelf.setName(request.name().trim());
        }
        ShelfVisibility previousVisibility = shelf.getVisibility();
        if (request.visibility() != null) {
            shelf.setVisibility(request.visibility());
        }
        if (request.description() != null) {
            shelf.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        if (request.icon() != null) {
            shelf.setIcon(request.icon().isBlank() ? null : request.icon().trim());
        }
        if (request.sortOrder() != null) {
            shelf.setSortOrder(request.sortOrder());
        }
        Shelf saved = shelfRepository.save(shelf);
        if (request.visibility() != null && request.visibility() != previousVisibility) {
            shelfEventService.record(
                    saved, userId, ShelfEventType.VISIBILITY_CHANGED, Map.of(
                            "from", previousVisibility.name(),
                            "to", saved.getVisibility().name()
                    ));
        } else {
            shelfEventService.record(saved, userId, ShelfEventType.SHELF_UPDATED, Map.of());
        }
        syncExploreModel(saved);
        evictPublicShelfCache(saved.getOwner().getId(), saved.getSlug());
        return toDto(saved, userId);
    }

    @Override
    public void delete(UUID userId, UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireOwner(shelf, userId);
        evictPublicShelfCache(shelf.getOwner().getId(), shelf.getSlug());
        UUID shelfIdToRemove = shelf.getId();
        shelfRepository.delete(shelf);
        exploreShelfReadModelService.remove(shelfIdToRemove);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfMemberDto> listMembers(UUID userId, UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireView(shelf, userId);
        return shelfMemberRepository.findByShelf_Id(shelfId).stream()
                .map(this::toMemberDto)
                .toList();
    }

    @Override
    public ShelfMemberDto addMember(UUID userId, UUID shelfId, AddShelfMemberRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);

        if (request.role() == ShelfMemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role via API");
        }
        if (request.userId().equals(shelf.getOwner().getId())) {
            throw new ConflictException("Shelf owner is already a member");
        }
        if (shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, request.userId()).isPresent()) {
            throw new ConflictException("User is already a shelf member");
        }

        User member = userLookupService.getPersistenceReference(request.userId());
        ShelfMember shelfMember = new ShelfMember(shelf, member, request.role(), userId);
        ShelfMember saved = shelfMemberRepository.save(shelfMember);
        shelfEventService.record(
                shelf, userId, ShelfEventType.MEMBER_ADDED, Map.of(
                        "memberName", formatDisplayName(member),
                        "role", request.role().name()
                ));
        return toMemberDto(saved);
    }

    @Override
    public ShelfMemberDto updateMember(UUID userId, UUID shelfId, UUID memberUserId, UpdateShelfMemberRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);
        if (request.role() == ShelfMemberRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role via API");
        }
        if (memberUserId.equals(shelf.getOwner().getId())) {
            throw new ForbiddenException("Cannot change owner role");
        }
        ShelfMember member = shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf member not found"));
        ShelfMemberRole previous = member.getRole();
        member.setRole(request.role());
        ShelfMember saved = shelfMemberRepository.save(member);
        shelfEventService.record(
                shelf, userId, ShelfEventType.MEMBER_ROLE_CHANGED, Map.of(
                        "memberName", formatDisplayName(member.getUser()),
                        "from", previous.name(),
                        "to", request.role().name()
                ));
        return toMemberDto(saved);
    }

    @Override
    public void removeMember(UUID userId, UUID shelfId, UUID memberUserId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);

        if (memberUserId.equals(shelf.getOwner().getId())) {
            throw new ForbiddenException("Cannot remove shelf owner");
        }

        ShelfMember member = shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf member not found"));
        shelfEventService.record(
                shelf, userId, ShelfEventType.MEMBER_REMOVED, Map.of(
                        "memberName", formatDisplayName(member.getUser())
                ));
        shelfMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShelfEventDto> listEvents(UUID userId, UUID shelfId, Pageable pageable) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireView(shelf, userId);
        return switch (shelfAccessService.activityScopeFor(shelf, userId)) {
            case NONE -> Page.empty(pageable);
            case BOOK_CHANGES -> shelfEventService.listByTypes(
                    shelfId, ShelfEventService.BOOK_CHANGE_TYPES, pageable);
            case ALL -> shelfEventService.list(shelfId, pageable);
        };
    }

    @Override
    public ShelfDto cloneShelf(UUID userId, UUID shelfId, CloneShelfRequest request) {
        Shelf source = getShelf(shelfId);
        shelfAccessService.requireView(source, userId);
        if (source.getOwner().getId().equals(userId)) {
            throw new ConflictException("You already own this shelf");
        }

        User owner = userLookupService.getPersistenceReference(userId);
        String slug = uniqueSlug(userId, request.name());
        int sortOrder = nextSortOrder(userId);

        Shelf clone = new Shelf(owner, request.name().trim(), slug);
        clone.setVisibility(request.visibilityOrDefault());
        clone.setDescription(source.getDescription());
        clone.setIcon(source.getIcon());
        clone.setSortOrder(sortOrder);
        shelfRepository.save(clone);
        shelfMemberRepository.save(new ShelfMember(clone, owner, ShelfMemberRole.OWNER, null));
        shelfEventService.record(
                clone, userId, ShelfEventType.SHELF_CREATED, Map.of(
                        "name", clone.getName(),
                        "clonedFrom", source.getId().toString()
                ));

        if (request.includeBooksOrDefault()) {
            List<ShelfBook> sourceBooks = shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId);
            int position = 0;
            for (ShelfBook sourceBook : sourceBooks) {
                if (!shelfAccessService.canViewShelfBook(sourceBook, userId)) {
                    continue;
                }
                UUID bookId = sourceBook.getUserBook().getBook().getId();
                UUID userBookId = libraryService.ensureInLibrary(userId, bookId).id();
                ShelfBook cloneBook = new ShelfBook(clone, userBookPersistencePort.getOwnedReference(userId, userBookId), position++, userId);
                shelfBookRepository.save(cloneBook);
            }
        }

        return toDto(clone, userId);
    }

    @Override
    public ShelfDto cloneForRecommendationRecipient(UUID recipientId, UUID recommenderId, UUID shelfId) {
        Shelf source = getShelf(shelfId);
        if (!source.getOwner().getId().equals(recommenderId)) {
            throw new ForbiddenException("Shelf does not belong to the recommender");
        }
        if (source.getOwner().getId().equals(recipientId)) {
            throw new ConflictException("You already own this shelf");
        }
        return cloneShelfInternal(recipientId, source, source.getName(), true, ShelfVisibility.PRIVATE);
    }

    @Override
    @Transactional(readOnly = true)
    public ShelfDto getRecommendationPreview(UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        return toDto(shelf, null);
    }

    private ShelfDto cloneShelfInternal(
            UUID recipientId,
            Shelf source,
            String name,
            boolean includeBooks,
            ShelfVisibility visibility
    ) {
        User owner = userLookupService.getPersistenceReference(recipientId);
        String slug = uniqueSlug(recipientId, name);
        int sortOrder = nextSortOrder(recipientId);

        Shelf clone = new Shelf(owner, name.trim(), slug);
        clone.setVisibility(visibility);
        clone.setDescription(source.getDescription());
        clone.setIcon(source.getIcon());
        clone.setSortOrder(sortOrder);
        shelfRepository.save(clone);
        shelfMemberRepository.save(new ShelfMember(clone, owner, ShelfMemberRole.OWNER, null));
        shelfEventService.record(
                clone, recipientId, ShelfEventType.SHELF_CREATED, Map.of(
                        "name", clone.getName(),
                        "clonedFrom", source.getId().toString()
                ));

        if (includeBooks) {
            UUID recommenderId = source.getOwner().getId();
            List<ShelfBook> sourceBooks = shelfBookRepository.findByShelf_IdOrderByPositionAsc(source.getId());
            int position = 0;
            for (ShelfBook sourceBook : sourceBooks) {
                if (!shelfAccessService.canViewShelfBook(sourceBook, recommenderId)) {
                    continue;
                }
                UUID bookId = sourceBook.getUserBook().getBook().getId();
                UUID userBookId = libraryService.ensureInLibrary(recipientId, bookId).id();
                ShelfBook cloneBook = new ShelfBook(
                        clone,
                        userBookPersistencePort.getOwnedReference(recipientId, userBookId),
                        position++,
                        recipientId
                );
                shelfBookRepository.save(cloneBook);
            }
        }

        return toDto(clone, recipientId);
    }

    @Override
    public ShelfDto cloneFromShare(UUID userId, UUID token, CloneShelfRequest request) {
        Shelf source = resolveShelfFromShareToken(token);
        if (source.getOwner().getId().equals(userId)) {
            throw new ConflictException("You already own this shelf");
        }

        User owner = userLookupService.getPersistenceReference(userId);
        String slug = uniqueSlug(userId, request.name());
        int sortOrder = nextSortOrder(userId);

        Shelf clone = new Shelf(owner, request.name().trim(), slug);
        clone.setVisibility(request.visibilityOrDefault());
        clone.setDescription(source.getDescription());
        clone.setIcon(source.getIcon());
        clone.setSortOrder(sortOrder);
        shelfRepository.save(clone);
        shelfMemberRepository.save(new ShelfMember(clone, owner, ShelfMemberRole.OWNER, null));
        shelfEventService.record(
                clone, userId, ShelfEventType.SHELF_CREATED, Map.of(
                        "name", clone.getName(),
                        "clonedFrom", source.getId().toString()
                ));

        if (request.includeBooksOrDefault()) {
            List<ShelfBook> sourceBooks = shelfBookRepository.findByShelf_IdOrderByPositionAsc(source.getId());
            int position = 0;
            for (ShelfBook sourceBook : sourceBooks) {
                if (!shelfAccessService.canViewShelfBookViaShareLink(sourceBook)) {
                    continue;
                }
                UUID bookId = sourceBook.getUserBook().getBook().getId();
                UUID userBookId = libraryService.ensureInLibrary(userId, bookId).id();
                ShelfBook cloneBook = new ShelfBook(
                        clone,
                        userBookPersistencePort.getOwnedReference(userId, userBookId),
                        position++,
                        userId
                );
                shelfBookRepository.save(cloneBook);
            }
        }

        return toDto(clone, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfShareLinkDto> listShareLinks(UUID userId, UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);
        return shelfShareLinkRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId).stream()
                .map(this::toShareLinkDto)
                .toList();
    }

    @Override
    public ShelfShareLinkDto createShareLink(UUID userId, UUID shelfId, CreateShelfShareLinkRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);
        User creator = userLookupService.getPersistenceReference(userId);
        Instant expiresAt = request != null ? request.expiresAt() : null;
        ShelfShareLink link = new ShelfShareLink(shelf, UUID.randomUUID(), creator, expiresAt);
        return toShareLinkDto(shelfShareLinkRepository.save(link));
    }

    @Override
    public void revokeShareLink(UUID userId, UUID shelfId, UUID linkId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireManageMembers(shelf, userId);
        ShelfShareLink link = shelfShareLinkRepository.findById(linkId)
                .filter(l -> l.getShelf().getId().equals(shelfId))
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        if (link.getRevokedAt() == null) {
            link.revoke();
            shelfShareLinkRepository.save(link);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SharedShelfDto getSharedShelf(UUID token) {
        Shelf shelf = resolveShelfFromShareToken(token);
        UUID ownerId = shelf.getOwner().getId();
        List<ShelfBookDto> books = shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelf.getId()).stream()
                .filter(shelfAccessService::canViewShelfBookViaShareLink)
                .map(entry -> toBookDto(entry, ownerId, null))
                .toList();
        return new SharedShelfDto(toDto(shelf, null), books);
    }

    private Shelf resolveShelfFromShareToken(UUID token) {
        ShelfShareLink link = shelfShareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));
        if (!link.isActive()) {
            throw new ForbiddenException("Share link is no longer valid");
        }
        return link.getShelf();
    }

    private ShelfShareLinkDto toShareLinkDto(ShelfShareLink link) {
        return new ShelfShareLinkDto(
                link.getId(),
                link.getToken(),
                link.getShelf().getId(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getRevokedAt(),
                link.isActive()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfBookDto> listBooks(UUID userId, UUID shelfId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireView(shelf, userId);
        UUID ownerId = shelf.getOwner().getId();
        return shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId).stream()
                .map(entry -> toBookDto(entry, ownerId, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfBookDto> listPublicBooks(UUID ownerId, String slug) {
        Shelf shelf = shelfRepository.findByOwner_IdAndSlug(ownerId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found"));
        if (shelf.getVisibility() != ShelfVisibility.PUBLIC) {
            throw new ForbiddenException("Shelf is not public");
        }
        UUID shelfOwnerId = shelf.getOwner().getId();
        return shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelf.getId()).stream()
                .filter(sb -> shelfAccessService.canViewShelfBook(sb, null))
                .map(entry -> toBookDto(entry, shelfOwnerId, null))
                .toList();
    }

    @Override
    public ShelfBookDto addBook(UUID userId, UUID shelfId, AddShelfBookRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireEdit(shelf, userId);

        UUID userBookId = resolveUserBookId(shelf, request);
        UserBook userBook = userBookPersistencePort.getOwnedReference(shelf.getOwner().getId(), userBookId);

        if (shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBookId)
                || shelfAlreadyHasWork(shelfId, userBook)) {
            throw new ConflictException("Book already on shelf");
        }

        int position = request.position() != null
                ? request.position()
                : shelfBookRepository.maxPosition(shelfId) + 1;

        ShelfBook shelfBook = new ShelfBook(shelf, userBook, position, userId);
        shelfBook.setVisibility(request.visibility());
        ShelfBook saved = shelfBookRepository.save(shelfBook);
        shelfEventService.record(
                shelf, userId, ShelfEventType.BOOK_ADDED, Map.of(
                        "bookTitle", userBook.getBook().getTitle()
                ));
        evictPublicShelfCacheIfPublic(shelf);
        return toBookDto(saved, shelf.getOwner().getId(), userId);
    }

    @Override
    public ShelfBookDto updateBook(UUID userId, UUID shelfId, UUID userBookId, UpdateShelfBookRequest request) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireEdit(shelf, userId);

        ShelfBook shelfBook = shelfBookRepository.findByShelf_IdAndUserBook_Id(shelfId, userBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not on shelf"));

        if (request.position() != null) {
            shelfBook.setPosition(request.position());
        }
        if (request.visibility() != null) {
            shelfBook.setVisibility(request.visibility());
        }
        return toBookDto(shelfBookRepository.save(shelfBook), shelf.getOwner().getId(), userId);
    }

    @Override
    public void removeBook(UUID userId, UUID shelfId, UUID userBookId) {
        Shelf shelf = getShelf(shelfId);
        shelfAccessService.requireEdit(shelf, userId);

        ShelfBook shelfBook = shelfBookRepository.findByShelf_IdAndUserBook_Id(shelfId, userBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not on shelf"));
        shelfEventService.record(
                shelf, userId, ShelfEventType.BOOK_REMOVED, Map.of(
                        "bookTitle", shelfBook.getUserBook().getBook().getTitle()
                ));
        evictPublicShelfCacheIfPublic(shelf);
        shelfBookRepository.delete(shelfBook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemShelfDto> listSystemShelves() {
        return Arrays.stream(ReadingStatus.values())
                .map(status -> new SystemShelfDto(status, formatStatusLabel(status)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserBookDto> listSystemShelfBooks(UUID userId, ReadingStatus status, Pageable pageable) {
        return libraryService.list(userId, status, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewShelf(UUID shelfId, UUID viewerId) {
        Shelf shelf = getShelf(shelfId);
        return shelfAccessService.canView(shelf, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getShelfOwnerId(UUID shelfId) {
        return getShelf(shelfId).getOwner().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Shelf getPersistenceReference(UUID shelfId) {
        return shelfRepository.getReferenceById(shelfId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewUserBookViaShelf(UUID userBookId, UUID viewerId) {
        return shelfBookRepository.findByUserBook_Id(userBookId).stream()
                .anyMatch(shelfBook -> shelfAccessService.canViewShelfBook(shelfBook, viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canViewBookViaShelf(UUID bookId, UUID viewerId) {
        return shelfBookRepository.findByBookId(bookId).stream()
                .anyMatch(shelfBook -> shelfAccessService.canViewShelfBook(shelfBook, viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserBookOnShelf(UUID shelfId, UUID userBookId) {
        return shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findShelfIdByOwnerAndName(UUID ownerId, String name) {
        return shelfRepository.findByOwner_IdAndNameIgnoreCase(ownerId, name.trim())
                .map(Shelf::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<String>> getShelfNamesForUserBooks(UUID userId, List<UUID> userBookIds) {
        if (userBookIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<String>> result = new HashMap<>();
        for (UUID userBookId : userBookIds) {
            result.put(userBookId, new java.util.ArrayList<>());
        }
        shelfBookRepository.findByUserBookIdsAndOwner(userId, userBookIds).forEach(shelfBook -> {
            result.computeIfAbsent(shelfBook.getUserBook().getId(), ignored -> new java.util.ArrayList<>())
                    .add(shelfBook.getShelf().getName());
        });
        return result;
    }

    private void evictPublicShelfCacheIfPublic(Shelf shelf) {
        if (shelf.getVisibility() == ShelfVisibility.PUBLIC) {
            evictPublicShelfCache(shelf.getOwner().getId(), shelf.getSlug());
        }
    }

    private void evictPublicShelfCache(UUID ownerId, String slug) {
        var cache = cacheManager.getCache(CacheConfig.PUBLIC_SHELF);
        if (cache != null) {
            cache.evict(ownerId + ":" + slug);
        }
    }

    private Shelf getShelf(UUID shelfId) {
        return shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found"));
    }

    private String uniqueSlug(UUID ownerId, String name) {
        String base = SlugUtils.slugify(name);
        String slug = base;
        int suffix = 2;
        while (shelfRepository.existsByOwner_IdAndSlug(ownerId, slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private int nextSortOrder(UUID ownerId) {
        return shelfRepository.findByOwner_IdOrderBySortOrderAsc(ownerId).stream()
                .mapToInt(Shelf::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private boolean shelfAlreadyHasWork(UUID shelfId, UserBook candidate) {
        return shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId).stream()
                .anyMatch(entry -> BookWorkMatcher.sameWork(
                        candidate.getBook().getTitle(),
                        candidate.getBook().getAuthors(),
                        entry.getUserBook().getBook().getTitle(),
                        entry.getUserBook().getBook().getAuthors()
                ));
    }

    private UUID resolveUserBookId(Shelf shelf, AddShelfBookRequest request) {
        if (request.userBookId() != null) {
            return request.userBookId();
        }
        if (request.bookId() != null) {
            return libraryService.ensureInLibrary(shelf.getOwner().getId(), request.bookId()).id();
        }
        throw new IllegalArgumentException("Either userBookId or bookId is required");
    }

    private ShelfDto toDto(Shelf shelf, UUID viewerId) {
        ShelfMemberRole role = shelfAccessService.roleFor(shelf, viewerId).orElse(null);
        return new ShelfDto(
                shelf.getId(),
                shelf.getName(),
                shelf.getSlug(),
                shelf.getDescription(),
                shelf.getIcon(),
                shelf.getVisibility(),
                shelf.getSortOrder(),
                shelf.getOwner().getId(),
                role,
                shelfBookRepository.countByShelf_Id(shelf.getId()),
                shelf.getCreatedAt(),
                shelf.getUpdatedAt(),
                formatDisplayName(shelf.getOwner())
        );
    }

    private ExploreShelfDto toExploreDto(Shelf shelf, ExploreShelfSource source) {
        User owner = shelf.getOwner();
        return new ExploreShelfDto(
                shelf.getId(),
                shelf.getName(),
                shelf.getSlug(),
                shelf.getDescription(),
                shelf.getIcon(),
                shelf.getVisibility(),
                source,
                shelfBookRepository.countByShelf_Id(shelf.getId()),
                owner.getId(),
                formatDisplayName(owner),
                shelf.getUpdatedAt()
        );
    }

    private ShelfMemberDto toMemberDto(ShelfMember member) {
        return new ShelfMemberDto(
                member.getId(),
                member.getUser().getId(),
                formatDisplayName(member.getUser()),
                member.getRole(),
                member.getInvitedBy(),
                member.getCreatedAt()
        );
    }

    private ShelfBookDto toBookDto(ShelfBook shelfBook, UUID ownerId, UUID viewerId) {
        UUID userBookId = shelfBook.getUserBook().getId();
        UserBookDto userBook = viewerId != null && viewerId.equals(ownerId)
                ? libraryService.get(ownerId, userBookId)
                : libraryService.getSharedView(ownerId, userBookId);
        return new ShelfBookDto(
                shelfBook.getUserBook().getId(),
                userBook,
                shelfBook.getPosition(),
                shelfBook.getVisibility(),
                shelfBook.effectiveVisibility(),
                shelfBook.getAddedBy(),
                shelfBook.getCreatedAt(),
                shelfBook.getUpdatedAt()
        );
    }
}
