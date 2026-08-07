import { describe, it, expect } from 'vitest';
import { formatMoney } from '@/lib/format';

// 금액 포맷 단위 테스트. 순수 함수라 가장 쉬운 출발점이다.
describe('formatMoney', () => {
  it('원 단위로 천 단위 구분 쉼표를 찍는다', () => {
    expect(formatMoney(15000000)).toBe('15,000,000원');
  });

  it('값이 없으면 하이픈을 돌려준다', () => {
    expect(formatMoney(null)).toBe('-');
    expect(formatMoney(undefined)).toBe('-');
  });
});
