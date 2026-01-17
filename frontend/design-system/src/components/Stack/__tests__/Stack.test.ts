import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Stack from '../Stack.vue';

describe('Stack', () => {
  it('renders slot content', () => {
    const wrapper = mount(Stack, {
      slots: {
        default: '<div>Child 1</div><div>Child 2</div>',
      },
    });
    expect(wrapper.text()).toContain('Child 1');
    expect(wrapper.text()).toContain('Child 2');
  });

  it('renders as div by default', () => {
    const wrapper = mount(Stack);
    expect(wrapper.element.tagName).toBe('DIV');
  });

  describe('as prop', () => {
    it.each(['div', 'section', 'article', 'ul', 'ol', 'nav'] as const)('renders as %s', (as) => {
      const wrapper = mount(Stack, {
        props: { as },
      });
      expect(wrapper.element.tagName).toBe(as.toUpperCase());
    });
  });

  describe('direction', () => {
    it('defaults to vertical (column)', () => {
      const wrapper = mount(Stack);
      expect(wrapper.classes()).toContain('flex-col');
    });

    it('applies horizontal (row) direction', () => {
      const wrapper = mount(Stack, {
        props: { direction: 'horizontal' },
      });
      expect(wrapper.classes()).toContain('flex-row');
    });
  });

  describe('gap', () => {
    it.each(['none', 'xs', 'sm', 'md', 'lg', 'xl'] as const)('applies %s gap class', (gap) => {
      const wrapper = mount(Stack, {
        props: { gap },
      });
      const gapMap = {
        none: 'gap-0',
        xs: 'gap-1',
        sm: 'gap-2',
        md: 'gap-4',
        lg: 'gap-6',
        xl: 'gap-8',
      };
      expect(wrapper.classes()).toContain(gapMap[gap]);
    });

    it('defaults to md gap', () => {
      const wrapper = mount(Stack);
      expect(wrapper.classes()).toContain('gap-4');
    });
  });

  describe('align', () => {
    it.each(['start', 'center', 'end', 'stretch'] as const)('applies %s align class', (align) => {
      const wrapper = mount(Stack, {
        props: { align },
      });
      const alignMap = {
        start: 'items-start',
        center: 'items-center',
        end: 'items-end',
        stretch: 'items-stretch',
      };
      expect(wrapper.classes()).toContain(alignMap[align]);
    });
  });

  describe('justify', () => {
    it.each(['start', 'center', 'end', 'between', 'around'] as const)('applies %s justify class', (justify) => {
      const wrapper = mount(Stack, {
        props: { justify },
      });
      const justifyMap = {
        start: 'justify-start',
        center: 'justify-center',
        end: 'justify-end',
        between: 'justify-between',
        around: 'justify-around',
      };
      expect(wrapper.classes()).toContain(justifyMap[justify]);
    });
  });

  describe('wrap', () => {
    it('does not wrap by default', () => {
      const wrapper = mount(Stack);
      expect(wrapper.classes()).not.toContain('flex-wrap');
    });

    it('applies wrap when true', () => {
      const wrapper = mount(Stack, {
        props: { wrap: true },
      });
      expect(wrapper.classes()).toContain('flex-wrap');
    });
  });

  describe('flex container', () => {
    it('has flex display', () => {
      const wrapper = mount(Stack);
      expect(wrapper.classes()).toContain('flex');
    });
  });
});
