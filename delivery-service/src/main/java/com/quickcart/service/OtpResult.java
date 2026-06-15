package com.quickcart.service;

/**
 * Result of an OTP verification attempt.
 *
 * <ul>
 *   <li>{@link #VALID}                  — OTP matched; delivery can proceed to DELIVERED.</li>
 *   <li>{@link #INVALID}                — OTP did not match; attempts counter incremented in Redis.</li>
 *   <li>{@link #EXPIRED}                — Redis TTL elapsed; key no longer exists. Customer must request a new OTP.</li>
 *   <li>{@link #MAX_ATTEMPTS_EXCEEDED}  — 3 wrong attempts; Redis key deleted. Customer must request a new OTP.</li>
 * </ul>
 */
public enum OtpResult {
    VALID,
    INVALID,
    EXPIRED,
    MAX_ATTEMPTS_EXCEEDED
}
