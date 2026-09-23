package com.revaro.entity;

import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    // Only one of these is set, depending on reportType. SET NULL keeps the report
    // around for history after the content is deleted.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_event_id", foreignKey = @ForeignKey(name = "fk_report_event",
            foreignKeyDefinition = "FOREIGN KEY (reported_event_id) REFERENCES events(id) ON DELETE SET NULL"))
    private Event reportedEvent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_comment_id", foreignKey = @ForeignKey(name = "fk_report_comment",
            foreignKeyDefinition = "FOREIGN KEY (reported_comment_id) REFERENCES comments(id) ON DELETE SET NULL"))
    private Comment reportedComment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_user_id", foreignKey = @ForeignKey(name = "fk_report_user",
            foreignKeyDefinition = "FOREIGN KEY (reported_user_id) REFERENCES users(id) ON DELETE SET NULL"))
    private User reportedUser;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(length = 500)
    private String adminNotes;

    protected Report() {
    }

    private Report(User reporter, ReportType reportType, String reason) {
        this.reporter = reporter;
        this.reportType = reportType;
        this.reason = reason;
    }

    public static Report forEvent(User reporter, Event event, String reason) {
        Report report = new Report(reporter, ReportType.EVENT, reason);
        report.reportedEvent = event;
        return report;
    }

    public static Report forComment(User reporter, Comment comment, String reason) {
        Report report = new Report(reporter, ReportType.COMMENT, reason);
        report.reportedComment = comment;
        return report;
    }

    public static Report forUser(User reporter, User user, String reason) {
        Report report = new Report(reporter, ReportType.USER, reason);
        report.reportedUser = user;
        return report;
    }

    public Long getId() { return id; }
    public User getReporter() { return reporter; }
    public ReportType getReportType() { return reportType; }
    public Event getReportedEvent() { return reportedEvent; }
    public void setReportedEvent(Event reportedEvent) { this.reportedEvent = reportedEvent; }
    public Comment getReportedComment() { return reportedComment; }
    public void setReportedComment(Comment reportedComment) { this.reportedComment = reportedComment; }
    public User getReportedUser() { return reportedUser; }
    public void setReportedUser(User reportedUser) { this.reportedUser = reportedUser; }
    public String getReason() { return reason; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
