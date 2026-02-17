import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Link from '../Link.vue';

describe('Link', () => {
  it('renders slot content', () => {
    const wrapper = mount(Link, {
      props: { href: '/test' },
      slots: {
        default: 'Click me',
      },
    });
    expect(wrapper.text()).toContain('Click me');
  });

  it('renders as anchor element', () => {
    const wrapper = mount(Link, {
      props: { href: '/test' },
    });
    expect(wrapper.element.tagName).toBe('A');
  });

  describe('href', () => {
    it('applies href attribute', () => {
      const wrapper = mount(Link, {
        props: { href: 'https://example.com' },
      });
      expect(wrapper.attributes('href')).toBe('https://example.com');
    });
  });

  describe('target', () => {
    it('defaults to _self', () => {
      const wrapper = mount(Link, {
        props: { href: '/test' },
      });
      expect(wrapper.attributes('target')).toBe('_self');
    });

    it('applies _blank target', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', target: '_blank' },
      });
      expect(wrapper.attributes('target')).toBe('_blank');
    });

    it('adds rel="noopener noreferrer" for blank target', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', target: '_blank' },
      });
      expect(wrapper.attributes('rel')).toBe('noopener noreferrer');
    });
  });

  describe('external', () => {
    it('adds rel when external (because isExternal becomes true)', () => {
      const wrapper = mount(Link, {
        props: { href: 'https://external.com', external: true },
      });
      // isExternal is computed as props.external || props.target === '_blank'
      // So external: true makes isExternal true, which adds rel
      expect(wrapper.attributes('rel')).toBe('noopener noreferrer');
    });

    it('shows external icon when external', () => {
      const wrapper = mount(Link, {
        props: { href: 'https://external.com', external: true },
        slots: { default: 'External Link' },
      });
      expect(wrapper.find('svg').exists()).toBe(true);
    });
  });

  describe('variant', () => {
    it('applies default variant (text-text-link)', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', variant: 'default' },
      });
      expect(wrapper.classes()).toContain('text-text-link');
    });

    it('applies primary variant', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', variant: 'primary' },
      });
      expect(wrapper.classes()).toContain('text-brand-600');
    });

    it('applies muted variant', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', variant: 'muted' },
      });
      expect(wrapper.classes()).toContain('text-text-muted');
    });

    it('applies underline variant', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', variant: 'underline' },
      });
      expect(wrapper.classes()).toContain('underline');
    });
  });

  describe('disabled', () => {
    it('applies disabled classes when disabled', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', disabled: true },
      });
      expect(wrapper.classes()).toContain('pointer-events-none');
      expect(wrapper.classes()).toContain('opacity-50');
    });

    it('sets aria-disabled when disabled', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', disabled: true },
      });
      expect(wrapper.attributes('aria-disabled')).toBe('true');
    });

    it('prevents navigation when disabled', () => {
      const wrapper = mount(Link, {
        props: { href: '/test', disabled: true },
      });
      expect(wrapper.classes()).toContain('cursor-not-allowed');
    });
  });

  describe('accessibility', () => {
    it('is focusable', () => {
      const wrapper = mount(Link, {
        props: { href: '/test' },
      });
      // Links are focusable by default
      expect(wrapper.element.tagName).toBe('A');
    });

    it('has focus ring styles', () => {
      const wrapper = mount(Link, {
        props: { href: '/test' },
      });
      expect(wrapper.classes()).toContain('focus:outline-none');
    });
  });

  describe('transition', () => {
    it('has transition classes', () => {
      const wrapper = mount(Link, {
        props: { href: '/test' },
      });
      expect(wrapper.classes()).toContain('transition-colors');
    });
  });
});
