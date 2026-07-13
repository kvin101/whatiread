package com.whatiread.goal.service;

import com.whatiread.goal.api.ReadingGoalDto;
import com.whatiread.goal.api.UpsertReadingGoalRequest;
import com.whatiread.goal.domain.ReadingGoal;
import com.whatiread.goal.repository.ReadingGoalRepository;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReadingGoalServiceImpl implements ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;
    private final UserLookupService userLookupService;
    private final LibraryService libraryService;

    public ReadingGoalServiceImpl(
            ReadingGoalRepository readingGoalRepository,
            UserLookupService userLookupService,
            LibraryService libraryService
    ) {
        this.readingGoalRepository = readingGoalRepository;
        this.userLookupService = userLookupService;
        this.libraryService = libraryService;
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingGoalDto get(UUID userId, short year) {
        ReadingGoal goal = readingGoalRepository.findByUser_IdAndGoalYear(userId, year)
                .orElseThrow(() -> new ResourceNotFoundException("Reading goal not found for " + year));
        return toDto(userId, goal);
    }

    @Override
    public ReadingGoalDto upsert(UUID userId, short year, UpsertReadingGoalRequest request) {
        User user = userLookupService.getPersistenceReference(userId);
        ReadingGoal goal = readingGoalRepository.findByUser_IdAndGoalYear(userId, year)
                .orElseGet(() -> new ReadingGoal(user, year, request.targetBooks()));
        goal.setTargetBooks(request.targetBooks());
        readingGoalRepository.save(goal);
        return toDto(userId, goal);
    }

    private ReadingGoalDto toDto(UUID userId, ReadingGoal goal) {
        int booksRead = libraryService.countBooksReadInYear(userId, goal.getGoalYear());
        return new ReadingGoalDto(goal.getGoalYear(), goal.getTargetBooks(), booksRead);
    }
}
