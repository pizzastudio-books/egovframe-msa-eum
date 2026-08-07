package com.pizzastudio.eum.code.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pizzastudio.eum.code.api.dto.CodeResponseDto;
import com.pizzastudio.eum.code.service.CodeService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "공통코드", description = "화면에서 쓰는 코드 목록")
@RestController
@RequiredArgsConstructor
public class CodeApiController {

    private final CodeService codeService;

    @GetMapping("/api/v1/common-codes/{group}")
    @ResponseStatus(HttpStatus.OK)
    public List<CodeResponseDto> findByGroup(@PathVariable("group") String group) {
        return codeService.findByParent(group).stream()
            .map(code -> CodeResponseDto.builder().entity(code).build())
            .toList();
    }
}
