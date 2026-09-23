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

    // Clicking the status you already have removes your RSVP.
    // Returns the new status, or null if it was removed.
    public RsvpStatus toggleRsvp(User user, Event event, RsvpStatus status) {
        Optional<Rsvp> existing = rsvpRepository.findByUserAndEvent(user, event);
        if (existing.isEmpty()) {
            rsvpRepository.save(new Rsvp(user, event, status));
            return status;
        }

        Rsvp rsvp = existing.get();
        if (rsvp.getStatus() == status) {
            rsvpRepository.delete(rsvp);
            return null;
        }
        rsvp.setStatus(status);
        rsvpRepository.save(rsvp);
        return status;
    }

    @Transactional(readOnly = true)
    public Optional<RsvpStatus> getUserRsvpStatus(User user, Event event) {
        return rsvpRepository.findByUserAndEvent(user, event).map(Rsvp::getStatus);
    }
}
