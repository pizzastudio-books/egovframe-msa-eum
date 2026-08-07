package com.pizzastudio.eum.program.domain;

import static com.pizzastudio.eum.program.domain.QProgram.program;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import com.pizzastudio.eum.common.dto.PageRequestDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/**
 * 지원 사업 조회. 조건이 있을 때만 붙인다.
 */
@RequiredArgsConstructor
public class ProgramRepositoryImpl implements ProgramRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Program> search(PageRequestDto requestDto, String categoryId, Boolean useAt, Pageable pageable) {
        return queryFactory
            .selectFrom(program)
            .where(where(requestDto, categoryId, useAt))
            .orderBy(program.requestEndDate.desc(), program.programId.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public long searchCount(PageRequestDto requestDto, String categoryId, Boolean useAt) {
        Long count = queryFactory
            .select(program.count())
            .from(program)
            .where(where(requestDto, categoryId, useAt))
            .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanBuilder where(PageRequestDto requestDto, String categoryId, Boolean useAt) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(categoryId)) {
            builder.and(program.categoryId.eq(categoryId));
        }
        if (useAt != null) {
            builder.and(program.useAt.eq(useAt));
        }
        if (requestDto != null && StringUtils.hasText(requestDto.getKeyword())
            && "programName".equals(requestDto.getKeywordType())) {
            builder.and(program.programName.contains(requestDto.getKeyword()));
        }
        return builder;
    }
}
