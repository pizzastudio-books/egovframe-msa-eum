import { client } from '@/api/client';

// 신청·심사 API. 이음 백엔드는 봉투 없이 본문을 주고 목록은 스프링 Page 로 준다.

function toSummary(a) {
  return {
    applicationId: a.applicationId,
    programId: a.programId,
    programName: a.programName,
    applicantId: a.applicantId,
    amount: a.amount,
    status: a.statusId,
    statusLabel: a.statusLabel,
    bizNo: a.bizNo,
    bizName: a.bizName,
    ownerName: a.ownerName,
    rejectReason: a.rejectReason,
    createdAt: a.createDate,
  };
}

// 신청 생성 — 신청자
export async function createApplication(payload) {
  const body = await client.post('/applications', {
    programId: payload.programId,
    // 폼은 requestAmount 로, 이음은 amount 로 부른다
    amount: payload.requestAmount ?? payload.amount,
    purposeContent: payload.purposeContent ?? payload.bizName,
    accountNo: payload.accountNo,
    bizNo: payload.bizNo,
    bizName: payload.bizName,
    ownerName: payload.ownerName,
    industryCode: payload.industryCode,
    // 폼은 시군구까지 고르지만 이음은 한 칸으로 받는다
    regionCode: payload.regionCode,
    applicantContactNo: payload.applicantContactNo,
    applicantEmailAddr: payload.applicantEmailAddr,
  });
  return toSummary(body);
}

// 내 신청 목록 — 신청자
export async function fetchMyApplications() {
  const body = await client.get('/applications/mine', { params: { size: 100 } });
  return body.content.map(toSummary);
}

// 접수 목록(서버 페이징) — 담당자
export async function searchApplications(condition) {
  const body = await client.get('/applications', { params: condition });
  return {
    data: body.content.map(toSummary),
    page: body.number,
    size: body.size,
    totalElements: body.totalElements,
    totalPages: body.totalPages,
  };
}

// 상태별 건수 — 담당자. 이음은 { request: 1, approve: 0, ... } 로 준다.
export async function fetchStats() {
  const body = await client.get('/applications/stats');
  return Object.entries(body).map(([status, count]) => ({ status, count }));
}

// 신청 상세
export async function fetchApplication(applicationId) {
  const body = await client.get(`/applications/${applicationId}`);
  return {
    ...toSummary(body),
    purposeContent: body.purposeContent,
    accountNo: body.accountNo,
    applicantContactNo: body.applicantContactNo,
    applicantEmailAddr: body.applicantEmailAddr,
  };
}

// 심사 이력
export async function fetchReviews(applicationId) {
  return client.get(`/applications/${applicationId}/reviews`);
}

/**
 * 상태 전이.
 * 이음에서는 선정·반려가 심사이고, 취소는 별도 경로다.
 */
export async function transitionApplication(applicationId, { action, comment }) {
  if (action === 'cancel') {
    return client.put(`/applications/${applicationId}/cancel`, { reason: comment });
  }
  return client.post(`/applications/${applicationId}/reviews`, {
    resultId: action, // approve 또는 reject
    opinion: comment,
  });
}
