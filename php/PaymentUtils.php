<?php

declare(strict_types=1);

/**
 * Payment Utilities Class - GP API (ACH/eCheck)
 *
 * Provides utility functions for ACH/eCheck payment processing using Global Payments GP API.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   GlobalPayments_Sample
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;
use GlobalPayments\Api\Entities\Address;
use GlobalPayments\Api\Entities\Customer;
use GlobalPayments\Api\Entities\Enums\AccountType;
use GlobalPayments\Api\Entities\Enums\Channel;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\SecCode;
use GlobalPayments\Api\Entities\Enums\TransactionStatus;
use GlobalPayments\Api\Entities\GpApi\AccessTokenInfo;
use GlobalPayments\Api\PaymentMethods\ECheck;
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;

class PaymentUtils
{
    public static function configureSdk(): void
    {
        $dotenv = Dotenv::createImmutable(__DIR__);
        $dotenv->load();

        $config = new GpApiConfig();
        $config->appId = $_ENV['GP_API_APP_ID'] ?? '';
        $config->appKey = $_ENV['GP_API_APP_KEY'] ?? '';
        $config->environment = Environment::TEST;
        $config->channel = Channel::CardNotPresent;
        $config->country = 'US';

        $accessTokenInfo = new AccessTokenInfo();
        $accessTokenInfo->transactionProcessingAccountName = 'transaction_processing';
        $accessTokenInfo->riskAssessmentAccountName = 'EOS_RiskAssessment';
        $config->accessTokenInfo = $accessTokenInfo;

        ServicesContainer::configureService($config);
    }

    public static function sanitizePostalCode(?string $postalCode): string
    {
        if ($postalCode === null) {
            return '';
        }

        $sanitized = preg_replace('/[^a-zA-Z0-9-]/', '', $postalCode);
        return substr($sanitized, 0, 10);
    }

    public static function validateRoutingNumber(?string $routingNumber): bool
    {
        if (empty($routingNumber) || strlen($routingNumber) !== 9 || !ctype_digit($routingNumber)) {
            return false;
        }

        $digits = str_split($routingNumber);
        $checksum = (
            3 * ($digits[0] + $digits[3] + $digits[6]) +
            7 * ($digits[1] + $digits[4] + $digits[7]) +
            1 * ($digits[2] + $digits[5] + $digits[8])
        ) % 10;

        return $checksum === 0;
    }

    public static function sanitizeAccountNumber(?string $accountNumber): string
    {
        if (empty($accountNumber)) {
            return '';
        }
        return preg_replace('/[^0-9]/', '', $accountNumber);
    }

    public static function processACHPayment(array $achData, float $amount, string $currency): array
    {
        try {
            // Create eCheck payment method
            $check = new ECheck();
            $check->accountNumber = $achData['account_number'];
            $check->routingNumber = $achData['routing_number'];
            $check->accountType = $achData['account_type']; // AccountType::SAVINGS or CHECKING
            $check->secCode = SecCode::WEB;
            $check->checkHolderName = $achData['check_holder_name'];

            // Create address from provided data
            $address = new Address();
            $address->streetAddress1 = $achData['street_address'] ?? '';
            $address->city = $achData['city'] ?? '';
            $address->state = $achData['state'] ?? '';
            $address->postalCode = self::sanitizePostalCode($achData['billing_zip'] ?? '');
            $address->countryCode = 'US';

            // Create customer data
            $customer = new Customer();
            $customer->firstName = $achData['first_name'] ?? '';
            $customer->lastName = $achData['last_name'] ?? '';
            $customer->email = $achData['email'] ?? '';

            // Process the ACH charge
            $response = $check->charge($amount)
                ->withCurrency($currency)
                ->withAddress($address)
                ->withCustomerData($customer)
                ->execute();

            // Validate GP API response
            if ($response->responseCode === 'SUCCESS' &&
                $response->responseMessage === TransactionStatus::CAPTURED) {

                return [
                    'transaction_id' => $response->transactionId ?? 'txn_' . uniqid(),
                    'amount' => $amount,
                    'currency' => $currency,
                    'status' => 'approved',
                    'response_code' => $response->responseCode,
                    'response_message' => $response->responseMessage ?? 'Approved',
                    'timestamp' => date('c'),
                    'gateway_response' => [
                        'auth_code' => $response->authorizationCode ?? '',
                        'reference_number' => $response->referenceNumber ?? ''
                    ]
                ];
            } else {
                throw new \Exception('ACH payment failed: ' . ($response->responseMessage ?? 'Unknown error'));
            }
        } catch (\Exception $e) {
            error_log('ACH payment processing error: ' . $e->getMessage());
            throw $e;
        }
    }

    public static function sendSuccessResponse($data, string $message = 'Operation completed successfully'): void
    {
        http_response_code(200);

        $response = [
            'success' => true,
            'data' => $data,
            'message' => $message,
            'timestamp' => date('c')
        ];

        echo json_encode($response);
        exit();
    }

    public static function sendErrorResponse(int $statusCode, string $message, string $errorCode = null): void
    {
        http_response_code($statusCode);

        $response = [
            'success' => false,
            'message' => $message,
            'timestamp' => date('c')
        ];

        if ($errorCode) {
            $response['error_code'] = $errorCode;
        }

        echo json_encode($response);
        exit();
    }

    public static function handleCORS(): void
    {
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization');
        header('Content-Type: application/json');

        if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
            http_response_code(200);
            exit();
        }
    }

    public static function parseJsonInput(): array
    {
        $inputData = [];
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            $rawInput = file_get_contents('php://input');
            if ($rawInput) {
                $inputData = json_decode($rawInput, true) ?? [];
            }
            $inputData = array_merge($_POST, $inputData);
        }
        return $inputData;
    }
}
