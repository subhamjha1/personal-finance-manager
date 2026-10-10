# Requirements & Traceability

> **Status: UNVERIFIED.** Written without the ability to compile or run. The "Test" column names the
> automated test intended to cover each requirement; none has been executed yet.
> Source of truth: the assignment PDF. The official `financial_manager_tests.sh` was **not** available.

## Decisions on unspecified / ambiguous behaviour

| # | Topic | Decision |
|---|-------|----------|
| A1 | Cross-user access to a **goal** | `403` if the goal exists but belongs to someone else, `404` if it does not exist (PDF lists both codes for goals). |
| A2 | Cross-user access to a **transaction** | `404` (PDF lists no 403 for transactions; also avoids confirming existence). |
| A3 | Custom category of another user | Not visible → `404` on delete; cannot be used in transactions (`400`). |
| A4 | Delete default category | `403`. Delete category referenced by transactions → `400`. Unknown category → `404`. |
| A5 | Category name uniqueness | Case-insensitive among categories visible to the user (defaults + own). Different users may reuse a name. |
| A6 | Transaction with unknown category | `400`. Category lookup is case-insensitive; the stored name is returned. |
| A7 | Update with a different `date` | `400` ("date cannot be modified"). Same date is accepted. Omitted/`null` fields keep their value. |
| A8 | `categoryId` filter | Numeric internal id (`GET /api/transactions?categoryId=`), as in the PDF example. Extra filters: `category` (name) and `type`. Date bounds inclusive. |
| A9 | Goal progress | `income - expenses` for transactions dated **on or after** `startDate` (inclusive), no end bound. Not capped (may be negative or >100%). Percentage rounded HALF_UP to 2 dp. |
| A10 | Goal date rules | `targetDate` must be after today and after `startDate`. `startDate` defaults to today (configured time zone, default UTC). `startDate` cannot be updated. |
| A11 | Password rules | 8–72 characters (BCrypt limit). No composition rules. |
| A12 | Phone number | Optional `+`, 7–15 digits, no country code assumed. |
| A13 | Username | E-mail, stored/compared trimmed + lower-case. |
| A14 | Money | `BigDecimal`, max 2 fraction digits (more → `400`), stored scale 2. |
| A15 | Reports | Calendar boundaries, inclusive; year 1–9999, month 1–12 else `400`. Yearly report is a direct aggregation of the year. |
| A16 | CSRF | Disabled deliberately; mitigated by `SameSite=Lax`, HttpOnly/Secure cookie and JSON-only bodies (see `SecurityConfig`). |
| A17 | Logout | Requires a valid session (`401` otherwise), as the PDF lists 401 for logout. |
| A18 | Future dates / "today" | Evaluated in `app.timezone` (default UTC). A client far ahead of UTC just after local midnight may see "today" rejected as future. |

## Requirement → implementation → test

| Req | Implementation | Test |
|-----|----------------|------|
| Register (email, password, fullName, phone), 201/400/409 | `AuthController.register`, `AuthService`, `AuthDtos` | `AuthIntegrationTest.register_*`, `AuthServiceTest` |
| Password hashing | `BCryptPasswordEncoder` | `register_storesBCryptHashNotPlaintext` |
| Login → session cookie, 401 on bad creds | `AuthController.login`, `SecurityConfig` | `login_*` |
| Logout invalidates session | `AuthController.logout` | `logout_*` |
| All other endpoints need auth | `SecurityConfig` (`anyRequest().authenticated()`) | `protectedEndpoints_withoutAuthentication_return401` |
| Data isolation | user id from `@AuthenticationPrincipal`; all queries filtered by user id | `userIsolation_*`, `customCategories_areIsolatedBetweenUsers`, `reports_containOnly…` |
| Create transaction (positive amount, no future date, valid category, optional description) | `TransactionService.create` | `create_*` |
| List newest first + filters (date range, category, type) | `TransactionService.list` (JPA Specification) | `list_*` |
| Update any field except date | `TransactionService.update` | `update_*` |
| Delete; excluded from goals/reports | on-demand aggregation | `deletingTransaction_immediatelyChangesGoalProgress`, `reports_excludeDeletedTransactions` |
| Default categories immutable | `DataInitializer`, `CategoryService.delete` | `list_containsSevenDefaults…`, `delete_defaultCategory_returns403` |
| Custom categories, unique per user | `CategoryService.create` + DB unique constraint | `create_duplicateName_returns409…` |
| Cannot delete referenced category | `CategoryService.delete` | `delete_categoryReferencedByTransaction…` |
| Goals: create/list/get/update/delete | `GoalService`, `GoalController` | `GoalIntegrationTest`, `GoalServiceTest` |
| Goal progress/percentage/remaining | `GoalService.toResponse` | `progress_*`, `GoalServiceTest.get_calculates…` |
| Monthly/yearly reports | `ReportService` | `ReportIntegrationTest`, `ReportServiceTest` |
| Validation, 4xx codes, no 5xx for known cases | `GlobalExceptionHandler` + Bean Validation | `*_invalidInput_returns400` tests |
| ≥ 80 % coverage | JaCoCo `check` (LINE ≥ 0.80) in `mvn verify` | build gate |
