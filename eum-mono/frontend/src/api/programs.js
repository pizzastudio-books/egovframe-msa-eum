import { client } from '@/api/client';

// 지원사업 API.
// 이음 백엔드는 스프링 Page 를 그대로 준다. 화면이 쓰는 모양으로 바꿔 준다.
function toProgram(p) {
  return {
    programId: p.programId,
    programName: p.programName,
    type: { code: p.categoryId, codeName: p.categoryName },
    applyStart: p.requestStartDate,
    applyEnd: p.requestEndDate,
    supportLimit: p.maxAmountPerCase,
    remainBudget: p.remainBudget,
    totalBudget: p.totalBudget,
    target: p.managerDeptName,
    description: p.purposeContent,
  };
}

export async function fetchPrograms(programType) {
  const body = await client.get('/programs', {
    params: { useAt: true, size: 100, ...(programType ? { categoryId: programType } : {}) },
  });
  return body.content.map(toProgram);
}

export async function fetchProgram(programId) {
  return toProgram(await client.get(`/programs/${programId}`));
}
