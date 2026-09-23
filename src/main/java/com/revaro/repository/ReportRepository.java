package com.revaro.repository;

import com.revaro.entity.Report;
import com.revaro.entity.User;
import com.revaro.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByStatus(ReportStatus status);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    boolean existsByReporterIdAndReportedEventId(Long reporterId, Long eventId);

    boolean existsByReporterIdAndReportedCommentId(Long reporterId, Long commentId);

    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long userId);

    @Modifying
    @Query("DELETE FROM Report r WHERE r.reporter = :user")
    void deleteByReporter(@Param("user") User user);

    @Modifying
    @Query("UPDATE Report r SET r.reviewedBy = null WHERE r.reviewedBy = :user")
    void clearReviewer(@Param("user") User user);
}
