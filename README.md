# ACH/eCheck Online Payment Processing

A comprehensive multi-language demonstration of ACH/eCheck payment processing using the Global Payments GP API. This example showcases secure electronic check payments with direct bank account entry, routing number validation, and full customer data integration across multiple programming languages.

## 🚀 Features

### Core Payment Capabilities
- **Direct Bank Account Entry** - Process payments using account and routing numbers directly
- **Account Type Support** - Both checking and savings account processing
- **Routing Number Validation** - Industry-standard ABA checksum algorithm validation
- **Account Number Sanitization** - Automatic cleaning and validation of account numbers
- **Customer Data Integration** - Associate full billing and contact information with each payment

### Development & Testing
- **Test Account Support** - Built-in JP Morgan Chase sandbox routing number (021000021)
- **Comprehensive Web Interface** - Complete UI for ACH payment submission and result display
- **Global Payments GP API Integration** - Server-side payment processing via GP API

### Technical Features
- **Consistent API Structure** - Identical endpoints and functionality across all language implementations
- **Environment Configuration** - Secure credential management with .env files
- **Bank Address Compliance** - Mandatory streetAddress1 handling per GP API requirements
- **Transaction Validation** - Dual-criteria response verification (SUCCESS + CAPTURED)

## 🌐 Available Implementations

Each implementation provides identical functionality with language-specific best practices:

