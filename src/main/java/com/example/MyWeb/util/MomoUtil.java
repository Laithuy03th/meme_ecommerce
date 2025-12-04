package com.example.MyWeb.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MomoUtil {

    /**
     * Generate HMAC SHA256 signature for Momo payment request
     */
    public static String generateSignature(String secretKey, String rawData) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(secretKeySpec);
            byte[] hash = hmacSHA256.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC SHA256 for Momo", e);
        }
    }

    /**
     * Verify signature from Momo callback
     */
    public static boolean verifySignature(String secretKey, String rawData, String signature) {
        String generatedSignature = generateSignature(secretKey, rawData);
        return generatedSignature.equalsIgnoreCase(signature);
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Generate request ID for Momo
     */
    public static String generateRequestId() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Generate order ID for Momo
     */
    public static String generateOrderId(Long orderId) {
        return "ORDER_" + orderId + "_" + System.currentTimeMillis();
    }
}
