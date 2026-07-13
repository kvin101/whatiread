package com.whatiread.goal.web;

import com.whatiread.goal.api.ReadingGoalDto;
import com.whatiread.goal.api.UpsertReadingGoalRequest;
import com.whatiread.goal.service.ReadingGoalService;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.GOALS)
public class ReadingGoalController {

    private final ReadingGoalService readingGoalService;

    public ReadingGoalController(ReadingGoalService readingGoalService) {
        this.readingGoalService = readingGoalService;
    }

    @GetMapping("/{year}")
    ReadingGoalDto get(@CurrentUserId UUID userId, @PathVariable short year) {
        return readingGoalService.get(userId, year);
    }

    @PutMapping("/{year}")
    ReadingGoalDto upsert(@CurrentUserId UUID userId, @PathVariable short year, @Valid @RequestBody UpsertReadingGoalRequest request) {
        return readingGoalService.upsert(userId, year, request);
    }
}
