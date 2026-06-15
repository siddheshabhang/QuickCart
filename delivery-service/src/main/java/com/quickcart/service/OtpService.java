package com.quickcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Manages delivery OTP lifecycle using Redis as the backing store.
 *
 * <h3>Why Redis instead of PostgreSQL?</h3>
 * <ul>
 *   <li><b>Native TTL</b> — Redis automatically deletes the key after 5 minutes.
 *       No scheduler or cleanup job needed.</li>
 *   <li><b>Transient data</b> — OTPs have no historical value; they must not
 *       pollute the {@code deliveries} table with a nullable column.</li>
 *   <li><b>Multi-instance safe</b> — All instances share one Redis, so an OTP
 *       generated on pod-A can be verified by pod-B.</li>
 *   <li><b>In-memory speed</b> — verification is a single memory read, not a
 *       full DB round-trip.</li>
 * </ul>
 *
 * <h3>Key structure</h3>
 * <pre>
 *   Key   :  otp:delivery:{orderId}
 *   Value :  {otp}:{attemptCount}     e.g. "482931:0"
 *   TTL   :  300 seconds (5 minutes)
 * </pre>
 *
 * <h3>Brute-force protection</h3>
 * After {@value #MAX_ATTEMPTS} consecutive wrong guesses the key is deleted,
 * forcing the agent to request a new OTP. This limits the attack surface
 * to {@value #MAX_ATTEMPTS} guesses per 5-minute window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    private static final SecureRandom SECURE_RANDOM  = new SecureRandom();
    private static final long         OTP_TTL_SECONDS = 300;   // 5 minutes
    private static final int          MAX_ATTEMPTS    = 3;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a cryptographically random 6-digit OTP, stores it in Redis
     * with a 5-minute TTL, and returns the plaintext value for emailing.
     *
     * <p>Calling this again for the same {@code orderId} will overwrite any
     * existing OTP and reset the TTL — useful for a "resend OTP" flow.
     *
     * @param orderId used as the Redis key identifier
     * @return the 6-digit OTP string, e.g. {@code "482931"}
     */
    public String generateOtp(Long orderId) {
        String otp   = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        String value = otp + ":0";   // format: "{otp}:{attemptCount}"

        redisTemplate.opsForValue().set(
                redisKey(orderId),
                value,
                Duration.ofSeconds(OTP_TTL_SECONDS)
        );

        log.info("OTP generated and stored in Redis for orderId: {} (TTL: {}s)",
                orderId, OTP_TTL_SECONDS);

        return otp;
    }

    /**
     * Verifies the submitted OTP against the Redis-stored value.
     *
     * <ul>
     *   <li>If the key is absent (TTL elapsed) → {@link OtpResult#EXPIRED}</li>
     *   <li>If the OTP matches → deletes the key (single-use) → {@link OtpResult#VALID}</li>
     *   <li>If wrong, attempts &lt; max → increments counter, preserves TTL → {@link OtpResult#INVALID}</li>
     *   <li>If wrong, attempts ≥ max → deletes key (lockout) → {@link OtpResult#MAX_ATTEMPTS_EXCEEDED}</li>
     * </ul>
     *
     * @param orderId  the order whose OTP is being verified
     * @param inputOtp the OTP entered by the delivery agent
     * @return {@link OtpResult} describing the outcome
     */
    public OtpResult verifyOtp(Long orderId, String inputOtp) {
        String key   = redisKey(orderId);
        String value = redisTemplate.opsForValue().get(key);

        // Key gone — TTL elapsed
        if (value == null) {
            log.warn("OTP verification failed — key expired or not found for orderId: {}", orderId);
            return OtpResult.EXPIRED;
        }

        String[] parts    = value.split(":");
        String   stored   = parts[0];
        int      attempts = Integer.parseInt(parts[1]);

        if (stored.equals(inputOtp)) {
            redisTemplate.delete(key);   // single-use — delete immediately after success
            log.info("OTP verified successfully for orderId: {}", orderId);
            return OtpResult.VALID;
        }

        // Wrong OTP — check attempt limit
        int newAttempts = attempts + 1;
        if (newAttempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(key);   // lockout — force regeneration
            log.warn("OTP max attempts ({}) exceeded for orderId: {} — key deleted", MAX_ATTEMPTS, orderId);
            return OtpResult.MAX_ATTEMPTS_EXCEEDED;
        }

        // Increment attempts counter, preserve remaining TTL
        Long remainingTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long ttl = (remainingTtl != null && remainingTtl > 0) ? remainingTtl : OTP_TTL_SECONDS;

        redisTemplate.opsForValue().set(
                key,
                stored + ":" + newAttempts,
                Duration.ofSeconds(ttl)
        );

        log.warn("Invalid OTP for orderId: {} — attempt {}/{}", orderId, newAttempts, MAX_ATTEMPTS);
        return OtpResult.INVALID;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String redisKey(Long orderId) {
        return "otp:delivery:" + orderId;
    }
}