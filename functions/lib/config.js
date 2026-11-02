"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RAZORPAY_WEBHOOK_SECRET = exports.RAZORPAY_KEY_ID = exports.RAZORPAY_KEY_SECRET = void 0;
exports.getRazorpayKeySecret = getRazorpayKeySecret;
exports.getRazorpayKeyId = getRazorpayKeyId;
exports.getRazorpayWebhookSecret = getRazorpayWebhookSecret;
const params_1 = require("firebase-functions/params");
exports.RAZORPAY_KEY_SECRET = (0, params_1.defineSecret)("RAZORPAY_KEY_SECRET");
exports.RAZORPAY_KEY_ID = (0, params_1.defineSecret)("RAZORPAY_KEY_ID");
exports.RAZORPAY_WEBHOOK_SECRET = (0, params_1.defineSecret)("RAZORPAY_WEBHOOK_SECRET");
/**
 * Reads Razorpay Key Secret from Firebase Secrets with fallback to environment variables.
 */
function getRazorpayKeySecret() {
    try {
        return exports.RAZORPAY_KEY_SECRET.value() || process.env.RAZORPAY_KEY_SECRET || "";
    }
    catch {
        return process.env.RAZORPAY_KEY_SECRET || "";
    }
}
/**
 * Reads Razorpay Key ID from Firebase Secrets with fallback to environment variables.
 */
function getRazorpayKeyId() {
    try {
        return exports.RAZORPAY_KEY_ID.value() || process.env.RAZORPAY_KEY_ID || "";
    }
    catch {
        return process.env.RAZORPAY_KEY_ID || "";
    }
}
/**
 * Reads Razorpay Webhook Secret from Firebase Secrets with fallback to environment variables or Key Secret.
 */
function getRazorpayWebhookSecret() {
    try {
        return (exports.RAZORPAY_WEBHOOK_SECRET.value() ||
            process.env.RAZORPAY_WEBHOOK_SECRET ||
            getRazorpayKeySecret());
    }
    catch {
        return process.env.RAZORPAY_WEBHOOK_SECRET || getRazorpayKeySecret();
    }
}
//# sourceMappingURL=config.js.map