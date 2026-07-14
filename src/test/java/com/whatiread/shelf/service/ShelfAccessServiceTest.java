package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.library.domain.UserBook;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.domain.ShelfMember;
import com.whatiread.shelf.domain.ShelfMemberRole;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ShelfMemberRepository;
import com.whatiread.social.service.FriendshipService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfAccessServiceTest {


    private static final String HASH = "hash";
    private static final String USER = "User";
    private static final String EDITOR_EXAMPLE_COM = "editor@example.com";
    private static final String EDITOR = "Editor";
    @Mock
    private ShelfMemberRepository shelfMemberRepository;

    @Mock
    private FriendshipService friendshipService;

    @InjectMocks
    private ShelfAccessService shelfAccessService;

    private UUID ownerId;
    private UUID viewerId;
    private Shelf shelf;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                var idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id field on " + entity.getClass());
    }

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        User owner = new User("owner@example.com", "owner", HASH, "Owner", USER);
        setId(owner, ownerId);
        shelf = new Shelf(owner, "Reading", "reading");
        setId(shelf, UUID.randomUUID());
    }

    @Test
    void publicShelfIsViewableByAnyone() {
        shelf.setVisibility(ShelfVisibility.PUBLIC);

        assertThat(shelfAccessService.canView(shelf, null)).isTrue();
        assertThat(shelfAccessService.canView(shelf, viewerId)).isTrue();
    }

    @Test
    void privateShelfRequiresMembership() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);
        User viewer = new User("viewer@example.com", "viewer", HASH, "Viewer", USER);
        setId(viewer, viewerId);
        ShelfMember member = new ShelfMember(shelf, viewer, ShelfMemberRole.EDITOR, ownerId);

        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(member));

        assertThat(shelfAccessService.canView(shelf, viewerId)).isTrue();
        assertThat(shelfAccessService.roleFor(shelf, viewerId)).contains(ShelfMemberRole.EDITOR);
    }

    @Test
    void friendsShelfRequiresFriendship() {
        shelf.setVisibility(ShelfVisibility.FRIENDS);
        when(friendshipService.areFriends(viewerId, ownerId)).thenReturn(true);

        assertThat(shelfAccessService.canView(shelf, viewerId)).isTrue();
    }

    @Test
    void secretShelfHiddenFromProfile() {
        shelf.setVisibility(ShelfVisibility.SECRET);

        assertThat(shelfAccessService.appearsOnProfile(shelf, viewerId)).isFalse();
        assertThat(shelfAccessService.appearsOnProfile(shelf, ownerId)).isTrue();
    }

    @Test
    void requireEditRejectsViewersWithoutRole() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfAccessService.requireEdit(shelf, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireViewRejectsInaccessibleShelf() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);

        assertThatThrownBy(() -> shelfAccessService.requireView(shelf, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireManageMembersRejectsEditorRole() {
        User editor = new User(EDITOR_EXAMPLE_COM, "editor", HASH, EDITOR, USER);
        setId(editor, viewerId);
        ShelfMember member = new ShelfMember(shelf, editor, ShelfMemberRole.EDITOR, ownerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> shelfAccessService.requireManageMembers(shelf, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireOwnerRejectsNonOwnerMember() {
        User admin = new User("admin@example.com", "adminuser", HASH, "Admin", USER);
        setId(admin, viewerId);
        ShelfMember member = new ShelfMember(shelf, admin, ShelfMemberRole.ADMIN, ownerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> shelfAccessService.requireOwner(shelf, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void privateShelfIsNotViewableByStrangers() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);

        assertThat(shelfAccessService.canView(shelf, viewerId)).isFalse();
    }

    @Test
    void friendsShelfBookVisibleToFriends() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);
        ShelfBook shelfBook = new ShelfBook(shelf, new UserBook(), 0, ownerId);
        shelfBook.setVisibility(ShelfVisibility.FRIENDS);
        when(friendshipService.areFriends(viewerId, ownerId)).thenReturn(true);

        assertThat(shelfAccessService.canViewShelfBook(shelfBook, viewerId)).isTrue();
    }

    @Test
    void appearsOnProfileForFriendsOnlyWhenFriends() {
        shelf.setVisibility(ShelfVisibility.FRIENDS);
        when(friendshipService.areFriends(viewerId, ownerId)).thenReturn(true);

        assertThat(shelfAccessService.appearsOnProfile(shelf, viewerId)).isTrue();
    }

    @Test
    void requireManageShelfRejectsEditor() {
        User editor = new User(EDITOR_EXAMPLE_COM, "editor", HASH, EDITOR, USER);
        setId(editor, viewerId);
        ShelfMember member = new ShelfMember(shelf, editor, ShelfMemberRole.EDITOR, ownerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> shelfAccessService.requireManageShelf(shelf, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ownerCanManageShelfAndMembers() {
        assertThat(shelfAccessService.roleFor(shelf, ownerId)).contains(ShelfMemberRole.OWNER);

        shelfAccessService.requireOwner(shelf, ownerId);
        shelfAccessService.requireManageShelf(shelf, ownerId);
        shelfAccessService.requireManageMembers(shelf, ownerId);
    }

    @Test
    void activityScopeForOwnerAndAdminIsAll() {
        assertThat(shelfAccessService.activityScopeFor(shelf, ownerId)).isEqualTo(ShelfActivityScope.ALL);

        User admin = new User("admin@example.com", "adminuser", HASH, "Admin", USER);
        setId(admin, viewerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(new ShelfMember(shelf, admin, ShelfMemberRole.ADMIN, ownerId)));

        assertThat(shelfAccessService.activityScopeFor(shelf, viewerId)).isEqualTo(ShelfActivityScope.ALL);
    }

    @Test
    void activityScopeForEditorIsBookChangesOnly() {
        User editor = new User(EDITOR_EXAMPLE_COM, "editor", HASH, EDITOR, USER);
        setId(editor, viewerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(new ShelfMember(shelf, editor, ShelfMemberRole.EDITOR, ownerId)));

        assertThat(shelfAccessService.activityScopeFor(shelf, viewerId)).isEqualTo(ShelfActivityScope.BOOK_CHANGES);
    }

    @Test
    void activityScopeForViewerAndStrangersIsNone() {
        User viewer = new User("viewer@example.com", "viewer", HASH, "Viewer", USER);
        setId(viewer, viewerId);
        when(shelfMemberRepository.findByShelf_IdAndUser_Id(shelf.getId(), viewerId))
                .thenReturn(Optional.of(new ShelfMember(shelf, viewer, ShelfMemberRole.VIEWER, ownerId)));

        assertThat(shelfAccessService.activityScopeFor(shelf, viewerId)).isEqualTo(ShelfActivityScope.NONE);
        assertThat(shelfAccessService.activityScopeFor(shelf, UUID.randomUUID())).isEqualTo(ShelfActivityScope.NONE);
    }

    @Test
    void canViewShelfBookRespectsEffectiveVisibility() {
        shelf.setVisibility(ShelfVisibility.PRIVATE);
        ShelfBook shelfBook = new ShelfBook(shelf, new UserBook(), 0, ownerId);
        shelfBook.setVisibility(ShelfVisibility.PUBLIC);

        assertThat(shelfAccessService.canViewShelfBook(shelfBook, null)).isTrue();
    }
}
