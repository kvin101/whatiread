package com.whatiread.comment.service;

import com.whatiread.comment.api.CommentDto;
import com.whatiread.comment.api.CreateCommentRequest;
import com.whatiread.comment.api.ReportCommentRequest;
import com.whatiread.comment.api.UpdateCommentRequest;
import com.whatiread.comment.domain.CommentTargetType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentDto create(UUID authorId, CreateCommentRequest request);

    Page<CommentDto> list(CommentTargetType targetType, UUID targetId, UUID viewerId, Pageable pageable);

    CommentDto update(UUID authorId, UUID commentId, UpdateCommentRequest request);

    void delete(UUID actorId, UUID commentId);

    void report(UUID reporterId, UUID commentId, ReportCommentRequest request);
}
