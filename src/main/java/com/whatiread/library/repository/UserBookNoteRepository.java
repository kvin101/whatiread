package com.whatiread.library.repository;

import com.whatiread.library.domain.UserBookNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBookNoteRepository extends JpaRepository<UserBookNote, UUID> {

    List<UserBookNote> findByUserBookIdOrderByCreatedAtAsc(UUID userBookId);

    Optional<UserBookNote> findByIdAndUserBook_IdAndUserBook_User_Id(UUID noteId, UUID userBookId, UUID userId);
}
