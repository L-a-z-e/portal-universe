import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Container from '../Container.vue';

describe('Container', () => {
  it('renders slot content', () => {
    const wrapper = mount(Container, {
      slots: {
        default: '<p>Content</p>',
      },
    });
    expect(wrapper.text()).toContain('Content');
  });

  it('renders as div by default', () => {
    const wrapper = mount(Container);
    expect(wrapper.element.tagName).toBe('DIV');
  });

  describe('as prop', () => {
    it('renders as custom element', () => {
      const wrapper = mount(Container, {
        props: { as: 'main' },
      });
      expect(wrapper.element.tagName).toBe('MAIN');
    });

    it('renders as section', () => {
      const wrapper = mount(Container, {
        props: { as: 'section' },
      });
      expect(wrapper.element.tagName).toBe('SECTION');
    });

    it('renders as article', () => {
      const wrapper = mount(Container, {
        props: { as: 'article' },
      });
      expect(wrapper.element.tagName).toBe('ARTICLE');
    });
  });

  describe('maxWidth', () => {
    it.each(['sm', 'md', 'lg', 'xl', '2xl'] as const)('applies %s max-width class', (maxWidth) => {
      const wrapper = mount(Container, {
        props: { maxWidth },
      });
      const widthMap = {
        sm: 'max-w-screen-sm',
        md: 'max-w-screen-md',
        lg: 'max-w-screen-lg',
        xl: 'max-w-screen-xl',
        '2xl': 'max-w-screen-2xl',
      };
      expect(wrapper.classes()).toContain(widthMap[maxWidth]);
    });

    it('applies full width class', () => {
      const wrapper = mount(Container, {
        props: { maxWidth: 'full' },
      });
      expect(wrapper.classes()).toContain('max-w-full');
    });

    it('defaults to lg max-width', () => {
      const wrapper = mount(Container);
      expect(wrapper.classes()).toContain('max-w-screen-lg');
    });
  });

  describe('padding', () => {
    it.each(['none', 'sm', 'md', 'lg'] as const)('applies %s padding class', (padding) => {
      const wrapper = mount(Container, {
        props: { padding },
      });
      const paddingMap = {
        none: 'px-0',
        sm: 'px-4',
        md: 'px-6',
        lg: 'px-8',
      };
      expect(wrapper.classes()).toContain(paddingMap[padding]);
    });

    it('defaults to md padding', () => {
      const wrapper = mount(Container);
      expect(wrapper.classes()).toContain('px-6');
    });
  });

  describe('centered', () => {
    it('centers by default', () => {
      const wrapper = mount(Container);
      expect(wrapper.classes()).toContain('mx-auto');
    });

    it('does not center when centered is false', () => {
      const wrapper = mount(Container, {
        props: { centered: false },
      });
      expect(wrapper.classes()).not.toContain('mx-auto');
    });
  });

  describe('width', () => {
    it('has full width class', () => {
      const wrapper = mount(Container);
      expect(wrapper.classes()).toContain('w-full');
    });
  });
});
