package com.revaro.service;

import com.revaro.entity.Event;
import com.revaro.entity.Rsvp;
import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class RsvpService {

    private final RsvpRepository rsvpRepository;

    public RsvpService(RsvpRepository rsvpRepository) {
        this.rsvpRepository = rsvpRepository;
    }

    /**
     * Toggle RSVP: if user already has this status, remove it.
     * If they have a different status, switch it. If none, create it.
     * Returns the new status, or null if removed.
     */
    public RsvpStatus toggleRsvp(User user, Event event, RsvpStatus requested) {
        Optional<Rsvp> existing = rsvpRepository.findByUserAndEvent(user, event);

        if (existing.isPresent()) {
            Rsvp rsvp = existing.get();
            if (rsvp.getStatus() == requested) {
                // Same status clicked again — remove it
                rsvpRepository.delete(rsvp);
                return null;
            } else {
                // Switch to new status
                rsvp.setStatus(requested);
                rsvpRepository.save(rsvp);
                return requested;
            }
        } else {
            // No existing RSVP — create one
            Rsvp rsvp = new Rsvp(user, event, requested);
            rsvpRepository.save(rsvp);
            return requested;
        }
    }

    @Transactional(readOnly = true)
    public Optional<RsvpStatus> getUserRsvpStatus(User user, Event event) {
        return rsvpRepository.findByUserAndEvent(user, event)
                .map(Rsvp::getStatus);
    }
}
