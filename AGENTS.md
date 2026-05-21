# online-check-payments

> Process ACH/eCheck payments by collecting bank account details directly and charging them through the GP API SDK in PHP, Node.js, .NET, and Java.

## Critical Patterns

1. **Treat this as a GP API repo, not a Portico repo.** Every server implementation configures `GpApiConfig`/`GpApiConfig` with `GP_API_APP_ID` and `GP_API_APP_KEY` (`php/PaymentUtils.php`, `nodejs/server.js`, `dotnet/Program.cs`, `java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java`). The `PUBLIC_API_KEY` and `SECRET_API_KEY` names in `docker-compose.yml` do not match the live code.
2. **Keep the ACH-specific fields together: routing number, account type, `SecCode.WEB`, and `bankAddress`.** All four servers sanitize the account number, validate the ABA routing checksum, set the SEC code to WEB, and attach a U.S. address before calling `charge()`. If one language drops any of those pieces, parity breaks and ACH requests may fail.
3. **Approval means both `SUCCESS` and `CAPTURED`.** The examples do not trust `responseCode` alone. Node, .NET, Java, and PHP all check for a captured result before returning success, so new flows should keep that two-part approval test.
4. **The repo is only mostly uniform.** Node, .NET, and Java expose `GET /config` plus `POST /process-payment`, but PHP still uses `config.php` and `process-payment.php`. PHP also returns `transaction_id` while the other servers return `transactionId`, so API changes must deliberately normalize all four implementations.

## Repository Structure

### PHP (plain PHP + built-in server)
- [`php/PaymentUtils.php`](php/PaymentUtils.php) — reference ACH helper; start with `configureSdk()`, `validateRoutingNumber()`, `sanitizeAccountNumber()`, `sanitizePostalCode()`, `processACHPayment()`, `sendSuccessResponse()`, `sendErrorResponse()`, `handleCORS()`, and `parseJsonInput()`.
- [`php/process-payment.php`](php/process-payment.php) — PHP POST entry point; validates required fields, maps `account_type` to `AccountType::SAVINGS` or `AccountType::CHECKING`, then calls `PaymentUtils::processACHPayment()`.
- [`php/config.php`](php/config.php) — divergent config endpoint; builds a GP API session token with `GpApiService::generateTransactionKey()` and `PMT_POST_Create_Single` permission instead of returning the direct-entry JSON used elsewhere.
- [`php/index.html`](php/index.html) — frontend copy for the PHP implementation; posts to `process-payment.php` and expects either `transaction_id` or `transactionId` in the response.
- [`php/.env.sample`](php/.env.sample) — GP API credentials sample; declares `GP_API_APP_ID`, `GP_API_APP_KEY`, `GP_API_ENVIRONMENT`, and `PORT`.

### Node.js (Express)
- [`nodejs/server.js`](nodejs/server.js) — canonical minimal server; configure the SDK near the top, then follow `sanitizePostalCode()`, `validateRoutingNumber()`, `sanitizeAccountNumber()`, the inline `app.get('/config')` handler, and the inline `app.post('/process-payment')` handler.
- [`nodejs/index.html`](nodejs/index.html) — frontend copy for the Node.js implementation; posts JSON to `process-payment`.
- [`nodejs/.env.sample`](nodejs/.env.sample) — GP API credentials plus `PORT=8000`.
- [`nodejs/run.sh`](nodejs/run.sh) — canonical run command; installs dependencies, then runs `npm start`.

### .NET (ASP.NET Core minimal API)
- [`dotnet/Program.cs`](dotnet/Program.cs) — all server logic in one file; use `ConfigureGlobalPaymentsSDK()`, `SanitizePostalCode()`, `ValidateRoutingNumber()`, `SanitizeAccountNumber()`, and `ConfigureEndpoints()`.
- [`dotnet/wwwroot/index.html`](dotnet/wwwroot/index.html) — frontend copy for the .NET implementation; posts JSON to `process-payment`.
- [`dotnet/.env.sample`](dotnet/.env.sample) — GP API credentials plus `PORT=8000`.
- [`dotnet/run.sh`](dotnet/run.sh) — canonical run command; restores packages, then runs the app.

