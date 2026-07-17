package com.whatiread.activity.web;

import com.whatiread.activity.api.ActivityItemDto;
import com.whatiread.activity.service.ActivityFeedService;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.web.ApiPaths;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ACTIVITY)
public class ActivityController {

    private final ActivityFeedService activityFeedService;

    public ActivityController(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @GetMapping
    CursorPage<ActivityItemDto> list(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return activityFeedService.list(userId, cursor, limit);
    }
}
