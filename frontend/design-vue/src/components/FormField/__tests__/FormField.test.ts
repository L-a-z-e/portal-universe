import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import FormField from '../FormField.vue';

describe('FormField', () => {
  it('renders slot content', () => {
    const wrapper = mount(FormField, {
      slots: {
        default: '<input type="text" />',
      },
    });
    expect(wrapper.find('input').exists()).toBe(true);
  });

  describe('label', () => {
    it('renders label when provided', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Email' },
      });
      expect(wrapper.find('label').text()).toContain('Email');
    });

    it('does not render label when not provided', () => {
      const wrapper = mount(FormField);
      expect(wrapper.find('label').exists()).toBe(false);
    });

    it('associates label with input via id', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Email', id: 'email-input' },
      });
      expect(wrapper.find('label').attributes('for')).toBe('email-input');
    });
  });

  describe('required', () => {
    it('shows asterisk when required', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Email', required: true },
      });
      expect(wrapper.text()).toContain('*');
    });

    it('does not show asterisk by default', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Email' },
      });
      expect(wrapper.find('span.text-status-error').exists()).toBe(false);
    });
  });

  describe('helperText', () => {
    it('shows helper text when provided', () => {
      const wrapper = mount(FormField, {
        props: { helperText: 'Enter a valid email' },
      });
      expect(wrapper.text()).toContain('Enter a valid email');
    });

    it('applies helper text styling', () => {
      const wrapper = mount(FormField, {
        props: { helperText: 'Help text' },
      });
      expect(wrapper.find('.text-text-muted').exists()).toBe(true);
    });
  });

  describe('error state', () => {
    it('shows error message when error is true', () => {
      const wrapper = mount(FormField, {
        props: { error: true, errorMessage: 'Invalid email' },
      });
      expect(wrapper.text()).toContain('Invalid email');
    });

    it('applies error styling', () => {
      const wrapper = mount(FormField, {
        props: { error: true, errorMessage: 'Error' },
      });
      expect(wrapper.find('.text-status-error').exists()).toBe(true);
    });

    it('hides helper text when error message is shown', () => {
      const wrapper = mount(FormField, {
        props: {
          helperText: 'Help text',
          error: true,
          errorMessage: 'Error'
        },
      });
      expect(wrapper.text()).not.toContain('Help text');
      expect(wrapper.text()).toContain('Error');
    });
  });

  describe('disabled', () => {
    it('applies disabled opacity', () => {
      const wrapper = mount(FormField, {
        props: { disabled: true },
      });
      expect(wrapper.classes()).toContain('opacity-60');
    });
  });

  describe('slots', () => {
    it('renders label slot', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Field' }, // Need label prop to render label element
        slots: {
          label: '<span data-testid="custom-label">Custom Label</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom-label"]').exists()).toBe(true);
    });

    it('renders helper slot when helperText is provided', () => {
      const wrapper = mount(FormField, {
        props: { helperText: 'some text' },
        slots: {
          helper: '<span data-testid="custom-helper">Custom Helper</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom-helper"]').exists()).toBe(true);
    });

    it('renders error slot when error is true with message', () => {
      const wrapper = mount(FormField, {
        props: { error: true, errorMessage: 'some error' },
        slots: {
          error: '<span data-testid="custom-error">Custom Error</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom-error"]').exists()).toBe(true);
    });
  });

  describe('id generation', () => {
    it('uses provided id', () => {
      const wrapper = mount(FormField, {
        props: { id: 'my-field', label: 'Field' },
      });
      expect(wrapper.find('label').attributes('for')).toBe('my-field');
    });

    it('generates unique id when not provided', () => {
      const wrapper = mount(FormField, {
        props: { label: 'Field' },
      });
      const labelFor = wrapper.find('label').attributes('for');
      expect(labelFor).toMatch(/^form-field-/);
    });
  });

  describe('accessibility', () => {
    it('links error message with aria-describedby', () => {
      const wrapper = mount(FormField, {
        props: {
          id: 'test-field',
          error: true,
          errorMessage: 'Error text'
        },
        slots: {
          default: '<input id="test-field" />',
        },
      });
      const errorP = wrapper.find('p');
      expect(errorP.attributes('id')).toBe('test-field-error');
    });
  });
});
