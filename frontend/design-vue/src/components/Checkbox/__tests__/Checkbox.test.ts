import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import Checkbox from '../Checkbox.vue';

describe('Checkbox', () => {
  it('renders with default props', () => {
    const wrapper = mount(Checkbox);
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(true);
  });

  it('renders label text', () => {
    const wrapper = mount(Checkbox, {
      props: { label: 'Accept terms' },
    });
    expect(wrapper.text()).toContain('Accept terms');
  });

  describe('v-model behavior', () => {
    it('reflects checked state', () => {
      const wrapper = mount(Checkbox, {
        props: { modelValue: true },
      });
      expect(wrapper.find('input').element.checked).toBe(true);
    });

    it('emits update:modelValue on change', async () => {
      const wrapper = mount(Checkbox, {
        props: { modelValue: false },
      });
      await wrapper.find('input').setValue(true);
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([true]);
    });

    it('emits change event on change', async () => {
      const wrapper = mount(Checkbox, {
        props: { modelValue: false },
      });
      await wrapper.find('input').setValue(true);
      expect(wrapper.emitted('change')?.[0]).toEqual([true]);
    });
  });

  describe('indeterminate state', () => {
    it('sets aria-checked to mixed when indeterminate', () => {
      const wrapper = mount(Checkbox, {
        props: { indeterminate: true },
      });
      expect(wrapper.find('input').attributes('aria-checked')).toBe('mixed');
    });

    it('shows indeterminate icon', () => {
      const wrapper = mount(Checkbox, {
        props: { indeterminate: true },
      });
      // Check for horizontal line path (indeterminate icon)
      const svg = wrapper.findAll('svg').find(s =>
        s.find('path').attributes('d')?.includes('M5 12h14')
      );
      expect(svg).toBeTruthy();
    });
  });

  describe('disabled state', () => {
    it('has disabled attribute when disabled', () => {
      const wrapper = mount(Checkbox, {
        props: { disabled: true },
      });
      expect(wrapper.find('input').attributes('disabled')).toBeDefined();
    });

    it('applies disabled styling', () => {
      const wrapper = mount(Checkbox, {
        props: { disabled: true },
      });
      expect(wrapper.find('label').classes()).toContain('cursor-not-allowed');
    });

    it('does not emit events when disabled', async () => {
      const wrapper = mount(Checkbox, {
        props: { disabled: true, modelValue: false },
      });
      const input = wrapper.find('input');
      await input.trigger('change');
      expect(wrapper.emitted('update:modelValue')).toBeFalsy();
    });
  });

  describe('error state', () => {
    it('shows error message when error is true', () => {
      const wrapper = mount(Checkbox, {
        props: { error: true, errorMessage: 'Required field' },
      });
      expect(wrapper.text()).toContain('Required field');
    });

    it('sets aria-invalid when error', () => {
      const wrapper = mount(Checkbox, {
        props: { error: true },
      });
      expect(wrapper.find('input').attributes('aria-invalid')).toBe('true');
    });

    it('links error message with aria-describedby', () => {
      const wrapper = mount(Checkbox, {
        props: { error: true, errorMessage: 'Error text' },
      });
      const input = wrapper.find('input');
      const errorP = wrapper.find('p');
      expect(input.attributes('aria-describedby')).toBe(errorP.attributes('id'));
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Checkbox, {
        props: { size, label: 'Test' },
      });
      const sizeMap = { sm: 'text-sm', md: 'text-base', lg: 'text-lg' };
      expect(wrapper.find('span:last-child').classes()).toContain(sizeMap[size]);
    });
  });

  describe('accessibility', () => {
    it('has proper role', () => {
      const wrapper = mount(Checkbox);
      expect(wrapper.find('input').attributes('type')).toBe('checkbox');
    });

    it('associates label with input via id', () => {
      const wrapper = mount(Checkbox, {
        props: { label: 'Test' },
      });
      const input = wrapper.find('input');
      const label = wrapper.find('label');
      expect(label.attributes('for')).toBe(input.attributes('id'));
    });
  });
});
