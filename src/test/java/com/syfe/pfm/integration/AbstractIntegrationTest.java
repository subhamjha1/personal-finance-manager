package com.syfe.pfm.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.pfm.repository.CategoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Shared helpers for full-stack tests (real Spring context, security filter chain and H2 database).
 * Every test creates its own uniquely named users, so tests do not depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected CategoryRepository categoryRepository;

    /** A registered and logged-in user together with the session holding their authentication. */
    protected record TestUser(String username, long id, MockHttpSession session) {
    }

    protected static LocalDate today() {
        return LocalDate.now(ZoneId.of("UTC"));
    }

    protected TestUser newUser() throws Exception {
        String username = "user-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("username", username);
        registration.put("password", PASSWORD);
        registration.put("fullName", "Test User");
        registration.put("phoneNumber", "+1234567890");
        JsonNode registered = call(null, post("/api/auth/register"), registration, 201);

        Map<String, Object> credentials = Map.of("username", username, "password", PASSWORD);
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andReturn();
        return new TestUser(username, registered.get("userId").asLong(),
                (MockHttpSession) login.getRequest().getSession(false));
    }

    /** Performs a request (optionally within a session and with a JSON body), asserts the status and returns the JSON body. */
    protected JsonNode call(MockHttpSession session, MockHttpServletRequestBuilder request, Object body,
                            int expectedStatus) throws Exception {
        if (session != null) {
            request.session(session);
        }
        if (body != null) {
            String json = body instanceof String s ? s : objectMapper.writeValueAsString(body);
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : objectMapper.readTree(content);
    }

    protected long addTransaction(TestUser user, String amount, LocalDate date, String category) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", new BigDecimal(amount));
        body.put("date", date.toString());
        body.put("category", category);
        body.put("description", "test");
        return call(user.session(), post("/api/transactions"), body, 201).get("id").asLong();
    }

    protected void deleteTransaction(TestUser user, long id) throws Exception {
        call(user.session(), delete("/api/transactions/{id}", id), null, 200);
    }

    protected long createGoal(TestUser user, String target, LocalDate targetDate, LocalDate startDate) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goalName", "Emergency Fund");
        body.put("targetAmount", new BigDecimal(target));
        body.put("targetDate", targetDate.toString());
        if (startDate != null) {
            body.put("startDate", startDate.toString());
        }
        return call(user.session(), post("/api/goals"), body, 201).get("id").asLong();
    }

    protected static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                "expected " + expected + " but was " + actual);
    }
}
