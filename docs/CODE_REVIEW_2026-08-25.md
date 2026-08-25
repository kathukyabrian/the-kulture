# Code review — 2026-08-25

## Scope and method

Reviewed the Angular/Capacitor client, Spring Boot API, Liquibase migrations, and Python location simulator. The review combined builds/tests with a focused inspection of authentication, authorization, configuration, persistence mappings, and secret handling. This is a targeted review, not a full penetration test.

## Summary

| Severity | Count |
| --- | ---: |
| Critical | 2 |
| High | 2 |
| Medium | 2 |

The most urgent work is rotating/removing the exposed database credentials and preventing production startup from creating predictable privileged accounts.

## Findings

### 1. Critical — Database credentials and a public database address are hard-coded

**Evidence:** `api/src/main/resources/application.yaml:5-7` supplies a public PostgreSQL host, username, and password as environment-variable fallbacks.

**Impact:** Anyone who can read this workspace or an artifact containing this configuration may be able to access the production database directly. Environment overrides do not make the fallback secret safe. Although this file is not currently tracked by Git, it is present in the application source tree and can be packaged or copied into an image.

**Recommendation:** Immediately rotate the database password, restrict database ingress, remove all secret-bearing defaults, and require `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` at deployment. Check Git history, container registries, backups, and CI logs for prior exposure.

### 2. Critical — Predictable privileged accounts are created on every application startup

**Evidence:** `UserBootstrapConfiguration.java:19-28` is unconditional configuration and creates `admin@kulture.test` / `admin123`, plus predictable crew and traveller credentials, whenever those emails do not exist.

**Impact:** A fresh or restored production database gains a known active administrator account. Deleting the account does not remediate the problem because a later restart recreates it.

**Recommendation:** Remove this bootstrap from production. If demo data is required, put it behind an explicit `dev`/`test` Spring profile and use random or externally supplied credentials. Rotate/delete any accounts already created this way.

### 3. High — Suspended or demoted users retain their session privileges

**Evidence:** Login stores the user's role in the HTTP session (`AuthResource.java:22-28`). Status and role changes only update the database (`UserService.java:101-114`); they do not invalidate sessions or re-evaluate authorities. `/api/auth/me` checks only that the stored session is authenticated and that the user still exists (`AuthResource.java:46-50`).

**Impact:** An administrator who is demoted or suspended can continue calling `/api/admin/**` until their session expires or is manually invalidated. The same stale-authority behavior applies to other role changes.

**Recommendation:** Re-load the account and its current status/role on every request, or maintain a session registry and expire all sessions when status/role/password changes. Add integration tests proving suspension and demotion take effect immediately.

### 4. High — CSRF protection is disabled while authentication uses cookies

**Evidence:** `SecurityConfiguration.java:21-22` enables CORS but disables CSRF globally; `AuthResource.java:27` persists authentication in an HTTP session, and the client sends credentials with requests.

**Impact:** Cookie-authenticated state-changing endpoints lack Spring Security's CSRF defense. CORS is not a substitute for CSRF protection and the current setup is fragile as new endpoints/content types are introduced.

**Recommendation:** Enable CSRF with a cookie-based token suitable for the Angular client (for example `CookieCsrfTokenRepository`) and send the token on mutations. If the API is intentionally stateless, replace cookie sessions with an appropriate stateless authentication design and explicitly configure session creation policy.

### 5. Medium — The API test suite cannot load the application context

**Evidence:** `./mvnw test` runs one test and errors. Hibernate reports: `routes.geometry` is a CLOB in H2 but the entity expects `text`/VARCHAR. `Route.java:38-42` hard-codes `columnDefinition = "text"`, while the test profile uses H2 PostgreSQL mode with schema validation (`application-test.yaml:3-12`).

**Impact:** The API has no passing automated safety net; even the context smoke test fails before exercising endpoints. The same mismatch may recur for `waypoints` after `geometry` is resolved.

**Recommendation:** Make the mapping portable (for example, use an appropriate large-text JDBC/JPA mapping without a database-specific column definition), or run persistence integration tests against PostgreSQL/Testcontainers. Keep schema validation enabled and add service/security endpoint tests.

### 6. Medium — Frontend tests are not runnable in the documented project environment

**Evidence:** `npm test -- --watch=false --browsers=ChromeHeadless` builds the test bundle but Karma reports that no `ChromeHeadless` binary exists and asks for `CHROME_BIN`. The command nevertheless exits with status 0. `package.json:10` provides only the default `ng test` script and no browser-independent or CI-specific setup.

**Impact:** CI or local automation can report success without executing a browser test, allowing regressions through. There is currently only one Angular spec file.

**Recommendation:** Install/configure a known browser in CI and make missing browser startup fail the job, or adopt a supported browser-independent unit-test runner. Add tests for authentication guards/interceptors, location queue recovery, and key component workflows.

## Verification results

| Check | Result |
| --- | --- |
| `npm run build` | Passed; production bundle generated. Initial bundle is 1.56 MB raw / 336.85 kB estimated transfer. |
| `npm test -- --watch=false --browsers=ChromeHeadless` | No tests executed; Chrome binary missing, but command exited 0. |
| `./mvnw test` | Failed: 1 test, 1 application-context error caused by schema type validation. |
| `python3 -m unittest -v` | Passed: 3 tests. |

## Suggested remediation order

1. Rotate the database credential, restrict access, and remove secret defaults.
2. Disable/remove unconditional demo-user bootstrapping and clean up deployed demo accounts.
3. Make authorization reflect account changes immediately and add regression tests.
4. Restore CSRF protection (or intentionally adopt stateless authentication).
5. Repair API persistence tests, then make frontend tests execute reliably in CI.
