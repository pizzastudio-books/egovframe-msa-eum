import { client } from '@/api/client';

// 공통코드 API. 한 번 받아 캐시하고 폼·필터·배지에서 재사용한다.
export async function fetchCodes(codeGroup, parentCode) {
  const body = await client.get(`/common-codes/${parentCode || codeGroup}`);
  // 이음은 [{ codeId, codeName, sortSeq }] 로 준다.
  return body.map((c) => ({ code: c.codeId, codeName: c.codeName, sortSeq: c.sortSeq }));
}
