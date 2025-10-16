/**
 * ACH/eCheck Payment Processing Server - GP API
 *
 * This Express application demonstrates ACH/eCheck payment processing using the Global Payments SDK
 * with GP API for direct bank account information processing.
 * This approach is suitable for server-side processing where PCI compliance requirements are met.
 */

import express from 'express';
import * as dotenv from 'dotenv';
import {
    ServicesContainer,
    GpApiConfig,
    AccessTokenInfo,
    Address,
    ECheck,
    AccountType,
    SecCode,
    ApiError,
    Environment,
    Channel,
    TransactionStatus
} from 'globalpayments-api';

dotenv.config();

const app = express();
const port = process.env.PORT || 8000;

app.use(express.static('.'));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

const config = new GpApiConfig();
config.appId = process.env.APP_ID;
config.appKey = process.env.APP_KEY;
config.environment = Environment.TEST;
config.channel = Channel.CardNotPresent;
config.country = 'US';

const accessTokenInfo = new AccessTokenInfo();
accessTokenInfo.transactionProcessingAccountName = 'transaction_processing';
accessTokenInfo.riskAssessmentAccountName = 'EOS_RiskAssessment';
config.accessTokenInfo = accessTokenInfo;

ServicesContainer.configureService(config);

/**
 * Sanitize postal code by removing invalid characters
 * @param {string} postalCode - The postal code to sanitize
 * @returns {string} The sanitized postal code
 */
const sanitizePostalCode = (postalCode) => {
    if (!postalCode) return '';
    return postalCode.replace(/[^a-zA-Z0-9-]/g, '').slice(0, 10);
};

/**
 * Validate routing number using the standard checksum algorithm
 *
 * @param {string} routingNumber - The 9-digit routing number to validate
 * @returns {boolean} True if the routing number is valid, false otherwise
 */
const validateRoutingNumber = (routingNumber) => {
    if (!routingNumber || routingNumber.length !== 9 || !/^\d{9}$/.test(routingNumber)) {
        return false;
    }

    const digits = routingNumber.split('').map(Number);
    const checksum = (
        3 * (digits[0] + digits[3] + digits[6]) +
        7 * (digits[1] + digits[4] + digits[7]) +
        1 * (digits[2] + digits[5] + digits[8])
    ) % 10;

    return checksum === 0;
};

/**
 * Sanitize account number by removing non-numeric characters
 *
 * @param {string} accountNumber - The account number to sanitize
 * @returns {string} The sanitized account number containing only digits
 */
const sanitizeAccountNumber = (accountNumber) => {
    if (!accountNumber) return '';
    return accountNumber.replace(/[^0-9]/g, '');
};

app.get('/config', (req, res) => {
    res.json({
        success: true,
        data: {
            directEntry: true,
            message: 'Direct bank account entry enabled'
        }
    });
});

app.post('/process-payment', async (req, res) => {
    try {
        const requiredFields = ['account_number', 'routing_number', 'amount',
                               'account_type', 'check_holder_name'];
        for (const field of requiredFields) {
            if (!req.body[field]) {
                throw new Error(`Missing required field: ${field}`);
            }
        }

        const amount = parseFloat(req.body.amount);
        if (isNaN(amount) || amount <= 0) {
            throw new Error('Invalid amount');
        }

        const routingNumber = req.body.routing_number.trim();
        if (!validateRoutingNumber(routingNumber)) {
            throw new Error('Invalid routing number');
        }

        const accountNumber = sanitizeAccountNumber(req.body.account_number);
        if (!accountNumber || accountNumber.length < 4) {
            throw new Error('Invalid account number');
        }

        const accountTypeStr = req.body.account_type.toLowerCase();
        const accountType = accountTypeStr === 'savings' ? AccountType.Savings : AccountType.Checking;

        const address = new Address();
        address.streetAddress1 = req.body.street_address || '';
        address.city = req.body.city || '';
        address.state = req.body.state || '';
        address.postalCode = sanitizePostalCode(req.body.billing_zip || '');
        address.country = 'US';

        const check = new ECheck();
        check.accountNumber = accountNumber;
        check.routingNumber = routingNumber;
        check.accountType = accountType;
        check.secCode = SecCode.WEB;
        check.checkName = req.body.check_holder_name;
        check.bankAddress = address;

        const response = await check.charge(amount)
            .withCurrency('USD')
            .withAddress(address)
            .execute();

        if (response.responseCode !== 'SUCCESS' || response.responseMessage !== TransactionStatus.CAPTURED) {
            return res.status(400).json({
                success: false,
                message: 'Payment processing failed',
                error: {
                    code: 'PAYMENT_DECLINED',
                    details: response.responseMessage || 'Unknown error'
                }
            });
        }

        res.json({
            success: true,
            message: `Payment successful! Transaction ID: ${response.transactionId}`,
            data: {
                transactionId: response.transactionId,
                responseCode: response.responseCode,
                responseMessage: response.responseMessage
            }
        });
    } catch (error) {
        console.error('Payment processing error:', error);

        const errorCode = error instanceof ApiError ? 'API_ERROR' : 'SERVER_ERROR';
        res.status(400).json({
            success: false,
            message: 'Payment processing failed',
            error: {
                code: errorCode,
                details: error.message
            }
        });
    }
});

app.listen(port, () => {
    console.log(`ACH/eCheck GP API server running at http://localhost:${port}`);
});
