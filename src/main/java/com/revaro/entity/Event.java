package com.revaro.entity;

import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.SourceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType eventType;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime eventDateTime;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 50)
    @Column(length = 50)
    private String state;

    @Size(max = 300)
    @Column(length = 300)
    private String address;

    @Size(max = 150)
    @Column(length = 150)
    private String organizerName;

    @Column(nullable = false)
    private boolean postedByOrganizer = false;

    @Size(max = 500)
    @Column(length = 500)
    private String officialSourceLink;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SourceType sourceType;

    @Column(length = 500)
    private String featuredImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // ── Relationships ─────────────────────────────────────────────────────────

    // EAGER — always needed for username display
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // Use @OrderBy to allow EAGER on multiple collections safely
    // Convert to List with @OrderColumn avoids the "multiple bags" problem
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Rsvp> rsvps = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ClaimRequest> claimRequests = new ArrayList<>();

    // Transient — set by service layer, not loaded from DB
    @Transient
    private Comment topComment;

    // Transient counts — set by service to avoid lazy loading issues
    @Transient
    private long goingCountCache = -1;

    @Transient
    private long interestedCountCache = -1;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public Event() {}

    // ── Convenience ───────────────────────────────────────────────────────────

    public long getGoingCount() {
        if (goingCountCache >= 0) return goingCountCache;
        if (rsvps == null) return 0;
        try {
            return rsvps.stream()
                    .filter(r -> com.revaro.enums.RsvpStatus.GOING.equals(r.getStatus()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getInterestedCount() {
        if (interestedCountCache >= 0) return interestedCountCache;
        if (rsvps == null) return 0;
        try {
            return rsvps.stream()
                    .filter(r -> com.revaro.enums.RsvpStatus.INTERESTED.equals(r.getStatus()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    public void setGoingCountCache(long count) { this.goingCountCache = count; }
    public void setInterestedCountCache(long count) { this.interestedCountCache = count; }

    public boolean isUpcoming() {
        return eventDateTime != null && eventDateTime.isAfter(LocalDateTime.now());
    }

    public boolean isPast() {
        return eventDateTime != null && eventDateTime.isBefore(LocalDateTime.now());
    }

    public boolean hasFeaturedImage() {
        return featuredImage != null && !featuredImage.isBlank();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public LocalDateTime getEventDateTime() { return eventDateTime; }
    public void setEventDateTime(LocalDateTime eventDateTime) { this.eventDateTime = eventDateTime; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public boolean isPostedByOrganizer() { return postedByOrganizer; }
    public void setPostedByOrganizer(boolean postedByOrganizer) { this.postedByOrganizer = postedByOrganizer; }

    public String getOfficialSourceLink() { return officialSourceLink; }
    public void setOfficialSourceLink(String officialSourceLink) { this.officialSourceLink = officialSourceLink; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getFeaturedImage() { return featuredImage; }
    public void setFeaturedImage(String featuredImage) { this.featuredImage = featuredImage; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public List<Rsvp> getRsvps() { return rsvps; }
    public void setRsvps(List<Rsvp> rsvps) { this.rsvps = rsvps; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public List<ClaimRequest> getClaimRequests() { return claimRequests; }
    public void setClaimRequests(List<ClaimRequest> claimRequests) { this.claimRequests = claimRequests; }

    public Comment getTopComment() { return topComment; }
    public void setTopComment(Comment topComment) { this.topComment = topComment; }
}
