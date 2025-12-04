package com.example.MyWeb.service;

import com.example.MyWeb.config.MomoConfig;
import com.example.MyWeb.exception.PaymentException;
import com.example.MyWeb.util.MomoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentService {

    private final MomoConfig momoConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Call Momo API để tạo payment request
     */
    public String createMomoPayment(Long orderId, Double amount, String orderInfo, String returnUrl) {
        try {
            String partnerCode = momoConfig.getPartnerCode();
            String accessKey = momoConfig.getAccessKey();
            String secretKey = momoConfig.getSecretKey();
            String endpoint = momoConfig.getEndpoint();

            String requestId = MomoUtil.generateRequestId();
            String momoOrderId = MomoUtil.generateOrderId(orderId);
            String redirectUrl = returnUrl != null ? returnUrl : momoConfig.getReturnUrl();
            String ipnUrl = momoConfig.getNotifyUrl();
            String requestType = "captureWallet";
            String amountStr = String.valueOf(amount.longValue());
            String extraData = "";

            // Build raw signature
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amountStr +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + ipnUrl +
                    "&orderId=" + momoOrderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + redirectUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = MomoUtil.generateSignature(secretKey, rawSignature);

            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("partnerName", "MyWeb Shop");
            requestBody.put("storeId", "MyWebStore");
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amountStr);
            requestBody.put("orderId", momoOrderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", redirectUrl);
            requestBody.put("ipnUrl", ipnUrl);
            requestBody.put("lang", "vi");
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", requestType);
            requestBody.put("signature", signature);

            // Send HTTP POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending Momo payment request for order: {}, requestId: {}", orderId, requestId);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                // Check response code
                int resultCode = jsonResponse.get("resultCode").asInt();
                if (resultCode == 0) {
                    // Success - return payUrl
                    String payUrl = jsonResponse.get("payUrl").asText();
                    log.info("Momo payment URL generated successfully for order: {}", orderId);
                    return payUrl;
                } else {
                    String message = jsonResponse.has("message") ? jsonResponse.get("message").asText()
                            : "Unknown error";
                    log.error("Momo payment request failed with code {}: {}", resultCode, message);
                    throw new PaymentException("Momo payment request failed: " + message);
                }
            } else {
                throw new PaymentException("Momo API returned unexpected status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error creating Momo payment for order: {}", orderId, e);
            throw new PaymentException("Failed to create Momo payment", e);
        }
    }
}
