package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderRef(String orderRef);
    List<PaymentOrder> findAllByOrderByCreatedAtDesc();
}
