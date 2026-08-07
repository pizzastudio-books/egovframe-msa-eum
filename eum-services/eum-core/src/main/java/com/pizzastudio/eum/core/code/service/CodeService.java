package com.pizzastudio.eum.core.code.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.code.domain.Code;
import com.pizzastudio.eum.core.code.domain.CodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 공통코드 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;

    public List<Code> findByParent(String parentCodeId) {
        return codeRepository.findByParentCodeIdAndUseAtTrueOrderBySortSeq(parentCodeId);
    }

    /**
     * 코드 이름을 한 번에 붙이기 위한 사전.
     */
    public Map<String, String> nameMapOf(String parentCodeId) {
        return findByParent(parentCodeId).stream()
            .collect(Collectors.toMap(Code::getCodeId, Code::getCodeName, (a, b) -> a));
    }

    public String nameOf(String codeId) {
        return codeRepository.findById(codeId).map(Code::getCodeName).orElse(null);
    }

    public List<String> idsOf(String parentCodeId) {
        return findByParent(parentCodeId).stream().map(Code::getCodeId).collect(Collectors.toList());
    }

    /** 코드 목록을 이름 사전으로 바꾼다. */
    public Map<String, String> toNameMap(List<Code> codes) {
        return codes.stream().collect(Collectors.toMap(Code::getCodeId, Code::getCodeName, (a, b) -> a));
    }

    public Function<String, String> nameResolver(String parentCodeId) {
        Map<String, String> names = nameMapOf(parentCodeId);
        return names::get;
    }
}
