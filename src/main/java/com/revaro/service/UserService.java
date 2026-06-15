package com.revaro.service;

import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.EventRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.repository.UserRepository;
import com.revaro.util.FileUploadUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadUtil fileUploadUtil;
    private final RsvpRepository rsvpRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       FileUploadUtil fileUploadUtil,
                       RsvpRepository rsvpRepository,
                       CommentLikeRepository commentLikeRepository,
                       CommentRepository commentRepository,
                       EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadUtil = fileUploadUtil;
        this.rsvpRepository = rsvpRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    public User register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered.");
        }
        User user = new User(username, email, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    // ── Find ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // ── Profile update ────────────────────────────────────────────────────────

    public User updateProfile(User user, MultipartFile profileImageFile,
                               String carYear, String carMake, String carModel,
                               MultipartFile carImageFile) throws IOException {

        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            if (user.getProfileImage() != null) {
                fileUploadUtil.deleteImage(user.getProfileImage());
            }
            String filename = fileUploadUtil.saveImage(profileImageFile);
            user.setProfileImage(filename);
        }

        user.setCarYear(carYear);
        user.setCarMake(carMake);
        user.setCarModel(carModel);

        if (carImageFile != null && !carImageFile.isEmpty()) {
            if (user.getCarImage() != null) {
                fileUploadUtil.deleteImage(user.getCarImage());
            }
            String filename = fileUploadUtil.saveImage(carImageFile);
            user.setCarImage(filename);
        }

        return userRepository.save(user);
    }

    // ── Rev Points ────────────────────────────────────────────────────────────

    /**
     * Rev Points calculated entirely via DB queries — no lazy collection access.
     * 5 pts per event created + 1 per Going RSVP received + 1 per comment like received
     */
    @Transactional(readOnly = true)
    public long calculateRevPoints(User user) {
        // 5 points per event created
        long eventCount = eventRepository.countByCreator(user);
        long points = eventCount * 5;

        // 1 point per Going RSVP on their events
        points += rsvpRepository.countGoingRsvpsForUserEvents(user);

        // 1 point per like received on their comments
        points += commentLikeRepository.countLikesReceivedByUser(user);

        return points;
    }

    @Transactional(readOnly = true)
    public long countEventsForUser(User user) {
        return eventRepository.countByCreator(user);
    }
}
