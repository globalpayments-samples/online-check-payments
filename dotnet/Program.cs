using GlobalPayments.Api;
using GlobalPayments.Api.Entities;
using GlobalPayments.Api.Entities.Enums;
using GlobalPayments.Api.Entities.GpApi;
using GlobalPayments.Api.PaymentMethods;
using GlobalPayments.Api.Services;
using dotenv.net;
using System.Text.Json;
using System.Text.RegularExpressions;
using Environment = GlobalPayments.Api.Entities.Environment;

namespace ECheckPaymentSample;

/// <summary>
/// ACH/eCheck Payment Processing Application - GP API
///
/// This application demonstrates ACH/eCheck payment processing using the Global Payments SDK
/// with GP API for direct bank account information processing.
/// This approach is suitable for server-side processing where PCI compliance requirements are met.
/// </summary>
public class Program
{
    public static void Main(string[] args)
    {
        DotEnv.Load();

        var builder = WebApplication.CreateBuilder(args);

        var app = builder.Build();

        app.UseDefaultFiles();
        app.UseStaticFiles();

        ConfigureGlobalPaymentsSDK();

        ConfigureEndpoints(app);

        var port = System.Environment.GetEnvironmentVariable("PORT") ?? "8000";
        app.Urls.Add($"http://0.0.0.0:{port}");

        app.Run();
    }

    private static void ConfigureGlobalPaymentsSDK()
    {
        var config = new GpApiConfig
        {
            AppId = System.Environment.GetEnvironmentVariable("APP_ID"),
            AppKey = System.Environment.GetEnvironmentVariable("APP_KEY"),
            Environment = Environment.TEST,
            Channel = Channel.CardNotPresent,
            Country = "US",
            AccessTokenInfo = new AccessTokenInfo
            {
                TransactionProcessingAccountName = "transaction_processing",
                RiskAssessmentAccountName = "EOS_RiskAssessment"
            }
        };

        ServicesContainer.ConfigureService(config);
    }

    private static string SanitizePostalCode(string? postalCode)
    {
        if (string.IsNullOrWhiteSpace(postalCode))
            return string.Empty;

        var sanitized = Regex.Replace(postalCode, @"[^a-zA-Z0-9-]", "");
        return sanitized.Length > 10 ? sanitized.Substring(0, 10) : sanitized;
    }

    private static bool ValidateRoutingNumber(string? routingNumber)
    {
        if (string.IsNullOrWhiteSpace(routingNumber) ||
            routingNumber.Length != 9 ||
            !Regex.IsMatch(routingNumber, @"^\d{9}$"))
        {
            return false;
        }

        var digits = routingNumber.Select(c => int.Parse(c.ToString())).ToArray();
        var checksum = (
            3 * (digits[0] + digits[3] + digits[6]) +
            7 * (digits[1] + digits[4] + digits[7]) +
            1 * (digits[2] + digits[5] + digits[8])
        ) % 10;

        return checksum == 0;
    }

    private static string SanitizeAccountNumber(string? accountNumber)
    {
        if (string.IsNullOrWhiteSpace(accountNumber))
            return string.Empty;

        return Regex.Replace(accountNumber, @"[^0-9]", "");
    }

