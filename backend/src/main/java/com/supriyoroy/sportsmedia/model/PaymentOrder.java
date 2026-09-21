package com.supriyoroy.sportsmedia.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Scaffolding for the paid tier you said you'd add later.
 * Records an intent; plug Razorpay/Cashfree (UPI, GPay, Paytm) into PaymentService when you're ready.
 */
@Entity @Table(name = "payment_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderRef;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String planCode;            // e.g. MONTHLY_AD_FREE
    private BigDecimal amount;
    private String currency = "INR";

    /** UPI | GPAY | PAYTM | PHONEPE | CARD */
    private String method;

    /** CREATED | PENDING | PAID | FAILED */
    private String status = "CREATED";

    private String gatewayOrderId;
    private String gatewayPaymentId;
    private Instant createdAt = Instant.now();
}
