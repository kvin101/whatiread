package com.whatiread.comment.repository;

import com.whatiread.comment.domain.CommentReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {

    boolean existsByReporter_IdAndComment_Id(UUID reporterId, UUID commentId);
}
