package com.revaro.dto;

import com.revaro.enums.EventType;
import com.revaro.enums.EventStatus;
import com.revaro.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for the event create/edit form.
 */
public class EventDto {

    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Title must be under 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must be under 5000 characters")
    private String description;

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotNull(message = "Event date and time are required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime eventDateTime;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String state;

    @Size(max = 300)
    private String address;

    // Organizer fields
    private boolean postedByOrganizer = false;

    @Size(max = 150)
    private String organizerName;

    // Source
    @Size(max = 500)
    private String officialSourceLink;

    private SourceType sourceType;

    // Status (edit only)
    private EventStatus status;

    // Image upload
    private MultipartFile imageFile;

    // Existing image filename (edit mode — keep if no new upload)
    private String existingImage;

    private Double latitude;
    private Double longitude;

    // Tags
    private List<Long> tagIds = new ArrayList<>();

    // ── Recurring fields ──────────────────────────────────────────────────────
    private boolean recurring = false;

    // WEEKLY, BIWEEKLY, MONTHLY
    private String recurringFrequency = "WEEKLY";

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recurringEndDate;

    // ── Getters & Setters ─────────────────────────────────────────────────────

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

    public boolean isPostedByOrganizer() { return postedByOrganizer; }
    public void setPostedByOrganizer(boolean postedByOrganizer) { this.postedByOrganizer = postedByOrganizer; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOfficialSourceLink() { return officialSourceLink; }
    public void setOfficialSourceLink(String officialSourceLink) { this.officialSourceLink = officialSourceLink; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public String getExistingImage() { return existingImage; }
    public void setExistingImage(String existingImage) { this.existingImage = existingImage; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds != null ? tagIds : new ArrayList<>(); }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getRecurringFrequency() { return recurringFrequency; }
    public void setRecurringFrequency(String recurringFrequency) { this.recurringFrequency = recurringFrequency; }

    public LocalDate getRecurringEndDate() { return recurringEndDate; }
    public void setRecurringEndDate(LocalDate recurringEndDate) { this.recurringEndDate = recurringEndDate; }
}
