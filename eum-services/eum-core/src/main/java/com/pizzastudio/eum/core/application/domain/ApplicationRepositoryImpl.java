package com.pizzastudio.eum.core.application.domain;

import static com.pizzastudio.eum.core.application.domain.QApplication.application;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import com.pizzastudio.eum.core.common.dto.PageRequestDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * 신청 조회.
 */
@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Application> search(PageRequestDto requestDto, Long programId, String statusId,
        Pageable pageable) {
        return queryFactory
            .selectFrom(application)
            .where(where(requestDto, programId, statusId))
            .orderBy(application.createDate.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public long searchCount(PageRequestDto requestDto, Long programId, String statusId) {
        Long count = queryFactory
            .select(application.count())
            .from(application)
            .where(where(requestDto, programId, statusId))
            .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public List<Application> searchForApplicant(String applicantId, String statusId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder().and(application.applicantId.eq(applicantId));
        if (StringUtils.hasText(statusId)) {
            builder.and(application.statusId.eq(statusId));
        }
        return queryFactory
            .selectFrom(application)
            .where(builder)
            .orderBy(application.createDate.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public long searchCountForApplicant(String applicantId, String statusId) {
        BooleanBuilder builder = new BooleanBuilder().and(application.applicantId.eq(applicantId));
        if (StringUtils.hasText(statusId)) {
            builder.and(application.statusId.eq(statusId));
        }
        Long count = queryFactory
            .select(application.count())
            .from(application)
            .where(builder)
            .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public List<Application> findApprovedForPayment(int limit) {
        return queryFactory
            .selectFrom(application)
            .where(application.statusId.eq(ApplicationStatus.APPROVE.getKey()))
            .orderBy(application.createDate.asc())
            .limit(limit)
            .fetch();
    }

    /**
     * 신청 저장. 식별자를 앱이 만들므로 save() 대신 persist 를 쓴다.
     * save() 는 식별자가 있으면 merge 로 동작해 select 가 한 번 더 나간다.
     */
    @Override
    public Application insert(Application entity) {
        entityManager.persist(entity);
        return entity;
    }

    private BooleanBuilder where(PageRequestDto requestDto, Long programId, String statusId) {
        BooleanBuilder builder = new BooleanBuilder();
        if (programId != null) {
            builder.and(application.programId.eq(programId));
        }
        if (StringUtils.hasText(statusId)) {
            builder.and(application.statusId.eq(statusId));
        }
        if (requestDto != null && StringUtils.hasText(requestDto.getKeyword())
            && "applicant".equals(requestDto.getKeywordType())) {
            builder.and(application.applicantId.contains(requestDto.getKeyword()));
        }
        return builder;
    }
}
