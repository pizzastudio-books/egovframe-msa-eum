package com.pizzastudio.eum.external.legacy;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 가상 행정정보에서 읽어 온 사업자 등록 정보.
 */
@Getter
@AllArgsConstructor
public class BusinessInfo {

    private String businessNo;
    private String businessName;
    private LocalDate openDate;
    private String statusCode;

    public boolean isOperating() {
        return "01".equals(statusCode);
    }
}