| Language | Framework | Requirements | Status |
|----------|-----------|--------------|--------|
| **[PHP](./php/)** - ([Preview](https://githubbox.com/globalpayments-samples/online-check-payments/tree/main/php)) | Native PHP | PHP 7.4+, Composer | ✅ Complete |
| **[Node.js](./nodejs/)** - ([Preview](https://githubbox.com/globalpayments-samples/online-check-payments/tree/main/nodejs)) | Express.js | Node.js 18+, npm | ✅ Complete |
| **[.NET](./dotnet/)** - ([Preview](https://githubbox.com/globalpayments-samples/online-check-payments/tree/main/dotnet)) | ASP.NET Core | .NET 9.0+ | ✅ Complete |
| **[Java](./java/)** - ([Preview](https://githubbox.com/globalpayments-samples/online-check-payments/tree/main/java)) | Jakarta EE | Java 11+, Maven | ✅ Complete |

## 🏗️ Architecture Overview

### Frontend Architecture
- **Direct Entry Form** - Bank account and routing number input with real-time validation
- **Customer Information Capture** - Full name, email, and billing address fields
- **Responsive Web Interface** - Clean payment form with status feedback
- **Client-Side Validation** - Routing number checksum verification before submission

### Backend Architecture
- **RESTful API Design** - Consistent endpoints across all implementations
- **GP API eCheck Processing** - Secure ACH transactions via Global Payments SDK
- **Input Sanitization** - Account number and routing number cleaning
- **Bank Address Handling** - Dual address assignment required by GP API ACH spec

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/config` | Returns frontend configuration for client initialization |
| `POST` | `/process-payment` | Processes ACH/eCheck payment via GP API |

## 🚀 Quick Start

### Prerequisites
- Global Payments account with GP API credentials ([Sign up here](https://developer.globalpayments.com/))
- Development environment for your chosen language
- Package manager (npm, composer, maven, or dotnet)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/globalpayments-samples/online-check-payments.git
   cd online-check-payments
   ```

2. **Choose your implementation**
   ```bash
   cd php  # or nodejs, dotnet, java
   ```

3. **Configure environment**
   ```bash
   cp .env.sample .env
   # Edit .env with your GP API credentials:
   # APP_ID=your_gp_api_app_id_here
   # APP_KEY=your_gp_api_app_key_here
   # GP_API_ENVIRONMENT=sandbox
   ```

4. **Install dependencies and run**
   ```bash
   ./run.sh
   ```

   Or manually per language:
   ```bash
   # PHP
   composer install && php -S localhost:8000

   # Node.js
   npm install && npm start

   # .NET
   dotnet restore && dotnet run

   # Java
   mvn clean compile cargo:run
   ```

5. **Access the application**
   Open [http://localhost:8000](http://localhost:8000) in your browser

## 🧪 Development & Testing

### Test Bank Account Details
For sandbox testing, use the following credentials:

| Field | Value |
|-------|-------|
| **Routing Number** | 021000021 (JP Morgan Chase) |
| **Account Number** | Any 4–17 digit number |
| **Account Type** | `checking` or `savings` |

### Test Amounts
Different amounts trigger different sandbox responses. Consult the [GP API documentation](https://developer.globalpayments.com/api/references-overview) for specific test scenarios.

### Routing Number Validation
All implementations validate routing numbers using the ABA standard checksum algorithm:
```
Checksum = (3 × (d₁+d₄+d₇) + 7 × (d₂+d₅+d₈) + 1 × (d₃+d₆+d₉)) mod 10
```
Valid routing numbers produce a checksum of `0`.

## 💳 Payment Flow

### 1. Customer Input
- User enters bank account number, routing number, and account type
- User provides name, email, and full billing address

### 2. Client-Side Validation
- Routing number checksum validated before submission
- Account number sanitized (digits only, 4–17 characters)

### 3. Backend Processing
- Server creates an `ECheck` object with account details and bank address
- Calls `eCheck.charge(amount)` via the GP API SDK
- Validates response: `responseCode === "SUCCESS"` and `responseMessage === "CAPTURED"`

### 4. Response
- Success: returns transaction ID, amount, currency, and status
- Failure: returns structured error with code and details

## 🔧 API Reference

### GET /config

Returns frontend configuration.

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

Processes an ACH/eCheck payment.

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `amount` | decimal | ✅ | Payment amount |
| `account_number` | string | ✅ | Bank account number (4–17 digits) |
| `routing_number` | string | ✅ | 9-digit ABA routing number |
| `account_type` | string | ✅ | `checking` or `savings` |
| `check_holder_name` | string | ✅ | Name on the account |
| `first_name` | string | ✅ | Customer first name |
| `last_name` | string | ✅ | Customer last name |
| `email` | string | ✅ | Customer email address |
| `street_address` | string | ✅ | Street address (required by GP API ACH) |
| `city` | string | ✅ | City |
| `state` | string | ✅ | 2-letter state code |
| `billing_zip` | string | ✅ | ZIP/postal code |

**Response (Success):**
```json
{
  "success": true,
  "message": "ACH payment processed successfully",
  "data": {
    "transaction_id": "TRN_xxx",
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

## 🔧 Customization

### Extending Functionality
Each implementation provides a solid foundation for:
- **Recurring ACH Payments** - Add scheduling and stored payment method support
- **Batch Processing** - Queue multiple ACH transactions for bulk settlement
- **Refunds & Voids** - Implement reversal endpoints using transaction IDs
- **Webhook Handling** - Receive async ACH settlement notifications
- **Enhanced Validation** - Add NACHA-compliant account verification flows

### Production Considerations
Before deploying to production:
- **Security** - Implement input validation, rate limiting, and HTTPS
- **Logging** - Add secure logging with PII protection (never log account numbers)
- **Compliance** - Ensure NACHA and PCI DSS compliance
- **Error Handling** - Surface meaningful errors without exposing internals
- **Authentication** - Add user authentication and access control

## 📚 Documentation

Each language implementation includes detailed documentation:
- **Setup Instructions** - Environment configuration and dependency installation
- **API Documentation** - Endpoint specifications with request/response examples
- **Code Structure** - File organization and architecture notes
- **Troubleshooting** - Common issues and solutions

## 🤝 Contributing

This project serves as a reference implementation for GP API ACH/eCheck integration. When contributing:
- Maintain consistency across all language implementations
- Follow each language's best practices and conventions
- Ensure thorough testing in the sandbox environment
- Update documentation to reflect any changes

## 📄 License

MIT License — see [LICENSE](./LICENSE) for details.

## 🆘 Support

- **Global Payments Developer Portal**: [https://developer.globalpayments.com/](https://developer.globalpayments.com/)
- **GP API Reference**: [https://developer.globalpayments.com/api](https://developer.globalpayments.com/api/references-overview)
- **SDK Documentation**: Language-specific SDK guides in each implementation directory

---

**Note**: This is a demonstration application for development and testing purposes. For production use, implement additional security measures, error handling, and compliance requirements specific to your use case.
