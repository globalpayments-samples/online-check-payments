package com.globalpayments.example;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Customer;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.AccountType;
import com.global.api.entities.enums.Channel;
import com.global.api.entities.enums.Environment;
import com.global.api.entities.enums.SecCode;
import com.global.api.entities.enums.TransactionStatus;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.entities.gpApi.entities.AccessTokenInfo;
import com.global.api.paymentMethods.eCheck;
import com.global.api.serviceConfigs.GpApiConfig;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * ACH/eCheck Payment Processing Servlet - GP API
 *
 * This servlet demonstrates ACH/eCheck payment processing using the Global Payments SDK
 * with GP API for direct bank account information processing.
 * This approach is suitable for server-side processing where PCI compliance requirements are met.
 *
 * Endpoints:
 * - GET /config: Returns minimal configuration for the client
 * - POST /process-payment: Processes ACH/eCheck payments using direct bank account data with GP API
 *
 * @author Global Payments
 * @version 1.0
 */

@WebServlet(urlPatterns = {"/process-payment", "/config"})
public class ProcessPaymentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Dotenv dotenv = Dotenv.load();

    @Override
    public void init() throws ServletException {
        super.init();
        configureSDK();
    }

    private void configureSDK() {
        try {
            GpApiConfig config = new GpApiConfig();
            config.setAppId(dotenv.get("APP_ID"));
            config.setAppKey(dotenv.get("APP_KEY"));
            config.setEnvironment(Environment.TEST);
            config.setChannel(Channel.CardNotPresent);
            config.setCountry("US");

            AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
            accessTokenInfo.setTransactionProcessingAccountName("transaction_processing");
            accessTokenInfo.setRiskAssessmentAccountName("EOS_RiskAssessment");
            config.setAccessTokenInfo(accessTokenInfo);

            ServicesContainer.configureService(config);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Failed to configure SDK", e);
        }
    }

    /**
     * Sanitize postal code by removing invalid characters
     * @param postalCode The postal code to sanitize
     * @return The sanitized postal code
     */
    private String sanitizePostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return "";
        }
        String sanitized = postalCode.replaceAll("[^a-zA-Z0-9-]", "");
        return sanitized.length() > 10 ? sanitized.substring(0, 10) : sanitized;
    }

    /**
     * Validate routing number using the standard checksum algorithm
     * @param routingNumber The 9-digit routing number to validate
     * @return True if the routing number is valid, false otherwise
     */
    private boolean validateRoutingNumber(String routingNumber) {
        if (routingNumber == null || routingNumber.length() != 9 || !routingNumber.matches("\\d{9}")) {
            return false;
        }

        int[] digits = routingNumber.chars().map(Character::getNumericValue).toArray();
        int checksum = (
            3 * (digits[0] + digits[3] + digits[6]) +
            7 * (digits[1] + digits[4] + digits[7]) +
            1 * (digits[2] + digits[5] + digits[8])
        ) % 10;

        return checksum == 0;
    }

    /**
     * Sanitize account number by removing non-numeric characters
     * @param accountNumber The account number to sanitize
     * @return The sanitized account number containing only digits
     */
    private String sanitizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "";
        }
        return accountNumber.replaceAll("[^0-9]", "");
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getServletPath();

        if ("/config".equals(pathInfo)) {
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{");
            out.println("  \"success\": true,");
            out.println("  \"data\": {");
            out.println("    \"directEntry\": true,");
            out.println("    \"message\": \"Direct bank account entry enabled\"");
            out.println("  }");
            out.println("}");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            String body = request.getReader().lines().collect(Collectors.joining());
            JSONObject json = new JSONObject(body);

            String accountNumber = json.optString("account_number", "");
            String routingNumber = json.optString("routing_number", "");
            String accountTypeStr = json.optString("account_type", "");
            String checkHolderName = json.optString("check_holder_name", "");
            String billingZip = json.optString("billing_zip", "");
            String amountStr = json.optString("amount", "0");

            String firstName = json.optString("first_name", "");
            String lastName = json.optString("last_name", "");
            String email = json.optString("email", "");

            String streetAddress = json.optString("street_address", "");
            String city = json.optString("city", "");
            String state = json.optString("state", "");
            if (accountNumber.isEmpty() || routingNumber.isEmpty() ||
                accountTypeStr.isEmpty() || checkHolderName.isEmpty() || amountStr.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = "{\"success\":false,\"message\":\"Missing required fields\",\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":\"Missing required fields\"}}";
                response.getWriter().write(errorResponse);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    String errorResponse = "{\"success\":false,\"message\":\"Amount must be positive\",\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":\"Amount must be positive\"}}";
                    response.getWriter().write(errorResponse);
                    return;
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = "{\"success\":false,\"message\":\"Invalid amount\",\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":\"Invalid amount format\"}}";
                response.getWriter().write(errorResponse);
                return;
            }

            routingNumber = routingNumber.trim();
            if (!validateRoutingNumber(routingNumber)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = "{\"success\":false,\"message\":\"Invalid routing number\",\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":\"Invalid routing number\"}}";
                response.getWriter().write(errorResponse);
                return;
            }

            accountNumber = sanitizeAccountNumber(accountNumber);
            if (accountNumber.isEmpty() || accountNumber.length() < 4) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = "{\"success\":false,\"message\":\"Invalid account number\",\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":\"Invalid account number\"}}";
                response.getWriter().write(errorResponse);
                return;
            }

            accountTypeStr = accountTypeStr.toLowerCase();
            AccountType accountType = "savings".equals(accountTypeStr) ? AccountType.Savings : AccountType.Checking;

            Address address = new Address();
            address.setStreetAddress1(streetAddress);
            address.setCity(city);
            address.setState(state);
            address.setPostalCode(sanitizePostalCode(billingZip));
            address.setCountry("US");

            eCheck check = new eCheck();
            check.setAccountNumber(accountNumber);
            check.setRoutingNumber(routingNumber);
            check.setAccountType(accountType);
            check.setSecCode(SecCode.Web);
            check.setCheckName(checkHolderName);
            check.setBankAddress(address);

            Customer customer = new Customer();
            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            customer.setEmail(email);
            customer.setHomePhone(billingZip);
            customer.setMobilePhone(billingZip);

            Transaction chargeResponse = check.charge(amount)
                .withCurrency("USD")
                .withAddress(address)
                .withCustomerData(customer)
                .execute();

            if (!"SUCCESS".equals(chargeResponse.getResponseCode()) ||
                !TransactionStatus.Captured.getValue().equals(chargeResponse.getResponseMessage())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject errorJson = new JSONObject();
                errorJson.put("success", false);
                errorJson.put("message", "Payment processing failed");
                JSONObject errorDetails = new JSONObject();
                errorDetails.put("code", "PAYMENT_DECLINED");
                errorDetails.put("details", chargeResponse.getResponseMessage());
                errorJson.put("error", errorDetails);
                response.getWriter().write(errorJson.toString());
                return;
            }

            JSONObject successJson = new JSONObject();
            successJson.put("success", true);
            successJson.put("message", "Payment successful! Transaction ID: " + chargeResponse.getTransactionId());
            JSONObject dataJson = new JSONObject();
            dataJson.put("transactionId", chargeResponse.getTransactionId());
            dataJson.put("responseCode", chargeResponse.getResponseCode());
            dataJson.put("responseMessage", chargeResponse.getResponseMessage());
            successJson.put("data", dataJson);
            response.getWriter().write(successJson.toString());

        } catch (ApiException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "Payment processing failed");
            JSONObject errorDetails = new JSONObject();
            errorDetails.put("code", "API_ERROR");
            errorDetails.put("details", e.getMessage());
            errorJson.put("error", errorDetails);
            response.getWriter().write(errorJson.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject errorJson = new JSONObject();
            errorJson.put("success", false);
            errorJson.put("message", "Internal server error");
            JSONObject errorDetails = new JSONObject();
            errorDetails.put("code", "INTERNAL_ERROR");
            errorDetails.put("details", e.getMessage());
            errorJson.put("error", errorDetails);
            response.getWriter().write(errorJson.toString());
        }
    }
}
