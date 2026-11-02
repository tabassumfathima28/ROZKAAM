import { defineSecret } from "firebase-functions/params";

export const RAZORPAY_KEY_SECRET = defineSecret("RAZORPAY_KEY_SECRET");
export const RAZORPAY_KEY_ID = defineSecret("RAZORPAY_KEY_ID");
export const RAZORPAY_WEBHOOK_SECRET = defineSecret("RAZORPAY_WEBHOOK_SECRET");

/**
 * Reads Razorpay Key Secret from Firebase Secrets with fallback to environment variables.
 */
export function getRazorpayKeySecret(): string {
  try {
    return RAZORPAY_KEY_SECRET.value() || process.env.RAZORPAY_KEY_SECRET || "";
  } catch {
    return process.env.RAZORPAY_KEY_SECRET || "";
  }
}

/**
 * Reads Razorpay Key ID from Firebase Secrets with fallback to environment variables.
 */
export function getRazorpayKeyId(): string {
  try {
    return RAZORPAY_KEY_ID.value() || process.env.RAZORPAY_KEY_ID || "";
  } catch {
    return process.env.RAZORPAY_KEY_ID || "";
  }
}

/**
 * Reads Razorpay Webhook Secret from Firebase Secrets with fallback to environment variables or Key Secret.
 */
export function getRazorpayWebhookSecret(): string {
  try {
    return (
      RAZORPAY_WEBHOOK_SECRET.value() ||
      process.env.RAZORPAY_WEBHOOK_SECRET ||
      getRazorpayKeySecret()
    );
  } catch {
    return process.env.RAZORPAY_WEBHOOK_SECRET || getRazorpayKeySecret();
  }
}

