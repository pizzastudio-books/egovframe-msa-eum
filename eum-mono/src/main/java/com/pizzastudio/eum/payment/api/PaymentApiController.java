package com.pizzastudio.eum.payment.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.pizzastudio.eum.payment.api.dto.PaymentResponseDto;
import com.pizzastudio.eum.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "지급", description = "지급 지시와 이체 결과")
@RestController
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @GetMapping("/api/v1/applications/{applicationId}/payment")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponseDto findByApplication(@PathVariable("applicationId") String applicationId) {
        return paymentService.findByApplication(applicationId);
    }
}
