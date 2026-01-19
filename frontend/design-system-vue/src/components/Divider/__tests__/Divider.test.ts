import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Divider from '../Divider.vue';

describe('Divider', () => {
  it('renders with default props', () => {
    const wrapper = mount(Divider);
    expect(wrapper.find('div').exists()).toBe(true);
  });

  describe('orientation', () => {
    it('renders horizontal by default (border-t)', () => {
      const wrapper = mount(Divider);
      expect(wrapper.classes()).toContain('border-t');
    });

    it('renders vertical orientation (border-l)', () => {
      const wrapper = mount(Divider, {
        props: { orientation: 'vertical' },
      });
      expect(wrapper.classes()).toContain('border-l');
      expect(wrapper.classes()).toContain('self-stretch');
    });
  });

  describe('variant', () => {
    it('applies solid variant by default', () => {
      const wrapper = mount(Divider);
      expect(wrapper.classes()).toContain('border-solid');
    });

    it('applies dashed variant', () => {
      const wrapper = mount(Divider, {
        props: { variant: 'dashed' },
      });
      expect(wrapper.classes()).toContain('border-dashed');
    });

    it('applies dotted variant', () => {
      const wrapper = mount(Divider, {
        props: { variant: 'dotted' },
      });
      expect(wrapper.classes()).toContain('border-dotted');
    });
  });

  describe('color', () => {
    it('applies default color', () => {
      const wrapper = mount(Divider);
      expect(wrapper.classes()).toContain('border-border-default');
    });

    it('applies muted color', () => {
      const wrapper = mount(Divider, {
        props: { color: 'muted' },
      });
      expect(wrapper.classes()).toContain('border-border-muted');
    });

    it('applies strong color', () => {
      const wrapper = mount(Divider, {
        props: { color: 'strong' },
      });
      expect(wrapper.classes()).toContain('border-gray-400');
    });
  });

  describe('spacing', () => {
    it('applies sm horizontal spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'sm', orientation: 'horizontal' },
      });
      expect(wrapper.classes()).toContain('my-2');
    });

    it('applies md horizontal spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'md', orientation: 'horizontal' },
      });
      expect(wrapper.classes()).toContain('my-4');
    });

    it('applies lg horizontal spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'lg', orientation: 'horizontal' },
      });
      expect(wrapper.classes()).toContain('my-6');
    });

    it('applies sm vertical spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'sm', orientation: 'vertical' },
      });
      expect(wrapper.classes()).toContain('mx-2');
    });

    it('applies md vertical spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'md', orientation: 'vertical' },
      });
      expect(wrapper.classes()).toContain('mx-4');
    });

    it('applies lg vertical spacing', () => {
      const wrapper = mount(Divider, {
        props: { spacing: 'lg', orientation: 'vertical' },
      });
      expect(wrapper.classes()).toContain('mx-6');
    });

    it('defaults to md spacing', () => {
      const wrapper = mount(Divider);
      expect(wrapper.classes()).toContain('my-4');
    });
  });

  describe('label', () => {
    it('renders label when provided', () => {
      const wrapper = mount(Divider, {
        props: { label: 'OR' },
      });
      expect(wrapper.text()).toContain('OR');
    });

    it('does not render label container when no label', () => {
      const wrapper = mount(Divider);
      expect(wrapper.find('span.text-text-muted').exists()).toBe(false);
    });

    it('renders as flex container when labeled', () => {
      const wrapper = mount(Divider, {
        props: { label: 'Separator' },
      });
      expect(wrapper.classes()).toContain('flex');
      expect(wrapper.classes()).toContain('items-center');
    });
  });

  describe('accessibility', () => {
    it('has role="separator" when label is present', () => {
      const wrapper = mount(Divider, {
        props: { label: 'Section' },
      });
      expect(wrapper.attributes('role')).toBe('separator');
    });

    it('has aria-orientation for horizontal', () => {
      const wrapper = mount(Divider, {
        props: { orientation: 'horizontal' },
      });
      expect(wrapper.attributes('aria-orientation')).toBe('horizontal');
    });

    it('has aria-orientation for vertical', () => {
      const wrapper = mount(Divider, {
        props: { orientation: 'vertical' },
      });
      expect(wrapper.attributes('aria-orientation')).toBe('vertical');
    });
  });
});
