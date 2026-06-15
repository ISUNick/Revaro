package com.revaro.entity;

import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A user report for abusive content — can target an event, comment, or user profile.
 */
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

    // Target IDs — only one will be set depending on reportType
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_event_id")
    private Event reportedEvent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_comment_id")
    private Comment reportedComment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(length = 500)
    private String adminNotes;

    // Getters & setters
    public Long getId() { return id; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    public Event getReportedEvent() { return reportedEvent; }
    public void setReportedEvent(Event reportedEvent) { this.reportedEvent = reportedEvent; }
    public Comment getReportedComment() { return reportedComment; }
    public void setReportedComment(Comment reportedComment) { this.reportedComment = reportedComment; }
    public User getReportedUser() { return reportedUser; }
    public void setReportedUser(User reportedUser) { this.reportedUser = reportedUser; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
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
