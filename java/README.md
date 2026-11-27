# Java ACH Payment Example

This example demonstrates ACH (eCheck) payment processing using Jakarta EE and the Global Payments SDK.

## Requirements

- Java 21 or later
- Maven
- Global Payments account and API credentials

## Project Structure

- `src/main/java/com/globalpayments/example/ProcessPaymentServlet.java` - Main servlet handling payment processing
- `src/main/webapp/index.html` - Client-side payment form
- `src/main/webapp/WEB-INF/web.xml` - Web application configuration
- `.env.sample` - Template for environment variables
- `pom.xml` - Project dependencies and build configuration
- `run.sh` - Convenience script to run the application

## Setup

1. Clone this repository
2. Copy `.env.sample` to `.env`
3. Update `.env` with your Global Payments credentials:
   ```
   GP_API_APP_ID=your_gp_api_app_id
   GP_API_APP_KEY=your_gp_api_app_key
   ```
4. Install dependencies:
   ```bash
   mvn clean install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   mvn clean package cargo:run
   ```

## Implementation Details

### Servlet Configuration
The application uses Jakarta EE servlets to:
- Handle payment processing requests
- Serve configuration data
- Process form submissions

### SDK Configuration
Global Payments SDK configuration is handled in the servlet's init method:
- Loads GP_API_APP_ID and GP_API_APP_KEY from the .env file
- Builds a GpApiConfig for the sandbox (TEST) environment, channel CNP, country US

### Payment Processing
Payment processing flow:
1. Client submits the bank account, routing number, account type, account holder name and billing address
2. Server checks the routing number checksum and strips non digits from the account number
3. Creates an eCheck with SEC code WEB and the bank address
4. Charges the amount in USD through GP API
5. Returns success only when GP API answers SUCCESS and CAPTURED

### Error Handling
Implements comprehensive error handling:
- Catches and processes API exceptions
- Returns appropriate HTTP status codes
- Provides meaningful error messages

## API Endpoints

### GET /config
Tells the page that direct bank entry is enabled.

### POST /process-payment
Charges a bank account (ACH) through GP API.

Request body (JSON):
- `amount`, `account_number`, `routing_number`, `account_type` (`checking` or `savings`), `check_holder_name` (required)
- `street_address`, `city`, `state`, `billing_zip`, `first_name`, `last_name`, `email` (optional)

Response (Success):
```json
{"success": true, "message": "Payment successful! Transaction ID: TRN_xxx", "data": {"transactionId": "TRN_xxx", "responseCode": "SUCCESS", "responseMessage": "CAPTURED"}}
```

Response (Error):
```json
{"success": false, "message": "Invalid routing number", "error": {"code": "VALIDATION_ERROR", "details": "Invalid routing number"}}
```

## Security Considerations

This example demonstrates basic implementation. For production use, consider:
- Implementing additional input validation
- Adding request rate limiting
- Including security headers
- Implementing proper logging
- Adding payment fraud prevention measures
- Configuring secure session management

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `mvn` command not found | Install Maven 3.6+. Run `mvn -v` to check version |
| Build fails | Ensure Java 21+ is installed. Run `java -version` to check |
| Port already in use | Stop other services on port 8000, or modify `pom.xml` cargo config |
| `.env` not loading | Verify `.env` file exists in the language directory (not project root) |

---

## Resources

- [Parent Project README](../README.md)
- [Global Payments Developer Portal](https://developer.globalpayments.com/)
- [API Reference](https://developer.globalpayments.com/api/references-overview)
- [Java SDK](https://github.com/globalpayments/java-sdk)
- [Test Cards](https://developer.globalpayments.com/resources/test-cards)
