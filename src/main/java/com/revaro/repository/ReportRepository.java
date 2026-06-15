package com.revaro.repository;

import com.revaro.entity.Report;
import com.revaro.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByStatus(ReportStatus status);
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    boolean existsByReporterIdAndReportedEventId(Long reporterId, Long eventId);
    boolean existsByReporterIdAndReportedCommentId(Long reporterId, Long commentId);
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long userId);
}
