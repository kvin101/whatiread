package com.whatiread.reading.service;

import com.whatiread.library.repository.UserBookRepository;
import com.whatiread.reading.api.ReadingStatsDto;
import com.whatiread.reading.domain.ReadingGoal;
import com.whatiread.reading.repository.ReadingGoalRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReadingStatsService {

    private final UserBookRepository userBookRepository;
    private final ReadingGoalRepository readingGoalRepository;

    public ReadingStatsService(UserBookRepository userBookRepository, ReadingGoalRepository readingGoalRepository) {
        this.userBookRepository = userBookRepository;
        this.readingGoalRepository = readingGoalRepository;
    }

    public ReadingStatsDto getStats(UUID userId, short year) {
        int booksRead = userBookRepository.countBooksReadInYear(userId, year);
        int pagesRead = userBookRepository.sumPagesReadInYear(userId, year);
        ReadingGoal goal = readingGoalRepository.findByUser_IdAndYear(userId, year).orElse(null);
        Integer targetBooks = goal != null && goal.getTargetBooks() > 0 ? goal.getTargetBooks() : null;
        Integer targetPages = goal != null ? goal.getTargetPages() : null;
        return new ReadingStatsDto(
                year,
                booksRead,
                pagesRead,
                targetBooks,
                targetPages,
                progressPercent(booksRead, targetBooks),
                progressPercent(pagesRead, targetPages)
        );
    }

    private static Double progressPercent(int actual, Integer target) {
        if (target == null || target <= 0) {
            return null;
        }
        return Math.min(100.0, (actual * 100.0) / target);
    }
}
