import { client } from '@/api/client';

// 인증 API.
export async function login({ username, password }) {
  // 이음은 memberId·password 로 받고 token·memberId·memberName·roleId 로 준다.
  const body = await client.post('/auth/login', { memberId: username, password });
  return {
    accessToken: body.token,
    userId: body.memberId,
    name: body.memberName,
    role: body.roleId,
  };
}

export async function fetchMe() {
  const body = await client.get('/auth/me');
  return {
    userId: body.memberId,
    username: body.memberId,
    name: body.memberName,
    role: body.roleId,
    businessNo: body.businessNo,
  };
}
