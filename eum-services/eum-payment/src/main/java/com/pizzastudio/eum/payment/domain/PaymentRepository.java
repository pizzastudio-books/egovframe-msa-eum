package com.pizzastudio.eum.payment.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByEventId(String eventId);

    /**
     * 신청 한 건에 지급이 둘 생길 수 있다. 3부에는 {@code existsByApplicationId} 로 막는
     * 코드가 있었는데 4부에서는 멱등 열쇠가 이벤트로 옮겨 가며 그 보장이 사라졌다.
     * 표에도 {@code application_id} 유니크 키가 없다.
     *
     * <p>{@code Optional} 로 받으면 두 건일 때 예외가 나고 500 이 된다. 목록으로 받는다(17.2).</p>
     */
    List<Payment> findByApplicationIdOrderByPaymentId(String applicationId);

    Optional<Payment> findByApplicationId(String applicationId);

    List<Payment> findByStatusIdOrderByPaymentId(String statusId);
}
