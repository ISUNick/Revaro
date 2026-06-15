package com.revaro.service;

import com.revaro.dto.EventDto;
import com.revaro.entity.Report;
import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import com.revaro.repository.ReportRepository;
import com.revaro.util.ProfanityFilter;
import com.revaro.util.GeocodingUtil;
import java.util.List;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.EventRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    private static final int PAGE_SIZE = 10;

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final GeocodingUtil geocodingUtil;
    private final ReportRepository reportRepository;
    private final ProfanityFilter profanityFilter;
    private final FileUploadUtil fileUploadUtil;

    public EventService(EventRepository eventRepository,
                        RsvpRepository rsvpRepository,
                        FileUploadUtil fileUploadUtil,
                        GeocodingUtil geocodingUtil,
                        ReportRepository reportRepository,
                        ProfanityFilter profanityFilter) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.fileUploadUtil = fileUploadUtil;
        this.geocodingUtil = geocodingUtil;
        this.reportRepository = reportRepository;
        this.profanityFilter = profanityFilter;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Event createEvent(EventDto dto, User creator) throws IOException {
        // Check for profanity before applying DTO so flagged is in scope
        boolean flagged = profanityFilter.containsProfanity(dto.getTitle())
                       || profanityFilter.containsProfanity(dto.getDescription());

        Event event = new Event();
        applyDto(event, dto);
        event.setCreator(creator);
        event.setStatus(EventStatus.ACTIVE);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String filename = fileUploadUtil.saveImage(dto.getImageFile());
            event.setFeaturedImage(filename);
        }

        if (dto.isPostedByOrganizer()) {
            event.setOrganizerName(creator.getUsername());
        } else {
            event.setOrganizerName(dto.getOrganizerName());
        }

        Event saved = eventRepository.save(event);
        if (flagged) {
            Report report = new Report();
            report.setReporter(creator);
            report.setReportType(ReportType.EVENT);
            report.setReportedEvent(saved);
            report.setReason("Auto-flagged: event content contained filtered language");
            report.setStatus(ReportStatus.PENDING);
            reportRepository.save(report);
        }
        return saved;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Event updateEvent(Long eventId, EventDto dto, User requestingUser) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getCreator().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You do not have permission to edit this event.");
        }

        applyDto(event, dto);

        if (dto.isPostedByOrganizer()) {
            event.setOrganizerName(event.getCreator().getUsername());
        } else {
            event.setOrganizerName(dto.getOrganizerName());
        }

        MultipartFile newImage = dto.getImageFile();
        if (newImage != null && !newImage.isEmpty()) {
            if (event.getFeaturedImage() != null) {
                fileUploadUtil.deleteImage(event.getFeaturedImage());
            }
            String filename = fileUploadUtil.saveImage(newImage);
            event.setFeaturedImage(filename);
        }

        return eventRepository.save(event);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteEvent(Long eventId, User requestingUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getCreator().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You do not have permission to delete this event.");
        }

        if (event.getFeaturedImage() != null) {
            fileUploadUtil.deleteImage(event.getFeaturedImage());
        }

        eventRepository.delete(event);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id).map(this::hydrateEvent);
    }

    /**
     * Public method to hydrate a list of events with RSVP counts.
     */
    @Transactional(readOnly = true)
    public List<Event> hydrateEvents(List<Event> events) {
        events.forEach(this::hydrateEvent);
        return events;
    }

    /**
     * Load and hydrate events for a user in a single transaction.
     */
    @Transactional(readOnly = true)
    public List<Event> findByCreatorHydrated(com.revaro.entity.User user) {
        List<Event> events = eventRepository.findByCreatorOrderByCreatedAtDesc(user);
        events.forEach(this::hydrateEvent);
        return events;
    }

    /**
     * Hydrate transient count fields while still inside the transaction.
     * This avoids LazyInitializationException after the session closes.
     */
    private Event hydrateEvent(Event event) {
        long going = rsvpRepository.countByEventAndStatus(event, RsvpStatus.GOING);
        long interested = rsvpRepository.countByEventAndStatus(event, RsvpStatus.INTERESTED);
        event.setGoingCountCache(going);
        event.setInterestedCountCache(interested);
        return event;
    }

    @Transactional(readOnly = true)
    public Page<Event> findEvents(String query, String state, String type, String sort, int page) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = buildPageable(sort, page);

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasType  = type  != null && !type.isBlank();
        boolean hasState = state != null && !state.isBlank();

        Page<Event> events;

        if (hasType) {
            EventType eventType;
            try {
                eventType = EventType.valueOf(type);
            } catch (IllegalArgumentException e) {
                events = eventRepository.findUpcomingEvents(now, pageable);
                return events.map(this::hydrateEvent);
            }
            if (hasQuery) {
                events = eventRepository.searchEventsByType(query, eventType, now, pageable);
            } else {
                events = eventRepository.findByEventType(eventType, now, pageable);
            }
        } else if (hasState && !hasQuery) {
            events = eventRepository.findByState(state, now, pageable);
        } else if (hasQuery) {
            // Search includes past events so people can find history
            events = eventRepository.searchEvents(query, pageable);
        } else {
            // Default feed: upcoming only
            events = eventRepository.findUpcomingEvents(now, pageable);
        }

        return events.map(this::hydrateEvent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyDto(Event event, EventDto dto) {
        // Filter profanity from title and description
        String title = profanityFilter.filter(dto.getTitle());
        String description = profanityFilter.filter(dto.getDescription());
        boolean flagged = profanityFilter.containsProfanity(dto.getTitle())
                       || profanityFilter.containsProfanity(dto.getDescription());

        event.setTitle(title);
        event.setDescription(description);
        event.setEventType(dto.getEventType());
        event.setEventDateTime(dto.getEventDateTime());
        event.setCity(dto.getCity());
        event.setState(dto.getState());
        event.setAddress(dto.getAddress());
        event.setPostedByOrganizer(dto.isPostedByOrganizer());
        event.setOfficialSourceLink(dto.getOfficialSourceLink());
        event.setSourceType(dto.getSourceType());
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }
        // Geocode city+state -> lat/lng for distance sorting
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            double[] coords = geocodingUtil.geocode(dto.getCity(), dto.getState());
            if (coords != null) {
                event.setLatitude(coords[0]);
                event.setLongitude(coords[1]);
            }
        }
    }


    private Pageable buildPageable(String sort, int page) {
        Sort sortOrder = switch (sort != null ? sort : "relevance") {
            case "newest"   -> Sort.by("createdAt").descending();
            // relevance and distance both use date ascending server-side;
            // the client-side JS re-sorts by the relevance/distance formula
            default         -> Sort.by("eventDateTime").ascending();
        };
        return PageRequest.of(Math.max(0, page), PAGE_SIZE, sortOrder);
    }

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventType(event.getEventType());
        dto.setEventDateTime(event.getEventDateTime());
        dto.setCity(event.getCity());
        dto.setState(event.getState());
        dto.setAddress(event.getAddress());
        dto.setPostedByOrganizer(event.isPostedByOrganizer());
        dto.setOrganizerName(event.getOrganizerName());
        dto.setOfficialSourceLink(event.getOfficialSourceLink());
        dto.setSourceType(event.getSourceType());
        dto.setStatus(event.getStatus());
        dto.setExistingImage(event.getFeaturedImage());
        return dto;
    }
}