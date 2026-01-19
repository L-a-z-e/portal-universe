import { render, screen, fireEvent } from '@testing-library/react';
import { Modal } from './Modal';

describe('Modal', () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders modal when open is true', () => {
      render(<Modal {...defaultProps}>Modal content</Modal>);
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('does not render modal when open is false', () => {
      render(
        <Modal {...defaultProps} open={false}>
          Modal content
        </Modal>
      );
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders children content', () => {
      render(<Modal {...defaultProps}>Test content</Modal>);
      expect(screen.getByText('Test content')).toBeInTheDocument();
    });

    it('renders title when provided', () => {
      render(<Modal {...defaultProps} title="Modal Title">Content</Modal>);
      expect(screen.getByText('Modal Title')).toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it.each(['sm', 'md', 'lg', 'xl'] as const)('renders %s size', (size) => {
      render(
        <Modal {...defaultProps} size={size}>
          Content
        </Modal>
      );
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  describe('Close Button', () => {
    it('shows close button by default', () => {
      render(<Modal {...defaultProps}>Content</Modal>);
      expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
    });

    it('hides close button when showClose is false', () => {
      render(
        <Modal {...defaultProps} showClose={false}>
          Content
        </Modal>
      );
      expect(screen.queryByRole('button', { name: /close/i })).not.toBeInTheDocument();
    });

    it('calls onClose when close button is clicked', () => {
      const onClose = vi.fn();
      render(
        <Modal {...defaultProps} onClose={onClose}>
          Content
        </Modal>
      );
      fireEvent.click(screen.getByRole('button', { name: /close/i }));
      expect(onClose).toHaveBeenCalled();
    });
  });

  describe('Backdrop', () => {
    it('closes on backdrop click by default', () => {
      const onClose = vi.fn();
      render(
        <Modal {...defaultProps} onClose={onClose}>
          Content
        </Modal>
      );
      const backdrop = screen.getByRole('dialog').parentElement?.querySelector('[aria-hidden="true"]');
      if (backdrop) {
        fireEvent.click(backdrop);
      }
      expect(onClose).toHaveBeenCalled();
    });

    it('does not close on backdrop click when closeOnBackdrop is false', () => {
      const onClose = vi.fn();
      render(
        <Modal {...defaultProps} onClose={onClose} closeOnBackdrop={false}>
          Content
        </Modal>
      );
      const backdrop = screen.getByRole('dialog').parentElement?.querySelector('[aria-hidden="true"]');
      if (backdrop) {
        fireEvent.click(backdrop);
      }
      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('Escape Key', () => {
    it('closes on escape key by default', () => {
      const onClose = vi.fn();
      render(
        <Modal {...defaultProps} onClose={onClose}>
          Content
        </Modal>
      );
      fireEvent.keyDown(document, { key: 'Escape' });
      expect(onClose).toHaveBeenCalled();
    });

    it('does not close on escape when closeOnEscape is false', () => {
      const onClose = vi.fn();
      render(
        <Modal {...defaultProps} onClose={onClose} closeOnEscape={false}>
          Content
        </Modal>
      );
      fireEvent.keyDown(document, { key: 'Escape' });
      expect(onClose).not.toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    it('has role="dialog"', () => {
      render(<Modal {...defaultProps}>Content</Modal>);
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('has aria-modal="true"', () => {
      render(<Modal {...defaultProps}>Content</Modal>);
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
    });

    it('has aria-labelledby when title is provided', () => {
      render(<Modal {...defaultProps} title="Test Title">Content</Modal>);
      expect(screen.getByRole('dialog')).toHaveAttribute('aria-labelledby', 'modal-title');
    });

    it('close button has accessible label', () => {
      render(<Modal {...defaultProps}>Content</Modal>);
      expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
    });
  });

  describe('Body Overflow', () => {
    it('sets body overflow to hidden when open', () => {
      render(<Modal {...defaultProps}>Content</Modal>);
      expect(document.body.style.overflow).toBe('hidden');
    });

    it('removes body overflow when closed', () => {
      const { rerender } = render(<Modal {...defaultProps}>Content</Modal>);
      rerender(
        <Modal {...defaultProps} open={false}>
          Content
        </Modal>
      );
      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('Custom className', () => {
    it('accepts custom className', () => {
      render(
        <Modal {...defaultProps} className="custom-class">
          Content
        </Modal>
      );
      const modalContent = screen.getByRole('dialog').querySelector('.custom-class');
      expect(modalContent).toBeInTheDocument();
    });
  });

  describe('Ref Forwarding', () => {
    it('forwards ref to modal content div', () => {
      const ref = { current: null } as React.RefObject<HTMLDivElement>;
      render(
        <Modal {...defaultProps} ref={ref}>
          Content
        </Modal>
      );
      expect(ref.current).toBeInstanceOf(HTMLDivElement);
    });
  });
});
