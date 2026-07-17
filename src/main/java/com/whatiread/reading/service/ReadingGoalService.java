package com.whatiread.reading.service;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.reading.api.ReadingGoalDto;
import com.whatiread.reading.api.UpdateReadingGoalRequest;
import com.whatiread.reading.domain.ReadingGoal;
import com.whatiread.reading.repository.ReadingGoalRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;
    private final UserLookupService userLookupService;

    public ReadingGoalService(ReadingGoalRepository readingGoalRepository, UserLookupService userLookupService) {
        this.readingGoalRepository = readingGoalRepository;
        this.userLookupService = userLookupService;
    }

    @Transactional(readOnly = true)
    public ReadingGoalDto getGoal(UUID userId, short year) {
        return readingGoalRepository.findByUser_IdAndYear(userId, year)
                .map(this::toDto)
                .orElse(new ReadingGoalDto(year, 0, null));
    }

    public ReadingGoalDto upsertGoal(UUID userId, UpdateReadingGoalRequest request) {
        short year = request.year();
        ReadingGoal goal = readingGoalRepository.findByUser_IdAndYear(userId, year)
                .orElseGet(() -> {
                    User user = userLookupService.getPersistenceReference(userId);
                    return new ReadingGoal(user, year, request.targetBooks(), request.targetPages());
                });
        goal.setTargetBooks(request.targetBooks());
        goal.setTargetPages(request.targetPages());
        return toDto(readingGoalRepository.save(goal));
    }

    private ReadingGoalDto toDto(ReadingGoal goal) {
        return new ReadingGoalDto(goal.getYear(), goal.getTargetBooks(), goal.getTargetPages());
    }
}
