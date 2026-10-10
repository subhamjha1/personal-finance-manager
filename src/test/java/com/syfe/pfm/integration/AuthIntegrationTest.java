package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.syfe.pfm.entity.User;
import com.syfe.pfm.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private Map<String, Object> registration(String username) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", PASSWORD);
        body.put("fullName", "John Doe");
        body.put("phoneNumber", "+1234567890");
        return body;
    }

    private String email() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void register_success_returns201WithUserIdAndNoPassword() throws Exception {
        JsonNode body = call(null, post("/api/auth/register"), registration(email()), 201);
        assertEquals("User registered successfully", body.get("message").asText());
        assertTrue(body.get("userId").asLong() > 0);
        assertFalse(body.has("password"));
    }

    @Test
    void register_storesBCryptHashNotPlaintext() throws Exception {
        String username = email();
        call(null, post("/api/auth/register"), registration(username), 201);
        User stored = userRepository.findByUsername(username).orElseThrow();
        assertNotEquals(PASSWORD, stored.getPassword());
        assertTrue(stored.getPassword().startsWith("$2"));
    }

    @Test
    void register_duplicateUsername_returns409_evenWithDifferentCase() throws Exception {
        String username = email();
        call(null, post("/api/auth/register"), registration(username), 201);
        call(null, post("/api/auth/register"), registration(username), 409);
        call(null, post("/api/auth/register"), registration(username.toUpperCase()), 409);
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        Map<String, Object> badEmail = registration("not-an-email");
        call(null, post("/api/auth/register"), badEmail, 400);

        Map<String, Object> shortPassword = registration(email());
        shortPassword.put("password", "short");
        call(null, post("/api/auth/register"), shortPassword, 400);

        Map<String, Object> badPhone = registration(email());
        badPhone.put("phoneNumber", "abc");
        call(null, post("/api/auth/register"), badPhone, 400);

        Map<String, Object> blankName = registration(email());
        blankName.put("fullName", "   ");
        JsonNode error = call(null, post("/api/auth/register"), blankName, 400);
        assertEquals(400, error.get("status").asInt());
        assertTrue(error.get("message").asText().contains("fullName"));

        Map<String, Object> missingPhone = registration(email());
        missingPhone.remove("phoneNumber");
        call(null, post("/api/auth/register"), missingPhone, 400);
    }

    @Test
    void register_acceptsDifferentCountryCodes() throws Exception {
        Map<String, Object> body = registration(email());
        body.put("phoneNumber", "+919876543210");
        call(null, post("/api/auth/register"), body, 201);
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        call(null, post("/api/auth/register"), "{ not json", 400);
    }

    @Test
    void login_success_createsSessionThatAuthenticatesLaterRequests() throws Exception {
        TestUser user = newUser();
        assertNotNull(user.session());
        call(user.session(), get("/api/categories"), null, 200);
    }

    @Test
    void login_usernameIsCaseInsensitive() throws Exception {
        String username = email();
        call(null, post("/api/auth/register"), registration(username), 201);
        Map<String, Object> credentials = Map.of("username", username.toUpperCase(), "password", PASSWORD);
        JsonNode body = call(null, post("/api/auth/login"), credentials, 200);
        assertEquals("Login successful", body.get("message").asText());
    }

    @Test
    void login_invalidCredentials_return401() throws Exception {
        String username = email();
        call(null, post("/api/auth/register"), registration(username), 201);
        call(null, post("/api/auth/login"), Map.of("username", username, "password", "wrong-password"), 401);
        call(null, post("/api/auth/login"), Map.of("username", email(), "password", PASSWORD), 401);
    }

    @Test
    void login_missingFields_return400() throws Exception {
        call(null, post("/api/auth/login"), Map.of("username", email()), 400);
    }

    @Test
    void login_whileAlreadyHavingSession_rotatesSessionAndStillWorks() throws Exception {
        TestUser user = newUser();
        Map<String, Object> credentials = Map.of("username", user.username(), "password", PASSWORD);
        call(user.session(), post("/api/auth/login"), credentials, 200);
        call(user.session(), get("/api/categories"), null, 200);
    }

    @Test
    void protectedEndpoints_withoutAuthentication_return401() throws Exception {
        JsonNode error = call(null, get("/api/transactions"), null, 401);
        assertEquals(401, error.get("status").asInt());
        call(null, post("/api/transactions"), Map.of("amount", 1), 401);
        call(null, put("/api/transactions/1"), Map.of("amount", 1), 401);
        call(null, delete("/api/transactions/1"), null, 401);
        call(null, get("/api/categories"), null, 401);
        call(null, post("/api/categories"), Map.of("name", "X", "type", "INCOME"), 401);
        call(null, delete("/api/categories/X"), null, 401);
        call(null, get("/api/goals"), null, 401);
        call(null, get("/api/goals/1"), null, 401);
        call(null, post("/api/goals"), Map.of("goalName", "x"), 401);
        call(null, put("/api/goals/1"), Map.of("targetAmount", 1), 401);
        call(null, delete("/api/goals/1"), null, 401);
        call(null, get("/api/reports/monthly/2024/1"), null, 401);
        call(null, get("/api/reports/yearly/2024"), null, 401);
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        TestUser user = newUser();
        JsonNode body = call(user.session(), post("/api/auth/logout"), null, 200);
        assertEquals("Logout successful", body.get("message").asText());
        // the old session no longer authenticates
        call(user.session(), get("/api/categories"), null, 401);
    }

    @Test
    void logout_withoutSession_returns401() throws Exception {
        call(null, post("/api/auth/logout"), null, 401);
        call(new MockHttpSession(), post("/api/auth/logout"), null, 401);
    }

    @Test
    void unknownRoute_whenAuthenticated_returns404Json() throws Exception {
        TestUser user = newUser();
        call(user.session(), get("/api/does-not-exist"), null, 404);
    }

    @Test
    void wrongMethod_returns405() throws Exception {
        TestUser user = newUser();
        call(user.session(), patch("/api/categories"), null, 405);
    }
}
