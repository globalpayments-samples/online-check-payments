# Node.js ACH Payment Example

This example demonstrates ACH (eCheck) payment processing using Express.js and the Global Payments SDK.

## Requirements

- Node.js 24 or later
- npm (Node Package Manager)
- Global Payments account and API credentials

## Project Structure

- `server.js` - Main application file containing server setup and payment processing
- `index.html` - Client-side payment form
- `package.json` - Project dependencies and scripts
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
   npm install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   node server.js
   ```

## Implementation Details

### Server Setup
The application uses Express.js to create a web server that:
- Serves the payment page (index.html)
- Processes payment requests
- Provides configuration endpoint for client-side SDK
- Handles JSON and form-encoded requests

### SDK Configuration
Global Payments SDK configuration using environment variables:
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
- Differentiates between API and general errors
- Returns appropriate error messages

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
{"success": false, "message": "Payment processing failed", "error": {"code": "SERVER_ERROR or API_ERROR", "details": "Invalid routing number"}}
```

## Security Considerations

This example demonstrates basic implementation. For production use, consider:
- Implementing additional input validation
- Adding request rate limiting
- Including security headers
- Implementing proper logging
- Adding payment fraud prevention measures
- Using HTTPS in production
- Configuring Cross-Origin Resource Sharing (CORS) appropriately

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `npm install` fails | Ensure Node.js 24+ is installed. Run `node -v` to check version |
| Port already in use | Set `PORT=3001` in `.env` or kill existing process on port 8000 |
| `.env` not loading | Verify `.env` file exists in the language directory (not project root) |
| Module not found errors | Delete `node_modules/` and run `npm install` again |

---

## Resources

- [Parent Project README](../README.md)
- [Global Payments Developer Portal](https://developer.globalpayments.com/)
- [API Reference](https://developer.globalpayments.com/api/references-overview)
- [Node.js SDK](https://github.com/globalpayments/node-sdk)
- [Test Cards](https://developer.globalpayments.com/resources/test-cards)
