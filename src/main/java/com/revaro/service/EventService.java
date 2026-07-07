package com.revaro.service;

import com.revaro.dto.EventDto;
import com.revaro.entity.Event;
import com.revaro.entity.Report;
import com.revaro.entity.Tag;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.EventRepository;
import com.revaro.repository.ReportRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.repository.TagRepository;
import com.revaro.util.FileUploadUtil;
import com.revaro.util.GeocodingUtil;
import com.revaro.util.ProfanityFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class EventService {

    private static final int PAGE_SIZE = 12;

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final FileUploadUtil fileUploadUtil;
    private final GeocodingUtil geocodingUtil;
    private final ReportRepository reportRepository;
    private final ProfanityFilter profanityFilter;
    private final TagRepository tagRepository;

    public EventService(EventRepository eventRepository,
                        RsvpRepository rsvpRepository,
                        FileUploadUtil fileUploadUtil,
                        GeocodingUtil geocodingUtil,
                        ReportRepository reportRepository,
                        ProfanityFilter profanityFilter,
                        TagRepository tagRepository) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.fileUploadUtil = fileUploadUtil;
        this.geocodingUtil = geocodingUtil;
        this.reportRepository = reportRepository;
        this.profanityFilter = profanityFilter;
        this.tagRepository = tagRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Event createEvent(EventDto dto, User creator) throws IOException {
        boolean flagged = profanityFilter.containsProfanity(dto.getTitle())
                       || profanityFilter.containsProfanity(dto.getDescription());

        String imageUrl = null;
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            imageUrl = fileUploadUtil.saveImage(dto.getImageFile());
        }

        double[] coords = null;
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            coords = geocodingUtil.geocode(dto.getCity(), dto.getState());
        }

        Set<Tag> tags = new HashSet<>();
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tags = new HashSet<>(tagRepository.findAllById(dto.getTagIds()));
        }

        // Handle specific dates (calendar picker)
        List<LocalDateTime> dates;
        if (dto.getSpecificDates() != null && !dto.getSpecificDates().isEmpty()) {
            dates = buildSpecificDates(dto);
        } else {
            dates = buildRecurringDates(dto);
        }
        boolean isSeries = dates.size() > 1;

        Event firstSaved = null;
        for (LocalDateTime date : dates) {
            Event event = new Event();
            applyDtoToEvent(event, dto, tags, coords);
            event.setEventDateTime(date);
            event.setCreator(creator);
            event.setStatus(EventStatus.ACTIVE);
            event.setFeaturedImage(imageUrl);
            event.setRecurring(isSeries);
            if (dto.isPostedByOrganizer()) {
                event.setOrganizerName(creator.getUsername());
            } else {
                event.setOrganizerName(dto.getOrganizerName());
            }

            Event saved = eventRepository.save(event);
            if (firstSaved == null) firstSaved = saved;

            if (flagged) {
                Report report = new Report();
                report.setReporter(creator);
                report.setReportType(ReportType.EVENT);
                report.setReportedEvent(saved);
                report.setReason("Auto-flagged: event content contained filtered language");
                report.setStatus(ReportStatus.PENDING);
                reportRepository.save(report);
            }
        }

        return firstSaved;
    }

    private List<LocalDateTime> buildSpecificDates(EventDto dto) {
        List<LocalDateTime> dates = new ArrayList<>();
        java.time.LocalTime time = dto.getEventDateTime() != null
                ? dto.getEventDateTime().toLocalTime()
                : java.time.LocalTime.of(9, 0);
        for (String dateStr : dto.getSpecificDates()) {
            try {
                dates.add(LocalDate.parse(dateStr).atTime(time));
            } catch (Exception ignored) {}
        }
        dates.sort(java.util.Comparator.naturalOrder());
        return dates.isEmpty() ? List.of(dto.getEventDateTime()) : dates;
    }

    private List<LocalDateTime> buildRecurringDates(EventDto dto) {
        List<LocalDateTime> dates = new ArrayList<>();
        if (dto.getEventDateTime() != null) dates.add(dto.getEventDateTime());

        if (!dto.isRecurring()
                || dto.getRecurringEndDate() == null
                || dto.getEventDateTime() == null) {
            return dates;
        }

        LocalDate endDate = dto.getRecurringEndDate();
        LocalDate current = dto.getEventDateTime().toLocalDate();
        int maxOccurrences = 52;
        int count = 0;

        if ("MONTHLY".equals(dto.getRecurringFrequency())) {
            current = current.plusMonths(1);
            while (!current.isAfter(endDate) && count < maxOccurrences) {
                dates.add(current.atTime(dto.getEventDateTime().toLocalTime()));
                current = current.plusMonths(1);
                count++;
            }
        } else {
            int days = "BIWEEKLY".equals(dto.getRecurringFrequency()) ? 14 : 7;
            current = current.plusDays(days);
            while (!current.isAfter(endDate) && count < maxOccurrences) {
                dates.add(current.atTime(dto.getEventDateTime().toLocalTime()));
                current = current.plusDays(days);
                count++;
            }
        }

        return dates;
    }

    public void createRecurringFromEdit(Long originalEventId, EventDto dto, User creator) throws IOException {
        Event original = eventRepository.findById(originalEventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (dto.getEventDateTime() == null) {
            dto.setEventDateTime(original.getEventDateTime());
        }

        List<LocalDateTime> dates;
        if (dto.getSpecificDates() != null && !dto.getSpecificDates().isEmpty()) {
            dates = buildSpecificDates(dto);
        } else {
            dates = buildRecurringDates(dto);
        }
        if (dates.size() <= 1) return;

        Set<Tag> tags = original.getTags() != null ? new HashSet<>(original.getTags()) : new HashSet<>();
        String imageUrl = original.getFeaturedImage();
        double[] coords = original.getLatitude() != null
                ? new double[]{original.getLatitude(), original.getLongitude()} : null;

        original.setRecurring(true);
        eventRepository.save(original);

        for (int i = 1; i < dates.size(); i++) {
            Event event = new Event();
            applyDtoToEvent(event, dto, tags, coords);
            event.setEventDateTime(dates.get(i));
            event.setCreator(creator);
            event.setStatus(EventStatus.ACTIVE);
            event.setFeaturedImage(imageUrl);
            event.setRecurring(true);
            if (dto.isPostedByOrganizer()) {
                event.setOrganizerName(creator.getUsername());
            } else {
                event.setOrganizerName(dto.getOrganizerName());
            }
            eventRepository.save(event);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Event updateEvent(Long eventId, EventDto dto, User requestingUser) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getCreator().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You do not have permission to edit this event.");
        }

        Set<Tag> tags = new HashSet<>();
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tags = new HashSet<>(tagRepository.findAllById(dto.getTagIds()));
        }

        double[] coords = null;
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            coords = geocodingUtil.geocode(dto.getCity(), dto.getState());
        }

        applyDtoToEvent(event, dto, tags, coords);

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
            event.setFeaturedImage(fileUploadUtil.saveImage(newImage));
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

    @Transactional(readOnly = true)
    public List<Event> findByCreatorHydrated(User user) {
        List<Event> events = eventRepository.findByCreatorOrderByCreatedAtDesc(user);
        events.forEach(this::hydrateEvent);
        return events;
    }

    private Event hydrateEvent(Event event) {
        long going = rsvpRepository.countByEventAndStatus(event, RsvpStatus.GOING);
        long interested = rsvpRepository.countByEventAndStatus(event, RsvpStatus.INTERESTED);
        event.setGoingCountCache(going);
        event.setInterestedCountCache(interested);
        return event;
    }

    /**
     * Main search + filter method used by the homepage.
     * Uses pg_trgm fuzzy matching when a query is present.
     * Filters (type, state, tag, organizer) narrow results server-side.
     */
    /**
     * Main search method.
     * searchIn: null = search everything, "tags" = tags only, "organizer" = organizer only,
     *           "location" = city/state only, "title" = title only
     */
    @Transactional(readOnly = true)
    public Page<Event> findEvents(String query, String state, String type,
                                   String tag, String organizer, String searchIn,
                                   String sort, int page) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = buildPageable(sort, page);

        boolean hasQuery = query != null && !query.isBlank();

        Page<Event> events;

        if (!hasQuery) {
            // No query — show upcoming events (default feed)
            events = eventRepository.findUpcomingEvents(now, pageable);
        } else if ("tags".equals(searchIn)) {
            events = eventRepository.searchByTagOnly(query, pageable);
        } else if ("organizer".equals(searchIn)) {
            events = eventRepository.searchByOrganizerOnly(query, pageable);
        } else if ("location".equals(searchIn)) {
            events = eventRepository.searchByLocationOnly(query, pageable);
        } else if ("title".equals(searchIn)) {
            events = eventRepository.searchByTitleOnly(query, pageable);
        } else {
            // Default: search everything with fuzzy matching
            events = eventRepository.fuzzySearchEvents(query, pageable);
        }

        return events.map(this::hydrateEvent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyDtoToEvent(Event event, EventDto dto, Set<Tag> tags, double[] coords) {
        event.setTitle(profanityFilter.filter(dto.getTitle()));
        event.setDescription(profanityFilter.filter(dto.getDescription()));
        event.setEventType(dto.getEventType());
        event.setEventDateTime(dto.getEventDateTime());
        event.setCity(dto.getCity());
        event.setState(dto.getState());
        event.setAddress(dto.getAddress());
        event.setPostedByOrganizer(dto.isPostedByOrganizer());
        event.setOfficialSourceLink(dto.getOfficialSourceLink());
        event.setSourceType(dto.getSourceType());
        event.setTags(tags);
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }
        if (coords != null) {
            event.setLatitude(coords[0]);
            event.setLongitude(coords[1]);
        }
    }

    private Pageable buildPageable(String sort, int page) {
        Sort sortOrder = switch (sort != null ? sort : "relevance") {
            case "newest" -> Sort.by("createdAt").descending();
            default       -> Sort.by("eventDateTime").ascending();
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
        if (event.getTags() != null) {
            dto.setTagIds(event.getTags().stream().map(Tag::getId).toList());
        }
        return dto;
    }
}
