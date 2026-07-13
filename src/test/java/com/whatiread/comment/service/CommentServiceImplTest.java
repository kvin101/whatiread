package com.whatiread.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.comment.api.CreateCommentRequest;
import com.whatiread.comment.api.ReportCommentRequest;
import com.whatiread.comment.api.UpdateCommentRequest;
import com.whatiread.comment.domain.Comment;
import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.comment.repository.CommentReportRepository;
import com.whatiread.comment.repository.CommentRepository;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    private static final String COMMENT_BODY = "Nice book";
    private static final String COMMENT_BODY_PADDED = "  Nice book  ";
    private static final String UPDATED_BODY = "Updated";
    private static final String SPAM_REASON = "spam";
    private static final String PASSWORD_HASH = "hash";
    private static final String AUTHOR_FIRST_NAME = "Author";
    private static final String AUTHOR_LAST_NAME = "User";

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private CommentTargetAccessService commentTargetAccessService;
    @Mock
    private CommentReportRepository commentReportRepository;
    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID authorId;
    private UUID targetId;
    private UUID commentId;
    private User author;
    private Comment comment;

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
        authorId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        author = new User("author@example.com", PASSWORD_HASH, AUTHOR_FIRST_NAME, AUTHOR_LAST_NAME);
        setId(author, authorId);
        comment = new Comment(author, CommentTargetType.BOOK, targetId, COMMENT_BODY);
        setId(comment, commentId);
    }

    @Test
    void createPersistsTrimmedComment() {
        when(userLookupService.getPersistenceReference(authorId)).thenReturn(author);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        var dto = commentService.create(
                authorId, new CreateCommentRequest(CommentTargetType.BOOK, targetId, COMMENT_BODY_PADDED));

        assertThat(dto.body()).isEqualTo(COMMENT_BODY);
        verify(commentTargetAccessService).requireCanView(CommentTargetType.BOOK, targetId, authorId);
        verify(businessMetrics).recordCommentCreated();
    }

    @Test
    void listReturnsCommentsForAccessibleTarget() {
        when(commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                eq(CommentTargetType.BOOK), eq(targetId), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(comment)));

        assertThat(commentService.list(CommentTargetType.BOOK, targetId, authorId, PageRequest.of(0, 10))
                .getContent()).hasSize(1);
    }

    @Test
    void updateAllowsAuthorToEdit() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        var dto = commentService.update(authorId, commentId, new UpdateCommentRequest(UPDATED_BODY));

        assertThat(dto.body()).isEqualTo(UPDATED_BODY);
    }

    @Test
    void updateRejectsNonAuthor() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(UUID.randomUUID(), commentId, new UpdateCommentRequest("x")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteAllowsAuthor() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));

        commentService.delete(authorId, commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteAllowsTargetOwner() {
        UUID ownerId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));
        when(commentTargetAccessService.resolveTargetOwnerId(CommentTargetType.BOOK, targetId)).thenReturn(ownerId);

        commentService.delete(ownerId, commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void reportPersistsReport() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));
        when(commentReportRepository.existsByReporter_IdAndComment_Id(authorId, commentId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(authorId)).thenReturn(author);

        commentService.report(authorId, commentId, new ReportCommentRequest(SPAM_REASON));

        verify(commentReportRepository).save(any());
    }

    @Test
    void reportRejectsDuplicate() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));
        when(commentReportRepository.existsByReporter_IdAndComment_Id(authorId, commentId)).thenReturn(true);

        assertThatThrownBy(() -> commentService.report(authorId, commentId, new ReportCommentRequest(SPAM_REASON)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsUnauthorizedActor() {
        when(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(comment));
        when(commentTargetAccessService.resolveTargetOwnerId(CommentTargetType.BOOK, targetId)).thenReturn(null);

        assertThatThrownBy(() -> commentService.delete(UUID.randomUUID(), commentId))
                .isInstanceOf(ForbiddenException.class);
    }
}
