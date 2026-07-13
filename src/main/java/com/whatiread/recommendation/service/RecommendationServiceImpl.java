package com.whatiread.recommendation.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.catalog.service.BookService;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.service.LibraryService;
import com.whatiread.recommendation.api.CreateBatchRecommendationRequest;
import com.whatiread.recommendation.api.CreateRecommendationRequest;
import com.whatiread.recommendation.api.RecommendationDto;
import com.whatiread.recommendation.api.RecommendationSuggestionDto;
import com.whatiread.recommendation.api.RecommendationUserDto;
import com.whatiread.recommendation.domain.Recommendation;
import com.whatiread.recommendation.domain.RecommendationSource;
import com.whatiread.recommendation.domain.RecommendationStatus;
import com.whatiread.recommendation.domain.RecommendationTargetType;
import com.whatiread.recommendation.repository.RecommendationRepository;
import com.whatiread.recommendation.repository.RecommendationSuggestionRepository;
import com.whatiread.shared.event.RecommendationAcceptedEvent;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shared.outbox.OutboxEventPublisher;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.service.ShelfService;
import com.whatiread.social.service.FriendshipService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    private static final int SUGGESTION_LIMIT = 10;

    private final RecommendationRepository recommendationRepository;
    private final RecommendationSuggestionRepository suggestionRepository;
    private final UserLookupService userLookupService;
    private final BookPersistencePort bookPersistencePort;
    private final BookService bookService;
    private final LibraryService libraryService;
    private final FriendshipService friendshipService;
    private final ShelfService shelfService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final BusinessMetrics businessMetrics;
    private final SimpMessagingTemplate messagingTemplate;

    public RecommendationServiceImpl(
            RecommendationRepository recommendationRepository,
            RecommendationSuggestionRepository suggestionRepository,
            UserLookupService userLookupService,
            BookPersistencePort bookPersistencePort,
            BookService bookService,
            LibraryService libraryService,
            FriendshipService friendshipService,
            ShelfService shelfService,
            OutboxEventPublisher outboxEventPublisher,
            BusinessMetrics businessMetrics,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.recommendationRepository = recommendationRepository;
        this.suggestionRepository = suggestionRepository;
        this.userLookupService = userLookupService;
        this.bookPersistencePort = bookPersistencePort;
        this.bookService = bookService;
        this.libraryService = libraryService;
        this.friendshipService = friendshipService;
        this.shelfService = shelfService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.businessMetrics = businessMetrics;
        this.messagingTemplate = messagingTemplate;
    }

    private static void validateFriendRecommendation(UUID fromUserId, UUID toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot recommend to yourself");
        }
    }

    private static RecommendationTargetType resolveTargetType(RecommendationTargetType targetType) {
        return targetType != null ? targetType : RecommendationTargetType.BOOK;
    }

    private static String trimMessage(String message) {
        return message != null ? message.trim() : null;
    }

    @Override
    public RecommendationDto createFriendRecommendation(UUID fromUserId, CreateRecommendationRequest request) {
        RecommendationTargetType targetType = resolveTargetType(request.targetType());
        return switch (targetType) {
            case BOOK -> createSingleBookRecommendation(
                    fromUserId,
                    request.toUserId(),
                    request.bookId(),
                    request.message()
            );
            case SHELF -> createSingleShelfRecommendation(
                    fromUserId,
                    request.toUserId(),
                    request.shelfId(),
                    request.message()
            );
        };
    }

    @Override
    public List<RecommendationDto> createBatchFriendRecommendations(
            UUID fromUserId,
            CreateBatchRecommendationRequest request
    ) {
        validateFriendRecommendation(fromUserId, request.toUserId());
        requireFriendship(fromUserId, request.toUserId());
        RecommendationTargetType targetType = resolveTargetType(request.targetType());
        return switch (targetType) {
            case BOOK -> createBatchBookRecommendations(fromUserId, request);
            case SHELF -> createBatchShelfRecommendations(fromUserId, request);
        };
    }

    private List<RecommendationDto> createBatchBookRecommendations(
            UUID fromUserId,
            CreateBatchRecommendationRequest request
    ) {
        if (request.bookIds() == null || request.bookIds().isEmpty()) {
            throw new IllegalArgumentException("bookIds is required for book recommendations");
        }
        List<UUID> bookIds = request.bookIds().stream().distinct().toList();
        List<RecommendationDto> created = new ArrayList<>(bookIds.size());
        for (UUID bookId : bookIds) {
            created.add(createSingleBookRecommendation(
                    fromUserId,
                    request.toUserId(),
                    bookId,
                    request.message()
            ));
        }
        return created;
    }

    private List<RecommendationDto> createBatchShelfRecommendations(
            UUID fromUserId,
            CreateBatchRecommendationRequest request
    ) {
        if (request.shelfIds() == null || request.shelfIds().isEmpty()) {
            throw new IllegalArgumentException("shelfIds is required for shelf recommendations");
        }
        List<UUID> shelfIds = request.shelfIds().stream().distinct().toList();
        List<RecommendationDto> created = new ArrayList<>(shelfIds.size());
        for (UUID shelfId : shelfIds) {
            created.add(createSingleShelfRecommendation(
                    fromUserId,
                    request.toUserId(),
                    shelfId,
                    request.message()
            ));
        }
        return created;
    }

    private RecommendationDto createSingleBookRecommendation(
            UUID fromUserId,
            UUID toUserId,
            UUID bookId,
            String message
    ) {
        validateFriendRecommendation(fromUserId, toUserId);
        requireFriendship(fromUserId, toUserId);
        if (bookId == null) {
            throw new IllegalArgumentException("bookId is required for book recommendations");
        }
        if (recommendationRepository.existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
                fromUserId,
                toUserId,
                bookId,
                RecommendationStatus.PENDING
        )) {
            throw new ConflictException("A pending recommendation for this book already exists");
        }
        User fromUser = userLookupService.getPersistenceReference(fromUserId);
        User toUser = requireRecipientAcceptingRecommendations(toUserId);
        Book book = bookPersistencePort.getReference(bookId);
        Recommendation recommendation = new Recommendation(
                fromUser,
                toUser,
                book,
                trimMessage(message),
                RecommendationSource.FRIEND
        );
        return persistAndNotify(recommendation, toUserId);
    }

    private RecommendationDto createSingleShelfRecommendation(
            UUID fromUserId,
            UUID toUserId,
            UUID shelfId,
            String message
    ) {
        validateFriendRecommendation(fromUserId, toUserId);
        requireFriendship(fromUserId, toUserId);
        if (shelfId == null) {
            throw new IllegalArgumentException("shelfId is required for shelf recommendations");
        }
        if (!shelfService.getShelfOwnerId(shelfId).equals(fromUserId)) {
            throw new ForbiddenException("You can only recommend your own shelves");
        }
        Shelf shelf = shelfService.getPersistenceReference(shelfId);
        if (recommendationRepository.existsByFromUser_IdAndToUser_IdAndShelf_IdAndStatus(
                fromUserId,
                toUserId,
                shelfId,
                RecommendationStatus.PENDING
        )) {
            throw new ConflictException("A pending recommendation for this shelf already exists");
        }
        User fromUser = userLookupService.getPersistenceReference(fromUserId);
        User toUser = requireRecipientAcceptingRecommendations(toUserId);
        Recommendation recommendation = new Recommendation(
                fromUser,
                toUser,
                shelf,
                trimMessage(message),
                RecommendationSource.FRIEND
        );
        return persistAndNotify(recommendation, toUserId);
    }

    private RecommendationDto persistAndNotify(Recommendation recommendation, UUID toUserId) {
        RecommendationDto saved = toDto(recommendationRepository.save(recommendation));
        messagingTemplate.convertAndSendToUser(
                toUserId.toString(),
                "/queue/recommendations",
                saved
        );
        businessMetrics.recordRecommendationCreated();
        return saved;
    }

    private void requireFriendship(UUID fromUserId, UUID toUserId) {
        if (!friendshipService.areFriends(fromUserId, toUserId)) {
            throw new ForbiddenException("You can only recommend to friends");
        }
    }

    private User requireRecipientAcceptingRecommendations(UUID toUserId) {
        User toUser = userLookupService.getPersistenceReference(toUserId);
        if (!toUser.isAcceptRecommendations()) {
            throw new ForbiddenException("This user is not accepting recommendations");
        }
        return toUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDto> listInbox(UUID userId) {
        return recommendationRepository
                .findByToUser_IdAndStatusOrderByCreatedAtDesc(userId, RecommendationStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDto> listSent(UUID userId) {
        return recommendationRepository
                .findByFromUser_IdAndStatusOrderByCreatedAtDesc(userId, RecommendationStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RecommendationDto accept(UUID userId, UUID recommendationId) {
        Recommendation recommendation = getPendingForRecipient(recommendationId, userId);
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        Recommendation saved = recommendationRepository.save(recommendation);
        if (saved.getTargetType() == RecommendationTargetType.BOOK) {
            outboxEventPublisher.publish(
                    "RecommendationAcceptedEvent",
                    new RecommendationAcceptedEvent(userId, saved.getBook().getId())
            );
        } else {
            UUID recommenderId = saved.getFromUser().getId();
            shelfService.cloneForRecommendationRecipient(userId, recommenderId, saved.getShelf().getId());
        }
        businessMetrics.recordRecommendationAccepted();
        return toDto(saved);
    }

    @Override
    public RecommendationDto dismiss(UUID userId, UUID recommendationId) {
        Recommendation recommendation = getPendingForRecipient(recommendationId, userId);
        recommendation.setStatus(RecommendationStatus.DISMISSED);
        return toDto(recommendationRepository.save(recommendation));
    }

    @Override
    public void delete(UUID userId, UUID recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new ConflictException("Recommendation is not pending");
        }
        UUID fromUserId = recommendation.getFromUser() != null ? recommendation.getFromUser().getId() : null;
        UUID toUserId = recommendation.getToUser().getId();
        if (userId.equals(fromUserId)) {
            recommendation.setStatus(RecommendationStatus.WITHDRAWN);
        } else if (userId.equals(toUserId)) {
            recommendation.setStatus(RecommendationStatus.DISMISSED);
        } else {
            throw new ForbiddenException("You cannot delete this recommendation");
        }
        recommendationRepository.save(recommendation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationSuggestionDto> listSuggestions(UUID userId) {
        Set<UUID> orderedBookIds = new LinkedHashSet<>();

        List<String> preferredTags = suggestionRepository.findPreferredTagNames(userId);
        if (!preferredTags.isEmpty()) {
            orderedBookIds.addAll(suggestionRepository.findBookIdsBySharedTags(
                    userId,
                    preferredTags,
                    SUGGESTION_LIMIT
            ));
        }

        orderedBookIds.addAll(suggestionRepository.findFriendHighlyRatedBookIds(userId, SUGGESTION_LIMIT));

        List<RecommendationSuggestionDto> suggestions = new ArrayList<>();
        for (UUID bookId : orderedBookIds) {
            if (suggestions.size() >= SUGGESTION_LIMIT) {
                break;
            }
            if (libraryService.hasBook(userId, bookId)) {
                continue;
            }
            BookDto book = bookService.getById(bookId);
            String reason = preferredTags.isEmpty()
                    ? "Highly rated by a friend"
                    : "Matches your reading interests";
            suggestions.add(new RecommendationSuggestionDto(book, RecommendationSource.SYSTEM, reason));
        }
        return suggestions;
    }

    private Recommendation getPendingForRecipient(UUID recommendationId, UUID userId) {
        Recommendation recommendation = recommendationRepository.findByIdAndToUser_Id(recommendationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new ConflictException("Recommendation is not pending");
        }
        return recommendation;
    }

    private RecommendationDto toDto(Recommendation recommendation) {
        User fromUser = recommendation.getFromUser();
        BookDto book = recommendation.getTargetType() == RecommendationTargetType.BOOK && recommendation.getBook() != null
                ? bookService.getById(recommendation.getBook().getId())
                : null;
        ShelfDto shelf = recommendation.getTargetType() == RecommendationTargetType.SHELF && recommendation.getShelf() != null
                ? shelfService.getRecommendationPreview(recommendation.getShelf().getId())
                : null;
        return new RecommendationDto(
                recommendation.getId(),
                fromUser != null ? toUserDto(fromUser) : null,
                toUserDto(recommendation.getToUser()),
                recommendation.getTargetType(),
                book,
                shelf,
                recommendation.getMessage(),
                recommendation.getSource(),
                recommendation.getStatus(),
                recommendation.getCreatedAt()
        );
    }

    private RecommendationUserDto toUserDto(User user) {
        return new RecommendationUserDto(user.getId(), user.getDisplayName(), user.getAvatarUrl());
    }
}