    private static void ConfigureEndpoints(WebApplication app)
    {
        app.MapGet("/config", () =>
        {
            var response = new
            {
                success = true,
                data = new
                {
                    directEntry = true,
                    message = "Direct bank account entry enabled"
                }
            };

            return Results.Json(response);
        });

        app.MapPost("/process-payment", async (HttpContext context) =>
        {
            string accountNumber = "";
            string routingNumber = "";
            string accountTypeStr = "";
            string checkHolderName = "";
            string billingZip = "";
            decimal amount = 0;
            string firstName = "";
            string lastName = "";
            string email = "";
            string streetAddress = "";
            string city = "";
            string state = "";

            try
            {
                using var reader = new StreamReader(context.Request.Body);
                var body = await reader.ReadToEndAsync();
                var requestData = JsonSerializer.Deserialize<JsonElement>(body);

                accountNumber = requestData.TryGetProperty("account_number", out var acctNum) ? acctNum.GetString() ?? "" : "";
                routingNumber = requestData.TryGetProperty("routing_number", out var routNum) ? routNum.GetString() ?? "" : "";
                accountTypeStr = requestData.TryGetProperty("account_type", out var acctType) ? acctType.GetString() ?? "" : "";
                checkHolderName = requestData.TryGetProperty("check_holder_name", out var holderName) ? holderName.GetString() ?? "" : "";
                billingZip = requestData.TryGetProperty("billing_zip", out var zip) ? zip.GetString() ?? "" : "";

                if (requestData.TryGetProperty("amount", out var amt))
                {
                    if (amt.ValueKind == JsonValueKind.Number)
                    {
                        amount = amt.GetDecimal();
                    }
                    else if (amt.ValueKind == JsonValueKind.String)
                    {
                        decimal.TryParse(amt.GetString(), out amount);
                    }
                }

                firstName = requestData.TryGetProperty("first_name", out var fName) ? fName.GetString() ?? "" : "";
                lastName = requestData.TryGetProperty("last_name", out var lName) ? lName.GetString() ?? "" : "";
                email = requestData.TryGetProperty("email", out var emailProp) ? emailProp.GetString() ?? "" : "";

                streetAddress = requestData.TryGetProperty("street_address", out var street) ? street.GetString() ?? "" : "";
                city = requestData.TryGetProperty("city", out var cityProp) ? cityProp.GetString() ?? "" : "";
                state = requestData.TryGetProperty("state", out var stateProp) ? stateProp.GetString() ?? "" : "";
            }
            catch (JsonException)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Invalid JSON format",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Request body must be valid JSON"
                    }
                });
            }

            if (string.IsNullOrEmpty(accountNumber) || string.IsNullOrEmpty(routingNumber) ||
                string.IsNullOrEmpty(accountTypeStr) || string.IsNullOrEmpty(checkHolderName) || amount <= 0)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Missing required fields"
                    }
                });
            }

            if (amount <= 0)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Amount must be a positive number"
                    }
                });
            }

            var sanitizedRoutingNumber = routingNumber.Trim();
            if (!ValidateRoutingNumber(sanitizedRoutingNumber))
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Invalid routing number"
                    }
                });
            }

            var sanitizedAccountNumber = SanitizeAccountNumber(accountNumber);
            if (string.IsNullOrEmpty(sanitizedAccountNumber) || sanitizedAccountNumber.Length < 4)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Invalid account number"
                    }
                });
            }

            var accountType = string.Equals(accountTypeStr, "savings", StringComparison.OrdinalIgnoreCase)
                ? AccountType.SAVINGS : AccountType.CHECKING;

            var address = new Address
            {
                StreetAddress1 = streetAddress,
                City = city,
                State = state,
                PostalCode = SanitizePostalCode(billingZip),
                CountryCode = "US"
            };

            var check = new eCheck
            {
                AccountNumber = sanitizedAccountNumber,
                RoutingNumber = sanitizedRoutingNumber,
                AccountType = accountType,
                SecCode = "WEB",
                CheckName = checkHolderName,
                BankAddress = address
            };

            var customer = new Customer
            {
                FirstName = firstName,
                LastName = lastName,
                Email = email
            };

            try
            {
                var response = check.Charge(amount)
                    .WithCurrency("USD")
                    .WithAddress(address)
                    .WithCustomerData(customer)
                    .Execute();

                if (response.ResponseCode != "SUCCESS" ||
                    !string.Equals(response.ResponseMessage, "CAPTURED", StringComparison.OrdinalIgnoreCase))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Payment processing failed",
                        error = new {
                            code = "PAYMENT_DECLINED",
                            details = response.ResponseMessage
                        }
                    });
                }

                return Results.Ok(new
                {
                    success = true,
                    message = $"Payment successful! Transaction ID: {response.TransactionId}",
                    data = new {
                        transactionId = response.TransactionId,
                        responseCode = response.ResponseCode,
                        responseMessage = response.ResponseMessage
                    }
                });
            }
            catch (Exception ex)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "API_ERROR",
                        details = ex.Message
                    }
                });
            }
        });
    }
}
