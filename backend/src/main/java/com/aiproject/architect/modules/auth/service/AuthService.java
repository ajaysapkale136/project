
package com.aiproject.architect.modules.auth.service;

import com.aiproject.architect.modules.auth.dto.AuthResponse;
import com.aiproject.architect.modules.auth.dto.LoginRequest;
import com.aiproject.architect.modules.auth.dto.RegisterRequest;
import com.aiproject.architect.modules.auth.entity.UserAccount;
import com.aiproject.architect.modules.auth.entity.UserRole;
import com.aiproject.architect.modules.auth.repository.UserAccountRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long jwtExpiration;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.expiration}") long jwtExpiration) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtExpiration = jwtExpiration;
    }

    // =========================
    // USER REGISTRATION
    // =========================

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        validatePassword(request.getPassword());

        UserAccount user = new UserAccount();

        user.setEmail(email);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Never accept the role from the registration request.
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        try {
            UserAccount savedUser = userRepository.saveAndFlush(user);
            return generateAuthResponse(savedUser);

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }
    }

    // =========================
    // USER LOGIN
    // =========================

    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password."
                        )
                );

        if (!user.isEnabled()) {
            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        return generateAuthResponse(user);
    }

    // =========================
    // JWT TOKEN GENERATION
    // =========================

    private AuthResponse generateAuthResponse(UserAccount user) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtExpiration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ai-project-architect")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(header, claims)
                )
                .getTokenValue();

        return new AuthResponse(
                token,
                "Bearer",
                jwtExpiration / 1000,
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name()
        );
    }

    // =========================
    // EMAIL NORMALIZATION
    // =========================

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    // =========================
    // PASSWORD VALIDATION
    // =========================

    private void validatePassword(String password) {

        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters."
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes."
            );
        }
    }
}
