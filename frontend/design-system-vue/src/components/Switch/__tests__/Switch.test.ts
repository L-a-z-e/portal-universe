import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Switch from '../Switch.vue';

describe('Switch', () => {
  it('renders with default props', () => {
    const wrapper = mount(Switch);
    expect(wrapper.find('[role="switch"]').exists()).toBe(true);
  });

  it('renders label when provided', () => {
    const wrapper = mount(Switch, {
      props: { label: 'Enable feature' },
    });
    expect(wrapper.text()).toContain('Enable feature');
  });

  describe('v-model behavior', () => {
    it('reflects checked state', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: true },
      });
      expect(wrapper.find('[role="switch"]').attributes('aria-checked')).toBe('true');
    });

    it('emits update:modelValue on click', async () => {
      const wrapper = mount(Switch, {
        props: { modelValue: false },
      });
      await wrapper.find('[role="switch"]').trigger('click');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([true]);
    });

    it('emits change event on click', async () => {
      const wrapper = mount(Switch, {
        props: { modelValue: false },
      });
      await wrapper.find('[role="switch"]').trigger('click');
      expect(wrapper.emitted('change')?.[0]).toEqual([true]);
    });

    it('toggles from true to false', async () => {
      const wrapper = mount(Switch, {
        props: { modelValue: true },
      });
      await wrapper.find('[role="switch"]').trigger('click');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false]);
    });
  });

  describe('disabled state', () => {
    it('has disabled attribute when disabled', () => {
      const wrapper = mount(Switch, {
        props: { disabled: true },
      });
      expect(wrapper.find('[role="switch"]').attributes('disabled')).toBeDefined();
    });

    it('applies disabled styling', () => {
      const wrapper = mount(Switch, {
        props: { disabled: true },
      });
      expect(wrapper.find('label').classes()).toContain('cursor-not-allowed');
    });

    it('does not emit events when disabled', async () => {
      const wrapper = mount(Switch, {
        props: { disabled: true, modelValue: false },
      });
      await wrapper.find('[role="switch"]').trigger('click');
      expect(wrapper.emitted('update:modelValue')).toBeFalsy();
    });
  });

  describe('labelPosition', () => {
    it('places label on right by default', () => {
      const wrapper = mount(Switch, {
        props: { label: 'Test' },
      });
      expect(wrapper.find('label').classes()).toContain('flex-row');
    });

    it('places label on left when specified', () => {
      const wrapper = mount(Switch, {
        props: { label: 'Test', labelPosition: 'left' },
      });
      expect(wrapper.find('label').classes()).toContain('flex-row-reverse');
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Switch, {
        props: { size },
      });
      const sizeMap = { sm: 'w-8', md: 'w-11', lg: 'w-14' };
      expect(wrapper.find('[role="switch"]').classes()).toContain(sizeMap[size]);
    });
  });

  describe('activeColor', () => {
    it('applies primary color by default when active', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: true },
      });
      expect(wrapper.find('[role="switch"]').classes()).toContain('bg-brand-600');
    });

    it.each(['primary', 'success', 'warning', 'error'] as const)('applies %s color when active', (color) => {
      const wrapper = mount(Switch, {
        props: { modelValue: true, activeColor: color },
      });

      const colorMap = {
        primary: 'bg-brand-600',
        success: 'bg-status-success',
        warning: 'bg-status-warning',
        error: 'bg-status-error',
      };

      expect(wrapper.find('[role="switch"]').classes()).toContain(colorMap[color]);
    });

    it('applies inactive color when not checked', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: false },
      });
      expect(wrapper.find('[role="switch"]').classes()).toContain('bg-gray-300');
    });
  });

  describe('thumb position', () => {
    it('positions thumb left when off', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: false },
      });
      expect(wrapper.find('span.absolute').classes()).toContain('translate-x-0');
    });

    it('positions thumb right when on', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: true, size: 'md' },
      });
      expect(wrapper.find('span.absolute').classes()).toContain('translate-x-5');
    });
  });

  describe('accessibility', () => {
    it('has role="switch"', () => {
      const wrapper = mount(Switch);
      expect(wrapper.find('[role="switch"]').exists()).toBe(true);
    });

    it('has aria-checked attribute', () => {
      const wrapper = mount(Switch, {
        props: { modelValue: true },
      });
      expect(wrapper.find('[role="switch"]').attributes('aria-checked')).toBe('true');
    });

    it('associates label with switch via id', () => {
      const wrapper = mount(Switch, {
        props: { label: 'Test' },
      });
      const switchEl = wrapper.find('[role="switch"]');
      const label = wrapper.find('label');
      expect(label.attributes('for')).toBe(switchEl.attributes('id'));
    });

    it('has focus ring classes', () => {
      const wrapper = mount(Switch);
      const switchEl = wrapper.find('[role="switch"]');
      expect(switchEl.classes()).toContain('focus:outline-none');
      expect(switchEl.classes()).toContain('focus:ring-2');
    });
  });

  describe('slot', () => {
    it('renders slot content', () => {
      const wrapper = mount(Switch, {
        slots: {
          default: '<span data-testid="custom">Custom content</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom"]').exists()).toBe(true);
    });
  });
});
