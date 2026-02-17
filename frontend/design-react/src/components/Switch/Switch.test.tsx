import { render, screen, fireEvent } from '@testing-library/react';
import { Switch } from './Switch';

describe('Switch', () => {
  describe('Rendering', () => {
    it('renders switch element', () => {
      render(<Switch />);
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('renders with label on right by default', () => {
      render(<Switch label="Enable notifications" />);
      expect(screen.getByText('Enable notifications')).toBeInTheDocument();
    });

    it('renders with label on left when labelPosition is left', () => {
      render(<Switch label="Enable" labelPosition="left" />);
      const label = screen.getByText('Enable');
      expect(label).toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('renders %s size', (size) => {
      render(<Switch size={size} label={size} />);
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });
  });

  describe('Active Colors', () => {
    it.each(['primary', 'success', 'warning', 'error'] as const)(
      'renders %s activeColor',
      (color) => {
        render(<Switch activeColor={color} checked onChange={() => {}} />);
        expect(screen.getByRole('switch')).toBeInTheDocument();
      }
    );
  });

  describe('States', () => {
    it('handles checked state', () => {
      render(<Switch checked onChange={() => {}} />);
      expect(screen.getByRole('switch')).toBeChecked();
    });

    it('handles unchecked state', () => {
      render(<Switch checked={false} />);
      expect(screen.getByRole('switch')).not.toBeChecked();
    });

    it('handles disabled state', () => {
      render(<Switch disabled />);
      expect(screen.getByRole('switch')).toBeDisabled();
    });

    it('applies disabled styling to label', () => {
      render(<Switch disabled label="Disabled Switch" />);
      expect(screen.getByText('Disabled Switch').closest('label')).toHaveClass(
        'cursor-not-allowed'
      );
    });
  });

  describe('Interactions', () => {
    it('calls onChange when clicked', () => {
      const handleChange = vi.fn();
      render(<Switch onChange={handleChange} />);
      fireEvent.click(screen.getByRole('switch'));
      expect(handleChange).toHaveBeenCalled();
    });

    it('does not call onChange when disabled', () => {
      const handleChange = vi.fn();
      render(<Switch disabled onChange={handleChange} />);
      fireEvent.click(screen.getByRole('switch'));
      expect(handleChange).not.toHaveBeenCalled();
    });

    it('can be toggled by clicking label', () => {
      const handleChange = vi.fn();
      render(<Switch label="Toggle me" onChange={handleChange} />);
      fireEvent.click(screen.getByText('Toggle me'));
      expect(handleChange).toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    it('has role="switch"', () => {
      render(<Switch />);
      expect(screen.getByRole('switch')).toBeInTheDocument();
    });

    it('is keyboard accessible', () => {
      const handleChange = vi.fn();
      render(<Switch onChange={handleChange} />);
      const switchElement = screen.getByRole('switch');
      switchElement.focus();
      expect(switchElement).toHaveFocus();
    });
  });

  describe('Custom className', () => {
    it('accepts custom className', () => {
      render(<Switch className="custom-class" />);
      expect(screen.getByRole('switch').closest('label')).toHaveClass('custom-class');
    });
  });

  describe('Ref Forwarding', () => {
    it('forwards ref to input element', () => {
      const ref = { current: null } as React.RefObject<HTMLInputElement>;
      render(<Switch ref={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
    });
  });
});
