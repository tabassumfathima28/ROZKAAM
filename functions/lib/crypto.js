"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.generatePaymentSignature = generatePaymentSignature;
exports.generateWebhookSignature = generateWebhookSignature;
exports.timingSafeCompare = timingSafeCompare;
const crypto = __importStar(require("crypto"));
/**
 * Generates HMAC-SHA256 signature for Razorpay payment verification: orderId|paymentId
 */
function generatePaymentSignature(orderId, paymentId, secret) {
    return crypto
        .createHmac("sha256", secret)
        .update(`${orderId}|${paymentId}`)
        .digest("hex");
}
/**
 * Generates HMAC-SHA256 signature for Razorpay webhook verification
 */
function generateWebhookSignature(body, secret) {
    return crypto
        .createHmac("sha256", secret)
        .update(body)
        .digest("hex");
}
/**
 * Constant-time comparison to protect against timing attacks.
 * Returns false immediately if inputs are not equal in byte length,
 * then checks timingSafeEqual on equal-length buffers.
 */
function timingSafeCompare(a, b) {
    if (typeof a !== "string" || typeof b !== "string") {
        return false;
    }
    const bufA = Buffer.from(a, "utf8");
    const bufB = Buffer.from(b, "utf8");
    if (bufA.length !== bufB.length) {
        return false;
    }
    return crypto.timingSafeEqual(bufA, bufB);
}
//# sourceMappingURL=crypto.js.map