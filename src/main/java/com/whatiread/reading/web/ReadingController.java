package com.whatiread.reading.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.reading.api.ReadingGoalDto;
import com.whatiread.reading.api.ReadingStatsDto;
import com.whatiread.reading.api.ReadingStreakDto;
import com.whatiread.reading.api.UpdateReadingGoalRequest;
import com.whatiread.reading.service.ReadingGoalService;
import com.whatiread.reading.service.ReadingStatsService;
import com.whatiread.reading.service.ReadingStreakService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.time.Year;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ME)
public class ReadingController {

    private final ReadingGoalService readingGoalService;
    private final ReadingStatsService readingStatsService;
    private final ReadingStreakService readingStreakService;

    public ReadingController(
            ReadingGoalService readingGoalService,
            ReadingStatsService readingStatsService,
            ReadingStreakService readingStreakService
    ) {
        this.readingGoalService = readingGoalService;
        this.readingStatsService = readingStatsService;
        this.readingStreakService = readingStreakService;
    }

    @GetMapping("/reading-goal")
    ReadingGoalDto getGoal(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) Integer year
    ) {
        return readingGoalService.getGoal(userId, resolveYear(year));
    }

    @PutMapping("/reading-goal")
    ReadingGoalDto upsertGoal(@CurrentUserId UUID userId, @Valid @RequestBody UpdateReadingGoalRequest request) {
        return readingGoalService.upsertGoal(userId, request);
    }

    @GetMapping("/stats")
    ReadingStatsDto getStats(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) Integer year
    ) {
        return readingStatsService.getStats(userId, resolveYear(year));
    }

    @GetMapping("/streak")
    ReadingStreakDto getStreak(@CurrentUserId UUID userId) {
        return readingStreakService.getStreak(userId);
    }

    private static short resolveYear(Integer year) {
        return year != null ? year.shortValue() : (short) Year.now().getValue();
    }
}
