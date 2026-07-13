package com.whatiread.recommendation.service;

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
import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.catalog.service.BookService;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.service.LibraryService;
import com.whatiread.recommendation.api.CreateBatchRecommendationRequest;
import com.whatiread.recommendation.api.CreateRecommendationRequest;
import com.whatiread.recommendation.domain.Recommendation;
import com.whatiread.recommendation.domain.RecommendationSource;
import com.whatiread.recommendation.domain.RecommendationStatus;
import com.whatiread.recommendation.domain.RecommendationTargetType;
import com.whatiread.recommendation.repository.RecommendationRepository;
import com.whatiread.recommendation.repository.RecommendationSuggestionRepository;
import com.whatiread.shared.event.RecommendationAcceptedEvent;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.outbox.OutboxEventPublisher;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.service.ShelfService;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendationServiceImplTest {


    private static final String ENJOY = "enjoy";
    private static final String SCI_FI = "sci-fi";
    private static final String HASH = "hash";
    private static final String USER = "User";
    private static final String DUNE = "Dune";
    @Mock
    private RecommendationRepository recommendationRepository;
    @Mock
    private RecommendationSuggestionRepository suggestionRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private BookPersistencePort bookPersistencePort;
    @Mock
    private BookService bookService;
    @Mock
    private LibraryService libraryService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private ShelfService shelfService;
    @Mock
    private OutboxEventPublisher outboxEventPublisher;
    @Mock
    private BusinessMetrics businessMetrics;
    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private UUID fromUserId;
    private UUID toUserId;
    private UUID bookId;
    private UUID shelfId;
    private User fromUser;
    private User toUser;
    private Book book;
    private Shelf shelf;

    private static BookDto bookDto() {
        return new BookDto(
                UUID.randomUUID(), DUNE, null, List.of("Frank Herbert"), null, 688,
                null, null, BookSource.OPEN_LIBRARY, null, null, 0, null, null,
                Instant.now(), Instant.now());
    }

    private static ShelfDto shelfDto() {
        return new ShelfDto(
                UUID.randomUUID(),
                "Favorites",
                "favorites",
                null,
                "📚",
                ShelfVisibility.PRIVATE,
                0,
                UUID.randomUUID(),
                null,
                3,
                Instant.now(),
                Instant.now(),
                "From User"
        );
    }

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @BeforeEach
    void setUp() {
        fromUserId = UUID.randomUUID();
        toUserId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        shelfId = UUID.randomUUID();
        fromUser = new User("from@example.com", HASH, "From", USER);
        toUser = new User("to@example.com", HASH, "To", USER);
        book = new Book();
        book.setTitle(DUNE);
        shelf = new Shelf(fromUser, "Favorites", "favorites");
        setId(fromUser, fromUserId);
        setId(toUser, toUserId);
        setId(book, bookId);
        setId(shelf, shelfId);
        when(bookService.getById(bookId)).thenReturn(bookDto());
        when(shelfService.getRecommendationPreview(shelfId)).thenReturn(shelfDto());
    }

    @Test
    void createFriendRecommendationRejectsSelfRecommendation() {
        assertThatThrownBy(() -> recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(fromUserId, RecommendationTargetType.BOOK, bookId, null, "read this")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createFriendRecommendationRequiresFriendship() {
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(false);

        assertThatThrownBy(() -> recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.BOOK, bookId, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createFriendRecommendationRejectsOptedOutRecipient() {
        toUser.setAcceptRecommendations(false);
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(userLookupService.getPersistenceReference(toUserId)).thenReturn(toUser);

        assertThatThrownBy(() -> recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.BOOK, bookId, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not accepting recommendations");
    }

    @Test
    void acceptPublishesOutboxEvent() {
        UUID recommendationId = UUID.randomUUID();
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, ENJOY, RecommendationSource.FRIEND);
        setId(recommendation, recommendationId);
        when(recommendationRepository.findByIdAndToUser_Id(recommendationId, toUserId))
                .thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

        recommendationService.accept(toUserId, recommendationId);

        verify(outboxEventPublisher).publish(eq("RecommendationAcceptedEvent"), any(RecommendationAcceptedEvent.class));
        verify(businessMetrics).recordRecommendationAccepted();
    }

    @Test
    void listSuggestionsSkipsBooksAlreadyInLibrary() {
        UUID suggestedBookId = UUID.randomUUID();
        when(suggestionRepository.findPreferredTagNames(toUserId)).thenReturn(List.of(SCI_FI));
        when(suggestionRepository.findBookIdsBySharedTags(toUserId, List.of(SCI_FI), 10))
                .thenReturn(List.of(suggestedBookId));
        when(suggestionRepository.findFriendHighlyRatedBookIds(toUserId, 10)).thenReturn(List.of());
        when(libraryService.hasBook(toUserId, suggestedBookId)).thenReturn(true);

        assertThat(recommendationService.listSuggestions(toUserId)).isEmpty();
    }

    @Test
    void dismissMarksRecommendationDismissed() {
        UUID recommendationId = UUID.randomUUID();
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(recommendation, recommendationId);
        when(recommendationRepository.findByIdAndToUser_Id(recommendationId, toUserId))
                .thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

        var dto = recommendationService.dismiss(toUserId, recommendationId);

        assertThat(dto.status()).isEqualTo(RecommendationStatus.DISMISSED);
    }

    @Test
    void deleteWithdrawsForSender() {
        UUID recommendationId = UUID.randomUUID();
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(recommendation, recommendationId);
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        recommendationService.delete(fromUserId, recommendationId);

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.WITHDRAWN);
        verify(recommendationRepository).save(recommendation);
    }

    @Test
    void deleteDismissesForRecipient() {
        UUID recommendationId = UUID.randomUUID();
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(recommendation, recommendationId);
        when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        recommendationService.delete(toUserId, recommendationId);

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.DISMISSED);
    }

    @Test
    void createRejectsDuplicatePendingRecommendation() {
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId, toUserId, bookId, RecommendationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.BOOK, bookId, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createBatchFriendRecommendationsCreatesMultiple() {
        UUID secondBookId = UUID.randomUUID();
        Recommendation saved1 = new Recommendation(fromUser, toUser, book, ENJOY, RecommendationSource.FRIEND);
        Recommendation saved2 = new Recommendation(fromUser, toUser, book, ENJOY, RecommendationSource.FRIEND);
        setId(saved1, UUID.randomUUID());
        setId(saved2, UUID.randomUUID());
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId, toUserId, bookId, RecommendationStatus.PENDING)).thenReturn(false);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId, toUserId, secondBookId, RecommendationStatus.PENDING)).thenReturn(false);
        when(userLookupService.getPersistenceReference(fromUserId)).thenReturn(fromUser);
        when(userLookupService.getPersistenceReference(toUserId)).thenReturn(toUser);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(bookPersistencePort.getReference(secondBookId)).thenReturn(book);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved1, saved2);

        var results = recommendationService.createBatchFriendRecommendations(
                fromUserId,
                new CreateBatchRecommendationRequest(
                        toUserId,
                        RecommendationTargetType.BOOK,
                        List.of(bookId, secondBookId),
                        null,
                        " enjoy "));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).message()).isEqualTo(ENJOY);
        verify(businessMetrics, org.mockito.Mockito.times(2)).recordRecommendationCreated();
    }

    @Test
    void createBatchFriendRecommendationsDedupesBookIds() {
        Recommendation saved = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(saved, UUID.randomUUID());
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId, toUserId, bookId, RecommendationStatus.PENDING)).thenReturn(false);
        when(userLookupService.getPersistenceReference(fromUserId)).thenReturn(fromUser);
        when(userLookupService.getPersistenceReference(toUserId)).thenReturn(toUser);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        var results = recommendationService.createBatchFriendRecommendations(
                fromUserId,
                new CreateBatchRecommendationRequest(
                        toUserId,
                        RecommendationTargetType.BOOK,
                        List.of(bookId, bookId),
                        null,
                        null));

        assertThat(results).hasSize(1);
    }

    @Test
    void createFriendRecommendationPersistsRecommendation() {
        Recommendation saved = new Recommendation(fromUser, toUser, book, ENJOY, RecommendationSource.FRIEND);
        setId(saved, UUID.randomUUID());
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId, toUserId, bookId, RecommendationStatus.PENDING)).thenReturn(false);
        when(userLookupService.getPersistenceReference(fromUserId)).thenReturn(fromUser);
        when(userLookupService.getPersistenceReference(toUserId)).thenReturn(toUser);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        var dto = recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.BOOK, bookId, null, " enjoy "));

        assertThat(dto.message()).isEqualTo(ENJOY);
        assertThat(dto.targetType()).isEqualTo(RecommendationTargetType.BOOK);
        verify(messagingTemplate).convertAndSendToUser(eq(toUserId.toString()), eq("/queue/recommendations"), any());
        verify(businessMetrics).recordRecommendationCreated();
    }

    @Test
    void createShelfRecommendationPersistsAndNotifies() {
        Recommendation saved = new Recommendation(fromUser, toUser, shelf, ENJOY, RecommendationSource.FRIEND);
        setId(saved, UUID.randomUUID());
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(recommendationRepository.existsByFromUser_IdAndToUser_IdAndShelf_IdAndStatus(
                fromUserId, toUserId, shelfId, RecommendationStatus.PENDING)).thenReturn(false);
        when(shelfService.getShelfOwnerId(shelfId)).thenReturn(fromUserId);
        when(shelfService.getPersistenceReference(shelfId)).thenReturn(shelf);
        when(userLookupService.getPersistenceReference(fromUserId)).thenReturn(fromUser);
        when(userLookupService.getPersistenceReference(toUserId)).thenReturn(toUser);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        var dto = recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.SHELF, null, shelfId, ENJOY));

        assertThat(dto.targetType()).isEqualTo(RecommendationTargetType.SHELF);
        assertThat(dto.shelf()).isNotNull();
        verify(messagingTemplate).convertAndSendToUser(eq(toUserId.toString()), eq("/queue/recommendations"), any());
    }

    @Test
    void createShelfRecommendationRequiresOwnership() {
        Shelf otherShelf = new Shelf(toUser, "Theirs", "theirs");
        setId(otherShelf, shelfId);
        when(friendshipService.areFriends(fromUserId, toUserId)).thenReturn(true);
        when(shelfService.getShelfOwnerId(shelfId)).thenReturn(toUserId);

        assertThatThrownBy(() -> recommendationService.createFriendRecommendation(
                fromUserId, new CreateRecommendationRequest(toUserId, RecommendationTargetType.SHELF, null, shelfId, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptShelfRecommendationClonesShelf() {
        UUID recommendationId = UUID.randomUUID();
        Recommendation recommendation = new Recommendation(fromUser, toUser, shelf, ENJOY, RecommendationSource.FRIEND);
        setId(recommendation, recommendationId);
        when(recommendationRepository.findByIdAndToUser_Id(recommendationId, toUserId))
                .thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);

        recommendationService.accept(toUserId, recommendationId);

        verify(shelfService).cloneForRecommendationRecipient(toUserId, fromUserId, shelfId);
        verify(outboxEventPublisher, never()).publish(eq("RecommendationAcceptedEvent"), any());
    }

    @Test
    void listSentReturnsPendingRecommendationsFromUser() {
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(recommendation, UUID.randomUUID());
        when(recommendationRepository.findByFromUser_IdAndStatusOrderByCreatedAtDesc(
                fromUserId, RecommendationStatus.PENDING)).thenReturn(List.of(recommendation));

        assertThat(recommendationService.listSent(fromUserId)).hasSize(1);
    }

    @Test
    void listInboxReturnsPendingRecommendations() {
        Recommendation recommendation = new Recommendation(fromUser, toUser, book, null, RecommendationSource.FRIEND);
        setId(recommendation, UUID.randomUUID());
        when(recommendationRepository.findByToUser_IdAndStatusOrderByCreatedAtDesc(
                toUserId, RecommendationStatus.PENDING)).thenReturn(List.of(recommendation));

        assertThat(recommendationService.listInbox(toUserId)).hasSize(1);
    }

    @Test
    void listSuggestionsReturnsFriendRatedBooks() {
        when(suggestionRepository.findPreferredTagNames(toUserId)).thenReturn(List.of());
        when(suggestionRepository.findFriendHighlyRatedBookIds(toUserId, 10)).thenReturn(List.of(bookId));
        when(libraryService.hasBook(toUserId, bookId)).thenReturn(false);

        assertThat(recommendationService.listSuggestions(toUserId)).hasSize(1);
    }
}
