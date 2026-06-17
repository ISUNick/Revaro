package com.revaro.service;

import com.revaro.entity.PasswordResetToken;
import com.revaro.entity.User;
import com.revaro.repository.PasswordResetTokenRepository;
import com.revaro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Initiates a password reset — creates a token and sends the email.
     * Always returns success message even if email not found (security best practice).
     */
    public void initiateReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return; // Silent — don't reveal if email exists

        User user = userOpt.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteAllByUser(user);

        // Create new token
        PasswordResetToken token = new PasswordResetToken(user);
        tokenRepository.save(token);

        // Send email
        emailService.sendPasswordReset(user.getEmail(), user.getUsername(), token.getToken());
    }

    /**
     * Validates a reset token — returns the user if valid, empty if not.
     */
    @Transactional(readOnly = true)
    public Optional<User> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(PasswordResetToken::isValid)
                .map(PasswordResetToken::getUser);
    }

    /**
     * Completes the reset — updates the password and invalidates the token.
     */
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token)
                .filter(PasswordResetToken::isValid);

        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate token
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }
}
