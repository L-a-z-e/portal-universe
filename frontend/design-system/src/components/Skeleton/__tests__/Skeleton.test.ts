import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Skeleton from '../Skeleton.vue';

describe('Skeleton', () => {
  it('renders with default props', () => {
    const wrapper = mount(Skeleton);
    expect(wrapper.find('div').exists()).toBe(true);
  });

  describe('variant', () => {
    it('applies text variant by default', () => {
      const wrapper = mount(Skeleton);
      // Text variant has h-4 and rounded classes on inner div
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('h-4');
      expect(innerDiv.classes()).toContain('rounded');
    });

    it('applies circular variant', () => {
      const wrapper = mount(Skeleton, {
        props: { variant: 'circular' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('rounded-full');
    });

    it('applies rectangular variant', () => {
      const wrapper = mount(Skeleton, {
        props: { variant: 'rectangular' },
      });
      // Rectangular has no rounded class (empty string in variantClasses)
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).not.toContain('rounded-lg');
      expect(innerDiv.classes()).not.toContain('rounded-full');
    });

    it('applies rounded variant', () => {
      const wrapper = mount(Skeleton, {
        props: { variant: 'rounded' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('rounded-lg');
    });
  });

  describe('animation', () => {
    it('applies pulse animation by default', () => {
      const wrapper = mount(Skeleton);
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('animate-pulse');
    });

    it('applies wave animation', () => {
      const wrapper = mount(Skeleton, {
        props: { animation: 'wave' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('skeleton-wave');
    });

    it('applies no animation', () => {
      const wrapper = mount(Skeleton, {
        props: { animation: 'none' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).not.toContain('animate-pulse');
      expect(innerDiv.classes()).not.toContain('skeleton-wave');
    });
  });

  describe('dimensions', () => {
    it('applies custom width', () => {
      const wrapper = mount(Skeleton, {
        props: { width: '200px' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.attributes('style')).toContain('width: 200px');
    });

    it('applies custom height', () => {
      const wrapper = mount(Skeleton, {
        props: { height: '100px' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.attributes('style')).toContain('height: 100px');
    });

    it('applies percentage width', () => {
      const wrapper = mount(Skeleton, {
        props: { width: '50%' },
      });
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.attributes('style')).toContain('width: 50%');
    });
  });

  describe('lines', () => {
    it('renders single line by default', () => {
      const wrapper = mount(Skeleton);
      expect(wrapper.findAll('.bg-gray-200').length).toBe(1);
    });

    it('renders multiple lines', () => {
      const wrapper = mount(Skeleton, {
        props: { lines: 3 },
      });
      expect(wrapper.findAll('.bg-gray-200').length).toBe(3);
    });

    it('last line has dynamic width for text variant', () => {
      const wrapper = mount(Skeleton, {
        props: { lines: 3, variant: 'text' },
      });
      const lines = wrapper.findAll('.bg-gray-200');
      // Last line should have a width style (60-80%)
      const lastLineStyle = lines[2].attributes('style');
      expect(lastLineStyle).toContain('width:');
    });
  });

  describe('styling', () => {
    it('has background color on inner element', () => {
      const wrapper = mount(Skeleton);
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.exists()).toBe(true);
    });

    it('inner element has dark mode background', () => {
      const wrapper = mount(Skeleton);
      const innerDiv = wrapper.find('.bg-gray-200');
      expect(innerDiv.classes()).toContain('dark:bg-gray-700');
    });
  });

  describe('accessibility', () => {
    it('has aria-busy for loading state', () => {
      const wrapper = mount(Skeleton);
      expect(wrapper.attributes('aria-busy')).toBe('true');
    });

    it('has aria-label for loading', () => {
      const wrapper = mount(Skeleton);
      expect(wrapper.attributes('aria-label')).toBe('Loading');
    });
  });
});
