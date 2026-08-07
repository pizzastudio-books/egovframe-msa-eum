package com.pizzastudio.eum.application.api;

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

import com.pizzastudio.eum.application.api.dto.ApplicationCancelRequestDto;
import com.pizzastudio.eum.application.api.dto.ApplicationResponseDto;
import com.pizzastudio.eum.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.application.api.dto.ApplicationUpdateRequestDto;
import com.pizzastudio.eum.application.service.ApplicationService;
import com.pizzastudio.eum.common.dto.PageRequestDto;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "지원금 신청", description = "접수·수정·취소")
@RestController
@RequiredArgsConstructor
public class ApplicationApiController {

    private final ApplicationService applicationService;

    @PostMapping("/api/v1/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponseDto apply(@Valid @RequestBody ApplicationSaveRequestDto saveRequestDto) {
        return applicationService.apply(saveRequestDto);
    }

    @GetMapping("/api/v1/applications")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ApplicationResponseDto> search(PageRequestDto requestDto,
        @RequestParam(value = "programId", required = false) Long programId,
        @RequestParam(value = "statusId", required = false) String statusId,
        @PageableDefault(size = 10) Pageable pageable) {
        return applicationService.search(requestDto, programId, statusId, pageable);
    }

    @GetMapping("/api/v1/applications/stats")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.Map<String, Long> stats() {
        return applicationService.stats();
    }

    @GetMapping("/api/v1/applications/mine")
    @ResponseStatus(HttpStatus.OK)
    public Page<ApplicationResponseDto> searchMine(
        @RequestParam(value = "statusId", required = false) String statusId,
        @PageableDefault(size = 10) Pageable pageable) {
        return applicationService.searchMine(statusId, pageable);
    }

    @GetMapping("/api/v1/applications/{applicationId}")
    @ResponseStatus(HttpStatus.OK)
    public ApplicationResponseDto findById(@PathVariable("applicationId") String applicationId) {
        return applicationService.findById(applicationId);
    }

    @PutMapping("/api/v1/applications/{applicationId}")
    @ResponseStatus(HttpStatus.OK)
    public ApplicationResponseDto update(@PathVariable("applicationId") String applicationId,
        @Valid @RequestBody ApplicationUpdateRequestDto updateRequestDto) {
        return applicationService.update(applicationId, updateRequestDto);
    }

    @PutMapping("/api/v1/applications/{applicationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable("applicationId") String applicationId,
        @RequestBody ApplicationCancelRequestDto cancelRequestDto) {
        applicationService.cancel(applicationId, cancelRequestDto);
    }
}
