package com.pizzastudio.eum.program.domain;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.pizzastudio.eum.common.dto.PageRequestDto;

public interface ProgramRepositoryCustom {

    List<Program> search(PageRequestDto requestDto, String categoryId, Boolean useAt, Pageable pageable);

    long searchCount(PageRequestDto requestDto, String categoryId, Boolean useAt);
}
