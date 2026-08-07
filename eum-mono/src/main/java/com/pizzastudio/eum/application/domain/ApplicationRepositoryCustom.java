package com.pizzastudio.eum.application.domain;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.pizzastudio.eum.common.dto.PageRequestDto;

public interface ApplicationRepositoryCustom {

    List<Application> search(PageRequestDto requestDto, Long programId, String statusId, Pageable pageable);

    long searchCount(PageRequestDto requestDto, Long programId, String statusId);

    List<Application> searchForApplicant(String applicantId, String statusId, Pageable pageable);

    long searchCountForApplicant(String applicantId, String statusId);

    /** 지급 대상 — 선정됐고 아직 지급되지 않은 건 */
    List<Application> findApprovedForPayment(int limit);

    Application insert(Application application);
}
