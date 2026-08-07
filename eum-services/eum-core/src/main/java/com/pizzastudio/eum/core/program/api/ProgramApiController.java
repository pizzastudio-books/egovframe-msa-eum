package com.pizzastudio.eum.core.program.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pizzastudio.eum.core.common.dto.PageRequestDto;
import com.pizzastudio.eum.core.program.api.dto.ProgramResponseDto;
import com.pizzastudio.eum.core.program.api.dto.ProgramSaveRequestDto;
import com.pizzastudio.eum.core.program.api.dto.ProgramUpdateRequestDto;
import com.pizzastudio.eum.core.program.service.ProgramService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "지원 사업", description = "공고와 예산 관리")
@RestController
@RequiredArgsConstructor
public class ProgramApiController {

    private final ProgramService programService;

    @GetMapping("/api/v1/programs")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProgramResponseDto> search(PageRequestDto requestDto,
        @RequestParam(value = "categoryId", required = false) String categoryId,
        @RequestParam(value = "useAt", required = false) Boolean useAt,
        @PageableDefault(size = 10) Pageable pageable) {
        return programService.search(requestDto, categoryId, useAt, pageable);
    }

    @GetMapping("/api/v1/programs/{programId}")
    @ResponseStatus(HttpStatus.OK)
    public ProgramResponseDto findById(@PathVariable("programId") Long programId) {
        return programService.findById(programId);
    }

    @PostMapping("/api/v1/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponseDto save(@Valid @RequestBody ProgramSaveRequestDto saveRequestDto) {
        return programService.save(saveRequestDto);
    }

    @PutMapping("/api/v1/programs/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable("programId") Long programId,
        @Valid @RequestBody ProgramUpdateRequestDto updateRequestDto) {
        programService.update(programId, updateRequestDto);
    }

    @PutMapping("/api/v1/programs/{programId}/{useAt}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUseAt(@PathVariable("programId") Long programId,
        @PathVariable("useAt") Boolean useAt) {
        programService.updateUseAt(programId, useAt);
    }
}
