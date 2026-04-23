package com.quickcart.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private final Map<Long, String> otpStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateOtp(Long orderId) {
        String otp = String.valueOf(100000 + random.nextInt(900000));
        otpStore.put(orderId, otp);
        return otp;
    }

    public boolean verifyOtp(Long orderId, String inputOtp) {
        String stored = otpStore.get(orderId);
        if (stored != null && stored.equals(inputOtp)) {
            otpStore.remove(orderId);
            return true;
        }
        return false;
    }
}