import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Modal from '@/components/Modal';

// 모달 접근성·동작 테스트.
describe('Modal', () => {
  it('열렸을 때 dialog 역할과 제목을 노출한다', () => {
    render(<Modal open title="반려 사유" onClose={() => {}}>내용</Modal>);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('반려 사유')).toBeInTheDocument();
  });

  it('닫혀 있으면 아무것도 렌더링하지 않는다', () => {
    render(<Modal open={false} title="반려 사유" onClose={() => {}}>내용</Modal>);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('닫기 버튼을 누르면 onClose가 호출된다', async () => {
    const onClose = vi.fn();
    render(<Modal open title="반려 사유" onClose={onClose}>내용</Modal>);
    await userEvent.click(screen.getByLabelText('닫기'));
    expect(onClose).toHaveBeenCalled();
  });
});
