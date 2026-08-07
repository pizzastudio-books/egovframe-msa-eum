package com.pizzastudio.eum.external.legacy;

import java.sql.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * 타 기관 데이터베이스를 직접 조회한다.
 *
 * <p>연계 규격도 없고 담당 조직도 다르다. 상대 기관이 테이블을 바꾸면 이음이 깨진다.</p>
 */
@Slf4j
@Repository
public class LegacyLookupRepository {

    private final JdbcTemplate bizInfo;
    private final JdbcTemplate taxInfo;

    public LegacyLookupRepository(
        @Qualifier("bizInfoJdbcTemplate") JdbcTemplate bizInfo,
        @Qualifier("taxInfoJdbcTemplate") JdbcTemplate taxInfo) {
        this.bizInfo = bizInfo;
        this.taxInfo = taxInfo;
    }

    /** 사업자 등록 정보 조회 */
    public Optional<BusinessInfo> findBusinessInfo(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return Optional.empty();
        }
        return bizInfo.query(
            "select business_no, business_name, open_date, status_code "
                + "from biz_registration where business_no = ?",
            (rs, rowNum) -> {
                Date openDate = rs.getDate("open_date");
                return new BusinessInfo(
                    rs.getString("business_no"),
                    rs.getString("business_name"),
                    openDate == null ? null : openDate.toLocalDate(),
                    rs.getString("status_code"));
            },
            businessNo).stream().findFirst();
    }

    /** 체납액 조회. 없으면 0 */
    public long findArrearsAmount(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return 0L;
        }
        Long amount = taxInfo.queryForObject(
            "select coalesce(sum(arrears_amount), 0) from tax_arrears where business_no = ?",
            Long.class, businessNo);
        return amount == null ? 0L : amount;
    }
}
