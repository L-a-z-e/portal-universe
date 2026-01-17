import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import Dropdown from '../Dropdown.vue';
import type { DropdownItem } from '../Dropdown.types';

const defaultItems: DropdownItem[] = [
  { label: 'Edit', value: 'edit' },
  { label: 'Delete', value: 'delete' },
  { label: 'Disabled', value: 'disabled', disabled: true },
  { label: '', divider: true },
  { label: 'Settings', value: 'settings' },
];

describe('Dropdown', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders trigger', () => {
    const wrapper = mount(Dropdown, {
      props: { items: defaultItems },
    });
    expect(wrapper.find('[role="button"]').exists()).toBe(true);
  });

  it('does not show menu by default', () => {
    const wrapper = mount(Dropdown, {
      props: { items: defaultItems },
    });
    expect(wrapper.find('[role="menu"]').exists()).toBe(false);
  });

  describe('click trigger', () => {
    it('opens menu on click', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(true);
    });

    it('closes menu on second click', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(true);
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    });

    it('emits open event when opened', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.emitted('open')).toBeTruthy();
    });

    it('emits close event when closed', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.find('[role="button"]').trigger('keydown', { key: 'Escape' });
      expect(wrapper.emitted('close')).toBeTruthy();
    });
  });

  describe('item selection', () => {
    it('renders all menu items', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.findAll('[role="menuitem"]')).toHaveLength(4);
    });

    it('renders divider', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="separator"]').exists()).toBe(true);
    });

    it('emits select event on item click', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.findAll('[role="menuitem"]')[0].trigger('click');
      expect(wrapper.emitted('select')?.[0]).toEqual([{ label: 'Edit', value: 'edit' }]);
    });

    it('closes menu after selection by default', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.findAll('[role="menuitem"]')[0].trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    });

    it('does not close menu when closeOnSelect is false', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems, closeOnSelect: false },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.findAll('[role="menuitem"]')[0].trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(true);
    });

    it('does not emit select for disabled items', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.findAll('[role="menuitem"]')[2].trigger('click');
      expect(wrapper.emitted('select')).toBeFalsy();
    });
  });

  describe('keyboard navigation', () => {
    it('opens menu on ArrowDown', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('keydown', { key: 'ArrowDown' });
      expect(wrapper.find('[role="menu"]').exists()).toBe(true);
    });

    it('opens menu on Enter', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('keydown', { key: 'Enter' });
      expect(wrapper.find('[role="menu"]').exists()).toBe(true);
    });

    it('closes menu on Escape', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.find('[role="button"]').trigger('keydown', { key: 'Escape' });
      expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    });

    it('navigates items with arrow keys', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      await wrapper.find('[role="button"]').trigger('keydown', { key: 'ArrowDown' });

      const items = wrapper.findAll('[role="menuitem"]');
      expect(items[0].classes().some(c => c.includes('brand'))).toBe(true);
    });
  });

  describe('disabled state', () => {
    it('does not open when disabled', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems, disabled: true },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    });

    it('sets aria-disabled when disabled', () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems, disabled: true },
      });
      expect(wrapper.find('[role="button"]').attributes('aria-disabled')).toBe('true');
    });

    it('sets tabindex=-1 when disabled', () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems, disabled: true },
      });
      expect(wrapper.find('[role="button"]').attributes('tabindex')).toBe('-1');
    });
  });

  describe('placement', () => {
    it.each([
      'bottom',
      'bottom-start',
      'bottom-end',
      'top',
      'top-start',
      'top-end',
    ] as const)('applies %s placement classes', async (placement) => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems, placement },
      });
      await wrapper.find('[role="button"]').trigger('click');

      const menu = wrapper.find('[role="menu"]');
      if (placement.includes('top')) {
        expect(menu.classes()).toContain('bottom-full');
      } else {
        expect(menu.classes()).toContain('top-full');
      }
    });
  });

  describe('accessibility', () => {
    it('has aria-haspopup on trigger', () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      expect(wrapper.find('[role="button"]').attributes('aria-haspopup')).toBe('menu');
    });

    it('updates aria-expanded when open', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      expect(wrapper.find('[role="button"]').attributes('aria-expanded')).toBe('false');
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.find('[role="button"]').attributes('aria-expanded')).toBe('true');
    });

    it('links trigger to menu via aria-controls', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');

      const trigger = wrapper.find('[role="button"]');
      const menu = wrapper.find('[role="menu"]');
      expect(trigger.attributes('aria-controls')).toBe(menu.attributes('id'));
    });

    it('menu items have role="menuitem"', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.findAll('[role="menuitem"]').length).toBeGreaterThan(0);
    });

    it('disabled items have disabled attribute', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
      });
      await wrapper.find('[role="button"]').trigger('click');

      const disabledItem = wrapper.findAll('[role="menuitem"]')[2];
      expect(disabledItem.attributes('disabled')).toBeDefined();
    });
  });

  describe('slots', () => {
    it('renders custom trigger via slot', () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
        slots: {
          trigger: '<span data-testid="custom-trigger">Click me</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom-trigger"]').exists()).toBe(true);
    });

    it('renders custom item content via slot', async () => {
      const wrapper = mount(Dropdown, {
        props: { items: defaultItems },
        slots: {
          item: '<span class="custom-item">Custom</span>',
        },
      });
      await wrapper.find('[role="button"]').trigger('click');
      expect(wrapper.findAll('.custom-item').length).toBeGreaterThan(0);
    });
  });
});
