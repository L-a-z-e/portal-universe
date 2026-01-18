import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Button from '../Button.vue';

describe('Button', () => {
  it('renders slot content', () => {
    const wrapper = mount(Button, {
      slots: {
        default: 'Click me',
      },
    });
    expect(wrapper.text()).toBe('Click me');
  });

  it('applies default variant and size classes', () => {
    const wrapper = mount(Button);
    // Linear-style primary button: light bg on dark
    expect(wrapper.classes()).toContain('bg-[#e6e6e6]');
    expect(wrapper.classes()).toContain('px-4');
  });

  describe('variants', () => {
    it('applies primary variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'primary' },
      });
      // Linear-style: light button bg, dark text
      expect(wrapper.classes()).toContain('bg-[#e6e6e6]');
      expect(wrapper.classes()).toContain('text-[#08090a]');
    });

    it('applies secondary variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'secondary' },
      });
      expect(wrapper.classes()).toContain('bg-transparent');
      expect(wrapper.classes()).toContain('text-text-meta');
    });

    it('applies outline variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'outline' },
      });
      expect(wrapper.classes()).toContain('bg-transparent');
      expect(wrapper.classes()).toContain('border-border-default');
    });

    it('applies ghost variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'ghost' },
      });
      expect(wrapper.classes()).toContain('bg-transparent');
      expect(wrapper.classes()).toContain('border-transparent');
    });

    it('applies danger variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'danger' },
      });
      expect(wrapper.classes()).toContain('bg-status-error');
      expect(wrapper.classes()).toContain('text-white');
    });
  });

  describe('sizes', () => {
    it('applies xs size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'xs' },
      });
      expect(wrapper.classes()).toContain('px-2');
      expect(wrapper.classes()).toContain('py-1');
      expect(wrapper.classes()).toContain('text-xs');
    });

    it('applies sm size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'sm' },
      });
      expect(wrapper.classes()).toContain('px-3');
      expect(wrapper.classes()).toContain('py-1.5');
      expect(wrapper.classes()).toContain('text-sm');
    });

    it('applies md size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'md' },
      });
      expect(wrapper.classes()).toContain('px-4');
      expect(wrapper.classes()).toContain('py-2');
      expect(wrapper.classes()).toContain('text-sm');
    });

    it('applies lg size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'lg' },
      });
      expect(wrapper.classes()).toContain('px-5');
      expect(wrapper.classes()).toContain('py-2.5');
      expect(wrapper.classes()).toContain('text-base');
    });
  });

  describe('disabled state', () => {
    it('applies disabled classes when disabled', () => {
      const wrapper = mount(Button, {
        props: { disabled: true },
      });
      expect(wrapper.classes()).toContain('opacity-50');
      expect(wrapper.classes()).toContain('cursor-not-allowed');
    });

    it('sets disabled attribute', () => {
      const wrapper = mount(Button, {
        props: { disabled: true },
      });
      expect(wrapper.attributes('disabled')).toBeDefined();
    });

    it('emits click event when not disabled', async () => {
      const wrapper = mount(Button);
      await wrapper.trigger('click');
      expect(wrapper.emitted('click')).toBeTruthy();
    });
  });

  describe('accessibility', () => {
    it('is a button element', () => {
      const wrapper = mount(Button);
      expect(wrapper.element.tagName).toBe('BUTTON');
    });

    it('has focus ring classes', () => {
      const wrapper = mount(Button);
      expect(wrapper.classes()).toContain('focus:outline-none');
      expect(wrapper.classes()).toContain('focus-visible:ring-2');
    });
  });
});