### Java (Jakarta Servlet)
- [`java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java`](java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java) — servlet entry point; use `init()`, `configureSDK()`, `sanitizePostalCode()`, `validateRoutingNumber()`, `sanitizeAccountNumber()`, `doGet()`, and `doPost()`.
- [`java/src/main/webapp/index.html`](java/src/main/webapp/index.html) — frontend copy for the Java implementation; posts JSON to `process-payment`.
- [`java/src/main/webapp/WEB-INF/web.xml`](java/src/main/webapp/WEB-INF/web.xml) — servlet app wiring for the web archive.
- [`java/.env.sample`](java/.env.sample) — GP API credentials plus `PORT=8000`, but the running server port is actually fixed in `pom.xml`.
- [`java/run.sh`](java/run.sh) — canonical run command; packages the WAR and starts embedded Tomcat through Cargo.

### Shared
- [`README.md`](README.md) — repo overview, but some details are more optimistic than the live code; verify against the language sources before copying claims.
- [`docker-compose.yml`](docker-compose.yml) — multi-service harness with host ports 8001-8006, but it still references missing `python/` and `go/` directories and passes `PUBLIC_API_KEY`/`SECRET_API_KEY` instead of `GP_API_APP_ID`/`GP_API_APP_KEY`.
- [`Dockerfile.tests`](Dockerfile.tests) — test container for the Docker Compose workflow.
- [`package.json`](package.json) — root convenience scripts that only start the Node.js server.
- Present languages: PHP, Node.js, .NET, Java. Absent languages: Python and Go, even though `docker-compose.yml` still declares them.

