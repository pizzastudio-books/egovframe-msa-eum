package com.pizzastudio.eum.external;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 실습용 이체 연계. 계좌번호가 비어 있지 않으면 성공으로 본다.
 *
 * <p>실제 사업에서는 기관 금융망 게이트웨이를 호출한다. 이 책에서는 목 서버로 둔다.</p>
 */
@Slf4j
@Component
public class MockBankTransferClient implements BankTransferClient {

    @Override
    public boolean transfer(String accountNo, Long amount, String referenceId) {
        log.info("이체 요청 account={} amount={} ref={}", accountNo, amount, referenceId);
        return accountNo != null && !accountNo.isBlank() && amount != null && amount > 0;
    }
}
