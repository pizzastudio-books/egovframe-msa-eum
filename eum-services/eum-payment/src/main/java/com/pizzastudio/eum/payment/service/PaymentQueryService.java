package com.pizzastudio.eum.payment.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.payment.api.dto.PaymentResponseDto;
import com.pizzastudio.eum.payment.domain.Payment;
import com.pizzastudio.eum.payment.domain.PaymentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지급 조회.
 *
 * <p>조회한 사람을 로그에 남긴다. 지급은 계좌와 금액을 다루므로 <b>누가 무엇을 열람했는지</b>가
 * 감사 대상이다. 게이트웨이의 감사 로그는 경로까지만 알고 어느 신청서인지는 모른다(16.4).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public List<PaymentResponseDto> find(String applicationId, String statusId) {
        log.info("지급 조회 열람자={} applicationId={} statusId={}", viewer(), applicationId, statusId);

        if (applicationId != null && !applicationId.isBlank()) {
            return paymentRepository.findByApplicationIdOrderByPaymentId(applicationId).stream()
                .map(PaymentResponseDto::new).toList();
        }
        if (statusId != null && !statusId.isBlank()) {
            return paymentRepository.findByStatusIdOrderByPaymentId(statusId).stream()
                .map(PaymentResponseDto::new).toList();
        }
        return paymentRepository.findAll().stream().map(PaymentResponseDto::new).toList();
    }

    public PaymentResponseDto findOne(Long paymentId) {
        log.info("지급 조회 열람자={} paymentId={}", viewer(), paymentId);
        return paymentRepository.findById(paymentId)
            .map(PaymentResponseDto::new)
            .orElseThrow(() -> new EntityNotFoundException("지급 내역이 없습니다. ID=" + paymentId));
    }

    private String viewer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "-" : authentication.getName();
    }

    /** 목록 화면이 여러 건을 한 번에 볼 때 쓴다. */
    List<Payment> all() {
        return paymentRepository.findAll();
    }
}
