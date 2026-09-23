package com.revaro.service;

import com.revaro.entity.User;
import com.revaro.exception.NotFoundException;
import com.revaro.repository.UserRepository;
import com.revaro.util.FileUploadUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadUtil fileUploadUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       FileUploadUtil fileUploadUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadUtil = fileUploadUtil;
    }

    public User register(String username, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("That username is taken, try a different one.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("That email is already registered.");
        }
        return userRepository.save(new User(username, normalizedEmail, passwordEncoder.encode(rawPassword)));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<User> searchByUsername(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrderByUsernameAsc(query.trim(),
                PageRequest.of(0, limit));
    }

    public void updateProfile(Long userId, MultipartFile profileImage, String carYear, String carMake,
                              String carModel, MultipartFile carImage) throws IOException {
        User user = getById(userId);
        user.setProfileImage(replaceImage(user.getProfileImage(), profileImage));
        user.setCarImage(replaceImage(user.getCarImage(), carImage));
        user.setCarYear(blankToNull(carYear));
        user.setCarMake(blankToNull(carMake));
        user.setCarModel(blankToNull(carModel));
        userRepository.save(user);
    }

    // Uploads first so a failed upload doesn't leave the user without their old image
    private String replaceImage(String currentUrl, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return currentUrl;
        }
        String newUrl = fileUploadUtil.saveImage(file);
        fileUploadUtil.deleteImage(currentUrl);
        return newUrl;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
