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
import lombok.extern.slf4j.Slf4j;

/**
 * 공통코드 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;

    /**
     * 없는 그룹을 물어도 빈 목록이 나간다. 그래서 <b>화면만 조용히 빈다.</b>
     *
     * <p>실제로 겪었다 — 화면이 {@code APP_STATUS} 를 부르는데 자료에는
     * {@code application-status} 로 들어 있었다. 404 가 아니라 200 에 빈 배열이라
     * 로그에도 안 남고 접수 목록의 상태 필터가 늘 비어 있었다(17.1).</p>
     *
     * <p>그래서 빈 결과를 경고로 남긴다. 코드가 정말 없는 그룹이면 그것도 알아야 한다.</p>
     */
    public List<Code> findByParent(String parentCodeId) {
        List<Code> codes = codeRepository.findByParentCodeIdAndUseAtTrueOrderBySortSeq(parentCodeId);
        if (codes.isEmpty()) {
            log.warn("공통코드가 비어 있습니다. 그룹 이름을 확인하십시오. group={}", parentCodeId);
        }
        return codes;
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
