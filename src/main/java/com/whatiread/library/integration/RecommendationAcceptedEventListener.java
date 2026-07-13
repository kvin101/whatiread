package com.whatiread.library.integration;

import com.whatiread.library.api.AddToLibraryRequest;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.event.RecommendationAcceptedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RecommendationAcceptedEventListener {

    private final LibraryService libraryService;

    public RecommendationAcceptedEventListener(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @EventListener
    @Transactional
    public void onRecommendationAccepted(RecommendationAcceptedEvent event) {
        if (!libraryService.hasBook(event.userId(), event.bookId())) {
            libraryService.add(event.userId(), new AddToLibraryRequest(event.bookId(), null, null));
        }
    }
}
