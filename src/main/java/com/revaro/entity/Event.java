package com.revaro.entity;

import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.SourceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private boolean recurring = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // Mapped for the cascade so deleting an event cleans up its RSVPs, comments and claims
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rsvp> rsvps = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimRequest> claimRequests = new ArrayList<>();

    // Set by EventService with count queries so pages don't load every RSVP
    @Transient
    private long goingCount;

    @Transient
    private long interestedCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPast() {
        return eventDateTime != null && eventDateTime.isBefore(LocalDateTime.now());
    }

    public boolean hasFeaturedImage() {
        return featuredImage != null && !featuredImage.isBlank();
    }

    public Long getId() { return id; }
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
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public long getGoingCount() { return goingCount; }
    public void setGoingCount(long goingCount) { this.goingCount = goingCount; }
    public long getInterestedCount() { return interestedCount; }
    public void setInterestedCount(long interestedCount) { this.interestedCount = interestedCount; }
}
