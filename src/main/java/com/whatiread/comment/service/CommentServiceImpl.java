package com.whatiread.comment.service;

import com.whatiread.comment.api.CommentAuthorDto;
import com.whatiread.comment.api.CommentDto;
import com.whatiread.comment.api.CreateCommentRequest;
import com.whatiread.comment.api.ReportCommentRequest;
import com.whatiread.comment.api.UpdateCommentRequest;
import com.whatiread.comment.domain.Comment;
import com.whatiread.comment.domain.CommentReport;
import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.comment.repository.CommentReportRepository;
import com.whatiread.comment.repository.CommentRepository;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserLookupService userLookupService;
    private final CommentTargetAccessService commentTargetAccessService;
    private final CommentReportRepository commentReportRepository;
    private final BusinessMetrics businessMetrics;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            UserLookupService userLookupService,
            CommentTargetAccessService commentTargetAccessService,
            CommentReportRepository commentReportRepository,
            BusinessMetrics businessMetrics
    ) {
        this.commentRepository = commentRepository;
        this.userLookupService = userLookupService;
        this.commentTargetAccessService = commentTargetAccessService;
        this.commentReportRepository = commentReportRepository;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public CommentDto create(UUID authorId, CreateCommentRequest request) {
        commentTargetAccessService.requireCanView(request.targetType(), request.targetId(), authorId);
        User author = userLookupService.getPersistenceReference(authorId);
        Comment comment = new Comment(
                author,
                request.targetType(),
                request.targetId(),
                request.body().trim()
        );
        CommentDto saved = toDto(commentRepository.save(comment));
        businessMetrics.recordCommentCreated();
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> list(CommentTargetType targetType, UUID targetId, UUID viewerId, Pageable pageable) {
        commentTargetAccessService.requireCanView(targetType, targetId, viewerId);
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable)
                .map(this::toDto);
    }

    @Override
    public CommentDto update(UUID authorId, UUID commentId, UpdateCommentRequest request) {
        Comment comment = getComment(commentId);
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new ForbiddenException("Only the author can edit this comment");
        }
        comment.setBody(request.body().trim());
        return toDto(commentRepository.save(comment));
    }

    @Override
    public void delete(UUID actorId, UUID commentId) {
        Comment comment = getComment(commentId);
        if (comment.getAuthor().getId().equals(actorId)) {
            commentRepository.delete(comment);
            return;
        }
        UUID targetOwnerId = commentTargetAccessService.resolveTargetOwnerId(
                comment.getTargetType(),
                comment.getTargetId()
        );
        if (targetOwnerId != null && targetOwnerId.equals(actorId)) {
            commentRepository.delete(comment);
            return;
        }
        throw new ForbiddenException("Only the author or target owner can delete this comment");
    }

    @Override
    public void report(UUID reporterId, UUID commentId, ReportCommentRequest request) {
        Comment comment = getComment(commentId);
        commentTargetAccessService.requireCanView(
                comment.getTargetType(),
                comment.getTargetId(),
                reporterId
        );
        if (commentReportRepository.existsByReporter_IdAndComment_Id(reporterId, commentId)) {
            throw new ConflictException("You already reported this comment");
        }
        User reporter = userLookupService.getPersistenceReference(reporterId);
        String reason = request.reason() != null ? request.reason().trim() : null;
        commentReportRepository.save(new CommentReport(reporter, comment, reason));
    }

    private Comment getComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private CommentDto toDto(Comment comment) {
        User author = comment.getAuthor();
        return new CommentDto(
                comment.getId(),
                comment.getTargetType(),
                comment.getTargetId(),
                new CommentAuthorDto(author.getId(), author.getDisplayName(), author.getAvatarUrl()),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
