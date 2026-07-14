package com.whatiread.recommendation.repository;

import com.whatiread.recommendation.domain.Recommendation;
import com.whatiread.recommendation.domain.RecommendationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    @EntityGraph(attributePaths = {"fromUser", "toUser", "book", "shelf"})
    List<Recommendation> findByToUser_IdAndStatusOrderByCreatedAtDesc(UUID toUserId, RecommendationStatus status);

    @EntityGraph(attributePaths = {"fromUser", "toUser", "book", "shelf"})
    List<Recommendation> findByFromUser_IdAndStatusOrderByCreatedAtDesc(UUID fromUserId, RecommendationStatus status);

    Optional<Recommendation> findByIdAndToUser_Id(UUID id, UUID toUserId);

    Optional<Recommendation> findByIdAndFromUser_Id(UUID id, UUID fromUserId);

    boolean existsByFromUser_IdAndToUser_IdAndBook_IdAndStatus(
            UUID fromUserId,
            UUID toUserId,
            UUID bookId,
            RecommendationStatus status
    );

    boolean existsByFromUser_IdAndToUser_IdAndShelf_IdAndStatus(
            UUID fromUserId,
            UUID toUserId,
            UUID shelfId,
            RecommendationStatus status
    );
}
