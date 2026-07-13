package com.whatiread.comment.service;

import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.comment.service.strategy.CommentTargetAccessStrategy;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommentTargetAccessService {

    private final Map<CommentTargetType, CommentTargetAccessStrategy> strategies;

    public CommentTargetAccessService(List<CommentTargetAccessStrategy> strategyList) {
        Map<CommentTargetType, CommentTargetAccessStrategy> map = new EnumMap<>(CommentTargetType.class);
        for (CommentTargetAccessStrategy strategy : strategyList) {
            map.put(strategy.targetType(), strategy);
        }
        this.strategies = Map.copyOf(map);
    }

    public void requireCanView(CommentTargetType targetType, UUID targetId, UUID viewerId) {
        if (!canView(targetType, targetId, viewerId)) {
            throw new ForbiddenException("Cannot access comments for this target");
        }
    }

    public boolean canView(CommentTargetType targetType, UUID targetId, UUID viewerId) {
        return strategyFor(targetType).canView(targetId, viewerId);
    }

    public UUID resolveTargetOwnerId(CommentTargetType targetType, UUID targetId) {
        try {
            return strategyFor(targetType).resolveOwnerId(targetId);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    private CommentTargetAccessStrategy strategyFor(CommentTargetType targetType) {
        CommentTargetAccessStrategy strategy = strategies.get(targetType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported comment target type: " + targetType);
        }
        return strategy;
    }
}
