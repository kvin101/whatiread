package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
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
import com.whatiread.shelf.api.AddShelfBookRequest;
import com.whatiread.shelf.api.AddShelfMemberRequest;
import com.whatiread.shelf.api.CloneShelfRequest;
import com.whatiread.shelf.api.CreateShelfRequest;
import com.whatiread.shelf.api.SharedShelfDto;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.api.UpdateShelfMemberRequest;
import com.whatiread.shelf.api.UpdateShelfRequest;
import com.whatiread.shelf.domain.ExploreShelfSource;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.domain.ShelfMember;
import com.whatiread.shelf.domain.ShelfMemberRole;
import com.whatiread.shelf.domain.ShelfShareLink;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ShelfBookRepository;
import com.whatiread.shelf.repository.ShelfMemberRepository;
import com.whatiread.shelf.repository.ShelfRepository;
import com.whatiread.shelf.repository.ShelfShareLinkRepository;
import com.whatiread.social.service.FriendshipService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShelfServiceImplTest {


    private static final String READING_LIST = "reading-list";
    private static final String HASH = "hash";
    private static final String USER = "User";
    private static final String READING_LIST_2 = "Reading List";
    private static final String DUNE = "Dune";
    private static final String MEMBER_EXAMPLE_COM = "member@example.com";
    private static final String MEMBER = "Member";
    private static final String V_2024_01_01T00_00_00Z = "2024-01-01T00:00:00Z";
    private static final String V_2024_01_02T00_00_00Z = "2024-01-02T00:00:00Z";
    private static final String CLONER_EXAMPLE_COM = "cloner@example.com";
    private static final String FRANK_HERBERT = "Frank Herbert";
    private static final String CLONER = "Cloner";
    @Mock
    private ShelfRepository shelfRepository;
    @Mock
    private ShelfMemberRepository shelfMemberRepository;
    @Mock
    private ShelfBookRepository shelfBookRepository;
    @Mock
    private ShelfShareLinkRepository shelfShareLinkRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private LibraryService libraryService;
    @Mock
    private UserBookPersistencePort userBookPersistencePort;
    @Mock
    private ShelfAccessService shelfAccessService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private ShelfEventService shelfEventService;
    @Mock
    private ShelfCloneService shelfCloneService;
    @Mock
    private ExploreShelfReadModelService exploreShelfReadModelService;
    @Mock
    private BusinessMetrics businessMetrics;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private ShelfServiceImpl shelfService;

    private UUID userId;
    private UUID shelfId;
    private User owner;
    private Shelf shelf;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                Field createdAt = findField(type, "createdAt");
                if (createdAt != null) {
                    createdAt.setAccessible(true);
                    createdAt.set(entity, Instant.parse(V_2024_01_01T00_00_00Z));
                }
                Field updatedAt = findField(type, "updatedAt");
                if (updatedAt != null) {
                    updatedAt.setAccessible(true);
                    updatedAt.set(entity, Instant.parse(V_2024_01_02T00_00_00Z));
                }
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on " + entity.getClass());
    }

    private static Field findField(Class<?> type, String name) {
        while (type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        shelfId = UUID.randomUUID();
        owner = new User("owner@example.com", "owner", HASH, "Owner", USER);
        setId(owner, userId);
        shelf = new Shelf(owner, READING_LIST_2, READING_LIST);
        setId(shelf, shelfId);
        shelf.setVisibility(ShelfVisibility.PRIVATE);
    }

    @Test
    void createPersistsShelfAndOwnerMembership() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);
        when(shelfRepository.existsByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(userId)).thenReturn(List.of());
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> {
            Shelf saved = invocation.getArgument(0);
            setId(saved, shelfId);
            return saved;
        });
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        ShelfDto created = shelfService.create(
                userId, new CreateShelfRequest(READING_LIST_2, "desc", "📚", ShelfVisibility.PUBLIC));

        assertThat(created.name()).isEqualTo(READING_LIST_2);
        verify(shelfMemberRepository).save(any());
        verify(businessMetrics).recordShelfCreated();
        verify(exploreShelfReadModelService).sync(any(Shelf.class));
    }

    @Test
    void getPublicRejectsNonPublicShelf() {
        when(shelfRepository.findByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.getPublic(userId, READING_LIST))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Shelf is not public");
    }

    @Test
    void getPublicReturnsPublicShelf() {
        shelf.setVisibility(ShelfVisibility.PUBLIC);
        when(shelfRepository.findByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(2L);

        ShelfDto dto = shelfService.getPublic(userId, READING_LIST);

        assertThat(dto.visibility()).isEqualTo(ShelfVisibility.PUBLIC);
        assertThat(dto.bookCount()).isEqualTo(2);
    }

    @Test
    void addMemberRejectsOwnerRole() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.addMember(
                userId, shelfId, new AddShelfMemberRequest(UUID.randomUUID(), ShelfMemberRole.OWNER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMemberRejectsDuplicateOwner() {
        UUID memberId = UUID.randomUUID();
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.addMember(
                userId, shelfId, new AddShelfMemberRequest(userId, ShelfMemberRole.EDITOR)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Shelf owner is already a member");
    }

    @Test
    void listSystemShelvesReturnsAllStatuses() {
        assertThat(shelfService.listSystemShelves())
                .hasSize(4)
                .extracting(dto -> dto.status().name())
                .containsExactly("TO_READ", "READING", "READ", "DNF");
    }

    @Test
    void deleteRequiresOwnerAndRemovesExploreModel() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(cacheManager.getCache(CacheConfig.PUBLIC_SHELF)).thenReturn(cache);

        shelfService.delete(userId, shelfId);

        verify(shelfRepository).delete(shelf);
        verify(exploreShelfReadModelService).remove(shelfId);
    }

    @Test
    void updateRecordsVisibilityChange() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfRepository.save(shelf)).thenReturn(shelf);
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);
        when(cacheManager.getCache(CacheConfig.PUBLIC_SHELF)).thenReturn(cache);

        shelfService.update(
                userId, shelfId, new UpdateShelfRequest(
                        "New Name", null, null, ShelfVisibility.PUBLIC, null));

        verify(shelfEventService).record(eq(shelf), eq(userId), any(), any());
        verify(cache).evict(userId + ":reading-list");
    }

    @Test
    void addBookRejectsDuplicate() {
        UserBook userBook = new UserBook(owner, new com.whatiread.catalog.domain.Book(), ReadingStatus.TO_READ);
        setId(userBook, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBook.getId())).thenReturn(true);

        assertThatThrownBy(() -> shelfService.addBook(
                userId, shelfId, new AddShelfBookRequest(userBook.getId(), null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void exploreFeedMapsSharedSourceForMembers() {
        when(friendshipService.listFriendIds(userId)).thenReturn(List.of());
        when(shelfRepository.findExploreFeed(eq(userId), eq(0), eq(List.of()), any()))
                .thenReturn(new PageImpl<>(List.of(shelf)));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, userId))
                .thenReturn(Optional.of(new ShelfMember(shelf, owner, ShelfMemberRole.EDITOR, userId)));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(1L);

        Page<?> feed = shelfService.exploreFeed(userId, PageRequest.of(0, 10));

        assertThat(feed.getContent()).hasSize(1);
    }

    @Test
    void getThrowsWhenShelfMissing() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.get(userId, shelfId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cloneShelfCopiesAccessibleBooksForNonOwner() {
        UUID clonerId = UUID.randomUUID();
        User cloner = new User(CLONER_EXAMPLE_COM, "cloner", HASH, CLONER, USER);
        setId(cloner, clonerId);
        Book book = new Book();
        book.setTitle(DUNE);
        book.setAuthors(List.of(FRANK_HERBERT));
        setId(book, UUID.randomUUID());
        UserBook sourceUserBook = new UserBook(owner, book, ReadingStatus.READ);
        setId(sourceUserBook, UUID.randomUUID());
        ShelfBook sourceShelfBook = new ShelfBook(shelf, sourceUserBook, 0, userId);
        UserBook clonedUserBook = new UserBook(cloner, book, ReadingStatus.READ);
        UUID clonedUserBookId = UUID.randomUUID();
        setId(clonedUserBook, clonedUserBookId);

        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userLookupService.getPersistenceReference(clonerId)).thenReturn(cloner);
        when(shelfRepository.existsByOwner_IdAndSlug(clonerId, "cloned-list")).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(clonerId)).thenReturn(List.of());
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(sourceShelfBook));
        when(shelfAccessService.canViewShelfBook(sourceShelfBook, clonerId)).thenReturn(true);
        when(libraryService.ensureInLibrary(clonerId, book.getId())).thenReturn(userBookDto(clonedUserBookId, book));
        when(userBookPersistencePort.getOwnedReference(clonerId, clonedUserBookId)).thenReturn(clonedUserBook);
        when(shelfAccessService.roleFor(any(), eq(clonerId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(any())).thenReturn(1L);

        ShelfDto cloned = shelfService.cloneShelf(
                clonerId, shelfId, new CloneShelfRequest("Cloned List", true, ShelfVisibility.PRIVATE));

        assertThat(cloned.name()).isEqualTo("Cloned List");
        verify(shelfCloneService).cloneWithBooks(eq(shelf), any(Shelf.class), eq(clonerId), eq(true), any());
    }

    @Test
    void cloneShelfRejectsWhenViewerOwnsSource() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.cloneShelf(
                userId, shelfId, new CloneShelfRequest("Copy", true, ShelfVisibility.PRIVATE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You already own this shelf");
    }

    @Test
    void addBookPersistsNewEntry() {
        Book bookEntity = new Book();
        bookEntity.setTitle(DUNE);
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.TO_READ);
        setId(userBook, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userBookPersistencePort.getOwnedReference(userId, userBook.getId())).thenReturn(userBook);
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBook.getId())).thenReturn(false);
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of());
        when(shelfBookRepository.maxPosition(shelfId)).thenReturn(0);
        when(shelfBookRepository.save(any(ShelfBook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryService.get(userId, userBook.getId())).thenReturn(userBookDto(userBook.getId(), userBook.getBook()));

        var dto = shelfService.addBook(
                userId, shelfId, new AddShelfBookRequest(userBook.getId(), null, null, ShelfVisibility.PUBLIC));

        assertThat(dto.userBook().id()).isEqualTo(userBook.getId());
        verify(shelfEventService).record(eq(shelf), eq(userId), any(), any());
    }

    @Test
    void removeBookDeletesEntryAndRecordsEvent() {
        UserBook userBook = new UserBook(owner, new Book(), ReadingStatus.TO_READ);
        userBook.getBook().setTitle(DUNE);
        setId(userBook, UUID.randomUUID());
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelf_IdAndUserBook_Id(shelfId, userBook.getId()))
                .thenReturn(Optional.of(shelfBook));

        shelfService.removeBook(userId, shelfId, userBook.getId());

        verify(shelfBookRepository).delete(shelfBook);
        verify(shelfEventService).record(eq(shelf), eq(userId), any(), any());
    }

    @Test
    void listBooksMapsOwnerAndViewerEntries() {
        UserBook userBook = new UserBook(owner, new Book(), ReadingStatus.READING);
        setId(userBook, UUID.randomUUID());
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(shelfBook));
        when(libraryService.get(userId, userBook.getId())).thenReturn(userBookDto(userBook.getId(), userBook.getBook()));

        assertThat(shelfService.listBooks(userId, shelfId)).hasSize(1);
    }

    @Test
    void listMembersReturnsShelfMembership() {
        User member = new User(MEMBER_EXAMPLE_COM, "member", HASH, MEMBER, USER);
        UUID memberId = UUID.randomUUID();
        setId(member, memberId);
        ShelfMember shelfMember = new ShelfMember(shelf, member, ShelfMemberRole.EDITOR, userId);
        setId(shelfMember, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfMemberRepository.findByShelf_Id(shelfId)).thenReturn(List.of(shelfMember));

        assertThat(shelfService.listMembers(userId, shelfId))
                .singleElement()
                .extracting(dto -> dto.role())
                .isEqualTo(ShelfMemberRole.EDITOR);
    }

    @Test
    void addMemberPersistsEditor() {
        UUID memberId = UUID.randomUUID();
        User member = new User(MEMBER_EXAMPLE_COM, "member", HASH, MEMBER, USER);
        setId(member, memberId);
        ShelfMember saved = new ShelfMember(shelf, member, ShelfMemberRole.EDITOR, userId);
        setId(saved, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userLookupService.getPersistenceReference(memberId)).thenReturn(member);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberId)).thenReturn(Optional.empty());
        when(shelfMemberRepository.save(any(ShelfMember.class))).thenReturn(saved);

        var dto = shelfService.addMember(
                userId, shelfId, new AddShelfMemberRequest(memberId, ShelfMemberRole.EDITOR));

        assertThat(dto.role()).isEqualTo(ShelfMemberRole.EDITOR);
    }

    @Test
    void updateMemberChangesRole() {
        UUID memberId = UUID.randomUUID();
        User member = new User(MEMBER_EXAMPLE_COM, "member", HASH, MEMBER, USER);
        setId(member, memberId);
        ShelfMember shelfMember = new ShelfMember(shelf, member, ShelfMemberRole.EDITOR, userId);
        setId(shelfMember, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberId)).thenReturn(Optional.of(shelfMember));
        when(shelfMemberRepository.save(shelfMember)).thenReturn(shelfMember);

        var dto = shelfService.updateMember(
                userId, shelfId, memberId, new UpdateShelfMemberRequest(ShelfMemberRole.ADMIN));

        assertThat(dto.role()).isEqualTo(ShelfMemberRole.ADMIN);
    }

    @Test
    void removeMemberDeletesNonOwner() {
        UUID memberId = UUID.randomUUID();
        User member = new User(MEMBER_EXAMPLE_COM, "member", HASH, MEMBER, USER);
        setId(member, memberId);
        ShelfMember shelfMember = new ShelfMember(shelf, member, ShelfMemberRole.EDITOR, userId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberId)).thenReturn(Optional.of(shelfMember));

        shelfService.removeMember(userId, shelfId, memberId);

        verify(shelfMemberRepository).delete(shelfMember);
    }

    @Test
    void exploreFeedUsesFriendSourceForFriendsOnlyShelf() {
        shelf.setVisibility(ShelfVisibility.FRIENDS);
        when(friendshipService.listFriendIds(userId)).thenReturn(List.of());
        when(shelfRepository.findExploreFeed(eq(userId), eq(0), eq(List.of()), any()))
                .thenReturn(new PageImpl<>(List.of(shelf)));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, userId)).thenReturn(Optional.empty());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        Page<?> feed = shelfService.exploreFeed(userId, PageRequest.of(0, 10));

        assertThat(feed.getContent()).hasSize(1);
    }

    @Test
    void listMineReturnsMemberShelves() {
        when(shelfMemberRepository.findShelvesForUser(userId)).thenReturn(List.of(shelf));
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(1L);

        assertThat(shelfService.listMine(userId)).hasSize(1);
    }

    @Test
    void listPublicByOwnerReturnsPublicShelves() {
        shelf.setVisibility(ShelfVisibility.PUBLIC);
        when(shelfRepository.findByOwner_IdAndVisibilityOrderBySortOrderAsc(userId, ShelfVisibility.PUBLIC))
                .thenReturn(List.of(shelf));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        assertThat(shelfService.listPublicByOwner(userId)).hasSize(1);
    }

    @Test
    void listVisibleOnProfileReturnsOwnShelvesForOwner() {
        when(shelfMemberRepository.findShelvesForUser(userId)).thenReturn(List.of(shelf));
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        assertThat(shelfService.listVisibleOnProfile(userId, userId)).hasSize(1);
    }

    @Test
    void listVisibleOnProfileFiltersForViewer() {
        UUID viewerId = UUID.randomUUID();
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(userId)).thenReturn(List.of(shelf));
        when(shelfAccessService.appearsOnProfile(shelf, viewerId)).thenReturn(true);
        when(shelfAccessService.roleFor(any(), eq(viewerId))).thenReturn(Optional.empty());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        assertThat(shelfService.listVisibleOnProfile(userId, viewerId)).hasSize(1);
    }

    @Test
    void getReturnsAccessibleShelf() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        assertThat(shelfService.get(userId, shelfId).id()).isEqualTo(shelfId);
    }

    @Test
    void updateWithoutVisibilityChangeRecordsShelfUpdated() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfRepository.save(shelf)).thenReturn(shelf);
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        shelfService.update(userId, shelfId, new UpdateShelfRequest("Renamed", null, null, null, null));

        verify(shelfEventService).record(eq(shelf), eq(userId), any(), any());
    }

    @Test
    void listPublicBooksReturnsVisibleEntries() {
        shelf.setVisibility(ShelfVisibility.PUBLIC);
        Book bookEntity = new Book();
        bookEntity.setTitle(DUNE);
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.READ);
        setId(userBook, UUID.randomUUID());
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfRepository.findByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(shelfBook));
        when(shelfAccessService.canViewShelfBook(shelfBook, null)).thenReturn(true);
        when(libraryService.getSharedView(userId, userBook.getId()))
                .thenReturn(userBookDto(userBook.getId(), bookEntity));

        assertThat(shelfService.listPublicBooks(userId, READING_LIST)).hasSize(1);
    }

    @Test
    void updateBookChangesPositionAndVisibility() {
        Book bookEntity = new Book();
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.TO_READ);
        setId(userBook, UUID.randomUUID());
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelf_IdAndUserBook_Id(shelfId, userBook.getId()))
                .thenReturn(Optional.of(shelfBook));
        when(shelfBookRepository.save(shelfBook)).thenReturn(shelfBook);
        when(libraryService.get(userId, userBook.getId())).thenReturn(userBookDto(userBook.getId(), bookEntity));

        var dto = shelfService.updateBook(
                userId, shelfId, userBook.getId(),
                new com.whatiread.shelf.api.UpdateShelfBookRequest(3, ShelfVisibility.PUBLIC));

        assertThat(dto.position()).isEqualTo(3);
    }

    @Test
    void addBookByBookIdEnsuresLibraryEntry() {
        UUID bookId = UUID.randomUUID();
        Book bookEntity = new Book();
        bookEntity.setTitle("Neuromancer");
        setId(bookEntity, bookId);
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.TO_READ);
        setId(userBook, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(libraryService.ensureInLibrary(userId, bookId)).thenReturn(userBookDto(userBook.getId(), bookEntity));
        when(userBookPersistencePort.getOwnedReference(userId, userBook.getId())).thenReturn(userBook);
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBook.getId())).thenReturn(false);
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of());
        when(shelfBookRepository.maxPosition(shelfId)).thenReturn(0);
        when(shelfBookRepository.save(any(ShelfBook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryService.get(userId, userBook.getId())).thenReturn(userBookDto(userBook.getId(), bookEntity));

        shelfService.addBook(userId, shelfId, new AddShelfBookRequest(null, bookId, null, null));

        verify(libraryService).ensureInLibrary(userId, bookId);
    }

    @Test
    void canViewShelfDelegatesToAccessService() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfAccessService.canView(shelf, userId)).thenReturn(true);

        assertThat(shelfService.canViewShelf(shelfId, userId)).isTrue();
    }

    @Test
    void getShelfNamesForUserBooksGroupsByUserBook() {
        UUID userBookId = UUID.randomUUID();
        Book bookEntity = new Book();
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.TO_READ);
        setId(userBook, userBookId);
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfBookRepository.findByUserBookIdsAndOwner(userId, List.of(userBookId)))
                .thenReturn(List.of(shelfBook));

        assertThat(shelfService.getShelfNamesForUserBooks(userId, List.of(userBookId)))
                .containsEntry(userBookId, List.of(READING_LIST_2));
    }

    @Test
    void getShelfNamesForUserBooksReturnsEmptyForNoIds() {
        assertThat(shelfService.getShelfNamesForUserBooks(userId, List.of())).isEmpty();
    }

    @Test
    void createUsesUniqueSlugWhenBaseTaken() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);
        when(shelfRepository.existsByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(true);
        when(shelfRepository.existsByOwner_IdAndSlug(userId, "reading-list-2")).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(userId)).thenReturn(List.of(shelf));
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> {
            Shelf saved = invocation.getArgument(0);
            setId(saved, UUID.randomUUID());
            return saved;
        });
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(any())).thenReturn(0L);

        ShelfDto created = shelfService.create(
                userId, new CreateShelfRequest(READING_LIST_2, null, null, ShelfVisibility.PRIVATE));

        assertThat(created.slug()).isEqualTo("reading-list-2");
    }

    @Test
    void addMemberRejectsDuplicateMember() {
        UUID memberId = UUID.randomUUID();
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberId))
                .thenReturn(Optional.of(new ShelfMember(shelf, owner, ShelfMemberRole.EDITOR, userId)));

        assertThatThrownBy(() -> shelfService.addMember(
                userId, shelfId, new AddShelfMemberRequest(memberId, ShelfMemberRole.EDITOR)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listEventsReturnsAllForOwner() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfAccessService.activityScopeFor(shelf, userId)).thenReturn(ShelfActivityScope.ALL);
        when(shelfEventService.list(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(shelfService.listEvents(userId, shelfId, PageRequest.of(0, 10)).getContent()).isEmpty();
        verify(shelfEventService).list(shelfId, PageRequest.of(0, 10));
    }

    @Test
    void listEventsReturnsBookChangesOnlyForEditor() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfAccessService.activityScopeFor(shelf, userId)).thenReturn(ShelfActivityScope.BOOK_CHANGES);
        when(shelfEventService.listByTypes(eq(shelfId), eq(ShelfEventService.BOOK_CHANGE_TYPES), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(shelfService.listEvents(userId, shelfId, PageRequest.of(0, 10)).getContent()).isEmpty();
        verify(shelfEventService).listByTypes(eq(shelfId), eq(ShelfEventService.BOOK_CHANGE_TYPES), any());
        verify(shelfEventService, never()).list(eq(shelfId), any());
    }

    @Test
    void listEventsReturnsEmptyForViewer() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfAccessService.activityScopeFor(shelf, userId)).thenReturn(ShelfActivityScope.NONE);

        assertThat(shelfService.listEvents(userId, shelfId, PageRequest.of(0, 10)).getContent()).isEmpty();
        verify(shelfEventService, never()).list(any(), any());
        verify(shelfEventService, never()).listByTypes(any(), any(), any());
    }

    @Test
    void canViewUserBookViaShelfChecksAnyAccessibleShelf() {
        UUID userBookId = UUID.randomUUID();
        ShelfBook shelfBook = new ShelfBook(shelf, new UserBook(), 0, userId);
        when(shelfBookRepository.findByUserBook_Id(userBookId)).thenReturn(List.of(shelfBook));
        when(shelfAccessService.canViewShelfBook(shelfBook, userId)).thenReturn(true);

        assertThat(shelfService.canViewUserBookViaShelf(userBookId, userId)).isTrue();
    }

    @Test
    void getShelfOwnerIdReturnsOwner() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThat(shelfService.getShelfOwnerId(shelfId)).isEqualTo(userId);
    }

    @Test
    void findShelfIdByOwnerAndNameReturnsMatch() {
        when(shelfRepository.findByOwner_IdAndNameIgnoreCase(userId, READING_LIST_2))
                .thenReturn(Optional.of(shelf));

        assertThat(shelfService.findShelfIdByOwnerAndName(userId, " Reading List "))
                .contains(shelfId);
    }

    @Test
    void cloneShelfWithoutBooksSkipsCopy() {
        UUID clonerId = UUID.randomUUID();
        User cloner = new User(CLONER_EXAMPLE_COM, "cloner", HASH, CLONER, USER);
        setId(cloner, clonerId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userLookupService.getPersistenceReference(clonerId)).thenReturn(cloner);
        when(shelfRepository.existsByOwner_IdAndSlug(clonerId, "copy")).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(clonerId)).thenReturn(List.of());
        when(shelfAccessService.roleFor(any(), eq(clonerId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(any())).thenReturn(0L);

        shelfService.cloneShelf(clonerId, shelfId, new CloneShelfRequest("Copy", false, ShelfVisibility.PRIVATE));

        verify(shelfCloneService).cloneWithBooks(eq(shelf), any(Shelf.class), eq(clonerId), eq(false), any());
    }

    @Test
    void canViewBookViaShelfChecksAccessibleEntries() {
        UUID bookId = UUID.randomUUID();
        ShelfBook shelfBook = new ShelfBook(shelf, new UserBook(), 0, userId);
        when(shelfBookRepository.findByBookId(bookId)).thenReturn(List.of(shelfBook));
        when(shelfAccessService.canViewShelfBook(shelfBook, userId)).thenReturn(true);

        assertThat(shelfService.canViewBookViaShelf(bookId, userId)).isTrue();
    }

    @Test
    void listSystemShelfBooksDelegatesToLibrary() {
        when(libraryService.list(userId, ReadingStatus.READ, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(shelfService.listSystemShelfBooks(userId, ReadingStatus.READ, PageRequest.of(0, 10)).getContent())
                .isEmpty();
    }

    @Test
    void addBookRejectsDuplicateWorkOnShelf() {
        Book existingBook = new Book();
        existingBook.setTitle(DUNE);
        existingBook.setAuthors(List.of(FRANK_HERBERT));
        UserBook existingUserBook = new UserBook(owner, existingBook, ReadingStatus.READ);
        setId(existingUserBook, UUID.randomUUID());
        ShelfBook existingShelfBook = new ShelfBook(shelf, existingUserBook, 0, userId);

        Book newBook = new Book();
        newBook.setTitle("DUNE");
        newBook.setAuthors(List.of(FRANK_HERBERT));
        UserBook newUserBook = new UserBook(owner, newBook, ReadingStatus.TO_READ);
        setId(newUserBook, UUID.randomUUID());

        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userBookPersistencePort.getOwnedReference(userId, newUserBook.getId())).thenReturn(newUserBook);
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, newUserBook.getId())).thenReturn(false);
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(existingShelfBook));

        assertThatThrownBy(() -> shelfService.addBook(
                userId, shelfId, new AddShelfBookRequest(newUserBook.getId(), null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void hasUserBookOnShelfDelegatesToRepository() {
        UUID userBookId = UUID.randomUUID();
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBookId)).thenReturn(true);

        assertThat(shelfService.hasUserBookOnShelf(shelfId, userBookId)).isTrue();
    }

    @Test
    void updateMemberRejectsOwnerRoleAssignment() {
        UUID memberId = UUID.randomUUID();
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.updateMember(
                userId, shelfId, memberId, new UpdateShelfMemberRequest(ShelfMemberRole.OWNER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMemberRejectsChangingOwnerRole() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.updateMember(
                userId, shelfId, userId, new UpdateShelfMemberRequest(ShelfMemberRole.ADMIN)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeMemberRejectsOwner() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.removeMember(userId, shelfId, userId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addBookRejectsMissingIdentifiers() {
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.addBook(
                userId, shelfId, new AddShelfBookRequest(null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addBookUsesExplicitPosition() {
        Book bookEntity = new Book();
        bookEntity.setTitle(DUNE);
        UserBook userBook = new UserBook(owner, bookEntity, ReadingStatus.TO_READ);
        setId(userBook, UUID.randomUUID());
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userBookPersistencePort.getOwnedReference(userId, userBook.getId())).thenReturn(userBook);
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBook.getId())).thenReturn(false);
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of());
        when(shelfBookRepository.save(any(ShelfBook.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryService.get(userId, userBook.getId())).thenReturn(userBookDto(userBook.getId(), bookEntity));

        var dto = shelfService.addBook(
                userId, shelfId, new AddShelfBookRequest(userBook.getId(), null, 7, null));

        assertThat(dto.position()).isEqualTo(7);
        verify(shelfBookRepository, never()).maxPosition(shelfId);
    }

    @Test
    void listPublicBooksRejectsPrivateShelf() {
        when(shelfRepository.findByOwner_IdAndSlug(userId, READING_LIST)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.listPublicBooks(userId, READING_LIST))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void exploreFeedUsesPublicSourceForPublicShelf() {
        shelf.setVisibility(ShelfVisibility.PUBLIC);
        when(friendshipService.listFriendIds(userId)).thenReturn(List.of());
        when(shelfRepository.findExploreFeed(eq(userId), eq(0), eq(List.of()), any()))
                .thenReturn(new PageImpl<>(List.of(shelf)));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, userId)).thenReturn(Optional.empty());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        Page<?> feed = shelfService.exploreFeed(userId, PageRequest.of(0, 10));

        assertThat(feed.getContent().getFirst())
                .extracting("source")
                .isEqualTo(ExploreShelfSource.PUBLIC);
    }

    @Test
    void cloneShelfSkipsInaccessibleBooks() {
        UUID clonerId = UUID.randomUUID();
        User cloner = new User(CLONER_EXAMPLE_COM, "cloner", HASH, CLONER, USER);
        setId(cloner, clonerId);
        Book visibleBook = new Book();
        setId(visibleBook, UUID.randomUUID());
        Book hiddenBook = new Book();
        setId(hiddenBook, UUID.randomUUID());
        UserBook visibleUserBook = new UserBook(owner, visibleBook, ReadingStatus.READ);
        setId(visibleUserBook, UUID.randomUUID());
        UserBook hiddenUserBook = new UserBook(owner, hiddenBook, ReadingStatus.READ);
        setId(hiddenUserBook, UUID.randomUUID());
        ShelfBook visible = new ShelfBook(shelf, visibleUserBook, 0, userId);
        ShelfBook hidden = new ShelfBook(shelf, hiddenUserBook, 1, userId);

        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(userLookupService.getPersistenceReference(clonerId)).thenReturn(cloner);
        when(shelfRepository.existsByOwner_IdAndSlug(clonerId, "copy")).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(clonerId)).thenReturn(List.of());
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(visible, hidden));
        when(shelfAccessService.canViewShelfBook(visible, clonerId)).thenReturn(true);
        when(shelfAccessService.canViewShelfBook(hidden, clonerId)).thenReturn(false);
        when(libraryService.ensureInLibrary(clonerId, visibleBook.getId()))
                .thenReturn(userBookDto(visibleUserBook.getId(), visibleBook));
        when(userBookPersistencePort.getOwnedReference(clonerId, visibleUserBook.getId())).thenReturn(visibleUserBook);
        when(shelfAccessService.roleFor(any(), eq(clonerId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(any())).thenReturn(1L);

        shelfService.cloneShelf(clonerId, shelfId, new CloneShelfRequest("Copy", true, ShelfVisibility.PRIVATE));

        verify(shelfCloneService).cloneWithBooks(eq(shelf), any(Shelf.class), eq(clonerId), eq(true), any());
    }

    @Test
    void updateClearsDescriptionAndIconWhenBlank() {
        shelf.setDescription("Old");
        shelf.setIcon("📚");
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfRepository.save(shelf)).thenReturn(shelf);
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        shelfService.update(userId, shelfId, new UpdateShelfRequest(null, "  ", "  ", null, null));

        assertThat(shelf.getDescription()).isNull();
        assertThat(shelf.getIcon()).isNull();
    }

    @Test
    void createWithDescriptionAndIcon() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);
        when(shelfRepository.existsByOwner_IdAndSlug(userId, "favorites")).thenReturn(false);
        when(shelfRepository.findByOwner_IdOrderBySortOrderAsc(userId)).thenReturn(List.of());
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> {
            Shelf saved = invocation.getArgument(0);
            setId(saved, shelfId);
            return saved;
        });
        when(shelfAccessService.roleFor(any(), eq(userId))).thenReturn(Optional.of(ShelfMemberRole.OWNER));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        ShelfDto created = shelfService.create(
                userId, new CreateShelfRequest("Favorites", "  My list  ", " ⭐ ", ShelfVisibility.PRIVATE));

        assertThat(created.name()).isEqualTo("Favorites");
    }

    @Test
    void canViewUserBookViaShelfReturnsFalseWhenInaccessible() {
        UUID userBookId = UUID.randomUUID();
        ShelfBook shelfBook = new ShelfBook(shelf, new UserBook(), 0, userId);
        when(shelfBookRepository.findByUserBook_Id(userBookId)).thenReturn(List.of(shelfBook));
        when(shelfAccessService.canViewShelfBook(shelfBook, userId)).thenReturn(false);

        assertThat(shelfService.canViewUserBookViaShelf(userBookId, userId)).isFalse();
    }

    @Test
    void listBooksUsesSharedViewForNonOwner() {
        UUID viewerId = UUID.randomUUID();
        UserBook userBook = new UserBook(owner, new Book(), ReadingStatus.READING);
        setId(userBook, UUID.randomUUID());
        ShelfBook shelfBook = new ShelfBook(shelf, userBook, 0, userId);
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of(shelfBook));
        when(libraryService.getSharedView(userId, userBook.getId()))
                .thenReturn(userBookDto(userBook.getId(), userBook.getBook()));

        assertThat(shelfService.listBooks(viewerId, shelfId)).hasSize(1);
        verify(libraryService, never()).get(viewerId, userBook.getId());
    }

    @Test
    void removeMemberThrowsWhenMemberMissing() {
        UUID memberId = UUID.randomUUID();
        when(shelfRepository.findById(shelfId)).thenReturn(Optional.of(shelf));
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelfId, memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.removeMember(userId, shelfId, memberId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSharedShelfRejectsRevokedLink() {
        UUID token = UUID.randomUUID();
        ShelfShareLink link = new ShelfShareLink(shelf, token, owner, null);
        link.revoke();
        when(shelfShareLinkRepository.findByToken(token)).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> shelfService.getSharedShelf(token))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getSharedShelfReturnsShelfAndBooks() {
        UUID token = UUID.randomUUID();
        ShelfShareLink link = new ShelfShareLink(shelf, token, owner, null);
        setId(link, UUID.randomUUID());
        when(shelfShareLinkRepository.findByToken(token)).thenReturn(Optional.of(link));
        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(shelfId)).thenReturn(List.of());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(0L);

        SharedShelfDto shared = shelfService.getSharedShelf(token);

        assertThat(shared.shelf().id()).isEqualTo(shelfId);
        assertThat(shared.books()).isEmpty();
    }

    private UserBookDto userBookDto(UUID userBookId, Book book) {
        BookDto bookDto = new BookDto(
                book.getId(), book.getTitle(), null, book.getAuthors(), null, book.getPageCount(),
                null, null, BookSource.MANUAL, null, null, 0, null, null,
                Instant.parse(V_2024_01_01T00_00_00Z), Instant.parse(V_2024_01_02T00_00_00Z));
        return new UserBookDto(
                userBookId, bookDto, ReadingStatus.TO_READ, null, null, null, null, null,
                null, null, List.of(), Instant.parse(V_2024_01_01T00_00_00Z),
                Instant.parse(V_2024_01_02T00_00_00Z));
    }
}
