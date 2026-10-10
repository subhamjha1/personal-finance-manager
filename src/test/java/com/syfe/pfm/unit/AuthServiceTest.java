package com.syfe.pfm.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.syfe.pfm.dto.AuthDtos.RegisterRequest;
import com.syfe.pfm.dto.AuthDtos.RegisterResponse;
import com.syfe.pfm.entity.User;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.repository.UserRepository;
import com.syfe.pfm.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

    private final RegisterRequest request =
            new RegisterRequest(" User@Example.com ", "password123", " John Doe ", "+1234567890");

    @Test
    void register_hashesPasswordAndNormalizesUsername() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().getUsername());
        assertEquals("HASHED", captor.getValue().getPassword());
        assertEquals("John Doe", captor.getValue().getFullName());
        assertEquals("User registered successfully", response.message());
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void normalizeUsername_handlesNull() {
        assertNull(AuthService.normalizeUsername(null));
        assertEquals("a@b.co", AuthService.normalizeUsername("  A@B.co "));
    }
}
