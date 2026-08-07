package com.pizzastudio.eum.payment.external;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** 실습용 이체 연계. 계좌번호가 비어 있지 않으면 성공으로 본다. */
@Slf4j
@Component
public class MockBankTransferClient implements BankTransferClient {

    @Override
    public boolean transfer(String accountNo, Long amount, String referenceId) {
        log.info("이체 요청 account={} amount={} ref={}", accountNo, amount, referenceId);
        return accountNo != null && !accountNo.isBlank() && amount != null && amount > 0;
    }
}
