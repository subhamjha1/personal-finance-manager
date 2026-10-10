package com.syfe.pfm.service;

import com.syfe.pfm.dto.AuthDtos.RegisterRequest;
import com.syfe.pfm.dto.AuthDtos.RegisterResponse;
import com.syfe.pfm.entity.User;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration logic. Passwords are hashed with the configured {@link PasswordEncoder} (BCrypt). */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Usernames are e-mail addresses and are matched case-insensitively (stored trimmed, lower-case). */
    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Registers a new user.
     *
     * @throws ApiException 409 when the username already exists
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Username already exists");
        }
        User user = userRepository.save(new User(username, passwordEncoder.encode(request.password()),
                request.fullName().trim(), request.phoneNumber()));
        return new RegisterResponse("User registered successfully", user.getId());
    }
}
