import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Breadcrumb from '../Breadcrumb.vue';

const defaultItems = [
  { label: 'Home', href: '/' },
  { label: 'Products', href: '/products' },
  { label: 'Electronics', href: '/products/electronics' },
  { label: 'Smartphones' },
];

describe('Breadcrumb', () => {
  it('renders all items', () => {
    const wrapper = mount(Breadcrumb, {
      props: { items: defaultItems },
    });
    expect(wrapper.text()).toContain('Home');
    expect(wrapper.text()).toContain('Products');
    expect(wrapper.text()).toContain('Electronics');
    expect(wrapper.text()).toContain('Smartphones');
  });

  it('renders links for items with href', () => {
    const wrapper = mount(Breadcrumb, {
      props: { items: defaultItems },
    });
    const links = wrapper.findAll('a');
    expect(links).toHaveLength(3);
    expect(links[0].attributes('href')).toBe('/');
    expect(links[1].attributes('href')).toBe('/products');
    expect(links[2].attributes('href')).toBe('/products/electronics');
  });

  it('renders last item as text (not link)', () => {
    const wrapper = mount(Breadcrumb, {
      props: { items: defaultItems },
    });
    const lastItem = wrapper.find('[aria-current="page"]');
    expect(lastItem.exists()).toBe(true);
    expect(lastItem.text()).toBe('Smartphones');
  });

  describe('separator', () => {
    it('uses "/" as default separator', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      expect(wrapper.text()).toContain('/');
    });

    it('uses custom separator', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems, separator: '>' },
      });
      expect(wrapper.text()).toContain('>');
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems, size },
      });
      const sizeMap = { sm: 'text-sm', md: 'text-base', lg: 'text-lg' };
      expect(wrapper.classes()).toContain(sizeMap[size]);
    });

    it('defaults to md size', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      expect(wrapper.classes()).toContain('text-base');
    });
  });

  describe('maxItems', () => {
    it('shows all items when maxItems is not set', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      expect(wrapper.findAll('li').length).toBe(4);
    });

    it('collapses middle items when maxItems is set', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems, maxItems: 2 },
      });
      // Should show first item, ellipsis, and last item
      expect(wrapper.text()).toContain('Home');
      expect(wrapper.text()).toContain('...');
      expect(wrapper.text()).toContain('Smartphones');
    });
  });

  describe('accessibility', () => {
    it('has nav element with aria-label', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      expect(wrapper.find('nav').attributes('aria-label')).toBe('Breadcrumb');
    });

    it('has ordered list', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      expect(wrapper.find('ol').exists()).toBe(true);
    });

    it('marks current page with aria-current', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      const currentItem = wrapper.find('[aria-current="page"]');
      expect(currentItem.exists()).toBe(true);
      expect(currentItem.text()).toBe('Smartphones');
    });

    it('separator has aria-hidden', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
      });
      const separators = wrapper.findAll('[aria-hidden="true"]');
      expect(separators.length).toBeGreaterThan(0);
    });
  });

  describe('slots', () => {
    it('renders custom separator via slot', () => {
      const wrapper = mount(Breadcrumb, {
        props: { items: defaultItems },
        slots: {
          separator: '<span data-testid="custom-sep">→</span>',
        },
      });
      expect(wrapper.findAll('[data-testid="custom-sep"]').length).toBeGreaterThan(0);
    });
  });
});
