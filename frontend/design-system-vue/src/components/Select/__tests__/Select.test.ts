import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import Select from '../Select.vue';

const defaultOptions = [
  { label: 'Option 1', value: 1 },
  { label: 'Option 2', value: 2 },
  { label: 'Option 3', value: 3, disabled: true },
];

describe('Select', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders with default props', () => {
    const wrapper = mount(Select, {
      props: { options: defaultOptions },
    });
    expect(wrapper.find('[role="combobox"]').exists()).toBe(true);
  });

  it('shows placeholder when no value selected', () => {
    const wrapper = mount(Select, {
      props: {
        options: defaultOptions,
        placeholder: 'Select an option',
      },
    });
    expect(wrapper.text()).toContain('Select an option');
  });

  it('shows selected option label', () => {
    const wrapper = mount(Select, {
      props: {
        options: defaultOptions,
        modelValue: 1,
      },
    });
    expect(wrapper.text()).toContain('Option 1');
  });

  describe('dropdown behavior', () => {
    it('opens dropdown on click', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
    });

    it('closes dropdown on escape', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.find('[role="listbox"]').exists()).toBe(true);

      await wrapper.find('[role="combobox"]').trigger('keydown', { key: 'Escape' });
      expect(wrapper.find('[role="listbox"]').exists()).toBe(false);
    });

    it('emits open event when opened', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.emitted('open')).toBeTruthy();
    });

    it('emits close event when closed', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.find('[role="combobox"]').trigger('keydown', { key: 'Escape' });
      expect(wrapper.emitted('close')).toBeTruthy();
    });
  });

  describe('option selection', () => {
    it('emits update:modelValue on option click', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.findAll('[role="option"]')[0].trigger('click');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([1]);
    });

    it('emits change event on selection', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.findAll('[role="option"]')[0].trigger('click');
      expect(wrapper.emitted('change')?.[0]).toEqual([1]);
    });

    it('does not select disabled options', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.findAll('[role="option"]')[2].trigger('click');
      expect(wrapper.emitted('update:modelValue')).toBeFalsy();
    });
  });

  describe('keyboard navigation', () => {
    it('opens on ArrowDown', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('keydown', { key: 'ArrowDown' });
      expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
    });

    it('opens on Enter', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('keydown', { key: 'Enter' });
      expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
    });

    it('opens on Space', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('keydown', { key: ' ' });
      expect(wrapper.find('[role="listbox"]').exists()).toBe(true);
    });

    it('navigates options with arrow keys', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.find('[role="combobox"]').trigger('keydown', { key: 'ArrowDown' });

      const options = wrapper.findAll('[role="option"]');
      expect(options[1].classes().some(c => c.includes('brand'))).toBe(true);
    });
  });

  describe('clearable', () => {
    it('shows clear button when clearable and has value', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          modelValue: 1,
          clearable: true,
        },
      });
      expect(wrapper.find('span svg').exists()).toBe(true);
    });

    it('clears value on clear button click', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          modelValue: 1,
          clearable: true,
        },
      });
      await wrapper.find('span svg').trigger('click');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([null]);
    });
  });

  describe('searchable', () => {
    it('shows search input when searchable', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          searchable: true,
        },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.find('input[type="text"]').exists()).toBe(true);
    });

    it('filters options based on search', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          searchable: true,
        },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.find('input[type="text"]').setValue('Option 1');

      const options = wrapper.findAll('[role="option"]');
      expect(options).toHaveLength(1);
      expect(options[0].text()).toContain('Option 1');
    });

    it('shows no options message when search has no results', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          searchable: true,
        },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      await wrapper.find('input[type="text"]').setValue('xyz');

      expect(wrapper.text()).toContain('No options found');
    });
  });

  describe('disabled state', () => {
    it('has disabled attribute when disabled', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          disabled: true,
        },
      });
      expect(wrapper.find('[role="combobox"]').attributes('disabled')).toBeDefined();
    });

    it('does not open when disabled', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          disabled: true,
        },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.find('[role="listbox"]').exists()).toBe(false);
    });
  });

  describe('error state', () => {
    it('shows error message', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          error: true,
          errorMessage: 'Required field',
        },
      });
      expect(wrapper.text()).toContain('Required field');
    });

    it('sets aria-invalid when error', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          error: true,
        },
      });
      expect(wrapper.find('[role="combobox"]').attributes('aria-invalid')).toBe('true');
    });
  });

  describe('label and required', () => {
    it('shows label', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          label: 'Country',
        },
      });
      expect(wrapper.text()).toContain('Country');
    });

    it('shows required indicator', () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          label: 'Country',
          required: true,
        },
      });
      expect(wrapper.text()).toContain('*');
    });
  });

  describe('accessibility', () => {
    it('has proper ARIA attributes on trigger', () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      const trigger = wrapper.find('[role="combobox"]');
      expect(trigger.attributes('aria-haspopup')).toBe('listbox');
      expect(trigger.attributes('aria-expanded')).toBe('false');
    });

    it('updates aria-expanded when open', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');
      expect(wrapper.find('[role="combobox"]').attributes('aria-expanded')).toBe('true');
    });

    it('has aria-selected on selected option', async () => {
      const wrapper = mount(Select, {
        props: {
          options: defaultOptions,
          modelValue: 1,
        },
      });
      await wrapper.find('[role="combobox"]').trigger('click');

      const selectedOption = wrapper.findAll('[role="option"]')[0];
      expect(selectedOption.attributes('aria-selected')).toBe('true');
    });

    it('has aria-disabled on disabled options', async () => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions },
      });
      await wrapper.find('[role="combobox"]').trigger('click');

      const disabledOption = wrapper.findAll('[role="option"]')[2];
      expect(disabledOption.attributes('aria-disabled')).toBe('true');
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Select, {
        props: { options: defaultOptions, size },
      });
      const sizeMap = { sm: 'h-8', md: 'h-10', lg: 'h-12' };
      expect(wrapper.find('[role="combobox"]').classes()).toContain(sizeMap[size]);
    });
  });
});
