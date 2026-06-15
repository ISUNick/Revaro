package com.revaro.service;

import com.revaro.entity.ClaimRequest;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.ClaimStatus;
import com.revaro.repository.ClaimRequestRepository;
import com.revaro.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClaimRequestService {

    private final ClaimRequestRepository claimRequestRepository;
    private final EventRepository eventRepository;

    public ClaimRequestService(ClaimRequestRepository claimRequestRepository,
                               EventRepository eventRepository) {
        this.claimRequestRepository = claimRequestRepository;
        this.eventRepository = eventRepository;
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    public ClaimRequest submitClaim(User requester, Event event, String message) {
        // Don't allow duplicate pending claims
        boolean alreadyPending = claimRequestRepository
                .existsByRequesterAndEventAndStatus(requester, event, ClaimStatus.PENDING);
        if (alreadyPending) {
            throw new IllegalStateException(
                    "You already have a pending claim for this event.");
        }

        // Creator can't claim their own event
        if (event.getCreator().getId().equals(requester.getId())) {
            throw new IllegalArgumentException(
                    "You are already the creator of this event.");
        }

        ClaimRequest claim = new ClaimRequest();
        claim.setRequester(requester);
        claim.setEvent(event);
        claim.setMessage(message);
        claim.setStatus(ClaimStatus.PENDING);
        return claimRequestRepository.save(claim);
    }

    // ── Review (admin) ────────────────────────────────────────────────────────

    public ClaimRequest reviewClaim(Long claimId, User admin,
                                    ClaimStatus decision, String adminNotes) {
        ClaimRequest claim = claimRequestRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found."));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("This claim has already been reviewed.");
        }

        claim.setStatus(decision);
        claim.setReviewedBy(admin);
        claim.setAdminNotes(adminNotes);

        // If approved, transfer event ownership to the claimant
        if (decision == ClaimStatus.APPROVED) {
            com.revaro.entity.Event event = claim.getEvent();
            event.setCreator(claim.getRequester());
            event.setPostedByOrganizer(true);
            event.setOrganizerName(claim.getRequester().getUsername());
            eventRepository.save(event);
        }

        return claimRequestRepository.save(claim);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClaimRequest> getPendingClaims() {
        return claimRequestRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ClaimRequest> getAllClaims() {
        return claimRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ClaimRequest> getClaimsForUser(User user) {
        return claimRequestRepository.findByRequesterOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Optional<ClaimRequest> findById(Long id) {
        return claimRequestRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingClaim(User user, Event event) {
        return claimRequestRepository
                .existsByRequesterAndEventAndStatus(user, event, ClaimStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return claimRequestRepository.countByStatus(ClaimStatus.PENDING);
    }
}
