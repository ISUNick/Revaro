package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.Report;
import com.revaro.entity.User;
import com.revaro.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportService {

    private static final String AUTO_FLAG_REASON = "Auto-flagged: contained filtered language";

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // Posts that trip the profanity filter still go up censored, but get flagged for an admin
    public void flagEvent(Event event, User author) {
        reportRepository.save(Report.forEvent(author, event, AUTO_FLAG_REASON));
    }

    public void flagComment(Comment comment, User author) {
        reportRepository.save(Report.forComment(author, comment, AUTO_FLAG_REASON));
    }

    // The report methods return false if this user already reported the same thing
    public boolean reportEvent(Event event, User reporter, String reason) {
        if (reportRepository.existsByReporterIdAndReportedEventId(reporter.getId(), event.getId())) {
            return false;
        }
        reportRepository.save(Report.forEvent(reporter, event, reason));
        return true;
    }

    public boolean reportComment(Comment comment, User reporter, String reason) {
        if (reportRepository.existsByReporterIdAndReportedCommentId(reporter.getId(), comment.getId())) {
            return false;
        }
        reportRepository.save(Report.forComment(reporter, comment, reason));
        return true;
    }

    public boolean reportUser(User user, User reporter, String reason) {
        if (user.getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("You can't report yourself.");
        }
        if (reportRepository.existsByReporterIdAndReportedUserId(reporter.getId(), user.getId())) {
            return false;
        }
        reportRepository.save(Report.forUser(reporter, user, reason));
        return true;
    }
}
