package com.whatiread.comment.web;

import com.whatiread.comment.api.CommentDto;
import com.whatiread.comment.api.CreateCommentRequest;
import com.whatiread.comment.api.ReportCommentRequest;
import com.whatiread.comment.api.UpdateCommentRequest;
import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.comment.service.CommentService;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.COMMENTS)
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CommentDto create(@CurrentUserId UUID userId, @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(userId, request);
    }

    @GetMapping
    Page<CommentDto> list(
            @CurrentUserId UUID userId,
            @RequestParam CommentTargetType targetType,
            @RequestParam UUID targetId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return commentService.list(targetType, targetId, userId, pageable);
    }

    @PatchMapping("/{commentId}")
    CommentDto update(
            @CurrentUserId UUID userId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return commentService.update(userId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID userId, @PathVariable UUID commentId) {
        commentService.delete(userId, commentId);
    }

    @PostMapping("/{commentId}/report")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void report(@CurrentUserId UUID userId, @PathVariable UUID commentId, @Valid @RequestBody ReportCommentRequest request) {
        commentService.report(userId, commentId, request);
    }
}
