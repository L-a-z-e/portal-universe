import { render, screen, fireEvent } from '@testing-library/react';
import { Alert } from './Alert';

describe('Alert', () => {
  describe('Rendering', () => {
    it('renders alert element', () => {
      render(<Alert>Alert message</Alert>);
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('renders children content', () => {
      render(<Alert>Test content</Alert>);
      expect(screen.getByText('Test content')).toBeInTheDocument();
    });

    it('renders title when provided', () => {
      render(<Alert title="Alert Title">Content</Alert>);
      expect(screen.getByText('Alert Title')).toBeInTheDocument();
    });

    it('shows icon by default', () => {
      render(<Alert>With icon</Alert>);
      expect(screen.getByRole('alert').querySelector('svg')).toBeInTheDocument();
    });

    it('hides icon when showIcon is false', () => {
      render(<Alert showIcon={false}>No icon</Alert>);
      const alert = screen.getByRole('alert');
      const svgs = alert.querySelectorAll('svg');
      expect(svgs.length).toBe(0);
    });
  });

  describe('Variants', () => {
    it.each(['info', 'success', 'warning', 'error'] as const)(
      'renders %s variant',
      (variant) => {
        render(<Alert variant={variant}>{variant} alert</Alert>);
        expect(screen.getByRole('alert')).toBeInTheDocument();
      }
    );

    it('defaults to info variant', () => {
      render(<Alert>Default</Alert>);
      expect(screen.getByRole('alert')).toHaveClass('bg-status-infoBg');
    });
  });

  describe('Bordered', () => {
    it('applies border when bordered is true', () => {
      render(<Alert bordered>Bordered alert</Alert>);
      expect(screen.getByRole('alert')).toHaveClass('border');
    });

    it('does not apply border by default', () => {
      render(<Alert>No border</Alert>);
      expect(screen.getByRole('alert')).not.toHaveClass('border');
    });
  });

  describe('Dismissible', () => {
    it('shows dismiss button when dismissible is true', () => {
      render(<Alert dismissible>Dismissible alert</Alert>);
      expect(screen.getByRole('button', { name: /dismiss/i })).toBeInTheDocument();
    });

    it('does not show dismiss button by default', () => {
      render(<Alert>Not dismissible</Alert>);
      expect(screen.queryByRole('button', { name: /dismiss/i })).not.toBeInTheDocument();
    });

    it('removes alert when dismiss button is clicked', () => {
      render(<Alert dismissible>Dismissible</Alert>);
      fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('calls onDismiss callback when dismissed', () => {
      const handleDismiss = vi.fn();
      render(
        <Alert dismissible onDismiss={handleDismiss}>
          Dismissible
        </Alert>
      );
      fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
      expect(handleDismiss).toHaveBeenCalled();
    });
  });

  describe('Custom className', () => {
    it('accepts custom className', () => {
      render(<Alert className="custom-class">Custom</Alert>);
      expect(screen.getByRole('alert')).toHaveClass('custom-class');
    });
  });

  describe('Ref Forwarding', () => {
    it('forwards ref to div element', () => {
      const ref = { current: null } as React.RefObject<HTMLDivElement>;
      render(<Alert ref={ref}>Ref test</Alert>);
      expect(ref.current).toBeInstanceOf(HTMLDivElement);
    });
  });

  describe('Accessibility', () => {
    it('has role="alert"', () => {
      render(<Alert>Accessible</Alert>);
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('dismiss button has accessible label', () => {
      render(<Alert dismissible>Dismissible</Alert>);
      expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    });
  });
});
