package com.pizzastudio.eum.core.program.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.code.service.CodeService;
import com.pizzastudio.eum.core.common.dto.PageRequestDto;
import com.pizzastudio.eum.core.common.exception.EntityNotFoundException;
import com.pizzastudio.eum.core.program.api.dto.ProgramResponseDto;
import com.pizzastudio.eum.core.program.api.dto.ProgramSaveRequestDto;
import com.pizzastudio.eum.core.program.api.dto.ProgramUpdateRequestDto;
import com.pizzastudio.eum.core.program.domain.Program;
import com.pizzastudio.eum.core.program.domain.ProgramRepository;

import lombok.RequiredArgsConstructor;

/**
 * 지원 사업 관리.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramService {

    /** 지원 유형 공통코드의 상위 코드 */
    public static final String CATEGORY_CODE = "support-category";

    private final ProgramRepository programRepository;
    private final CodeService codeService;

    @Transactional(readOnly = true)
    public Page<ProgramResponseDto> search(PageRequestDto requestDto, String categoryId,
        Boolean useAt, Pageable pageable) {
        List<Program> programs = programRepository.search(requestDto, categoryId, useAt, pageable);
        Map<String, String> categoryNames = codeService.nameMapOf(CATEGORY_CODE);
        List<ProgramResponseDto> content = programs.stream()
            .map(program -> program.setCategoryName(categoryNames.get(program.getCategoryId())))
            .map(program -> ProgramResponseDto.builder().entity(program).build())
            .toList();
        return new PageImpl<>(content, pageable,
            programRepository.searchCount(requestDto, categoryId, useAt));
    }

    @Transactional(readOnly = true)
    public ProgramResponseDto findById(Long programId) {
        Program program = findEntity(programId);
        program.setCategoryName(codeService.nameOf(program.getCategoryId()));
        return ProgramResponseDto.builder().entity(program).build();
    }

    public ProgramResponseDto save(ProgramSaveRequestDto saveRequestDto) {
        Program saved = programRepository.save(saveRequestDto.toEntity());
        return ProgramResponseDto.builder().entity(saved).build();
    }

    public void update(Long programId, ProgramUpdateRequestDto updateRequestDto) {
        findEntity(programId).update(
            updateRequestDto.getProgramName(),
            updateRequestDto.getTotalBudget(),
            updateRequestDto.getMaxAmountPerCase(),
            updateRequestDto.getRequestStartDate(),
            updateRequestDto.getRequestEndDate(),
            updateRequestDto.getPurposeContent());
    }

    public void updateUseAt(Long programId, Boolean useAt) {
        findEntity(programId).updateUseAt(useAt);
    }

    /**
     * 신청·심사에서 쓰는 조회. 잠금을 걸지 않으므로 예산 차감은 같은 트랜잭션 안에서 한다.
     */
    @Transactional(readOnly = true)
    public Program findEntity(Long programId) {
        return programRepository.findById(programId)
            .orElseThrow(() -> new EntityNotFoundException("지원 사업이 없습니다. ID=" + programId));
    }
}
