package com.supriyoroy.sportsmedia.service;

import com.supriyoroy.sportsmedia.dto.Dtos.*;
import com.supriyoroy.sportsmedia.model.PaymentOrder;
import com.supriyoroy.sportsmedia.repo.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Placeholder gateway. Records the order and hands back a UPI deep link that
 * GPay / Paytm / PhonePe all understand, so you can collect money today and
 * swap in Razorpay or Cashfree later without changing the frontend contract.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository repo;
    private final SettingsService settings;

    public PaymentIntentResponse create(PaymentIntentRequest req) {
        String ref = "SM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentOrder order = repo.save(PaymentOrder.builder()
                .orderRef(ref)
                .planCode(req.planCode())
                .amount(req.amount())
                .currency("INR")
                .method(req.method() == null ? "UPI" : req.method())
                .customerName(req.customerName())
                .customerEmail(req.customerEmail())
                .customerPhone(req.customerPhone())
                .status("PENDING")
                .build());

        String vpa = settings.get("payments.upiId", "");
        String payee = settings.get("payments.payeeName", "Supriyo Roy");
        String link = vpa.isBlank() ? null :
                "upi://pay?pa=" + enc(vpa) + "&pn=" + enc(payee)
                        + "&am=" + req.amount() + "&cu=INR&tn=" + enc(ref);

        return new PaymentIntentResponse(order.getOrderRef(), order.getStatus(),
                order.getAmount(), "INR", link,
                link == null ? "Add your UPI ID in Admin > Settings to enable payments." : "Pay and keep the reference.");
    }

    private String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
