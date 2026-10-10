# Test Plan

> **Status: UNVERIFIED.** Tests are written but have never been compiled or executed.

| Layer | Classes | Technique |
|---|---|---|
| Integration (full stack, H2, real security chain) | `AuthIntegrationTest`, `CategoryIntegrationTest`, `TransactionIntegrationTest`, `GoalIntegrationTest`, `ReportIntegrationTest` | `@SpringBootTest` + MockMvc with `MockHttpSession`; each test registers unique users |
| Unit (Mockito) | `AuthServiceTest`, `CategoryServiceTest`, `GoalServiceTest`, `ReportServiceTest`, `TransactionServiceTest` | `@ExtendWith(MockitoExtension.class)`, fixed `Clock` |

Coverage gate: JaCoCo `check` fails `mvn verify` below 80 % line coverage (bundle; the `main` class is excluded).
Report: `target/site/jacoco/index.html`.

## Manual / script checks still to be done
1. `bash financial_manager_tests.sh <BASE_URL>` against a local run, then the deployed URL (script not provided to the author).
2. Browser/cURL cookie check over HTTPS on Render (Secure cookie).
3. Restart check: data persists with PostgreSQL profile.
