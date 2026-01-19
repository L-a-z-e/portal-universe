import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Tabs from '../Tabs.vue';

const defaultItems = [
  { label: 'Tab 1', value: 'tab1' },
  { label: 'Tab 2', value: 'tab2' },
  { label: 'Tab 3', value: 'tab3', disabled: true },
];

describe('Tabs', () => {
  it('renders all tab items', () => {
    const wrapper = mount(Tabs, {
      props: {
        items: defaultItems,
        modelValue: 'tab1',
      },
    });
    expect(wrapper.findAll('[role="tab"]')).toHaveLength(3);
  });

  it('displays tab labels', () => {
    const wrapper = mount(Tabs, {
      props: {
        items: defaultItems,
        modelValue: 'tab1',
      },
    });
    expect(wrapper.text()).toContain('Tab 1');
    expect(wrapper.text()).toContain('Tab 2');
    expect(wrapper.text()).toContain('Tab 3');
  });

  describe('selection', () => {
    it('marks selected tab as active', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      const tabs = wrapper.findAll('[role="tab"]');
      expect(tabs[0].attributes('aria-selected')).toBe('true');
      expect(tabs[1].attributes('aria-selected')).toBe('false');
    });

    it('emits update:modelValue on tab click', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      await wrapper.findAll('[role="tab"]')[1].trigger('click');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['tab2']);
    });

    it('emits change event on tab click', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      await wrapper.findAll('[role="tab"]')[1].trigger('click');
      expect(wrapper.emitted('change')?.[0]).toEqual(['tab2']);
    });

    it('does not emit events for disabled tabs', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      await wrapper.findAll('[role="tab"]')[2].trigger('click');
      expect(wrapper.emitted('update:modelValue')).toBeFalsy();
    });
  });

  describe('keyboard navigation', () => {
    it('navigates with ArrowRight', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      await wrapper.findAll('[role="tab"]')[0].trigger('keydown', { key: 'ArrowRight' });
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['tab2']);
    });

    it('navigates with ArrowLeft', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab2',
        },
      });
      await wrapper.findAll('[role="tab"]')[1].trigger('keydown', { key: 'ArrowLeft' });
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['tab1']);
    });

    it('navigates to first with Home', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab2',
        },
      });
      await wrapper.findAll('[role="tab"]')[1].trigger('keydown', { key: 'Home' });
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['tab1']);
    });

    it('navigates to last with End', async () => {
      const wrapper = mount(Tabs, {
        props: {
          items: [
            { label: 'Tab 1', value: 'tab1' },
            { label: 'Tab 2', value: 'tab2' },
          ],
          modelValue: 'tab1',
        },
      });
      await wrapper.findAll('[role="tab"]')[0].trigger('keydown', { key: 'End' });
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['tab2']);
    });
  });

  describe('variants', () => {
    it('applies default variant classes', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
          variant: 'default',
        },
      });
      expect(wrapper.find('[role="tablist"]').classes()).toContain('border-b');
    });

    it('applies pills variant classes', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
          variant: 'pills',
        },
      });
      expect(wrapper.find('[role="tablist"]').classes()).toContain('bg-gray-100');
      expect(wrapper.find('[role="tablist"]').classes()).toContain('rounded-lg');
    });

    it('applies underline variant classes', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
          variant: 'underline',
        },
      });
      expect(wrapper.find('[role="tablist"]').classes()).toContain('border-b-2');
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
          size,
        },
      });
      const sizeMap = { sm: 'text-sm', md: 'text-base', lg: 'text-lg' };
      expect(wrapper.find('[role="tab"]').classes()).toContain(sizeMap[size]);
    });
  });

  describe('fullWidth', () => {
    it('applies flex-1 to tabs when fullWidth is true', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
          fullWidth: true,
        },
      });
      expect(wrapper.find('[role="tab"]').classes()).toContain('flex-1');
    });
  });

  describe('disabled tabs', () => {
    it('applies disabled styling', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      const disabledTab = wrapper.findAll('[role="tab"]')[2];
      expect(disabledTab.classes()).toContain('cursor-not-allowed');
      expect(disabledTab.classes()).toContain('opacity-50');
    });

    it('sets aria-disabled on disabled tabs', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      const disabledTab = wrapper.findAll('[role="tab"]')[2];
      expect(disabledTab.attributes('aria-disabled')).toBe('true');
    });
  });

  describe('accessibility', () => {
    it('has role="tablist" on container', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      expect(wrapper.find('[role="tablist"]').exists()).toBe(true);
    });

    it('has role="tab" on each tab', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
      });
      expect(wrapper.findAll('[role="tab"]')).toHaveLength(3);
    });

    it('sets tabindex correctly', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab2',
        },
      });
      const tabs = wrapper.findAll('[role="tab"]');
      expect(tabs[0].attributes('tabindex')).toBe('-1');
      expect(tabs[1].attributes('tabindex')).toBe('0');
      expect(tabs[2].attributes('tabindex')).toBe('-1');
    });
  });

  describe('slots', () => {
    it('renders custom tab content via slot', () => {
      const wrapper = mount(Tabs, {
        props: {
          items: defaultItems,
          modelValue: 'tab1',
        },
        slots: {
          tab: '<span class="custom-tab">Custom</span>',
        },
      });
      expect(wrapper.findAll('.custom-tab')).toHaveLength(3);
    });
  });
});
