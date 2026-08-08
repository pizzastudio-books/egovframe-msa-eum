package com.pizzastudio.eum.core.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pizzastudio.eum.core.application.api.dto.ApplicationCancelRequestDto;
import com.pizzastudio.eum.core.application.api.dto.ApplicationResponseDto;
import com.pizzastudio.eum.core.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.core.application.api.dto.ApplicationUpdateRequestDto;
import com.pizzastudio.eum.core.application.domain.Application;
import com.pizzastudio.eum.core.application.domain.ApplicationRepository;
import com.pizzastudio.eum.core.code.service.CodeService;
import com.pizzastudio.eum.core.common.dto.PageRequestDto;
import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.pizzastudio.eum.core.common.exception.EntityNotFoundException;
import com.pizzastudio.eum.core.external.legacy.EligibilityChecker;
import com.pizzastudio.eum.core.member.domain.Role;
import com.pizzastudio.eum.core.member.service.MemberService;
import com.pizzastudio.eum.core.outbox.service.NotificationRequests;
import com.pizzastudio.eum.core.program.domain.Program;
import com.pizzastudio.eum.core.program.service.ProgramService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 지원금 신청.
 *
 * <p>접수 한 건이 신청 저장 · 예산 차감 · 알림 발송을 <b>한 트랜잭션</b>으로 처리한다.
 * 어느 하나가 실패하면 전부 되돌아간다. 이 단순함이 지금은 이점이지만, 4부에서 지급과
 * 알림을 떼어내는 순간 성립하지 않는다. 그 자리가 사가가 필요해지는 지점이다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProgramService programService;
    private final NotificationRequests notificationRequests;
    private final MemberService memberService;
    private final CodeService codeService;
    private final EligibilityChecker eligibilityChecker;

    /**
     * 접수.
     */
    public ApplicationResponseDto apply(ApplicationSaveRequestDto saveRequestDto) {
        // 담당자가 대리 접수할 때만 신청자를 지정할 수 있다. 지정하지 않으면 본인 신청이다.
        if (!isAdmin() || !StringUtils.hasText(saveRequestDto.getApplicantId())) {
            saveRequestDto.setApplicantId(currentMemberId());
        }

        // 타 기관 데이터베이스를 직접 읽어 자격을 본다. 상대가 내려가면 접수도 멈춘다.
        eligibilityChecker.check(memberService.findEntity(saveRequestDto.getApplicantId()));

        Program program = programService.findEntity(saveRequestDto.getProgramId());
        validate(program, saveRequestDto.getAmount());

        program.decreaseBudget(saveRequestDto.getAmount());

        Application saved = applicationRepository.insert(saveRequestDto.toEntity());
        saved.setProgram(program);

        // 업무가 무엇을 했는지 한 줄 남긴다. 요청 식별자는 로그 형식이 앞머리에 찍는다.
        // **식별자만 붙이고 남길 로그가 없으면 이을 것이 없다** — 실제로 그랬다.
        // 게이트웨이와 알림에는 식별자가 찍히는데 본체만 0건이었다(19.1).
        log.info("접수 신청번호={} 신청자={} 사업={} 금액={}",
            saved.getApplicationId(), saved.getApplicantId(), program.getProgramId(), saved.getAmount());

        notificationRequests.request(saved.getApplicantId(), "email",
            "지원금 신청이 접수되었습니다",
            program.getProgramName() + " 신청이 접수되었습니다. 신청번호 " + saved.getApplicationId());

        return ApplicationResponseDto.builder().entity(saved).statusLabel(statusLabelOf(saved)).build();
    }

    /**
     * 접수 가능 여부. 실패하면 그 자리에서 예외를 던진다.
     */
    private void validate(Program program, Long amount) {
        if (!Boolean.TRUE.equals(program.getUseAt())) {
            throw new BusinessMessageException("종료된 지원 사업입니다.");
        }
        if (!program.isOpen(LocalDateTime.now())) {
            throw new BusinessMessageException("접수 기간이 아닙니다.");
        }
        if (!program.withinLimit(amount)) {
            throw new BusinessMessageException(
                "건당 최대 신청 금액을 넘었습니다. (최대 " + program.getMaxAmountPerCase() + "원)");
        }
        if (!program.hasRemainBudget()) {
            throw new BusinessMessageException("예산이 모두 소진되었습니다.");
        }
        if (!program.canAfford(amount)) {
            throw new BusinessMessageException(
                "남은 예산이 부족합니다. (잔여 " + program.getRemainBudget() + "원)");
        }
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> search(PageRequestDto requestDto, Long programId,
        String statusId, Pageable pageable) {
        List<Application> applications =
            applicationRepository.search(requestDto, programId, statusId, pageable);
        return toPage(applications, pageable,
            applicationRepository.searchCount(requestDto, programId, statusId));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> searchMine(String statusId, Pageable pageable) {
        String memberId = currentMemberId();
        List<Application> applications =
            applicationRepository.searchForApplicant(memberId, statusId, pageable);
        return toPage(applications, pageable,
            applicationRepository.searchCountForApplicant(memberId, statusId));
    }

    @Transactional(readOnly = true)
    public ApplicationResponseDto findById(String applicationId) {
        Application application = findEntity(applicationId);
        application.setProgram(programService.findEntity(application.getProgramId()));
        return ApplicationResponseDto.builder().entity(application).statusLabel(statusLabelOf(application)).build();
    }

    /**
     * 수정. 접수 상태일 때만 되고, 금액이 바뀌면 예산도 그만큼 맞춘다.
     */
    public ApplicationResponseDto update(String applicationId, ApplicationUpdateRequestDto dto) {
        Application application = findEntity(applicationId);

        if (!isAdmin() && !application.isApplicant(currentMemberId())) {
            throw new BusinessMessageException("해당 신청은 수정할 수 없습니다.");
        }
        if (!application.isRequest()) {
            throw new BusinessMessageException("접수 상태인 경우에만 수정할 수 있습니다.");
        }

        Program program = programService.findEntity(application.getProgramId());
        long difference = dto.getAmount() - application.getAmount();
        if (difference > 0 && !program.canAfford(difference)) {
            throw new BusinessMessageException(
                "남은 예산이 부족합니다. (잔여 " + program.getRemainBudget() + "원)");
        }
        program.decreaseBudget(difference);

        application.updateForApplicant(dto.getAmount(), dto.getPurposeContent(),
            dto.getAttachmentCode(), dto.getApplicantContactNo(), dto.getApplicantEmailAddr(),
            dto.getAccountNo());
        application.setProgram(program);

        return ApplicationResponseDto.builder().entity(application).statusLabel(statusLabelOf(application)).build();
    }

    /**
     * 취소. 차감했던 예산을 되돌린다.
     */
    public void cancel(String applicationId, ApplicationCancelRequestDto cancelRequestDto) {
        Application application = findEntity(applicationId);

        if (!isAdmin() && !application.isApplicant(currentMemberId())) {
            throw new BusinessMessageException("해당 신청은 취소할 수 없습니다.");
        }

        application.cancel(cancelRequestDto.getReason(), "이미 지급된 신청은 취소할 수 없습니다.");

        Program program = programService.findEntity(application.getProgramId());
        program.decreaseBudget(-application.getAmount());

        notificationRequests.request(application.getApplicantId(), "email",
            "지원금 신청이 취소되었습니다",
            "신청번호 " + application.getApplicationId() + " 이 취소되었습니다.");
    }

    /**
     * 담당자 대시보드 — 상태별 건수.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> stats() {
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (com.pizzastudio.eum.core.application.domain.ApplicationStatus status
            : com.pizzastudio.eum.core.application.domain.ApplicationStatus.values()) {
            result.put(status.getKey(),
                applicationRepository.searchCount(null, null, status.getKey()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Application findEntity(String applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("신청이 없습니다. ID=" + applicationId));
    }

    private Page<ApplicationResponseDto> toPage(List<Application> applications, Pageable pageable,
        long total) {
        List<ApplicationResponseDto> content = applications.stream()
            .map(a -> ApplicationResponseDto.builder().entity(a).statusLabel(statusLabelOf(a)).build())
            .toList();
        return new PageImpl<>(content, pageable, total);
    }

    /** 화면이 그대로 뿌릴 수 있게 상태 이름을 붙여 준다. */
    private String statusLabelOf(Application application) {
        return codeService.nameOf(application.getStatusId());
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (Role.ADMIN.getKey().equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
