package com.whatiread.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.comment.service.strategy.CommentTargetAccessStrategy;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentTargetAccessServiceTest {

    private CommentTargetAccessStrategy shelfStrategy;
    private CommentTargetAccessService service;
    private UUID targetId;
    private UUID viewerId;

    @BeforeEach
    void setUp() {
        shelfStrategy = mock(CommentTargetAccessStrategy.class);
        when(shelfStrategy.targetType()).thenReturn(CommentTargetType.SHELF);
        service = new CommentTargetAccessService(List.of(shelfStrategy));
        targetId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
    }

    @Test
    void requireCanViewThrowsWhenStrategyDeniesAccess() {
        when(shelfStrategy.canView(targetId, viewerId)).thenReturn(false);

        assertThatThrownBy(() -> service.requireCanView(CommentTargetType.SHELF, targetId, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void resolveTargetOwnerIdReturnsNullWhenTargetMissing() {
        when(shelfStrategy.resolveOwnerId(targetId)).thenThrow(new ResourceNotFoundException("missing"));

        assertThat(service.resolveTargetOwnerId(CommentTargetType.SHELF, targetId)).isNull();
    }

    @Test
    void rejectsUnsupportedTargetTypes() {
        assertThatThrownBy(() -> service.canView(CommentTargetType.BOOK, targetId, viewerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported comment target type");
    }
}
