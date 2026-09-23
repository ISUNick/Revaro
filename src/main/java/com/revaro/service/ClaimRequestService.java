package com.revaro.service;

import com.revaro.entity.ClaimRequest;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.ClaimStatus;
import com.revaro.exception.NotFoundException;
import com.revaro.repository.ClaimRequestRepository;
import com.revaro.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    public ClaimRequest submitClaim(User requester, Event event, String message) {
        if (event.getCreator().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You are already the creator of this event.");
        }
        if (hasPendingClaim(requester, event)) {
            throw new IllegalStateException("You already have a pending claim for this event.");
        }
        return claimRequestRepository.save(new ClaimRequest(requester, event, message));
    }

    public void reviewClaim(Long claimId, User admin, ClaimStatus decision, String adminNotes) {
        ClaimRequest claim = claimRequestRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim not found"));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("This claim has already been reviewed.");
        }
        claim.setStatus(decision);
        claim.setAdminNotes(adminNotes);
        claim.setReviewedBy(admin);
        claim.setReviewedAt(LocalDateTime.now());

        // Approving hands the event over to whoever claimed it
        if (decision == ClaimStatus.APPROVED) {
            Event event = claim.getEvent();
            event.setCreator(claim.getRequester());
            event.setPostedByOrganizer(true);
            event.setOrganizerName(claim.getRequester().getUsername());
            eventRepository.save(event);
        }
        claimRequestRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingClaim(User user, Event event) {
        return claimRequestRepository.existsByRequesterAndEventAndStatus(user, event, ClaimStatus.PENDING);
    }

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
    public long countPending() {
        return claimRequestRepository.countByStatus(ClaimStatus.PENDING);
    }
}
