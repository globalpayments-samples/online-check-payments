# ACH/eCheck Online Payment Processing

This project demonstrates ACH/eCheck payment processing using the Global Payments GP API across multiple programming languages. Each implementation provides secure electronic check processing with direct bank account entry, routing number validation, and comprehensive customer data integration.

## Available Implementations

- [.NET Core](./dotnet/) - ASP.NET Core web application
- [Java](./java/) - Jakarta EE servlet-based web application
- [Node.js](./nodejs/) - Express.js web application
- [PHP](./php/) - PHP web application

## Key Features

### ACH/eCheck Processing
- **Direct Bank Account Entry** - Secure processing with routing and account numbers
- **GP API Integration** - Server-side payment processing using Global Payments GP API
- **Account Type Support** - Both checking and savings account processing
- **Routing Number Validation** - Industry-standard checksum algorithm validation
- **Account Number Sanitization** - Automatic cleaning and validation of account numbers

### Customer Data Integration
- **Complete Customer Information** - Name, email, and contact details
- **Billing Address Support** - Full address capture including street, city, state, zip
- **Bank Address Requirements** - Mandatory streetAddress1 for bank transfer compliance

### Security & Validation
- **Input Sanitization** - Comprehensive data cleaning and validation
- **Error Handling** - Robust error management with meaningful messages
- **Transaction Validation** - Response code verification (SUCCESS + CAPTURED status)
- **PCI Compliance Ready** - Structure supports tokenization for production use

## Quick Start

1. **Choose your language** - Navigate to any implementation directory (nodejs, php, java, dotnet)
2. **Set up credentials** - Copy `.env.sample` to `.env` and add your GP API credentials
3. **Run the server** - Execute `./run.sh` to install dependencies and start the server
4. **Test the integration** - Open your browser to the specified port and process a test payment

## GP API Configuration

All implementations use the following environment variables:

```bash
# GP API Credentials
APP_ID=your_gp_api_app_id_here
APP_KEY=your_gp_api_app_key_here

# Environment (sandbox or production)
GP_API_ENVIRONMENT=sandbox
```

### GP API Settings
- **Environment**: TEST/Sandbox
- **Channel**: CardNotPresent
- **Country**: US
- **Accounts**:
  - Transaction Processing: `transaction_processing`
  - Risk Assessment: `EOS_RiskAssessment`

## API Endpoints

Each implementation provides the following endpoints:

### GET /config
Returns configuration information for the client

**Response:**
```json
{
  "success": true,
  "data": {
    "directEntry": true,
    "message": "Direct bank account entry enabled"
  }
}
```

### POST /process-payment
Processes an ACH/eCheck payment using direct bank account information

**Request Parameters:**
- `amount` (decimal, required) - Payment amount
- `account_number` (string, required) - Bank account number
- `routing_number` (string, required) - 9-digit ABA routing number
- `account_type` (string, required) - "checking" or "savings"
- `check_holder_name` (string, required) - Name on the account
- `first_name` (string, required) - Customer first name
- `last_name` (string, required) - Customer last name
- `email` (string, required) - Customer email address
- `street_address` (string, required) - Street address (line 1)
- `city` (string, required) - City
- `state` (string, required) - 2-letter state code
- `billing_zip` (string, required) - ZIP/postal code

**Response (Success):**
```json
{
  "success": true,
  "message": "ACH payment processed successfully",
  "data": {
    "transaction_id": "txn_xxx",
    "amount": 10.00,
    "currency": "USD",
    "status": "approved",
    "response_code": "SUCCESS",
    "response_message": "CAPTURED"
  }
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": "Invalid routing number"
  }
}
```

## Use Cases

This implementation can be adapted for various ACH payment scenarios:

- **Bill Payments** - Utility bills, invoices, and recurring charges
- **Direct Debit** - Subscription services and membership fees
- **One-Time Payments** - Large purchases and service payments
- **B2B Payments** - Vendor payments and supplier settlements
- **Recurring Payments** - Scheduled automatic withdrawals
- **Payment Plans** - Installment and subscription billing

## Prerequisites

- Global Payments account with GP API credentials ([Sign up here](https://developer.globalpay.com/))
- Development environment for your chosen language:
  - **Node.js**: v14.x or later with npm
  - **PHP**: v7.4 or later with Composer
  - **Java**: JDK 11 or later with Maven
  - **.NET**: .NET 6.0 or later

## Implementation Details

### Routing Number Validation
All implementations include routing number validation using the ABA standard checksum algorithm:
```
Checksum = (3 × (d₁ + d₄ + d₇) + 7 × (d₂ + d₅ + d₈) + 1 × (d₃ + d₆ + d₉)) mod 10
```
Valid routing numbers produce a checksum of 0.

### Transaction Response Validation
Successful ACH transactions must meet both criteria:
- `responseCode` === "SUCCESS"
- `responseMessage` === "CAPTURED" (TransactionStatus.CAPTURED)

### Bank Address Requirements
The GP API requires `streetAddress1` (bank_transfer.bank.address.line_1) for ACH transactions. All implementations set both:
- `bankAddress` property on the eCheck/ECheck object
- `withAddress()` for billing address

## Security Considerations

### Production Deployment
For production use, implement the following security measures:

1. **Input Validation**
   - Validate all user inputs server-side
   - Sanitize data before processing
   - Implement rate limiting on payment endpoints

2. **Data Protection**
   - Use HTTPS/TLS for all connections
   - Never log sensitive banking information
   - Implement proper session management
   - Consider tokenization for recurring payments

3. **Compliance**
   - Follow PCI DSS guidelines for payment data
   - Implement proper audit logging
   - Ensure NACHA compliance for ACH transactions
   - Review security best practices regularly

4. **Error Handling**
   - Don't expose internal error details to users
   - Implement comprehensive logging for debugging
   - Monitor failed transactions for fraud patterns

5. **Access Control**
   - Secure API credentials (never commit to version control)
   - Use environment variables for configuration
   - Implement authentication and authorization
   - Regular credential rotation

## Testing

### Test Bank Account Numbers
For sandbox testing, use these test account details:
- **Routing Number**: 021000021 (JP Morgan Chase)
- **Account Number**: Any 4-17 digit number
- **Account Type**: checking or savings

### Test Amounts
Different amounts can trigger different responses in sandbox mode. Consult the GP API documentation for specific test scenarios.

## Support & Resources

- **Documentation**: [Global Payments Developer Portal](https://developer.globalpay.com/)
- **API Reference**: [GP API Documentation](https://developer.globalpay.com/api)
- **SDKs**: [Official SDKs](https://developer.globalpay.com/sdks)
- **Support**: Contact Global Payments developer support

## Language-Specific Notes

### PHP Implementation
- PSR-12 coding standards
- Composer for dependency management
- PaymentUtils class for shared functionality
- Built-in routing number validation

### Java Implementation
- Jakarta EE servlet-based architecture
- Maven for dependency management
- Customer data object required for transactions
- Comprehensive error handling with JSON responses

### Node.js Implementation
- Express.js framework
- ES6 module syntax
- Async/await for payment processing
- ECheck class requires bankAddress property

### .NET Implementation
- ASP.NET Core minimal API
- NuGet for package management
- Environment variable configuration with dotenv
- Strongly-typed models for request/response

## License

This project is provided as-is for demonstration purposes. Review the Global Payments SDK license terms for production use.
