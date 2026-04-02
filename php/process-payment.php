<?php

declare(strict_types=1);

/**
 * ACH/eCheck Payment Processing Script - GP API
 *
 * This script demonstrates ACH/eCheck payment processing using the Global Payments GP API SDK.
 * It handles direct bank account information to process ACH payments securely.
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
require_once 'PaymentUtils.php';

use GlobalPayments\Api\Entities\Enums\AccountType;
use GlobalPayments\Api\Entities\Exceptions\ApiException;

ini_set('display_errors', '0');

PaymentUtils::configureSdk();
PaymentUtils::handleCORS();

try {
    $inputData = PaymentUtils::parseJsonInput();
    $requiredFields = ['account_number', 'routing_number', 'amount', 'account_type', 'check_holder_name'];
    foreach ($requiredFields as $field) {
        if (!isset($inputData[$field]) || empty(trim($inputData[$field]))) {
            throw new ApiException("Missing required field: $field");
        }
    }

    if (!PaymentUtils::validateRoutingNumber($inputData['routing_number'])) {
        throw new ApiException('Invalid routing number');
    }

    $inputData['account_number'] = PaymentUtils::sanitizeAccountNumber($inputData['account_number']);
    if (strlen($inputData['account_number']) < 4) {
        throw new ApiException('Invalid account number');
    }

    $amount = floatval($inputData['amount']);
    if ($amount <= 0) {
        throw new ApiException('Invalid amount');
    }

    $inputData['account_type'] = strtolower($inputData['account_type']) === 'savings'
        ? AccountType::SAVINGS
        : AccountType::CHECKING;

    $result = PaymentUtils::processACHPayment(
        $inputData,
        $amount,
        $inputData['currency'] ?? 'USD'
    );

    PaymentUtils::sendSuccessResponse($result, 'ACH payment processed successfully');

} catch (ApiException $e) {
    PaymentUtils::sendErrorResponse(400, 'ACH payment failed: ' . $e->getMessage(), 'API_ERROR');
} catch (Exception $e) {
    PaymentUtils::sendErrorResponse(500, 'Server error: ' . $e->getMessage(), 'SERVER_ERROR');
}
