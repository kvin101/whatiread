package com.whatiread.reading.service;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.reading.api.ReadingStreakDto;
import com.whatiread.reading.domain.ReadingActivityDay;
import com.whatiread.reading.repository.ReadingActivityDayRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReadingStreakService {

    private final ReadingActivityDayRepository readingActivityDayRepository;
    private final UserLookupService userLookupService;

    public ReadingStreakService(
            ReadingActivityDayRepository readingActivityDayRepository,
            UserLookupService userLookupService
    ) {
        this.readingActivityDayRepository = readingActivityDayRepository;
        this.userLookupService = userLookupService;
    }

    public void recordActivity(UUID userId) {
        LocalDate today = LocalDate.now();
        if (!readingActivityDayRepository.existsByUser_IdAndActivityDate(userId, today)) {
            User user = userLookupService.getPersistenceReference(userId);
            readingActivityDayRepository.save(new ReadingActivityDay(user, today));
        }
    }

    @Transactional(readOnly = true)
    public ReadingStreakDto getStreak(UUID userId) {
        List<LocalDate> dates = readingActivityDayRepository.findActivityDatesByUserIdOrderByDateDesc(userId);
        if (dates.isEmpty()) {
            return new ReadingStreakDto(0, 0, null);
        }
        Set<LocalDate> activityDates = new HashSet<>(dates);
        LocalDate lastActivity = dates.getFirst();
        int currentStreak = computeCurrentStreak(activityDates, lastActivity);
        int longestStreak = computeLongestStreak(dates);
        return new ReadingStreakDto(currentStreak, longestStreak, lastActivity);
    }

    private static int computeCurrentStreak(Set<LocalDate> activityDates, LocalDate lastActivity) {
        LocalDate today = LocalDate.now();
        if (lastActivity.isBefore(today.minusDays(1))) {
            return 0;
        }
        LocalDate cursor = activityDates.contains(today) ? today : lastActivity;
        int streak = 0;
        while (activityDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static int computeLongestStreak(List<LocalDate> descendingDates) {
        if (descendingDates.isEmpty()) {
            return 0;
        }
        List<LocalDate> ascending = descendingDates.reversed();
        int longest = 1;
        int current = 1;
        for (int i = 1; i < ascending.size(); i++) {
            if (ascending.get(i - 1).plusDays(1).equals(ascending.get(i))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}
