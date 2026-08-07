package com.pizzastudio.eum.payment.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByEventId(String eventId);

    Optional<Payment> findByApplicationId(String applicationId);

    List<Payment> findByStatusIdOrderByPaymentId(String statusId);
}
