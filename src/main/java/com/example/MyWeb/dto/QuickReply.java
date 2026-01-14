package com.example.MyWeb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QuickReply DTO for chatbot quick action buttons
 * Used to suggest quick replies/actions to users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickReply {

    /**
     * Display label shown on the button
     * Example: "Tìm sản phẩm", "Kiểm tra đơn hàng"
     */
    private String label;

    /**
     * Value sent back when user clicks this quick reply
     * Example: "tìm sản phẩm", "kiểm tra đơn hàng"
     */
    private String value;

    /**
     * Optional icon emoji
     * Example: "🔍", "📦", "💳"
     */
    private String icon;
}
