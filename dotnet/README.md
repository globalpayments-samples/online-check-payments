# .NET ACH Payment Example

This example demonstrates ACH (eCheck) payment processing using ASP.NET Core and the Global Payments SDK.

## Requirements

- .NET 6.0 or later
- Global Payments account and API credentials

## Project Structure

- `Program.cs` - Main application file containing server setup and payment processing
- `wwwroot/index.html` - Client-side payment form
- `.env.sample` - Template for environment variables
- `run.sh` - Convenience script to run the application
- `appsettings.json` - Application configuration file

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
   dotnet restore
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   dotnet run
   ```

## Implementation Details

### Server Setup
The application uses ASP.NET Core's minimal API approach to create a lightweight web server that:
- Serves static files from wwwroot directory
- Processes payment requests
- Provides configuration endpoint for client-side SDK

### SDK Configuration
The Global Payments SDK is configured using environment variables and the GpApiConfig class:
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
{"success": false, "message": "Payment processing failed", "error": {"code": "VALIDATION_ERROR or API_ERROR", "details": "Invalid routing number"}}
```

## Security Considerations

This example demonstrates basic implementation. For production use, consider:
- Implementing additional input validation
- Adding request rate limiting
- Including security headers
- Implementing proper logging
- Adding payment fraud prevention measures

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `dotnet` command not found | Install .NET SDK 6.0+. Run `dotnet --version` to check |
| Build fails | Run `dotnet restore` before `dotnet run` |
| Port already in use | Set a different port in `launchSettings.json` or use `--urls` flag |
| `.env` not loading | Verify `.env` file exists in the language directory (not project root) |

---

## Resources

- [Parent Project README](../README.md)
- [Global Payments Developer Portal](https://developer.globalpayments.com/)
- [API Reference](https://developer.globalpayments.com/api/references-overview)
- [.NET SDK](https://github.com/globalpayments/dotnet-sdk)
- [Test Cards](https://developer.globalpayments.com/resources/test-cards)
