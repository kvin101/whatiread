package com.whatiread.comment.repository;

import com.whatiread.comment.domain.Comment;
import com.whatiread.comment.domain.CommentTargetType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
            CommentTargetType targetType,
            UUID targetId,
            Pageable pageable
    );
}
