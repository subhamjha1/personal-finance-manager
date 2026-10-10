package com.syfe.pfm.controller;

import com.syfe.pfm.dto.AuthDtos.LoginRequest;
import com.syfe.pfm.dto.AuthDtos.RegisterRequest;
import com.syfe.pfm.dto.AuthDtos.RegisterResponse;
import com.syfe.pfm.dto.MessageResponse;
import com.syfe.pfm.exception.ApiException;
import com.syfe.pfm.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registration, login (creates the server-side session) and logout (destroys it). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Authenticates credentials, rotates any existing session id (anti-fixation) and stores the context in the session. */
    @PostMapping("/login")
    public MessageResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                    .unauthenticated(AuthService.normalizeUsername(request.username()), request.password()));
        } catch (AuthenticationException e) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return new MessageResponse("Login successful");
    }

    /** Invalidates the server-side session and expires the session cookie. Requires authentication (401 otherwise). */
    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        String cookieName = httpRequest.getServletContext().getSessionCookieConfig().getName();
        Cookie cookie = new Cookie(cookieName != null ? cookieName : "JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(httpRequest.isSecure());
        cookie.setPath(httpRequest.getContextPath().isEmpty() ? "/" : httpRequest.getContextPath());
        httpResponse.addCookie(cookie);
        return new MessageResponse("Logout successful");
    }
}
