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
    expect(wrapper.classes()).toContain('bg-brand-600');
    expect(wrapper.classes()).toContain('px-6');
  });

  describe('variants', () => {
    it('applies primary variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'primary' },
      });
      expect(wrapper.classes()).toContain('bg-brand-600');
      expect(wrapper.classes()).toContain('text-white');
    });

    it('applies secondary variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'secondary' },
      });
      expect(wrapper.classes()).toContain('bg-gray-100');
      expect(wrapper.classes()).toContain('text-gray-900');
    });

    it('applies outline variant classes', () => {
      const wrapper = mount(Button, {
        props: { variant: 'outline' },
      });
      expect(wrapper.classes()).toContain('bg-transparent');
      expect(wrapper.classes()).toContain('border-white');
    });
  });

  describe('sizes', () => {
    it('applies sm size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'sm' },
      });
      expect(wrapper.classes()).toContain('px-4');
      expect(wrapper.classes()).toContain('py-2');
      expect(wrapper.classes()).toContain('text-sm');
    });

    it('applies md size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'md' },
      });
      expect(wrapper.classes()).toContain('px-6');
      expect(wrapper.classes()).toContain('py-3');
      expect(wrapper.classes()).toContain('text-base');
    });

    it('applies lg size classes', () => {
      const wrapper = mount(Button, {
        props: { size: 'lg' },
      });
      expect(wrapper.classes()).toContain('px-8');
      expect(wrapper.classes()).toContain('py-4');
      expect(wrapper.classes()).toContain('text-lg');
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
      expect(wrapper.classes()).toContain('focus:ring-2');
    });
  });
});
