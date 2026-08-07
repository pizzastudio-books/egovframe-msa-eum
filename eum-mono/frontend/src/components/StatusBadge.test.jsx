import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from '@/components/StatusBadge';

// 컴포넌트 렌더 테스트. 상태 배지가 라벨 텍스트를 노출하는지 확인한다.
// (색만이 아니라 텍스트로 상태를 알리는 것이 접근성의 핵심이라, 이 테스트가 그 규칙을 검증한다.)
describe('StatusBadge', () => {
  it('라벨 텍스트를 화면에 보여 준다', () => {
    render(<StatusBadge status="APPROVED" label="승인" />);
    expect(screen.getByText('승인')).toBeInTheDocument();
  });

  it('라벨이 없으면 상태 코드를 대신 보여 준다', () => {
    render(<StatusBadge status="RECEIVED" />);
    expect(screen.getByText('RECEIVED')).toBeInTheDocument();
  });
});
