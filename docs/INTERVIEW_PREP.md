# Technical Interview Preparation

> Answers describe the code **as written**. The code is **unverified** (never compiled/run) — say so honestly if asked, and be ready to demo after you run it.

**1. Why Spring Boot with a layered architecture?** Assignment mandates it; controllers (HTTP only) → services (business rules, transactions) → repositories (Spring Data). DTOs (Java records) decouple the API from entities.

**2. How does registration work?** `AuthController.register` validates `RegisterRequest` (email regex, 8–72 char password, phone regex, non-blank name); `AuthService` lower-cases the username, 409 if it exists, BCrypt-encodes the password, saves, returns `{message,userId}` with 201.

**3. Why BCrypt?** Adaptive, salted, built into Spring Security. The hash is never returned; max input is 72 bytes, hence the `@Size(max=72)`.

**4. How does login/session auth work?** `AuthController.login` calls `AuthenticationManager` (DaoAuthenticationProvider + `AppUserDetailsService`), rotates the session id if one exists, stores the `SecurityContext` via `HttpSessionSecurityContextRepository`. Tomcat issues `JSESSIONID`; later requests are authenticated by `SecurityContextHolderFilter` loading the context from that session.

**5. How is the cookie configured?** `HttpOnly`, `SameSite=Lax`, `Secure` via `SESSION_COOKIE_SECURE`/prod profile, 30-minute timeout. Behind Render's proxy `forward-headers-strategy=framework` makes the app aware of HTTPS.

**6. What about CSRF?** Disabled deliberately because clients (curl/test scripts) cannot send tokens. Mitigations: SameSite=Lax blocks cross-site cookie on unsafe methods, and JSON-only bodies can't be sent by plain HTML forms. Trade-off: weaker than tokens for same-site attackers/older browsers.

**7. How is logout implemented?** Invalidates the HttpSession, clears the context, and sends an expiring cookie. It requires authentication, so unauthenticated logout returns 401 (as the PDF lists).

**8. How is user isolation enforced?** The user id comes only from the `UserPrincipal` in the security context (`@AuthenticationPrincipal`), never from the request. Every repository query filters by that id (`findByIdAndUserId`, Specification with `user.id`, aggregate queries with `t.user.id`). Goals are loaded by id then ownership-checked (403).

**9. Why 403 for goals but 404 for transactions?** The PDF lists 403 for goals/categories but not for transactions. Trade-off: 403 reveals existence; I documented it (A1/A2).

**10. Why `BigDecimal`?** Exact decimal arithmetic for money; `double` has binary rounding errors. Columns are `DECIMAL(19,2)`, input limited to 2 fraction digits (`@Digits`), percentages use `divide(..., 2, HALF_UP)`.

**11. How are categories modelled?** One `Category` table; default categories have `user = null` (shared, read-only), custom ones reference their user. Visible set = defaults + own. Unique `(user_id, name)` constraint plus a case-insensitive check in the service (the DB constraint doesn't cover NULL users, so defaults are protected by the seeder's idempotent check and the service check).

**12. How are defaults seeded?** `DataInitializer` (ApplicationRunner) inserts the 7 defaults if absent at each start.

**13. How is transaction type determined?** From the category's type; any client-sent `type` is ignored.

**14. How does filtering work?** `TransactionService.list` composes a JPA `Specification` (user id always, plus optional dates/categoryId/category name/type) and sorts by date desc, id desc. A null-parameter `OR` JPQL was avoided because PostgreSQL can't infer types of null parameters.

**15. Why can't the date be updated?** Requirement. A different `date` in the PUT body returns 400; omitted or identical is fine.

**16. How is goal progress calculated?** `GoalService.toResponse`: sum of INCOME minus sum of EXPENSE for the user with `transactionDate >= startDate`; percentage = progress×100/target (2 dp HALF_UP); remaining = target − progress. Computed on every read, so deletes are reflected immediately; no caching.

**17. Are percentages capped?** No — negative and >100 % values are returned as-is (documented).

**18. How are reports aggregated?** JPQL `select category.name, sum(amount) … group by category.name` in the database for each type, with inclusive calendar bounds (`YearMonth.atEndOfMonth`); net = totals difference; `TreeMap` gives deterministic order. Two queries per report, no N+1.

**19. How are validation and errors handled?** Bean Validation on DTOs → `MethodArgumentNotValidException`; business errors throw `ApiException(status,msg)`; `@RestControllerAdvice` maps these plus malformed JSON, type mismatches, 404/405/415 and integrity violations to a uniform JSON body. Security 401/403 use `JsonSecurityErrorHandler`. Unknown errors → generic 500, details only in logs.

**20. How is "today" determined, and why a `Clock`?** An injected `Clock` (zone from `app.timezone`) makes date rules testable (fixed clock in unit tests) and configurable.

**21. Testing strategy?** MockMvc integration tests exercise the real security chain and H2 (auth, isolation, CRUD, goals, reports, error codes); Mockito unit tests cover service rules with a fixed clock; JaCoCo enforces ≥ 80 % line coverage in `mvn verify`.

**22. Deployment configuration?** Multi-stage Dockerfile; `render.yaml` provisions a web service and PostgreSQL; `prod` profile reads DB_* env vars, forces Secure cookies; health check `/actuator/health`; `PORT` binding.

**23. Known limitations / trade-offs?** In-memory sessions (single instance; use Spring Session + JDBC/Redis to scale), `ddl-auto=update` instead of Flyway, no rate limiting, CSRF disabled with mitigations, interpretation of ambiguous spec points.

**24. How would you scale/improve it?** Flyway migrations, Spring Session JDBC, pagination on transaction lists, idempotency/optimistic locking, account lockout, audit logging, Testcontainers PostgreSQL tests.

**25. What would you do first now?** Run `mvn clean verify`, fix any compile/test failures, run the official script, then deploy and re-run it against the live URL.
