package com.syfe.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response bodies of the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Pragmatic e-mail pattern (requires a dotted domain). */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";

    /** International phone number: optional '+', 7-15 digits; no particular country code is assumed. */
    public static final String PHONE_REGEX = "^\\+?[0-9]{7,15}$";

    public record RegisterRequest(
            @NotBlank(message = "username is required")
            @Size(max = 254, message = "username must be at most 254 characters")
            @Pattern(regexp = EMAIL_REGEX, message = "username must be a valid email address")
            String username,

            @NotBlank(message = "password is required")
            @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
            String password,

            @NotBlank(message = "fullName is required")
            @Size(max = 100, message = "fullName must be at most 100 characters")
            String fullName,

            @NotBlank(message = "phoneNumber is required")
            @Pattern(regexp = PHONE_REGEX, message = "phoneNumber must be a valid phone number (7-15 digits, optional leading +)")
            String phoneNumber) {
    }

    public record RegisterResponse(String message, Long userId) {
    }

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password) {
    }
}
