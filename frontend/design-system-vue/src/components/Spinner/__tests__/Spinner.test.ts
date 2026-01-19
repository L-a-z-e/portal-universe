import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Spinner from '../Spinner.vue';

describe('Spinner', () => {
  it('renders with default props', () => {
    const wrapper = mount(Spinner);
    expect(wrapper.find('[role="status"]').exists()).toBe(true);
  });

  it('has screen reader text', () => {
    const wrapper = mount(Spinner);
    expect(wrapper.find('.sr-only').text()).toBe('Loading');
  });

  describe('sizes', () => {
    it.each(['xs', 'sm', 'md', 'lg', 'xl'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Spinner, {
        props: { size },
      });
      const sizeMap = { xs: 'w-3', sm: 'w-4', md: 'w-6', lg: 'w-8', xl: 'w-12' };
      expect(wrapper.find('[role="status"]').classes()).toContain(sizeMap[size]);
    });
  });

  describe('colors', () => {
    it('applies primary color by default', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').classes()).toContain('border-brand-600');
    });

    it('applies primary color class', () => {
      const wrapper = mount(Spinner, {
        props: { color: 'primary' },
      });
      expect(wrapper.find('[role="status"]').classes()).toContain('border-brand-600');
    });

    it('applies current color class', () => {
      const wrapper = mount(Spinner, {
        props: { color: 'current' },
      });
      expect(wrapper.find('[role="status"]').classes()).toContain('border-current');
    });

    it('applies white color class', () => {
      const wrapper = mount(Spinner, {
        props: { color: 'white' },
      });
      expect(wrapper.find('[role="status"]').classes()).toContain('border-white');
    });
  });

  describe('label', () => {
    it('uses default label "Loading"', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').attributes('aria-label')).toBe('Loading');
    });

    it('uses custom label', () => {
      const wrapper = mount(Spinner, {
        props: { label: 'Please wait' },
      });
      expect(wrapper.find('[role="status"]').attributes('aria-label')).toBe('Please wait');
      expect(wrapper.find('.sr-only').text()).toBe('Please wait');
    });
  });

  describe('accessibility', () => {
    it('has role="status"', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').exists()).toBe(true);
    });

    it('has aria-label', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').attributes('aria-label')).toBeDefined();
    });

    it('has visually hidden label', () => {
      const wrapper = mount(Spinner);
      const srOnly = wrapper.find('.sr-only');
      expect(srOnly.exists()).toBe(true);
    });
  });

  describe('animation', () => {
    it('has animate-spin class', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').classes()).toContain('animate-spin');
    });

    it('is a rounded element', () => {
      const wrapper = mount(Spinner);
      expect(wrapper.find('[role="status"]').classes()).toContain('rounded-full');
    });
  });
});
