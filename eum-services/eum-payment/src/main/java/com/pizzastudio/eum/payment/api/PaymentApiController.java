package com.pizzastudio.eum.payment.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pizzastudio.eum.payment.api.dto.PaymentResponseDto;
import com.pizzastudio.eum.payment.service.PaymentQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 지급 조회.
 *
 * <p>3부에서는 {@code /api/v1/applications/{id}/payment} 였다. 신청 아래에 있는 경로였는데
 * 데이터 주인은 지급이다. 서비스를 나누자 그 경로가 본체로 가서 404 가 났다.</p>
 *
 * <p>권한은 {@code SecurityConfig} 의 경로 규칙으로 건다. 메서드에 걸면 거부 응답이
 * 401 로 나온다(17.2).</p>
 *
 * <p><b>경로도 소유권을 따라간다.</b> 지급 데이터를 지급 서비스가 갖는다면 경로도
 * {@code /api/v1/payments} 아래여야 한다. 게이트웨이 라우트가 그 접두사로 갈라 주기
 * 때문이다(16.1·17.1).</p>
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentQueryService paymentQueryService;

    /** 신청 한 건의 지급 현황. 3부의 경로를 대신한다. */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<PaymentResponseDto> find(
        @RequestParam(value = "applicationId", required = false) String applicationId,
        @RequestParam(value = "statusId", required = false) String statusId) {
        return paymentQueryService.find(applicationId, statusId);
    }

    @GetMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponseDto findOne(@PathVariable("paymentId") Long paymentId) {
        return paymentQueryService.findOne(paymentId);
    }
}
