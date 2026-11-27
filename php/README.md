# PHP ACH Payment Example

This example demonstrates ACH (eCheck) payment processing using PHP and the Global Payments SDK.

## Requirements

- PHP 8.2.35+, 8.3.36+, 8.4.27+, or 8.5.12+ for the web server
- Composer
- Global Payments account and API credentials

## Project Structure

- `process-payment.php` - Payment processing script
- `index.php` - Client-side payment form
- `composer.json` - Project dependencies
- `.env.sample` - Template for environment variables
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
   composer install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   ./run.sh
   ```

## Implementation Details

### Application Structure
The application uses a simple PHP structure:
- Static HTML form for payment collection
- Separate PHP script for payment processing
- Composer for dependency management

### SDK Configuration
Global Payments SDK configuration using environment variables:
- Loads GP_API_APP_ID and GP_API_APP_KEY from the .env file
- Builds a GpApiConfig for the sandbox (TEST) environment, channel CNP, country US

### Payment Processing
Payment processing flow:
1. Client submits the bank account, routing number, account type, account holder name and billing address
2. Server checks the routing number checksum and strips non digits from the account number
3. Creates an eCheck with SEC code WEB and the account holder name, and passes the billing address and customer name and email with the charge
4. Charges the amount in USD through GP API
5. Returns success only when GP API answers SUCCESS and CAPTURED

### Error Handling
Implements comprehensive error handling:
- Catches and processes API exceptions
- Returns appropriate error messages
- Handles edge cases gracefully

## API Endpoints

### GET /config.php
Returns a GP API access token (not used by the ACH form).

### POST /process-payment.php
Charges a bank account (ACH) through GP API.

Request body (JSON):
- `amount`, `account_number`, `routing_number`, `account_type` (`checking` or `savings`), `check_holder_name` (required)
- `street_address`, `city`, `state`, `billing_zip`, `first_name`, `last_name`, `email` (optional)

Response (Success):
```json
{"success": true, "message": "ACH payment processed successfully", "data": {"transaction_id": "TRN_xxx", "amount": 10, "currency": "USD", "status": "approved", "response_code": "SUCCESS", "response_message": "CAPTURED"}}
```

Response (Error):
```json
{"success": false, "message": "ACH payment failed: Invalid routing number", "timestamp": "...", "error_code": "API_ERROR"}
```

## Security Considerations

This example demonstrates basic implementation. For production use, consider:
- Implementing additional input validation
- Adding request rate limiting
- Including security headers
- Implementing proper logging
- Adding payment fraud prevention measures
- Using HTTPS in production
- Implementing CSRF protection
- Configuring proper session handling
- Setting appropriate PHP security directives

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `composer install` fails | Ensure PHP 8.2.35+, 8.3.36+, 8.4.27+, or 8.5.12+ and Composer are installed. Run `php -v` to check version |
| Port already in use | Set `PORT=8001` before `./run.sh` |
| `.env` not loading | Verify `.env` file exists in the language directory (not project root) |
| SDK errors on startup | Run `composer update` to get latest SDK version |

---

## Resources

- [Parent Project README](../README.md)
- [Global Payments Developer Portal](https://developer.globalpayments.com/)
- [API Reference](https://developer.globalpayments.com/api/references-overview)
- [PHP SDK](https://github.com/globalpayments/php-sdk)
- [Test Cards](https://developer.globalpayments.com/resources/test-cards)
