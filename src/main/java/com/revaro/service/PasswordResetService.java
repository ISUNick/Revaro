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
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Does nothing for unknown emails so the form can't be used to find out who has an account
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            tokenRepository.deleteAllByUser(user);
            PasswordResetToken token = tokenRepository.save(new PasswordResetToken(user));
            emailService.sendPasswordReset(user.getEmail(), user.getUsername(), token.getToken());
        });
    }

    @Transactional(readOnly = true)
    public boolean isValidToken(String token) {
        return findValidToken(token).isPresent();
    }

    // Returns false if the token is expired, already used, or doesn't exist
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetToken = findValidToken(token);
        if (resetToken.isEmpty()) {
            return false;
        }
        User user = resetToken.get().getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.get().setUsed(true);
        return true;
    }

    private Optional<PasswordResetToken> findValidToken(String token) {
        return tokenRepository.findByToken(token).filter(PasswordResetToken::isValid);
    }
}
