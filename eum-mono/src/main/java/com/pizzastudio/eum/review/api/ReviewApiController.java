package com.pizzastudio.eum.review.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pizzastudio.eum.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.review.api.dto.ReviewResponseDto;
import com.pizzastudio.eum.review.service.ReviewService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "심사", description = "선정·반려")
@RestController
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/applications/{applicationId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponseDto review(@PathVariable("applicationId") String applicationId,
        @Valid @RequestBody ReviewRequestDto requestDto) {
        return reviewService.review(applicationId, requestDto);
    }

    @GetMapping("/api/v1/applications/{applicationId}/reviews")
    @ResponseStatus(HttpStatus.OK)
    public List<ReviewResponseDto> findByApplication(
        @PathVariable("applicationId") String applicationId) {
        return reviewService.findByApplication(applicationId);
    }
}