## API Surface

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/config` | Node.js, .NET, and Java return `{ success: true, data: { directEntry: true, message: ... } }` so the browser knows direct bank entry is enabled. |
| `GET` | `/config.php` | PHP-only config endpoint; generates and returns a GP API access token instead of the direct-entry payload used by the other languages. |
| `POST` | `/process-payment` | Node.js, .NET, and Java validate the ACH payload, sanitize input, create an `ECheck`/`eCheck`, then execute a USD charge. |
| `POST` | `/process-payment.php` | PHP-only ACH processor; performs the same validation and charge flow through `PaymentUtils::processACHPayment()`. |

## Environment Variables

- `GP_API_APP_ID` — required in every implementation; GP API app ID used when building `GpApiConfig`.
- `GP_API_APP_KEY` — required in every implementation; GP API app key used when building `GpApiConfig`.
- `PORT` — present in every language `.env.sample`; used by Node.js (`process.env.PORT || 8000`) and .NET (`Environment.GetEnvironmentVariable("PORT") ?? "8000"`), honored by `php/run.sh`, and currently ignored by Java because `java/pom.xml` hard-codes Cargo to port 8000.
- `GP_API_ENVIRONMENT` — PHP-only sample setting; `php/.env.sample` exposes `sandbox` / `production`, but `PaymentUtils::configureSdk()` and `config.php` currently force the SDK to `Environment::TEST`.
- Default behavior when credentials are absent: the code still boots, but GP API calls will fail because the SDK config is created with empty or null credentials.

## Sandbox Credentials

| Item | Value | Notes |
| --- | --- | --- |
| Gateway | GP API | Confirmed by `GpApiConfig` usage and the `GP_API_APP_ID` / `GP_API_APP_KEY` variable names. |
| Routing number | `021000021` | JP Morgan Chase sandbox routing number used in the root README and all frontend forms. |
| Account number | Any 4-17 digit number | The servers sanitize non-digits and reject values shorter than 4 digits. |
| Account type | `checking` or `savings` | Mapped to the SDK account type enum before charging. |
| Real credentials | <https://developer.globalpay.com/> | Use GP API sandbox credentials from the developer portal; the repo does not include test API keys. |

## Architecture Summary

- **Browser-to-charge flow:** per-language `index.html` form -> client-side routing/account cleanup -> JSON POST to the language-specific payment endpoint -> server-side validation and sanitization -> `ECheck` / `eCheck` `charge()` -> JSON success or error response.
- **SDK boot flow:** `.env` load -> `GpApiConfig` with test environment, `CardNotPresent`, country `US`, and access-token account names -> `ServicesContainer.configureService(...)` -> request handlers execute charges.
- **PHP config divergence:** `php/config.php` creates a transaction key for tokenization, while the current ACH UI posts raw bank data directly and never uses that token in `php/index.html`.

## Security Notes

This is demo code, not a production-ready ACH service. Every implementation accepts raw routing and account numbers directly from the browser, and none of the servers add authentication, rate limiting, encrypted persistence, or structured secret management. PHP also sets permissive CORS headers in `config.php` and `PaymentUtils::handleCORS()`, so treat the repo as a learning sample only.

## How to Run

- PHP: `cd php && ./run.sh` — serves on port 8000 by default; override with shell `PORT` before running.
- Node.js: `cd nodejs && ./run.sh` — serves on port 8000 by default from `process.env.PORT || 8000`.
- .NET: `cd dotnet && ./run.sh` — serves on port 8000 by default from `Environment.GetEnvironmentVariable("PORT") ?? "8000"`.
- Java: `cd java && ./run.sh` — serves on port 8000 because `java/pom.xml` sets `cargo.servlet.port` to 8000.
- Docker Compose: `docker-compose up` is not currently reliable without cleanup because `docker-compose.yml` expects missing `python/` and `go/` directories and uses stale credential variable names.

## How to Verify

- Node.js / .NET / Java config endpoint: `curl http://localhost:8000/config` -> expect `success: true` with `data.directEntry: true`.
- PHP config endpoint: `curl http://localhost:8000/config.php` -> expect `success: true` with `data.accessToken` when valid GP API credentials are present.
- Payment endpoint request body for all languages: send JSON with `amount`, `account_number`, `routing_number`, `account_type`, `check_holder_name`, `street_address`, `city`, `state`, and `billing_zip`; `first_name`, `last_name`, and `email` are also accepted and forwarded where supported.
- Node.js / .NET / Java payment endpoint: `curl -X POST http://localhost:8000/process-payment -H 'Content-Type: application/json' -d '{...}'` -> expect `success: true` plus `data.transactionId`, or a 400 with `error.code` such as `VALIDATION_ERROR`, `PAYMENT_DECLINED`, or `API_ERROR`.
- PHP payment endpoint: `curl -X POST http://localhost:8000/process-payment.php -H 'Content-Type: application/json' -d '{...}'` -> expect `success: true` plus `data.transaction_id`, or an error payload with `error_code`.

## Making Changes

All four implementations are supposed to demonstrate the same ACH flow, but they are not perfectly aligned today. If you change request fields, response shapes, validation rules, or success criteria in one language, update the other three in separate commits and then re-check each frontend copy (`php/index.html`, `nodejs/index.html`, `dotnet/wwwroot/index.html`, `java/src/main/webapp/index.html`). Do not modify shared files like `README.md`, `docker-compose.yml`, `Dockerfile.tests`, or the root `package.json` in isolation without confirming the change applies across every implementation. Do not add Python or Go code unless explicitly asked; those folders are absent even though Compose still references them.

## SDK Versions

- PHP: `globalpayments/php-sdk` `^13.1`
- Node.js: `globalpayments-api` `^3.10.6`
- .NET: `GlobalPayments.Api` `9.0.16`
- Java: `com.heartlandpaymentsystems:globalpayments-sdk` `14.2.20`
