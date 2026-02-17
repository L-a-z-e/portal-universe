import { render, screen, fireEvent } from '@testing-library/react';
import { Checkbox } from './Checkbox';

describe('Checkbox', () => {
  describe('Rendering', () => {
    it('renders checkbox element', () => {
      render(<Checkbox />);
      expect(screen.getByRole('checkbox')).toBeInTheDocument();
    });

    it('renders with label', () => {
      render(<Checkbox label="Accept terms" />);
      expect(screen.getByText('Accept terms')).toBeInTheDocument();
      expect(screen.getByLabelText('Accept terms')).toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('renders %s size', (size) => {
      render(<Checkbox size={size} label={size} />);
      expect(screen.getByRole('checkbox')).toBeInTheDocument();
    });
  });

  describe('States', () => {
    it('handles checked state', () => {
      render(<Checkbox checked onChange={() => {}} />);
      expect(screen.getByRole('checkbox')).toBeChecked();
    });

    it('handles unchecked state', () => {
      render(<Checkbox checked={false} />);
      expect(screen.getByRole('checkbox')).not.toBeChecked();
    });

    it('handles disabled state', () => {
      render(<Checkbox disabled />);
      expect(screen.getByRole('checkbox')).toBeDisabled();
    });

    it('handles error state', () => {
      render(<Checkbox error errorMessage="Required field" />);
      expect(screen.getByRole('checkbox')).toHaveAttribute('aria-invalid', 'true');
      expect(screen.getByText('Required field')).toBeInTheDocument();
    });

    it('renders indeterminate state visually', () => {
      render(<Checkbox indeterminate checked onChange={() => {}} />);
      expect(screen.getByRole('checkbox')).toBeInTheDocument();
    });
  });

  describe('Interactions', () => {
    it('calls onChange when clicked', () => {
      const handleChange = vi.fn();
      render(<Checkbox onChange={handleChange} />);
      fireEvent.click(screen.getByRole('checkbox'));
      expect(handleChange).toHaveBeenCalled();
    });

    it('toggles checked state on click', () => {
      const handleChange = vi.fn();
      render(<Checkbox checked={false} onChange={handleChange} />);
      fireEvent.click(screen.getByRole('checkbox'));
      expect(handleChange).toHaveBeenCalled();
    });

    it('does not call onChange when disabled', () => {
      const handleChange = vi.fn();
      render(<Checkbox disabled onChange={handleChange} />);
      fireEvent.click(screen.getByRole('checkbox'));
      expect(handleChange).not.toHaveBeenCalled();
    });

    it('can be toggled by clicking label', () => {
      const handleChange = vi.fn();
      render(<Checkbox label="Click me" onChange={handleChange} />);
      fireEvent.click(screen.getByText('Click me'));
      expect(handleChange).toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    it('has proper aria-invalid when error', () => {
      render(<Checkbox error />);
      expect(screen.getByRole('checkbox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('has proper aria-describedby for error message', () => {
      render(<Checkbox id="test-cb" error errorMessage="Error" />);
      expect(screen.getByRole('checkbox')).toHaveAttribute(
        'aria-describedby',
        'test-cb-error'
      );
    });

    it('is keyboard accessible', () => {
      const handleChange = vi.fn();
      render(<Checkbox onChange={handleChange} />);
      const checkbox = screen.getByRole('checkbox');
      checkbox.focus();
      fireEvent.keyDown(checkbox, { key: ' ' });
      fireEvent.keyUp(checkbox, { key: ' ' });
    });
  });

  describe('Custom className', () => {
    it('accepts custom className', () => {
      const { container } = render(<Checkbox className="custom-class" />);
      expect(container.firstChild).toHaveClass('custom-class');
    });
  });

  describe('Ref Forwarding', () => {
    it('forwards ref to input element', () => {
      const ref = { current: null } as React.RefObject<HTMLInputElement>;
      render(<Checkbox ref={ref} />);
      expect(ref.current).toBeInstanceOf(HTMLInputElement);
    });
  });

  describe('Value', () => {
    it('accepts value prop', () => {
      render(<Checkbox value="option1" />);
      expect(screen.getByRole('checkbox')).toHaveAttribute('value', 'option1');
    });
  });
});
