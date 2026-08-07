package com.pizzastudio.eum.external;

/**
 * 가상 금융망 연계. 이체를 요청한다.
 */
public interface BankTransferClient {

    /**
     * @return 이체 성공 여부
     */
    boolean transfer(String accountNo, Long amount, String referenceId);
}
