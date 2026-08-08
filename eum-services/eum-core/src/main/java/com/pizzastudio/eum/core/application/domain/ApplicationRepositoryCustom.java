package com.pizzastudio.eum.core.application.domain;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.pizzastudio.eum.core.common.dto.PageRequestDto;

public interface ApplicationRepositoryCustom {

    List<Application> search(PageRequestDto requestDto, Long programId, String statusId, Pageable pageable);

    long searchCount(PageRequestDto requestDto, Long programId, String statusId);

    List<Application> searchForApplicant(String applicantId, String statusId, Pageable pageable);

    long searchCountForApplicant(String applicantId, String statusId);


    Application insert(Application application);
}
