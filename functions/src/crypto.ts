import * as crypto from "crypto";

/**
 * Generates HMAC-SHA256 signature for Razorpay payment verification: orderId|paymentId
 */
export function generatePaymentSignature(
  orderId: string,
  paymentId: string,
  secret: string
): string {
  return crypto
    .createHmac("sha256", secret)
    .update(`${orderId}|${paymentId}`)
    .digest("hex");
}

/**
 * Generates HMAC-SHA256 signature for Razorpay webhook verification
 */
export function generateWebhookSignature(
  body: string | Buffer,
  secret: string
): string {
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
export function timingSafeCompare(a: string, b: string): boolean {
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

