package com.pizzastudio.eum.payment.external;

/** 가상 금융망 연계. */
public interface BankTransferClient {

    boolean transfer(String accountNo, Long amount, String referenceId);
}
